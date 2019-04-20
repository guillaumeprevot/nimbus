package fr.techgp.nimbus.facets;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.bson.Document;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;

public class PDFBoxFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "pdf".equals(extension);
	}

	@Override
	public void loadMetadata(Document bson, JsonObject node) {
		node.addProperty("thumbnail", bson.getString("thumbnail"));
		node.addProperty("pageCount", bson.getInteger("pageCount"));
		node.addProperty("pageWidthInMillimeters", bson.getInteger("pageWidthInMillimeters"));
		node.addProperty("pageHeightInMillimeters", bson.getInteger("pageHeightInMillimeters"));
	}

	@Override
	public void updateMetadata(File file, String extension, Document bson) throws Exception {
		try (PDDocument doc = PDDocument.load(file)) {
			// Page count
			int pageCount = doc.getNumberOfPages();
			bson.put("pageCount", pageCount);
			if (pageCount > 0) {
				// Dimensions of first page
				PDPage page = doc.getPage(0);
				PDRectangle dimensions = page.getMediaBox();
				float ratio = 25.4f / 72;
				bson.put("pageWidthInMillimeters", Math.round(dimensions.getWidth() * ratio));
				bson.put("pageHeightInMillimeters", Math.round(dimensions.getHeight() * ratio));
				// Preview of first page
				PDFRenderer pdfRenderer = new PDFRenderer(doc);
				int previewDotPerInch = 36;
				BufferedImage image = pdfRenderer.renderImageWithDPI(0, previewDotPerInch, ImageType.RGB);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(image, "png", baos);
				byte[] bytes = baos.toByteArray();
				bson.put("thumbnail", "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bytes));
			}
		}
	}

}

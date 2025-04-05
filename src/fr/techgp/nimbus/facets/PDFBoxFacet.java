package fr.techgp.nimbus.facets;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;

public class PDFBoxFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "pdf".equals(extension);
	}

	@Override
	public void loadMetadata(Metadatas metadatas, JsonObject node) {
		node.addProperty("thumbnail", metadatas.getString("thumbnail"));
		node.addProperty("pageCount", metadatas.getInteger("pageCount"));
		node.addProperty("pageWidthInMillimeters", metadatas.getInteger("pageWidthInMillimeters"));
		node.addProperty("pageHeightInMillimeters", metadatas.getInteger("pageHeightInMillimeters"));
	}

	@Override
	public void updateMetadata(File file, String extension, Metadatas metadatas) throws Exception {
		try (PDDocument doc = Loader.loadPDF(file)) {
			// Page count
			int pageCount = doc.getNumberOfPages();
			metadatas.put("pageCount", pageCount);
			if (pageCount > 0) {
				// Dimensions of first page
				PDPage page = doc.getPage(0);
				PDRectangle dimensions = page.getMediaBox();
				float ratio = 25.4f / 72;
				metadatas.put("pageWidthInMillimeters", Math.round(dimensions.getWidth() * ratio));
				metadatas.put("pageHeightInMillimeters", Math.round(dimensions.getHeight() * ratio));
				// Preview of first page
				PDFRenderer pdfRenderer = new PDFRenderer(doc);
				int previewDotPerInch = 36;
				BufferedImage image = pdfRenderer.renderImageWithDPI(0, previewDotPerInch, ImageType.RGB);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(image, "png", baos);
				byte[] bytes = baos.toByteArray();
				metadatas.put("thumbnail", "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bytes));
			}
		}
	}

}

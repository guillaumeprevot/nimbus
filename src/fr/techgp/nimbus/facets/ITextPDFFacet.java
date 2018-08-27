package fr.techgp.nimbus.facets;

import java.io.File;

import org.bson.Document;

import com.google.gson.JsonObject;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.Utilities;
import com.itextpdf.text.pdf.PdfReader;

import fr.techgp.nimbus.Facet;

public class ITextPDFFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "pdf".equals(extension);
	}

	@Override
	public void loadMetadata(Document bson, JsonObject node) {
		node.addProperty("pageCount", bson.getInteger("pageCount"));
		node.addProperty("pageWidthInMillimeters", bson.getInteger("pageWidthInMillimeters"));
		node.addProperty("pageHeightInMillimeters", bson.getInteger("pageHeightInMillimeters"));
	}

	@Override
	public void updateMetadata(File file, String extension, Document bson) throws Exception {
		PdfReader reader = new PdfReader(file.getAbsolutePath());
		int pageCount = reader.getNumberOfPages();
		bson.put("pageCount", pageCount);
		if (pageCount > 0) {
			Rectangle rectangle = reader.getPageSizeWithRotation(1);
			bson.put("pageWidthInMillimeters", Math.round(Utilities.pointsToMillimeters(rectangle.getWidth())));
			bson.put("pageHeightInMillimeters", Math.round(Utilities.pointsToMillimeters(rectangle.getHeight())));
		}
	}

}

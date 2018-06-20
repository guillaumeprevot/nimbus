package fr.techgp.nimbus.facets;

import java.io.File;

import org.bson.Document;

import com.google.gson.JsonObject;
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
		bson.put("pageCount", reader.getNumberOfPages());
		bson.put("pageWidthInMillimeters", Math.round(reader.getPageSize(0).getWidth() / 2));
		bson.put("pageHeightInMillimeters", Math.round(reader.getPageSize(0).getHeight() / 2));
	}

}

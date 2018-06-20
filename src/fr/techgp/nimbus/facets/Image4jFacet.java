package fr.techgp.nimbus.facets;

import java.io.File;

import org.bson.Document;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import net.sf.image4j.codec.ico.ICODecoder;

public class Image4jFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "ico".equals(extension);
	}

	@Override
	public void loadMetadata(Document bson, JsonObject node) {
		node.addProperty("width", bson.getInteger("width"));
		node.addProperty("height", bson.getInteger("height"));
		node.addProperty("depth", bson.getInteger("depth"));
	}

	@Override
	public void updateMetadata(File file, String extension, Document bson) throws Exception {
		ICODecoder.readExt(file).stream().reduce((i1, i2) -> i1.getWidth() > i2.getWidth() ? i1 : i2).ifPresent((i) -> {
			bson.put("width", i.getWidth());
			bson.put("height", i.getHeight());
			bson.put("depth", i.getColourDepth());
		});
	}

}

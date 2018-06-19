package fr.techgp.nimbus.facets;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.bson.Document;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;

public class StandardImageFacet implements Facet {

	private final String extensions = "," + String.join(",", ImageIO.getReaderFileSuffixes()).toLowerCase() + ",";

	@Override
	public boolean supports(String extension) {
		return this.extensions.contains("," + extension + ",");
	}

	@Override
	public void loadMetadata(Document bson, JsonObject node) {
		node.addProperty("width", bson.getInteger("width"));
		node.addProperty("height", bson.getInteger("height"));
		node.addProperty("depth", bson.getInteger("depth"));
	}

	@Override
	public void updateMetadata(File file, String extension, Document bson) throws Exception {
		BufferedImage image = ImageIO.read(file);
		if (image != null) {
			bson.put("width", image.getWidth());
			bson.put("height", image.getHeight());
			bson.put("depth", image.getColorModel().getPixelSize());
		}
	}

}

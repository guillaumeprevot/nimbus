package fr.techgp.nimbus.facets;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;

public class StandardImageFacet implements Facet {

	private final String extensions = "," + String.join(",", ImageIO.getReaderFileSuffixes()).toLowerCase() + ",";

	@Override
	public boolean supports(String extension) {
		return this.extensions.contains("," + extension + ",");
	}

	@Override
	public void loadMetadata(Metadatas metadatas, JsonObject node) {
		node.addProperty("width", metadatas.getInteger("width"));
		node.addProperty("height", metadatas.getInteger("height"));
		node.addProperty("depth", metadatas.getInteger("depth"));
	}

	@Override
	public void updateMetadata(File file, String extension, Metadatas metadatas) throws Exception {
		BufferedImage image = ImageIO.read(file);
		if (image != null) {
			metadatas.put("width", image.getWidth());
			metadatas.put("height", image.getHeight());
			metadatas.put("depth", image.getColorModel().getPixelSize());
		}
	}

}

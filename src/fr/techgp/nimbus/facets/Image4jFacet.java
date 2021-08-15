package fr.techgp.nimbus.facets;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;
import fr.techgp.nimbus.utils.GraphicsUtils;
import net.sf.image4j.codec.ico.ICODecoder;
import net.sf.image4j.codec.ico.ICOEncoder;

public class Image4jFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "ico".equals(extension);
	}

	@Override
	public void loadMetadata(Metadatas metadatas, JsonObject node) {
		node.addProperty("width", metadatas.getInteger("width"));
		node.addProperty("height", metadatas.getInteger("height"));
		node.addProperty("depth", metadatas.getInteger("depth"));
	}

	@Override
	public void updateMetadata(File file, String extension, Metadatas metadatas) throws Exception {
		ICODecoder.readExt(file).stream().reduce((i1, i2) -> i1.getWidth() > i2.getWidth() ? i1 : i2).ifPresent((i) -> {
			metadatas.put("width", i.getWidth());
			metadatas.put("height", i.getHeight());
			metadatas.put("depth", i.getColourDepth());
		});
	}

	/** Cette méthode crée une miniature du fichier .ico passé en paramètre en conservant les proportions d'origine */
	public static final byte[] getScaleICOImage(File file, Integer targetWidth, Integer targetHeight) throws IOException {
		try (InputStream inputStream = new FileInputStream(file)) {
			List<BufferedImage> images = ICODecoder.read(inputStream);
			Integer targetSize = targetWidth != null ? targetWidth : targetHeight;
			BufferedImage bestImage = null;
			for (BufferedImage i : images) {
				if (bestImage == null) {
					bestImage = i;
				} else {
					Integer bestSize = targetWidth != null ? bestImage.getWidth() : bestImage.getHeight();
					Integer currentSize = targetWidth != null ? i.getWidth() : i.getHeight();
					if ((bestSize < targetSize && currentSize > bestSize) // on préfère une plus grande qu'une plus petite
						|| (bestSize > targetSize && currentSize > targetSize && currentSize < bestSize)) // mais la + petite des + grandes
						bestImage = i;
				}
			}

			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				images = new ArrayList<>(1);
				images.add(GraphicsUtils.scaleImageWithMaxDimensions(bestImage, targetWidth, targetHeight));
				ICOEncoder.write(images, outputStream);
				return outputStream.toByteArray();
			}
		}
	}

}

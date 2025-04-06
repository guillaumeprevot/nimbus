package fr.techgp.nimbus.facets;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.imaging.Imaging;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;
import fr.techgp.nimbus.utils.GraphicsUtils;

public class ApacheCommonsImagingICOFacet implements Facet {

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
		List<BufferedImage> allImages = load(file, extension);
		Optional<BufferedImage> largestImage = allImages.stream().reduce((i1, i2) -> i1.getWidth() > i2.getWidth() ? i1 : i2);
		if (largestImage.isPresent()) {
			BufferedImage image = largestImage.get();
			metadatas.put("width", image.getWidth());
			metadatas.put("height", image.getHeight());
			metadatas.put("depth", image.getColorModel().getPixelSize());
		}
	}

	@Override
	public boolean supportsThumbnail(String extension) {
		return supports(extension);
	}

	@Override
	public String getThumbnailMimeType(String extension) {
		return "image/png";
	}

	@Override
	public byte[] generateThumbnail(File file, String extension, Integer targetWidth, Integer targetHeight)
			throws IOException {
		List<BufferedImage> images = load(file, extension);
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

		BufferedImage output = GraphicsUtils.scaleImageWithMaxDimensions(bestImage, targetWidth, targetHeight);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(baos)) {

			ImageWriter writer = ImageIO.getImageWritersBySuffix("png").next();
			writer.setOutput(outputStream);
			writer.write(output);
			writer.setOutput(null);
		}
		return baos.toByteArray();
	}

	private static final List<BufferedImage> load(File file, String extension) throws IOException {
		try (FileInputStream is = new FileInputStream(file)) {
			return Imaging.getAllBufferedImages(is, "fakenameforextension." + extension);
		}
	}
}

package fr.techgp.nimbus.utils;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

public final class ImageUtils {

	private ImageUtils() {
		//
	}

	/** Cette méthode crée une miniature du fichier passé en paramètre en conservant les proportions d'origine */
	public static final byte[] getScaleImage(File file, Integer targetWidth, Integer targetHeight) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ImageInputStream inputStream = ImageIO.createImageInputStream(file);
				ImageOutputStream outputStream = ImageIO.createImageOutputStream(baos)) {

			Iterator<ImageReader> readers = ImageIO.getImageReaders(inputStream);
			ImageReader reader = readers.next();
			reader.setInput(inputStream);

			ImageWriter writer = ImageIO.getImageWriter(reader);
			writer.setOutput(outputStream);

			BufferedImage image = reader.read(0);
			image = getScaledImageWithMaxDimensions(image, targetWidth, targetHeight);
			writer.write(image);
			reader.setInput(null);
			writer.setOutput(null);

		}
		return baos.toByteArray();
	}

	/** Cette méthode redimensionne une image afin qu'elle tienne dans le rectangle donnée, en conservant les proportions */
	private static final BufferedImage getScaledImageWithMaxDimensions(BufferedImage source, Integer targetWidth, Integer targetHeight) {
		int width = source.getWidth();
		int height = source.getHeight();
		if (targetWidth != null && width > targetWidth) {
			height = height * targetWidth / width;
			width = targetWidth;
		}
		if (targetHeight != null && height > targetHeight) {
			width = width * targetHeight / height;
			height = targetHeight;
		}
		BufferedImage result = source;
		if (width != source.getWidth() || height != source.getHeight()) {
			result = getScaledImageWithFixedDimensions(source, width, height, false);
			result.flush();
		}
		return result;
	}

	/** Cette méthode redimensionne une image aux dimensions données, sans se préoccuper des des proportions */
	//@see http://www.thebuzzmedia.com/software/imgscalr-java-image-scaling-library/
	private static final BufferedImage getScaledImageWithFixedDimensions(BufferedImage source, int targetWidth, int targetHeight,
			boolean superQuality) {

		int currentWidth = source.getWidth();
		int currentHeight = source.getHeight();
		int fraction = superQuality ? 7 : 2;
		BufferedImage image = source;
		do {
			int prevCurrentWidth = currentWidth;
			int prevCurrentHeight = currentHeight;

			if (currentWidth > targetWidth) {
				currentWidth -= (currentWidth / fraction);
				if (currentWidth < targetWidth)
					currentWidth = targetWidth;
			}

			if (currentHeight > targetHeight) {
				currentHeight -= (currentHeight / fraction);
				if (currentHeight < targetHeight)
					currentHeight = targetHeight;
			}

			if (prevCurrentWidth == currentWidth && prevCurrentHeight == currentHeight)
				break;

			// Render the incremental scaled image.
			image = getScaledImageLoop(image, currentWidth, currentHeight);
			image.flush();

		} while (currentWidth != targetWidth || currentHeight != targetHeight);

		return image;
	}

	private static final BufferedImage getScaledImageLoop(BufferedImage source, int targetWidth, int targetHeight) {
		boolean withAlpha = source.getTransparency() != Transparency.OPAQUE;
		int type = withAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
		BufferedImage result = new BufferedImage(targetWidth, targetHeight, type);
		Graphics2D resultGraphics = result.createGraphics();
		resultGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		resultGraphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
		resultGraphics.dispose();
		return result;
	}

}

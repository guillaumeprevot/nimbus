package fr.techgp.nimbus.facets;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Configuration;
import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;

public class ApacheCommonsImagingJPGFacet implements Facet {

	@Override
	public void init(Configuration configuration) {
		throw new UnsupportedOperationException(ApacheCommonsImagingJPGFacet.class.getSimpleName() + " n'est pas utilisable pour le moment");
	}

	@Override
	public boolean supports(String extension) {
		return "jpg".equals(extension) || "jpeg".equals(extension);
	}

	@Override
	public void loadMetadata(Metadatas metadatas, JsonObject node) {
		node.addProperty("latitude", metadatas.getDouble("latitude"));
		node.addProperty("longitude", metadatas.getDouble("longitude"));
		node.addProperty("date", metadatas.getLong("date"));
	}

	@Override
	public void updateMetadata(File file, String extension, Metadatas metadatas) throws Exception {
		try (FileInputStream is = new FileInputStream(file)) {
			final ImageMetadata metadata = Imaging.getMetadata(is, "fakenameforextension." + extension);
			if (metadata instanceof JpegImageMetadata) {
				final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;

				//final TiffField dateField = jpegMetadata.findExifValueWithExactMatch(TiffTagConstants.TIFF_TAG_DATE_TIME);
				final TiffField dateField = jpegMetadata.findExifValueWithExactMatch(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
				if (dateField != null) {
					// Une date au format texte uniquement, par exemple "2024:04:30 18:36:42"
					//String dateString = dateField.getStringValue();
					// Malheureusement, le format n'est pas standardisé, c'est MetadataExtractor qui nous siomplifiait la vie
					// https://github.com/drewnoakes/metadata-extractor/blob/main/Source/com/drew/metadata/Directory.java#L870
					// - le séparateur peut être ":", "-", "." ou absent
					// - le séparateur entre date et heure peut être " ", "'T'" ou absent
					// - on peut avoir "annnée[mois[jour[heureMinute[seconde]]]]"
					// - on peut aussi avoir "    :  :     :  :  " pour dire "inconnue"
					// - on peut aussi avoir "0000:00:00 00:00:00" pour dire "null"
				}

				final TiffImageMetadata exifMetadata = jpegMetadata.getExif();
				if (exifMetadata != null) {
					final TiffImageMetadata.GpsInfo gpsInfo = exifMetadata.getGpsInfo();
					if (gpsInfo != null) {
						metadatas.put("latitude", gpsInfo.getLatitudeAsDegreesNorth());
						metadatas.put("longitude", gpsInfo.getLongitudeAsDegreesEast());

					}
				}
			}

		}
	}

}

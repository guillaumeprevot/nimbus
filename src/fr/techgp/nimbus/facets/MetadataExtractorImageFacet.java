package fr.techgp.nimbus.facets;

import java.io.File;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;

public class MetadataExtractorImageFacet implements Facet {

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
		Metadata metadata = ImageMetadataReader.readMetadata(file);
		// Get metadatas from EXIF
		// http://www.awaresystems.be/imaging/tiff/tifftags/privateifd/exif.html
		ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
		if (exif != null && exif.getDateOriginal() != null) {
			metadatas.put("date", exif.getDateOriginal().getTime());
		}
		// Get metadatas from GPS
		// http://www.awaresystems.be/imaging/tiff/tifftags/privateifd/gps.html
		GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
		if (gps != null) {
			GeoLocation location = gps.getGeoLocation();
			if (location != null) {
				metadatas.put("latitude", location.getLatitude());
				metadatas.put("longitude", location.getLongitude());
				if (!metadatas.has("date") && gps.getGpsDate() != null)
					metadatas.put("date", gps.getGpsDate().getTime());
			}
		}
	}

}

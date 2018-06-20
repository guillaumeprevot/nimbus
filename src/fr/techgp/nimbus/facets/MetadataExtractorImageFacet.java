package fr.techgp.nimbus.facets;

import java.io.File;
import java.util.Date;

import org.bson.Document;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;

public class MetadataExtractorImageFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "jpg".equals(extension) || "jpeg".equals(extension);
	}

	@Override
	public void loadMetadata(Document bson, JsonObject node) {
		node.addProperty("latitude", bson.getDouble("latitude"));
		node.addProperty("longitude", bson.getDouble("longitude"));
		Date date = bson.getDate("date");
		if (date != null)
			node.addProperty("date", date.getTime());
	}


	@Override
	public void updateMetadata(File file, String extension, Document bson) throws Exception {
		Metadata metadata = ImageMetadataReader.readMetadata(file);
		// Get metadatas from EXIF
		// http://www.awaresystems.be/imaging/tiff/tifftags/privateifd/exif.html
		ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
		if (exif != null) {
			bson.put("date", exif.getDateOriginal());
		}
		// Get metadatas from GPS
		// http://www.awaresystems.be/imaging/tiff/tifftags/privateifd/gps.html
		GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
		if (gps != null) {			
			GeoLocation location = gps.getGeoLocation();
			if (location != null) {
				bson.put("latitude", location.getLatitude());
				bson.put("longitude", location.getLongitude());
				bson.putIfAbsent("date", gps.getGpsDate());
			}
		}
	}

}

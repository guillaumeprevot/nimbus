package fr.techgp.nimbus.facets;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

import org.bson.Document;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.techgp.nimbus.Facet;

public class CalendarFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "calendar".equals(extension);
	}

	@Override
	public void loadMetadata(Document bson, JsonObject node) {
		node.addProperty("displayName", bson.getString("displayName"));
		node.addProperty("typeCount", bson.getInteger("typeCount"));
		node.addProperty("eventCount", bson.getInteger("eventCount"));
	}

	@Override
	public void updateMetadata(File file, String extension, Document bson) throws Exception {
		try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
			JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
			if (o.has("name"))
				bson.append("displayName", o.get("name").getAsString());
			if (o.has("types"))
				bson.append("typeCount", o.get("types").getAsJsonArray().size());
			if (o.has("events"))
				bson.append("eventCount", o.get("events").getAsJsonArray().size());
		} catch (Exception ex) {
			bson.remove("displayName");
			bson.remove("typeCount");
			bson.remove("eventCount");
		}
	}

}

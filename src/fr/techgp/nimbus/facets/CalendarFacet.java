package fr.techgp.nimbus.facets;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;

public class CalendarFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "calendar".equals(extension);
	}

	@Override
	public void loadMetadata(Metadatas metadatas, JsonObject node) {
		node.addProperty("displayName", metadatas.getString("displayName"));
		node.addProperty("typeCount", metadatas.getInteger("typeCount"));
		node.addProperty("eventCount", metadatas.getInteger("eventCount"));
	}

	@Override
	public void updateMetadata(File file, String extension, Metadatas metadatas) throws Exception {
		try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
			JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
			if (o.has("name") && !o.get("name").isJsonNull())
				metadatas.put("displayName", o.get("name").getAsString());
			if (o.has("types") && o.get("types").isJsonArray())
				metadatas.put("typeCount", o.get("types").getAsJsonArray().size());
			if (o.has("events") && o.get("events").isJsonArray())
				metadatas.put("eventCount", o.get("events").getAsJsonArray().size());
		} catch (Exception ex) {
			metadatas.remove("displayName");
			metadatas.remove("typeCount");
			metadatas.remove("eventCount");
		}
	}

}

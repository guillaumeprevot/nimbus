package fr.techgp.nimbus.facets;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;

public class NimbusContactsFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "contacts".equals(extension);
	}

	@Override
	public void loadMetadata(Metadatas metadatas, JsonObject node) {
		node.addProperty("displayName", metadatas.getString("displayName"));
		node.addProperty("contactCount", metadatas.getInteger("contactCount"));
		node.addProperty("favoriteCount", metadatas.getInteger("favoriteCount"));
	}

	@Override
	public void updateMetadata(File file, String extension, Metadatas metadatas) throws Exception {
		try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
			JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
			if (o.has("name") && !o.get("name").isJsonNull())
				metadatas.put("displayName", o.get("name").getAsString());
			if (o.has("contacts") && o.get("contacts").isJsonArray()) {
				JsonArray contacts = o.get("contacts").getAsJsonArray();
				metadatas.put("contactCount", contacts.size());
				int favoriteCount = 0;
				for (JsonElement e : contacts) {
					JsonObject c = e.getAsJsonObject();
					if (c != null && c.has("favorite") && c.get("favorite").getAsBoolean())
						favoriteCount++;
				}
				metadatas.put("favoriteCount", favoriteCount);
			}
		} catch (Exception ex) {
			metadatas.remove("displayName");
			metadatas.remove("contactCount");
			metadatas.remove("favoriteCount");
		}
	}

}

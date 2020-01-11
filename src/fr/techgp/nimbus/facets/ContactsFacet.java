package fr.techgp.nimbus.facets;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

import org.bson.Document;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.techgp.nimbus.Facet;

public class ContactsFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "contacts".equals(extension);
	}

	@Override
	public void loadMetadata(Document bson, JsonObject node) {
		node.addProperty("displayName", bson.getString("displayName"));
		node.addProperty("contactCount", bson.getInteger("contactCount"));
		node.addProperty("favoriteCount", bson.getInteger("favoriteCount"));
	}

	@Override
	public void updateMetadata(File file, String extension, Document bson) throws Exception {
		try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
			JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
			if (o.has("name"))
				bson.append("displayName", o.get("name").getAsString());
			if (o.has("contacts")) {
				JsonArray contacts = o.get("contacts").getAsJsonArray();
				bson.append("contactCount", contacts.size());
				int favoriteCount = 0;
				for (JsonElement e : contacts) {
					JsonObject c = e.getAsJsonObject();
					if (c != null && c.has("favorite") && c.get("favorite").getAsBoolean())
						favoriteCount++;
				}
				bson.append("favoriteCount", favoriteCount);
			}
		} catch (Exception ex) {
			bson.remove("displayName");
			bson.remove("contactCount");
			bson.remove("favoriteCount");
		}
	}

}

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

public class BookmarksFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "bookmarks".equals(extension);
	}

	@Override
	public void loadMetadata(Metadatas metadatas, JsonObject node) {
		node.addProperty("displayName", metadatas.getString("displayName"));
		node.addProperty("folderCount", metadatas.getInteger("folderCount"));
		node.addProperty("bookmarkCount", metadatas.getInteger("bookmarkCount"));
	}

	@Override
	public void updateMetadata(File file, String extension, Metadatas metadatas) throws Exception {
		try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
			JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
			if (o.has("name") && !o.get("name").isJsonNull())
				metadatas.put("displayName", o.get("name").getAsString());
			if (o.has("folders") && o.get("folders").isJsonArray()) {
				JsonArray folders = o.get("folders").getAsJsonArray();
				metadatas.put("folderCount", folders.size());
				int bookmarkCount = 0;
				for (JsonElement e : folders) {
					JsonObject f = e.getAsJsonObject();
					if (f != null && f.has("bookmarks") && f.get("bookmarks").isJsonArray())
						bookmarkCount += f.get("bookmarks").getAsJsonArray().size();
				}
				metadatas.put("bookmarkCount", bookmarkCount);
			}
		} catch (Exception ex) {
			metadatas.remove("displayName");
			metadatas.remove("folderCount");
			metadatas.remove("bookmarkCount");
		}
	}

}

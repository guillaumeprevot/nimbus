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

public class BookmarksFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "bookmarks".equals(extension);
	}

	@Override
	public void loadMetadata(Document bson, JsonObject node) {
		node.addProperty("displayName", bson.getString("displayName"));
		node.addProperty("folderCount", bson.getInteger("folderCount"));
		node.addProperty("bookmarkCount", bson.getInteger("bookmarkCount"));
	}

	@Override
	public void updateMetadata(File file, String extension, Document bson) throws Exception {
		try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
			JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
			if (o.has("name"))
				bson.append("displayName", o.get("name").getAsString());
			if (o.has("folders")) {
				JsonArray folders = o.get("folders").getAsJsonArray();
				bson.append("folderCount", folders.size());
				int bookmarkCount = 0;
				for (JsonElement e : folders) {
					JsonObject f = e.getAsJsonObject();
					if (f != null && f.has("bookmarks"))
						bookmarkCount += f.get("bookmarks").getAsJsonArray().size();
				}
				bson.append("bookmarkCount", bookmarkCount);
			}
		} catch (Exception ex) {
			bson.remove("displayName");
			bson.remove("folderCount");
			bson.remove("bookmarkCount");
		}
	}

}

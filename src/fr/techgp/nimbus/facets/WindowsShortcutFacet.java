package fr.techgp.nimbus.facets;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.bson.Document;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;

public class WindowsShortcutFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "url".equals(extension);
	}

	@Override
	public void loadMetadata(Document bson, JsonObject node) {
		node.addProperty("url", bson.getString("url"));
		node.addProperty("iconURL", bson.getString("iconURL"));
		node.addProperty("iconIndex", bson.getInteger("iconIndex"));
	}

	@Override
	public void updateMetadata(File file, String extension, Document bson) throws Exception {
		try (LineIterator li = FileUtils.lineIterator(file);) {
			boolean isInternetShortcut = false;
			while (li.hasNext()) {
				String line = li.next();
				if ("[InternetShortcut]".equals(line))
					isInternetShortcut = true; // on entre dans la section
				else if (isInternetShortcut && line.startsWith("URL="))
					bson.put("url", line.substring(4)); // on trouve l'URL
				else if (isInternetShortcut && line.startsWith("IconFile="))
					bson.put("iconURL", line.substring(9)); // on trouve une image
				else if (isInternetShortcut && line.startsWith("IconIndex="))
					bson.put("iconIndex", Integer.valueOf(line.substring(10))); // on trouve l'image à utiliser dans les .ico contenant plusieurs images
				else if (line.startsWith("["))
					isInternetShortcut = false; // on sort de la section
			}
		}
	}
}

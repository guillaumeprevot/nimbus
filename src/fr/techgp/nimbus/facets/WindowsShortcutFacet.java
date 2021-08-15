package fr.techgp.nimbus.facets;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;

public class WindowsShortcutFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "url".equals(extension);
	}

	@Override
	public void loadMetadata(Metadatas metadatas, JsonObject node) {
		node.addProperty("url", metadatas.getString("url"));
		node.addProperty("iconURL", metadatas.getString("iconURL"));
		node.addProperty("iconIndex", metadatas.getInteger("iconIndex"));
	}

	@Override
	public void updateMetadata(File file, String extension, Metadatas metadatas) throws Exception {
		try (LineIterator li = FileUtils.lineIterator(file);) {
			boolean isInternetShortcut = false;
			while (li.hasNext()) {
				String line = li.next();
				if ("[InternetShortcut]".equals(line))
					isInternetShortcut = true; // on entre dans la section
				else if (isInternetShortcut && line.startsWith("URL="))
					metadatas.put("url", line.substring(4)); // on trouve l'URL
				else if (isInternetShortcut && line.startsWith("IconFile="))
					metadatas.put("iconURL", line.substring(9)); // on trouve une image
				else if (isInternetShortcut && line.startsWith("IconIndex="))
					metadatas.put("iconIndex", Integer.valueOf(line.substring(10))); // on trouve l'image à utiliser dans les .ico contenant plusieurs images
				else if (line.startsWith("["))
					isInternetShortcut = false; // on sort de la section
			}
		}
	}
}

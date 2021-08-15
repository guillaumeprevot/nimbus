package fr.techgp.nimbus.facets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Set;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Configuration;
import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;

public class StandardTextFacet implements Facet {

	private Set<String> extensions;

	@Override
	public void init(Configuration configuration) {
		this.extensions = configuration.getTextFileExtensions();
	}

	@Override
	public boolean supports(String extension) {
		return this.extensions.contains(extension);
	}

	@Override
	public void loadMetadata(Metadatas metadatas, JsonObject node) {
		node.addProperty("lines", metadatas.getInteger("lines"));
	}

	@Override
	public void updateMetadata(File file, String extension, Metadatas metadatas) throws Exception {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			int lines = 0;
			while (reader.readLine() != null) {
				lines++;
			}
			metadatas.put("lines", lines);
		}
	}

}

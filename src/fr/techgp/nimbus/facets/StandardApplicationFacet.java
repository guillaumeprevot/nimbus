package fr.techgp.nimbus.facets;

import java.io.File;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;

public class StandardApplicationFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "application".equals(extension);
	}

	@Override
	public void loadMetadata(Metadatas metadatas, JsonObject node) {
		node.addProperty("runCount", metadatas.getInteger("runCount"));
		node.addProperty("runLast", metadatas.getLong("runLast"));
	}

	@Override
	public void updateMetadata(File file, String extension, Metadatas metadatas) throws Exception {
		// Pour le moment, on profite de la mise à jour des méta-données pour vider les stats de l'application
		metadatas.put("runCount", 0);
		metadatas.put("runLast", 0L);
	}

}

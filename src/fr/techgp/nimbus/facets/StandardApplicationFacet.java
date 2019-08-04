package fr.techgp.nimbus.facets;

import java.io.File;

import org.bson.Document;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;

public class StandardApplicationFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "application".equals(extension);
	}

	@Override
	public void loadMetadata(Document bson, JsonObject node) {
		node.addProperty("runCount", bson.getInteger("runCount"));
		node.addProperty("runLast", bson.getLong("runLast"));
	}

	@Override
	public void updateMetadata(File file, String extension, Document bson) throws Exception {
		// Pour le moment, on profite de la mise à jour des méta-données pour vider les stats de l'application
		bson.put("runCount", 0);
		bson.put("runLast", 0L);
	}

}

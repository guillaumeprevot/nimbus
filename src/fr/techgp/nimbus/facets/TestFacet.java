package fr.techgp.nimbus.facets;

import java.io.File;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;

public class TestFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "test".equals(extension);
	}

	@Override
	public void loadMetadata(Metadatas metadatas, JsonObject node) {
		// server-side properties set in "TestFacet.updateMetadata"
		node.addProperty("testserver", metadatas.getInteger("testserver"));
		// client-side properties set in "Items.metadata"
		node.addProperty("testboolean", metadatas.getBoolean("testboolean"));
		node.addProperty("testinteger", metadatas.getInteger("testinteger"));
		node.addProperty("testlong", metadatas.getLong("testlong"));
		node.addProperty("testdouble", metadatas.getDouble("testdouble"));
		node.addProperty("teststring", metadatas.getString("teststring"));
	}

	@Override
	public void updateMetadata(File file, String extension, Metadatas metadatas) throws Exception {
		metadatas.put("testserver", 42);
	}

}

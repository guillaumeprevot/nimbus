package fr.techgp.nimbus.facets;

import java.io.File;
import java.util.Date;
import java.util.Optional;

import org.bson.Document;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;

public class TestFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "test".equals(extension);
	}

	@Override
	public void loadMetadata(Document bson, JsonObject node) {
		// server-side properties set in "TestFacet.updateMetadata"
		node.addProperty("testserver", bson.getInteger("testserver"));
		// client-side properties set in "Items.metadata"
		node.addProperty("testboolean", bson.getBoolean("testboolean"));
		node.addProperty("testinteger", bson.getInteger("testinteger"));
		node.addProperty("testlong", bson.getLong("testlong"));
		node.addProperty("testdouble", bson.getDouble("testdouble"));
		node.addProperty("testdatetime", Optional.ofNullable(bson.getDate("testdatetime")).map(Date::getTime).orElse(null));
		node.addProperty("teststring", bson.getString("teststring"));
	}

	@Override
	public void updateMetadata(File file, String extension, Document bson) throws Exception {
		bson.put("testserver", 42);
	}

}

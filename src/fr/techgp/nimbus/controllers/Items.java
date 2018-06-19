package fr.techgp.nimbus.controllers;

import org.apache.commons.io.FilenameUtils;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.utils.SparkUtils;
import spark.Route;

public class Items extends Controller {

	/**
	 * Renvoie en JSON les infos sur un élément dont on donne l'identifiant par ":itemId".
	 *
	 * (itemId) => JSON
	 */
	public static final Route info = (request, response) -> {
		return actionOnSingleItem(request, request.params(":itemId"), (item) -> {
			// Retourner l'élément en JSON
			return SparkUtils.renderJSON(response, asJSON(item));
		});
	};

	/**
	 * Cette méthode encode un élément "item" en un objet JSON pour être renvoyé côté client contenant :
	 * <ul>
	 * <li>les propriétés générales (id, parentId, path, folder?, name, createDate, updateDate, tags)
	 * <li>les propriétés des dossiers si c'est un dossier (itemCount, iconURL)
	 * <li>les propriétés des fichiers si c'est un fichier (mimetype, length)
	 * <li>les propriétés des fichiers génées par les "Facet" si c'est un fichier image, video, audio, ...
	 *</ul>
	 *
	 * @param item
	 * @return le JsonObject associé à l'élément
	 */
	private static final JsonObject asJSON(Item item) {
		JsonObject node = new JsonObject();
		node.addProperty("id", item.id);
		node.addProperty("parentId", item.parentId);
		node.addProperty("path", item.path);
		// node.addProperty("userLogin", item.userLogin);
		node.addProperty("folder", item.folder);
		node.addProperty("name", item.name);
		node.addProperty("createDate", item.createDate.getTime());
		node.addProperty("updateDate", item.updateDate.getTime());
		if (item.tags != null && !item.tags.isEmpty())
			node.addProperty("tags", String.join(",", item.tags));
		try {
			if (item.folder) {
				node.addProperty("itemCount", item.content.getInteger("itemCount"));
				node.addProperty("iconURL", item.content.getString("iconURL"));
			} else {
				String extension = FilenameUtils.getExtension(item.name).toLowerCase();
				node.addProperty("mimetype", configuration.getMimeType(extension, null));
				node.addProperty("length", item.content.getLong("length"));
				for (Facet facet : configuration.getFacets()) {
					if (facet.supports(extension)) {
						facet.loadMetadata(item.content, node);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return node;
	}

}

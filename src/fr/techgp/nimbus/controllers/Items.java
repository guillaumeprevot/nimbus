package fr.techgp.nimbus.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.utils.SparkUtils;
import spark.Route;

public class Items extends Controller {

	/**
	 * Renvoie en JSON les élements de l'utilisateurs correspondant aux critères données.
	 *
	 * (parentId, recursive, sortBy, sortAscending, searchBy, searchText, folders, deleted, extensions) => JSON
	 */
	public static final Route list = (request, response) -> {
		// Récupérer l'utilisateur connecté
		String userLogin = request.session().attribute("userLogin");

		// Extraire la requête
		Long parentId = SparkUtils.queryParamLong(request, "parentId", null);
		boolean recursive = SparkUtils.queryParamBoolean(request, "recursive", false);
		String sortBy = request.queryParams("sortBy");
		boolean sortAscending = SparkUtils.queryParamBoolean(request, "sortAscending", false);
		String searchBy = request.queryParams("searchBy");
		String searchText = SparkUtils.queryParamString(request, "searchText", "").toLowerCase();
		Boolean folders = SparkUtils.queryParamBoolean(request, "folders", null); // true/false/null
		Boolean deleted = SparkUtils.queryParamBoolean(request, "deleted", null); // true/false/null
		String extensions = request.queryParams("extensions");
		boolean pretty = SparkUtils.queryParamBoolean(request, "pretty", false);

		// Vérifier l'accès à l'élément racine
		if (parentId != null && !Item.hasItem(userLogin, parentId))
			return SparkUtils.haltBadRequest();

		// Récupérer les éléments
		List<Item> items = Item.findAll(userLogin, parentId, recursive, sortBy, sortAscending, searchBy, searchText, folders, deleted, extensions);
		List<Item> folderItems = items.stream().filter((i) -> i.folder).collect(Collectors.toList());
		List<Item> fileItems = items.stream().filter((i) -> !i.folder).collect(Collectors.toList());
		items = new ArrayList<>();
		items.addAll(folderItems);
		items.addAll(fileItems);

		// Retourner la liste en un document JSON
		if (pretty) {
			response.type("application/json");
			JsonArray a = items.stream().map(Items::asJSON).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
			return new GsonBuilder().setPrettyPrinting().create().toJson(a);
		}
		return SparkUtils.renderJSONCollection(response, items, Items::asJSON);
	};

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
	 * <li>les propriétés générales (id, parentId, path, folder?, name, createDate, updateDate, deleteDate, tags)
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
		if (item.deleteDate != null)
			node.addProperty("deleteDate", item.deleteDate.getTime());
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

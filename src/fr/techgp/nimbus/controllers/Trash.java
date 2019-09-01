package fr.techgp.nimbus.controllers;

import java.util.Arrays;
import java.util.List;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.utils.SparkUtils;
import spark.Route;
import spark.utils.StringUtils;

public class Trash extends Controller {

	/**
	 * Cette route retourne la page d'accès à la corbeille.
	 * 
	 * () => HTML
	 */
	public static final Route page = (request, response) -> {
		return renderTemplate("trash.html",
				"fromUrl", request.headers("Referer"),
				"lang", SparkUtils.getRequestLang(request));
	};

	/**
	 * Renvoie en texte brut le nombre d'élements dans la corbeille, pour savoir si elle est vide par exemple
	 *
	 * () => int
	 *
	 * @see Item#trashCount(String)
	 */
	public static final Route count = (request, response) -> {
		// Récupérer l'utilisateur connecté
		String userLogin = request.session().attribute("userLogin");
		// Retourner le nombre d'éléments dans la corbeille de l'utilisateur
		return Integer.toString(Item.trashCount(userLogin));
	};

	/**
	 * Renvoie en JSON la liste des élements dans la corbeille
	 *
	 * () => JSON
	 *
	 * @see Item
	 */
	public static final Route items = (request, response) -> {
		// Récupérer l'utilisateur connecté
		String userLogin = request.session().attribute("userLogin");
		// Récupérer les éléments
		List<Item> items = Item.findAll(userLogin, null, true, null, true, null, null, null, null, true, null);
		// Retourner la liste en un document JSON
		if (SparkUtils.queryParamBoolean(request, "pretty", false)) {
			response.type("application/json");
			JsonArray a = items.stream().map(Trash::asJSON).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
			return new GsonBuilder().setPrettyPrinting().create().toJson(a);
		}
		return SparkUtils.renderJSONCollection(response, items, Trash::asJSON);
	};

	/**
	 * Envoie les éléments "itemIds" dans la corbeille
	 *
	 * (itemIds) => void
	 *
	 * @see Item#delete(Item)
	 * @see Item#notifyFolderContentChanged(Long, int)
	 */
	public static final Route delete = (request, response) -> {
		// Extraire la requête
		String itemIds = request.queryParams("itemIds");
		// Parcourir chaque élément pour le supprimer
		return actionOnMultipleItems(request, itemIds, Item::delete);
	};

	/**
	 * Restaure les éléments "itemIds" précédemment envoyés dans la corbeille
	 *
	 * (itemIds) => void
	 *
	 * @see Item#restore(Item)
	 * @see Item#notifyFolderContentChanged(Long, int)
	 */
	public static final Route restore = (request, response) -> {
		// Extraire la requête
		String itemIds = request.queryParams("itemIds");
		// Parcourir chaque élément pour le restaurer
		return actionOnMultipleItems(request, itemIds, Item::restore);
	};

	/**
	 * Supprime définitivement les éléments "itemIds" précédemment envoyés dans la corbeille
	 *
	 * (itemIds) => void
	 *
	 * @see Item#erase(Item)
	 */
	public static final Route erase = (request, response) -> {
		// Extraire la requête
		String itemIds = request.queryParams("itemIds");
		// Parcourir chaque élément
		return actionOnMultipleItems(request, itemIds, (item) -> {
			// ... pour l'effacer définitivement
			if (item.folder) {
				// Pour les dossiers, supprimer récursivement
				// L'important ici est le paramètre "recursive=true" pour récupèrer toute la descendance en une boucle
				List<Item> children = Item.findAll(item.userLogin, item.id, true, null, true, null, null, null, null, null, null);
				for (Item child : children) {
					if (! child.folder)
						Controller.getFile(child).delete();
					Item.erase(child);
				}
			} else {
				// Pour les fichiers, supprimer le fichier associé
				Controller.getFile(item).delete();
			}
			// Supprimer définitivement en base
			Item.erase(item);
		});
	};

	/**
	 * Cette méthode encode un élément "item" en un objet JSON pour être renvoyé côté client contenant :
	 *
	 * @param item
	 * @return le JsonObject associé à l'élément
	 */
	private static final JsonObject asJSON(Item item) {
		JsonObject node = new JsonObject();
		node.addProperty("id", item.id);
		node.addProperty("parentId", item.parentId);
		if (StringUtils.isBlank(item.path))
			node.addProperty("path", "");
		else {
			// TODO Améliorer les performances dans Trash.asJSON
			String[] path = item.path.substring(0, item.path.length() - 1).split(",");
			node.addProperty("path", Arrays.stream(path).map(s -> Item.findById(Long.valueOf(s)).name).reduce((s1, s2) -> s1 + "," + s2).orElse(""));
		}
		node.addProperty("name", item.name);
		node.addProperty("createDate", item.createDate.getTime());
		node.addProperty("deleteDate", item.deleteDate.getTime());
		node.addProperty("length", item.content.getLong("length"));
		return node;
	}

}

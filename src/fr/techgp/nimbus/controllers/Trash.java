package fr.techgp.nimbus.controllers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Route;
import fr.techgp.nimbus.utils.StringUtils;

public class Trash extends Controller {

	/**
	 * Cette route retourne la page d'accès à la corbeille.
	 *
	 * () => HTML
	 */
	public static final Route page = (request, response) -> {
		return Templates.render(request, "trash.html",
				"fromUrl", request.header("Referer"));
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
		return Render.string(Integer.toString(Item.trashCount(userLogin)));
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
		// Récupérer la liste des ids des différents 'path', qu'il va falloir transformer en noms pour l'utilisateur
		HashSet<Long> pathIds = new HashSet<>();
		for (Item item : items) {
			if (StringUtils.isNotBlank(item.path)) {
				Arrays.stream(item.path.substring(0, item.path.length() - 1).split(",")).map(Long::valueOf).forEach(pathIds::add);
			}
		}
		// Récupérer la map associant les noms aux ids
		HashMap<Long, String> names = new HashMap<>();
		for (Item item : Item.findByIds(pathIds)) {
			names.put(item.id, item.name);
		}
		// Retourner la liste en un document JSON
		return Render.json(items, (item) -> {
			JsonObject node = new JsonObject();
			node.addProperty("id", item.id);
			node.addProperty("parentId", item.parentId);
			node.addProperty("name", item.name);
			node.addProperty("createDate", item.createDate.getTime());
			node.addProperty("deleteDate", item.deleteDate.getTime());
			node.addProperty("length", item.content.getLong("length"));
			if (StringUtils.isNotBlank(item.path)) {
				String[] path = item.path.substring(0, item.path.length() - 1).split(",");
				node.addProperty("path", Arrays.stream(path).map(Long::valueOf).map(names::get).collect(Collectors.joining("/")));
			}
			return node;
		});
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
		String itemIds = request.queryParameter("itemIds");
		// Récupérer les éléments
		List<Item> items = loadMultipleItems(request, itemIds);
		if (items == null)
			return Render.badRequest();
		// Supprimer les éléments demandés
		items.stream().forEach(Item::delete);
		// Envoyer une réponse OK vide
		return Render.EMPTY;
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
		String itemIds = request.queryParameter("itemIds");
		// Récupérer les éléments
		List<Item> items = loadMultipleItems(request, itemIds);
		if (items == null)
			return Render.badRequest();
		// Restaurer les éléments demandés
		items.stream().forEach(Item::restore);
		// Envoyer une réponse OK vide
		return Render.EMPTY;
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
		String itemIds = request.queryParameter("itemIds");
		// Récupérer les éléments
		List<Item> items = loadMultipleItems(request, itemIds);
		if (items == null)
			return Render.badRequest();
		// Parcourir les éléments demandés
		for (Item item : items) {
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
		}
		// Envoyer une réponse OK vide
		return Render.EMPTY;
	};

}

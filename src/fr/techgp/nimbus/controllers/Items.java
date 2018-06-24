package fr.techgp.nimbus.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.utils.SparkUtils;
import fr.techgp.nimbus.utils.StringUtils;
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
	 * Renvoie "true" ou "false" en texte brut pour indiquer si un fichier de "parentId" existe déjà avec un des noms de "names"
	 *
	 * (parentId, names[]) => "true"/"false"
	 */
	public static final Route exists = (request, response) -> {
		// Récupérer l'utilisateur connecté
		String userLogin = request.session().attribute("userLogin");
		// Extraire la requête
		Long parentId = SparkUtils.queryParamLong(request, "parentId", null);
		String[] filenames = request.queryParamsValues("names[]");
		// Vérifier si la racine demandée est accessible à l'utilisateur
		if (parentId != null) {
			Item parent = Item.findById(parentId);
			if (parent == null || !parent.userLogin.equals(userLogin))
				return SparkUtils.haltBadRequest();
		}
		// Tester si un élément existe déjà avec l'un des noms demandés
		boolean result = Item.hasItemsWithNames(userLogin, parentId, filenames);
		return Boolean.toString(result);
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
	 * Renvoie en JSON la liste des tags de l'utilisateur, correspondant éventuellement à la recherche donnée par "term".
	 *
	 * (term) => [{label, value}, ...]
	 */
	public static final Route tags = (request, response) -> {
		// Récupérer l'utilisateur connecté
		String userLogin = request.session().attribute("userLogin");
		// Extraire de la requête le texte à rechercher dans les tags (NB .toLowerCase())
		String term = SparkUtils.queryParamString(request, "term", "").toLowerCase();
		// Retourner en JSON les tags sous la forme d'objets { label: tag, value: tag }
		JsonArray results = new JsonArray();
		Item.forEachTag(userLogin, (tag) -> {
			if ("*".equals(term) || tag.toLowerCase().contains(term)) {
				JsonObject o = new JsonObject();
				o.addProperty("label", tag);
				o.addProperty("value", tag);
				results.add(o);
			}
		});
		return SparkUtils.renderJSON(response, results);
	};

	/**
	 * Ajoute un nouveau dossier de nom "name" dans le dossier "parentId" (facultatif)
	 *
	 * (name, parentId) => nouvelId
	 */
	public static final Route addFolder = (request, response) -> {
		// Récupérer l'utilisateur connecté
		String userLogin = request.session().attribute("userLogin");
		// Extraire la requête
		String name = request.queryParams("name");
		Long parentId = SparkUtils.queryParamLong(request, "parentId", null);
		// Ajouter un dossier dans le dossier demandé avec le nom donné
		Item item = Item.add(userLogin, parentId, true, name, null);
		if (item == null)
			return SparkUtils.haltBadRequest();
		// Retourner son id
		return item.id.toString();
	};

	/**
	 * Ajoute un nouvel élément en dupliquant l'élément "itemId".
	 * - dans le cas d'un fichier, duplique le fichier
	 * - dans le cas d'un dossier, les sous-éléments NE sont PAS dupliqués
	 *
	 * (itemId, name) => nouvelId
	 */
	public static final Route duplicate = (request, response) -> {
		String newName = request.queryParams("name");
		if (StringUtils.isBlank(newName))
			return SparkUtils.haltBadRequest();
		return actionOnSingleItem(request, request.queryParams("itemId"), (source) -> {
			// Vérifier que le nom choisi pour la copie est correct
			if (Item.hasItemsWithNames(source.userLogin, source.parentId, newName))
				return SparkUtils.haltConflict();
			// Vérification des quotas d'espace disque
			if (!source.folder)
				checkQuotaAndHaltIfNecessary(source.userLogin, source.content.getLong("length"));

			// Dupliquer l'élément
			Item item = Item.duplicate(source, newName);
			// Copier le fichier
			if (! item.folder) {
				File sourceFile = getFile(source);
				File newFile = getFile(item);
				if (sourceFile.exists()) {
					try {
						FileUtils.copyFile(sourceFile, newFile);
					} catch (IOException ex) {
						//
					}
				}
			}
			// Retourner l'id de l'élément ainsi créé
			return item.id.toString();
		});
	};

	/**
	 * Renomme un élément "itemId", et en même temp ses tags "tags" et/ou URL "iconURL" pour les miniatures
	 *
	 * (itemId, name, tags, iconURL) =>
	 */
	public static final Route rename = (request, response) -> {
		return actionOnSingleItem(request, request.queryParams("itemId"), (item) -> {
			// Extraire la requête
			String name = request.queryParams("name");
			String tags = request.queryParams("tags");
			String iconURL = request.queryParams("iconURL");
			// On met à jour l'élément
			item.name = name;
			item.tags = StringUtils.isBlank(tags) ? null : Arrays.asList(tags.split(","));
			if (StringUtils.isBlank(iconURL)) {
				item.content.remove("iconURL");
			} else {
				item.content.put("iconURL", iconURL);
			}
			// pas de changement de updateDate car le contenu reste le même
			//item.updateDate = new Date();
			Item.update(item);

			// Ajuster la date de modification du dossier parent
			if (item.parentId != null)
				Item.notifyFolderContentChanged(item.parentId, 0);
			return "";
		});
	};

	/**
	 * Déplace les éléments "itemIds" vers un dossier "targetParentId" ou à la racine si "targetParentId" n'est pas précisé.
	 *
	 * (itemIds, targetParentId) => ""
	 */
	public static final Route move = (request, response) -> {
		// Récupérer l'utilisateur connecté
		String userLogin = request.session().attribute("userLogin");
		// Extraire la requête
		String itemIds = request.queryParams("itemIds");
		Long targetParentId = SparkUtils.queryParamLong(request, "targetParentId", null);
		// Récupérer et vérifier l'accès à la cible
		Item targetParent = null;
		if (targetParentId != null) {
			targetParent = Item.findById(targetParentId);
			if (targetParent == null || !targetParent.userLogin.equals(userLogin))
				return SparkUtils.haltBadRequest();
		}
		// Le chemin concaténé jusqu'à la cible
		String path = targetParent == null ? "" : (targetParent.path + targetParent.id + ",");
		// Parcourir chaque élément
		return actionOnMultipleItems(request, itemIds, (item) -> {
			// Rien à faire si l'élément est déjà dans la cible
			if (item.parentId == targetParentId)
				return;
			// Erreur si on tente de déplacer un élément dans lui-même ou un de ses descendants
			if (path.startsWith(item.path + item.id + ","))
				return;
			// Retirer l'élément de son ancien parent
			if (item.parentId != null)
				Item.notifyFolderContentChanged(item.parentId, -1);
			item.parentId = targetParentId;
			/*
			 * Avant de sauvegarder le nouveau chemin pour "item", il faut mettre à jour le chemin pour tous les
			 * sous-éléments de "item". Hors, MongoDB ne nous permet pas de le faire de manière atomique. On ne met donc
			 * à jour le chemin qu'une fois tous les sous-éléments déplacés. Ainsi, en cas de souci, on peut reprendre
			 * l'opération en cours (parentId !== path signifie qu'un déplacement est en cours). Pour plus d'info,
			 * regarder le fonctionnement de Item.updatePath.
			 */
			// item.path = path;

			// pas de changement de updateDate car le contenu reste le même
			// item.updateDate = new Date();
			Item.update(item);

			// Ajouter l'élément dans son nouveau parent
			if (item.parentId != null)
				Item.notifyFolderContentChanged(item.parentId, 1);
			Item.updatePath(item.path, path, item.id);
		});
	};

	/**
	 * Retourne un fichier zip contenant les éléments "itemIds" et leurs éventuels sous-éléments.
	 *
	 * (itemIds) => cloud.zip
	 */
	public static final Route zip = (request, response) -> {
		// Use temp zip file
		File file = File.createTempFile("cloud", null);
		// Generate file
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
			zos.setLevel(Deflater.DEFAULT_COMPRESSION);
			// Extraire la requête
			String itemIds = request.queryParams("itemIds");
			// Parcourir chaque élément
			actionOnMultipleItems(request, itemIds, (item) -> {
				// ... pour l'ajouter au zip définitivement
				try {
					zipItem(item, null, zos);
				} catch (Exception ex) {
					ex.printStackTrace();
					throw new RuntimeException(ex);
				}
			});
		}
		// Retourner le fichier zip
		return SparkUtils.renderFile(response, "application/zip", file, "cloud.zip");
	};

	/**
	 * Cette méthode ajoute dans le zip "zos" une sous-entrée de "path" pour l'élément "item" :
	 * - si "item" est un fichier, ajoute le fichier associé au zip
	 * - si "item" est un dossier, ajoute récursivement le contenu du dossier
	 *
	 * @param item l'élément à ajouter dans le zip
	 * @param path le chemin du dossier parent de "item"
	 * @param zos le flux dans lequel générer les entrées du zip
	 * @throws IOException en cas d'erreur
	 */
	private static final void zipItem(Item item, String path, ZipOutputStream zos) throws IOException {
		String itemPath = path == null ? item.name : (path + "/" + item.name);
		if (item.folder) {
			// Ajouter récursivement les éléments à l'archive
			List<Item> children = Item.findAll(item.userLogin, item.id, false, null, true, null, null, null, false, null);
			for (Item child : children) {
				zipItem(child, itemPath, zos);
			}
		} else {
			// Ajouter une entrée dans le zip pour les fichiers
			zos.putNextEntry(new ZipEntry(itemPath));
			File file = getFile(item);
			if (file.exists())
				FileUtils.copyFile(file, zos);
			zos.closeEntry();
		}
	}

	/**
	 * Cette méthode encode un élément "item" en un objet JSON pour être renvoyé côté client contenant :
	 * <ul>
	 * <li>les propriétés générales (id, parentId, path, folder?, name, createDate, updateDate, deleteDate, tags)
	 * <li>les propriétés des dossiers si c'est un dossier (itemCount, iconURL)
	 * <li>les propriétés des fichiers si c'est un fichier (mimetype, length)
	 * <li>les propriétés des fichiers gérées par les "Facet" si c'est un fichier image, video, audio, ...
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

package fr.techgp.nimbus.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.models.User;
import fr.techgp.nimbus.server.Route;
import fr.techgp.nimbus.utils.SparkUtils;
import fr.techgp.nimbus.utils.StringUtils;
import fr.techgp.nimbus.utils.WebUtils;

public class Items extends Controller {

	/**
	 * Cette méthode renvoie les informations liées à l'ocupation de l'espace disponible
	 * pour l'utilisateur :
	 * - maxSpace : espace total disponible pour l'utilisateur (= quota si défini, en fonction du volume sinon)
	 * - usedSpace : espace actuellement occupé par l'utilisateur (= somme des tailles de ses fichiers)
	 * - freeSpace : expace libre pour l'utilisateur (max - used)
	 * - clientQuotaWarning : pourcentage d'occupation du quota à partir duquel la barre apparait en avertissement
	 * - clientQuotaDanger : pourcentage d'occupation du quota à partir duquel la barre apparait en danger
	 */
	public static final Route quota = (request, response) -> {
		// Récupérer l'utilisateur connecté
		String userLogin = request.session().attribute("userLogin");
		User user = User.findByLogin(userLogin);
		// Récupération des quotas de l'utilisateur
		long usedSpace = Item.calculateUsedSpace(userLogin);
		long maxSpace, freeSpace;
		if (user.quota == null) {
			freeSpace = configuration.getStorageFolder().getFreeSpace();
			maxSpace = freeSpace + usedSpace;
		} else {
			maxSpace = (user.quota.longValue() * 1024L * 1024L);
			freeSpace = Math.min(configuration.getStorageFolder().getFreeSpace(), maxSpace - usedSpace);
		}
		int clientQuotaWarning = configuration.getClientQuotaWarning();
		int clientQuotaDanger = configuration.getClientQuotaDanger();
		// Renvoyer les informations en JSON
		JsonObject result = new JsonObject();
		result.addProperty("freeSpace", freeSpace);
		result.addProperty("usedSpace", usedSpace);
		result.addProperty("maxSpace", maxSpace);
		result.addProperty("clientQuotaWarning", clientQuotaWarning);
		result.addProperty("clientQuotaDanger", clientQuotaDanger);
		return SparkUtils.renderJSON(response, result);
	};

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
		Boolean hidden = SparkUtils.queryParamBoolean(request, "hidden", null); // true/false/null
		Boolean deleted = SparkUtils.queryParamBoolean(request, "deleted", null); // true/false/null
		String extensions = request.queryParams("extensions");

		// Vérifier l'accès à l'élément racine
		if (parentId != null && !Item.hasItem(userLogin, parentId))
			return SparkUtils.haltBadRequest();

		// Récupérer les éléments
		List<Item> items = Item.findAll(userLogin, parentId, recursive, sortBy, sortAscending, searchBy, searchText, folders, hidden, deleted, extensions);
		List<Item> folderItems = items.stream().filter((i) -> i.folder).collect(Collectors.toList());
		List<Item> fileItems = items.stream().filter((i) -> !i.folder).collect(Collectors.toList());
		items = new ArrayList<>();
		items.addAll(folderItems);
		items.addAll(fileItems);

		// Retourner la liste en un document JSON
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
	 * Renvoie en JSON les infos sur les éléments indiqués par "itemIds".
	 *
	 * (itemId) => JSON
	 */
	public static final Route infos = (request, response) -> {
		// Extraire la requête
		String itemIds = request.queryParams("itemIds");
		// Préparer le résultat
		JsonArray results = new JsonArray();
		// Parcourir chaque élément
		actionOnMultipleItems(request, itemIds, (item) -> {
			results.add(asJSON(item));
		});
		// Retourner la liste en JSON
		return SparkUtils.renderJSON(response, results);
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
			if (StringUtils.isNotBlank(tag) && ("".equals(term) || tag.toLowerCase().contains(term)))
				results.add(tag);
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
		// Vérifier l'unicité des noms
		if (Item.hasItemWithName(userLogin, parentId, name))
			return SparkUtils.haltConflict();
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
	 * Le nom de la copie peut, au choix :
	 * - être fourni via le paramètre "name"
	 * - être calculé via les paramètres "firstPattern" et "nextPattern"
	 *
	 * (itemId, name) => nouvelId
	 */
	public static final Route duplicate = (request, response) -> {
		// Vérifier qu'un nom ou un couple de pattern est proposé
		String newName = request.queryParams("name");
		String firstPattern = request.queryParams("firstPattern");
		String nextPattern = request.queryParams("nextPattern");
		if (StringUtils.isBlank(newName) && (StringUtils.isBlank(firstPattern) || StringUtils.isBlank(nextPattern)))
			return SparkUtils.haltBadRequest();
		return actionOnSingleItem(request, request.queryParams("itemId"), (source) -> {
			// Vérifier que le nom proposé pour la copie est correct
			if (StringUtils.isNotBlank(newName) && Item.hasItemWithName(source.userLogin, source.parentId, newName))
				return SparkUtils.haltConflict();

			// Vérification des quotas d'espace disque
			if (!source.folder)
				checkQuotaAndHaltIfNecessary(source.userLogin, source.content.getLong("length"));

			// Trouver un nom unique pour la copie, si le nom n'est pas fourni
			String duplicateName = StringUtils.isNotBlank(newName) ? newName : Item.findName(source, firstPattern, nextPattern);
			// Dupliquer l'élément
			Item item = Item.duplicate(source, duplicateName);
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
			// Vérifier que le nom choisi pour la copie est correct
			Item existing = Item.findItemWithName(item.userLogin, item.parentId, name);
			if (existing != null && !existing.id.equals(item.id))
				return SparkUtils.haltConflict();
			// On met à jour l'élément
			item.name = name;
			item.tags = StringUtils.isBlank(tags) ? null : Arrays.asList(tags.split(","));
			item.content.remove("iconURLCache");
			if (StringUtils.isBlank(iconURL)) {
				item.content.remove("iconURL");
			} else {
				item.content.put("iconURL", iconURL);
				// Mise en cache des URLs externes pour ne pas "pinger" à chaque affichage
				if (iconURL.toLowerCase().startsWith("http") && !iconURL.toLowerCase().startsWith(configuration.getServerAbsoluteUrl())) {
					String defaultMimetype = configuration.getMimeTypeByFileName(item.name);
					String dataURL = WebUtils.downloadURLAsDataUrl(iconURL, defaultMimetype);
					if (dataURL != null)
						item.content.put("iconURLCache", dataURL);
				}
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
	 * Active ou désactive le statut "masqué" de l'élément "itemId"
	 *
	 * (itemId, hidden) =>
	 */
	public static final Route hide = (request, response) -> {
		return actionOnSingleItem(request, request.queryParams("itemId"), (item) -> {
			// Extraire la requête
			boolean hidden = SparkUtils.queryParamBoolean(request, "hidden", true);
			// On met à jour l'élément
			item.hidden = hidden;
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
	 * Met à jour (récursivement) les méta-données de l'élément "itemId" gérées par les Facets.
	 *
	 * (itemId) => ""
	 */
	public static final Route refresh = (request, response) -> {
		return actionOnSingleItem(request, request.queryParams("itemId"), (item) -> {
			if (!item.folder) {
				// On recalcule et on sauvegarde les méta-données
				updateFile(item, null, true);
			} else {
				// On récupère le contenu du dossier RECURSIVEMENT
				List<Item> children = Item.findAll(item.userLogin, item.id, true, null, true, null, null, null, null, false, null);
				// On prépare une map des dossiers, pour compter les éléments que chacun contient
				Map<Long, Integer> itemCountByFolderId = new HashMap<>();
				// Premier parcours = MAJ des fichiers + comptage des éléments dans chaque dossier
				for (Item child : children) {
					if (!child.folder) {
						// On recalcule et on sauvegarde les méta-données
						updateFile(child, null, true);
					}
					// On comptabilise le nombre d'éléments
					Integer itemCount = itemCountByFolderId.getOrDefault(child.parentId, 0);
					itemCountByFolderId.put(child.parentId, itemCount + 1);
				}
				// Second parcours = sauvegarde du nombre d'éléments dans chaque dossier
				for (Item child : children) {
					if (child.folder) {
						// On sauvegarde le nombre d'éléments dans chaque sous-dossier
						Integer itemCount = itemCountByFolderId.getOrDefault(child.id, 0);
						child.content.put("itemCount", itemCount);
						Item.update(item);
					}
				}
				// Enfin, on sauvegarde le nombre d'éléments dans le dossier de départ
				Integer itemCount = itemCountByFolderId.getOrDefault(item.id, 0);
				item.content.put("itemCount", itemCount);
				Item.update(item);
			}

			// Ajuster la date de modification du dossier parent
			if (item.parentId != null)
				Item.notifyFolderContentChanged(item.parentId, 0);
			return "";
		});
	};

	/**
	 * Met à jour les méta-données de l'élément "itemId" gérées côté client.
	 *
	 * Chaque entrée de méta-donnée se compose des propriétés suivantes :
	 * - "name", le nom de la propriété
	 * - "action", l'action à effectuer ("set", "remove")
	 * - "value", la valeur de la popriété, dans le cas où "action" est égal à "set"
	 * - "type", le type de la propriété ("boolean", "integer", "long", "double", "datetime" ou "string")
	 *
	 * (itemId, metadata[]) => ""
	 */
	public static final Route metadata = (request, response) -> {
		return actionOnSingleItem(request, request.queryParams("itemId"), (item) -> {
			// Extraire la requête
			String json = request.queryParams("metadata");
			JsonElement element = JsonParser.parseString(json);
			if (!element.isJsonArray())
				return SparkUtils.haltBadRequest();
			// Via la liste "metadata", les méta-données seront ajustées comme demandées côté client
			JsonArray array = element.getAsJsonArray();
			for (int i = 0; i < array.size(); i++) {
				element = array.get(i);
				if (!element.isJsonObject())
					return SparkUtils.haltBadRequest();
				JsonObject object = element.getAsJsonObject();
				String name = object.get("name").getAsString();
				String action = object.get("action").getAsString();
				if ("remove".equals(action))
					item.content.remove(name);
				else {
					JsonElement valueElement = object.get("value");
					if (!valueElement.isJsonPrimitive())
						return SparkUtils.haltBadRequest();
					JsonPrimitive valuePrimitive = valueElement.getAsJsonPrimitive();
					Object value;
					String type = object.get("type").getAsString();
					switch (type) {
					case "boolean":
						value = Boolean.valueOf(valuePrimitive.getAsBoolean());
						break;
					case "integer":
						value = Integer.valueOf(valuePrimitive.getAsInt());
						break;
					case "long":
						value = Long.valueOf(valuePrimitive.getAsLong());
						break;
					case "double":
						value = Double.valueOf(valuePrimitive.getAsDouble());
						break;
					case "datetime":
						value = new Date(valuePrimitive.getAsLong());
						break;
					case "string":
					default:
						value = valuePrimitive.getAsString();
						break;
					}
					item.content.put(name, value);
				}
			}

			// pas de changement de updateDate car le contenu reste le même
			//item.updateDate = new Date();
			// On met à jour l'élément
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
		String conflict = SparkUtils.queryParamString(request, "conflict", "skip");
		String firstConflictPattern = SparkUtils.queryParamString(request, "firstConflictPattern", "Conflict of {0}");
		String nextConflictPattern = SparkUtils.queryParamString(request, "nextConflictPattern", "Conflict ({1}) of {0}");
		Long targetParentId = SparkUtils.queryParamLong(request, "targetParentId", null);
		// Récupérer et vérifier l'accès à la cible
		Item targetParent = targetParentId == null ? null : Item.findById(targetParentId);
		if (targetParentId != null && (targetParent == null || !targetParent.userLogin.equals(userLogin)))
			return SparkUtils.haltBadRequest();
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
			// Lancer l'opération pour cet élément
			move(item, targetParent, conflict, firstConflictPattern, nextConflictPattern);
		});
	};

	private static final boolean move(Item item, Item targetParent, String conflict, String firstConflictPattern, String nextConflictPattern) {
		// Vérifier la présence d'un élément portant le même nom dans la destination, qui représenterait un conflit
		Long targetParentId = targetParent == null ? null : targetParent.id;
		Item existingItem = Item.findItemWithName(item.userLogin, targetParentId, item.name);

		// Les conflits entre fichier et dossier ne sont pas gérés. On stoppe l'opération dans ces cas de figure
		if (existingItem != null && (existingItem.folder != item.folder)) {
			SparkUtils.haltConflict();
			return false; // peu importe car haltConflict lance une exception
		}

		if (! item.folder) {

			// Fichier inexistant dans la destination, il suffit de le déplacer
			if (existingItem == null) {
				Item.move(item, targetParent, null);
				return true;
			}
			// Fichier existant dans la destination, il faut résoudre le conflit comme demandé
			boolean done = true;
			switch (conflict) {
			case "keepsource":
				Controller.getFile(existingItem).delete();
				Item.erase(existingItem);
				Item.move(item, targetParent, null);
				break;
			case "keeptarget":
				Controller.getFile(item).delete();
				Item.erase(item);
				break;
			case "renamesource":
				Item.move(item, targetParent, () -> Item.findName(item, firstConflictPattern, nextConflictPattern));
				break;
			case "renametarget":
				Item.rename(existingItem, Item.findName(existingItem, firstConflictPattern, nextConflictPattern));
				Item.move(item, targetParent, null);
				break;
			case "keepnewest":
				if (item.updateDate.after(existingItem.updateDate)) {
					Controller.getFile(existingItem).delete();
					Item.erase(existingItem);
					Item.move(item, targetParent, null);
				} else {
					Controller.getFile(item).delete();
					Item.erase(item);
				}
				break;
			case "skip":
				done = false;
				break;
			case "abort":
				SparkUtils.haltConflict();
				break;
			default:
				SparkUtils.haltBadRequest();
				break;
			}
			return done;

		}

		// Récupérer le contenu du dossier "item"
		List<Item> children = Item.findAll(item.userLogin, item.id, false, null, false, null, null, null, null, null, null);
		// Pour des dossiers vides n'existant pas dans la destination, il suffit de les déplacer
		if (existingItem == null && children.isEmpty()) {
			Item.move(item, targetParent, null);
			return true;
		}
		// Pour les dossiers non vides, on déplacera les sous-éléments un par un dans la destination
		if (existingItem == null) {
			existingItem = Item.add(item.userLogin, targetParent, true, item.name, (i) -> {
				if (item.tags != null) {
					i.tags = new ArrayList<>();
					i.tags.addAll(item.tags);
				}
				i.content.append("iconURL", item.content.getString("iconURL"));
				i.content.append("iconURLCache", item.content.getString("iconURLCache"));
			});
		}
		// Déplacer chaque sous-élément de "item" vers "existingItem"
		boolean allMoved = true;
		for (Item child : children) {
			allMoved &= move(child, existingItem, conflict, firstConflictPattern, nextConflictPattern);
		}
		// Si tous les éléments ont été déplacés, le dossier source est vide et peut être supprimé
		if (allMoved)
			Item.erase(item);
		return allMoved;
	}

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
			List<Item> children = Item.findAll(item.userLogin, item.id, false, null, true, null, null, null, null, false, null);
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
	 * <li>les propriétés générales (id, parentId, path, folder?, hidden?, name, share*, createDate, updateDate, deleteDate, tags)
	 * <li>les propriétés des dossiers si c'est un dossier (itemCount, iconURL, iconURLCache)
	 * <li>les propriétés des fichiers si c'est un fichier (mimetype, length, progress, status, sourceURL)
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
		node.addProperty("hidden", item.hidden);
		node.addProperty("name", item.name);
		if (StringUtils.isNotBlank(item.sharedPassword))
			node.addProperty("sharedPassword", item.sharedPassword);
		if (item.sharedDate != null)
			node.addProperty("sharedDate", item.sharedDate.getTime());
		if (item.sharedDuration != null)
			node.addProperty("sharedDuration", item.sharedDuration);
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
				node.addProperty("iconURLCache", item.content.getString("iconURLCache"));
			} else {
				String extension = FilenameUtils.getExtension(item.name).toLowerCase();
				node.addProperty("mimetype", configuration.getMimeType(extension));
				node.addProperty("length", item.content.getLong("length"));
				if (item.content.containsKey("progress"))
					node.addProperty("progress", item.content.getInteger("progress"));
				if (item.content.containsKey("status"))
					node.addProperty("status", item.content.getString("status"));
				if (item.content.containsKey("sourceURL"))
					node.addProperty("sourceURL", item.content.getString("sourceURL"));
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

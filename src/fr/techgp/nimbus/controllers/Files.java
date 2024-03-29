package fr.techgp.nimbus.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.google.gson.JsonArray;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.models.User;
import fr.techgp.nimbus.server.MimeTypes;
import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Response;
import fr.techgp.nimbus.server.Route;
import fr.techgp.nimbus.server.Upload;
import fr.techgp.nimbus.utils.StringUtils;

public class Files extends Controller {

	/**
	 * Ajoute un ou plusieurs fichiers "files" dans le dossier "parentId" (facultatif).
	 * Le formulaire est envoyé en POST et encodé en "multipart/form-data".
	 * L'appelant peut préciser une date qui servira de date de mise à jour pour
	 * l'ensemble des fichiers reçus.
	 *
	 * (files, parentId[, updateDate]) => [id1, id2, ...]
	 */
	public static final Route upload = (request, response) -> {
		// Récupérer l'utilisateur pour connaitre son quota
		String userLogin = getUserLogin(request);
		User user = User.findByLogin(userLogin);

		// Rechercher le dossier parent (où déposer les fichiers) et en vérifier l'accès
		Long parentId = null;
		Upload parentIdUpload = request.upload("parentId");
		if (parentIdUpload != null) {
			try (InputStream is = parentIdUpload.getInputStream()) {
				String parentIdString = IOUtils.toString(is, "UTF-8");
				parentId = (StringUtils.isBlank(parentIdString) || "null".equals(parentIdString)) ? null : Long.valueOf(parentIdString);
			}
		}
		Item parent = parentId == null ? null : Item.findById(parentId);
		if (parentId != null && (parent == null || !parent.userLogin.equals(userLogin))) {
			// Nettoyer les fichiers uploadés qui ont été stockés sur disque
			for (Upload upload : request.uploads()) {
				upload.delete();
			}
			return Render.badRequest();
		}

		// Récupérer les fichiers uploadés, les éventuels items correspondants et l'espace disque nécessaire
		List<Upload> uploads = new ArrayList<>();
		List<Item> items = new ArrayList<>();
		long requiredSpace = 0;
		for (Upload upload : request.uploads("files")) {
			Item item = Item.findItemWithName(userLogin, parentId, upload.fileName());
			uploads.add(upload);
			items.add(item);
			requiredSpace += upload.contentLength();
			if (item != null && item.metadatas.getLong("length") != null)
				requiredSpace -= item.metadatas.getLong("length").longValue();
		}

		// Vérifier avant de commencer que l'espace disque est suffisant
		long availableSpace = user.quota == null ? configuration.getStorageFolder().getFreeSpace() : (user.quota.longValue() * 1024L * 1024L - Item.calculateUsedSpace(userLogin));
		if (availableSpace < requiredSpace) {
			// Nettoyer les fichiers uploadés qui ont été stockés sur disque
			for (Upload upload : request.uploads()) {
				upload.delete();
			}
			return Render.insufficientStorage();
		}

		// OK, lancer l'intégration
		JsonArray results = new JsonArray();
		// Récupérer, si elle est précisée, la date de mise à jour à utiliser pour les fichiers uploadés
		long generalUpdateDate = request.queryParameterLong("updateDate", System.currentTimeMillis());
		for (int i = 0; i < uploads.size(); i++) {
			Upload upload = uploads.get(i);
			Item item = items.get(i);
			// Ajouter l'élément s'il n'existe pas encore
			if (item == null)
				item = Item.add(userLogin, parent, false, upload.fileName(), null);
			// Récupérer, si elle est précisée, la date de mise à jour à utiliser pour ce fichier en particulier
			Date updateDate = new Date(request.queryParameterLong("updateDate" + i, generalUpdateDate));
			// Enregistrer le fichier dans le dossier de stockage
			upload.saveTo(getFile(item));
			// Mettre à jour les infos du fichier et sauvegarder
			updateFile(item, updateDate, true);
			// Renvoyer la liste des ids créés
			results.add(item.id);
		}
		return Render.json(results);
	};

	/**
	 * Met à jour le contenu "file" d'un fichier identifié par son "itemId" (obligatoire).
	 * Le formulaire est envoyé en POST et encodé en "multipart/form-data".
	 * L'appelant peut préciser une date qui servira de date de mise à jour (par exemple
	 * celle du fichier associé sur un disque synchronisé.
	 *
	 * (file, itemId[, updateDate]) => ""
	 */
	public static final Route update = (request, response) -> {
		// Récupérer l'utilisateur connecté
		String userLogin = getUserLogin(request);

		// Rechercher l'élément et en vérifier l'accès
		Item item = Item.findById(Long.valueOf(request.pathParameter(":itemId")));
		if (item == null || !item.userLogin.equals(userLogin))
			return Render.badRequest();

		// Récupérer l'utilisateur pour connaitre son quota
		User user = User.findByLogin(userLogin);
		// Récupérer le fichiers uploadés pour connaitre l'espace disque nécessaire
		Upload upload = request.upload("file");
		if (user.quota != null) {
			// Vérifier que l'espace libre est suffisamment grand pour la différence avant/après
			long availableSpace = user.quota.longValue() * 1024L * 1024L - Item.calculateUsedSpace(userLogin);
			long newSize = upload.contentLength();
			long oldSize = Optional.ofNullable(item.metadatas.getLong("length")).orElse(0L);
			if (availableSpace + oldSize - newSize < 0) {
				upload.delete(); // Nettoyer le fichier uploadé qui a été stocké sur disque
				return Render.insufficientStorage();
			}
		}

		// OK, lancer l'intégration à la date demandée (par défaut, maintenant)
		long updateDate = request.queryParameterLong("updateDate", System.currentTimeMillis());
		// Enregistrer le fichier dans le dossier de stockage
		upload.saveTo(getFile(item));
		// Mettre à jour les infos du fichier et sauvegarder
		updateFile(item, new Date(updateDate), true);
		return Render.EMPTY;
	};

	/**
	 * Ajoute un nouveau fichier vide de nom "name" dans le dossier "parentId" (facultatif)
	 *
	 * (name, parentId) => nouvelId
	 */
	public static final Route touch = (request, response) -> {
		// Récupérer l'utilisateur connecté
		String userLogin = getUserLogin(request);
		// Extraire la requête
		String name = request.queryParameter("name");
		Long parentId = request.queryParameterLong("parentId", null);
		// Vérifier l'unicité des noms
		if (Item.hasItemWithName(userLogin, parentId, name))
			return Render.conflict();
		// Ajouter un fichier vide dans le dossier demandé avec le nom donné
		Item item = Item.add(userLogin, parentId, false, name, null);
		if (item == null)
			return Render.badRequest();
		// Retourner son id
		return Render.string(item.id.toString());
	};

	/**
	 * Retourne le contenu d'un fichier dont le chemin depuis la racine est "*".
	 *
	 * (path) => stream
	 */
	public static final Route browse = (request, response) -> {
		// Récupérer l'utilisateur connecté
		String userLogin = getUserLogin(request);
		// Extraire la requête
		String path = request.path().substring("/files/browse/".length());
		if (StringUtils.isBlank(path))
			return Render.badRequest();
		try {
			path = URLDecoder.decode(path, "UTF-8");
		} catch (Exception ex) {
			// UTF-8 is always here, right ?
		}
		// Récupérer l'élément dont on reçoit le chemin
		Item item = Item.findItemWithPath(userLogin, null, path);
		if (item == null)
			return Render.badRequest();
		// Renvoyer le fichier au bout du chemin
		String mimeType = MimeTypes.byName(item.name);
		return Render.file(getFile(item), mimeType, item.name, false, false);
	};

	/**
	 * Calcule le chemin dans l'arborescence du fichier ":itemId" et redirige
	 * vers l'URL associée "/files/browse/path/to/filename.ext".
	 *
	 * (itemId) => redirect
	 */
	public static final Route browseTo = (request, response) -> {
		return actionOnSingleItem(request, request.pathParameter(":itemId"), (item) -> {
			StringBuilder url = new StringBuilder("/files/browse/");
			if (StringUtils.isNotBlank(item.path)) {
				String[] path = item.path.substring(0, item.path.length() - 1).split(",");
				Arrays.stream(path).map(s -> Item.findById(Long.valueOf(s)).name).forEach((name) -> url.append(name).append('/'));
			}
			url.append(item.name);
			try {
				// Le nom de fichier peut contenir des caractères gênants comme %
				String asciiURL = new URI(null, null, url.toString(), null, null).toASCIIString();
				return Render.redirect(asciiURL);
			} catch (URISyntaxException ex) {
				return Render.badRequest();
			}
		});
	};

	/**
	 * Retourne le contenu d'un fichier ":itemId" en entier.
	 * L'en-tête HTTP "Range", si elle est spécifiée, spécifie la plage du contenu à renvoyer.
	 *
	 * (itemId[, Range]) => stream
	 */
	public static final Route stream = (request, response) -> {
		return actionOnSingleItem(request, request.pathParameter(":itemId"), (item) -> {
			String range = request.header("Range");
			if (range != null && range.startsWith("bytes="))
				return returnFileRange(response, item, range.substring("bytes=".length()));
			String mimeType = MimeTypes.byName(item.name);
			return Render.file(getFile(item), mimeType, item.name, false, false);
		});
	};

	/**
	 * Retourne le contenu d'un fichier ":itemId" afin d'être téléchargé sous le nom "item.name".
	 *
	 * (itemId) => stream
	 */
	public static final Route download = (request, response) -> {
		return actionOnSingleItem(request, request.pathParameter(":itemId"), (item) -> {
			String mimeType = MimeTypes.byName(item.name);
			return Render.file(getFile(item), mimeType, item.name, true, false);
		});
	};

	/**
	 * Retourne le contenu d'un fichier ":itemId" représentant une image, afin d'être téléchargé sous le nom "item.name".
	 * Le paramètre facultatif "size" permet de spécifier la taille de la miniature souhaitée (32 par défaut).
	 *
	 * (itemId) => stream
	 */
	public static final Route thumbnail = (request, response) -> {
		int size = request.queryParameterInteger("size", 32);
		return actionOnSingleItem(request, request.pathParameter(":itemId"), (item) -> {
			File file = getFile(item);
			if (!file.exists())
				// Fichier absent, pas possible de faire une miniature
				return Render.notFound();
			try {
				// L'extension va nous permettre de rechercher comment générer la Facet
				String extension = FilenameUtils.getExtension(item.name).toLowerCase();
				// Si l'une des Facet ait générer la miniature, on connaitre le type MIME et le contenu
				String mimetype = null;
				byte[] thumbnail = null;
				// Parcourir les Facet pour voir si l'une peut nous générer la miniature
				for (Facet facet : configuration.getFacets()) {
					if (facet.supportsThumbnail(extension)) {
						thumbnail = facet.generateThumbnail(file, extension, size, size);
						mimetype = facet.getThumbnailMimeType(extension);
						if (thumbnail != null && mimetype != null)
							return Render.bytes(thumbnail, mimetype, null, false);
					}
				}
				// Si aucune Facet ne peut répondre à la demande de miniature, on indique au client que la demande est incorrecte
				return Render.badRequest();
			} catch (IOException | NoSuchElementException ex) {
				// Si une erreur survient pendant la génération de la miniature, on suppose que c'est parce que la demande était incorrecte
				return Render.badRequest();
			}
		});
	};

	/**
	 * Utilise une miniature de l'élément "itemId" comme icone de son parent, d'une taille "size" de 32 pixels par défaut.
	 *
	 * (itemId[, size]) => ""
	 */
	public static final Route useAsFolderIcon = (request, response) -> {
		int size = request.queryParameterInteger("size", 32);
		return actionOnSingleItem(request, request.pathParameter(":itemId"), (item) -> {
			// Vérifier que l'élément a bien un parent
			if (item.parentId == null)
				return Render.badRequest();
			// Récupérer et vérifier le parent de l'élément
			Item parent = Item.findById(item.parentId);
			if (! parent.folder)
				return Render.badRequest();
			// OK, c'est bon
			parent.metadatas.put("iconURL", "/files/thumbnail/" + item.id + "?size=" + size);
			parent.metadatas.remove("iconURLCache");
			parent.updateDate = new Date();
			Item.update(parent);
			return Render.EMPTY;
		});
	};

	// TODO Gérer l'erreur 416 Range Not Satisfiable
	// TODO Gérer l'en-tête Range multiple, par exemple "0-10, 20-30, 40-50"
	// TODO Déplacer dans un Render réutilisable
	private static final Render returnFileRange(Response response, Item item, String range) {
		File file = getFile(item);
		if (!file.exists())
			return Render.notFound();
		// System.out.println("ByteRange=" + range + " pour " + item.name);
		response.type(MimeTypes.byName(item.name));
		response.header("Content-Disposition", "inline; filename=\"" + item.name + "\"");

		// http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.35
		int indexOfDash = range.indexOf('-');
		long start, end;
		if (indexOfDash == -1) {
			// Example: '500' which means "bytes from 0 to 500 included"
			start = 0;
			end = Long.parseLong(range);
		} else if (indexOfDash == 0) {
			// Example: '-500' which means "the final 500 bytes"
			end = file.length() - 1;
			start = end - Long.parseLong(range.substring(1)) + 1;
		} else if (indexOfDash == range.length() - 1) {
			// Example: '500-' which means 'from 500 included to the end'
			start = Long.parseLong(range.substring(0, indexOfDash));
			end = file.length() - 1;
		} else {
			// Example: '500-600' which means 'from 500 to 600, both included'
			start = Long.parseLong(range.substring(0, indexOfDash));
			end = Long.parseLong(range.substring(indexOfDash + 1));
		}
		response.header("Accept-Ranges", "bytes");
		response.dateHeader("Last-Modified", file.lastModified());
		response.header("Content-Range", "bytes " + start + "-" + end + "/" + file.length());
		response.header("Connection", "keep-alive");
		// ne pas préciser "Content-Length" car doublon avec "Content-Range" et ne fonctionne plus en plus
		// response.header("Content-Length", Long.toString(end - start + 1));
		return (request, r, charset, stream) -> {
			try (FileInputStream fis = new FileInputStream(file)) {
				fis.skip(start);
				response.status(206); // Partial Content*/
				long remaining = end - start + 1;
				byte[] buffer = new byte[1024 * 1024];
				try (OutputStream os = stream.get()) {
					while (remaining > 0) {
						int read = fis.read(buffer, 0, (int) Math.min(remaining, buffer.length));
						os.write(buffer, 0, read);
						remaining -= read;
					}
				}
			}
		};
	}

}

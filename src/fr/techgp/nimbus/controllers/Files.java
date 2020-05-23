package fr.techgp.nimbus.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonArray;

import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.models.User;
import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Response;
import fr.techgp.nimbus.server.Route;
import fr.techgp.nimbus.server.Upload;
import fr.techgp.nimbus.utils.ImageUtils;
import fr.techgp.nimbus.utils.SparkUtils;
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
		String userLogin = request.session().attribute("userLogin");
		User user = User.findByLogin(userLogin);

		// Rechercher le dossier parent (où déposer les fichiers) et en vérifier l'accès
		Long parentId = null;
		for (Upload upload : request.uploads()) {
			if ("parentId".equals(upload.name())) {
				try (InputStream is = upload.getInputStream()) {
					String parentIdString = IOUtils.toString(is, "UTF-8");
					parentId = (StringUtils.isBlank(parentIdString) || "null".equals(parentIdString)) ? null : Long.valueOf(parentIdString);
					break;
				}
			}
		}
		Item parent = parentId == null ? null : Item.findById(parentId);
		if (parentId != null && (parent == null || !parent.userLogin.equals(userLogin))) {
			for (Upload upload : request.uploads()) {
				upload.delete(); // Nettoyer les fichiers uploadés qui ont été stockés sur disque
			}
			return Render.badRequest();
		}

		// Récupérer les fichiers uploadés, les éventuels items correspondants et l'espace disque nécessaire
		List<Upload> uploads = new ArrayList<>();
		List<Item> items = new ArrayList<>();
		long requiredSpace = 0;
		for (Upload upload : request.uploads()) {
			if (!"files".equals(upload.name()))
				continue;
			Item item = Item.findItemWithName(userLogin, parentId, upload.fileName());
			uploads.add(upload);
			items.add(item);
			requiredSpace += upload.contentLength();
			if (item != null && item.content.getLong("length") != null)
				requiredSpace -= item.content.getLong("length").longValue();
		}

		// Vérifier avant de commencer que l'espace disque est suffisant
		long availableSpace = user.quota == null ? configuration.getStorageFolder().getFreeSpace() : (user.quota.longValue() * 1024L * 1024L - Item.calculateUsedSpace(userLogin));
		if (availableSpace < requiredSpace) {
			for (Upload upload : request.uploads()) {
				upload.delete(); // Nettoyer les fichiers uploadés qui ont été stockés sur disque
			}
			return Render.insufficientStorage();
		}

		// OK, lancer l'intégration
		JsonArray results = new JsonArray();
		// Récupérer, si elle est précisée, la date de mise à jour à utiliser pour les fichiers uploadés
		long generalUpdateDate = SparkUtils.queryParamLong(request, "updateDate", System.currentTimeMillis());
		for (int i = 0; i < uploads.size(); i++) {
			Upload upload = uploads.get(i);
			Item item = items.get(i);
			// Ajouter l'élément s'il n'existe pas encore
			if (item == null)
				item = Item.add(userLogin, parent, false, upload.fileName(), null);
			// Récupérer, si elle est précisée, la date de mise à jour à utiliser pour ce fichier en particulier
			Date updateDate = new Date(SparkUtils.queryParamLong(request, "updateDate" + i, generalUpdateDate));
			// Enregistrement sur disque
			updateFileFromFilePart(upload, item, updateDate);
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
		String userLogin = request.session().attribute("userLogin");

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
			long oldSize = Optional.ofNullable(item.content.getLong("length")).orElse(0L);
			if (availableSpace + oldSize - newSize < 0) {
				upload.delete(); // Nettoyer le fichier uploadé qui a été stocké sur disque
				return Render.insufficientStorage();
			}
		}

		// OK, lancer l'intégration à la date demandée (par défaut, maintenant)
		long updateDate = SparkUtils.queryParamLong(request, "updateDate", System.currentTimeMillis());
		updateFileFromFilePart(upload, item, new Date(updateDate));
		return Render.EMPTY;
	};

	/**
	 * Ajoute un nouveau fichier vide de nom "name" dans le dossier "parentId" (facultatif)
	 *
	 * (name, parentId) => nouvelId
	 */
	public static final Route touch = (request, response) -> {
		// Récupérer l'utilisateur connecté
		String userLogin = request.session().attribute("userLogin");
		// Extraire la requête
		String name = request.queryParameter("name");
		Long parentId = SparkUtils.queryParamLong(request, "parentId", null);
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
		String userLogin = request.session().attribute("userLogin");
		// Extraire la requête
		String path = request.path().substring("/files/browse/".length());
		if (StringUtils.isBlank(path))
			return Render.badRequest();
		try {
			path = URLDecoder.decode(path, "UTF-8");
		} catch (Exception ex) {
			// UTF-8 is always here, right ?
		}
		// Parcourir le chemin
		Long currentId = null;
		Item item = null;
		for (String part : path.split("/")) {
			item = Item.findItemWithName(userLogin, currentId, part);
			if (item == null)
				return Render.badRequest();
			currentId = item.id;
		}
		// Vérification
		if (item == null)
			return Render.badRequest();
		// Renvoyer le fichier au bout du chemin
		String mimeType = configuration.getMimeTypeByFileName(item.name);
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
			String mimeType = configuration.getMimeTypeByFileName(item.name);
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
			String mimeType = configuration.getMimeTypeByFileName(item.name);
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
		int size = SparkUtils.queryParamInteger(request, "size", 32);
		return actionOnSingleItem(request, request.pathParameter(":itemId"), (item) -> {
			File file = getFile(item);
			if (!file.exists())
				// Fichier absent, pas possible de faire une miniature
				return Render.notFound();
			try {
				String mimeType = configuration.getMimeTypeByFileName(item.name);
				if (item.name.endsWith(".ico"))
					return Render.bytes(ImageUtils.getScaleICOImage(file, size, size), mimeType, item.name, false);
				return Render.bytes(ImageUtils.getScaleImage(file, size, size), mimeType, item.name, false);
			} catch (IOException | NoSuchElementException ex) { // Erreur de lecture ou format non supporté (comme SVG)
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
		int size = SparkUtils.queryParamInteger(request, "size", 32);
		return actionOnSingleItem(request, request.pathParameter(":itemId"), (item) -> {
			// Vérifier que l'élément a bien un parent
			if (item.parentId == null)
				return Render.badRequest();
			// Récupérer et vérifier le parent de l'élément
			Item parent = Item.findById(item.parentId);
			if (! parent.folder)
				return Render.badRequest();
			// OK, c'est bon
			parent.content.put("iconURL", "/files/thumbnail/" + item.id + "?size=" + size);
			parent.content.remove("iconURLCache");
			parent.updateDate = new Date();
			Item.update(parent);
			return Render.EMPTY;
		});
	};

	/** Met à jour le fichier à partir du fichier uploadé, recalcule les méta-données et les sauvegarde en base */
	private static final void updateFileFromFilePart(Upload upload, Item item, Date updateDate) throws IOException {
		File storedFile = getFile(item);
		if (upload.getFile() != null) {
			// La limite a été dépassée et le fichier a donc été écrit sur disque.
			// => on déplace le fichier (= rapide puisque c'est le même volume)
			java.nio.file.Files.move(upload.getFile().toPath(), storedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

		} else if (upload.getBytes() != null) {
			// La taille est en dessous de la limite et le contenu est donc en mémoire
			// => on écrit dans le fichier demandé
			try (OutputStream os = new FileOutputStream(storedFile)) {
				os.write(upload.getBytes());
			}

		} else {
			// La méthode par défaut est d'ouvrir le flux pour le copier
			// => c'est juste une fallback si on n'a détecté ni fichier, ni byte[]
			try (InputStream is = upload.getInputStream()) {
				//too slow : FileUtils.copyInputStreamToFile(is, storedFile);
				try (OutputStream os = new FileOutputStream(storedFile)) {
					IOUtils.copyLarge(is, os, new byte[1024*1024*10]);
				}
			}
		}
		// Mettre à jour les infos du fichier et sauvegarder
		updateFile(item, updateDate, true);
	}

	// TODO Gérer l'erreur 416 Range Not Satisfiable
	// TODO Gérer l'en-tête Range multiple, par exemple "0-10, 20-30, 40-50"
	// TODO EN faire un Render réuntilisable
	private static final Render returnFileRange(Response response, Item item, String range) {
		File file = getFile(item);
		if (!file.exists())
			return Render.notFound();
		// System.out.println("ByteRange=" + range + " pour " + item.name);
		response.type(configuration.getMimeTypeByFileName(item.name));
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
		response.header("Last-Modified", SparkUtils.HTTP_RESPONSE_HEADER_DATE_FORMAT.format(new Date(file.lastModified())));
		response.header("Content-Range", "bytes " + start + "-" + end + "/" + file.length());
		response.header("Connection", "keep-alive");
		// ne rien préciser puisqu'on ne peut pas savoir
		// response().setHeader("Expires", SparkUtils.HTTP_RESPONSE_HEADER_DATE_FORMAT.format(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)));
		// ne pas précisé CONTENT_LENGTH car doublon avec CONTENT_RANGE et ne fonctionne plus en plus
		// response().setHeader("Content-Length", Long.toString(end - start + 1));
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

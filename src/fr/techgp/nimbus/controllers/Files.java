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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonArray;

import fr.techgp.nimbus.Configuration;
import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.models.User;
import fr.techgp.nimbus.utils.ImageUtils;
import fr.techgp.nimbus.utils.SparkUtils;
import fr.techgp.nimbus.utils.StringUtils;
import spark.Request;
import spark.Response;
import spark.Route;

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

		// Configurer le traitement de la requête multi-part (Seuil et dossier pour écriture sur disque)
		prepareUploadRequest(request, configuration);

		// Extraire la requête
		Collection<Part> requestParts = request.raw().getParts();

		// Rechercher le dossier parent (où déposer les fichiers) et en vérifier l'accès
		Long parentId = null;
		for (Part part : requestParts) {
			if ("parentId".equals(part.getName())) {
				String parentIdString = IOUtils.toString(part.getInputStream(), "UTF-8");
				parentId = (StringUtils.isBlank(parentIdString) || "null".equals(parentIdString)) ? null : Long.valueOf(parentIdString);
				break;
			}
		}
		Item parent = parentId == null ? null : Item.findById(parentId);
		if (parentId != null && (parent == null || !parent.userLogin.equals(userLogin))) {
			for (Part part : requestParts) {
				part.delete(); // Nettoyer les fichiers uploadés qui ont été stockés sur disque
			}
			return SparkUtils.haltBadRequest();
		}

		// Récupérer les fichiers uploadés, les éventuels items correspondants et l'espace disque nécessaire
		List<Part> parts = new ArrayList<>();
		List<Item> items = new ArrayList<>();
		long requiredSpace = 0;
		for (Part part : request.raw().getParts()) {
			if (!"files".equals(part.getName()))
				continue;
			Item item = Item.findItemWithName(userLogin, parentId, part.getSubmittedFileName());
			parts.add(part);
			items.add(item);
			requiredSpace += part.getSize();
			if (item != null && item.content.getLong("length") != null)
				requiredSpace -= item.content.getLong("length").longValue();
		}

		// Vérifier avant de commencer que l'espace disque est suffisant
		long availableSpace = user.quota == null ? configuration.getStorageFolder().getFreeSpace() : (user.quota.longValue() * 1024L * 1024L - Item.calculateUsedSpace(userLogin));
		if (availableSpace < requiredSpace) {
			for (Part part : requestParts) {
				part.delete(); // Nettoyer les fichiers uploadés qui ont été stockés sur disque
			}
			return SparkUtils.haltInsufficientStorage();
		}

		// OK, lancer l'intégration
		JsonArray results = new JsonArray();
		// Récupérer, si elle est précisée, la date de mise à jour à utiliser pour les fichiers uploadés
		long generalUpdateDate = SparkUtils.queryParamLong(request, "updateDate", System.currentTimeMillis());
		for (int i = 0; i < parts.size(); i++) {
			Part part = parts.get(i);
			Item item = items.get(i);
			// Ajouter l'élément s'il n'existe pas encore
			if (item == null)
				item = Item.add(userLogin, parent, false, part.getSubmittedFileName(), null);
			// Récupérer, si elle est précisée, la date de mise à jour à utiliser pour ce fichier en particulier
			Date updateDate = new Date(SparkUtils.queryParamLong(request, "updateDate" + i, generalUpdateDate));
			// Enregistrement sur disque
			updateFileFromFilePart(part, item, updateDate);
			// Renvoyer la liste des ids créés
			results.add(item.id);
		}
		return SparkUtils.renderJSON(response, results);
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

		// Configurer le traitement de la requête multi-part (Seuil et dossier pour écriture sur disque)
		prepareUploadRequest(request, configuration);

		// Rechercher l'élément et en vérifier l'accès
		Item item = Item.findById(Long.valueOf(request.params(":itemId")));
		if (item == null || !item.userLogin.equals(userLogin))
			return SparkUtils.haltBadRequest();

		// Récupérer l'utilisateur pour connaitre son quota
		User user = User.findByLogin(userLogin);
		// Récupérer le fichiers uploadés pour connaitre l'espace disque nécessaire
		Part filePart = request.raw().getPart("file");
		if (user.quota != null) {
			// Vérifier que l'espace libre est suffisamment grand pour la différence avant/après
			long availableSpace = user.quota.longValue() * 1024L * 1024L - Item.calculateUsedSpace(userLogin);
			long newSize = filePart.getSize();
			long oldSize = Optional.ofNullable(item.content.getLong("length")).orElse(0L);
			if (availableSpace + oldSize - newSize < 0) {
				filePart.delete(); // Nettoyer le fichier uploadé qui a été stocké sur disque
				return SparkUtils.haltInsufficientStorage();
			}
		}

		// OK, lancer l'intégration à la date demandée (par défaut, maintenant)
		long updateDate = SparkUtils.queryParamLong(request, "updateDate", System.currentTimeMillis());
		updateFileFromFilePart(filePart, item, new Date(updateDate));
		return "";
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
		String name = request.queryParams("name");
		Long parentId = SparkUtils.queryParamLong(request, "parentId", null);
		// Vérifier l'unicité des noms
		if (Item.hasItemWithName(userLogin, parentId, name))
			return SparkUtils.haltConflict();
		// Ajouter un fichier vide dans le dossier demandé avec le nom donné
		Item item = Item.add(userLogin, parentId, false, name, null);
		if (item == null)
			return SparkUtils.haltBadRequest();
		// Retourner son id
		return item.id.toString();
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
		String path = request.splat()[0];
		if (StringUtils.isBlank(path))
			return SparkUtils.haltBadRequest();
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
				return SparkUtils.haltBadRequest();
			currentId = item.id;
		}
		// Renvoyer le fichier au bout du chemin
		return returnFile(response, item, false, null, null);
	};

	/**
	 * Calcule le chemin dans l'arborescence du fichier ":itemId" et redirige
	 * vers l'URL associée "/files/browse/path/to/filename.ext".
	 *
	 * (itemId) => redirect
	 */
	public static final Route browseTo = (request, response) -> {
		return actionOnSingleItem(request, request.params(":itemId"), (item) -> {
			StringBuilder url = new StringBuilder("/files/browse/");
			if (StringUtils.isNotBlank(item.path)) {
				String[] path = item.path.substring(0, item.path.length() - 1).split(",");
				Arrays.stream(path).map(s -> Item.findById(Long.valueOf(s)).name).forEach((name) -> url.append(name).append('/'));
			}
			url.append(item.name);
			try {
				response.redirect(new URI(null, null, url.toString(), null, null).toASCIIString());
			} catch (URISyntaxException ex) {
				ex.printStackTrace();
			}
			return null;
		});
	};

	/**
	 * Retourne le contenu d'un fichier ":itemId" en entier.
	 * L'en-tête HTTP "Range", si elle est spécifiée, spécifie la plage du contenu à renvoyer.
	 *
	 * (itemId[, Range]) => stream
	 */
	public static final Route stream = (request, response) -> {
		return actionOnSingleItem(request, request.params(":itemId"), (item) -> {
			String range = request.headers("Range");
			if (range != null && range.startsWith("bytes="))
				return returnFileRange(response, item, range.substring("bytes=".length()));
			return returnFile(response, item, false, null, null);
		});
	};

	/**
	 * Retourne le contenu d'un fichier ":itemId" afin d'être téléchargé sous le nom "item.name".
	 *
	 * (itemId) => stream
	 */
	public static final Route download = (request, response) -> {
		return actionOnSingleItem(request, request.params(":itemId"), (item) -> {
			return returnFile(response, item, true, null, null);
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
		return actionOnSingleItem(request, request.params(":itemId"), (item) -> {
			return returnFile(response, item, false, size, size);
		});
	};

	/**
	 * Utilise une miniature de l'élément "itemId" comme icone de son parent, d'une taille "size" de 32 pixels par défaut.
	 *
	 * (itemId[, size]) => ""
	 */
	public static final Route useAsFolderIcon = (request, response) -> {
		int size = SparkUtils.queryParamInteger(request, "size", 32);
		return actionOnSingleItem(request, request.params(":itemId"), (item) -> {
			// Vérifier que l'élément a bien un parent
			if (item.parentId == null)
				return SparkUtils.haltBadRequest();
			// Récupérer et vérifier le parent de l'élément
			Item parent = Item.findById(item.parentId);
			if (! parent.folder)
				return SparkUtils.haltBadRequest();
			// OK, c'est bon
			parent.content.put("iconURL", "/files/thumbnail/" + item.id + "?size=" + size);
			parent.content.remove("iconURLCache");
			parent.updateDate = new Date();
			Item.update(parent);
			return "";
		});
	};

	// Configurer le traitement de la requête multi-part (Seuil et dossier pour écriture sur disque)
	private static final void prepareUploadRequest(Request request, Configuration configuration) {
		String uploadFolder = configuration.getStorageFolder().getAbsolutePath();
		long maxFileSize = -1L; // peu importe
		long maxRequestSize = -1L; // on vérifie plus loin le quota
		int fileSizeThreshold = 100 * 1024 * 1024; // en mémoire jusqu'à 100 Mo
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(uploadFolder, maxFileSize, maxRequestSize, fileSizeThreshold));
	}

	/** Met à jour le fichier à partir du fichier uploadé, recalcule les méta-données et les sauvegarde en base */
	@SuppressWarnings("deprecation")
	private static final void updateFileFromFilePart(Part filePart, Item item, Date updateDate) throws IOException {
		File storedFile = getFile(item);
		// NB1 : utiliser "part.getInputStream()" n'est pas efficace quand le fichier
		//       a été écrit sur disque car on le copie inutilement à sa destination
		// NB2 : utiliser "part.write(filePath)" fonctionne bien pour les fichiers
		//       car elle les déplace mais on ne contrôle pas la taille du buffer pour la copie en mémoire
		// NB3 : caster "part" en "MultiPart" fonctionne et on peut tester si c'est un
		//       fichier (pour utiliser java.nio.file.Files.move) ou un byte[] (pour écrit dans un FileOutputStream)
		if (! (filePart instanceof org.eclipse.jetty.util.MultiPartInputStreamParser.MultiPart)) {
			if (logger.isWarnEnabled()) {
				logger.warn("Spark n'utilise apparemment plus la classe dépréciée MultiPartInputStreamParser.MultIPart");
				logger.warn("Passer sur MultiPartFormInputStream.MultiPart et supprimer @SuppressWarnings");
			}
			// Cette méthode n'est pas idéale car on copie inutilement si le fichier uploadé a été stocké sur disque.
			// => c'est juste une fallback car Jetty fournit des MultiPart (cf ci-dessous).
			try (InputStream is = filePart.getInputStream()) {
				//too slow : FileUtils.copyInputStreamToFile(is, storedFile);
				try (OutputStream os = new FileOutputStream(storedFile)) {
					IOUtils.copyLarge(is, os, new byte[1024*1024*10]);
				}
			}
		} else {
			org.eclipse.jetty.util.MultiPartInputStreamParser.MultiPart mpart = (org.eclipse.jetty.util.MultiPartInputStreamParser.MultiPart) filePart;
			if (mpart.getFile() != null) {
				// La limite a été dépassée et le fichier a donc été écrit sur disque.
				// => on déplace le fichier (= rapide puisque c'est le même volume)
				java.nio.file.Files.move(mpart.getFile().toPath(), storedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} else {
				// La taille est en dessous de la limite et le contenu est donc en mémoire
				try (OutputStream os = new FileOutputStream(storedFile)) {
					os.write(mpart.getBytes());
				}
			}
		}
		// Mettre à jour les infos du fichier et sauvegarder
		updateFile(item, updateDate, true);
	}

	private static final Object returnFile(Response response, Item item, boolean download, Integer thumbnailWidth, Integer thumbnailHeight) {
		String mimetype = configuration.getMimeTypeByFileName(item.name);
		if (download)
			response.header("Content-Disposition", "attachment; filename=\"" + item.name + "\"");
		else
			response.header("Content-Disposition", "inline; filename=\"" + item.name + "\"");
		response.type(mimetype);
		File file = getFile(item);
		if (!file.exists()) {
			if (thumbnailWidth == null && thumbnailHeight == null) {
				// OK, le fichier est considéré comme vide
				response.header("Content-Length", "0");
				return "";
			}
			// Fichier absent, pas possible de faire une miniature
			return SparkUtils.haltNotFound();
		}
		try {
			if (thumbnailWidth == null && thumbnailHeight == null)
				return SparkUtils.renderFile(response, mimetype, file, null);
			if (item.name.endsWith(".ico"))
				return SparkUtils.renderBytes(response, mimetype, ImageUtils.getScaleICOImage(file, thumbnailWidth, thumbnailHeight));
			return SparkUtils.renderBytes(response, mimetype, ImageUtils.getScaleImage(file, thumbnailWidth, thumbnailHeight));
		} catch (IOException | NoSuchElementException ex) { // Erreur de lecture ou format non supporté (comme SVG)
			return SparkUtils.haltBadRequest();
		}
	}

	// TODO : gérer l'erreur 416 Range Not Satisfiable
	// TODO : gérer l'en-tête Range multiple, par exemple "0-10, 20-30, 40-50"
	private static final Object returnFileRange(Response response, Item item, String range) {
		File file = getFile(item);
		if (!file.exists())
			SparkUtils.haltNotFound();
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
		try (FileInputStream fis = new FileInputStream(file)) {
			fis.skip(start);
			response.status(206); // Partial Content
			long remaining = end - start + 1;
			byte[] buffer = new byte[1024 * 1024];
			try (OutputStream os = response.raw().getOutputStream()) {
				while (remaining > 0) {
					int read = fis.read(buffer, 0, (int) Math.min(remaining, buffer.length));
					os.write(buffer, 0, read);
					remaining -= read;
				}
			}
			return "";
		} catch (IOException ex) {
			return SparkUtils.haltBadRequest();
		}
	}

}

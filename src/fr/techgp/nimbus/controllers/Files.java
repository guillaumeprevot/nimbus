package fr.techgp.nimbus.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.Date;

import javax.servlet.MultipartConfigElement;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.google.gson.JsonArray;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.utils.ImageUtils;
import fr.techgp.nimbus.utils.SparkUtils;
import fr.techgp.nimbus.utils.StringUtils;
import spark.Response;
import spark.Route;

public class Files extends Controller {

	/**
	 * Ajoute un ou plusieurs fichiers "files" dans le dossier "parentId" (facultatif).
	 * Le formulaire est envoyé en POST et encodé en "multipart/form-data".
	 *
	 * (files, parentId) => ""
	 */
	public static final Route upload = (request, response) -> {
		// Récupérer l'utilisateur connecté
		String userLogin = request.session().attribute("userLogin");
		// Extraire la requête
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(configuration.getStorageFolder().getAbsolutePath()));
		// Rechercher le parent éventuel et en vérifier l'accès
		String parentIdString = IOUtils.toString(request.raw().getPart("parentId").getInputStream(), "UTF-8");
		Long parentId = (StringUtils.isBlank(parentIdString) || "null".equals(parentIdString)) ? null : Long.valueOf(parentIdString);
		Item parent = parentId == null ? null : Item.findById(parentId);
		if (parentId != null && (parent == null || !parent.userLogin.equals(userLogin)))
			return SparkUtils.haltBadRequest();
		// Parcourir les fichiers uploadés
		JsonArray results = new JsonArray();
		request.raw().getParts().forEach((part) -> {
			if (!"files".equals(part.getName()))
				return;
			try {
				// Informations sur le fichier reçu
				String name = part.getSubmittedFileName();
				// Rechercher l'élément ayant ce nom ou l'ajouter s'il n'existe pas encore
				Item item = Item.findItemWithName(userLogin, parentId, name);
				if (item == null)
					item = Item.add(userLogin, parent, false, name, null);
				// Enregistrement sur disque
				File storedFile = getFile(item);
				try (InputStream is = part.getInputStream()) {
					FileUtils.copyInputStreamToFile(is, storedFile);
				}
				// Mettre à jour les infos du fichier
				updateFile(item);
				// Sauvegarde des métadonnées
				item.updateDate = new Date();
				Item.update(item);
				// Renvoyer la liste des ids créés
				results.add(item.id);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		});
		return SparkUtils.renderJSON(response, results);
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

	private static final Object returnFile(Response response, Item item, boolean download, Integer thumbnailWidth, Integer thumbnailHeight) {
		String mimetype = configuration.getMimeTypeByFileName(item.name);
		if (download)
			response.header("Content-disposition", "attachment; filename=\"" + item.name + "\"");
		else
			response.header("Content-disposition", "inline; filename=\"" + item.name + "\"");
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
		} catch (IOException ex) {
			return SparkUtils.haltBadRequest();
		}
	}

	private static final Object returnFileRange(Response response, Item item, String range) {
		File file = getFile(item);
		if (!file.exists())
			SparkUtils.haltNotFound();
		// System.out.println("ByteRange=" + range + " pour " + item.name);
		response.type(configuration.getMimeTypeByFileName(item.name));
		response.header("Content-disposition", "inline; filename=\"" + item.name + "\"");

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
			return null;
		} catch (IOException ex) {
			return SparkUtils.haltBadRequest();
		}
	}

	/**
	 * Cette méthode met à jour les méta-données de l'élément "item".
	 *
	 * @param item l'élément représentant un fichier dans le cloud
	 * @throws Exception si l'une des facets lance une erreur
	 */
	private static final void updateFile(Item item) throws Exception {
		// Informations sur l'élément
		File storedFile = getFile(item);
		String extension = FilenameUtils.getExtension(item.name).toLowerCase();
		// Mise à jour des méta-données
		item.content.clear();
		item.content.append("length", storedFile.length());
		// Mettre à jour les propriétés spécifiques aux Facet
		for (Facet facet : configuration.getFacets()) {
			try {
				if (facet.supports(extension))
					facet.updateMetadata(storedFile, extension, item.content);
			} catch (Exception ex) {
				if (Controller.logger.isErrorEnabled())
					Controller.logger.error("Erreur de la facet " + facet.getClass().getSimpleName() + " sur l'élément n°" + item.id + " (" + item.name + ")", ex);
			}
		}
	}

}

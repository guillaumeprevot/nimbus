package fr.techgp.nimbus.controllers;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Route;

/** Ce controlleur implémente la gestion des fichiers statiques et gère la mise en cache par Etag / Last-Modified / If-None-Match / If-Modified-Since */
public class StaticFiles extends Controller {

	public static final Route publicFolder = (request, response) -> {
		String path = request.path();
		if (path.contains(".."))
			return Render.forbidden();
		File file = new File("public", path);
		if (!file.exists() || !file.isFile())
			return Render.notFound();
		// Indiquer le bon type MIME
		String extension = FilenameUtils.getExtension(file.getName());
		String mimetype = configuration.getMimeType(extension);
		// Renvoyer le fichier tout en gérant le cache
		return Render.staticFile(file, mimetype);
	};

}

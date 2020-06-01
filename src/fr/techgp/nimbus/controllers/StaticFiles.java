package fr.techgp.nimbus.controllers;

import java.io.File;

import fr.techgp.nimbus.server.MimeTypes;
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
		String mimetype = MimeTypes.byName(file.getName());
		// Renvoyer le fichier tout en gérant le cache
		return Render.staticFile(file, mimetype);
	};

}

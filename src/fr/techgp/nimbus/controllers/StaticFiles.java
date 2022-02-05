package fr.techgp.nimbus.controllers;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

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

	public static final Route svgFolder = (request, response) -> {
		if (configuration.getClientFaviconColor() == null)
			return publicFolder.handle(request, response);
		String path = request.path();
		if (path.contains(".."))
			return Render.forbidden();
		File file = new File("public", path);
		if (!file.exists() || !file.isFile())
			return Render.notFound();
		// Lire le contenu SVG
		String svg = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
		// Indiquer le bon type MIME
		response.type(MimeTypes.byName(file.getName()));
		// Renvoyer l'icône avec la couleur ajustée
		return Render.string(svg.replace("#18bc9c", configuration.getClientFaviconColor()));
	};
}

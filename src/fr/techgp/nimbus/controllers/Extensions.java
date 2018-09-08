package fr.techgp.nimbus.controllers;

import fr.techgp.nimbus.utils.SparkUtils;
import spark.Route;

public class Extensions extends Controller {

	/**
	 * Cette route affiche la page du lecteur de fichiers ePub
	 * 
	 * () => HTML
	 */
	public static final Route epub = (request, response) -> {
		// Générer la page
		return renderTemplate("epub.html",
				"url", SparkUtils.queryParamUrl(request, "url", ""),
				"fromUrl", SparkUtils.queryParamUrl(request, "fromUrl", ""),
				"fromTitle", SparkUtils.queryParamUrl(request, "fromTitle", ""),
				"lang", SparkUtils.getRequestLang(request));
	};

}

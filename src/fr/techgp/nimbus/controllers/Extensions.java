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

	/**
	 * Cette route affiche la page du lecteur de fichiers PDF
	 * 
	 * () => HTML
	 */
	public static final Route pdf = (request, response) -> {
		// Générer la page
		return renderTemplate("pdf.html",
				"url", SparkUtils.queryParamUrl(request, "url", ""),
				"fromUrl", SparkUtils.queryParamUrl(request, "fromUrl", ""),
				"fromTitle", SparkUtils.queryParamUrl(request, "fromTitle", ""),
				"lang", SparkUtils.getRequestLang(request));
	};

	/**
	 * Cette route affiche la page du lecteur vidéo
	 * 
	 * () => HTML
	 */
	public static final Route video = (request, response) -> {
		// Générer la page
		return renderTemplate("video.html",
				"url", SparkUtils.queryParamUrl(request, "url", ""),
				"fromUrl", SparkUtils.queryParamUrl(request, "fromUrl", ""),
				"fromTitle", SparkUtils.queryParamUrl(request, "fromTitle", ""),
				"lang", SparkUtils.getRequestLang(request));
	};

	/**
	 * Cette route affiche la page du lecteur audio
	 * 
	 * () => HTML
	 */
	public static final Route audio = (request, response) -> {
		// Générer la page
		return renderTemplate("audio.html",
				"fromUrl", SparkUtils.queryParamUrl(request, "fromUrl", ""),
				"fromTitle", SparkUtils.queryParamUrl(request, "fromTitle", ""),
				"lang", SparkUtils.getRequestLang(request));
	};

}

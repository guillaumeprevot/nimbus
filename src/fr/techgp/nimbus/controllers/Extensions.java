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
		return renderTemplate(request, "epub.html",
				"url", SparkUtils.queryParamUrl(request, "url", ""),
				"fromUrl", SparkUtils.queryParamUrl(request, "fromUrl", ""),
				"fromTitle", SparkUtils.queryParamUrl(request, "fromTitle", ""));
	};

	/**
	 * Cette route affiche la page du lecteur de fichiers PDF
	 * 
	 * () => HTML
	 */
	public static final Route pdf = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "pdf.html",
				"url", SparkUtils.queryParamUrl(request, "url", ""),
				"fromUrl", SparkUtils.queryParamUrl(request, "fromUrl", ""),
				"fromTitle", SparkUtils.queryParamUrl(request, "fromTitle", ""));
	};

	/**
	 * Cette route affiche la page du lecteur vidéo
	 * 
	 * () => HTML
	 */
	public static final Route video = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "video.html",
				"itemId", SparkUtils.queryParamLong(request, "itemId", null),
				"fromUrl", SparkUtils.queryParamUrl(request, "fromUrl", ""),
				"fromTitle", SparkUtils.queryParamUrl(request, "fromTitle", ""));
	};

	/**
	 * Cette route affiche la page du lecteur audio
	 * 
	 * () => HTML
	 */
	public static final Route audio = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "audio.html",
				"fromUrl", SparkUtils.queryParamUrl(request, "fromUrl", ""),
				"fromTitle", SparkUtils.queryParamUrl(request, "fromTitle", ""));
	};

	/**
	 * Cette route affiche la page du diaporama
	 * 
	 * () => HTML
	 */
	public static final Route diaporama = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "diaporama.html",
				"ids", SparkUtils.queryParamString(request, "ids", ""),
				"play", SparkUtils.queryParamBoolean(request, "play", false),
				"selection", SparkUtils.queryParamString(request, "selection", ""),
				"fromUrl", SparkUtils.queryParamUrl(request, "fromUrl", ""),
				"fromTitle", SparkUtils.queryParamUrl(request, "fromTitle", ""));
	};

	/**
	 * Cette route affiche la page de l'éditeur de texte
	 * 
	 * () => HTML
	 */
	public static final Route textEditor = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "text-editor.html",
				"markdown", false,
				"baseURL", configuration.getServerAbsoluteUrl(),
				"itemId", SparkUtils.queryParamLong(request, "itemId", null),
				"fromUrl", SparkUtils.queryParamUrl(request, "fromUrl", ""),
				"fromTitle", SparkUtils.queryParamUrl(request, "fromTitle", ""));
	};

	/**
	 * Cette route affiche la page de l'éditeur Markdown
	 * 
	 * () => HTML
	 */
	public static final Route markdownEditor = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "text-editor.html",
				"markdown", true,
				"highlighter", SparkUtils.queryParamString(request, "highlighter", configuration.getClientCodeHighlighter()),
				"baseURL", configuration.getServerAbsoluteUrl(),
				"itemId", SparkUtils.queryParamLong(request, "itemId", null),
				"fromUrl", SparkUtils.queryParamUrl(request, "fromUrl", ""),
				"fromTitle", SparkUtils.queryParamUrl(request, "fromTitle", ""));
	};

	/**
	 * Cette route affiche la page de l'éditeur de notes
	 * 
	 * () => HTML
	 */
	public static final Route noteEditor = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "note-editor.html",
				"baseURL", configuration.getServerAbsoluteUrl(),
				"itemId", SparkUtils.queryParamLong(request, "itemId", null),
				"fromUrl", SparkUtils.queryParamUrl(request, "fromUrl", ""),
				"fromTitle", SparkUtils.queryParamUrl(request, "fromTitle", ""));
	};

	/**
	 * Cette route affiche la page de l'éditeur de code basé sur CodeMirror
	 * 
	 * () => HTML
	 */
	public static final Route codeEditor = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "code-editor.html",
				"itemId", SparkUtils.queryParamLong(request, "itemId", null),
				"fromUrl", SparkUtils.queryParamUrl(request, "fromUrl", ""),
				"fromTitle", SparkUtils.queryParamUrl(request, "fromTitle", ""));
	};

	/**
	 * Cette route affiche la page permettant de déchiffrer, modifier puis rechiffrer du texte
	 * 
	 * () => HTML
	 */
	public static final Route secretEditor = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "secret-editor.html",
				"itemId", SparkUtils.queryParamLong(request, "itemId", null),
				"fromUrl", SparkUtils.queryParamUrl(request, "fromUrl", ""),
				"fromTitle", SparkUtils.queryParamUrl(request, "fromTitle", ""));
	};

	/**
	 * Cette route affiche la page du calendrier
	 * 
	 * () => HTML
	 */
	public static final Route calendar = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "calendar.html",
				"itemId", SparkUtils.queryParamLong(request, "itemId", null),
				"fromUrl", SparkUtils.queryParamUrl(request, "fromUrl", ""),
				"fromTitle", SparkUtils.queryParamUrl(request, "fromTitle", ""));
	};

}

package fr.techgp.nimbus.controllers;

import java.net.URLDecoder;

import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Route;
import fr.techgp.nimbus.utils.StringUtils;

public class Extensions extends Controller {

	/**
	 * Cette route affiche la page du lecteur de fichiers ePub
	 *
	 * () => HTML
	 */
	public static final Route epub = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "epub.html",
				"userLogin", request.session().attribute("userLogin"),
				"url", queryParameterURL(request, "url", ""),
				"fromUrl", queryParameterURL(request, "fromUrl", ""),
				"fromTitle", request.queryParameter("fromTitle", ""));
	};

	/**
	 * Cette route affiche la page du lecteur de fichiers PDF
	 *
	 * () => HTML
	 */
	public static final Route pdf = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "pdf.html",
				"userLogin", request.session().attribute("userLogin"),
				"url", queryParameterURL(request, "url", ""),
				"fromUrl", queryParameterURL(request, "fromUrl", ""),
				"fromTitle", request.queryParameter("fromTitle", ""));
	};

	/**
	 * Cette route affiche la page du lecteur vidéo
	 *
	 * () => HTML
	 */
	public static final Route video = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "video.html",
				"userLogin", request.session().attribute("userLogin"),
				"itemId", request.queryParameterLong("itemId", null),
				"fromUrl", queryParameterURL(request, "fromUrl", ""),
				"fromTitle", request.queryParameter("fromTitle", ""));
	};

	/**
	 * Cette route affiche la page du lecteur audio
	 *
	 * () => HTML
	 */
	public static final Route audio = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "audio.html",
				"fromUrl", queryParameterURL(request, "fromUrl", ""),
				"fromTitle", request.queryParameter("fromTitle", ""));
	};

	/**
	 * Cette route affiche la page du diaporama
	 *
	 * () => HTML
	 */
	public static final Route diaporama = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "diaporama.html",
				"ids", request.queryParameter("ids", ""),
				"play", request.queryParameterBoolean("play", false),
				"selection", request.queryParameter("selection", ""),
				"fromUrl", queryParameterURL(request, "fromUrl", ""),
				"fromTitle", request.queryParameter("fromTitle", ""));
	};

	/**
	 * Cette route affiche la page de l'éditeur de texte
	 *
	 * () => HTML
	 */
	public static final Route textEditor = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "text-editor.html",
				"itemId", request.queryParameterLong("itemId", null),
				"fromUrl", queryParameterURL(request, "fromUrl", ""),
				"fromTitle", request.queryParameter("fromTitle", ""));
	};

	/**
	 * Cette route affiche la page de l'éditeur Markdown
	 *
	 * () => HTML
	 */
	public static final Route markdownEditor = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "markdown-editor.html",
				"markdown", true,
				"highlighter", request.queryParameter("highlighter", configuration.getClientCodeHighlighter()),
				"baseURL", configuration.getServerAbsoluteUrl(),
				"itemId", request.queryParameterLong("itemId", null),
				"fromUrl", queryParameterURL(request, "fromUrl", ""),
				"fromTitle", request.queryParameter("fromTitle", ""));
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
				"itemId", request.queryParameterLong("itemId", null),
				"fromUrl", queryParameterURL(request, "fromUrl", ""),
				"fromTitle", request.queryParameter("fromTitle", ""));
	};

	/**
	 * Cette route affiche la page de l'éditeur de code basé sur CodeMirror
	 *
	 * () => HTML
	 */
	public static final Route codeEditor = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "code-editor.html",
				"itemId", request.queryParameterLong("itemId", null),
				"fromUrl", queryParameterURL(request, "fromUrl", ""),
				"fromTitle", request.queryParameter("fromTitle", ""));
	};

	/**
	 * Cette route affiche la page permettant de déchiffrer, modifier puis rechiffrer du texte
	 *
	 * () => HTML
	 */
	public static final Route secretEditor = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "secret-editor.html",
				"itemId", request.queryParameterLong("itemId", null),
				"fromUrl", queryParameterURL(request, "fromUrl", ""),
				"fromTitle", request.queryParameter("fromTitle", ""));
	};

	/**
	 * Cette route affiche la page du calendrier
	 *
	 * () => HTML
	 */
	public static final Route calendar = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "calendar.html",
				"itemId", request.queryParameterLong("itemId", null));
	};

	/**
	 * Cette route affiche la page de l'application "Contacts"
	 *
	 * () => HTML
	 */
	public static final Route contacts = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "contacts.html",
				"itemId", request.queryParameterLong("itemId", null));
	};

	/**
	 * Cette route affiche la page de l'application "Favoris"
	 *
	 * () => HTML
	 */
	public static final Route bookmarks = (request, response) -> {
		// Générer la page
		return renderTemplate(request, "bookmarks.html",
				"itemId", request.queryParameterLong("itemId", null));
	};

	private static final String queryParameterURL(Request request, String name, String defaultValue) {
		String s = request.queryParameter(name);
		try {
			if (StringUtils.isNotBlank(s))
				return URLDecoder.decode(s, "UTF-8");
		} catch (Exception ex) { /* */ }
		return defaultValue;
	}

}

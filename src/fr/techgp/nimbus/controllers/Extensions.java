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
		return Templates.render(request, "epub.html",
				"userLogin", getUserLogin(request),
				"url", queryParameterURL(request, "url", ""));
	};

	/**
	 * Cette route affiche la page du lecteur de fichiers PDF
	 *
	 * () => HTML
	 */
	public static final Route pdf = (request, response) -> {
		// Générer la page
		return Templates.render(request, "pdf.html",
				"userLogin", getUserLogin(request),
				"url", queryParameterURL(request, "url", ""));
	};

	/**
	 * Cette route affiche la page du lecteur vidéo
	 *
	 * () => HTML
	 */
	public static final Route video = (request, response) -> {
		// Générer la page
		return Templates.render(request, "video.html",
				"userLogin", getUserLogin(request),
				"itemId", request.queryParameterLong("itemId", null));
	};

	/**
	 * Cette route affiche la page du lecteur audio
	 *
	 * () => HTML
	 */
	public static final Route audio = authenticateOrRedirect((request, response) -> {
		// Générer la page
		return Templates.render(request, "audio.html");
	});

	/**
	 * Cette route affiche la page du diaporama
	 *
	 * () => HTML
	 */
	public static final Route diaporama = authenticateOrRedirect((request, response) -> {
		// Générer la page
		return Templates.render(request, "diaporama.html",
				"ids", request.queryParameter("ids", ""),
				"play", request.queryParameterBoolean("play", false),
				"selection", request.queryParameter("selection", ""));
	});

	/**
	 * Cette route affiche la page de l'éditeur de texte
	 *
	 * () => HTML
	 */
	public static final Route textEditor = authenticateOrRedirect((request, response) -> {
		// Générer la page
		return Templates.render(request, "text-editor.html",
				"itemId", request.queryParameterLong("itemId", null));
	});

	/**
	 * Cette route affiche la page de l'éditeur Markdown
	 *
	 * () => HTML
	 */
	public static final Route markdownEditor = authenticateOrRedirect((request, response) -> {
		// Générer la page
		return Templates.render(request, "markdown-editor.html",
				"baseURL", configuration.getServerAbsoluteUrl(),
				"itemId", request.queryParameterLong("itemId", null));
	});

	/**
	 * Cette route affiche la page de l'éditeur de notes
	 *
	 * () => HTML
	 */
	public static final Route noteEditor = authenticateOrRedirect((request, response) -> {
		// Générer la page
		return Templates.render(request, "note-editor.html",
				"baseURL", configuration.getServerAbsoluteUrl(),
				"itemId", request.queryParameterLong("itemId", null));
	});

	/**
	 * Cette route affiche la page de l'éditeur de code basé sur CodeMirror
	 *
	 * () => HTML
	 */
	public static final Route codeEditor = authenticateOrRedirect((request, response) -> {
		// Générer la page
		return Templates.render(request, "code-editor.html",
				"itemId", request.queryParameterLong("itemId", null));
	});

	/**
	 * Cette route affiche la page permettant de déchiffrer, modifier puis rechiffrer du texte
	 *
	 * () => HTML
	 */
	public static final Route secretEditor = authenticateOrRedirect((request, response) -> {
		// Générer la page
		return Templates.render(request, "secret-editor.html",
				"itemId", request.queryParameterLong("itemId", null));
	});

	/**
	 * Cette route affiche la page du calendrier
	 *
	 * () => HTML
	 */
	public static final Route calendar = authenticateOrRedirect((request, response) -> {
		// Générer la page
		return Templates.render(request, "calendar.html",
				"itemId", request.queryParameterLong("itemId", null));
	});

	/**
	 * Cette route affiche la page de l'application "Contacts"
	 *
	 * () => HTML
	 */
	public static final Route contacts = authenticateOrRedirect((request, response) -> {
		// Générer la page
		return Templates.render(request, "contacts.html",
				"itemId", request.queryParameterLong("itemId", null));
	});

	/**
	 * Cette route affiche la page de l'application "Favoris"
	 *
	 * () => HTML
	 */
	public static final Route bookmarks = authenticateOrRedirect((request, response) -> {
		// Générer la page
		return Templates.render(request, "bookmarks.html",
				"itemId", request.queryParameterLong("itemId", null));
	});

	private static final String queryParameterURL(Request request, String name, String defaultValue) {
		String s = request.queryParameter(name);
		try {
			if (StringUtils.isNotBlank(s))
				return URLDecoder.decode(s, "UTF-8");
		} catch (Exception ex) { /* */ }
		return defaultValue;
	}

	public static final Route authenticateOrRedirect(Route route) {
		return (request, response) -> {
			String login = Filters.getLogin(request, response, false);
			if (login == null)
				return Filters.redirect(request);
			return route.handle(request, response);
		};
	}

}

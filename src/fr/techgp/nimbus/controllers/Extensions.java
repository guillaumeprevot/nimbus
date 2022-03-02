package fr.techgp.nimbus.controllers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Route;
import fr.techgp.nimbus.utils.ConversionUtils;
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

	/**
	 * Cette route vérifie les sommes de contrôles qu'elle lit dans le fichier indiqué en suivant l'algorithme indiqué.
	 * La méthode renverra du JSON indiquant le résultat du contrôle : fichiers trouvés, checksums validées ou incorrectes.
	 *
	 * (itemId, algorithm) => JsonArray<JsonObject<path, expected, actual?>>
	 */
	public static final Route checkDigest = (request, response) -> {
		// Récupérer l'utilisateur connecté
		String userLogin = getUserLogin(request);
		if (userLogin == null)
			return Render.unauthorized();
		// Rechercher l'élément et en vérifier l'accès
		String itemIdString = request.queryParameter("itemId");
		Item item = Item.findById(Long.valueOf(itemIdString));
		if (item == null || !item.userLogin.equals(userLogin) || item.folder)
			return Render.badRequest();
		try {
			// Récupérer l'algorithme demandé
			String algorithm = request.queryParameter("algorithm", "SHA-256");
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			byte[] buffer = new byte[1014 * 1014];
			// Préparer le résultat
			JsonArray results = new JsonArray();
			// Lire le contenu
			List<String> lines = FileUtils.readLines(getFile(item), StandardCharsets.UTF_8);
			for (String line : lines) {
				String hash = line.substring(0, 2 * digest.getDigestLength()).toLowerCase();
				String name = line.substring(2 * digest.getDigestLength()).trim();
				JsonObject result = new JsonObject();
				result.addProperty("name", name);
				result.addProperty("expected", hash);
				Item sibling = Item.findItemWithName(userLogin, item.parentId, name);
				if (sibling != null) {
					// Vérifier le fichier
					digest.reset();
					try (InputStream is = new FileInputStream(getFile(sibling))) {
						int read;
						while ((read = is.read(buffer)) != -1) {
							digest.update(buffer, 0, read);
						}
					}
					result.addProperty("actual", ConversionUtils.bytes2hex(digest.digest()));
				}
				results.add(result);
			}
			return Render.json(results);
		} catch (NoSuchAlgorithmException ex) {
			return Render.badRequest();
		} catch (IOException ex) {
			ex.printStackTrace();
			return Render.internalServerError();
		}
	};

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

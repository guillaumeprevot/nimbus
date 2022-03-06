package fr.techgp.nimbus.controllers;

import java.io.File;

import fr.techgp.nimbus.models.User;
import fr.techgp.nimbus.server.MimeTypes;
import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Route;
import fr.techgp.nimbus.server.Session;
import fr.techgp.nimbus.utils.StringUtils;

public class Authentication extends Controller {

	/**
	 * Cette méthode affiche la page de connexion.
	 *
	 * ($urlToLoad, $logout) => HTML
	 *
	 * @see login.html
	 */
	public static final Route page = (request, response) -> {
		String urlToLoad = null;
		boolean logout = false;
		Session session = getSession(request, false);
		if (session != null) {
			urlToLoad = session.attribute("urlToLoad");
			logout = Boolean.TRUE.equals(session.attribute("logout"));
			session.removeAttribute("logout");
			session.removeAttribute("urlToLoad");
		}
		return renderLoginPage(request, false, logout, urlToLoad);
	};

	/**
	 * Cette méthode authentifie l'utilisateur qui envoie un couple (login, password) :
	 * - si l'authentification réussit, il est redirigé vers "urlToLoad", si précisée
	 * - si l'authentification échoue, il doit retenter l'authentification
	 *
	 * (login, password, urlToLoad) => redirect(urlToLoad)
	 *
	 * @see Controller#authenticate(String, String)
	 * @see login.html
	 */
	public static final Route login = (request, response) -> {
		String login = request.queryParameter("login");
		String password = request.queryParameter("password");
		String error = authenticate(login, password);
		String urlToLoad = StringUtils.withDefault(request.queryParameter("urlToLoad"), "/");
		if (error != null) {
			if (Controller.logger.isWarnEnabled())
				Controller.logger.warn("Authentification échouée ({} / {}) : {}", login, request.ip(), error);
			return renderLoginPage(request, true, false, urlToLoad);
		}
		getSession(request, true).attribute("userLogin", login);
		return Render.redirect(urlToLoad);
	};

	/**
	 * Cette méthode déconnecte l'utilisateur en supprimant l'attribut "userLogin" de la session et le renvoie vers la page de connexion.
	 *
	 * () => redirect(page de connexion)
	 */
	public static final Route logout = (request, response) -> {
		Session session = getSession(request, true);
		session.removeAttribute("userLogin");
		session.attribute("logout", Boolean.TRUE);
		return Render.redirect("/login.html");
	};

	/**
	 * Cette route renvoie l'image de fond de la page de login, si cette image est définie.
	 *
	 * () => image
	 */
	public static final Route background = (request, response) -> {
		String background = configuration.getClientLoginBackground();
		if (StringUtils.isBlank(background))
			return Render.notFound();
		File file = new File(configuration.getStorageFolder(), background);
		if (!file.exists())
			return Render.notFound();
		// Indiquer le bon type MIME
		String mimetype = MimeTypes.byName(file.getName());
		// Renvoyer le fichier tout en gérant le cache
		return Render.staticFile(file, mimetype);
	};

	private static final Render renderLoginPage(Request request, boolean error, boolean logout, String urlToLoad) {
		return Templates.render(request, "login.html",
				"background", StringUtils.isNotBlank(configuration.getClientLoginBackground()),
				"login", StringUtils.withDefault(request.queryParameter("login"), ""),
				"urlToLoad", StringUtils.withDefault(urlToLoad, "/"),
				"error", error,
				"logout", logout,
				"install", User.count() == 0);
	}
}
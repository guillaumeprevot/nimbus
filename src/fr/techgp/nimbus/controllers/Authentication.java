package fr.techgp.nimbus.controllers;

import java.io.File;

import fr.techgp.nimbus.models.User;
import fr.techgp.nimbus.utils.SparkUtils;
import fr.techgp.nimbus.utils.StringUtils;
import spark.Request;
import spark.Route;

public class Authentication extends Controller {

	/**
	 * Cette méthode affiche la page de connexion.
	 *
	 * ($urlToLoad, $logout) => HTML
	 *
	 * @see login.html
	 */
	public static final Route page = (request, response) -> {
		String urlToLoad = request.session().attribute("urlToLoad");
		boolean logout = Boolean.TRUE.equals(request.session().attribute("logout"));
		request.session().removeAttribute("logout");
		request.session().removeAttribute("urlToLoad");
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
		String login = request.queryParams("login");
		String password = request.queryParams("password");
		String error = authenticate(login, password);
		String urlToLoad = StringUtils.withDefault(request.queryParams("urlToLoad"), "/");
		if (error != null) {
			if (logger.isWarnEnabled())
				logger.warn("Authentification échouée (" + login + " / " + request.ip() + ") : " + error);
			return renderLoginPage(request, true, false, urlToLoad);
		}
		request.session().attribute("userLogin", login);
		response.redirect(urlToLoad);
		return null;
	};

	/**
	 * Cette méthode déconnecte l'utilisateur en supprimant l'attribut "userLogin" de la session et le renvoie vers la page de connexion.
	 *
	 * () => redirect(page de connexion)
	 */
	public static final Route logout = (request, response) -> {
		request.session().removeAttribute("userLogin");
		request.session().attribute("logout", Boolean.TRUE);
		response.redirect("/login.html");
		return null;
	};

	/**
	 * Cette route renvoie l'image de fond de la page de login, si cette image est définie.
	 *
	 * () => image
	 */
	public static final Route background = (request, response) -> {
		String background = configuration.getClientLoginBackground();
		if (StringUtils.isBlank(background))
			return SparkUtils.haltNotFound();
		File file = new File(configuration.getStorageFolder(), background);
		if (!file.exists())
			return SparkUtils.haltNotFound();
		return StaticFiles.sendCacheable(request.raw(), response.raw(), file);
	};

	private static final String renderLoginPage(Request request, boolean error, boolean logout, String urlToLoad) {
		return renderTemplate(request, "login.html",
				"background", StringUtils.isNotBlank(configuration.getClientLoginBackground()),
				"login", StringUtils.withDefault(request.queryParams("login"), ""),
				"urlToLoad", StringUtils.withDefault(urlToLoad, "/"),
				"error", error,
				"logout", logout,
				"install", User.count() == 0);
	}
}
package fr.techgp.nimbus.controllers;

import java.util.HashMap;
import java.util.Map;

import fr.techgp.nimbus.models.User;
import fr.techgp.nimbus.utils.SparkUtils;
import fr.techgp.nimbus.utils.StringUtils;
import spark.ModelAndView;
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
		Map<String, Object> attributes = attributes(request, false, logout, urlToLoad);
		return Controller.templateEngine.render(new ModelAndView(attributes, "login.html"));
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
			Map<String, Object> attributes = attributes(request, true, false, urlToLoad);
			return Controller.templateEngine.render(new ModelAndView(attributes, "login.html"));
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

	private static final Map<String, Object> attributes(Request request, boolean error, boolean logout, String urlToLoad) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("lang", SparkUtils.getRequestLang(request));
		attributes.put("theme", StringUtils.withDefault(request.session().attribute("theme"), ""));
		attributes.put("login", StringUtils.withDefault(request.queryParams("login"), ""));
		attributes.put("urlToLoad", StringUtils.withDefault(urlToLoad, "/"));
		attributes.put("error", error);
		attributes.put("logout", logout);
		attributes.put("install", User.count() == 0);
		return attributes;
	}

}
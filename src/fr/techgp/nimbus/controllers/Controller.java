package fr.techgp.nimbus.controllers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import fr.techgp.nimbus.models.Mongo;
import fr.techgp.nimbus.models.User;
import fr.techgp.nimbus.utils.CryptoUtils;
import fr.techgp.nimbus.utils.StringUtils;
import fr.techgp.nimbus.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModelException;
import spark.ModelAndView;
import spark.Spark;
import spark.TemplateEngine;
import spark.template.freemarker.FreeMarkerEngine;

public class Controller {

	protected static Logger logger = null;
	protected static Configuration configuration;
	protected static TemplateEngine templateEngine;

	public static final void init(Logger logger, Configuration configuration, boolean dev) {
		Controller.logger = logger;
		Controller.configuration = configuration;
		Controller.templateEngine = prepareTemplateEngine(dev);

		Spark.before("/admin.html", Filters.filterAdministratorOrRedirect);
		Spark.get("/admin.html", (request, response) -> {
			String login = request.session().attribute("userLogin");
			User user = User.findByLogin(login);
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("userName", StringUtils.withDefault(user.name, user.login));
			return Controller.templateEngine.render(new ModelAndView(attributes, "admin.html"));
		});

		Spark.before("/main.html", Filters.filterAuthenticatedOrRedirect);
		Spark.get("/main.html", (request, response) -> {
			String login = request.session().attribute("userLogin");
			User user = User.findByLogin(login);
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("userName", StringUtils.withDefault(user.name, user.login));
			return Controller.templateEngine.render(new ModelAndView(attributes, "main.html"));
		});

		Spark.get("/login.html", Authentication.page);
		Spark.post("/login.html", Authentication.login);
		Spark.get("/logout", Authentication.logout);

		// TODO : supprimer à terme cette méthode qui n'est là que pour les tests
		Spark.get("/reset", (request, response) -> {
			Mongo.reset(true);
			request.session().removeAttribute("userLogin");
			response.redirect("/login.html");
			return null;
		});
	}

	/**
	 * Cette méthode prépare le moteur de rendu des vues en utilisant FreeMarker.
	 *
	 * @param dev indique si on active les fonctions utiles pendant le DEV mais à désactiver en PROD.
	 * @return retourne le TemplateEngine utilisant FreeMarker
	 */
	private static final TemplateEngine prepareTemplateEngine(boolean dev) {
		try {
			// https://freemarker.apache.org/docs/pgui_quickstart_createconfiguration.html
			freemarker.template.Version version = freemarker.template.Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS;
			freemarker.template.Configuration configuration = new freemarker.template.Configuration(version);
			configuration.setDirectoryForTemplateLoading(new File("templates"));
			configuration.setDefaultEncoding("UTF-8");
			if (dev) {
				configuration.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
				configuration.setTemplateUpdateDelayMilliseconds(500);
			} else {
				configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
			}
			configuration.setLogTemplateExceptions(false);
			configuration.setWrapUncheckedExceptions(true);
			configuration.setSharedVariable("appName", "Nimbus");
			return new FreeMarkerEngine(configuration);
		} catch (TemplateModelException | IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Cette méthode vérifie si le couple (login, password) est valide.
	 * En cas d'erreur, une chaine de caractères non null est renvoyée.
	 * A l'installation (= base vide), un premier compte est créé automatiquement.
	 *
	 * @param login le login fourni
	 * @param password le mot de passe fourni
	 * @return null si OK ou une chaine de caractère représentant l'erreur sinon
	 */
	protected static final String authenticate(String login, String password) {
		if (StringUtils.isBlank(login))
			return "utilisateur vide";
		if (StringUtils.isBlank(password))
			return "mot de passe vide";

		if (User.count() == 0) {
			// Installation
			try {
				User user = new User();
				user.login = login;
				user.password = CryptoUtils.hashPassword(password);
				user.admin = true;
				User.insert(user);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		User user = User.findByLogin(login);
		if (user == null)
			return "utilisateur inconnu";
		if (!CryptoUtils.validatePassword(password, user.password))
			return "mot de passe incorrect";
		return null;
	}

}

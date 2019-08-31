package fr.techgp.nimbus.controllers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import fr.techgp.nimbus.models.User;
import fr.techgp.nimbus.utils.CryptoUtils;
import fr.techgp.nimbus.utils.SparkUtils;
import fr.techgp.nimbus.utils.StringUtils;
import spark.Route;

public class Preferences extends Controller {

	/**
	 * Cette route redirige vers la feuille CSS appropriée : le thème souhaité ou Bootstrap par défaut
	 * 
	 * ($theme) => redirect
	 */
	public static final Route stylesheet = (request, response) -> {
		String theme = getUserTheme(request);
		if ("dark".equals(theme))
			response.redirect("/libs/bootswatch/darkly.min.css");
		else
			response.redirect("/libs/bootswatch/flatly.min.css");
		return null;
	};

	/**
	 * Cette route permet de modifier le thème pour la session en cours
	 * 
	 * (theme) => ""
	 */
	public static final Route theme = (request, response) -> {
		request.session().attribute("theme", request.queryParams("theme"));
		return "";
	};

	/**
	 * Cette route retourne la page d'accès aux préférences.
	 * 
	 * () => HTML
	 */
	public static final Route page = (request, response) -> {
		// Récupérer l'utilisateur connecté
		User user = User.findByLogin(request.session().attribute("userLogin"));
		// Générer la page
		return renderTemplate("preferences.html",
				"fromUrl", request.headers("Referer"),
				"lang", SparkUtils.getRequestLang(request),
				"plugins", configuration.getClientPlugins(),
				"name", user.name,
				"showHiddenItems", user.showHiddenItems,
				"showItemTags", user.showItemTags,
				"showItemDescription", user.showItemDescription,
				"showItemThumbnail", user.showItemThumbnail,
				"visibleItemColumns", user.visibleItemColumns == null ? Collections.emptyList() : user.visibleItemColumns);
	};

	/**
	 * Cette méthode enregistre les préférences de l'utilisateur connecté.
	 *
	 * (preferences) => ""
	 *
	 * @see User#update(User)
	 */
	public static final Route save = (request, response) -> {
		// Vérifier le formulaire
		String password = StringUtils.withDefault(request.queryParams("password"), "");
		String passwordConfirmation = StringUtils.withDefault(request.queryParams("passwordConfirmation"), "");
		if (!password.equals(passwordConfirmation))
			return SparkUtils.haltBadRequest();
		// Récupérer l'utilisateur connecté
		User user = User.findByLogin(request.session().attribute("userLogin"));
		// Appliquer le formulaire
		if (StringUtils.isNotBlank(password))
			user.password = CryptoUtils.hashPassword(password);
		user.name = request.queryParams("name");
		user.showHiddenItems = "true".equals(request.queryParams("showHiddenItems"));
		user.showItemTags = "true".equals(request.queryParams("showItemTags"));
		user.showItemDescription = "true".equals(request.queryParams("showItemDescription"));
		user.showItemThumbnail = "true".equals(request.queryParams("showItemThumbnail"));
		user.visibleItemColumns = Optional.ofNullable(request.queryParamsValues("visibleItemColumns[]")).map((a) -> Arrays.asList(a)).orElse(null);
		User.update(user);
		return "";
	};

}

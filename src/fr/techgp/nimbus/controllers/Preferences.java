package fr.techgp.nimbus.controllers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import fr.techgp.nimbus.models.User;
import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Route;
import fr.techgp.nimbus.utils.CryptoUtils;
import fr.techgp.nimbus.utils.StringUtils;

public class Preferences extends Controller {

	/**
	 * Cette route permet de modifier le thème pour la session en cours
	 *
	 * (theme) => ""
	 */
	public static final Route theme = (request, response) -> {
		request.session().attribute("theme", request.queryParameter("theme"));
		return Render.EMPTY;
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
		return Templates.render(request, "preferences.html",
				"fromUrl", request.header("Referer"),
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
		String password = StringUtils.withDefault(request.queryParameter("password"), "");
		String passwordConfirmation = StringUtils.withDefault(request.queryParameter("passwordConfirmation"), "");
		if (!password.equals(passwordConfirmation))
			return Render.badRequest();
		// Récupérer l'utilisateur connecté
		User user = User.findByLogin(request.session().attribute("userLogin"));
		// Appliquer le formulaire
		if (StringUtils.isNotBlank(password))
			user.password = CryptoUtils.hashPassword(password);
		user.name = request.queryParameter("name");
		user.showHiddenItems = "true".equals(request.queryParameter("showHiddenItems"));
		user.showItemTags = "true".equals(request.queryParameter("showItemTags"));
		user.showItemDescription = "true".equals(request.queryParameter("showItemDescription"));
		user.showItemThumbnail = "true".equals(request.queryParameter("showItemThumbnail"));
		user.visibleItemColumns = Optional.ofNullable(request.queryParameterValues("visibleItemColumns[]")).map((a) -> Arrays.asList(a)).orElse(null);
		User.update(user);
		return Render.EMPTY;
	};

}

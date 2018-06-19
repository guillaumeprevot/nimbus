package fr.techgp.nimbus.controllers;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.models.User;
import fr.techgp.nimbus.utils.CryptoUtils;
import fr.techgp.nimbus.utils.SparkUtils;
import fr.techgp.nimbus.utils.StringUtils;
import spark.Route;

public class Users extends Controller {

	/**
	 * Cette route retourne la page de gestion des utilisateurs.
	 * La page n'est accessible qu'aux administrateurs.
	 * 
	 * () => HTML
	 */
	public static final Route page = (request, response) -> {
		return renderTemplate("users.html",
				"lang", SparkUtils.getRequestLang(request),
				"userLogin", request.session().attribute("userLogin"));
	};

	/**
	 * Cette méthode accessible aux administrateurs renvoie la liste des utilisateurs
	 *
	 * () => JSON
	 *
	 * @see User#findAll()
	 * @see Users#toJSON(User)
	 */
	public static final Route list = (request, response) -> {
		// Récupérer les utilisateurs dans la base de données
		return SparkUtils.renderJSONCollection(response, User.findAll(), Users::toJSON);
	};

	/**
	 * Cette méthode accessible aux administrateurs ajoute un utilisateur ":login" dont les infos sont extraites du corps de la requête.
	 *
	 * (:login, form) => ""
	 *
	 * @see User#findByLogin(String)
	 * @see User#insert(User)
	 */
	public static final Route insert = (request, response) -> {
		// Récupérer le login de l'utilisateur à modifier
		String login = request.params(":login");
		if (StringUtils.isBlank(login))
			return SparkUtils.haltBadRequest();
		// Récupérer l'utilisateur
		User user = User.findByLogin(login);
		if (user != null)
			return SparkUtils.haltConflict();
		// Récupérer le mot de passe
		String password = request.queryParams("password");
		if (StringUtils.isBlank(password))
			return SparkUtils.haltBadRequest();
		// Récupérer le formulaire
		user = new User();
		user.login = login;
		user.password = CryptoUtils.hashPassword(password);
		user.name = request.queryParams("name");
		user.admin = SparkUtils.queryParamBoolean(request, "admin", false);
		user.quota = SparkUtils.queryParamInteger(request, "quota", null);
		User.insert(user);
		return SparkUtils.renderJSON(response, toJSON(user));
	};

	/**
	 * Cette méthode accessible aux administrateurs modifie un utilisateur ":login" avec les infos extraites du corps de la requête.
	 *
	 * (:login, form) => ""
	 *
	 * @see User#findByLogin(String)
	 * @see User#update(User)
	 * @see CryptoUtils#hashPassword(String)
	 */
	public static final Route update = (request, response) -> {
		// Récupérer le login de l'utilisateur à modifier
		String login = request.params(":login");
		if (StringUtils.isBlank(login))
			return SparkUtils.haltBadRequest();
		// Récupérer l'utilisateur
		User user = User.findByLogin(login);
		if (user == null)
			return SparkUtils.haltNotFound();
		// Appliquer le formulaire
		String password = request.queryParams("password");
		if (StringUtils.isNotBlank(password))
			user.password = CryptoUtils.hashPassword(password);
		user.name = request.queryParams("name");
		user.admin = SparkUtils.queryParamBoolean(request, "admin", false);
		user.quota = SparkUtils.queryParamInteger(request, "quota", null);
		User.update(user);
		return SparkUtils.renderJSON(response, toJSON(user));
	};

	/**
	 * Cette méthode accessible aux administrateurs supprime un utilisateur ":login".
	 *
	 * (:login) => ""
	 *
	 * @see User#findByLogin(String)
	 * @see User#delete(User)
	 */
	public static final Route delete = (request, response) -> {
		// Récupérer le login de l'utilisateur à supprimer
		String login = request.params(":login");
		if (StringUtils.isBlank(login))
			return SparkUtils.haltBadRequest();
		// Récupérer l'utilisateur
		User user = User.findByLogin(login);
		if (user == null)
			return SparkUtils.haltNotFound();
		User.delete(user);
		// TODO Supprimer le contenu (base+disque) de l'utilisateur dans Users.delete
		return SparkUtils.renderJSON(response, toJSON(user));
	};

	private static final JsonObject toJSON(User u) {
		JsonObject o = new JsonObject();
		o.addProperty("login", u.login);
		o.addProperty("name", u.name);
		o.addProperty("admin", u.admin);
		o.addProperty("quota", u.quota);
		return o;
	}

}

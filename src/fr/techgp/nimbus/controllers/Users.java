package fr.techgp.nimbus.controllers;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.models.User;
import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Route;
import fr.techgp.nimbus.utils.CryptoUtils;
import fr.techgp.nimbus.utils.StringUtils;

public class Users extends Controller {

	/**
	 * Cette route retourne la page de gestion des utilisateurs.
	 * La page n'est accessible qu'aux administrateurs.
	 *
	 * () => HTML
	 */
	public static final Route page = (request, response) -> {
		return Templates.render(request, "users.html");
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
		return Render.json(User.findAll(), Users::toJSON);
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
		String login = request.pathParameter(":login");
		if (StringUtils.isBlank(login))
			return Render.badRequest();
		// Récupérer l'utilisateur
		User user = User.findByLogin(login);
		if (user != null)
			return Render.conflict();
		// Récupérer le mot de passe
		String password = request.queryParameter("password");
		if (StringUtils.isBlank(password))
			return Render.badRequest();
		// Récupérer le formulaire
		user = new User();
		user.login = login;
		user.password = CryptoUtils.hashPassword(password);
		user.name = request.queryParameter("name");
		user.admin = request.queryParameterBoolean("admin", false);
		user.quota = request.queryParameterInteger("quota", null);
		User.insert(user);
		return Render.json(toJSON(user));
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
		String login = request.pathParameter(":login");
		if (StringUtils.isBlank(login))
			return Render.badRequest();
		// Récupérer l'utilisateur
		User user = User.findByLogin(login);
		if (user == null)
			return Render.notFound();
		// Appliquer le formulaire
		String password = request.queryParameter("password");
		if (StringUtils.isNotBlank(password))
			user.password = CryptoUtils.hashPassword(password);
		user.name = request.queryParameter("name");
		user.admin = request.queryParameterBoolean("admin", false);
		user.quota = request.queryParameterInteger("quota", null);
		User.update(user);
		return Render.json(toJSON(user));
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
		String login = request.pathParameter(":login");
		if (StringUtils.isBlank(login))
			return Render.badRequest();
		// Récupérer l'utilisateur
		User user = User.findByLogin(login);
		if (user == null)
			return Render.notFound();
		User.delete(user);
		Item.eraseAll(user.login);
		Controller.eraseAllUserItemFiles(user.login);
		return Render.json(toJSON(user));
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

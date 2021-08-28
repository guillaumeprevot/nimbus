package fr.techgp.nimbus.models;

import java.util.List;

public class User {

	/** L'identifiant de l'utilisateur */
	public String login;
	/** Le mot de passe de l'utilisateur, de la forme "iterations:iv:pbkdf2WithHmacSha1(password,iterations,iv)" */
	public String password;
	/** Le nom que l'utilisateur souhaite voir affiché */
	public String name;
	/** L'utilisateur est-il administrateur ? */
	public boolean admin = true;
	/** Quotas d'espace disque en Mo ou "null" si illimité */
	public Integer quota = null;
	/** L'utilisateur souhaite-t-il afficher les éléments marqués comme cachés ? */
	public boolean showHiddenItems = false;
	/** L'utilisateur souhaite-t-il afficher les tags des éléments ? */
	public boolean showItemTags = true;
	/** L'utilisateur souhaite-t-il afficher les description des éléments */
	public boolean showItemDescription = true;
	/** L'utilisateur souhaite-t-il afficher les miniatures des éléments ? */
	public boolean showItemThumbnail = true;
	/** Liste des colonnes que l'utilisateur souhaite dans la liste des éléments */
	public List<String> visibleItemColumns;

	public User() {
		super();
	}

	public static final User findByLogin(String login) {
		return getDatabase().findUserByLogin(login);
	}

	public static final int count() {
		return getDatabase().countUsers();
	}

	public static final List<User> findAll() {
		return getDatabase().findAllUsers();
	}

	public static final void insert(User user) {
		getDatabase().insertUser(user);
	}

	public static final void update(User user) {
		getDatabase().updateUser(user);
	}

	public static final void delete(User user) {
		getDatabase().deleteUser(user);
	}

	public static final Database getDatabase() {
		return Database.get();
	}

}

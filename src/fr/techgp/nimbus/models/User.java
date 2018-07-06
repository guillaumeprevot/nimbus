package fr.techgp.nimbus.models;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

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
		return read(getCollection().find().filter(Filters.eq("login", login)).first());
	}

	public static final long count() {
		return getCollection().countDocuments();
	}

	public static final List<User> findAll() {
		return getCollection().find().map(User::read).into(new ArrayList<>());
	}

	public static final void insert(User user) {
		getWriteCollection().insertOne(write(user));
	}

	public static final void update(User user) {
		getWriteCollection().updateOne(Filters.eq("login", user.login), new Document("$set", write(user)));
	}

	public static final void delete(User user) {
		getWriteCollection().deleteOne(Filters.eq("login", user.login));
	}

	@SuppressWarnings("unchecked")
	private static final User read(Document document) {
		if (document == null)
			return null;
		User user = new User();
		user.login = document.getString("login");
		user.password = document.getString("password");
		user.name = document.getString("name");
		user.admin = document.getBoolean("admin", true);
		user.quota = document.getInteger("quota");
		user.showItemTags = document.getBoolean("showItemTags", true);
		user.showItemDescription = document.getBoolean("showItemDescription", true);
		user.showItemThumbnail = document.getBoolean("showItemThumbnail", true);
		user.visibleItemColumns = (List<String>) document.get("visibleItemColumns");
		return user;
	}

	private static final Document write(User user) {
		return new Document()
			.append("login", user.login)
			.append("password", user.password)
			.append("name", user.name)
			.append("admin", user.admin)
			.append("quota", user.quota)
			.append("showItemTags", user.showItemTags)
			.append("showItemDescription", user.showItemDescription)
			.append("showItemThumbnail", user.showItemThumbnail)
			.append("visibleItemColumns", user.visibleItemColumns == null ? new ArrayList<>() : user.visibleItemColumns);
	}

	private static final MongoCollection<Document> getCollection() {
		return Mongo.getCollection("users");
	}

	private static final MongoCollection<Document> getWriteCollection() {
		return Mongo.getCollection("users", WriteConcern.JOURNALED);
	}

}

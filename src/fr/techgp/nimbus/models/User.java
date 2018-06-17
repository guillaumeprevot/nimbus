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

	public User() {
		super();
	}

	public static final User findByLogin(String login) {
		return read(getCollection().find().filter(Filters.eq("login", login)).first());
	}

	public static final long count() {
		return getCollection().count();
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

	private static final User read(Document document) {
		if (document == null)
			return null;
		User user = new User();
		user.login = document.getString("login");
		user.password = document.getString("password");
		user.name = document.getString("name");
		user.admin = document.getBoolean("admin", true);
		user.quota = document.getInteger("quota");
		return user;
	}

	private static final Document write(User user) {
		return new Document()
			.append("login", user.login)
			.append("password", user.password)
			.append("name", user.name)
			.append("admin", user.admin)
			.append("quota", user.quota);
	}

	private static final MongoCollection<Document> getCollection() {
		return Mongo.getCollection("users");
	}

	private static final MongoCollection<Document> getWriteCollection() {
		return Mongo.getCollection("users", WriteConcern.JOURNALED);
	}

}

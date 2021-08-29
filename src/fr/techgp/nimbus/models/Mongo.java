package fr.techgp.nimbus.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.bson.BsonInt32;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import fr.techgp.nimbus.utils.StringUtils;

public class Mongo implements Database {

	// Singleton
	private static Mongo instance = null;

	public static final void init(String host, int port, String database) {
		Mongo.instance = new Mongo(host, port, database);
	}

	public static final Mongo get() {
		return Mongo.instance;
	}

	// Connection à MongoDB (mongoClient est "thread-safe")
	private final MongoClient mongoClient; // = new MongoClient("localhost", 27017)
	// Connection à une base de données (et créaton si elle n'existe pas encore)
	private final MongoDatabase mongoDatabase; // = mongoClient.getDatabase("mydb");

	private Mongo(String host, int port, String database) {
		this.mongoClient = new MongoClient(host, port);
		this.mongoDatabase = this.mongoClient.getDatabase(database);
		this.initSequence("items");
	}

	private final MongoCollection<Document> getCollection(String collectionName) {
		return this.mongoDatabase.getCollection(collectionName);
	}

	private final MongoCollection<Document> getWriteCollection(String collectionName) {
		return this.mongoDatabase.withWriteConcern(WriteConcern.JOURNALED).getCollection(collectionName);
	}

	private final void initSequence(String sequence) {
		MongoCollection<Document> collection = getWriteCollection("counters");
		Bson filter = Filters.eq("_id", sequence);
		Document d = collection.find().filter(filter).first();
		if (d == null)
			collection.insertOne(new Document().append("_id", sequence).append("value", 0L));
	}

	private final Long getNextSequence(String sequence) {
		MongoCollection<Document> collection = getWriteCollection("counters");
		Bson filter = Filters.eq("_id", sequence);
		Document operation = new Document().append("$inc", new Document().append("value", 1));
		Document result = collection.findOneAndUpdate(filter, operation);
		return result.getLong("value");
	}

	@Override
	public final void reset() {
		this.getCollection("users").drop();
		this.getCollection("counters").drop();
		this.getCollection("items").drop();
		this.initSequence("items");
	}

	@Override
	public final User findUserByLogin(String login) {
		return readUser(getCollection("users").find().filter(Filters.eq("login", login)).first());
	}

	@Override
	public final int countUsers() {
		return (int) getCollection("users").countDocuments();
	}

	@Override
	public final List<User> findAllUsers() {
		return getCollection("users").find().map(Mongo::readUser).into(new ArrayList<>());
	}

	@Override
	public final void insertUser(User user) {
		getWriteCollection("users").insertOne(writeUser(user));
	}

	@Override
	public final void updateUser(User user) {
		getWriteCollection("users").updateOne(Filters.eq("login", user.login), new Document("$set", writeUser(user)));
	}

	@Override
	public final void deleteUser(User user) {
		getWriteCollection("users").deleteOne(Filters.eq("login", user.login));
	}

	@SuppressWarnings("unchecked")
	private static final User readUser(Document document) {
		if (document == null)
			return null;
		User user = new User();
		user.login = document.getString("login");
		user.password = document.getString("password");
		user.name = document.getString("name");
		user.admin = document.getBoolean("admin", true);
		user.quota = document.getInteger("quota");
		user.showHiddenItems = document.getBoolean("showHiddenItems", false);
		user.showItemTags = document.getBoolean("showItemTags", true);
		user.showItemDescription = document.getBoolean("showItemDescription", true);
		user.showItemThumbnail = document.getBoolean("showItemThumbnail", true);
		user.visibleItemColumns = (List<String>) document.get("visibleItemColumns");
		return user;
	}

	private static final Document writeUser(User user) {
		return new Document()
			.append("login", user.login)
			.append("password", user.password)
			.append("name", user.name)
			.append("admin", user.admin)
			.append("quota", user.quota)
			.append("showHiddenItems", user.showHiddenItems)
			.append("showItemTags", user.showItemTags)
			.append("showItemDescription", user.showItemDescription)
			.append("showItemThumbnail", user.showItemThumbnail)
			.append("visibleItemColumns", user.visibleItemColumns == null ? new ArrayList<>() : user.visibleItemColumns);
	}

	@Override
	public void insertItem(Item item) {
		item.id = getNextSequence("items");
		getWriteCollection("items").insertOne(writeItem(item, null));
	}

	@Override
	public void updateItem(Item item) {
		Document unset = new Document();
		Document set = writeItem(item, unset);
		getWriteCollection("items").updateOne(Filters.eq("_id", item.id), new Document("$set", set).append("$unset", unset));
	}

	@Override
	public void deleteItem(Item item) {
		getWriteCollection("items").updateOne(Filters.eq("_id", item.id), new Document("$set", new Document("deleteDate", item.deleteDate)));
	}

	@Override
	public void restoreItem(Item item) {
		getWriteCollection("items").updateOne(Filters.eq("_id", item.id), new Document("$unset", new Document("deleteDate", "")));
	}

	@Override
	public void eraseItem(Item item) {
		getWriteCollection("items").deleteOne(Filters.eq("_id", item.id));
	}

	@Override
	public void eraseAllItems(String userLogin) {
		getWriteCollection("items").deleteMany(Filters.eq("userLogin", userLogin));
	}

	@Override
	public void notifyFolderContentChanged(Long folderId, int itemCountIncrement) {
		Document modifications = new Document()
				.append("$inc", new Document("content.itemCount", itemCountIncrement))
				.append("$set", new Document("updateDate", new Date()));
		getWriteCollection("items").updateOne(Filters.eq("_id", folderId), modifications);
	}

	@Override
	public Item findItemById(Long id) {
		return getCollection("items").find().filter(Filters.eq("_id", id)).map(Mongo::readItem).first();
	}

	@Override
	public List<Item> findItemsByIds(Collection<Long> ids) {
		return findItems(Filters.in("_id", ids));
	}

	@Override
	public List<Item> findItems(String userLogin, Long parentId, boolean recursive,
			String sortBy, boolean sortAscending, boolean sortFoldersFirst,
			String searchBy, String searchText, Boolean folders, Boolean hidden, Boolean deleted, String extensions) {
		// Filtres de la recherche
		List<Bson> filters = new ArrayList<>(3);
		filters.add(Filters.eq("userLogin", userLogin));

		// Récursivité
		if (! recursive) {
			filters.add(Filters.eq("parentId", parentId));
		} else if (parentId != null) {
			Item item = Item.findById(parentId);
			filters.add(Filters.regex("path", "^" + item.path + item.id + ',', "i"));
		}

		// Texte recherché
		if (StringUtils.isNotBlank(searchText) && !"*".equals(searchText)) {
			if (StringUtils.isBlank(searchBy)) {
				// Par défaut, chercher dans le nom et les tags
				filters.add(Filters.or(
						Filters.elemMatch("tags", new Document("$regex", searchText).append("$options", "i")),
						Filters.regex("name", ".*" + Pattern.quote(searchText) + ".*", "i")));
			} else {
				// Sinon, chercher dans la propriété demandée
				filters.add(Filters.regex(searchBy, ".*" + Pattern.quote(searchText) + ".*", "i"));
			}
		}

		// Dossiers, Fichiers ou tout
		if (folders != null)
			filters.add(Filters.eq("folder", folders.booleanValue()));

		// Masqués, non masqués ou tout
		if (Boolean.TRUE.equals(hidden))
			filters.add(Filters.eq("hidden", true)); // hidden=true
		else if (Boolean.FALSE.equals(hidden))
			filters.add(Filters.ne("hidden", true)); // hidden=false ou hidden=absent

		// Eléments supprimés, non supprimés ou tout
		if (deleted != null)
			filters.add(Filters.exists("deleteDate", deleted.booleanValue()));

		// Restriction par extensions pour les fichiers (exemple : rechercher 'Toto' dans les dossiers ou les fichiers '.mp3')
		if (StringUtils.isNotBlank(extensions))
			filters.add(Filters.or(Filters.eq("folder", true), Filters.regex("name", ".*\\.(" + extensions.replace(",", "|") + ")$", "i")));

		// Tri de la recherche
		List<Bson> sorts = new ArrayList<>();
		// Renvoyer les dossiers en premier, sauf si on trie selon ce champ justement
		if (sortFoldersFirst && !"folder".equals(sortBy))
			sorts.add(Sorts.descending("folder"));
		// Puis trier selon la propriété demandée
		if (StringUtils.isNotBlank(sortBy)) {
			String mongoSortBy = "id".equals(sortBy) ? "_id" : sortBy;
			if (sortAscending)
				sorts.add(Sorts.ascending(mongoSortBy));
			else
				sorts.add(Sorts.descending(mongoSortBy));
		}
		// Puis trier alphabétiquement
		if (!"name".equals(sortBy))
			sorts.add(Sorts.ascending("name"));
		// Puis trier par id pour que le tri soit consistant
		if (!"id".equals(sortBy))
			sorts.add(Sorts.ascending("id"));
		Bson sort = Sorts.orderBy(sorts);

		// Transformer en une liste d'items
		return getCollection("items").find().filter(Filters.and(filters)).sort(sort).map(Mongo::readItem).into(new ArrayList<>());
	}

	public List<Item> findItems(Bson filter) {
		return getCollection("items").find().filter(filter).map(Mongo::readItem).into(new ArrayList<>());
	}

	@Override
	public int countItems(String userLogin, Long parentId) {
		return (int) getCollection("items").countDocuments(new Document("userLogin", userLogin).append("parentId", parentId));
	}

	@Override
	public int trashItemCount(String userLogin) {
		return (int) getCollection("items").countDocuments(Filters.and(Filters.eq("userLogin", userLogin), Filters.exists("deleteDate")));
	}

	@Override
	public boolean hasItem(String userLogin, Long itemId) {
		return getCollection("items").countDocuments(new Document("userLogin", userLogin).append("_id", itemId)) == 1;
	}

	@Override
	public boolean hasItemWithName(String userLogin, Long parentId, String name) {
		return getCollection("items").countDocuments(new Document("userLogin", userLogin).append("parentId", parentId).append("name", name)) > 0;
	}

	@Override
	public boolean hasItemsWithNames(String userLogin, Long parentId, String... names) {
		return getCollection("items").countDocuments(new Document("userLogin", userLogin).append("parentId", parentId).append("name", new Document("$in", Arrays.asList(names)))) > 0;
	}

	@Override
	public Item findItemWithName(String userLogin, Long parentId, String name) {
		return getCollection("items").find(new Document("userLogin", userLogin).append("parentId", parentId).append("name", name)).map(Mongo::readItem).first();
	}

	@Override
	public void forEachItemTag(String userLogin, Consumer<String> consumer) {
		getCollection("items").distinct("tags", String.class).filter(Filters.eq("userLogin", userLogin))
			.into(new ArrayList<String>()).stream().sorted().forEach(consumer);
	}

	@Override
	public void forEachItemTagWithCount(String userLogin, boolean orderByCount, BiConsumer<String, Integer> consumer) {
		Document unwind = new Document("$unwind", "$tags");
		Document match = new Document("$match", new Document("userLogin", userLogin));
		Document group = new Document("$group", new Document("_id", "$tags").append("count", new Document("$sum", 1)));
		Document sort = new Document("$sort", (orderByCount ? new Document("count", new BsonInt32(-1)) : new Document()).append("_id", new BsonInt32(1)));
		Consumer<Document> documentConsumer = (document) -> consumer.accept(document.getString("_id"), document.getInteger("count"));
		getCollection("items").aggregate(Arrays.asList(unwind, match, group, sort)).forEach(documentConsumer);
	}

	@Override
	public long calculateUsedSpace(String userLogin) {
		Document match = new Document("$match", new Document("userLogin", userLogin));
		Document group = new Document("$group", new Document("_id", null).append("total", new Document("$sum", "$content.length")));
		Document result = getCollection("items").aggregate(Arrays.asList(match, group)).first();
		return Optional.ofNullable(result).map(r -> ((Number) r.get("total")).longValue()).orElse(0L);
	}

	@SuppressWarnings("unchecked")
	private static final Item readItem(Document document) {
		if (document == null)
			return null;
		Item item = new Item();
		item.id = document.getLong("_id");
		item.parentId = document.getLong("parentId");
		item.path = document.getString("path");
		item.userLogin = document.getString("userLogin");
		item.folder = document.getBoolean("folder").booleanValue();
		item.hidden = document.getBoolean("hidden", false);
		item.name = document.getString("name");
		item.tags = (List<String>) document.get("tags");
		item.sharedDate = document.getDate("sharedDate");
		item.sharedPassword = document.getString("sharedPassword");
		item.sharedDuration = document.getInteger("sharedDuration");
		item.createDate = document.getDate("createDate");
		item.updateDate = document.getDate("updateDate");
		item.deleteDate = document.getDate("deleteDate");
		item.metadatas.clear();
		for (Map.Entry<String, Object> entry : ((Document) document.get("content")).entrySet()) {
			if (entry.getValue() instanceof String)
				item.metadatas.put(entry.getKey(), (String) entry.getValue());
			else if (entry.getValue() instanceof Boolean)
				item.metadatas.put(entry.getKey(), (Boolean) entry.getValue());
			else if (entry.getValue() instanceof Integer)
				item.metadatas.put(entry.getKey(), (Integer) entry.getValue());
			else if (entry.getValue() instanceof Long)
				item.metadatas.put(entry.getKey(), (Long) entry.getValue());
			else if (entry.getValue() instanceof Double)
				item.metadatas.put(entry.getKey(), (Double) entry.getValue());
			else if (entry.getValue() instanceof Date)
				item.metadatas.put(entry.getKey(), ((Date) entry.getValue()).getTime());
			else if (entry.getValue() != null)
				throw new UnsupportedOperationException("Unsupported BSON value " + entry.getValue().getClass().getName());
		}
		Item.check(item);
		return item;
	}

	private static final Document writeItem(Item item, Document unset) {
		Item.check(item);
		Document content = new Document();
		item.metadatas.visit(content::append, content::append, content::append, content::append, content::append);
		Document d = new Document()
			.append("_id", item.id)
			.append("parentId", item.parentId)
			.append("path", item.path)
			.append("userLogin", item.userLogin)
			.append("folder", item.folder)
			.append("hidden", item.hidden)
			.append("name", item.name)
			.append("tags", item.tags == null ? new ArrayList<>() : item.tags)
			.append("createDate", item.createDate)
			.append("updateDate", item.updateDate)
			.append("content", content);

		if (item.sharedDate != null)
			d.append("sharedDate", item.sharedDate);
		else if (unset != null)
			unset.append("sharedDate", "");

		if (item.sharedDuration != null)
			d.append("sharedDuration", item.sharedDuration);
		else if (unset != null)
			unset.append("sharedDuration", "");

		if (item.sharedPassword != null)
			d.append("sharedPassword", item.sharedPassword);
		else if (unset != null)
			unset.append("sharedPassword", "");

		if (item.deleteDate != null)
			d.append("deleteDate", item.deleteDate);
		else if (unset != null)
			unset.append("deleteDate", "");
		return d;
	}

}

package fr.techgp.nimbus.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bson.BsonInt32;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;

import fr.techgp.nimbus.utils.StringUtils;

public class Item {

	/** Identifiant unique de l'élément */
	public Long id;
	/** Identifiant du parent auquel cet élément appartient (= gestion de l'arborescence) */
	public Long parentId;
	/** Le chemin de l'élément dans l'arboresence, par exemple "" ou "id1," ou "id1,id2,id3," */
	public String path;
	/** Login de l'utilisateur ayant créé cet élément */
	public String userLogin;
	/** Indique si l'élément est un dossier ou un fichier */
	public boolean folder;
	/** Nom de l'élement */
	public String name;
	/** Date de création de l'élément */
	public Date createDate;
	/** Date de dernière modification de l'élement */
	public Date updateDate;
	/** Date de suppression de l'élement (mis à la corbeille) */
	public Date deleteDate;
	/** Liste des tags, séparés par des virgules */
	public List<String> tags;
	/** Contenu de l'élément spécifique à la facet (largeur et hauteur d'une image par exemple) */
	public Document content;

	public Item() {
		super();
	}

	public static final void insert(Item item) {
		getWriteCollection().insertOne(write(item));
	}

	public static final void update(Item item) {
		getWriteCollection().updateOne(Filters.eq("_id", item.id), new Document("$set", write(item)));
	}

	public static final void delete(Item item) {
		item.deleteDate = new Date();
		getWriteCollection().updateOne(Filters.eq("_id", item.id), new Document("$set", new Document("deleteDate", item.deleteDate)));
	}

	public static final void restore(Item item) {
		item.deleteDate = null;
		getWriteCollection().updateOne(Filters.eq("_id", item.id), new Document("$unset", new Document("deleteDate", "")));
	}

	public static final void erase(Item item) {
		getWriteCollection().deleteOne(Filters.eq("_id", item.id));
	}

	public static final Item add(String userLogin, Long parentId, boolean folder, String name, Consumer<Item> init) {
		Item parent = null;
		if (parentId != null) {
			parent = findById(parentId);
			if (parent == null || !parent.userLogin.equals(userLogin))
				return null;
		}
		return add(userLogin, parent, folder, name, init);
	}

	public static final Item add(String userLogin, Item checkedParent, boolean folder, String name, Consumer<Item> init) {
		Item child = new Item();
		child.id = Mongo.getNextSequence("items");
		child.parentId = checkedParent == null ? null : checkedParent.id;
		child.path = checkedParent == null ? "" : (checkedParent.path + checkedParent.id + ',');
		child.userLogin = userLogin;
		child.folder = folder;
		child.name = name;
		child.createDate = new Date();
		child.updateDate = new Date();
		child.deleteDate = null;
		child.tags = null;
		child.content = new Document();
		if (child.folder)
			child.content.append("itemCount", 0);
		else
			child.content.append("length", 0L);
		if (init != null)
			init.accept(child);
		Item.insert(child);
		if (child.parentId != null)
			notifyFolderContentChanged(child.parentId, 1);
		return child;
	}

	public static final Item duplicate(Item item, String name) {
		Item duplicate = new Item();
		duplicate.id = Mongo.getNextSequence("items");
		duplicate.parentId = item.parentId;
		duplicate.path = item.path;
		duplicate.userLogin = item.userLogin;
		duplicate.folder = item.folder;
		duplicate.name = name;
		duplicate.createDate = new Date();
		duplicate.updateDate = new Date();
		duplicate.deleteDate = null;
		duplicate.tags = item.tags == null ? null : new ArrayList<>(item.tags);
		duplicate.content = Document.parse(item.content.toJson()); // ensure deep metadatas copy
		if (duplicate.folder)
			duplicate.content.append("itemCount", 0); // no recursive duplicate
		Item.insert(duplicate);
		if (duplicate.parentId != null)
			Item.notifyFolderContentChanged(duplicate.parentId, 1);
		return duplicate;
	}

	public static final boolean notifyFolderContentChanged(Long folderId, int itemCountIncrement) {
		Document modifications = new Document()
				.append("$inc", new Document("content.itemCount", itemCountIncrement))
				.append("$set", new Document("updateDate", new Date()));
		return 1 == getWriteCollection().updateOne(Filters.eq("_id", folderId), modifications).getModifiedCount();
	}

	public static final void updatePath(String oldPath, String newPath, Long itemId) {
		// Récupérer la collection
		MongoCollection<Document> collection = getWriteCollection();
		// Marquer que l'élément est en cours de déplacement (pour la récupération en cas d'interruption)
		collection.updateOne(Filters.eq("_id", itemId), new Document("$set", new Document("newPath", newPath)));
		// Préparer les substitutions de chemin pour les sous-éléments
		String fullOldPath = oldPath + itemId + ",";
		String fullNewPath = newPath + itemId + ",";
		// Préparer le consumer qui mettre à jour le chemin d'un sous-éléments à la fois
		Consumer<Document> updatePathConsumer = (document) -> {
			// Nouveau chemin de l'élément
			String path = fullNewPath + document.getString("path").substring(fullOldPath.length());
			// Update pour cet élément
			collection.updateOne(Filters.eq("_id", document.getLong("_id")), new Document("$set", new Document("path", path)));
		};
		// Rechercher les sous-éléments pointant sur l'ancien chemin
		collection.find().filter(Filters.regex("path", "^" + fullOldPath)).projection(Projections.include("_id", "path")).forEach(updatePathConsumer);
		// Une fois tous les sous-éléments mis à jour, on peut stocker le nouveau chemin de l'élément déplacé
		collection.updateOne(Filters.eq("_id", itemId), new Document("$set", new Document("path", newPath)).append("$unset", new Document("newPath", "")));
	}

	public static final Item findById(Long id) {
		return getCollection().find().filter(Filters.eq("_id", id)).map(Item::read).first();
	}

	/**
	 * Cette méthode est la fonction qui extrait de la base les éléments correspondants à la recherche
	 *
	 * @param userLogin utilisateur consultant les données
	 * @param parentId dossier dans lequel il faut rechercher
	 * @param recursive true pour aller chercher dans les sous-dossier
	 * @param sortBy nom de la propriété selon laquelle on tri les résultats
	 * @param sortAscending true pour trier dans l'ordre ascendant, false sinon
	 * @param searchBy nom de la propriété dans laquelle chercher (par exemple content.author), on null pour le comportement par défaut (name + tags)
	 * @param searchText le texte recherché, ou vide par défaut
	 * @param folders true/false/null pour chercher un dossier, un fichier ou peu importe
	 * @param deleted true/false/null pour chercher un élément supprimé, un élément non supprimé ou peu importe
	 * @param extensions liste des extensions, séparées par "," ou null pour ne pas limiter la recherche
	 * @return la liste des éléments correspondant à la rechercher
	 */
	public static final List<Item> findAll(String userLogin, Long parentId, boolean recursive, String sortBy,
			boolean sortAscending, String searchBy, String searchText, Boolean folders, Boolean deleted, String extensions) {
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
						Filters.regex("name", ".*" + searchText + ".*", "i")));
			} else {
				// Sinon, chercher dans la propriété demandée
				filters.add(Filters.regex("name", ".*" + searchText + ".*", "i"));
			}
		}

		// Dossiers, Fichiers ou tout
		if (folders != null)
			filters.add(Filters.eq("folder", folders.booleanValue()));

		// Eléments supprimés, non supprimés ou tout
		if (deleted != null)
			filters.add(Filters.exists("deleteDate", deleted.booleanValue()));

		// Extensions recherchées
		if (StringUtils.isNotBlank(extensions))
			filters.add(Filters.or(Filters.eq("folder", true), Filters.regex("name", ".*\\.(" + extensions.replace(",", "|") + ")", "i")));

		// Tri de la recherche
		Bson sort;
		if (StringUtils.isBlank(sortBy))
			sort = Sorts.orderBy(Sorts.descending("folder"), Sorts.ascending("name"));
		else if (sortAscending)
			sort = Sorts.ascending(sortBy);
		else
			sort = Sorts.descending(sortBy);

		// Transformer en une liste d'items
		return getCollection().find().filter(Filters.and(filters)).sort(sort).map(Item::read).into(new ArrayList<>());
	}

	public static final List<Item> findAll(Bson filter) {
		return getCollection().find().filter(filter).map(Item::read).into(new ArrayList<>());
	}

	public static final int count(String userLogin, Long parentId) {
		return (int) getCollection().count(new Document("userLogin", userLogin).append("parentId", parentId));
	}

	public static final int trashCount(String userLogin) {
		return (int) getCollection().count(Filters.and(Filters.eq("userLogin", userLogin), Filters.exists("deleteDate")));
	}

	public static final boolean hasItem(String userLogin, Long itemId) {
		return getCollection().count(new Document("userLogin", userLogin).append("_id", itemId)) == 1;
	}

	public static final boolean hasItemWithName(String userLogin, Long parentId, String name) {
		return getCollection().count(new Document("userLogin", userLogin).append("parentId", parentId).append("name", name)) > 0;
	}

	public static final boolean hasItemsWithNames(String userLogin, Long parentId, String... names) {
		return getCollection().count(new Document("userLogin", userLogin).append("parentId", parentId).append("name", new Document("$in", Arrays.asList(names)))) > 0;
	}

	public static final Item findItemWithName(String userLogin, Long parentId, String name) {
		return getCollection().find(new Document("userLogin", userLogin).append("parentId", parentId).append("name", name)).map(Item::read).first();
	}

	public static final void forEachTag(String userLogin, Consumer<String> consumer) {
		getCollection().distinct("tags", String.class).filter(Filters.eq("userLogin", userLogin))
			.into(new ArrayList<String>()).stream().sorted().forEach(consumer);
	}

	public static final void forEachTagWithCount(String userLogin, boolean orderByCount, BiConsumer<String, Integer> consumer) {
		Document unwind = new Document("$unwind", "$tags");
		Document match = new Document("$match", new Document("userLogin", userLogin));
		Document group = new Document("$group", new Document("_id", "$tags").append("count", new Document("$sum", 1)));
		Document sort = new Document("$sort", (orderByCount ? new Document("count", new BsonInt32(-1)) : new Document()).append("_id", new BsonInt32(1)));
		Consumer<Document> documentConsumer = (document) -> consumer.accept(document.getString("_id"), document.getInteger("count"));
		getCollection().aggregate(Arrays.asList(unwind, match, group, sort)).forEach(documentConsumer);
	}

	public static final long calculateUsedSpace(String userLogin) {
		Document match = new Document("$match", new Document("userLogin", userLogin));
		Document group = new Document("$group", new Document("_id", null).append("total", new Document("$sum", "$content.length")));
		Document result = getCollection().aggregate(Arrays.asList(match, group)).first();
		return Optional.ofNullable(result).map(r -> r.getLong("total")).orElse(0L);
	}

	@SuppressWarnings("unchecked")
	private static final Item read(Document document) {
		if (document == null)
			return null;
		Item item = new Item();
		item.id = document.getLong("_id");
		item.parentId = document.getLong("parentId");
		item.path = document.getString("path");
		item.userLogin = document.getString("userLogin");
		item.folder = document.getBoolean("folder").booleanValue();
		item.name = document.getString("name");
		item.tags = (List<String>) document.get("tags");
		item.createDate = document.getDate("createDate");
		item.updateDate = document.getDate("updateDate");
		item.deleteDate = document.getDate("deleteDate");
		item.content = (Document) document.get("content");
		check(item);
		return item;
	}

	private static final Document write(Item item) {
		check(item);
		Document d = new Document()
			.append("_id", item.id)
			.append("parentId", item.parentId)
			.append("path", item.path)
			.append("userLogin", item.userLogin)
			.append("folder", item.folder)
			.append("name", item.name)
			.append("tags", item.tags == null ? new ArrayList<>() : item.tags)
			.append("createDate", item.createDate)
			.append("updateDate", item.updateDate)
			.append("content", item.content);
		if (item.deleteDate != null)
			d.append("deleteDate", item.deleteDate);
		return d;
	}

	private static final void check(Item item) {
		BiConsumer<Boolean, String> checker = (test, message) -> {
			try {
				if (!Boolean.TRUE.equals(test))
					throw new InputMismatchException(message);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw ex;
			}
		};
		checker.accept(item.id != null, "id ne doit pas être null");
		checker.accept(item.path != null, "path ne doit pas être null");
		checker.accept(item.path.isEmpty() || item.path.endsWith(","), "path doit finir par une virgule ou être vide");
		checker.accept(StringUtils.isNotBlank(item.userLogin), "userLogin ne doit pas être vide");
		checker.accept(StringUtils.isNotBlank(item.name), "name ne doit pas être vide");
	}

	private static final MongoCollection<Document> getCollection() {
		return Mongo.getCollection("items");
	}

	private static final MongoCollection<Document> getWriteCollection() {
		return Mongo.getCollection("items", WriteConcern.JOURNALED);
	}

}

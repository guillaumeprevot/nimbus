package fr.techgp.nimbus.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.bson.BsonInt32;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
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
	/** Indique si l'élément est élément caché */
	public boolean hidden;
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
	/** La date du partage ou null si l'élément n'est pas partagé */
	public Date sharedDate;
	/** Le mot de passe du partage, de la forme 30 caractères alpha-numérique aléatoires */
	public String sharedPassword;
	/** La durée en minutes du partage ou null pour un partage illimité */
	public Integer sharedDuration;
	/** Contenu de l'élément spécifique à la facet (largeur et hauteur d'une image par exemple) */
	public Document content;

	public Item() {
		super();
	}

	public static final void insert(Item item) {
		getWriteCollection().insertOne(write(item, null));
	}

	public static final void update(Item item) {
		Document unset = new Document();
		Document set = write(item, unset);
		getWriteCollection().updateOne(Filters.eq("_id", item.id), new Document("$set", set).append("$unset", unset));
	}

	public static final void delete(Item item) {
		// Marquer comme supprimé
		item.deleteDate = new Date();
		getWriteCollection().updateOne(Filters.eq("_id", item.id), new Document("$set", new Document("deleteDate", item.deleteDate)));
		// Décrémenter le nombre d'éléments du parent
		if (item.parentId != null)
			Item.notifyFolderContentChanged(item.parentId, -1);
	}

	public static final void restore(Item item) {
		// Retirer l'indicateur de suppression
		item.deleteDate = null;
		getWriteCollection().updateOne(Filters.eq("_id", item.id), new Document("$unset", new Document("deleteDate", "")));
		//Incrémenter le nombre d'éléments du parent
		if (item.parentId != null)
			Item.notifyFolderContentChanged(item.parentId, 1);
	}

	public static final void erase(Item item) {
		// Supprimer définitivement l'élément
		getWriteCollection().deleteOne(Filters.eq("_id", item.id));
		// Décrémenter le nombre d'éléments du parent si "item" n'était pas encore supprimé
		if (item.deleteDate == null && item.parentId != null)
			Item.notifyFolderContentChanged(item.parentId, -1);
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
		child.hidden = false;
		child.name = name;
		child.createDate = new Date();
		child.updateDate = new Date();
		child.deleteDate = null;
		child.tags = null;
		child.sharedDate = null;
		child.sharedPassword = null;
		child.sharedDuration = null;
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
		duplicate.hidden = item.hidden;
		duplicate.name = name;
		duplicate.createDate = new Date();
		duplicate.updateDate = new Date();
		duplicate.deleteDate = null;
		duplicate.tags = item.tags == null ? null : new ArrayList<>(item.tags);
		duplicate.sharedDate = null;
		duplicate.sharedPassword = null;
		duplicate.sharedDuration = null;
		duplicate.content = Document.parse(item.content.toJson()); // ensure deep metadatas copy
		if (duplicate.folder)
			duplicate.content.append("itemCount", 0); // no recursive duplicate
		Item.insert(duplicate);
		if (duplicate.parentId != null)
			Item.notifyFolderContentChanged(duplicate.parentId, 1);
		return duplicate;
	}

	public static final String findName(Item item, String firstPattern, String nextPattern) {
		// Renommage l'élément en fonction des patterns donnés
		String newName = firstPattern.replace("{0}", item.name).replace("{1}", Integer.toString(1));
		int i = 1;
		while (Item.hasItemWithName(item.userLogin, item.parentId, newName)) {
			i++;
			newName = nextPattern.replace("{0}", item.name).replace("{1}", Integer.toString(i));
		}
		return newName;
	}

	public static final void rename(Item item, String newName) {
		// Renommage l'élément en fonction des patterns donnés
		item.name = newName;
		// pas de changement de updateDate car le contenu reste le même
		//item.updateDate = new Date();
		Item.update(item);
		// Ajuster la date de modification du dossier parent
		if (item.parentId != null)
			Item.notifyFolderContentChanged(item.parentId, 0);
	}

	public static final void move(Item item, Item targetParent, Supplier<String> newName) {
		// Retirer l'élément de son ancien parent
		if (item.parentId != null)
			Item.notifyFolderContentChanged(item.parentId, -1);
		// Mettre à jour l'élément
		item.parentId = targetParent == null ? null : targetParent.id;
		item.path = targetParent == null ? "" : (targetParent.path + targetParent.id + ",");
		if (newName != null)
			item.name = newName.get();
		// pas de changement de updateDate car le contenu reste le même
		// item.updateDate = new Date();
		Item.update(item);
		// Ajouter l'élément dans son nouveau parent
		if (item.parentId != null)
			Item.notifyFolderContentChanged(item.parentId, 1);
	}

	public static final boolean notifyFolderContentChanged(Long folderId, int itemCountIncrement) {
		Document modifications = new Document()
				.append("$inc", new Document("content.itemCount", itemCountIncrement))
				.append("$set", new Document("updateDate", new Date()));
		return 1 == getWriteCollection().updateOne(Filters.eq("_id", folderId), modifications).getModifiedCount();
	}

	public static final Item findById(Long id) {
		return getCollection().find().filter(Filters.eq("_id", id)).map(Item::read).first();
	}

	public static final List<Item> findByIds(Collection<Long> ids) {
		return findAll(Filters.in("_id", ids));
	}

	/**
	 * Cette méthode est la fonction qui extrait de la base les éléments correspondants à la recherche
	 *
	 * @param userLogin utilisateur consultant les données
	 * @param parentId dossier dans lequel il faut rechercher
	 * @param recursive true pour aller chercher dans les sous-dossier
	 * @param sortBy nom de la propriété selon laquelle on tri les résultats
	 * @param sortAscending true pour trier dans l'ordre ascendant, false sinon
	 * @param searchBy nom de la propriété dans laquelle chercher (par exemple content.artist), on null pour le comportement par défaut (name + tags)
	 * @param searchText le texte recherché, ou vide par défaut
	 * @param folders true/false/null pour chercher un dossier, un fichier ou peu importe
	 * @param hidden true/false/null pour chercher un élément masqué, un élément non masqué ou peu importe
	 * @param deleted true/false/null pour chercher un élément supprimé, un élément non supprimé ou peu importe
	 * @param extensions liste des extensions, séparées par "," ou null pour ne pas limiter la recherche
	 * @return la liste des éléments correspondant à la rechercher
	 */
	public static final List<Item> findAll(String userLogin, Long parentId, boolean recursive, String sortBy,
			boolean sortAscending, String searchBy, String searchText, Boolean folders, Boolean hidden, Boolean deleted, String extensions) {
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

		// Extensions recherchées
		if (StringUtils.isNotBlank(extensions))
			filters.add(Filters.or(Filters.eq("folder", true), Filters.regex("name", ".*\\.(" + extensions.replace(",", "|") + ")$", "i")));

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
		return (int) getCollection().countDocuments(new Document("userLogin", userLogin).append("parentId", parentId));
	}

	public static final int trashCount(String userLogin) {
		return (int) getCollection().countDocuments(Filters.and(Filters.eq("userLogin", userLogin), Filters.exists("deleteDate")));
	}

	public static final boolean hasItem(String userLogin, Long itemId) {
		return getCollection().countDocuments(new Document("userLogin", userLogin).append("_id", itemId)) == 1;
	}

	public static final boolean hasItemWithName(String userLogin, Long parentId, String name) {
		return getCollection().countDocuments(new Document("userLogin", userLogin).append("parentId", parentId).append("name", name)) > 0;
	}

	public static final boolean hasItemsWithNames(String userLogin, Long parentId, String... names) {
		return getCollection().countDocuments(new Document("userLogin", userLogin).append("parentId", parentId).append("name", new Document("$in", Arrays.asList(names)))) > 0;
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
		return Optional.ofNullable(result).map(r -> ((Number) r.get("total")).longValue()).orElse(0L);
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
		item.hidden = document.getBoolean("hidden", false);
		item.name = document.getString("name");
		item.tags = (List<String>) document.get("tags");
		item.sharedDate = document.getDate("sharedDate");
		item.sharedPassword = document.getString("sharedPassword");
		item.sharedDuration = document.getInteger("sharedDuration");
		item.createDate = document.getDate("createDate");
		item.updateDate = document.getDate("updateDate");
		item.deleteDate = document.getDate("deleteDate");
		item.content = (Document) document.get("content");
		check(item);
		return item;
	}

	private static final Document write(Item item, Document unset) {
		check(item);
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
			.append("content", item.content);

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
		checker.accept(item.parentId != null || item.path.isEmpty(), "path doit être vide pour les éléments à la racine");
		checker.accept(item.parentId == null || item.path.endsWith(item.parentId + ","), "path doit correspondre au parent des sous-éléments");
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

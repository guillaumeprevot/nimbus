package fr.techgp.nimbus.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
	public final Metadatas metadatas = new Metadatas();

	public Item() {
		super();
	}

	public static final void insert(Item item) {
		getDatabase().insertItem(item);
	}

	public static final void update(Item item) {
		getDatabase().updateItem(item);
	}

	public static final void delete(Item item) {
		// Marquer comme supprimé
		item.deleteDate = new Date();
		// Envoyer en base
		getDatabase().deleteItem(item);
		// Décrémenter le nombre d'éléments du parent
		if (item.parentId != null)
			Item.notifyFolderContentChanged(item.parentId, -1);
	}

	public static final void restore(Item item) {
		// Retirer l'indicateur de suppression
		item.deleteDate = null;
		// Restaurer en base
		getDatabase().restoreItem(item);
		// Incrémenter le nombre d'éléments du parent
		if (item.parentId != null)
			Item.notifyFolderContentChanged(item.parentId, 1);
	}

	public static final void erase(Item item) {
		// Supprimer définitivement l'élément
		getDatabase().eraseItem(item);
		// Décrémenter le nombre d'éléments du parent si "item" n'était pas encore supprimé
		if (item.deleteDate == null && item.parentId != null)
			Item.notifyFolderContentChanged(item.parentId, -1);
	}

	public static final void eraseAll(String userLogin) {
		// Supprimer définitivement les éléments
		getDatabase().eraseAllItems(userLogin);
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
		if (child.folder)
			child.metadatas.put("itemCount", 0);
		else
			child.metadatas.put("length", 0L);
		if (init != null)
			init.accept(child);
		getDatabase().insertItem(child);
		if (child.parentId != null)
			Item.notifyFolderContentChanged(child.parentId, 1);
		return child;
	}

	public static final Item duplicate(Item item, String name) {
		Item duplicate = new Item();
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
		duplicate.metadatas.copy(item.metadatas);
		if (duplicate.folder)
			duplicate.metadatas.put("itemCount", 0); // no recursive duplicate
		getDatabase().insertItem(duplicate);
		if (duplicate.parentId != null)
			Item.notifyFolderContentChanged(duplicate.parentId, 1);
		return duplicate;
	}

	public static final String findName(Item item, String firstPattern, String nextPattern) {
		// Renommage l'élément en fonction des patterns donnés
		String newName = firstPattern.replace("{0}", item.name).replace("{1}", Integer.toString(1));
		int i = 1;
		while (getDatabase().hasItemWithName(item.userLogin, item.parentId, newName)) {
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
		getDatabase().updateItem(item);
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
		getDatabase().updateItem(item);
		// Ajouter l'élément dans son nouveau parent
		if (item.parentId != null)
			Item.notifyFolderContentChanged(item.parentId, 1);
	}

	public static final void notifyFolderContentChanged(Long folderId, int itemCountIncrement) {
		getDatabase().notifyFolderContentChanged(folderId, itemCountIncrement);
	}

	public static final Item findById(Long id) {
		return getDatabase().findItemById(id);
	}

	public static final List<Item> findByIds(Collection<Long> ids) {
		return getDatabase().findItemsByIds(ids);
	}

	/**
	 * Cette méthode est la fonction qui extrait de la base les éléments correspondants à la recherche
	 *
	 * @param userLogin utilisateur consultant les données
	 * @param parentId dossier dans lequel il faut rechercher
	 * @param recursive true pour aller chercher dans les sous-dossier
	 * @param sortBy nom de la propriété selon laquelle on tri les résultats
	 * @param sortAscending true pour trier dans l'ordre ascendant, false sinon
	 * @param sortFoldersFirst true pour grouper les dossiers d'abord, false sinon
	 * @param searchBy nom de la propriété dans laquelle chercher (par exemple content.artist), on null pour le comportement par défaut (name + tags)
	 * @param searchText le texte recherché, ou vide par défaut
	 * @param folders true/false/null pour chercher un dossier, un fichier ou peu importe
	 * @param hidden true/false/null pour chercher un élément masqué, un élément non masqué ou peu importe
	 * @param deleted true/false/null pour chercher un élément supprimé, un élément non supprimé ou peu importe
	 * @param extensions liste des extensions, séparées par "," ou null pour ne pas limiter la recherche
	 * @return la liste des éléments correspondant à la rechercher
	 */
	public static final List<Item> findAll(String userLogin, Long parentId, boolean recursive,
			String sortBy, boolean sortAscending, boolean sortFoldersFirst,
			String searchBy, String searchText, Boolean folders, Boolean hidden, Boolean deleted, String extensions) {
		return getDatabase().findItems(userLogin, parentId, recursive, sortBy, sortAscending, sortFoldersFirst, searchBy, searchText, folders, hidden, deleted, extensions);
	}

	public static final int count(String userLogin, Long parentId) {
		return getDatabase().countItems(userLogin, parentId);
	}

	public static final int trashCount(String userLogin) {
		return getDatabase().trashItemCount(userLogin);
	}

	public static final boolean hasItem(String userLogin, Long itemId) {
		return getDatabase().hasItem(userLogin, itemId);
	}

	public static final boolean hasItemWithName(String userLogin, Long parentId, String name) {
		return getDatabase().hasItemWithName(userLogin, parentId, name);
	}

	public static final boolean hasItemsWithNames(String userLogin, Long parentId, String... names) {
		return getDatabase().hasItemsWithNames(userLogin, parentId, names);
	}

	public static final Item findItemWithName(String userLogin, Long parentId, String name) {
		return getDatabase().findItemWithName(userLogin, parentId, name);
	}

	public static final void forEachTag(String userLogin, Consumer<String> consumer) {
		getDatabase().forEachItemTag(userLogin, consumer);
	}

	public static final void forEachTagWithCount(String userLogin, BiConsumer<String, Integer> consumer) {
		getDatabase().forEachItemTagWithCount(userLogin, consumer);
	}

	public static final long calculateUsedSpace(String userLogin) {
		return getDatabase().calculateUsedSpace(userLogin);
	}

	public static final void calculateStatistics(String userLogin, Long parentId, boolean recursive, BiConsumer<String, Long> consumer) {
		getDatabase().calculateStatistics(userLogin, parentId, recursive, consumer);
	}

	public static final void check(Item item) {
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
		checker.accept(item.userLogin != null && !item.userLogin.isBlank(), "userLogin ne doit pas être vide");
		checker.accept(item.name != null && !item.name.isBlank(), "name ne doit pas être vide");
	}

	public static final Database getDatabase() {
		return Database.get();
	}
}

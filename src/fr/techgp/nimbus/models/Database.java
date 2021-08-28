package fr.techgp.nimbus.models;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import fr.techgp.nimbus.Configuration;

public interface Database {

	public void reset();

	public void insertUser(User user);
	public void updateUser(User user);
	public void deleteUser(User user);

	public User findUserByLogin(String login);
	public int countUsers();
	public List<User> findAllUsers();

	public void insertItem(Item item);
	public void updateItem(Item item);
	public void deleteItem(Item item);
	public void restoreItem(Item item);
	public void eraseItem(Item item);
	public void notifyFolderContentChanged(Long folderId, int itemCountIncrement);

	public Item findItemById(Long id);
	public List<Item> findItemsByIds(Collection<Long> ids);
	public List<Item> findItems(String userLogin, Long parentId, boolean recursive, String sortBy, boolean sortAscending, String searchBy, String searchText, Boolean folders, Boolean hidden, Boolean deleted, String extensions);
	public int countItems(String userLogin, Long parentId);
	public int trashItemCount(String userLogin);
	public boolean hasItem(String userLogin, Long itemId);
	public boolean hasItemWithName(String userLogin, Long parentId, String name);
	public boolean hasItemsWithNames(String userLogin, Long parentId, String... names);
	public Item findItemWithName(String userLogin, Long parentId, String name);
	public void forEachItemTag(String userLogin, Consumer<String> consumer);
	public void forEachItemTagWithCount(String userLogin, boolean orderByCount, BiConsumer<String, Integer> consumer);
	public long calculateUsedSpace(String userLogin);

	public static void init(Configuration configuration) {
		Mongo.init(configuration.getMongoHost(), configuration.getMongoPort(), configuration.getMongoDatabase());
	}

	public static Database get() {
		if (Mongo.get() != null)
			return Mongo.get();
		throw new IllegalStateException("Database not initialized");
	}

}

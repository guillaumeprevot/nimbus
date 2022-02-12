package fr.techgp.nimbus.models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.postgresql.util.PGobject;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.techgp.nimbus.utils.StringUtils;

public class PostgreSQL implements Database {

	// Singleton
	private static PostgreSQL instance = null;

	public static final void init(String url, String username, String password) {
		PostgreSQL.instance = new PostgreSQL(url, username, password);
	}

	public static final PostgreSQL get() {
		return PostgreSQL.instance;
	}

	// Connexion à PostgreSQL
	private final String username;
	private final DataSource dataSource;
	private final Map<String, String> propertyToFieldMap;
	private final Pattern metadatasExpressionPattern;

	private PostgreSQL(String url, String username, String password) {
		this.username = username;
		this.dataSource = createDataSource(url, username, password);
		this.propertyToFieldMap = new HashMap<>();
		this.propertyToFieldMap.put("id", "id");
		this.propertyToFieldMap.put("parentId", "parent_id");
		this.propertyToFieldMap.put("path", "path");
		this.propertyToFieldMap.put("userLogin", "user_login");
		this.propertyToFieldMap.put("folder", "folder");
		this.propertyToFieldMap.put("hidden", "hidden");
		this.propertyToFieldMap.put("name", "name");
		this.propertyToFieldMap.put("createDate", "create_date");
		this.propertyToFieldMap.put("updateDate", "update_date");
		this.propertyToFieldMap.put("deleteDate", "delete_date");
		this.propertyToFieldMap.put("tags", "tags");
		this.propertyToFieldMap.put("sharedPassword", "shared_password");
		this.propertyToFieldMap.put("sharedDate", "shared_date");
		this.propertyToFieldMap.put("sharedDuration", "shared_duration");
		this.metadatasExpressionPattern = Pattern.compile("^content\\.[a-zA-Z0-9]+$");
		createIfNotExists();
	}

	@Override
	public void reset() {
		execute("DROP TABLE IF EXISTS users, items");
		createIfNotExists();
	}

	private void createIfNotExists() {
		execute("CREATE TABLE IF NOT EXISTS users ("
				+ "login character varying(32) NOT NULL,"
				+ "password character varying(263) NOT NULL,"
				+ "name character varying(64),"
				+ "admin boolean NOT NULL,"
				+ "quota integer,"
				+ "show_hidden_items boolean NOT NULL,"
				+ "show_item_tags boolean NOT NULL,"
				+ "show_item_description boolean NOT NULL,"
				+ "show_item_thumbnail boolean NOT NULL,"
				+ "visible_item_columns character varying(256),"
				+ "PRIMARY KEY (login)"
				+ ")");
		execute("ALTER TABLE users OWNER to " + this.username);
		execute("CREATE TABLE IF NOT EXISTS items ("
				+ "id serial NOT NULL,"
				+ "parent_id integer,"
				+ "path character varying(128) NOT NULL,"
				+ "user_login character varying(32) NOT NULL,"
				+ "folder boolean NOT NULL,"
				+ "hidden boolean NOT NULL,"
				+ "name character varying(256) NOT NULL,"
				+ "create_date timestamp without time zone NOT NULL,"
				+ "update_date timestamp without time zone NOT NULL,"
				+ "delete_date timestamp without time zone,"
				+ "tags character varying(64),"
				+ "shared_date timestamp without time zone,"
				+ "shared_password character varying(30),"
				+ "shared_duration integer,"
				+ "metadatas json,"
				+ "PRIMARY KEY (id)"
				+ ")");
		execute("ALTER TABLE items OWNER to " + this.username);
	}

	@Override
	public void insertUser(User user) {
		execute("INSERT INTO users (login, password, name, admin, quota, show_hidden_items, show_item_tags, show_item_description, show_item_thumbnail, visible_item_columns) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				(ps) -> {
					ps.setString(1, user.login);
					ps.setString(2, user.password);
					ps.setString(3, user.name);
					ps.setBoolean(4, user.admin);
					ps.setObject(5, user.quota, Types.INTEGER);
					ps.setBoolean(6, user.showHiddenItems);
					ps.setBoolean(7, user.showItemTags);
					ps.setBoolean(8, user.showItemDescription);
					ps.setBoolean(9, user.showItemThumbnail);
					ps.setString(10, user.visibleItemColumns != null ? String.join(",", user.visibleItemColumns) : null);
				});
	}

	@Override
	public void updateUser(User user) {
		execute("UPDATE users SET password=?, name=?, admin=?, quota=?, show_hidden_items=?, show_item_tags=?, show_item_description=?, show_item_thumbnail=?, visible_item_columns=? WHERE login=?",
				(ps) -> {
					ps.setString(1, user.password);
					ps.setString(2, user.name);
					ps.setBoolean(3, user.admin);
					ps.setObject(4, user.quota, Types.INTEGER);
					ps.setBoolean(5, user.showHiddenItems);
					ps.setBoolean(6, user.showItemTags);
					ps.setBoolean(7, user.showItemDescription);
					ps.setBoolean(8, user.showItemThumbnail);
					ps.setString(9, user.visibleItemColumns != null ? String.join(",", user.visibleItemColumns) : null);
					ps.setString(10, user.login);
				});
	}

	@Override
	public void deleteUser(User user) {
		execute("DELETE FROM users WHERE login=?", (ps) -> {
			ps.setString(1, user.login);
		});
	}

	@Override
	public User findUserByLogin(String login) {
		return selectOne("SELECT * FROM users WHERE login = ?", (ps) -> ps.setString(1, login), this::readUser);
	}

	@Override
	public int countUsers() {
		return count("SELECT count(*) as c FROM users");
	}

	@Override
	public List<User> findAllUsers() {
		return selectAll("SELECT * FROM users", this::readUser);
	}

	@Override
	public void insertItem(Item item) {
		String sql = "INSERT INTO items (parent_id, path, user_login, folder, hidden, name, create_date, update_date, tags, metadatas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (Connection connection = this.dataSource.getConnection();
				PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			int index = 1;
			ps.setObject(index++, item.parentId, Types.BIGINT);
			ps.setString(index++, item.path);
			ps.setString(index++, item.userLogin);
			ps.setBoolean(index++, item.folder);
			ps.setBoolean(index++, item.hidden);
			ps.setString(index++, item.name);
			ps.setTimestamp(index++, new Timestamp(item.createDate.getTime()));
			ps.setTimestamp(index++, new Timestamp(item.updateDate.getTime()));
			ps.setString(index++, item.tags == null ? null : String.join(",", item.tags));
			writeMetadatas(ps, index++, item.metadatas);
			ps.executeUpdate();
			try (ResultSet pk = ps.getGeneratedKeys()) {
				if (pk.next())
					item.id = pk.getLong(1);
				else
					throw new RuntimeException("Key generation failed for item " + item.name + " in path " + item.path);
			}
		} catch (SQLException ex) {
			throw new RuntimeException("SQLException on query " + sql, ex);
		}
	}

	@Override
	public void updateItem(Item item) {
		execute("UPDATE items SET parent_id=?, path=?, hidden=?, name=?, update_date=?, delete_date=?, tags=?, shared_date=?, shared_password=?, shared_duration=?, metadatas=? WHERE id=?",
				(ps) -> {
					int index = 1;
					ps.setObject(index++, item.parentId, Types.BIGINT);
					ps.setString(index++, item.path);
					ps.setBoolean(index++, item.hidden);
					ps.setString(index++, item.name);
					ps.setTimestamp(index++, new Timestamp(item.updateDate.getTime()));
					ps.setTimestamp(index++, item.deleteDate == null ? null : new Timestamp(item.deleteDate.getTime()));
					ps.setString(index++, item.tags == null ? null : String.join(",", item.tags));
					ps.setTimestamp(index++, item.sharedDate == null ? null : new Timestamp(item.sharedDate.getTime()));
					ps.setString(index++, item.sharedPassword);
					ps.setObject(index++, item.sharedDuration, Types.INTEGER);
					writeMetadatas(ps, index++, item.metadatas);
					ps.setObject(index++, item.id, Types.BIGINT);
				});
	}

	@Override
	public void deleteItem(Item item) {
		execute("UPDATE items SET delete_date = ? WHERE id = ?", (ps) -> {
			ps.setTimestamp(1, new Timestamp(item.deleteDate.getTime()));
			ps.setObject(2, item.id, Types.BIGINT);
		});
	}

	@Override
	public void restoreItem(Item item) {
		execute("UPDATE items SET delete_date = ? WHERE id = ?", (ps) -> {
			ps.setTimestamp(1, null);
			ps.setObject(2, item.id, Types.BIGINT);
		});
	}

	@Override
	public void eraseItem(Item item) {
		execute("DELETE FROM items WHERE id = ?", (ps) -> ps.setObject(1, item.id, Types.BIGINT));
	}

	@Override
	public void eraseAllItems(String userLogin) {
		execute("DELETE FROM items WHERE user_login = ?", (ps) -> ps.setString(1, userLogin));
	}

	@Override
	public void notifyFolderContentChanged(Long folderId, int itemCountIncrement) {
		Item item = findItemById(folderId);
		Integer itemCount = item.metadatas.getInteger("itemCount");
		item.metadatas.put("itemCount", itemCount.intValue() + itemCountIncrement);
		updateItem(item);
	}

	@Override
	public Item findItemById(Long id) {
		return selectOne("SELECT * FROM items WHERE id = ?", (ps) -> ps.setObject(1, id, Types.BIGINT), this::readItem);
	}

	@Override
	public List<Item> findItemsByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty())
			return new ArrayList<>();
		String idsString = ids.stream().map(id -> String.valueOf(id)).collect(Collectors.joining(","));
		return selectAll("SELECT * FROM items WHERE id IN (" + idsString + ")", this::readItem);
	}

	@Override
	public List<Item> findItems(String userLogin, Long parentId, boolean recursive,
			String sortBy, boolean sortAscending, boolean sortFoldersFirst,
			String searchBy, String searchText, Boolean folders, Boolean hidden, Boolean deleted,
			String extensions) {
		StringBuilder sql = new StringBuilder("SELECT * FROM items WHERE user_login = ?");

		if (!recursive) {
			sql.append(parentId == null ? " AND parent_id IS NULL" : " AND parent_id = ?");
		} else if (parentId != null) {
			sql.append(" AND path LIKE (select path || id || '%' from items where id = ?)");
		}

		// Texte recherché
		AtomicInteger searchPatternOccurences = new AtomicInteger(0);
		if (StringUtils.isNotBlank(searchText) && !"*".equals(searchText)) {
			if (StringUtils.isBlank(searchBy)) {
				// Par défaut, chercher dans le nom et les tags
				sql.append(" AND (tags ILIKE ? OR name ILIKE ?)");
				searchPatternOccurences.addAndGet(2);
			} else if (this.metadatasExpressionPattern.matcher(searchBy).matches()) {
				// ... ou dans les méta-données ("searchBy" du type "content.propertyName")
				String propertyName = searchBy.substring("content.".length());
				sql.append(" AND metadatas -> '" + propertyName + "' ->> 'value' ILIKE ?");
				searchPatternOccurences.incrementAndGet();
			} else {
				// ... ou dans une des colonnes de base (après vérification via propertyToFieldMap)
				String field = this.propertyToFieldMap.get(searchBy);
				if (field != null) {
					sql.append(" AND " + field + " ILIKE ?");
					searchPatternOccurences.incrementAndGet();
				}
			}
		}

		// Dossiers, Fichiers ou tout
		if (Boolean.TRUE.equals(folders))
			sql.append(" AND folder = true");
		else if (Boolean.FALSE.equals(folders))
			sql.append(" AND folder = false");

		// Masqués, non masqués ou tout
		if (Boolean.TRUE.equals(hidden))
			sql.append(" AND hidden = true");
		else if (Boolean.FALSE.equals(hidden))
			sql.append(" AND hidden = false");

		// Eléments supprimés, non supprimés ou tout
		if (Boolean.TRUE.equals(deleted))
			sql.append(" AND delete_date IS NOT NULL");
		else if (Boolean.FALSE.equals(deleted))
			sql.append(" AND delete_date IS NULL");

		// Restriction par extensions pour les fichiers (exemple : rechercher 'Toto' dans les dossiers ou les fichiers '.mp3')
		// https://www.postgresql.org/docs/current/functions-matching.html#FUNCTIONS-POSIX-REGEXP
		String extensionRegexp = StringUtils.isBlank(extensions) ? null : ".*\\.(" + extensions.replace(",", "|") + ")$";
		if (StringUtils.isNotBlank(extensions))
			sql.append(" AND (folder = true OR name ~* ?)");

		// Tri
		List<String> sorts = new ArrayList<>();
		// Renvoyer les dossiers en premier, sauf si on trie selon ce champ justement
		if (sortFoldersFirst && !"folder".equals(sortBy))
			sorts.add("folder DESC");
		// Puis trier selon la propriété demandée
		if (StringUtils.isNotBlank(sortBy))
			consumeSortExpressions(sortBy, sortAscending ? " ASC" : " DESC", sorts::add);
		// Puis trier alphabétiquement
		if (!"name".equals(sortBy))
			sorts.add("name ASC");
		// Puis trier par id pour que le tri soit consistant
		if (!"id".equals(sortBy))
			sorts.add("id ASC");
		sql.append(" ORDER BY ").append(sorts.stream().collect(Collectors.joining(", ")));

		return selectAll(sql.toString(), (ps) -> {
			int index = 1;
			ps.setString(index++, userLogin);
			if (parentId != null)
				ps.setObject(index++, parentId, Types.BIGINT);
			while (searchPatternOccurences.getAndDecrement() > 0) {
				ps.setString(index++, '%' + searchText + '%');
			}
			if (extensionRegexp != null)
				ps.setString(index++, extensionRegexp);
		}, this::readItem);
	}

	@Override
	public int countItems(String userLogin, Long parentId) {
		StringBuilder sb = new StringBuilder("SELECT count(*) AS c FROM items WHERE user_login = ?");
		sb.append(parentId == null ? " AND parent_id IS NULL" : " AND parent_id = ?");
		return count(sb.toString(), (ps) -> {
			ps.setString(1, userLogin);
			if (parentId != null)
				ps.setObject(2, parentId, Types.BIGINT);
		});
	}

	@Override
	public int trashItemCount(String userLogin) {
		return count("SELECT count(*) AS c FROM items WHERE user_login = ? AND delete_date IS NOT NULL",
				(ps) -> ps.setString(1, userLogin));
	}

	@Override
	public boolean hasItem(String userLogin, Long itemId) {
		return 1 == count("SELECT count(*) AS c FROM items WHERE user_login = ? AND id = ?", (ps) -> {
			ps.setString(1, userLogin);
			ps.setObject(2, itemId, Types.BIGINT);
		});
	}

	@Override
	public boolean hasItemWithName(String userLogin, Long parentId, String name) {
		StringBuilder sb = new StringBuilder("SELECT count(*) AS c FROM items WHERE user_login = ? AND name = ?");
		sb.append(parentId == null ? " AND parent_id IS NULL" : " AND parent_id = ?");
		return 1 == count(sb.toString(), (ps) -> {
			ps.setString(1, userLogin);
			ps.setString(2, name);
			if (parentId != null)
				ps.setObject(3, parentId, Types.BIGINT);
		});
	}

	@Override
	public boolean hasItemsWithNames(String userLogin, Long parentId, String... names) {
		if (names == null || names.length == 0)
			return false;
		StringBuilder sb = new StringBuilder("SELECT count(*) AS c FROM items WHERE user_login = ?");
		sb.append(parentId == null ? " AND parent_id IS NULL" : " AND parent_id = ?");
		sb.append(" AND name in (?");
		for (int i = 1; i < names.length; i++) {
			sb.append(", ?");
		}
		sb.append(")");
		return 1 <= count(sb.toString(), (ps) -> {
			int index = 1;
			ps.setString(index++, userLogin);
			if (parentId != null)
				ps.setObject(index++, parentId, Types.BIGINT);
			for (int i = 0; i < names.length; i++) {
				ps.setString(index + i, names[i]);
			}
		});
	}

	@Override
	public Item findItemWithName(String userLogin, Long parentId, String name) {
		StringBuilder sb = new StringBuilder("SELECT * FROM items WHERE user_login = ? AND name = ?");
		sb.append(parentId == null ? " AND parent_id IS NULL" : " AND parent_id = ?");
		return selectOne(sb.toString(), (ps) -> {
			ps.setString(1, userLogin);
			ps.setString(2, name);
			if (parentId != null)
				ps.setObject(3, parentId, Types.BIGINT);
		}, this::readItem);
	}

	@Override
	public void forEachItemTag(String userLogin, Consumer<String> consumer) {
		selectAll("SELECT distinct(regexp_split_to_table(tags, E',')) AS tag FROM items WHERE user_login = ? ORDER BY tag",
				(ps) -> ps.setString(1, userLogin),
				(rs) -> {
					consumer.accept(rs.getString(1));
					return null;
				});
	}

	@Override
	public void forEachItemTagWithCount(String userLogin, BiConsumer<String, Integer> consumer) {
		selectAll("SELECT distinct(regexp_split_to_table(tags, E',')) AS tag, count(*) as c FROM items WHERE user_login = ? GROUP BY tag ORDER BY count(*) DESC, tag",
				(ps) -> ps.setString(1, userLogin),
				(rs) -> {
					while (rs.next()) {
						consumer.accept(rs.getString(1), rs.getObject(2, Integer.class));
					}
					return null;
				});
	}

	@Override
	public long calculateUsedSpace(String userLogin) {
		return selectOne("SELECT SUM(CAST(metadatas -> 'length' ->> 'value' AS BIGINT)) AS l FROM items WHERE folder = false AND user_login = ?",
				(ps) -> ps.setString(1, userLogin),
				(rs) -> rs.getLong(1));
	}

	@Override
	public void calculateStatistics(String userLogin, Long parentId, boolean recursive, BiConsumer<String, Long> consumer) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ");
		sql.append(" SUM(case when folder then 1 else 0 end) AS folders,");
		sql.append(" SUM(case when folder then 0 else 1 end) AS files,");
		sql.append(" SUM(case when folder then 0 else CAST(metadatas -> 'length' ->> 'value' AS BIGINT) end) AS size");
		sql.append(" FROM items WHERE user_login = ?");
		if (!recursive) {
			sql.append(parentId == null ? " AND parent_id IS NULL" : " AND parent_id = ?");
		} else if (parentId != null) {
			sql.append(" AND path LIKE (select path || id || '%' from items where id = ?)");
		}
		selectOne(sql.toString(),
				(ps) -> {
					ps.setString(1, userLogin);
					if (parentId != null)
						ps.setObject(2, parentId, Types.BIGINT);
				},
				(rs) -> {
					for (String usage : List.of("folders", "files", "size")) {
						consumer.accept(usage, rs.getLong(usage));
					}
					return null;
				});
	}

	private User readUser(ResultSet rs) throws SQLException {
		User user = new User();
		user.login = rs.getString("login");
		user.password = rs.getString("password");
		user.name = rs.getString("name");
		user.admin = rs.getBoolean("admin");
		user.quota = rs.getObject("quota", Integer.class);
		user.showHiddenItems = rs.getBoolean("show_hidden_items");
		user.showItemTags = rs.getBoolean("show_item_tags");
		user.showItemDescription = rs.getBoolean("show_item_description");
		user.showItemThumbnail = rs.getBoolean("show_item_thumbnail");
		user.visibleItemColumns = Optional.ofNullable(rs.getString("visible_item_columns"))
				.map(s -> Arrays.asList(s.split(","))).orElse(null);
		return user;
	}

	private Item readItem(ResultSet rs) throws SQLException {
		Item item = new Item();
		item.id = rs.getLong("id");
		item.parentId = rs.getLong("parent_id");
		if (rs.wasNull())
			item.parentId = null;
		item.path = rs.getString("path");
		item.userLogin = rs.getString("user_login");
		item.folder = rs.getBoolean("folder");
		item.hidden = rs.getBoolean("hidden");
		item.name = rs.getString("name");
		item.createDate = toDate(rs, "create_date");
		item.updateDate = toDate(rs, "update_date");
		item.deleteDate = toDate(rs, "delete_date");
		item.tags = Optional.ofNullable(rs.getString("tags")).map(s -> Arrays.asList(s.split(","))).orElse(null);
		item.sharedDate = toDate(rs, "shared_date");
		item.sharedPassword = rs.getString("shared_password");
		item.sharedDuration = rs.getObject("shared_duration", Integer.class);
		JsonObject o = JsonParser.parseString(rs.getString("metadatas")).getAsJsonObject();
		for (Map.Entry<String, JsonElement> e : o.entrySet()) {
			String name = e.getKey();
			String type = e.getValue().getAsJsonObject().get("type").getAsString();
			JsonElement value = e.getValue().getAsJsonObject().get("value");
			switch (type) {
			case "string":
				item.metadatas.put(name, value.getAsString());
				break;
			case "boolean":
				item.metadatas.put(name, value.getAsBoolean());
				break;
			case "integer":
				item.metadatas.put(name, Optional.ofNullable(value.getAsNumber()).map(Number::intValue).orElse(null));
				break;
			case "long":
				item.metadatas.put(name, Optional.ofNullable(value.getAsNumber()).map(Number::longValue).orElse(null));
				break;
			case "double":
				item.metadatas.put(name, Optional.ofNullable(value.getAsNumber()).map(Number::doubleValue).orElse(null));
				break;
			default:
				throw new IllegalStateException("Unsupported number type \"" + type + "\" with value " + value.toString());
			}
		}
		return item;
	}

	private void writeMetadatas(PreparedStatement st, int index, Metadatas metadatas) throws SQLException {
		// Metadatas => JsonObject => PGobject
		JsonObject jo = new JsonObject();
		metadatas.visit(
				(name, s) -> writeMetadataEntry(jo, name, "string").addProperty("value", escapeForJSON(s)),
				(name, b) -> writeMetadataEntry(jo, name, "boolean").addProperty("value", b),
				(name, i) -> writeMetadataEntry(jo, name, "integer").addProperty("value", i),
				(name, l) -> writeMetadataEntry(jo, name, "long").addProperty("value", l),
				(name, d) -> writeMetadataEntry(jo, name, "double").addProperty("value", d));
		PGobject po = new PGobject();
		po.setType("json");
		po.setValue(jo.toString());
		st.setObject(index, po);
	}

	private JsonObject writeMetadataEntry(JsonObject container, String name, String type) {
		JsonObject o = new JsonObject();
		o.addProperty("type", type);
		container.add(name, o);
		return o;
	}

	private String escapeForJSON(String value) {
		// Il peut arriver que les méta-données extraites des fichiers contiennent un caractères NULL (\u0000).
		// Or, ce caractère posera problème lors de la lecture des données, alors même qu'il est autorisé au moment du INSERT.
		//     DETAIL:  \u0000 ne peut pas être converti en texte.
		//     CONTEXT:  données JSON, ligne 1 : ...
		// Cela fait suite à un changement dans PostreSQL 9.4.1
		//     https://www.postgresql.org/docs/9.4/release-9-4-1.html
		// Du coup, on supprime les occurences de ce caractère pour être tranquille.
		//     https://stackoverflow.com/questions/31671634/handling-unicode-sequences-in-postgresql
		return value == null ? null : value.replace("\u0000", "");
	}

	private void consumeSortExpressions(String sortBy, String sortDirection, Consumer<String> consumer) {
		if (this.metadatasExpressionPattern.matcher(sortBy).matches()) {
			// transform "content.propertyName" to a sort expression on JSON content "metadatas -> 'propertyName' -> 'value'"
			String field = sortBy.substring("content.".length());
			// first, add the sort expression matching numerical properties
			String numericSortFormat = "case when json_typeof(metadatas -> '%1$s' -> 'value') = 'number'"
					+ " then cast(metadatas -> '%1$s' ->> 'value' as numeric)"
					+ " else 0 end %2$s";
			consumer.accept(String.format(numericSortFormat, field, sortDirection));
			// then, add the sort expression matching text-comparable properties
			String otherSortFormat = "case when json_typeof(metadatas -> '%1$s' -> 'value') is null"
					+ " then ''"
					+ " else metadatas -> '%1$s' ->> 'value' end %2$s";
			consumer.accept(String.format(otherSortFormat, field, sortDirection));
		} else {
			// change "propertyName" to "property_name"
			String field = this.propertyToFieldMap.get(sortBy);
			if (field != null)
				consumer.accept(field + sortDirection);
		}
	}

	private int count(String sql) {
		return selectOne(sql, (rs) -> rs.getInt("c"));
	}

	private int count(String sql, SQLConsumer<PreparedStatement> prepare) {
		return selectOne(sql, prepare, (rs) -> rs.getInt("c"));
	}

	private <T> T selectOne(String sql, SQLFunction<ResultSet, T> load) {
		try (Connection connection = this.dataSource.getConnection();
				Statement s = connection.createStatement()) {
			try (ResultSet rs = s.executeQuery(sql)) {
				if (rs.next())
					return load.apply(rs);
				return null;
			}
		} catch (SQLException ex) {
			throw new RuntimeException("SQLException on query " + sql, ex);
		}
	}

	private <T> T selectOne(String sql, SQLConsumer<PreparedStatement> prepare, SQLFunction<ResultSet, T> load) {
		try (Connection connection = this.dataSource.getConnection();
				PreparedStatement ps = connection.prepareStatement(sql)) {
			prepare.accept(ps);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return load.apply(rs);
				return null;
			}
		} catch (SQLException ex) {
			throw new RuntimeException("SQLException on query " + sql, ex);
		}
	}

	private <T> List<T> selectAll(String sql, SQLFunction<ResultSet, T> load) {
		try (Connection connection = this.dataSource.getConnection();
				Statement s = connection.createStatement()) {
			try (ResultSet rs = s.executeQuery(sql)) {
				List<T> l = new ArrayList<>();
				while (rs.next()) {
					l.add(load.apply(rs));
				}
				return l;
			}
		} catch (SQLException ex) {
			throw new RuntimeException("SQLException on query " + sql, ex);
		}
	}

	private <T> List<T> selectAll(String sql, SQLConsumer<PreparedStatement> prepare, SQLFunction<ResultSet, T> load) {
		try (Connection connection = this.dataSource.getConnection();
				PreparedStatement ps = connection.prepareStatement(sql)) {
			prepare.accept(ps);
			try (ResultSet rs = ps.executeQuery()) {
				List<T> l = new ArrayList<>();
				while (rs.next()) {
					l.add(load.apply(rs));
				}
				return l;
			}
		} catch (SQLException ex) {
			throw new RuntimeException("SQLException on query " + sql, ex);
		}
	}

	private int execute(String sql) {
		try (Connection connection = this.dataSource.getConnection();
				Statement s = connection.createStatement()) {
			return s.executeUpdate(sql);
		} catch (SQLException ex) {
			throw new RuntimeException("SQLException on query " + sql, ex);
		}
	}

	private int execute(String sql, SQLConsumer<PreparedStatement> executor) {
		try (Connection connection = this.dataSource.getConnection();
				PreparedStatement ps = connection.prepareStatement(sql)) {
			executor.accept(ps);
			return ps.executeUpdate();
		} catch (SQLException ex) {
			throw new RuntimeException("SQLException on query " + sql, ex);
		}
	}

	private final DataSource createDataSource(String url, String username, String password) {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		dataSource.setDriverClassName("org.postgresql.Driver");
		dataSource.setValidationQuery("SELECT version()");
		return dataSource;
	}

	private static final Date toDate(ResultSet rs, String column) throws SQLException {
		Timestamp ts = rs.getTimestamp(column);
		return ts == null ? null : new Date(ts.getTime());
	}

	@FunctionalInterface
	public static interface SQLConsumer<T> {
		public void accept(T value) throws SQLException;
	}

	@FunctionalInterface
	public static interface SQLFunction<T, R> {
		public R apply(T value) throws SQLException;
	}

}

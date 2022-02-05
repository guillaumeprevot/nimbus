package fr.techgp.nimbus.controllers;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import fr.techgp.nimbus.Configuration;
import fr.techgp.nimbus.models.Database;
import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.models.User;
import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Router;
import fr.techgp.nimbus.server.Session;
import fr.techgp.nimbus.utils.CryptoUtils;
import fr.techgp.nimbus.utils.StringUtils;

public class Controller {

	protected static Logger logger = null;
	protected static Configuration configuration;

	public static final Router init(Logger logger, Configuration configuration, boolean dev) {
		Controller.logger = logger;
		Controller.configuration = configuration;
		Router r = new Router();

		r.redirect("/", "/nav");

		r.get("/favicon.ico", StaticFiles.publicFolder);
		r.get("/favicon.png", StaticFiles.publicFolder);
		r.get("/nimbus.css", StaticFiles.publicFolder);
		r.get("/nimbus.js", StaticFiles.publicFolder);
		r.get("/langs/*", StaticFiles.publicFolder);
		r.get("/libs/*", StaticFiles.publicFolder);
		r.get("/plugins/*", StaticFiles.publicFolder);
		r.get("/svg/*", StaticFiles.publicFolder);

		r.before("/nav", Filters.filterAuthenticatedOrRedirect);
		r.before("/nav/*", Filters.filterAuthenticatedOrRedirect);
		r.get("/nav", (request, response) -> nav(request));
		r.get("/nav/*", (request, response) -> {
			String splat = request.path().substring("/nav/".length());
			if (splat.length() == 0) // "/nav/"
				return nav(request);
			String[] path = splat.split("/");
			return actionOnSingleItem(request, path[path.length - 1], (item) -> {
				if (item.folder)
					return nav(request);
				return Render.redirect("/files/stream/" + item.id);
			});
		});

		r.get("/login/background", Authentication.background); // URL publique
		r.get("/login.html", Authentication.page); // URL publique
		r.post("/login.html", Authentication.login); // URL publique
		r.get("/logout", Authentication.logout); // URL publique

		r.before("/users.html", Filters.filterAdministratorOrRedirect);
		r.before("/user/*", Filters.filterAdministrator);
		r.get("/users.html", Users.page);
		r.get("/user/list", Users.list);
		r.post("/user/insert/:login", Users.insert);
		r.post("/user/update/:login", Users.update);
		r.post("/user/delete/:login", Users.delete);

		r.before("/files/*", Filters.filterAuthenticated);
		r.post("/files/upload", Files.upload);
		r.post("/files/update/:itemId", Files.update);
		r.post("/files/touch", Files.touch);
		r.get("/files/browse/*", Files.browse);
		r.get("/files/browseTo/:itemId", Files.browseTo);
		r.get("/files/stream/:itemId", Files.stream);
		r.get("/files/download/:itemId", Files.download);
		r.get("/files/thumbnail/:itemId", Files.thumbnail);
		r.post("/files/useAsFolderIcon/:itemId", Files.useAsFolderIcon);

		r.before("/items/*", Filters.filterAuthenticated);
		r.get("/items/quota", Items.quota);
		r.get("/items/list", Items.list);
		r.post("/items/exists", Items.exists); // pour éviter 414 URI Too Long
		r.get("/items/info/:itemId", Items.info);
		r.get("/items/infos", Items.infos);
		r.get("/items/tags", Items.tags);
		r.post("/items/add/folder", Items.addFolder);
		r.get("/items/folder/statistics", Items.folderStatistics);
		r.post("/items/duplicate", Items.duplicate);
		r.post("/items/rename", Items.rename);
		r.post("/items/hide", Items.hide);
		r.post("/items/refresh", Items.refresh);
		r.post("/items/metadata", Items.metadata);
		r.post("/items/move", Items.move);
		r.get("/items/zip", Items.zip);

		r.before("/trash.html", Filters.filterAuthenticatedOrRedirect);
		r.before("/trash/*", Filters.filterAuthenticated);
		r.get("/trash.html", Trash.page);
		r.get("/trash/count", Trash.count);
		r.get("/trash/items", Trash.items);
		r.post("/trash/delete", Trash.delete);
		r.post("/trash/restore", Trash.restore);
		r.post("/trash/erase", Trash.erase);

		r.before("/download/*", Filters.filterAuthenticated);
		r.get("/download/autocomplete", Downloads.autocomplete);
		r.post("/download/add", Downloads.add);
		r.post("/download/refresh", Downloads.refresh);
		r.post("/download/done", Downloads.done);

		r.before("/share/add", Filters.filterAuthenticated);
		r.before("/share/delete", Filters.filterAuthenticated);
		r.post("/share/add", Shares.add);
		r.post("/share/delete", Shares.delete);
		r.get("/share/get/:itemId", Shares.get); // URL publique

		r.before("/preferences.html", Filters.filterAuthenticatedOrRedirect);
		r.before("/preferences/save", Filters.filterAuthenticated);
		r.get("/preferences/theme", Preferences.theme); // URL publique
		r.get("/preferences.html", Preferences.page);
		r.post("/preferences/save", Preferences.save);

		r.get("/epub.html", Extensions.epub); // URL publique
		r.get("/pdf.html", Extensions.pdf); // URL publique
		r.get("/video.html", Extensions.video); // URL publique
		r.get("/audio.html", Extensions.audio);
		r.get("/diaporama.html", Extensions.diaporama);
		r.get("/text-editor.html", Extensions.textEditor);
		r.get("/markdown-editor.html", Extensions.markdownEditor);
		r.get("/note-editor.html", Extensions.noteEditor);
		r.get("/code-editor.html", Extensions.codeEditor);
		r.get("/secret-editor.html", Extensions.secretEditor);
		r.get("/calendar.html", Extensions.calendar);
		r.get("/contacts.html", Extensions.contacts);
		r.get("/bookmarks.html", Extensions.bookmarks);

		// Accès à la page de test en mode DEV uniquement
		r.get("/test.html", (request, response) -> {
			if (!dev)
				return Render.notFound();
			try {
				User.findAll().forEach((u) -> Controller.eraseAllUserItemFiles(u.login));
			} catch (Exception ex) {
				if (Controller.logger.isWarnEnabled())
					Controller.logger.warn("Erreur pendant la remise à 0 des fichiers pour test", ex);
			}
			Database.get().reset();
			Session session = getSession(request, false);
			if (session != null) {
				session.removeAttribute("userLogin");
				session.removeAttribute("theme");
			}
			return Templates.render(request, "test.html",
					"serverAbsoluteUrl", configuration.getServerAbsoluteUrl());
		});

		return r;
	}

	private static final Render nav(Request request) {
		String login = getUserLogin(request);
		User user = User.findByLogin(login);
		return Templates.render(request, "main.html",
				"plugins", configuration.getClientPlugins(),
				"name", StringUtils.withDefault(user.name, user.login),
				"admin", user.admin,
				"textFileExtensions", configuration.getTextFileExtensions(),
				"showHiddenItems", user.showHiddenItems,
				"showItemTags", user.showItemTags,
				"showItemDescription", user.showItemDescription,
				"showItemThumbnail", user.showItemThumbnail,
				"visibleItemColumns", user.visibleItemColumns == null ? Collections.emptyList() : user.visibleItemColumns);
	}

	/**
	 * Cette méthode récupère l'élément demandé "itemId" de l'utilisateur connecté dans "request".
	 * L'élément est ensute "consommé" de manière personnalisée par "consumer".
	 *
	 * @param request la requête pour savoir qui est connecté (via "userLogin")
	 * @param itemId l'id de l'élément demandé
	 * @param consumer le traitement à effectuer sur chaque élément
	 * @return badRequest en cas de problème ou "" si tous les éléments ont été consommés sans erreur
	 */
	protected static final Render actionOnSingleItem(Request request, String itemIdString, Function<Item, Render> consumer) {
		// Récupérer l'utilisateur connecté
		String userLogin = getUserLogin(request);
		// Rechercher l'élément et en vérifier l'accès
		Item item = Item.findById(Long.valueOf(itemIdString));
		if (item == null || !item.userLogin.equals(userLogin))
			return Render.badRequest();
		// Laisser l'appelant "consommer" l'élément
		return consumer.apply(item);
	}

	/**
	 * Cette méthode récupère les éléments demandés "itemIds" de l'utilisateur connecté dans "request".
	 *
	 * @param request la requête pour savoir qui est connecté (via "userLogin")
	 * @param itemIds les ids des éléments demandés, séparés par ","
	 * @return null en cas de requête incorrecte ou la liste demandée sinon
	 */
	protected static final List<Item> loadMultipleItems(Request request, String itemIds) {
		if (itemIds != null && itemIds.trim().length() > 0) {
			// Récupérer l'utilisateur connecté
			String userLogin = getUserLogin(request);
			// Récupérer les identifiants des éléments demandés
			Set<Long> ids = Arrays.stream(itemIds.split(",")).map(Long::valueOf).collect(Collectors.toSet());
			// Récupérer les éléments demandés
			List<Item> items = Item.findByIds(ids);
			// Vérifier la pertinence des ids demandés
			if (items.size() != ids.size())
				return null;
			// Parcourir les éléments à traiter
			for (Item item : items) {
				// Vérifier les droits d'accès
				if (! item.userLogin.equals(userLogin))
					return null;
			}
			// Renvoyer la liste si tout est OK
			return items;
		}
		return null;
	}

	/**
	 * Cette méthode retourne le fichier associé à l'élément "item".
	 *
	 * @param item l'élément représentant un fichier dans le cloud
	 * @return le fichier associé à l'élément sur le disque
	 */
	protected static final File getFile(Item item) {
		return configuration.getStoredFile(item);
	}

	/**
	 * Cette méthode met à jour les méta-données de l'élément "item".
	 *
	 * @param item l'élément représentant un fichier dans le cloud
	 * @param date nouvelle date de modification, si le contenu du fichier a changé, ou null poru ne pas y toucher
	 * @param save indique si on sauvegarde l'élément dans la base de données
	 */
	protected static final void updateFile(Item item, Date date, boolean save) {
		configuration.updateStoredFile(item, (facet, th) -> {
			if (Controller.logger.isErrorEnabled())
				Controller.logger.error("{} dans {} sur l'élément n°{} ({}) : {}",
						th.getClass().getName(), facet.getClass().getSimpleName(), item.id, item.name, th.getMessage());
		});
		// Mettre à jour la date de modification, si demandé (= dans le cas où le contenu du fichier a changé)
		if (date != null)
			item.updateDate = date;
		// Sauvegarder dans la base, si demandé
		if (save)
			Item.update(item);
	}

	/**
	 * Cette méthode vérifie si l'espace dispo de l'utilisateur est supérieur ou égal à neededSpace.
	 *
	 * @param userLogin l'utilisateur donc il faudra vérifier le quota et l'espace utilisé
	 * @param neededSpace l'espace nécessaire pour accomplir l'opération en cours (upload, duplicate, ...)
	 * @return true si l'espace est suffisant, false sinon
	 */
	protected static final boolean checkQuotaAndHaltIfNecessary(String userLogin, Long neededSpace) {
		// Pas de souci si la taille n'augmente pas
		if (neededSpace == null || neededSpace <= 0)
			return true;
		// Récupérer le quota de l'utilisateur connecté
		User user = User.findByLogin(userLogin);
		// Vérifier pour les utilisateurs sans quota qu'il reste suffisamment d'espace disque
		if (user.quota == null)
			return neededSpace <= configuration.getStorageFolder().getFreeSpace();
		// Récupérer l'espace occupé
		long usedSpace = Item.calculateUsedSpace(userLogin);
		//System.out.println(String.format("Needed space = %7d bytes", neededSpace));
		//System.out.println(String.format("User quota   = %7d bytes", user.quota * 1024 * 1024));
		//System.out.println(String.format("Used space   = %7d bytes", usedSpace));
		//System.out.println(String.format("Free space   = %7d bytes", user.quota * 1024 * 1024 - usedSpace));
		return neededSpace <= user.quota.longValue() * 1024L * 1024L - usedSpace;
	}

	/**
	 * Cette méthode supprime tous les fichiers de l'utilisateur précisé.
	 *
	 * @param userLogin l'utilisateur dont on souhaite supprimer les fichiers
	 * @return true si la suppression s'est bien passée ou false si une erreur a eu lieu pendant la suppression
	 */
	protected static final boolean eraseAllUserItemFiles(String userLogin) {
		try {
			File folder = new File(configuration.getStorageFolder(), userLogin);
			if (folder.exists())
				FileUtils.cleanDirectory(folder);
			return true;
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
	}

	/**
	 * Cette méthode vérifie si le couple (login, password) est valide.
	 * En cas d'erreur, une chaine de caractères non null est renvoyée.
	 * A l'installation (= base vide), un premier compte est créé automatiquement.
	 *
	 * @param login le login fourni
	 * @param password le mot de passe fourni
	 * @return null si OK ou une chaine de caractère représentant l'erreur sinon
	 */
	protected static final String authenticate(String login, String password) {
		if (StringUtils.isBlank(login))
			return "utilisateur vide";
		if (StringUtils.isBlank(password))
			return "mot de passe vide";

		if (User.count() == 0) {
			// Installation
			try {
				User user = new User();
				user.login = login;
				user.password = CryptoUtils.hashPassword(password);
				user.admin = true;
				User.insert(user);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		User user = User.findByLogin(login);
		if (user == null)
			return "utilisateur inconnu";
		// Pendant longtemps, on a utilisé PBKDF2WithHmacSHA1 et 10000 itérations.
		// Puis on est passé sur PBKDF2WithHmacSHA256 et 99000 itérations
		// Du coup, le nombre d'itérations du mot de passe enregistré non indique également l'algo utilisé lors du hash
		String alg = user.password.startsWith("10000:") ? "PBKDF2WithHmacSHA1" : CryptoUtils.PASSWORD_HASH_ALGORITHM;
		if (!CryptoUtils.validatePassword(password, user.password, alg, null))
			return "mot de passe incorrect";
		return null;
	}

	protected static Session getSession(Request request, boolean create) {
		return configuration.getSessionOnClient() ? request.clientSession(create) : request.session(create);
	}

	protected static String getUserLogin(Request request) {
		Session session = getSession(request, false);
		return session == null ? null : session.attribute("userLogin");
	}

}

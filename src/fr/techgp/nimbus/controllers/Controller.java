package fr.techgp.nimbus.controllers;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import fr.techgp.nimbus.Configuration;
import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.models.Mongo;
import fr.techgp.nimbus.models.User;
import fr.techgp.nimbus.utils.CryptoUtils;
import fr.techgp.nimbus.utils.SparkUtils;
import fr.techgp.nimbus.utils.StringUtils;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModelException;
import spark.ModelAndView;
import spark.Request;
import spark.Spark;
import spark.TemplateEngine;
import spark.template.freemarker.FreeMarkerEngine;

public class Controller {

	protected static Logger logger = null;
	protected static Configuration configuration;
	protected static TemplateEngine templateEngine;

	public static final void init(Logger logger, Configuration configuration, boolean dev) {
		Controller.logger = logger;
		Controller.configuration = configuration;
		Controller.templateEngine = prepareTemplateEngine(dev);

		Spark.redirect.get("/", "/nav");

		Spark.before("/*", new StaticFiles(configuration, "public"));

		Spark.before("/nav", Filters.filterAuthenticatedOrRedirect);
		Spark.before("/nav/*", Filters.filterAuthenticatedOrRedirect);
		Spark.get("/nav", (request, response) -> nav(request));
		Spark.get("/nav/*", (request, response) -> {
			String[] splat = request.splat();
			if (splat.length == 0) // "/nav/"
				return nav(request);
			String[] path = splat[0].split("/");
			return actionOnSingleItem(request, path[path.length - 1], (item) -> {
				if (item.folder)
					return nav(request);
				response.redirect("/files/stream/" + item.id);
				return null;
			});
		});

		Spark.get("/login/background", Authentication.background); // URL publique
		Spark.get("/login.html", Authentication.page); // URL publique
		Spark.post("/login.html", Authentication.login); // URL publique
		Spark.get("/logout", Authentication.logout); // URL publique

		Spark.before("/users.html", Filters.filterAdministratorOrRedirect);
		Spark.before("/user/*", Filters.filterAdministrator);
		Spark.get("/users.html", Users.page);
		Spark.get("/user/list", Users.list);
		Spark.post("/user/insert/:login", Users.insert);
		Spark.post("/user/update/:login", Users.update);
		Spark.post("/user/delete/:login", Users.delete);

		Spark.before("/files/*", Filters.filterAuthenticated);
		Spark.post("/files/upload", Files.upload);
		Spark.post("/files/update/:itemId", Files.update);
		Spark.post("/files/touch", Files.touch);
		Spark.get("/files/browse/*", Files.browse);
		Spark.get("/files/browseTo/:itemId", Files.browseTo);
		Spark.get("/files/stream/:itemId", Files.stream);
		Spark.get("/files/download/:itemId", Files.download);
		Spark.get("/files/thumbnail/:itemId", Files.thumbnail);
		Spark.post("/files/useAsFolderIcon/:itemId", Files.useAsFolderIcon);

		Spark.before("/items/*", Filters.filterAuthenticated);
		Spark.get("/items/quota", Items.quota);
		Spark.get("/items/list", Items.list);
		Spark.post("/items/exists", Items.exists); // pour éviter 414 URI Too Long
		Spark.get("/items/info/:itemId", Items.info);
		Spark.get("/items/infos", Items.infos);
		Spark.get("/items/tags", Items.tags);
		Spark.post("/items/add/folder", Items.addFolder);
		Spark.post("/items/duplicate", Items.duplicate);
		Spark.post("/items/rename", Items.rename);
		Spark.post("/items/hide", Items.hide);
		Spark.post("/items/refresh", Items.refresh);
		Spark.post("/items/metadata", Items.metadata);
		Spark.post("/items/move", Items.move);
		Spark.get("/items/zip", Items.zip);

		Spark.before("/trash.html", Filters.filterAuthenticatedOrRedirect);
		Spark.before("/trash/*", Filters.filterAuthenticated);
		Spark.get("/trash.html", Trash.page);
		Spark.get("/trash/count", Trash.count);
		Spark.get("/trash/items", Trash.items);
		Spark.post("/trash/delete", Trash.delete);
		Spark.post("/trash/restore", Trash.restore);
		Spark.post("/trash/erase", Trash.erase);

		Spark.before("/download/*", Filters.filterAuthenticated);
		Spark.get("/download/autocomplete", Downloads.autocomplete);
		Spark.post("/download/add", Downloads.add);
		Spark.post("/download/refresh", Downloads.refresh);
		Spark.post("/download/done", Downloads.done);

		Spark.before("/share/add", Filters.filterAuthenticated);
		Spark.before("/share/delete", Filters.filterAuthenticated);
		Spark.post("/share/add", Shares.add);
		Spark.post("/share/delete", Shares.delete);
		Spark.get("/share/get/:itemId", Shares.get); // URL publique

		Spark.before("/preferences.html", Filters.filterAuthenticatedOrRedirect);
		Spark.before("/preferences/save", Filters.filterAuthenticated);
		Spark.get("/preferences/theme", Preferences.theme); // URL publique
		Spark.get("/preferences.html", Preferences.page);
		Spark.post("/preferences/save", Preferences.save);

		Spark.get("/epub.html", Extensions.epub); // URL publique
		Spark.get("/pdf.html", Extensions.pdf); // URL publique
		Spark.get("/video.html", Extensions.video); // URL publique
		Spark.get("/audio.html", Extensions.audio); // URL publique
		Spark.get("/diaporama.html", Extensions.diaporama); // URL publique
		Spark.get("/text-editor.html", Extensions.textEditor); // URL publique
		Spark.get("/markdown-editor.html", Extensions.markdownEditor); // URL publique
		Spark.get("/note-editor.html", Extensions.noteEditor); // URL publique
		Spark.get("/code-editor.html", Extensions.codeEditor); // URL publique
		Spark.get("/secret-editor.html", Extensions.secretEditor); // URL publique

		// Accès à la page de test en mode DEV uniquement
		Spark.get("/test.html", (request, response) -> {
			if (!dev)
				return SparkUtils.haltNotFound();
			User.findAll().forEach((u) -> {
				try {
					File folder = new File(configuration.getStorageFolder(), u.login);
					if (folder.exists())
						FileUtils.cleanDirectory(folder);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			});
			Mongo.reset(true);
			request.session().removeAttribute("userLogin");
			request.session().removeAttribute("theme");
			return renderTemplate(request, "test.html",
					"serverAbsoluteUrl", configuration.getServerAbsoluteUrl());
		});
	}

	private static final Object nav(Request request) {
		String login = request.session().attribute("userLogin");
		User user = User.findByLogin(login);
		return renderTemplate(request, "main.html",
				"plugins", configuration.getClientPlugins(),
				"name", StringUtils.withDefault(user.name, user.login),
				"admin", user.admin,
				"trashCount", Item.trashCount(login),
				"textFileExtensions", configuration.getTextFileExtensions(),
				"showHiddenItems", user.showHiddenItems,
				"showItemTags", user.showItemTags,
				"showItemDescription", user.showItemDescription,
				"showItemThumbnail", user.showItemThumbnail,
				"visibleItemColumns", user.visibleItemColumns == null ? Collections.emptyList() : user.visibleItemColumns);
	}

	/**
	 * Cette méthode prépare le moteur de rendu des vues en utilisant FreeMarker.
	 *
	 * @param dev indique si on active les fonctions utiles pendant le DEV mais à désactiver en PROD.
	 * @return retourne le TemplateEngine utilisant FreeMarker
	 */
	private static final TemplateEngine prepareTemplateEngine(boolean dev) {
		try {
			// https://freemarker.apache.org/docs/pgui_quickstart_createconfiguration.html
			freemarker.template.Version version = freemarker.template.Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS;
			freemarker.template.Configuration configuration = new freemarker.template.Configuration(version);
			configuration.setDirectoryForTemplateLoading(new File("templates"));
			configuration.setDefaultEncoding("UTF-8");
			if (dev) {
				configuration.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
				configuration.setTemplateUpdateDelayMilliseconds(500);
			} else {
				configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
			}
			configuration.setNumberFormat("###0.##");
			configuration.setLogTemplateExceptions(false);
			configuration.setWrapUncheckedExceptions(true);
			configuration.setSharedVariable("appName", "Nimbus");
			return new FreeMarkerEngine(configuration);
		} catch (TemplateModelException | IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Cette méthode génère un template FreeMarker avec les paramètres indiqués
	 * 
	 * @param name le nom du template à générer
	 * @param paramAndValues une suite de paramètres "name1:String, value1, name2:String, value2, ..."
	 * @return le texte issu de la génération du template
	 */
	protected static final String renderTemplate(Request request, String name, Object... paramAndValues) {
		String theme = getUserTheme(request);

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("lang", SparkUtils.getRequestLang(request));
		attributes.put("theme", theme);
		attributes.put("stylesheet", "dark".equals(theme) ? "/libs/bootswatch/darkly.min.css" : "/libs/bootswatch/flatly.min.css");

		for (int i = 0; i < paramAndValues.length; i += 2) {
			attributes.put((String) paramAndValues[i], paramAndValues[i + 1]);
		}
		return Controller.templateEngine.render(new ModelAndView(attributes, name));
	}

	/**
	 * Cette méthode centralise la récupération du thème de l'utilisateur
	 * 
	 * @param request la requête pour chercher dans la session si l'utilisateur a choisi un thème
	 * @return le nom du thème choisi par l'utilisateur ou le thème par défaut sinon (cf nimbus.conf) 
	 */
	protected static final String getUserTheme(Request request) {
		return StringUtils.withDefault(request.session().attribute("theme"), configuration.getClientDefaultTheme());
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
	protected static final Object actionOnSingleItem(Request request, String itemIdString, Function<Item, Object> consumer) {
		// Récupérer l'utilisateur connecté
		String userLogin = request.session().attribute("userLogin");
		// Rechercher l'élément et en vérifier l'accès
		Item item = Item.findById(Long.valueOf(itemIdString));
		if (item == null || !item.userLogin.equals(userLogin))
			return SparkUtils.haltBadRequest();
		// Laisser l'appelant "consommer" l'élément
		return consumer.apply(item);
	}

	/**
	 * Cette méthode récupère les éléments demandés "itemIds" de l'utilisateur connecté dans "request".
	 * Chaque élément est "consommé" de manière personnalisée par "consumer".
	 *
	 * @param request la requête pour savoir qui est connecté (via "userLogin")
	 * @param itemIds les ids des éléments demandés, séparés par ","
	 * @param consumer le traitement à effectuer sur chaque élément
	 * @return badRequest en cas de problème ou "" si tous les éléments ont été consommés sans erreur
	 */
	protected static final Object actionOnMultipleItems(Request request, String itemIds, Consumer<Item> consumer) {
		if (itemIds != null && itemIds.trim().length() > 0) {
			// Récupérer l'utilisateur connecté
			String userLogin = request.session().attribute("userLogin");
			// Récupérer les identifiants des éléments demandés
			String[] ids = itemIds.split(",");
			for (String itemIdString : ids) {
				// Récupérer l'élément de chaque identifiant
				Item item = Item.findById(Long.valueOf(itemIdString));
				// Vérifier les droits d'accès
				if (item == null || !item.userLogin.equals(userLogin))
					return SparkUtils.haltBadRequest();
				// Laisser l'appelant "consommer" l'élément
				consumer.accept(item);
			}
		}
		return "";
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
	 * Si l'espace est insuffisant, la méthode SparkUtils.haltInsufficientStorage() est appelée pour interrompre le traitement.
	 * 
	 * @param userLogin l'utilisateur donc il faudra vérifier le quota et l'espace utilisé
	 * @param neededSpace l'espace nécessaire pour accomplir l'opération en cours (upload, duplicate, ...)
	 */
	protected static final void checkQuotaAndHaltIfNecessary(String userLogin, Long neededSpace) {
		// Pas de souci si la taille n'augmente pas
		if (neededSpace == null || neededSpace <= 0)
			return;
		// Récupérer le quota de l'utilisateur connecté
		User user = User.findByLogin(userLogin);
		// Vérifier pour les utilisateurs sans quota qu'il reste suffisamment d'espace disque
		if (user.quota == null) {
			if (neededSpace > configuration.getStorageFolder().getFreeSpace())
				SparkUtils.haltInsufficientStorage(); 
			return;
		}
		// Récupérer l'espace occupé
		long usedSpace = Item.calculateUsedSpace(userLogin);
		//System.out.println(String.format("Needed space = %7d bytes", neededSpace));
		//System.out.println(String.format("User quota   = %7d bytes", user.quota * 1024 * 1024));
		//System.out.println(String.format("Used space   = %7d bytes", usedSpace));
		//System.out.println(String.format("Free space   = %7d bytes", user.quota * 1024 * 1024 - usedSpace));
		if (neededSpace > user.quota.longValue() * 1024L * 1024L - usedSpace)
			SparkUtils.haltInsufficientStorage();
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
		if (!CryptoUtils.validatePassword(password, user.password))
			return "mot de passe incorrect";
		return null;
	}

}

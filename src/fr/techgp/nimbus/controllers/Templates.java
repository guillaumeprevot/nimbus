package fr.techgp.nimbus.controllers;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.render.RenderFreeMarker;
import fr.techgp.nimbus.utils.StringUtils;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModelException;

public class Templates extends Controller {

	/**
	 * Cette méthode prépare le moteur de rendu des vues en utilisant FreeMarker.
	 *
	 * @param dev indique si on active les fonctions utiles pendant le DEV mais à désactiver en PROD.
	 */
	public static final void init(boolean dev) {
		try {
			// https://freemarker.apache.org/docs/pgui_quickstart_createconfiguration.html
			Configuration configuration = RenderFreeMarker.defaultConfiguration();
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
		} catch (TemplateModelException | IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Cette méthode génère un template FreeMarker avec les paramètres indiqués
	 *
	 * @param request la requête, pour déterminer la langue et le thème
	 * @param name le nom du template à générer
	 * @param paramAndValues une suite de paramètres "name1:String, value1, name2:String, value2, ..."
	 * @return le rendu qui se chargera de générer le template
	 */
	public static final Render render(Request request, String name, Object... paramAndValues) {
		String theme = getUserTheme(request);
		return new RenderFreeMarker(name, paramAndValues)
				.with("backURL", Optional.ofNullable(request.header("Referer")).orElse("/"))
				.with("lang", getUserLang(request))
				.with("theme", theme)
				.with("stylesheet", "dark".equals(theme) ? "/libs/bootswatch/darkly.min.css" : "/libs/bootswatch/flatly.min.css");
	}

	/**
	 * Cette méthode centralise la récupération du thème de l'utilisateur.
	 *
	 * @param request la requête pour chercher dans la session si l'utilisateur a choisi un thème
	 * @return le nom du thème choisi par l'utilisateur ou le thème par défaut sinon (cf nimbus.conf)
	 */
	private static final String getUserTheme(Request request) {
		return StringUtils.coalesce(
				request.queryParameter("theme", null),
				request.session().attribute("theme"),
				configuration.getClientDefaultTheme());
	}

	/**
	 * Cette méthode centralise la détection de la langue à utiliser pour l'utilisateur.
	 *
	 * @param request la requête indiquant les préférences de langues via l'en-tête "Accept-Language"
	 * @return la langue supportée, dans l'ordre de préférence, parmi "en" et "fr" avec "en" par défaut.
	 */
	private static final String getUserLang(Request request) {
		String acceptLanguage = request.header("Accept-Language");
		if (acceptLanguage != null) {
			String[] options = acceptLanguage.split(",");
			for (String option : options) {
				// en, en-US, en-US;q=0.5 sont possibles
				// le "matches" fonctionne mais on se contentera de "startsWith", plus rapide
				// if (option.matches("^en(-.{2})?(;q=\\d+\\.\\d+)?$")) return "en";
				if (option.startsWith("en"))
					return "en";
				if (option.startsWith("fr"))
					return "fr";
			}
		}
		return "en";
	}

}

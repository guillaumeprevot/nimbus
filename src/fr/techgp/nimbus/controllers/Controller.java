package fr.techgp.nimbus.controllers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import fr.techgp.nimbus.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModelException;
import spark.ModelAndView;
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

		Spark.get("/main.html", (request, response) -> {
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("message", "Hello World!");
			return Controller.templateEngine.render(new ModelAndView(attributes, "main.html.ftl"));
		});
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
			configuration.setLogTemplateExceptions(false);
			configuration.setWrapUncheckedExceptions(true);
			configuration.setSharedVariable("appName", "Nimbus");
			return new FreeMarkerEngine(configuration);
		} catch (TemplateModelException | IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}

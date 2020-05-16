package fr.techgp.nimbus;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;

public class FreeMarker {

	private static Configuration configuration = null;

	/**
	 * Cette méthode prépare le moteur de rendu des vues en utilisant FreeMarker.
	 *
	 * @param dev indique si on active les fonctions utiles pendant le DEV mais à désactiver en PROD.
	 */
	public static final void init(boolean dev) {
		try {
			// https://freemarker.apache.org/docs/pgui_quickstart_createconfiguration.html
			Version version = Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS;
			Configuration configuration = new Configuration(version);
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
			FreeMarker.configuration = configuration;
		} catch (TemplateModelException | IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Cette méthode génère un template "name" personnalisé par "attributes" en utilisant FreeMarker.
	 *
	 * @param name le nom du templates dans le dossier "templates"
	 * @param attributes la liste des paramètres utilisables dans le "templates"
	 */

	public static final String render(String name, Map<String, Object> attributes) {
		try {
			StringWriter stringWriter = new StringWriter();
			Template template = FreeMarker.configuration.getTemplate(name);
			template.process(attributes, stringWriter);
			return stringWriter.toString();
		} catch (IOException | TemplateException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

}

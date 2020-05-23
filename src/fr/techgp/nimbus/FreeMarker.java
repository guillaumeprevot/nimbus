package fr.techgp.nimbus;

import java.io.File;
import java.io.IOException;

import fr.techgp.nimbus.server.render.RenderFreeMarker;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModelException;

public class FreeMarker {

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

}

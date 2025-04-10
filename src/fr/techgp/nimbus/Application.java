package fr.techgp.nimbus;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.techgp.nimbus.controllers.Controller;
import fr.techgp.nimbus.controllers.Templates;
import fr.techgp.nimbus.models.Database;
import fr.techgp.nimbus.server.Router;
import fr.techgp.nimbus.server.impl.JettyServer;
import fr.techgp.nimbus.utils.StringUtils;

public class Application {

	// Get expected system properties
	private static final String logPath = System.getProperty("nimbus.log", "nimbus.log");
	private static final String confPath = System.getProperty("nimbus.conf", "nimbus.conf");
	private static final boolean dev = Boolean.parseBoolean(System.getProperty("nimbus.dev", "false"));
	// Apply log configuration as soon as possible
	private static final Logger logger = prepareLogger();

	private static final Logger prepareLogger() {
		if (!"none".equals(logPath))
			System.setProperty("org.slf4j.simpleLogger.logFile", logPath);
		System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
		System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "dd/MM/yyyy HH:mm:ss");
		System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
		System.setProperty("org.slf4j.simpleLogger.showLogName", "false");
		System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");
		System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "true");
		System.setProperty("org.slf4j.simpleLogger.log.org.mongodb.driver", "warn");
		System.setProperty("org.slf4j.simpleLogger.log.org.eclipse.jetty", "warn");
		return LoggerFactory.getLogger(Application.class);
	}

	public static final void main(String[] args) {
		try {
			// Log start
			if (logger.isInfoEnabled())
				logger.info("Démarrage de l'application");

			// Load configuration from System properties
			Configuration configuration = new Configuration(confPath);

			// Prepare database
			Database.init(configuration);

			// Prepare FreeMarker
			Templates.init(dev);

			// Configure routes
			Router router = Controller.init(logger, configuration, dev);

			// Prepare Jetty
			JettyServer server = new JettyServer(configuration.getServerPort())
					.https(configuration.getServerKeystore(), configuration.getServerKeystorePassword())
					.invalidSNIHandler((req) -> logger.warn("Invalid SNI ({}) : {}", req.getRemoteAddr(), req.getPathInfo()))
					.multipart(configuration.getStorageFolder().getAbsolutePath(), -1L, -1L, 100 * 1024 * 1024)
					.session(configuration.getSessionTimeout(), configuration.getSessionCookiePath(), configuration.getSessionCookieDomain(), configuration.getSessionSecretKey())
					.errors(dev)
					.start(router);

			// Launch URL
			/*
			try {
				if (Desktop.getDesktop().isSupported(Action.BROWSE))
					Desktop.getDesktop().browse(new URI(configuration.getServerAbsoluteUrl()));
			} catch (Exception ex) {
				//
			}
			*/

			// Log started
			if (logger.isInfoEnabled()) {
				logger.info("Application démarrée");
				logger.info("Stockage dans : {}", configuration.getStorageFolder().getAbsolutePath());
				if (StringUtils.isNotBlank(configuration.getPostgresqlURL()))
					logger.info("PostgreSQL {} : {}", configuration.getPostgresqlUsername(), configuration.getPostgresqlURL());
				else
					logger.info("MongoDB : {}:{}/{}", configuration.getMongoHost(), configuration.getMongoPort(), configuration.getMongoDatabase());
				logger.info("Plugins chargés : {}", configuration.getFacets().size());
				logger.info("Serveur en attente : {}", configuration.getServerAbsoluteUrl());
			}

			// Wait for commands
			try (Scanner scanner = new Scanner(System.in)) {
				while (scanner.hasNext()) {
					String command = scanner.next();
					if (configuration.getServerStopCommand().equals(command)) {
						logger.info("Arrêt de l'application");
						server.stop();
						logger.info("Application arrêtée");
						break;
					}
				}
			}
		} catch (Exception ex) {
			if (logger.isErrorEnabled())
				logger.error("Le programme va quitter suite aux erreurs précédentes", ex);
		}
	}

}

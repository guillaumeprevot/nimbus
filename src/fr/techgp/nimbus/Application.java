package fr.techgp.nimbus;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.techgp.nimbus.controllers.Controller;
import fr.techgp.nimbus.models.Mongo;
import spark.Spark;

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
		System.setProperty("org.slf4j.simpleLogger.log.org.mongodb.driver.cluster", "warn");
		return LoggerFactory.getLogger(Application.class);
	}

	public static final void main(String[] args) {
		try {
			// Log start
			if (logger.isInfoEnabled())
				logger.info("Démarrage de l'application");

			// Load configuration from System properties
			Configuration configuration = new Configuration(confPath);

			// Prepare MongoDB
			Mongo.init(configuration.getMongoHost(), configuration.getMongoPort(), configuration.getMongoDatabase());

			// Prepare server
			Spark.externalStaticFileLocation("public");
			if (configuration.getServerKeystore() != null)
				Spark.secure(configuration.getServerKeystore(), configuration.getServerKeystorePassword(), null, null);
			Spark.port(configuration.getServerPort());

			// Configure routes
			Controller.init(logger, configuration, dev);

			// Enable GZIP for all responses
			/*
			Spark.after("*", (request, response) -> {
				String accceptEncoding = request.headers("Accept-Encoding");
				if (accceptEncoding != null && accceptEncoding.startsWith("gzip,"))
					response.header("Content-Encoding", "gzip");
			});
			*/

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
				logger.info("Connexion MongoDB : {}:{}/{}", configuration.getMongoHost(), configuration.getMongoPort(), configuration.getMongoDatabase());
				logger.info("Serveur en attente : {}", configuration.getServerAbsoluteUrl());
			}

			// Wait for commands
			try (Scanner scanner = new Scanner(System.in)) {
				while (true) {
					String command = scanner.next();
					if (configuration.getServerStopCommand().equals(command)) {
						logger.info("Arrêt de l'application");
						Spark.stop();
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

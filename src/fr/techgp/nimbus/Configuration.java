package fr.techgp.nimbus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

public class Configuration {

	private final Properties properties = new Properties();

	private final String mongoHost;
	private final int mongoPort;
	private final String mongoDatabase;

	private final int serverPort;
	private final String serverKeystore;
	private final String serverKeystorePassword;
	private final String serverStopCommand;
	private final String serverAbsoluteUrl;

	private final File storageFolder;

	public Configuration(String path) throws IOException {
		super();
		// Inject optional configuration file properties as system properties
		File configFile = new File(path);
		if (configFile.exists()) {
			try (FileInputStream fis = new FileInputStream(configFile)) {
				this.properties.load(fis);
			}
		}

		this.mongoHost = getString("mongo.host", "localhost");
		this.mongoPort = getInt("mongo.port", 27017);
		this.mongoDatabase = getString("mongo.database", "nimbus");

		this.serverPort = getInt("server.port", 10001);
		this.serverKeystore = getString("server.keystore", null);
		this.serverKeystorePassword = getString("server.keystore.password", null);
		this.serverStopCommand = getString("server.stop.command", "stop");
		this.serverAbsoluteUrl = getString("server.absolute.url", (this.serverKeystore != null ? "https" : "http") + "://localhost:" + this.serverPort);

		this.storageFolder = new File(getString("storage.path", "storage"));
	}

	private final String getString(String property, String defaultValue) {
		// Look in command line System properties
		// If not found, look in configuration file
		// If not found, return default value
		return System.getProperty(property, this.properties.getProperty(property, defaultValue));
	}

	private final int getInt(String property, int defaultValue) {
		return Optional.ofNullable(getString(property, null)).map(Integer::valueOf).orElse(defaultValue).intValue();
	}

	public String getMongoHost() {
		return this.mongoHost;
	}

	public int getMongoPort() {
		return this.mongoPort;
	}

	public String getMongoDatabase() {
		return this.mongoDatabase;
	}

	public int getServerPort() {
		return this.serverPort;
	}

	public String getServerKeystore() {
		return this.serverKeystore;
	}

	public String getServerKeystorePassword() {
		return this.serverKeystorePassword;
	}

	public String getServerStopCommand() {
		return this.serverStopCommand;
	}

	public String getServerAbsoluteUrl() {
		return this.serverAbsoluteUrl;
	}

	public File getStorageFolder() {
		return this.storageFolder;
	}

}

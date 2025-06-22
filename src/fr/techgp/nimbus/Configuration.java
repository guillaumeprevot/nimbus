package fr.techgp.nimbus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.commons.io.FilenameUtils;

import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.server.MimeTypes;
import fr.techgp.nimbus.server.impl.JettyServer;
import fr.techgp.nimbus.utils.StringUtils;

public class Configuration {

	private final Properties properties = new Properties();

	private final String postgresqlURL;
	private final String postgresqlUsername;
	private final String postgresqlPassword;

	private final int serverPort;
	private final String serverKeystore;
	private final String serverKeystorePassword;
	private final String serverStopCommand;
	private final String serverAbsoluteUrl;

	private final boolean sessionOnClient;
	private final String sessionSecretKey;
	private final int sessionTimeout;
	private final String sessionCookiePath;
	private final String sessionCookieDomain;

	private final Set<String> serverUseTempFiles;
	private final File storageFolder;
	private final String clientDefaultTheme;
	private final String clientLoginBackground;
	private final String[] clientPlugins;
	private final int clientQuotaWarning;
	private final int clientQuotaDanger;
	private final String clientFaviconColor;
	private final Set<String> textFileExtensions;
	private final List<Facet> facets;

	public Configuration(String path) throws IOException {
		super();
		String[] pathParts = path.split(File.pathSeparator);
		for (String pathPart : pathParts) {
			File configFile = new File(pathPart);
			if (configFile.exists()) {
				try (FileInputStream fis = new FileInputStream(configFile)) {
					this.properties.load(fis);
				}
			}
		}

		this.postgresqlURL = getString("postgresql.url", "jdbc:postgresql://localhost:5432/nimbus");
		this.postgresqlUsername = getString("postgresql.username", "postgres");
		this.postgresqlPassword = getString("postgresql.password", "postgres");

		this.serverPort = getInt("server.port", 10001);
		this.serverKeystore = getString("server.keystore", null);
		this.serverKeystorePassword = getString("server.keystore.password", null);
		this.serverStopCommand = getString("server.stop.command", "stop");
		this.serverAbsoluteUrl = getString("server.absolute.url", (this.serverKeystore != null ? "https" : "http") + "://localhost:" + this.serverPort);

		this.sessionOnClient = getBoolean("session.on.client", false);
		this.sessionSecretKey = getString("session.secret.key", null);
		this.sessionTimeout = getInt("session.timeout", 3600);
		this.sessionCookiePath = getString("session.cookie.path", null);
		this.sessionCookieDomain = getString("session.cookie.domain", null);

		this.serverUseTempFiles = Set.of(getString("server.use.temp.files", "").split(","));
		this.storageFolder = new File(getString("storage.path", "storage"));
		this.clientDefaultTheme = getString("client.default.theme", "light");
		this.clientLoginBackground = getString("client.login.background", null);
		this.clientPlugins = getString("client.plugins", "default-before,note,application,secret,calendar,contacts,bookmarks,epub,pdf,video,audio,image,windows-shortcut,markdown,code,digest,text,default-open,default-after").split(",");
		this.clientQuotaWarning = getInt("client.quota.warning", 75);
		this.clientQuotaDanger = getInt("client.quota.danger", 90);
		this.clientFaviconColor = getString("client.favicon.color", null);
		this.textFileExtensions = Set.of(getString("text.file.extensions", "txt,md,markdown").split(","));

		this.facets = new ArrayList<>();
		this.loadFacets();
		this.configureMimeTypes();
	}

	private String getString(String property, String defaultValue) {
		// Look in command line System properties
		// If not found, look in configuration file
		// If not found, return default value
		return System.getProperty(property, this.properties.getProperty(property, defaultValue));
	}

	private int getInt(String property, int defaultValue) {
		String s = getString(property, null);
		return s == null ? defaultValue : Integer.parseInt(s);
	}

	private boolean getBoolean(String property, boolean defaultValue) {
		String s = getString(property, null);
		return s == null ? defaultValue : Boolean.parseBoolean(s);
	}

	private void loadFacets() {
		String facetNumbers = getString("facet.enabled", "");
		if (StringUtils.isBlank(facetNumbers))
			return;
		for (String facetNumber : facetNumbers.split(",")) {
			String s = getString("facet." + facetNumber, null);
			if (s == null) {
				System.out.println("Configuration référence une facet non définie : " + facetNumber);
				continue;
			}
			try {
				Facet instance = Class.forName(s).asSubclass(Facet.class).getDeclaredConstructor().newInstance();
				instance.init(this);
				// System.out.println("Loaded facet " + instance.getClass().getSimpleName());
				this.facets.add(instance);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
	}

	public final void configureMimeTypes() throws IOException {
		// Load default MIME types
		MimeTypes.loadDefaultMimeTypes();
		// Enlarge MIME type database with Jetty
		JettyServer.registerToMimeTypes();
		// Register some custom MIME types (should move to plugins later)
		MimeTypes.register(MimeTypes.HTML, "note");
		MimeTypes.register(MimeTypes.HTML, "application");
		MimeTypes.register(MimeTypes.JSON, "secret");
		MimeTypes.register(MimeTypes.JSON, "calendar");
		MimeTypes.register(MimeTypes.JSON, "contacts");
		MimeTypes.register(MimeTypes.JSON, "bookmarks");
		// Register the textFileExtensions as "text/plain" if not specified yet
		this.textFileExtensions.forEach((extension) -> {
			if (MimeTypes.byExtension(extension) == null)
				MimeTypes.register(MimeTypes.TEXT, extension);
		});
		// Use "application/octet-stream" as the default MIME type
		MimeTypes.registerDefault(MimeTypes.BINARY);
	}

	public String getPostgresqlURL() {
		return this.postgresqlURL;
	}

	public String getPostgresqlUsername() {
		return this.postgresqlUsername;
	}

	public String getPostgresqlPassword() {
		return this.postgresqlPassword;
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

	public boolean getSessionOnClient() {
		return this.sessionOnClient;
	}

	public String getSessionSecretKey() {
		return this.sessionSecretKey;
	}

	public int getSessionTimeout() {
		return this.sessionTimeout;
	}

	public String getSessionCookiePath() {
		return this.sessionCookiePath;
	}

	public String getSessionCookieDomain() {
		return this.sessionCookieDomain;
	}

	public Set<String> getServerUseTempFiles() {
		return this.serverUseTempFiles;
	}

	public File getStorageFolder() {
		return this.storageFolder;
	}

	public String getClientDefaultTheme() {
		return this.clientDefaultTheme;
	}

	public String getClientLoginBackground() {
		return this.clientLoginBackground;
	}

	public String[] getClientPlugins() {
		return this.clientPlugins;
	}

	public int getClientQuotaWarning() {
		return this.clientQuotaWarning;
	}

	public int getClientQuotaDanger() {
		return this.clientQuotaDanger;
	}

	public String getClientFaviconColor() {
		return this.clientFaviconColor;
	}

	public Set<String> getTextFileExtensions() {
		return this.textFileExtensions;
	}

	public List<Facet> getFacets() {
		return this.facets;
	}

	/**
	 * Cette méthode retourne le fichier associé à l'élément "item".
	 *
	 * @param item l'élément représentant un fichier dans le cloud
	 * @return le fichier associé à l'élément sur le disque
	 */
	public final File getStoredFile(Item item) {
		// Les fichiers sont répartis dans 256 dossiers. A partir de l'id du fichier, on en déduit son dossier
		long folder = item.id & 0xFF;
		// On récupère le dossier spécifié pour l'utilisateur
		File baseFolder = new File(this.storageFolder, item.userLogin);
		// Au final, on retourne le fichier "itemId" dans l'un des 256 dossiers du répertoire utilisateur
		File result = new File(baseFolder, Long.toString(folder, 16) + File.separator + item.id.toString());
		// S'assurer que les dossiers existent
		result.getParentFile().mkdirs();
		// OK, on est prêt
		return result;
	}

	/**
	 * Cette méthode met à jour les méta-données de l'élément "item".
	 *
	 * @param item l'élément représentant un fichier dans le cloud
	 * @param errorConsumer appelé en cas d'erreur lancée par une facet
	 */
	public final void updateStoredFile(Item item, BiConsumer<Facet, Throwable> errorConsumer) {
		// Informations sur l'élément
		File storedFile = getStoredFile(item);
		String extension = FilenameUtils.getExtension(item.name).toLowerCase();
		// Mise à jour des méta-données
		item.metadatas.clear();
		item.metadatas.put("length", storedFile.length());
		// Mettre à jour les propriétés spécifiques aux Facet
		for (Facet facet : this.facets) {
			try {
				if (facet.supports(extension))
					facet.updateMetadata(storedFile, extension, item.metadatas);
			} catch (Throwable ex) {
				if (errorConsumer != null)
					errorConsumer.accept(facet, ex);
			}
		}
	}

}

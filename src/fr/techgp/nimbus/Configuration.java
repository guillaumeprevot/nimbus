package fr.techgp.nimbus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.utils.StringUtils;

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
	private final String clientDefaultTheme;
	private final String[] clientPlugins;
	private final int clientQuotaWarning;
	private final int clientQuotaDanger;
	private final Set<String> textFileExtensions;
	private final List<Facet> facets;
	private final Map<String, String> mimetypes;

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

		this.mongoHost = getString("mongo.host", "localhost");
		this.mongoPort = getInt("mongo.port", 27017);
		this.mongoDatabase = getString("mongo.database", "nimbus");

		this.serverPort = getInt("server.port", 10001);
		this.serverKeystore = getString("server.keystore", null);
		this.serverKeystorePassword = getString("server.keystore.password", null);
		this.serverStopCommand = getString("server.stop.command", "stop");
		this.serverAbsoluteUrl = getString("server.absolute.url", (this.serverKeystore != null ? "https" : "http") + "://localhost:" + this.serverPort);

		this.storageFolder = new File(getString("storage.path", "storage"));
		this.clientDefaultTheme = getString("client.default.theme", "light");
		this.clientPlugins = getString("client.plugins", "default-before,epub,pdf,video,audio,image,markdown,html,code,text,windows-shortcut,default-open,default-after").split(",");
		this.clientQuotaWarning = getInt("client.quota.warning", 75);
		this.clientQuotaDanger = getInt("client.quota.danger", 90);
		this.textFileExtensions = Arrays.stream(getString("text.file.extensions", "txt,md").split(",")).collect(Collectors.toSet());
		this.facets = getInstances("facet", Facet.class);
		this.facets.forEach((f) -> f.init(this));
		this.mimetypes = getPairs("mimetype");
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

	private final Map<String, String> getPairs(String property) {
		Map<String, String> pairs = new HashMap<>();
		Consumer<Properties> part = (properties) -> {
			properties.stringPropertyNames().stream()
				.filter(key -> key.startsWith(property + "."))
				.forEach(key -> pairs.put(key.substring(property.length() + 1), properties.getProperty(key)));
		};
		part.accept(this.properties);
		part.accept(System.getProperties());
		return pairs;
	}

	private final <T> List<T> getInstances(String property, Class<T> clazz) {
		List<T> instances = new ArrayList<>();
		int i = 0;
		while (true) {
			String s = getString(property + "." + i, null);
			if (s == null)
				break;
			try {
				T instance = Class.forName(s).asSubclass(clazz).newInstance();
				instances.add(instance);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			i++;
		}
		return instances;
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

	public String getClientDefaultTheme() {
		return this.clientDefaultTheme;
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

	public Set<String> getTextFileExtensions() {
		return this.textFileExtensions;
	}

	public List<Facet> getFacets() {
		return this.facets;
	}

	/**
	 * Obtenir le type MIME à partir d'une extension :
	 * - le type MIME d'une extension peut être configuré dans "nimbus.conf"
	 * - le type MIME par défaut d'un fichier texte est "plain/text"
	 * - sinon, le type MIME est "application/octet-stream"
	 * 
	 * @return le type MIME pour cette extension
	 */
	public final String getMimeType(String extension) {
		if (StringUtils.isBlank(extension))
			return null;
		String result = this.mimetypes.get(extension);
		if (result != null)
			return result;
		if (this.textFileExtensions.contains(extension))
			return "text/plain";
		return "application/octet-stream";
	}

	/**
	 * Obtenir le type MIME à partir d'un nom de fichier
	 * @return le type MIME pour le fichier ou null si le nom de fichier ne contient pas de '.'
	 */
	public final String getMimeTypeByFileName(String filename) {
		int index = filename.lastIndexOf('.');
		if (index == -1)
			return getMimeType(null);
		String extension = filename.substring(index + 1).toLowerCase();
		return getMimeType(extension);
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
		item.content.clear();
		item.content.append("length", storedFile.length());
		// Mettre à jour les propriétés spécifiques aux Facet
		for (Facet facet : this.facets) {
			try {
				if (facet.supports(extension))
					facet.updateMetadata(storedFile, extension, item.content);
			} catch (Throwable ex) {
				if (errorConsumer != null)
					errorConsumer.accept(facet, ex);
			}
		}
	}

}

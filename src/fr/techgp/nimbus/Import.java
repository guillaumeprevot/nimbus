package fr.techgp.nimbus;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Objects;
import java.util.Scanner;
import java.util.Stack;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.techgp.nimbus.models.Database;
import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.models.User;

public class Import {

	// Get expected system properties
	private static final String LOG_PATH = System.getProperty("nimbus.log", "nimbus.log");
	private static final String CONF_PATH = System.getProperty("nimbus.conf", "nimbus.conf");
	private static final boolean OVERWRITE_EXISTING = Boolean.parseBoolean(System.getProperty("nimbus.overwrite", "false"));
	private static final boolean REFRESH_EXISTING = Boolean.parseBoolean(System.getProperty("nimbus.refresh", "false"));
	// Apply log configuration as soon as possible
	protected static final Logger logger = prepareLogger();

	private static Logger prepareLogger() {
		if (!"none".equals(LOG_PATH))
			System.setProperty("org.slf4j.simpleLogger.logFile", LOG_PATH);
		System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
		System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "dd/MM/yyyy HH:mm:ss");
		System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
		System.setProperty("org.slf4j.simpleLogger.showLogName", "false");
		System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");
		System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "true");
		System.setProperty("org.slf4j.simpleLogger.log.fr.techgp.nimbus.Import", "debug");
		System.setProperty("org.slf4j.simpleLogger.log.org.jaudiotagger", "off");
		return LoggerFactory.getLogger(Import.class);
	}

	public static void main(String[] args) {
		try {
			// Log start
			if (logger.isInfoEnabled())
				logger.info("Démarrage de l'import");

			// Load configuration from System properties
			Configuration configuration = new Configuration(CONF_PATH);

			// Prepare database
			Database.init(configuration);

			// Vérifier l'appel :
			// - soit "java fr.techgp.nimbus.Import <login> <folderPath> <parentId>" pour importer dans un dossier spécifique
			// - soit "java fr.techgp.nimbus.Import <login> <folderPath>" pour importer à la racine
			if (args.length < 2 || args.length > 3)
				throw new UnsupportedOperationException();

			// Récupérer l'utilisateur pour qui on fait l'import
			User user = User.findByLogin(args[0]);
			if (user == null)
				throw new UnsupportedOperationException("L'utilisateur " + args[0] + " n'existe pas.");

			// Récupérer le dossier à importer
			Path folder = Paths.get(args[1]);
			if (! Files.exists(folder))
				throw new UnsupportedOperationException("Le dossier à importer " + args[1] + " n'existe pas.");

			// Récupérer le dossier (facultatif) dans lequel on fera l'import
			Item parent = null;
			if (args.length == 3) {
				parent = Item.findById(Long.valueOf(args[2]));
				if (parent == null)
					throw new UnsupportedOperationException("L'élément parent " + args[2] + " n'existe pas.");
				if (!user.login.equals(parent.userLogin))
					throw new UnsupportedOperationException("L'élément parent " + args[2] + " n'appartient pas à l'utilisateur " + user.login + ".");
			}

			// Parcourir une première fois pour calculer la taille et le nombre d'éléments
			CountFileVisitor cfv = new CountFileVisitor();
			Files.walkFileTree(folder, cfv);

			// Demander confirmation
			System.out.printf("%s, %d fichiers, %d dossiers. Continuer (oui/non) ?\n", FileUtils.byteCountToDisplaySize(cfv.totalSize), cfv.fileCount, cfv.folderCount);
			try (Scanner scanner = new Scanner(System.in)) {
				if (!"oui".equals(scanner.next())) {
					System.out.println("Import annulé.");
					if (logger.isWarnEnabled())
						logger.warn("Import annulé.");
					return;
				}
			}

			// Lancer l'import une fois confirmé
			Files.walkFileTree(folder, new ImportFileVisitor(user, parent, configuration));

		} catch (UnsupportedOperationException ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Usage : java fr.techgp.nimbus.Import <login> <folderPath> [<parentId>]");
				if (ex.getMessage() != null)
					logger.error(ex.getMessage());
			}
		} catch (Exception ex) {
			if (logger.isErrorEnabled())
				logger.error("Le programme va quitter suite aux erreurs précédentes", ex);
		}
	}

	private static final class CountFileVisitor extends SimpleFileVisitor<Path> {

		public int fileCount = 0;
		public int folderCount = 0;
		public long totalSize = 0L;

		public CountFileVisitor() {
			//
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			Objects.requireNonNull(file);
			Objects.requireNonNull(attrs);
			this.fileCount++;
			this.totalSize += attrs.size();
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			Objects.requireNonNull(dir);
			if (exc != null)
				throw exc;
			this.folderCount++;
			return FileVisitResult.CONTINUE;
		}

	}

	private static final class ImportFileVisitor extends SimpleFileVisitor<Path> {

		private final User user;
		private final Item root;
		private final Stack<Long> idPath;
		private final Stack<Item> itemPath;
		private final Configuration configuration;

		public ImportFileVisitor(User user, Item root, Configuration configuration) {
			super();
			this.user = user;
			this.root = root;
			this.idPath = new Stack<>();
			this.itemPath = new Stack<>();
			this.configuration = configuration;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			Objects.requireNonNull(dir);
			Objects.requireNonNull(attrs);
			if (logger.isTraceEnabled())
				logger.trace(dir.toAbsolutePath() + File.separator);
			if (this.idPath.isEmpty()) {
				this.idPath.push(this.root == null ? null : this.root.id);
				this.itemPath.push(this.root);
			} else {
				Item item = findOrAdd(dir, true);
				this.idPath.push(item.id);
				this.itemPath.push(item);
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			Objects.requireNonNull(file);
			Objects.requireNonNull(attrs);
			// Ajouter l'élément en base (et lui attribuer un id par la même occasion)
			Item item = findOrAdd(file, false);
			// Récupérer le fichier associé dans l'espace de stockage de l'utilisateur
			File storedFile = this.configuration.getStoredFile(item);
			// Vérifier si on zappe le fichier à importer
			boolean sameSize = storedFile.exists() && storedFile.length() == attrs.size();
			boolean sameDate = storedFile.exists() && storedFile.lastModified() == attrs.lastModifiedTime().toMillis();
			if (sameSize && sameDate && !OVERWRITE_EXISTING && !REFRESH_EXISTING) {
				if (logger.isTraceEnabled())
					logger.trace(file.toAbsolutePath() + " : exclu (n°" + item.id + ")");
				return FileVisitResult.CONTINUE;				
			}
			// Récupérer le nouveau fichier
			if (!sameDate || !sameSize || OVERWRITE_EXISTING) {
				// Copier le fichier dans l'espace de stockage de l'utilisateur
				Files.copy(file, storedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				// Mettre à jour les dates du fichier
				item.createDate = new Date(attrs.creationTime().toMillis());
				item.updateDate = new Date(attrs.lastModifiedTime().toMillis());
				if (logger.isDebugEnabled())
					logger.debug(file.toAbsolutePath() + " : importé (n°" + item.id + ")");
			} else {
				if (logger.isTraceEnabled())
					logger.trace(file.toAbsolutePath() + " : raffraichi (n°" + item.id + ")");
			}
			// Mettre à jour les méta-données
			this.configuration.updateStoredFile(item, (facet, th) -> {
				if (logger.isWarnEnabled())
					logger.warn("{} dans {} sur l'élément n°{} ({}) : {}",
							th.getClass().getName(), facet.getClass().getSimpleName(), item.id, item.name, th.getMessage());
			});
			// Sauvegarder l'élément
			Item.update(item);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			Objects.requireNonNull(dir);
			if (exc != null)
				throw exc;
			this.idPath.pop();
			this.itemPath.pop();
			return FileVisitResult.CONTINUE;
		}

		private Item findOrAdd(Path path, boolean folder) {
			String name = path.getFileName().toString();
			Item item = Item.findItemWithName(this.user.login, this.idPath.peek(), name);
			if (item == null)
				item = Item.add(this.user.login, this.itemPath.peek(), folder, name, null);
			return item;
		}

	}

}

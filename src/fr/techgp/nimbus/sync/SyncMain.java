package fr.techgp.nimbus.sync;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.Function;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import fr.techgp.nimbus.utils.StringUtils;

/**
 * Le point d'entrée du programme de synchronisation :
 * <ul>
 * <li>Chargement du fichier de configuration, si précisé, dams {@link SyncMain#loadConfigurationFile()}
 * <li>Chargement du fichier de log, si précisé, dans {@link SyncMain#loadLogFile()}
 * <li>Interactions avec l'utilisateur pour obtenir les options manquantes ou incorrectes dans
 *   {@link SyncMain#getPropertyAsString(String, String, Function)} et {@link SyncMain#getPropertyAsPassword(String, String, Function)}
 * <li>Configuration de {@link Sync} puis authentification avec {@link Sync#authenticateAndGetJSESSIONID()}
 * <li>Synchronisation des dossiers demandés avec {@link Sync#run(String, String)}
 * </ul>
 */
public class SyncMain {

	/*
	 * java -cp ... -Dnimbus.log=sync.log -Dnimbus.conf=sync.conf fr.techgp.nimbus.sync.SyncMain 0 1 2
	 */
	public static void main(String[] args) {
		// Conf
		loadConfigurationFile();
		// Logger
		try (PrintWriter writer = loadLogFile()) {
			String url = getPropertyAsString(
					"nimbus.url",
					"Please enter the server URL (https://host[:port])",
					(s) -> s.matches("http(s)?://.*"));
			String login = getPropertyAsString(
					"nimbus.login",
					"Please enter your login",
					(s) -> true);
			char[] password = getPropertyAsPassword(
					"nimbus.password",
					"Please enter your password",
					(s) -> true);
			String direction = getPropertyAsString(
					"nimbus.direction",
					"Sync direction (u=upload/d=download) ?",
					(s) -> s.equalsIgnoreCase("u") || s.equalsIgnoreCase("d"));
			String traceOnly = getPropertyAsString(
					"nimbus.traceOnly",
					"Trace only (y/n) ?",
					(s) -> s.equalsIgnoreCase("y") || s.equalsIgnoreCase("n"));
			String skipExistingWithSameDateAndSize = getPropertyAsString(
					"nimbus.skipExistingWithSameDateAndSize",
					"Skip files with same date and size (y/n) ?",
					(s) -> s.equalsIgnoreCase("y") || s.equalsIgnoreCase("n"));
			String forceHTTPSCertificate = getPropertyAsString(
					"nimbus.forceHTTPSCertificate",
					"Force HTTPS certificate as trusted (y/n) ?",
					(s) -> s.equalsIgnoreCase("y") || s.equalsIgnoreCase("n"));

			// Prepare synchronization instance
			Sync sync = new Sync();
			sync.url = url;
			sync.login = login;
			sync.password = new String(password);
			sync.traceOnly = "y".equalsIgnoreCase(traceOnly);
			sync.skipExistingWithSameDateAndSize = "y".equalsIgnoreCase(skipExistingWithSameDateAndSize);
			sync.forceHTTPSCertificate = "y".equalsIgnoreCase(forceHTTPSCertificate);
			if (writer != null) {
				sync.ontrace = (s) -> { writer.format(s + "\n"); };
				sync.onerror = (s) -> { writer.format(s + "\n"); System.err.println(s); System.exit(3); };
			}
			// Authentication
			String jsessionid = sync.authenticateAndGetJSESSIONID();
			// Parcours des dossiers demandés
			for (String index : args) {
				System.out.println(index);
				String localFolder = getPropertyAsString(
						"nimbus." + index + ".localFolder",
						"Please enter the local folder",
						(s) -> new File(s).isDirectory());
				String serverFolderId = getPropertyAsString(
						"nimbus." + index + ".serverFolderId",
						"Please enter the server folder id (type 'root' to select all server content)",
						(s) -> "root".equals(s) || s.matches("\\d+"));
				System.out.printf("Sync local folder %s with server folder %s at %s with account %s (skip=%s, unsecured=%s)\n",
						localFolder, serverFolderId, url, login, skipExistingWithSameDateAndSize, forceHTTPSCertificate);
				sync.localFolder = new File(localFolder);
				sync.serverFolderId = "root".equals(serverFolderId) ? null : Long.valueOf(serverFolderId);
				sync.run(jsessionid, direction);
			}
		} catch (Exception ex) {
			System.err.println("Export failed due to an unexpected error");
			ex.printStackTrace();
		}
	}

	private static final void loadConfigurationFile() {
		String conf = System.getProperty("nimbus.conf");
		if (StringUtils.isNotBlank(conf)) {
			File file = new File(conf);
			if (file.exists()) {
				Properties p = new Properties();
				try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
					p.load(reader);
					for (Map.Entry<Object, Object> entry : p.entrySet()) {
						System.setProperty((String) entry.getKey(), (String) entry.getValue());
					}
				} catch (IOException ex) {
					System.err.println("Could not load configuration file \"" + conf + "\"");
					ex.printStackTrace();
					System.exit(1);
				}
			}
		}
	}

	private static final PrintWriter loadLogFile() {
		String log = System.getProperty("nimbus.log");
		if (StringUtils.isNotBlank(log)) {
			File file = new File(log);
			try {
				return new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8));
			} catch (FileNotFoundException ex) {
				System.err.println("Could not open log file \"" + log + "\"");
				ex.printStackTrace();
				System.exit(2);
			}
		}
		return null;
	}

	@SuppressWarnings("resource")
	private static final String getPropertyAsString(String name, String label, Function<String, Boolean> check) {
		String s = System.getProperty(name);
		Console console = System.console();
		while (s == null || s.trim().length() == 0 || !check.apply(s)) {
			if (console != null)
				s = console.readLine(label + ": ");
			else {
				System.out.println(label + ": ");
				s = new Scanner(System.in).nextLine();
			}
		}
		return s;
	}

	private static final char[] getPropertyAsPassword(String name, String label, Function<char[], Boolean> check) {
		String s = System.getProperty(name);
		char[] p = s == null ? new char[0] : s.toCharArray();
		Console console = System.console();
		while (p.length == 0 || !check.apply(p)) {
			if (console != null)
				p = console.readPassword(label + ": ");
			else {
				JPasswordField field = new JPasswordField();
				int result = JOptionPane.showConfirmDialog(null, field, label, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (result == JOptionPane.OK_OPTION)
					p = field.getPassword();
				else
					throw new NullPointerException("Operation cancelled by user");
			}
		}
		return p;
	}

}

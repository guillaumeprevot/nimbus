package fr.techgp.nimbus;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletResponse;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.techgp.nimbus.utils.StringUtils;
import fr.techgp.nimbus.utils.WebUtils;
import fr.techgp.nimbus.utils.WebUtils.MultiPartAdapter;

public class Sync {

	// Le système de fichier n'a pas forçément une précision à la milliseconde.
	// Dans mes tests (exFAT), les timestamp étaient arrondis aux 2 secondes supérieures
	public static final long DATE_EPSILON = 2000;

	// Cette classe représente une entrée de l'arborescence, présent en local et/ou sur le serveur
	public static final class SyncItem {
		// Le nom de l'élément
		public String name;
		// L'id s'il existe côté Nimbus ou null s'il existe en local mais n'est pas encore créé sur le serveur
		public Long nimbusId;
		// Indique s'il s'agit d'un dossier (true) ou d'un fichier (false) sur le serveur
		public boolean nimbusFolder;
		// Indiue s'il s'agit d'un dossier (true) ou d'un fichier (false) en local
		public boolean localFolder;
		// La date de modification sur le serveur (ou null s'il n'existe pas encore sur le serveur)
		public Long nimbusDate;
		// La date de modification en local (ou null s'il n'existe pas encore sur le disque)
		public Long localDate;
		// La taille de l'élément sur le serveur (ou null s'il n'existe pas encore sur le serveur)
		public Long nimbusLength;
		// La taille de l'élément en local (ou null s'il n'existe pas encore sur le disque)
		public Long localLength;
		// La liste des sous-éléments (mélangeant le contenu du serveur et/ou en local)
		public ArrayList<SyncItem> children;

		public boolean isNimbusFile() {
			return this.nimbusId != null && !this.nimbusFolder;
		}

		public boolean isLocalFile() {
			return this.localDate != null && !this.localFolder;
		}

		public boolean isNimbusFolder() {
			return this.nimbusId != null && this.nimbusFolder;
		}

		public boolean isLocalFolder() {
			return this.localDate != null && this.localFolder;
		}

		public boolean isSkipable(boolean skipExistingWithSameDateAndSize) {
			return skipExistingWithSameDateAndSize
					&& isNimbusFile()
					&& isLocalFile()
					&& (Math.abs(this.nimbusDate - this.localDate) < DATE_EPSILON || Math.abs(this.nimbusDate - this.localDate) == 3600000)
					&& this.nimbusLength.equals(this.localLength);
		}

		public boolean createNimbusFolder(Sync sync, String jsessionid, Long parentId) throws IOException {
			this.nimbusDate = System.currentTimeMillis();
			this.nimbusFolder = true;
			this.nimbusLength = null;
			String query = "/items/add/folder?name=" + URLEncoder.encode(name, "UTF-8") + "&parentId=" + (parentId == null ? "" : parentId.toString());
			this.nimbusId = sync.sendRequest(jsessionid, query, true, false, true, (c) -> {
				if (c.getResponseCode() != HttpServletResponse.SC_OK)
					return null;
				return Long.valueOf(IOUtils.toString(c.getInputStream(), StandardCharsets.UTF_8));
			});
			return this.nimbusId != null;
		}

		public boolean createLocalFolder(File file) throws IOException {
			this.localDate = this.nimbusDate;
			this.localFolder = true;
			this.localLength = null;
			return (file.exists() || file.mkdirs()) && file.setLastModified(this.nimbusDate);
		}

		public boolean updateNimbusFile(Sync sync, String jsessionid, Long parentId, File file) throws IOException {
			String query = "/files/upload";
			return sync.sendRequest(jsessionid, query, true, true, true, (c) -> {
				// L'idée est de ne pas intégrer HttpClient pour si peu
				try (MultiPartAdapter adapter = new MultiPartAdapter(c, "******")) {
					adapter.addFormField("parentId", parentId == null ? "" : parentId.toString());
					adapter.addFormField("updateDate", Long.toString(file.lastModified()));
					adapter.addFileUpload("files", SyncItem.this.name, file);
				}
				boolean result = c.getResponseCode() == HttpServletResponse.SC_OK;
				if (result) {
					this.nimbusDate = file.lastModified();
					this.nimbusFolder = false;
					this.nimbusLength = file.length();
				}
				return result;
			});
		}

		public boolean updateLocalFile(Sync sync, String jsessionid, File file) throws IOException {
			String query = "/files/stream/" + this.nimbusId;
			boolean success = sync.sendRequest(jsessionid, query, false, false, true, (c) -> {
				if (c.getResponseCode() != HttpServletResponse.SC_OK)
					return false;
				InputStream is = c.getInputStream();
				try (OutputStream os = new FileOutputStream(file)) {
					IOUtils.copyLarge(is, os);
				}
				return true;
			});
			if (success) {
				this.localDate = this.nimbusDate;
				this.localFolder = false;
				this.localLength = file.length();
				success = file.setLastModified(this.nimbusDate);
			}
			return success;
		}

		public boolean deleteNimbus(Sync sync, String jsessionid) throws IOException {
			if (this.nimbusId == null)
				return true;
			String query = "/trash/delete?itemIds=" + this.nimbusId;
			return sync.sendRequest(jsessionid, query, true, false, true, c -> {
				boolean result = c.getResponseCode() == HttpServletResponse.SC_OK;
				if (result) {
					SyncItem.this.nimbusId = null;
					SyncItem.this.nimbusDate = null;
					SyncItem.this.nimbusFolder = false;
					SyncItem.this.nimbusLength = null;
				}
				return result;
			});
		}

		public boolean deleteLocal(File file) throws IOException {
			if (this.localDate == null)
				return true;
			boolean success;
			if (this.localFolder) {
				FileUtils.deleteDirectory(file);
				success = !file.exists();
			} else {
				success = file.delete();
			}
			if (success) {
				this.localDate = null;
				this.localFolder = false;
				this.localLength = null;
			}
			return success;
		}

	}

	// Cette interface définit une méthode de traitement du résultat d'une requête HTTP envoyée sur le serveur
	public static interface SyncRequest<T> {
		public T consume(HttpURLConnection connection) throws IOException;
	}

	public String url;
	public String login;
	public String password;
	public File localFolder;
	public Long serverFolderId;
	public boolean traceOnly;
	public boolean skipExistingWithSameDateAndSize;
	public boolean forceHTTPSCertificate;
	public Consumer<String> ontrace = System.out::println;
	public Consumer<String> onerror = System.err::println;

	public final String authenticateAndGetJSESSIONID() throws IOException {
		String query = "/login.html";
		byte[] form = ("login=" + URLEncoder.encode(this.login, "UTF-8")
				+ "&password=" + URLEncoder.encode(this.password, "UTF-8")
				+ "&urlToLoad=" + URLEncoder.encode("/", "UTF-8")).getBytes();

		HttpURLConnection.setFollowRedirects(false); // pour avoir le retour de "/login.html" avec le(s) cookie(s) mais pas la redirection
		HttpURLConnection connection = (HttpURLConnection) new URL(this.url + query).openConnection();
		try {
			if (this.forceHTTPSCertificate && connection instanceof HttpsURLConnection)
				WebUtils.unsecuredConnectionUseAtYourOwnRisk((HttpsURLConnection) connection);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Content-Length", Integer.toString(form.length));
			connection.setUseCaches(false);
			connection.setDoInput(true); // pour lire les cookies
			connection.setDoOutput(true); // pour envoyer le formulaire

			// Envoyer le formulaire d'authentification
			connection.getOutputStream().write(form);

			// Si ça fonctionne, le serveur doit nous renvoyer un code 302 redirigeant vers la page demandée "/"
			if (connection.getResponseCode() != HttpServletResponse.SC_FOUND) {
				this.onerror.accept("Echec : authentification incorrecte");
				return null;
			}

			// Si ça fonctionne, le serveur doit aussi nous renvoyer un cookie de session
			// JSESSIONID=.....;Path=/;Secure;HttpOnly
			String cookieLine = connection.getHeaderField("Set-Cookie");
			if (cookieLine == null) {
				this.onerror.accept("Echec : cookie absent");
				return null;
			}

			// Extraction de la valeur de JSESSIONID
			String[] cookieEntry = cookieLine.split(";", 2)[0].split("=", 2);
			if (! "JSESSIONID".equals(cookieEntry[0])) {
				this.onerror.accept("Echec : cookie inattendu " + cookieEntry[0]);
				return null;
			}

			// Retourner le cookie
			return cookieEntry[1];
		} finally {
			connection.disconnect();
		}
	}

	public final JsonArray getContentFromServerFolder(String jsessionid) throws IOException {
		String query = "/items/list?recursive=true&deleted=false&parentId=" + (this.serverFolderId == null ? "" : this.serverFolderId.toString());
		return sendRequest(jsessionid, query, false, false, true, (c) -> {
			String json = IOUtils.toString(c.getInputStream(), StandardCharsets.UTF_8);
			return new JsonParser().parse(json).getAsJsonArray(); 
		});
	}

	public final ArrayList<SyncItem> buildTreeFromJSON(JsonArray array, Long parentId) {
		ArrayList<SyncItem> results = new ArrayList<>();
		for (int i = 0; i < array.size(); i++) {
			JsonObject o = array.get(i).getAsJsonObject();
			if (parentId == null && o.get("parentId") != null && !o.get("parentId").isJsonNull()
					|| parentId != null && (o.get("parentId") == null || o.get("parentId").getAsLong() != parentId.longValue()))
				continue;
			SyncItem item = new SyncItem();
			item.name = o.get("name").getAsString();
			item.nimbusId = o.get("id").getAsLong();
			item.nimbusFolder = o.get("folder").getAsBoolean();
			item.nimbusDate = o.get("updateDate").getAsLong();
			if (item.nimbusFolder)
				item.children = this.buildTreeFromJSON(array, item.nimbusId);
			else
				item.nimbusLength = o.get("length").getAsLong();
			results.add(item);
		}
		return results;
	}

	public final void mergeContentFromLocalFolder(ArrayList<SyncItem> items, File folder) {
		Map<String, SyncItem> map = items.stream().collect(Collectors.toMap((i) -> i.name, Function.identity()));
		folder.listFiles((child) -> {
			SyncItem item = map.get(child.getName());
			if (item == null) {
				item = new SyncItem();
				item.name = child.getName();
			}
			item.localFolder = child.isDirectory();
			item.localDate = child.lastModified();
			if (child.isFile())
				item.localLength = child.length();
			if (child.isDirectory()) {
				if (item.children == null)
					item.children = new ArrayList<>();
				this.mergeContentFromLocalFolder(item.children, child);
			}
			if (item.nimbusId == null)
				items.add(item);
			return false;
		});
	}

	public final void minimize(ArrayList<SyncItem> items) {
		Iterator<SyncItem> it = items.iterator();
		while (it.hasNext()) {
			SyncItem item = it.next();
			// Fichier identique des 2 côtés, on peut le zapper
			if (item.isSkipable(this.skipExistingWithSameDateAndSize)) {
				it.remove();
				continue;
			}
			// Dossier au contenu identique des 2 côtés, on peut le zapper
			if (item.isNimbusFolder() && item.isLocalFolder()) {
				if (item.children != null)
					this.minimize(item.children);
				if (item.children == null || item.children.isEmpty()) {
					it.remove();
					continue;
				}
			}
		}
	}

	public final void toLocal(String jsessionid, ArrayList<SyncItem> items, File folder, String prefix) throws IOException {
		if (items == null)
			return;
		for (SyncItem item : items) {
			File file = new File(folder, item.name);

			// Absent du serveur, il faut le supprimer en local
			if (item.nimbusId == null) {
				if (item.localFolder)
					this.ontrace.accept(prefix + "+- [DELETE] " + item.name);
				else
					this.ontrace.accept(prefix + "|- [DELETE] " + item.name);
				if (!this.traceOnly)
					item.deleteLocal(file);
				continue;
			}

			// Dossier présent sur le serveur
			if (item.nimbusFolder) {
				// Si un fichier local a le même nom qu'un dossier Nimbus, on supprime le fichier local
				if (file.isFile()) {
					this.ontrace.accept(prefix + "|- [DELETE] " + item.name);
					if (!this.traceOnly)
						item.deleteLocal(file);
				}
				// Si le dossier local est absent, on le crée
				if (file.exists()) {
					this.ontrace.accept(prefix + "+- " + item.name);
				} else {
					this.ontrace.accept(prefix + "+- [++++++] " + item.name);
					if (!this.traceOnly)
						item.createLocalFolder(file);
				}
				// On parcourt ensuite récursivement
				this.toLocal(jsessionid, item.children, file, prefix + "  ");
				continue;
			}

			// Fichier présent sur le serveur
			if (!item.nimbusFolder) {
				// Si un dossier local a le même nom d'un fichier Nimbus, on supprime le dossier local
				if (file.isDirectory()) {
					this.ontrace.accept(prefix + "+- [DELETE] " + item.name);
					if (!this.traceOnly)
						item.deleteLocal(file);
				}
				// Télécharger le fichier si nécessaire (différents) ou forcé (option)
				if (item.isSkipable(this.skipExistingWithSameDateAndSize)) {
					this.ontrace.accept(prefix + "|- " + item.name);
				} else {
					if (item.isLocalFile())
						this.ontrace.accept(prefix + "|- [UPDATE] " + item.name);
					else
						this.ontrace.accept(prefix + "|- [++++++] " + item.name);
					if (!this.traceOnly)
						item.updateLocalFile(this, jsessionid, file);
				}
			}
		}
	}

	public final void toServer(String jsessionid, ArrayList<SyncItem> items, File folder, Long parentId, String prefix) throws IOException {
		if (items == null)
			return;
		for (SyncItem item : items) {
			File file = new File(folder, item.name);

			// Absent en local, il faut le supprimer du serveur
			if (!file.exists()) {
				if (item.nimbusFolder)
					this.ontrace.accept(prefix + "+- [DELETE] " + item.name);
				else
					this.ontrace.accept(prefix + "|- [DELETE] " + item.name);
				if (!this.traceOnly)
					item.deleteNimbus(this, jsessionid);
				continue;
			}

			// Dossier présent en local
			if (item.localFolder) {
				// Si un fichier sur le serveur a le même nom qu'un dossier local, on supprime le fichier du serveur
				if (item.isNimbusFile()) {
					this.ontrace.accept(prefix + "|- [DELETE] " + item.name);
					if (!this.traceOnly)
						item.deleteNimbus(this, jsessionid);
				}
				// Si le dossier est absent du serveur, on le crée
				if (item.isNimbusFolder()) {
					this.ontrace.accept(prefix + "+- " + item.name);
				} else {
					this.ontrace.accept(prefix + "+- [++++++] " + item.name);
					if (!this.traceOnly)
						item.createNimbusFolder(this, jsessionid, parentId);
				}
				// On parcourt ensuite récursivement
				this.toServer(jsessionid, item.children, file, item.nimbusId, prefix + "  ");
				continue;
			}

			// Fichier présent en local
			if (!item.localFolder) {
				// Si un dossier sur le serveur a le même nom d'un fichier local, on supprime le dossier du serveur
				if (item.isNimbusFolder()) {
					this.ontrace.accept(prefix + "+- [DELETE] " + item.name);
					if (!this.traceOnly)
						item.deleteNimbus(this, jsessionid);
				}
				// Téléverser le fichier si nécessaire (différents) ou forcé (option)
				if (item.isSkipable(this.skipExistingWithSameDateAndSize)) {
					this.ontrace.accept(prefix + "|- " + item.name);
				} else {
					if (item.isNimbusFile())
						this.ontrace.accept(prefix + "|- [UPDATE] " + item.name);
					else
						this.ontrace.accept(prefix + "|- [++++++] " + item.name);
					if (!this.traceOnly)
						item.updateNimbusFile(this, jsessionid, parentId, file);
				}
			}
		}
	}

	public final <T> T sendRequest(String jsessionid, String query, boolean isPost, boolean output, boolean input, SyncRequest<T> consumer) throws IOException {
		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection connection = (HttpURLConnection) new URL(this.url + query).openConnection();
		try {
			if (this.forceHTTPSCertificate && connection instanceof HttpsURLConnection)
				WebUtils.unsecuredConnectionUseAtYourOwnRisk((HttpsURLConnection) connection);
			if (isPost)
				connection.setRequestMethod("POST");
			connection.setDoOutput(output);
			connection.setDoInput(input);
			connection.setUseCaches(false);
			connection.setAllowUserInteraction(false);
			connection.setRequestProperty("Cookie", "JSESSIONID=" + jsessionid);
			return consumer.consume(connection);
		} finally {
			connection.disconnect();
		}
	}

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

	public static void main(String[] args) {
		// Logger
		String log = System.getProperty("nimbus.log");
		PrintWriter writer = null;
		if (StringUtils.isNotBlank(log)) {
			File file = new File(log);
			try {
				writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8));
			} catch (FileNotFoundException ex) {
				System.err.println("Could not open log file \"" + log + "\"");
				ex.printStackTrace();
				return;
			}
		}
		try {
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
			String localFolder = getPropertyAsString(
					"nimbus.localFolder",
					"Please enter the local folder",
					(s) -> new File(s).isDirectory());
			String serverFolderId = getPropertyAsString(
					"nimbus.serverFolderId",
					"Please enter the server folder id (type 'root' to select all server content)",
					(s) -> "root".equals(s) || s.matches("\\d+"));
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

			System.out.printf("Sync local folder %s with server folder %s at %s with account %s (skip=%s, unsecured=%s)\n",
					localFolder, serverFolderId, url, login, skipExistingWithSameDateAndSize, forceHTTPSCertificate);
			// Prepare synchronization instance
			Sync export = new Sync();
			export.url = url;
			export.login = login;
			export.password = new String(password);
			export.localFolder = new File(localFolder);
			export.serverFolderId = "root".equals(serverFolderId) ? null : Long.valueOf(serverFolderId);
			export.traceOnly = "y".equalsIgnoreCase(traceOnly);
			export.skipExistingWithSameDateAndSize = "y".equalsIgnoreCase(skipExistingWithSameDateAndSize);
			export.forceHTTPSCertificate = "y".equalsIgnoreCase(forceHTTPSCertificate);
			if (writer != null) {
				PrintWriter w = writer;
				export.ontrace = (s) -> { w.format(s + "\n"); };
				export.onerror = (s) -> { w.format(s + "\n"); System.err.println(s); System.exit(1); };
			}
			// Authentication
			String jsessionid = export.authenticateAndGetJSESSIONID();
			// Extract content from Nimbus
			JsonArray array = export.getContentFromServerFolder(jsessionid);
			// Transform array into the item tree
			ArrayList<SyncItem> items = export.buildTreeFromJSON(array, export.serverFolderId);
			// Merge info from existing items found on disk
			export.mergeContentFromLocalFolder(items, export.localFolder);
			// Minimize modification tree
			export.minimize(items);
			// Run synchronization ...
			if (direction.equalsIgnoreCase("u"))
				// ... from local folder to server
				export.toServer(jsessionid, items, export.localFolder, export.serverFolderId, "");
			else
				// ... from server to local folder
				export.toLocal(jsessionid, items, export.localFolder, "");
		} catch (Exception ex) {
			System.err.println("Export failed due to an unexpected error");
			ex.printStackTrace();
		} finally {
			if (writer != null)
				writer.close();
		}
	}

}

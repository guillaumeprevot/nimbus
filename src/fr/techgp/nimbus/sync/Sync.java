package fr.techgp.nimbus.sync;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.techgp.nimbus.utils.WebUtils;

/**
 * Cette classe représente la logique de synchronisation : authentification, détection et application des modifications,
 * trace des changements effectués. Les opérations atomiques (création/modification/suppression de fichier/dossier sont
 * déléguées à {@link SyncItem}.
 */
public class Sync {

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

	public final void run(String jsessionid, String direction) throws IOException {
		// Extract content from Nimbus
		JsonArray array = this.getContentFromServerFolder(jsessionid);
		// Transform array into the item tree
		ArrayList<SyncItem> items = this.buildTreeFromJSON(array, this.serverFolderId);
		// Merge info from existing items found on disk
		this.mergeContentFromLocalFolder(items, this.localFolder);
		// Minimize modification tree
		this.minimize(items);
		// Run synchronization ...
		if (direction.equalsIgnoreCase("u"))
			// ... from local folder to server
			this.toServer(jsessionid, items, this.localFolder, this.serverFolderId, "");
		else
			// ... from server to local folder
			this.toLocal(jsessionid, items, this.localFolder, "");
	}

	protected final JsonArray getContentFromServerFolder(String jsessionid) throws IOException {
		String query = "/items/list?recursive=true&deleted=false&parentId=" + (this.serverFolderId == null ? "" : this.serverFolderId.toString());
		return sendRequest(jsessionid, query, false, false, true, (c) -> {
			String json = IOUtils.toString(c.getInputStream(), StandardCharsets.UTF_8);
			return new JsonParser().parse(json).getAsJsonArray();
		});
	}

	protected final ArrayList<SyncItem> buildTreeFromJSON(JsonArray array, Long parentId) {
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

	protected final void mergeContentFromLocalFolder(ArrayList<SyncItem> items, File folder) {
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

	protected final void minimize(ArrayList<SyncItem> items) {
		Iterator<SyncItem> it = items.iterator();
		while (it.hasNext()) {
			SyncItem item = it.next();
			// Fichier identique des 2 côtés, on peut le zapper
			if (item.isSkipable(this.skipExistingWithSameDateAndSize, this::dateDiffMatcher)) {
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

	protected final void toLocal(String jsessionid, ArrayList<SyncItem> items, File folder, String prefix) throws IOException {
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
				if (item.isSkipable(this.skipExistingWithSameDateAndSize, this::dateDiffMatcher)) {
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

	protected final void toServer(String jsessionid, ArrayList<SyncItem> items, File folder, Long parentId, String prefix) throws IOException {
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
				if (item.isSkipable(this.skipExistingWithSameDateAndSize, this::dateDiffMatcher)) {
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

	protected final <T> T sendRequest(String jsessionid, String query, boolean isPost, boolean output, boolean input, SyncRequest<T> consumer) throws IOException {
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

	/**
	 * Soit "l1" et "l2" les dates de modification d'un fichier (1) en local et (2) sur le serveur.
	 *
	 * Idéalement, on voudrait considérer que la date est identique si "l1 == l2". Malheureusement :
	 * <ul>
	 * <li>avec l'heure d'été, on se retrouve parfois avec 1 heure exactement de décalage
	 * <li>le système de fichier n'a pas forçément une précision à la milliseconde (par exemple,
	 * dans mes tests avec exFAT, les timestamp étaient arrondis aux 2 secondes supérieures).
	 * </ul>
	 *
	 * La méthode utilisée sera donc optimiste en permettant une comparaison plus souple.
	 *
	 * @param diff la différence de temps, en ms, entre les 2 dates de modificaiton
	 * @return true si on "peut" considérer que les 2 dates sont les mêmes
	 */
	protected boolean dateDiffMatcher(long diff) {
		// return diff == 0L;
		return Math.abs(diff) < 2000/*2 secondes*/ || Math.abs(diff) == 3600000/*1 heure*/;
	}

}

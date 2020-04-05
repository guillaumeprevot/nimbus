package fr.techgp.nimbus.sync;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.function.LongPredicate;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import fr.techgp.nimbus.utils.WebUtils.MultiPartAdapter;

/**
 * Cette classe représente une entrée de l'arborescence, présent en local et/ou sur le serveur
 */
public class SyncItem {

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

	public boolean isSkipable(boolean skipExistingWithSameDateAndSize, LongPredicate dateDiffMatcher) {
		return skipExistingWithSameDateAndSize
				&& isNimbusFile()
				&& isLocalFile()
				&& dateDiffMatcher.test(this.nimbusDate - this.localDate)
				&& this.nimbusLength.equals(this.localLength);
	}

	public boolean createNimbusFolder(Sync sync, String jsessionid, Long parentId) throws IOException {
		this.nimbusDate = System.currentTimeMillis();
		this.nimbusFolder = true;
		this.nimbusLength = null;
		String query = "/items/add/folder?name=" + URLEncoder.encode(this.name, "UTF-8") + "&parentId=" + (parentId == null ? "" : parentId.toString());
		this.nimbusId = sync.sendRequest(jsessionid, query, true, false, true, (c) -> {
			if (c.getResponseCode() != HttpServletResponse.SC_OK)
				return (Long) null;
			try (InputStream stream = c.getInputStream()) {
				return Long.valueOf(IOUtils.toString(stream, StandardCharsets.UTF_8));
			}
		});
		return this.nimbusId != null;
	}

	public boolean createLocalFolder(File file) {
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
			try (InputStream is = c.getInputStream();
					OutputStream os = new FileOutputStream(file)) {
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

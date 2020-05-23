package fr.techgp.nimbus.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Date;

import com.google.gson.JsonArray;

import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.server.HaltException;
import fr.techgp.nimbus.server.Response;
import fr.techgp.nimbus.server.Route;
import fr.techgp.nimbus.utils.SparkUtils;
import fr.techgp.nimbus.utils.StringUtils;
import fr.techgp.nimbus.utils.WebUtils;

public class Downloads extends Controller {

	/**
	 * Cette méthode propose des alternatives de téléchargement en fonction d'une URL "url".
	 * Pas d'autocomplétion pour le moment.
	 * Il n'y avait que YouTube mais la méthode change trop souvent alors que je n'utilise pas cette fonction.
	 *
	 * (url) => [{label, value, name}, ...]
	 *
	 * @see YoutubeUtils
	 */
	public static final Route autocomplete = (request, response) -> {
		// String url = request.queryParams("url");
		JsonArray results = new JsonArray();
		return SparkUtils.renderJSON(response, results);
	};

	/**
	 * Cette méthode ajoute dans "parentId" un fichier "name" en téléchargeant le contenu de l'URL "url" et renvoie l'identifiant de l'élément créé.
	 *
	 * (url, name, parentId) => newId
	 *
	 * @see Item#add(String, Long, boolean, String, java.util.function.Consumer)
	 * @see Downloads#execute(Item, Response)
	 */
	public static final Route add = (request, response) -> {
		// Récupérer l'utilisateur connecté
		String userLogin = request.session().attribute("userLogin");
		// Extraire la requête
		String url = request.queryParameter("url");
		String name = SparkUtils.queryParamString(request, "name", url.substring(url.lastIndexOf('/') + 1));
		Long parentId = SparkUtils.queryParamLong(request, "parentId", null);
		// Vérifier l'unicité des noms
		if (Item.hasItemWithName(userLogin, parentId, name))
			return SparkUtils.haltConflict();
		// Ajouter l'élément
		Item item = Item.add(userLogin, parentId, false, name, (i) -> {
			i.content.append("sourceURL", url);
		});
		if (item == null)
			return SparkUtils.haltBadRequest();
		// Lancer le téléchargement
		return execute(item, response);
	};

	/**
	 * Cette méthode télécharge à nouveau le fichier de "itemId", dont l'URL d'origine avait été conservée dans "item.content.sourceURL".
	 *
	 * (itemId) => void
	 *
	 * @see Downloads#execute(Item, Response)
	 */
	public static final Route refresh = (request, response) -> {
		return actionOnSingleItem(request, request.queryParameter("itemId"), (item) -> {
			// Vérifier que le fichier en question a bien une URL à l'origine
			String url = item.content.getString("sourceURL");
			if (StringUtils.isBlank(url))
				return SparkUtils.haltBadRequest();
			// Lancer le téléchargement
			return execute(item, response);
		});
	};

	/**
	 * Cette méthode est appelée quand l'utilisateur a vu l'état du fichier téléchargé "itemId".
	 * On vide alors les attributs de l'élément qui stockait la progression du téléchargement.
	 *
	 * (itemId) => void
	 *
	 * @see Item#update(Item)
	 */
	public static final Route done = (request, response) -> {
		return actionOnSingleItem(request, request.queryParameter("itemId"), (item) -> {
			item.content.remove("status");
			item.content.remove("progress");
			item.updateDate = new Date();
			Item.update(item);
			return "";
		});
	};

	private static final Object execute(Item item, Response response) {
		try {
			File file = getFile(item);
			String sourceURL = item.content.getString("sourceURL");
			HttpURLConnection connection = WebUtils.openURL(sourceURL);

			// Vérifier avant de commencer que l'espace disque est suffisant
			long newLength = connection.getContentLengthLong();
			if (newLength > file.length())
				checkQuotaAndHaltIfNecessary(item.userLogin, newLength - file.length());

			// Utiliser le code de retour de la requête HTTP comme réponse
			//response.statusMessage(connection.getResponseMessage());
			response.status(connection.getResponseCode());

			// Vider le fichier actuel
			file.delete();
			item.content.append("status", "download");
			item.content.append("length", 0L);
			item.content.append("progress", 0);
			item.updateDate = new Date();
			Item.update(item);

			// Lancer le téléchargement de manière asynchrone
			new Thread(new DownloadURLRunnable(item, connection)).start();

			// Renvoyer l'id (surtout pour "add" mais pas utile pour "refresh")
			return item.id.toString();
		} catch (HaltException ex) {
			throw ex;
		} catch (Exception ex) {
			ex.printStackTrace();
			return SparkUtils.haltInternalServerError();
		}
	}

	public static final class DownloadURLRunnable implements Runnable {

		private Item item;
		private HttpURLConnection connection;

		public DownloadURLRunnable(Item item, HttpURLConnection connection) {
			super();
			this.item = item;
			this.connection = connection;
		}

		@Override
		public void run() {
			File file = getFile(this.item);
			String sourceURL = this.item.content.getString("sourceURL");
			try {
				// Taille de la réponse totale
				long totalLength = this.connection.getContentLengthLong();
				// Taille de la réponse déjà téléchargée
				long currentLength = 0L;
				// Progression (pourcentage si possible ou en Mo sinon)
				int progress = 0;
				int nextProgress = 0;

				try (InputStream in = this.connection.getInputStream(); OutputStream out = new FileOutputStream(file)) {
					byte[] buffer = new byte[1024 * 1024];
					int count;
					while ((count = in.read(buffer)) != -1) {
						// Ecriture
						out.write(buffer, 0, count);
						currentLength += count;
						// Progression
						if (totalLength == -1)
							nextProgress++; // Mo par Mo
						else
							nextProgress = (int) Math.round(currentLength * 100.0 / totalLength); // Pourcentage
						// Enregistrement en base
						if (nextProgress > progress) {
							progress = nextProgress;
							this.item.content.replace("length", currentLength);
							this.item.content.replace("progress", progress);
							this.item.updateDate = new Date();
							Item.update(this.item);
						}
					}
				} finally {
					this.connection.disconnect();
				}

				// Extraire les méta-données du fichier, y compris "length"
				updateFile(this.item, null, false);
				// Mark item as success
				this.item.content.put("status", "success");
			} catch (Exception ex) {
				// Pas de méta-données du fichier, uniquement "length"
				this.item.content.clear();
				this.item.content.put("length", file.length());
				// Mark item as error
				this.item.content.put("status", "error");
			}
			this.item.content.put("sourceURL", sourceURL);
			this.item.updateDate = new Date();
			Item.update(this.item);
		}
	}
}

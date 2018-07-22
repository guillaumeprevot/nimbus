package fr.techgp.nimbus.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Date;

import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.utils.SparkUtils;
import fr.techgp.nimbus.utils.StringUtils;
import fr.techgp.nimbus.utils.WebUtils;
import spark.Response;
import spark.Route;

public class Downloads extends Controller {

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
		String url = request.queryParams("url");
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
		return actionOnSingleItem(request, request.queryParams("itemId"), (item) -> {
			// Vérifier que le fichier en question a bien une URL à l'origine
			String url = item.content.getString("sourceURL");
			if (StringUtils.isBlank(url))
				return SparkUtils.haltBadRequest();
			// Lancer le téléchargement
			return execute(item, response);
		});
	};

	private static final Object execute(Item item, Response response) {
		try {
			HttpURLConnection connection = WebUtils.openURL(item.content.getString("sourceURL"));
			response.status(connection.getResponseCode());
			response.body(connection.getResponseMessage());
			new Thread(new DownloadURLRunnable(item, connection)).start();
			return item.id.toString();
		} catch (Exception ex) {
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
			this.item.content.append("status", "download");
			this.item.content.append("length", 0L);
			this.item.content.append("progress", 0);
			this.item.updateDate = new Date();
			Item.update(this.item);

			File file = getFile(this.item);
			String sourceURL = this.item.content.getString("sourceURL");
			try {
				String totalHeader = this.connection.getHeaderField("Content-Length");
				Long total = StringUtils.isNotBlank(totalHeader) ? Long.valueOf(totalHeader) : null;
				int progress = 0; // pourcentage si possible ou tous les 2 Mo sinon
				int newProgress = 0; // pourcentage si possible ou tous les 2 Mo sinon
				long current = 0L;
				try (InputStream in = this.connection.getInputStream(); OutputStream out = new FileOutputStream(file)) {
					byte[] buffer = new byte[1024 * 1024];
					int count = in.read(buffer);
					while (count != -1) {
						out.write(buffer, 0, count);
						current += count;
						newProgress = (int) ((total == null) ? (current / 2 * 1024 * 1024) : Math.round(current * 100.0 / total));
						if (newProgress > progress) {
							progress = newProgress;
							this.item.content.replace("length", current);
							this.item.content.replace("progress", progress);
							this.item.updateDate = new Date();
							Item.update(this.item);
						}
						count = in.read(buffer);
					}
				} finally {
					this.connection.disconnect();
				}

				// Extraire les méta-données du fichier, y compris "length"
				updateFile(this.item);
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

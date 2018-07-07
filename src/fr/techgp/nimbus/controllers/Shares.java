package fr.techgp.nimbus.controllers;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import fr.techgp.nimbus.models.Item;
import fr.techgp.nimbus.utils.SparkUtils;
import fr.techgp.nimbus.utils.StringUtils;
import spark.Route;

public class Shares extends Controller {

	/**
	 * Cette méthode crée un partage de l'élément "itemId" (éventuellement limité à "duration" minutes) et renvoie le mot de passe généré
	 *
	 * (itemId, duration) => sharedPassword
	 */
	public static final Route add = (request, response) -> {
		return actionOnSingleItem(request, request.queryParams("itemId"), (item) -> {
			// Extraire la requête
			Integer duration = SparkUtils.queryParamInteger(request, "duration", null);
			// Créer un partage avec un mot de passe aléatoire
			if (StringUtils.isBlank(item.sharedPassword)) {
				item.sharedDate = new Date();
				item.sharedPassword = StringUtils.randomString(30, true, true);
			}
			item.sharedDuration = duration;
			// Sauvegarder l'élément
			item.updateDate = new Date();
			Item.update(item);
			// Retourner le mot de passe permettant de générer l'URL de partage
			return item.sharedPassword;
		});
	};

	/**
	 * Cette méthode supprime l'éventuel partage de l'élément "itemId" et renvoie un texte vide
	 *
	 * (itemId) => ""
	 */
	public static final Route delete = (request, response) -> {
		return actionOnSingleItem(request, request.queryParams("itemId"), (item) -> {
			if (StringUtils.isBlank(item.sharedPassword))
				return SparkUtils.haltBadRequest();
			// Supprimer le partage
			item.sharedPassword = null;
			item.sharedDate = null;
			item.sharedDuration = null;
			// Sauvegarder l'élément
			item.updateDate = new Date();
			Item.update(item);
			return "";
		});
	};

	/**
	 * Cette méthode PUBLIQUE retourne le fichier ":itemId", à condition que le mot de passe "password" soit correct
	 *
	 * (itemId, password) => stream
	 *
	 * @see SparkUtils#renderFile(spark.Response, String, File, String)
	 */
	public static final Route get = (request, response) -> {
		// Extraire et vérifier la requête
		Long itemId = Long.valueOf(request.params(":itemId"));
		String password = request.queryParams("password");
		if (itemId == null || StringUtils.isBlank(password))
			return SparkUtils.haltBadRequest();
		// Rechercher l'élément et en vérifier l'accès
		Item item = Item.findById(itemId);
		if (item == null || item.folder || StringUtils.isBlank(item.sharedPassword) || !password.equals(item.sharedPassword))
			return SparkUtils.haltBadRequest();
		// Vérifier si la durée du partage (facultative) est dépassée
		if (item.sharedDuration != null) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(item.sharedDate);
			calendar.add(Calendar.MINUTE, item.sharedDuration);
			if (calendar.getTime().before(new Date()))
				return SparkUtils.haltBadRequest();
		}
		// Vérifier si le fichier existe
		File file = getFile(item);
		if (! file.exists())
			return SparkUtils.haltNotFound();
		// Retourner le résultat
		String mimeType = configuration.getMimeTypeByFileName(item.name);
		String fileName = item.name;
		return SparkUtils.renderFile(response, mimeType, file, fileName);
	};

}

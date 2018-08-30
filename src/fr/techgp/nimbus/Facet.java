package fr.techgp.nimbus;

import java.io.File;
import java.util.List;

import org.bson.Document;

import com.google.gson.JsonObject;

public interface Facet {

	/**
	 * Cette méthode peut-être surchargée par les Facet qui auraient besoin de la configuration pour s'initialiser.
	 *
	 * @param configuration la configuration venant du fichier de configuration
	 */
	default void init(Configuration configuration) {
		//
	}

	/**
	 * Cette méthode est appelée sur toutes les Facets pour connaitre la liste des plugins JS
	 * se trouvant dans le sous-dossier "public/plugins" qu'il faudra charger côté client.
	 *
	 * @param plugins la liste des fichiers à charger
	 */
	default void fillClientPlugins(List<String> plugins) {
		//
	}

	/**
	 * Cette méthode sert à déterminer la ou les facets d'un fichier quand on connait son extension.
	 *
	 * @param extension l'extension du fichier dont on cherche la facet (par exemple "jpg", "txt", ...)
	 * @return true si cette facet supporte les fichiers de ce type
	 */
	default boolean supports(String extension) {
		return false;
	}

	/**
	 * Cette méthode est appelé lors du remplissage de la liste des éléments en page principale afin de remonter les
	 * informations utiles uniquement.
	 *
	 * @param bson le document BSON dans lequel on a précédemment stocké des méta-données
	 * @param node le noeud JSON à remplir
	 */
	default void loadMetadata(Document bson, JsonObject node) {
		//
	}

	/**
	 * Cette méthode est appelée quand le fichier change, afin de mettre à jour les méta-données stockées en base.
	 *
	 * @param file le fichier qui a été modifié
	 * @param extension l'extension du fichier, pour aider si besoin
	 * @param bson le document BSON à mettre à jour qui sera conservé avec l'élément en base
	 * @throws Exception en cas d'erreur quelconque pendant la mise à jour des méta-données
	 */
	default void updateMetadata(File file, String extension, Document bson) throws Exception {
		//
	}

}

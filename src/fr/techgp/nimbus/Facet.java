package fr.techgp.nimbus;

import java.io.File;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.models.Metadatas;

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
	 * @param metadatas les méta-données précédemment extraitres du fichier et stockées en base
	 * @param node le noeud JSON à remplir
	 */
	default void loadMetadata(Metadatas metadatas, JsonObject node) {
		//
	}

	/**
	 * Cette méthode est appelée quand le fichier change, afin de mettre à jour les méta-données stockées en base.
	 *
	 * @param file le fichier qui a été modifié
	 * @param extension l'extension du fichier, pour aider si besoin
	 * @param metadatas les méta-données à mettre à jour afin de les stocker avec l'élément en base
	 * @throws Exception en cas d'erreur quelconque pendant la mise à jour des méta-données
	 */
	default void updateMetadata(File file, String extension, Metadatas metadatas) throws Exception {
		//
	}

}

package fr.techgp.nimbus;

import java.io.File;

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
	 * Cette méthode sert à déterminer la ou les facets d'un fichier quand on connait son extension.
	 *
	 * @param extension l'extension du fichier dont on cherche la facet (par exemple "jpg", "txt", ...)
	 * @return true si cette facet supporte les fichiers de ce type
	 */
	public boolean supports(String extension);

	/**
	 * Cette méthode est appelé lors du remplissage de la liste des éléments en page principale afin de remonter les
	 * informations utiles uniquement.
	 *
	 * @param bson le document BSON dans lequel on a précédemment stocké des méta-données
	 * @param node le noeud JSON à remplir
	 */
	public void loadMetadata(Document bson, JsonObject node);

	/**
	 * Cette méthode est appelée quand le fichier change, afin de mettre à jour les méta-données stockées en base.
	 *
	 * @param file le fichier qui a été modifié
	 * @param extension l'extension du fichier, pour aider si besoin
	 * @param bson le document BSON à mettre à jour qui sera conservé avec l'élément en base
	 * @throws Exception en cas d'erreur quelconque pendant la mise à jour des méta-données
	 */
	public void updateMetadata(File file, String extension, Document bson) throws Exception;

}

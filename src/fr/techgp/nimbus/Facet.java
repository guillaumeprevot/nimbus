package fr.techgp.nimbus;

import java.io.File;
import java.io.IOException;

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

	/**
	 * Cette méthode permet à une Facet d'indiquer qu'elle souhaite prendre en charge la génération des miniatures de ce type de fichier.
	 *
	 * @param extension l'extension du fichier dont on cherche à générer une miniature
	 * @return true si cette facet souhaite générer des miniatures pour les fichiers de ce type
	 */
	default boolean supportsThumbnail(String extension) {
		return false;
	}

	/**
	 * Cette méthode retourne le type mime de l'image qui sera générée par cette Facet.
	 *
	 * @param extension l'extension du fichier dont on cherche à générer une miniature
	 * @return le type MIME de l'image qui sera générée par generateThumbnail
	 */
	default String getThumbnailMimeType(String extension) {
		return null;
	}

	/**
	 * Cette méthode est appelée si la Facet l'a demandé et devra renvoyer un tableau de byte représentant la miniature (ou null).
	 *
	 * @param file le fichier dont on cherche à générer une miniature
	 * @param extension l'extension du fichier dont on cherche à générer une miniature
	 * @param targetWidth la largeur max en pixel de l'image à générer
	 * @param targetHeight la hauteur max en pixel de l'image à générer
	 * @return la miniature sous la forme d'un tableau de byte (ou null si la Facet n'a pas généré de miniature finalement)
	 * @throws IOException en cas de souci de lecture du fichier
	 */
	default byte[] generateThumbnail(File file, String extension, Integer targetWidth, Integer targetHeight) throws IOException  {
		return null;
	}

}

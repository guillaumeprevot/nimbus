package fr.techgp.nimbus.sync;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Cette interface définit une méthode de traitement du résultat d'une requête HTTP envoyée sur le serveur
 */
public interface SyncRequest<T> {
	public T consume(HttpURLConnection connection) throws IOException;
}
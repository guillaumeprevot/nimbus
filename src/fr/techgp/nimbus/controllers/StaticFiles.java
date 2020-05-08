package fr.techgp.nimbus.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.eclipse.jetty.io.EofException;

import fr.techgp.nimbus.utils.CryptoUtils;
import fr.techgp.nimbus.utils.SparkUtils;
import spark.Route;

/** Ce controlleur remplace la gestion des fichiers statiques par Spark afin de gérer la mise en cache par Etag / Last-Modified / If-None-Match / If-Modified-Since */
public class StaticFiles extends Controller {

	private static final FastDateFormat CACHE_DATE_FORMAT = FastDateFormat.getInstance("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);

	public static final Route publicFolder = (request, response) -> {
		String path = request.pathInfo();
		if (path.contains(".."))
			return SparkUtils.haltForbidden();
		File file = new File("public", path);
		if (!file.exists() || !file.isFile())
			return SparkUtils.haltNotFound();
		return sendCacheable(request.raw(), response.raw(), file);
	};

	public static final String sendCacheable(HttpServletRequest request, HttpServletResponse response, File file) throws IOException {
		// La date de modification du fichier sert de date pour le cache
		Date fileDate = new Date(file.lastModified());
		String lastModified = CACHE_DATE_FORMAT.format(fileDate);
		String etag = CryptoUtils.sha1Hex(lastModified);

		// En-têtes correspondantes aux infos calculées du cache
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Etag", etag);
		response.setHeader("Last-Modified", lastModified);

		//1er type de cache : If-None-Match :""9e3fa9259d22837a4e72fe8b69112968b88e3cca""
		String ifNoneMatch = request.getHeader("If-None-Match");
		if (ifNoneMatch == null || !ifNoneMatch.equals(etag)) {
			//2ème type de cache : If-Modified-Since :"Mon, 16 Mar 2015 07:42:10 GMT"
			String ifModifiedSince = request.getHeader("If-Modified-Since");
			if (ifModifiedSince == null || !ifModifiedSince.equals(lastModified)) {
				// Tant pis, pas de cache
				response.setStatus(HttpServletResponse.SC_OK);
				response.setHeader("Date", lastModified);
				// Indiquer le bon type MIME
				String extension = FilenameUtils.getExtension(file.getName());
				String mimetype = configuration.getMimeType(extension);
				response.setContentType(mimetype != null ? mimetype : "application/octet-stream");
				// Envoyer le fichier demandé
				try (InputStream is = new FileInputStream(file);
						OutputStream os = response.getOutputStream()) {
					IOUtils.copyLarge(is, os, new byte[1024 * 1024]);
				} catch (EofException ex) {
					// Requête interrompue par le client
				}
				return "";
			}
		}

		// OK, la donnée en cache semble à jour, on renvoie le statut 304 (Not Modified)
		response.setContentLength(0);
		response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		return "";
	}

}

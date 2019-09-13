package fr.techgp.nimbus.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.io.EofException;

import fr.techgp.nimbus.Configuration;
import fr.techgp.nimbus.utils.CryptoUtils;
import fr.techgp.nimbus.utils.SparkUtils;
import fr.techgp.nimbus.utils.StringUtils;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.resource.AbstractFileResolvingResource;
import spark.resource.ExternalResource;
import spark.resource.ExternalResourceHandler;

/** Ce filtre remplace la gestion des fichiers statiques de Spark qui ne gère pas la mise en cache par Etag / Last-Modified / If-None-Match / If-Modified-Since */
public class StaticFiles implements Filter {

	private static final DateFormat CACHE_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);

	private synchronized static final String formatHTTPDate(Date date) {
		return CACHE_DATE_FORMAT.format(date); 
	}

	private final Configuration configuration;
	private final ExternalResourceHandler handler;

	public StaticFiles(Configuration configuration, String folder) {
		if (StringUtils.isBlank(folder))
			throw new InvalidParameterException("'folder' is required");
		ExternalResource root = new ExternalResource(folder);
		if (! root.exists() || ! root.isDirectory())
			throw new InvalidParameterException("'folder' must point to an existing folder");
		this.handler = new ExternalResourceHandler(folder);
		this.configuration = configuration;
	}

	@Override
	public void handle(Request request, Response response) throws Exception {
		AbstractFileResolvingResource r = this.handler.getResource(request.raw());
		if (r == null || !r.isReadable())
			return;
		File file = r.getFile();
		if (!file.isFile())
			return;
		// System.out.println(file.getAbsolutePath());
		cacheable(request, response, this.configuration, file);
	}

	public static final void cacheable(Request request, Response response, Configuration configuration, File file) throws IOException {
		// La date de modification du fichier sert de date pour le cache
		Date fileDate = new Date(file.lastModified());
		String lastModified = formatHTTPDate(fileDate);
		String etag = CryptoUtils.sha1Hex(lastModified);

		// En-têtes correspondantes aux infos calculées du cache
		response.header("Cache-Control", "no-cache");
		response.header("Etag", etag);
		response.header("Last-Modified", lastModified);

		//1er type de cache : If-None-Match :""9e3fa9259d22837a4e72fe8b69112968b88e3cca""
		String ifNoneMatch = request.headers("If-None-Match");
		if (ifNoneMatch == null || !ifNoneMatch.equals(etag)) {
			//2ème type de cache : If-Modified-Since :"Mon, 16 Mar 2015 07:42:10 GMT"
			String ifModifiedSince = request.headers("If-Modified-Since");
			if (ifModifiedSince == null || !ifModifiedSince.equals(lastModified)) {
				// Tant pis, pas de cache
				response.header("Date", lastModified);
				// Indiquer le bon type MIME
				String extension = FilenameUtils.getExtension(file.getName());
				String mimetype = configuration.getMimeType(extension);
				response.type(mimetype != null ? mimetype : "application/octet-stream");
				// Envoyer le fichier demandé
				// System.out.println("sending file for " + file.getAbsolutePath());
				try (InputStream is = new FileInputStream(file);
						OutputStream os = response.raw().getOutputStream()/*GzipUtils.checkAndWrap(request.raw(), response.raw(), false)*/) {
					IOUtils.copy(is, os);
					Spark.halt();
					return;
				} catch (EofException ex) {
					// Requête interrompue par le client
				}
			}
		}

		// OK, la donnée en cache semble à jour, on renvoie le statut 304 (Not Modified)
		// System.out.println("using cache for " + file.getAbsolutePath());
		response.header("Content-Length", "0");
		SparkUtils.haltNotModified();
	}

}

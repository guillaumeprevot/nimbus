package fr.techgp.nimbus.server.render;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Locale;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.time.FastDateFormat;

import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Response;
import fr.techgp.nimbus.utils.CryptoUtils;

public class RenderStatic implements Render {

	private static final FastDateFormat CACHE_DATE_FORMAT = FastDateFormat.getInstance("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);

	private final File file;
	private final String mimeType;

	public RenderStatic(File file, String mimeType) {
		super();
		this.file = file;
		this.mimeType = mimeType;
	}

	@Override
	public void render(Request request, Response response, Charset charset, Supplier<OutputStream> stream) throws IOException {
		// En-tête
		response.type(this.mimeType != null ? this.mimeType : "application/octet-stream");

		// La date de modification du fichier sert de date pour le cache
		Date fileDate = new Date(this.file.lastModified());
		String lastModified = CACHE_DATE_FORMAT.format(fileDate);
		String etag = CryptoUtils.sha1Hex(lastModified);

		// En-têtes correspondantes aux infos calculées du cache
		response.header("Cache-Control", "no-cache");
		response.header("Etag", etag);
		response.header("Last-Modified", lastModified);

		//1er type de cache : If-None-Match :""9e3fa9259d22837a4e72fe8b69112968b88e3cca""
		String ifNoneMatch = request.header("If-None-Match");
		if (ifNoneMatch == null || !ifNoneMatch.equals(etag)) {
			//2ème type de cache : If-Modified-Since :"Mon, 16 Mar 2015 07:42:10 GMT"
			String ifModifiedSince = request.header("If-Modified-Since");
			if (ifModifiedSince == null || !ifModifiedSince.equals(lastModified)) {
				// Tant pis, pas de cache
				response.status(HttpServletResponse.SC_OK);
				response.header("Date", lastModified);
				// Envoyer le fichier demandé
				response.length(this.file.length());
				try (InputStream is = new FileInputStream(this.file)) {
					try (OutputStream os = stream.get()) {
						this.copy(is, os);
						return;
					}
				}
			}
		}

		// OK, la donnée en cache semble à jour, on renvoie le statut 304 (Not Modified)
		response.status(HttpServletResponse.SC_NOT_MODIFIED);
		response.length(0);
		try (OutputStream os = stream.get()) {
			//
		}
	}

}

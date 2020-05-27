package fr.techgp.nimbus.server.render;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletResponse;

import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Response;

public class RenderStatic implements Render {

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
		long lastModified = this.file.lastModified();
		String etag = etag(this.file);

		// En-têtes correspondantes aux infos calculées du cache
		response.header("Cache-Control", "no-cache");
		response.header("Etag", etag);
		response.dateHeader("Last-Modified", lastModified);

		//1er type de cache : If-None-Match :""9e3fa9259d22837a4e72fe8b69112968b88e3cca""
		String ifNoneMatch = request.header("If-None-Match");
		if (ifNoneMatch == null || !ifNoneMatch.equals(etag)) {
			//2ème type de cache : If-Modified-Since :"Mon, 16 Mar 2015 07:42:10 GMT"
			long ifModifiedSince = request.dateHeader("If-Modified-Since");
			if (ifModifiedSince == -1L || ifModifiedSince != lastModified) {
				// Tant pis, pas de cache
				response.status(HttpServletResponse.SC_OK);
				response.dateHeader("Date", lastModified);
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

	/** https://developer.mozilla.org/fr/docs/Web/HTTP/Headers/ETag */
	private String etag(File file) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			String digestValue = file.getAbsolutePath() + "?" + file.lastModified();
			digest.update(digestValue.getBytes(StandardCharsets.UTF_8));
			byte[] digestResult = digest.digest();
			return new BigInteger(1, digestResult).toString(16);
		} catch (NoSuchAlgorithmException ex) {
			throw new IOException(ex);
		}
	}
}

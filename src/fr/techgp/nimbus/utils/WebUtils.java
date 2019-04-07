package fr.techgp.nimbus.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;

public final class WebUtils {

	private WebUtils() {
		//
	}

	public static final String downloadURL(String url) {
		try {
			HttpURLConnection connection = openURL(url);
			try (InputStream stream = connection.getInputStream()) {
				return IOUtils.toString(stream, "UTF-8");
			}
		} catch (IOException ex) {
			return null;
		}
	}

	public static final String downloadURLAsDataUrl(String url, String defaultMimetype) {
		try {
			HttpURLConnection connection = openURL(url);
			try (InputStream stream = connection.getInputStream()) {
				byte[] bytes = IOUtils.toByteArray(connection.getInputStream());
				String mimetype = connection.getContentType();
				if (StringUtils.isBlank(mimetype))
					mimetype = defaultMimetype;
				return "data:" + mimetype + ";base64," + java.util.Base64.getEncoder().encodeToString(bytes);
			}
		} catch (IOException ex) {
			return null;
		}
	}

	public static final HttpURLConnection openURL(String url) throws IOException {
		HttpURLConnection.setFollowRedirects(true);
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.addRequestProperty("User-Agent", "Nimbus");
		connection.setRequestMethod("GET");
		connection.connect();
		return connection;
	}

	/* Méthode risquée, uniquement faite pour des tests */
	public static final void unsecuredConnectionUseAtYourOwnRisk(HttpsURLConnection connection) throws IOException {
		try {
			connection.setHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});
			SSLContext sc = SSLContext.getInstance("SSL");
			X509TrustManager manager = new X509TrustManager() {

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					//
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					//
				}
			};
			sc.init(null, new TrustManager[] { manager }, new java.security.SecureRandom());
			connection.setSSLSocketFactory(sc.getSocketFactory());
		} catch (NoSuchAlgorithmException | KeyManagementException ex) {
			throw new IOException(ex);
		}
	}

	/**
	 * Une classe pour générer une requête en "multipart/form-data" sans devoir inclure HttpClient.
	 * L'idée de départ vient d'ici : https://blog.morizyun.com/blog/android-httpurlconnection-post-multipart/index.html
	 */
	public static final class MultiPartAdapter implements AutoCloseable {

		private final HttpURLConnection connection;
		private final String boundary;
		private final OutputStream stream;

		public MultiPartAdapter(HttpURLConnection connection, String boundary) throws IOException {
			super();
			this.connection = connection;
			this.boundary = boundary;
			this.connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + this.boundary);
			this.stream = this.connection.getOutputStream();
		}

		public void addFormField(String name, String value) throws IOException {
			this.write("--" + this.boundary + "\r\n");
			this.write("Content-Disposition: form-data; name=\"" + name + "\"\r\n");
			this.write("Content-Type: text/plain; charset=UTF-8\r\n");
			this.write("\r\n");
			this.write(value + "\r\n");
			this.stream.flush();
		}

		public void addFileUpload(String name, String fileName, File file) throws IOException {
			this.write("--" + this.boundary + "\r\n");
			this.write("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"\r\n");
			this.write("\r\n");
			try (FileInputStream is = new FileInputStream(file)) {
				IOUtils.copyLarge(is, this.stream);
			}
			this.write("\r\n");
			this.stream.flush();
		}

		@Override
		public void close() throws IOException {
			this.write("--" + this.boundary + "--\r\n");
			this.stream.flush();
			this.stream.close();
		} 

		private void write(String s) throws IOException {
			this.stream.write(s.getBytes(StandardCharsets.UTF_8));
		}
	}
}

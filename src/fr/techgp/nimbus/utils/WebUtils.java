package fr.techgp.nimbus.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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

}

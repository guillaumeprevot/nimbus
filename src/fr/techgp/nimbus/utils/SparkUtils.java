package fr.techgp.nimbus.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Response;

public final class SparkUtils {

	private SparkUtils() {
		//
	}

	public static final String renderJSON(Response response, JsonElement object) {
		response.type("application/json");
		return object.toString();
	}

	public static final <T> String renderJSONCollection(Response response, List<T> objects, Function<T, JsonElement> transformer) {
		JsonArray a = new JsonArray();
		for (T object : objects) {
			a.add(transformer.apply(object));
		}
		response.type("application/json");
		return a.toString();
	}

	public static final String renderFile(Response response, String mimeType, File file, String fileName) throws IOException {
		if (fileName != null)
			response.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		response.header("Content-Length", Long.toString(file.length()));
		try (InputStream is = new FileInputStream(file)) {
			return renderStream(response, mimeType, is);
		}
	}

	public static final String renderBytes(Response response, String mimeType, byte[] bytes) throws IOException {
		response.header("Content-Length", Integer.toString(bytes.length));
		try (InputStream stream = new ByteArrayInputStream(bytes)) {
			return renderStream(response, mimeType, stream);
		}
	}

	public static final String renderStream(Response response, String mimeType, InputStream stream) throws IOException {
		response.type(mimeType);
		try (OutputStream os = response.raw().getOutputStream()) {
			IOUtils.copy(stream, os, 1024 * 1024);
		}
		return "";
	}

	public static final String queryParamString(Request request, String name, String defaultValue) {
		String s = request.queryParameter(name);
		return StringUtils.isBlank(s) ? defaultValue : s;
	}

	public static final Long queryParamLong(Request request, String name, Long defaultValue) {
		String s = request.queryParameter(name);
		return StringUtils.isBlank(s) ? defaultValue : Long.valueOf(s);
	}

	public static final long queryParamLong(Request request, String name, long defaultValue) {
		String s = request.queryParameter(name);
		return StringUtils.isBlank(s) ? defaultValue : Long.parseLong(s);
	}

	public static final Integer queryParamInteger(Request request, String name, Integer defaultValue) {
		String s = request.queryParameter(name);
		return StringUtils.isBlank(s) ? defaultValue : Integer.valueOf(s);
	}

	public static final int queryParamInteger(Request request, String name, int defaultValue) {
		String s = request.queryParameter(name);
		return StringUtils.isBlank(s) ? defaultValue : Integer.parseInt(s);
	}

	public static final Boolean queryParamBoolean(Request request, String name, Boolean defaultValue) {
		String s = request.queryParameter(name);
		return StringUtils.isBlank(s) ? defaultValue : Boolean.valueOf(s);
	}

	public static final boolean queryParamBoolean(Request request, String name, boolean defaultValue) {
		String s = request.queryParameter(name);
		return StringUtils.isBlank(s) ? defaultValue : Boolean.parseBoolean(s);
	}

	public static final String queryParamUrl(Request request, String name, String defaultValue) {
		String s = request.queryParameter(name);
		try {
			if (StringUtils.isNotBlank(s))
				return URLDecoder.decode(s, "UTF-8");
		} catch (Exception ex) { /* */ }
		return defaultValue;
	}

	public static final String getRequestLang(Request request) {
		String acceptLanguage = request.header("Accept-Language");
		if (acceptLanguage != null) {
			String[] options = acceptLanguage.split(",");
			for (String option : options) {
				// en, en-US, en-US;q=0.5 sont possibles
				// le "matches" fonctionne mais on se contentera de "startsWith", plus rapide
				// if (option.matches("^en(-.{2})?(;q=\\d+\\.\\d+)?$")) return "en";
				if (option.startsWith("en"))
					return "en";
				if (option.startsWith("fr"))
					return "fr";
			}
		}
		return "en";
	}

	/**
	 * Date format used for HTTP response headers
	 */
	public static final DateFormat HTTP_RESPONSE_HEADER_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
	static {
		HTTP_RESPONSE_HEADER_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

}

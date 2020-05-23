package fr.techgp.nimbus.utils;

import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import fr.techgp.nimbus.server.Request;

public final class SparkUtils {

	private SparkUtils() {
		//
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

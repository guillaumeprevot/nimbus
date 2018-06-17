package fr.techgp.nimbus.utils;

import javax.servlet.http.HttpServletResponse;

import spark.Request;
import spark.Spark;

public final class SparkUtils {

	private SparkUtils() {
		//
	}

	public static final Object unauthorized() {
		Spark.halt(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
		return null;
	}

	public static final Object forbidden() {
		Spark.halt(HttpServletResponse.SC_FORBIDDEN, "Forbidden"); // 403
		return null;
	}

	public static final String getRequestLang(Request request) {
		String acceptLanguage = request.headers("Accept-Language");
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

}

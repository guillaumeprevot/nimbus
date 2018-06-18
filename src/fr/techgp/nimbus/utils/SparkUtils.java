package fr.techgp.nimbus.utils;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import spark.Request;
import spark.Response;
import spark.Spark;

public final class SparkUtils {

	private SparkUtils() {
		//
	}

	public static final Object haltBadRequest() {
		Spark.halt(HttpServletResponse.SC_BAD_REQUEST, "Bad Request"); // 400
		return null;
	}

	public static final Object haltUnauthorized() {
		Spark.halt(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
		return null;
	}

	public static final Object haltForbidden() {
		Spark.halt(HttpServletResponse.SC_FORBIDDEN, "Forbidden"); // 403
		return null;
	}

	public static final Object haltNotFound() {
		Spark.halt(HttpServletResponse.SC_NOT_FOUND, "Not Found"); // 404
		return null;
	}

	public static final Object haltConflict() {
		Spark.halt(HttpServletResponse.SC_CONFLICT, "Conflict"); // 409
		return null;
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

	public static final boolean queryParamBoolean(Request request, String name) {
		return "true".equals(request.queryParams(name));
	}

	public static final Integer queryParamInteger(Request request, String name, Integer defaultValue) {
		return Optional.ofNullable(request.queryParams(name)).map(Integer::valueOf).orElse(defaultValue);
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

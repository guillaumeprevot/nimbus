package fr.techgp.nimbus.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@FunctionalInterface
public interface Matcher {

	public boolean matches(Request request);

	default public Matcher and(Matcher other) {
		return (req) -> this.matches(req) && other.matches(req);
	}

	default public Matcher or(Matcher other) {
		return (req) -> this.matches(req) || other.matches(req);
	}

	/**
	 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods
	 * https://tools.ietf.org/html/rfc7231#section-4 (Specifies GET, HEAD, POST, PUT, DELETE, CONNECT, OPTIONS, TRACE)
	 * https://tools.ietf.org/html/rfc5789#section-2 (Specifies PATCH)
	 * https://tools.ietf.org/html/rfc4918#section-9 (Specifies PROPFIND, PROPPATCH, MKCOL, COPY, MOVE, LOCK, UNLOCK for WEBDAV)
	 */
	public static class Method {

		public static final Matcher GET = is("GET");
		public static final Matcher HEAD = is("HEAD");
		public static final Matcher POST = is("POST");
		public static final Matcher PUT = is("PUT");
		public static final Matcher DELETE = is("DELETE");
		public static final Matcher CONNECT = is("CONNECT");
		public static final Matcher OPTIONS = is("OPTIONS");
		public static final Matcher TRACE = is("TRACE");
		public static final Matcher PATCH = is("PATCH");

		public static Matcher is(String method) {
			return (req) -> method.equals(req.method());
		}

		public static Matcher in(String... methods) {
			Set<String> set = new HashSet<>(Arrays.asList(methods));
			return (req) -> set.contains(req.method());
		}

	}

	/**
	 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept
	 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Type
	 */
	public static class Type {

		public static final Matcher TEXT = is("text/plain");
		public static final Matcher HTML = is("text/html");
		public static final Matcher CSS = is("text/css");
		public static final Matcher JS = is("application/javascript");
		public static final Matcher JSON = is("application/json");
		public static final Matcher XML = is("application/xml");

		public static Matcher is(String type) {
			return (req) -> type.equals(req.acceptType());
		}

		public static Matcher in(String... types) {
			Set<String> set = new HashSet<>(Arrays.asList(types));
			return (req) -> set.contains(req.acceptType());
		}

	}

	public static class Path {

		private static final String WILDCARD = "*";
		private static final char PARAMS_PREFIX = ':';

		public static Matcher of(String path) {
			if (path.startsWith(WILDCARD))
				return endsWith(path.substring(WILDCARD.length()));
			if (path.endsWith(WILDCARD))
				return startsWith(path.substring(0, path.length() - WILDCARD.length()));
			if (path.indexOf(PARAMS_PREFIX) == -1)
				return is(path);
			return params(path);
		}

		public static Matcher is(String path) {
			return (req) -> path.equals(req.pathInfo());
		}

		public static Matcher startsWith(String prefix) {
			return (req) -> req.pathInfo().startsWith(prefix);
		}

		public static Matcher endsWith(String suffix) {
			return (req) -> req.pathInfo().endsWith(suffix);
		}

		public static Matcher params(String path) {
			String[] model = path.substring(1).split("/");
			return (req) -> {
				// System.out.println("Checking " + req.pathInfo().substring(1) + " against " + path.substring(1));
				String[] value = req.pathInfo().substring(1).split("/");
				if (value.length != model.length)
					return false;
				Map<String, String> m = new HashMap<>();
				for (int i = 0; i < model.length; i++) {
					if (model[i].charAt(0) == PARAMS_PREFIX)
						m.put(model[i], value[i]); // keep track of param values
					else if (!model[i].equals(value[i]))
						return false; // and check to fixed chunck match to model
				}
				m.entrySet().forEach(e -> req.params(e.getKey(), e.getValue()));
				return true;
			};
		}

		public static Matcher like(String regexp) {
			Pattern r = Pattern.compile(regexp);
			return (req) -> r.matcher(req.pathInfo()).matches();
		}
	}

	public static Matcher not(Matcher matcher) {
		return (req) -> !matcher.matches(req);
	}

	public static Matcher all(Matcher... matchers) {
		return (req) -> {
			for (Matcher m : matchers) {
				if (!m.matches(req))
					return false;
			}
			return true;
		};
	}

	public static Matcher any(Matcher... matchers) {
		return (req) -> {
			for (Matcher m : matchers) {
				if (m.matches(req))
					return true;
			}
			return false;
		};
	}

}

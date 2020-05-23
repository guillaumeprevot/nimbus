package fr.techgp.nimbus.server;

import javax.servlet.http.HttpServletResponse;

public class Halt {

	public static final class Exception extends RuntimeException {

		private static final long serialVersionUID = 1L;
		private final int code;
		private final String body;

		public Exception(int code, String body) {
			super(null, null, false, false);
			this.code = code;
			this.body = body;
		}

		public int code() {
			return this.code;
		}

		public String body() {
			return this.body;
		}

	}

	public static final Halt.Exception now() {
		return new Halt.Exception(-1, null);
	}

	public static final Halt.Exception status(int code, String body) {
		return new Halt.Exception(code, body);
	}

	public static final Halt.Exception notModified() {
		return status(HttpServletResponse.SC_NOT_MODIFIED, ""); // 304
	}

	public static final Halt.Exception badRequest() {
		return status(HttpServletResponse.SC_BAD_REQUEST, "Bad Request"); // 400
	}

	public static final Halt.Exception unauthorized() {
		return status(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
	}

	public static final Halt.Exception forbidden() {
		return status(HttpServletResponse.SC_FORBIDDEN, "Forbidden"); // 403
	}

	public static final Halt.Exception notFound() {
		return status(HttpServletResponse.SC_NOT_FOUND, "Not Found"); // 404
	}

	public static final Halt.Exception conflict() {
		return status(HttpServletResponse.SC_CONFLICT, "Conflict"); // 409
	}

	public static final Halt.Exception internalServerError() {
		return status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error"); // 500
	}

	public static final Halt.Exception insufficientStorage() {
		// Emprunté de WEBDAV : https://tools.ietf.org/html/rfc4918#section-11.5
		return status(507, "Insufficient Storage"); // 507
	}

}

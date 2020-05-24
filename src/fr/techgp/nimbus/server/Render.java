package fr.techgp.nimbus.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonElement;

import fr.techgp.nimbus.server.render.RenderBytes;
import fr.techgp.nimbus.server.render.RenderFile;
import fr.techgp.nimbus.server.render.RenderJSON;
import fr.techgp.nimbus.server.render.RenderRedirect;
import fr.techgp.nimbus.server.render.RenderSamePage;
import fr.techgp.nimbus.server.render.RenderStatic;
import fr.techgp.nimbus.server.render.RenderStatus;
import fr.techgp.nimbus.server.render.RenderString;
import fr.techgp.nimbus.server.render.RenderThrowable;

@FunctionalInterface
public interface Render {

	public void render(Request request, Response response, Charset charset, Supplier<OutputStream> stream) throws IOException;

	default void copy(InputStream is, OutputStream os) throws IOException {
		int n;
		byte[] buffer = new byte[1024 * 1024];
		while ((n = is.read(buffer)) != -1) {
			os.write(buffer, 0, n);
		}
	}

	public static Render EMPTY = (request, response, charset, stream) -> { /* */ };

	/**
	 * This class make it possible to throw an Exception anywhere in {@link Route#handle(Request, Response)}
	 * while still providing a proper {@link Render}.
	 *
	 * By default, it is catched in {@link Router#process(Request, Response, Supplier)} to set {@link Response#body(Render)}.
	 */
	public static class Exception extends RuntimeException {

		private static final long serialVersionUID = 1L;
		private final Render render;

		public Exception(Render render) {
			super();
			this.render = render;
		}

		public Render get() {
			return this.render;
		}
	}

	public static Render string(String value) {
		return new RenderString(value);
	}

	public static Render bytes(byte[] value) {
		return new RenderBytes(value);
	}

	public static Render bytes(byte[] value, String mimeType, String fileName, boolean download) {
		return new RenderBytes(value, mimeType, fileName, download);
	}

	public static Render redirect(String url) {
		return new RenderRedirect(url);
	}

	public static Render samePage() {
		return new RenderSamePage();
	}

	public static Render staticFile(File file, String mimeType) {
		return new RenderStatic(file, mimeType);
	}

	public static Render file(File file) {
		return new RenderFile(file);
	}

	public static Render file(File file, String mimeType, String fileName, boolean download, boolean deleteAfter) {
		return new RenderFile(file, mimeType, fileName, download, deleteAfter);
	}

	public static Render throwable(java.lang.Throwable throwable) {
		return new RenderThrowable(throwable);
	}

	public static Render status(int code, String body) {
		return new RenderStatus(code, body);
	}

	public static Render notModified() {
		return new RenderStatus(HttpServletResponse.SC_NOT_MODIFIED, ""); // 304
	}

	public static Render badRequest() {
		return new RenderStatus(HttpServletResponse.SC_BAD_REQUEST, "Bad Request"); // 400
	}

	public static Render unauthorized() {
		return new RenderStatus(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
	}

	public static Render forbidden() {
		return new RenderStatus(HttpServletResponse.SC_FORBIDDEN, "Forbidden"); // 403
	}

	public static Render notFound() {
		return new RenderStatus(HttpServletResponse.SC_NOT_FOUND, "Not Found"); // 404
	}

	public static Render conflict() {
		return new RenderStatus(HttpServletResponse.SC_CONFLICT, "Conflict"); // 409
	}

	public static Render internalServerError() {
		return new RenderStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error"); // 500
	}

	public static Render insufficientStorage() {
		return new RenderStatus(507, "Insufficient Storage"); // Détourné de WEBDAV : https://tools.ietf.org/html/rfc4918#section-11.5
	}

	public static Render json(JsonElement object) {
		return new RenderJSON(object);
	}

	public static <T> Render json(List<T> objects, Function<T, JsonElement> transformer) {
		return new RenderJSON(objects, transformer);
	}

}

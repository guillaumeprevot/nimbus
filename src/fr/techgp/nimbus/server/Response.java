package fr.techgp.nimbus.server;

import javax.servlet.http.HttpServletResponse;

public class Response {

	private final HttpServletResponse response;
	private Body body;

	public Response(HttpServletResponse response) {
		this.response = response;
	}

	public HttpServletResponse raw() {
		return this.response;
	}

	public int status() {
		return this.response.getStatus();
	}

	public void status(int status) {
		this.response.setStatus(status);
	}

	public String type() {
		return this.response.getContentType();
	}

	public void type(String contentType) {
		this.response.setContentType(contentType);
	}

	public Body body() {
		return this.body;
	}

	public void body(Body body) {
		this.body = body;
	}

	public void body(String body) {
		this.body = Body.string(body);
	}

	public void body(byte[] body) {
		this.body = Body.bytes(body);
	}

	public String header(String name) {
		return this.response.getHeader(name);
	}

	public void header(String name, String value) {
		this.response.setHeader(name, value);
	}

	public void addHeader(String name, String value) {
		this.response.addHeader(name, value);
	}

	public void intHeader(String name, int value) {
		this.response.setIntHeader(name, value);
	}

	public void addIntHeader(String name, int value) {
		this.response.addIntHeader(name, value);
	}

	public void dateHeader(String name, long value) {
		this.response.setDateHeader(name, value);
	}

	public void addDateHeader(String name, long value) {
		this.response.addDateHeader(name, value);
	}

	public void renderRedirect(String location) {
		this.status(HttpServletResponse.SC_FOUND);
		this.header("Location", this.response.encodeRedirectURL(location));
		this.body(Body.empty());
	}

	public void length(int length) {
		this.response.setContentLength(length);
	}

	public Cookie cookie(String name, String value) {
		return cookie(name, "", value, null, -1, true, true);
	}

	public Cookie removeCookie(String name) {
		return cookie(name, "", "", null, 0, true, true);
	}

	public Cookie cookie(String name, String path, String value, String domain, int maxAge, boolean secure, boolean httpOnly) {
		Cookie cookie = new Cookie(name).path(path).value(value).domain(domain).maxAge(maxAge).secure(secure).httpOnly(httpOnly);
		this.response.addCookie(cookie.raw());
		return cookie;
	}

}

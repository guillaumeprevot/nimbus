package fr.techgp.nimbus.server.impl;

import javax.servlet.http.HttpServletResponse;

import fr.techgp.nimbus.server.Cookie;
import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Response;

public class ServletResponse implements Response {

	private final HttpServletResponse response;
	private Render body;

	public ServletResponse(HttpServletResponse response) {
		this.response = response;
	}

	public HttpServletResponse raw() {
		return this.response;
	}

	@Override
	public int status() {
		return this.response.getStatus();
	}

	@Override
	public void status(int status) {
		this.response.setStatus(status);
	}

	@Override
	public String type() {
		return this.response.getContentType();
	}

	@Override
	public void type(String contentType) {
		this.response.setContentType(contentType);
	}

	@Override
	public Render body() {
		return this.body;
	}

	@Override
	public void body(Render body) {
		this.body = body;
	}

	@Override
	public String header(String name) {
		return this.response.getHeader(name);
	}

	@Override
	public void header(String name, String value) {
		this.response.setHeader(name, value);
	}

	@Override
	public void addHeader(String name, String value) {
		this.response.addHeader(name, value);
	}

	@Override
	public void intHeader(String name, int value) {
		this.response.setIntHeader(name, value);
	}

	@Override
	public void addIntHeader(String name, int value) {
		this.response.addIntHeader(name, value);
	}

	@Override
	public void dateHeader(String name, long value) {
		this.response.setDateHeader(name, value);
	}

	@Override
	public void addDateHeader(String name, long value) {
		this.response.addDateHeader(name, value);
	}

	@Override
	public void length(long length) {
		this.response.setContentLengthLong(length);
	}

	@Override
	public Cookie cookie(String name, String value) {
		return cookie(name, "", value, null, -1, true, true);
	}

	@Override
	public Cookie cookie(String name, String path, String value, String domain, int maxAge, boolean secure, boolean httpOnly) {
		ServletCookie cookie = new ServletCookie(name, value);
		cookie.path(path);
		cookie.domain(domain);
		cookie.maxAge(maxAge);
		cookie.secure(secure);
		cookie.httpOnly(httpOnly);
		this.response.addCookie(cookie.raw());
		return cookie;
	}

	@Override
	public Cookie removeCookie(String name) {
		return cookie(name, "", "", null, 0, true, true);
	}

	@Override
	public Render redirect(String location) {
		this.status(HttpServletResponse.SC_FOUND);
		this.header("Location", this.response.encodeRedirectURL(location));
		return Render.EMPTY;
	}

}

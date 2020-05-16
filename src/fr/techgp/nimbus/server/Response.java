package fr.techgp.nimbus.server;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class Response {

	private final HttpServletResponse response;
	private String body;

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

	public String body() {
		return this.body;
	}

	public void body(String body) {
		this.body = body;
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

	public void redirect(String location) {
		try {
			this.response.sendRedirect(this.response.encodeRedirectURL(location));
		} catch (IOException ex) {
			// TODO Spark was hiding this exception. Should I do it ?
		}
	}

	public void setContentLength(int length) {
		this.response.setContentLength(length);
	}

	public void cookie(String name, String value) {
		cookie(name, value, null, null, -1, true, true);
	}

	public void cookie(String name, String value, int maxAge, boolean secured, boolean httpOnly) {
		cookie(name, value, null, null, maxAge, secured, httpOnly);
	}

	public void cookie(String name, String value, String path, String domain, int maxAge, boolean secured, boolean httpOnly) {
		Cookie cookie = new Cookie(name, value);
		cookie.setPath(path);
		cookie.setDomain(domain);
		cookie.setMaxAge(maxAge);
		cookie.setSecure(secured);
		cookie.setHttpOnly(httpOnly);
		this.response.addCookie(cookie);
	}

	public void removeCookie(String name) {
		cookie(name, "", null, null, 0, false, false);
	}

	public void removeCookie(String name, String path) {
		cookie(name, "", path, null, 0, false, false);
	}

}

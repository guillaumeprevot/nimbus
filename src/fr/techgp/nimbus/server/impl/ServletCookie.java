package fr.techgp.nimbus.server.impl;

import fr.techgp.nimbus.server.Cookie;

public class ServletCookie implements Cookie {

	private final javax.servlet.http.Cookie cookie;

	public ServletCookie(String name, String value) {
		super();
		this.cookie = new javax.servlet.http.Cookie(name, value);
	}

	public ServletCookie(javax.servlet.http.Cookie cookie) {
		super();
		this.cookie = cookie;
	}

	public javax.servlet.http.Cookie raw() {
		return this.cookie;
	}

	@Override
	public String name() {
		return this.cookie.getName();
	}

	@Override
	public String path() {
		return this.cookie.getPath();
	}

	@Override
	public void path(String path) {
		this.cookie.setPath(path);
	}

	@Override
	public String value() {
		return this.cookie.getValue();
	}

	@Override
	public void value(String value) {
		this.cookie.setValue(value);
	}

	@Override
	public String domain() {
		return this.cookie.getDomain();
	}

	@Override
	public void domain(String domain) {
		this.cookie.setDomain(domain);
	}

	@Override
	public int maxAge() {
		return this.cookie.getMaxAge();
	}

	@Override
	public void maxAge(int maxAge) {
		this.cookie.setMaxAge(maxAge);
	}

	@Override
	public boolean secure() {
		return this.cookie.getSecure();
	}

	@Override
	public void secure(boolean secure) {
		this.cookie.setSecure(secure);
	}

	@Override
	public boolean httpOnly() {
		return this.cookie.isHttpOnly();
	}

	@Override
	public void httpOnly(boolean httpOnly) {
		this.cookie.setHttpOnly(httpOnly);
	}


}

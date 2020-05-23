package fr.techgp.nimbus.server;

public class Cookie {

	private final javax.servlet.http.Cookie cookie;

	public Cookie(String name) {
		super();
		this.cookie = new javax.servlet.http.Cookie(name, "");
	}

	public Cookie(javax.servlet.http.Cookie cookie) {
		super();
		this.cookie = cookie;
	}

	public javax.servlet.http.Cookie raw() {
		return this.cookie;
	}

	public String name() {
		return this.cookie.getName();
	}

	public String path() {
		return this.cookie.getPath();
	}

	public Cookie path(String path) {
		this.cookie.setPath(path);
		return this;
	}

	public String value() {
		return this.cookie.getValue();
	}

	public Cookie value(String value) {
		this.cookie.setValue(value);
		return this;
	}

	public String domain() {
		return this.cookie.getDomain();
	}

	public Cookie domain(String domain) {
		this.cookie.setDomain(domain);
		return this;
	}

	public int maxAge() {
		return this.cookie.getMaxAge();
	}

	public Cookie maxAge(int maxAge) {
		this.cookie.setMaxAge(maxAge);
		return this;
	}

	public boolean secure() {
		return this.cookie.getSecure();
	}

	public Cookie secure(boolean secure) {
		this.cookie.setSecure(secure);
		return this;
	}

	public boolean httpOnly() {
		return this.cookie.isHttpOnly();
	}

	public Cookie httpOnly(boolean httpOnly) {
		this.cookie.setHttpOnly(httpOnly);
		return this;
	}

}

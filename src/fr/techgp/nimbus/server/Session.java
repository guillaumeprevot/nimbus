package fr.techgp.nimbus.server;

import javax.servlet.http.HttpSession;

public class Session {

	private final Request request;
	private final HttpSession session;

	public Session(Request request, HttpSession session) {
		this.request = request;
		this.session = session;
	}

	public HttpSession raw() {
		return this.session;
	}

	public String id() {
		return this.session.getId();
	}

	public long creationTime() {
		return this.session.getCreationTime();
	}

	public long lastAccessedTime() {
		return this.session.getLastAccessedTime();
	}

	public boolean isNew() {
		return this.session.isNew();
	}

	@SuppressWarnings("unchecked")
	public <T> T attribute(String name) {
		return (T) this.session.getAttribute(name);
	}

	public void attribute(String name, Object value) {
		this.session.setAttribute(name, value);
	}

	public void removeAttribute(String name) {
		this.session.removeAttribute(name);
	}

	public int maxInactiveInterval() {
		return this.session.getMaxInactiveInterval();
	}

	public void maxInactiveInterval(int interval) {
		this.session.setMaxInactiveInterval(interval);
	}

	public void invalidate() {
		this.request.invalidateSession();
		this.session.invalidate();
	}

}

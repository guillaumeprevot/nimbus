package fr.techgp.nimbus.server;

public interface Session {

	public String id();

	public long creationTime();

	public long lastAccessedTime();

	public boolean isNew();

	public <T> T attribute(String name);

	public void attribute(String name, Object value);

	public void removeAttribute(String name);

	public int maxInactiveInterval();

	public void maxInactiveInterval(int interval);

	public void invalidate();

}

package fr.techgp.nimbus.server;

public interface Cookie {

	public String name();

	public String path();

	public void path(String path);

	public String value();

	public void value(String value);

	public String domain();

	public void domain(String domain);

	public int maxAge();

	public void maxAge(int maxAge);

	public boolean secure();

	public void secure(boolean secure);

	public boolean httpOnly();

	public void httpOnly(boolean httpOnly);

}

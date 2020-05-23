package fr.techgp.nimbus.server;

public interface Response {

	public int status();
	public void status(int status);

	public String type();
	public void type(String contentType);

	public Render body();
	public void body(Render body);

	public String header(String name);
	public void header(String name, String value);
	public void addHeader(String name, String value);
	public void intHeader(String name, int value);
	public void addIntHeader(String name, int value);
	public void dateHeader(String name, long value);
	public void addDateHeader(String name, long value);

	public void length(long length);

	public Cookie cookie(String name, String value);
	public Cookie cookie(String name, String path, String value, String domain, int maxAge, boolean secure, boolean httpOnly);
	public Cookie removeCookie(String name);

	public Render redirect(String location);

}

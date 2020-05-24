package fr.techgp.nimbus.server;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Request {

	/** https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods */
	public String method();

	/** https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept */
	public String acceptType();

//	public String protocol(); // HTTP/1.1
//	public String scheme(); // http
//	public String host(); // localhost
//	public int port(); // 8080
//	public String contextPath(); // /webapp
//	public String servletPath() // /servlet
//	public String uri(); // /webapp/servlet/item/info/2
//	public String url(); // http://localhost:8080/webapp/servlet/item/info/2

	/** /item/info/2 */
	public String path();

	/** :itemId => 2, :toto => null */
	public String pathParameter(String name);
	/** :itemId => 2, :toto => defaultValue */
	public String pathParameter(String name, String defaultValue);
	/** see {@link Matcher.Path#params(String)} */
	public void addPathParameter(String name, String value);

	/** param1=value1&param2=value21&param2=value22 */
	public String query();

	/** param1 => value1, param2 => value21, toto => null */
	public String queryParameter(String name);
	/** param1 => value1, param2 => value21, toto => defaultValue */
	public String queryParameter(String name, String defaultValue);
	/** param1 => [value1], param2 => [value21, value22], toto => null */
	public String[] queryParameterValues(String name);

	default <T> T queryParameterObject(String name, Function<String, T> map, T defaultValue) {
		return Optional.ofNullable(queryParameter(name)).filter(s -> s.trim().length() > 0).map(map).orElse(defaultValue);
	}
	default boolean queryParameterBoolean(String name, boolean defaultValue) {
		return defaultValue ? !"false".equals(queryParameter(name)) : "true".equals(queryParameter(name));
	}
	default Boolean queryParameterBoolean(String name, Boolean defaultValue) {
		return queryParameterObject(name, Boolean::valueOf, defaultValue);
	}
	default long queryParameterLong(String name, long defaultValue) {
		return Optional.ofNullable(queryParameter(name)).stream().mapToLong(Long::parseLong).findAny().orElse(defaultValue);
	}
	default Long queryParameterLong(String name, Long defaultValue) {
		return queryParameterObject(name, Long::valueOf, defaultValue);
	}
	default int queryParameterInteger(String name, int defaultValue) {
		return Optional.ofNullable(queryParameter(name)).stream().mapToInt(Integer::parseInt).findAny().orElse(defaultValue);
	}
	default Integer queryParameterInteger(String name, Integer defaultValue) {
		return queryParameterObject(name, Integer::valueOf, defaultValue);
	}

//	public String contentType();
//	public int contentLength();
//	public String characterEncoding();

	public String ip();
//	public String userAgent();
//	public String referer();

	public String header(String name);
	public int intHeader(String name);
	public long dateHeader(String name);

	public <T> T attribute(String name);
	public void attribute(String name, Object value);
	public void removeAttribute(String name);

	public Cookie cookie(String name);
	public Cookie cookie(String name, String path);
	public Cookie cookie(Predicate<Cookie> predicate);

	public Upload upload(String name);
	public Collection<? extends Upload> uploads();

	public Session session();
	public Session session(boolean create);

}

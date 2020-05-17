package fr.techgp.nimbus.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import fr.techgp.nimbus.utils.StringUtils;

public class Request {

	/** The wrapped request */
	private final HttpServletRequest request;
	/** The parameters, extracted from path when ":" is found, during route selection */
	private final Map<String, String> params = new HashMap<>();
	/** Should "Method", "IP" and "Host" look into proxy modification in headers */
	private final boolean checkProxy;
	/** The session wrapper */
	private Session session;

	public Request(HttpServletRequest request, boolean checkProxy) {
		this.request = request;
		this.checkProxy = checkProxy;
	}

	public HttpServletRequest raw() {
		return this.request;
	}

	public String method() {
		return getMethod(this.request, this.checkProxy); // GET, POST, ...
	}

	public String acceptType() {
		return this.request.getHeader("Accept"); // text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8
	}

//	public String protocol() {
//		return this.request.getProtocol(); // HTTP/1.1
//	}

//	public String scheme() {
//		return this.request.getScheme(); // http
//	}

	public String host() {
		return getHost(this.request, this.checkProxy); // localhost
	}

//	public int port() {
//		return this.request.getServerPort(); // 8080
//	}

//	public String contextPath() {
//		return this.request.getContextPath(); // /webapp
//	}

//	public String servletPath() {
//		return this.request.getServletPath(); // /servlet
//	}

	public String path() {
		return this.request.getPathInfo(); // /item/info/2
	}

	public String pathParameter(String name) {
		return this.params.get(name); // :itemId => 2
	}

	public void pathParameter(String name, String value) {
		this.params.put(name, value); // see Matcher.Path.params
	}

	public String query() {
		return this.request.getQueryString(); // param1=value1&param2=value21&param2=value22
	}

	public String queryParameter(String name) {
		return this.request.getParameter(name); // param1 => value1, param2 => value21
	}

	public String[] queryParameterValues(String name) {
		return this.request.getParameterValues(name); // param1 => [value1], param2 => [value21, value22]
	}

	public String uri() {
		return this.request.getRequestURI(); // /webapp/servlet/item/info/2
	}

//	public String url() {
//		return this.request.getRequestURL().toString(); // http://localhost:8080/webapp/servlet/item/info/2
//	}

//	public String contentType() {
//		return this.request.getContentType();
//	}

//	public int contentLength() {
//		return this.request.getContentLength();
//	}

//	public String characterEncoding() {
//		return this.request.getCharacterEncoding();
//	}

	public String ip() {
		return getIP(this.request, this.checkProxy);
	}

//	public String userAgent() {
//		return this.request.getHeader("User-Agent");
//	}

//	public String referer() {
//		return this.request.getHeader("Referer");
//	}

	public String header(String name) {
		return this.request.getHeader(name);
	}

	public int intHeader(String name) {
		return this.request.getIntHeader(name);
	}

	public long dateHeader(String name) {
		return this.request.getDateHeader(name);
	}

	@SuppressWarnings("unchecked")
	public <T> T attribute(String name) {
		return (T) this.request.getAttribute(name);
	}

	public void attribute(String name, Object value) {
		this.request.setAttribute(name, value);
	}

	public void removeAttribute(String name) {
		this.request.removeAttribute(name);
	}

	public Session session() {
		if (this.session == null)
			this.session = new Session(this, this.request.getSession());
		return this.session;
	}

	public Session session(boolean create) {
		if (this.session == null)
			this.session = Optional.ofNullable(this.request.getSession(create))
					.map((s) -> new Session(this, s))
					.orElse(null);
		return this.session;
	}

	protected void invalidateSession() {
		this.session = null;
	}


	private static final String getMethod(HttpServletRequest request, boolean checkProxy) {
		if (checkProxy) {
			String r = request.getHeader("X-HTTP-Method");
			if (StringUtils.isNotBlank(r))
				return r;
			r = request.getHeader("X-HTTP-Method-Override");
			if (StringUtils.isNotBlank(r))
				return r;
			r = request.getHeader("X-METHOD-OVERRIDE");
			if (StringUtils.isNotBlank(r))
				return r;
		}
		return request.getMethod();
	}

	private static final String getIP(HttpServletRequest request, boolean checkProxy) {
		if (checkProxy) {
			String r = request.getHeader("X-Real-IP");
			if (StringUtils.isNotBlank(r))
				return r;
			r = request.getHeader("X-Forwarded-For");
			if (StringUtils.isNotBlank(r))
				return r.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private static final String getHost(HttpServletRequest request, boolean checkProxy) {
		if (checkProxy) {
			String r = request.getHeader("X-Forwarded-Host");
			if (StringUtils.isNotBlank(r))
				return r.split(",")[0].trim();
		}
		return request.getHeader("Host");
	}

}

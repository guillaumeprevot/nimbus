package fr.techgp.nimbus.server.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import fr.techgp.nimbus.server.Cookie;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Upload;
import fr.techgp.nimbus.utils.StringUtils;

public class ServletRequest implements Request {

	/** The wrapped request */
	private final HttpServletRequest request;
	/** The parameters, extracted from path when ":" is found, during route selection */
	private final Map<String, String> params = new HashMap<>();
	/** Should "Method", "IP" and "Host" look into proxy modification in headers */
	private final boolean checkProxy;
	/** The cookie collections */
	private List<ServletCookie> cookies;
	/** The upload collections */
	private List<ServletUpload> uploads;
	/** The session wrapper */
	private ServletSession session;

	public ServletRequest(HttpServletRequest request, boolean checkProxy) {
		this.request = request;
		this.checkProxy = checkProxy;
	}

	public HttpServletRequest raw() {
		return this.request;
	}

	@Override
	public String method() {
		return getMethod(this.request, this.checkProxy);
	}

	@Override
	public String acceptType() {
		return this.request.getHeader("Accept");
	}

	public String protocol() {
		return this.request.getProtocol();
	}

	public String scheme() {
		return this.request.getScheme();
	}

	public String host() {
		return getHost(this.request, this.checkProxy);
	}

	public int port() {
		return this.request.getServerPort();
	}

	public String contextPath() {
		return this.request.getContextPath();
	}

	public String servletPath() {
		return this.request.getServletPath();
	}

	public String uri() {
		return this.request.getRequestURI();
	}

	public String url() {
		return this.request.getRequestURL().toString();
	}

	@Override
	public String path() {
		return this.request.getPathInfo();
	}

	@Override
	public String pathParameter(String name) {
		return this.params.get(name);
	}

	@Override
	public String pathParameter(String name, String defaultValue) {
		return this.params.getOrDefault(name, defaultValue);
	}

	@Override
	public void addPathParameter(String name, String value) {
		this.params.put(name, value);
	}

	@Override
	public String query() {
		return this.request.getQueryString();
	}

	@Override
	public String queryParameter(String name) {
		return this.request.getParameter(name);
	}

	@Override
	public String queryParameter(String name, String defaultValue) {
		return Optional.ofNullable(this.request.getParameter(name)).orElse(defaultValue);
	}

	@Override
	public String[] queryParameterValues(String name) {
		return this.request.getParameterValues(name);
	}

	public String contentType() {
		return this.request.getContentType();
	}

	public int contentLength() {
		return this.request.getContentLength();
	}

	public String characterEncoding() {
		return this.request.getCharacterEncoding();
	}

	@Override
	public String ip() {
		return getIP(this.request, this.checkProxy);
	}

	public String userAgent() {
		return this.request.getHeader("User-Agent");
	}

	public String referer() {
		return this.request.getHeader("Referer");
	}

	@Override
	public String header(String name) {
		return this.request.getHeader(name);
	}

	@Override
	public int intHeader(String name) {
		return this.request.getIntHeader(name);
	}

	@Override
	public long dateHeader(String name) {
		return this.request.getDateHeader(name);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T attribute(String name) {
		return (T) this.request.getAttribute(name);
	}

	@Override
	public void attribute(String name, Object value) {
		this.request.setAttribute(name, value);
	}

	@Override
	public void removeAttribute(String name) {
		this.request.removeAttribute(name);
	}

	@Override
	public ServletCookie cookie(String name) {
		return this.cookie((c) -> c.name().equals(name));
	}

	@Override
	public ServletCookie cookie(String name, String path) {
		return this.cookie((c) -> c.name().equals(name) && c.path().equals(path));
	}

	@Override
	public ServletCookie cookie(Predicate<Cookie> predicate) {
		if (this.cookies == null)
			this.cookies = Arrays.stream(this.request.getCookies()).map(ServletCookie::new).collect(Collectors.toList());
		return this.cookies.stream().filter(predicate).findAny().orElse(null);
	}

	@Override
	public Upload upload(String name) {
		if (this.uploads == null)
			this.uploads = loadUploads();
		return this.uploads.stream().filter((u) -> u.name().equals(name)).findAny().orElse(null);
	}

	@Override
	public Collection<? extends Upload> uploads() {
		if (this.uploads == null)
			this.uploads = loadUploads();
		return this.uploads;
	}

	/** Cette méthode peut être surchargée */
	protected List<ServletUpload> loadUploads() {
		try {
			Collection<Part> parts = this.request.getParts();
			return parts.stream().map(ServletUpload::new).collect(Collectors.toList());
		} catch (IOException | ServletException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public ServletSession session() {
		if (this.session == null)
			this.session = new ServletSession(this, this.request.getSession());
		return this.session;
	}

	@Override
	public ServletSession session(boolean create) {
		if (this.session == null)
			this.session = Optional.ofNullable(this.request.getSession(create))
					.map((s) -> new ServletSession(this, s))
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

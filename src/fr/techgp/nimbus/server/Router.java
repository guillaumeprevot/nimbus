package fr.techgp.nimbus.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.io.EofException;

public class Router {

	private static final Router INSTANCE = new Router();

	public static final Router getInstance() {
		return INSTANCE;
	}

	public static final String NOT_FOUND = "<html><body><h2>404 Not found</h2></body></html>";
	public static final String INTERNAL_SERVER_ERROR = "<html><body><h2>500 Internal Server Error</h2></body></html>";

	private List<BeforeEntry> befores = new ArrayList<>();
	private List<RouteEntry> routes = new ArrayList<>();
	private List<AfterEntry> afters = new ArrayList<>();

	public void process(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Request req = new Request(request, false);
		Response res = new Response(response);
		try {
			for (BeforeEntry e : this.befores) {
				if (e.matcher.matches(req)) {
					e.filter.handle(req, res);
				}
			}
			for (RouteEntry e : this.routes) {
				if (e.matcher.matches(req)) {
					Object body = e.route.handle(req, res);
					if (body != null) {
						if (body instanceof byte[])
							res.body((byte[]) body);
						else if (body instanceof String)
							res.body((String) body);
						else
							throw new UnsupportedOperationException("Unsupported body type : " + body.getClass().getName());
					}
					if (res.body() != null)
						break;
				}
			}
			for (AfterEntry e : this.afters) {
				if (e.matcher.matches(req)) {
					e.filter.handle(req, res);
				}
			}

		} catch (HaltException ex) {
			if (ex.code() != -1)
				res.status(ex.code());
			if (ex.body() != null)
				res.body(ex.body());

		} catch (Exception ex) {
			res.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			res.body(INTERNAL_SERVER_ERROR);
		}

		// No match
		if (res.body() == null) {
			res.status(HttpServletResponse.SC_NOT_FOUND);
			res.body(NOT_FOUND);
		}

		if (!response.isCommitted()) {
			if (response.getContentType() == null)
				response.setContentType("text/html; charset=utf-8");
			// Check if GZIP is wanted/accepted and in that case handle that
			try (OutputStream os = checkAndWrap(request, response, true)) {
				res.body().render(os);
				os.flush();
			} catch (EofException ex) {
				// Requête interrompue par le client
			}
		}
	}

	public Router before(String path, Filter filter) {
		return before(Matcher.Path.of(path), filter);
	}

	public Router before(String method, String path, Filter filter) {
		return before(Matcher.Method.is(method).and(Matcher.Path.of(path)), filter);
	}

	public Router before(Matcher matcher, Filter filter) {
		BeforeEntry e = new BeforeEntry();
		e.matcher = matcher;
		e.filter = filter;
		this.befores.add(e);
		return this;
	}

	public Router get(String path, Route route) {
		return route(Matcher.Method.GET.and(Matcher.Path.of(path)), route);
	}

	public Router post(String path, Route route) {
		return route(Matcher.Method.POST.and(Matcher.Path.of(path)), route);
	}

	public Router redirect(String from, String to) {
		return route(Matcher.Path.is(from), (req, resp) -> { resp.renderRedirect(to); return ""; });
	}

	public Router route(String path, Route route) {
		return route(Matcher.Path.of(path), route);
	}

	public Router route(Matcher matcher, Route route) {
		RouteEntry e = new RouteEntry();
		e.matcher = matcher;
		e.route = route;
		this.routes.add(e);
		return this;
	}

	public Router after(String path, Filter filter) {
		return after(Matcher.Path.of(path), filter);
	}

	public Router after(String method, String path, Filter filter) {
		return after(Matcher.Method.is(method).and(Matcher.Path.of(path)), filter);
	}

	public Router after(Matcher matcher, Filter filter) {
		AfterEntry e = new AfterEntry();
		e.matcher = matcher;
		e.filter = filter;
		this.afters.add(e);
		return this;
	}

	private static OutputStream checkAndWrap(HttpServletRequest req, HttpServletResponse res, boolean requireWantsHeader) throws IOException {
		OutputStream responseStream = res.getOutputStream();
		// GZIP Support handled here. First we must ensure that we want to use gzip, and that the client supports gzip
		boolean acceptsGzip = Collections.list(req.getHeaders("Accept-Encoding")).stream().anyMatch(h -> h != null && h.contains("gzip"));
		boolean wantGzip = res.getHeaders("Content-Encoding").contains("gzip");
		if (acceptsGzip && (wantGzip || !requireWantsHeader)) {
			responseStream = new GZIPOutputStream(responseStream, true);
			if (!wantGzip)
				res.setHeader("Content-Encoding", "gzip");
		}
		return responseStream;
	}

	private static final class BeforeEntry {
		public Matcher matcher;
		public Filter filter;
	}

	private static final class RouteEntry {
		public Matcher matcher;
		public Route route;
	}

	private static final class AfterEntry {
		public Matcher matcher;
		public Filter filter;
	}

}

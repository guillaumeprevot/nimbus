package fr.techgp.nimbus.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Router {

	public static final String DEFAULT_CONTENT_TYPE = "text/html; charset=utf-8";

	private static final class RouteEntry {
		public Matcher matcher;
		public Route route;
	}

	private List<RouteEntry> befores = new ArrayList<>();
	private List<RouteEntry> routes = new ArrayList<>();
	private List<RouteEntry> afters = new ArrayList<>();

	public void process(Request request, Response response, Supplier<OutputStream> output) throws IOException {
		try {
			// Process ALL before filters
			process(request, response, this.befores, true);
			// Process routes if body ios not set yet and stop as soon as a body exists
			if (response.body() == null)
				process(request, response, this.routes, false);
			// Process ALL after filters
			process(request, response, this.afters, true);

		} catch (Exception ex) {
			// Reply 500 for exceptions
			response.type(DEFAULT_CONTENT_TYPE);
			response.body(Render.internalServerError());
		}

		// Reply 404 Not Found if no route matches request
		if (response.body() == null) {
			response.type(DEFAULT_CONTENT_TYPE);
			response.body(Render.notFound());
		}

		// Write response to output stream
		if (response.type() == null)
			response.type(DEFAULT_CONTENT_TYPE);
		response.body().render(request, response, StandardCharsets.UTF_8, output);
	}

	private void process(Request request, Response response, List<RouteEntry> entries, boolean processAll) throws Exception {
		for (RouteEntry entry : entries) {
			if (entry.matcher.matches(request)) {
				Render body = entry.route.handle(request, response);
				if (body != null)
					response.body(body);
				if (response.body() != null && !processAll)
					break;
			}
		}
	}

	public Router before(String path, Route filter) {
		return before(Matcher.Path.of(path), filter);
	}

	public Router before(String method, String path, Route filter) {
		return before(Matcher.Method.is(method).and(Matcher.Path.of(path)), filter);
	}

	public Router before(Matcher matcher, Route filter) {
		RouteEntry e = new RouteEntry();
		e.matcher = matcher;
		e.route = filter;
		this.befores.add(e);
		return this;
	}

	public Router route(String path, Route route) {
		return route(Matcher.Path.of(path), route);
	}

	public Router route(String method, String path, Route route) {
		return after(Matcher.Method.is(method).and(Matcher.Path.of(path)), route);
	}

	public Router route(Matcher matcher, Route route) {
		RouteEntry e = new RouteEntry();
		e.matcher = matcher;
		e.route = route;
		this.routes.add(e);
		return this;
	}

	public Router after(String path, Route filter) {
		return after(Matcher.Path.of(path), filter);
	}

	public Router after(String method, String path, Route filter) {
		return after(Matcher.Method.is(method).and(Matcher.Path.of(path)), filter);
	}

	public Router after(Matcher matcher, Route filter) {
		RouteEntry e = new RouteEntry();
		e.matcher = matcher;
		e.route = filter;
		this.afters.add(e);
		return this;
	}

	public Router get(String path, Route route) {
		return route(Matcher.Method.GET.and(Matcher.Path.of(path)), route);
	}

	public Router post(String path, Route route) {
		return route(Matcher.Method.POST.and(Matcher.Path.of(path)), route);
	}

	public Router redirect(String from, String to) {
		return route(Matcher.Path.is(from), (req, resp) -> Render.redirect(to));
	}

	/** TODO en faire un Render
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
	*/

}

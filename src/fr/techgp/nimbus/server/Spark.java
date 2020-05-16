package fr.techgp.nimbus.server;

public class Spark {

	public static final void before(String path, Filter filter) {
		Router.getInstance().before(path, filter);
	}

	public static final void get(String path, Route route) {
		Router.getInstance().route(Matcher.Method.GET.and(Matcher.Path.of(path)), route);
	}

	public static final void post(String path, Route route) {
		Router.getInstance().route(Matcher.Method.POST.and(Matcher.Path.of(path)), route);
	}

	public static final void halt() {
		throw new HaltException(-1, null);
	}

	public static final void halt(int code, String body) {
		throw new HaltException(code, body);
	}

	public static void redirect(String from, String to) {
		Router.getInstance().before(Matcher.Path.is(from), (req, resp) -> resp.redirect(to));
	}

}

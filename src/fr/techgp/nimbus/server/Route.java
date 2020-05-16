package fr.techgp.nimbus.server;

@FunctionalInterface
public interface Route {

	public Object handle(Request request, Response response) throws Exception;

}

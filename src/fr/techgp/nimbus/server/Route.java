package fr.techgp.nimbus.server;

@FunctionalInterface
public interface Route {

	public String handle(Request request, Response response) throws Exception;

}

package fr.techgp.nimbus.server;

@FunctionalInterface
public interface Route {

	public Render handle(Request request, Response response) throws Exception;

}

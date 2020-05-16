package fr.techgp.nimbus.server;

@FunctionalInterface
public interface Filter {

	public void handle(Request request, Response response) throws Exception;

}

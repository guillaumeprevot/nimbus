package fr.techgp.nimbus;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.MultiPartFormDataCompliance;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import fr.techgp.nimbus.server.Router;
import fr.techgp.nimbus.server.impl.ServletRequest;
import fr.techgp.nimbus.server.impl.ServletResponse;

// https://www.eclipse.org/jetty/documentation/current/
// https://www.eclipse.org/jetty/documentation/current/embedding-jetty.html
public class Jetty {

	public static final class RouterHandler extends SessionHandler {

		private final Router router;

		public RouterHandler(Router router) {
			this.router = router;
		}

		@Override
		public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			ServletRequest req = new ServletRequest(request, false); // TODO surcharger pour optimiser les Upload
			ServletResponse res = new ServletResponse(response);
			this.router.process(req, res, () -> {
				try {
					return response.getOutputStream();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			});
			baseRequest.setHandled(true);
		}

	}

	@SuppressWarnings("resource")
	public static final Server init(int port, String keystore, String keystorePassword, Router router) throws Exception {
		// Create server
		Server server = new Server();

		// Add connector
		ServerConnector connector;
		if (keystore != null) {
			SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
			sslContextFactory.setKeyStorePath(keystore);
			if (keystorePassword != null)
				sslContextFactory.setKeyStorePassword(keystorePassword);
			connector = new ServerConnector(server, sslContextFactory);
		} else {
			connector = new ServerConnector(server);
		}
		// Utilisation de MultiPartFormInputStream (rapide) au lieu de MultiPartInputStreamParser (legacy)
		// https://webtide.com/fast-multipart-formdata/
		connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration().setMultiPartFormDataCompliance(MultiPartFormDataCompliance.RFC7578);
		connector.setPort(port);
		server.setConnectors(new Connector[] { connector });

		// Add handler
		RouterHandler handler = new RouterHandler(router);
		handler.getSessionCookieConfig().setHttpOnly(true);
		server.setHandler(handler);

		// Start
		server.start();
		// server.join();
		return server;
	}

}

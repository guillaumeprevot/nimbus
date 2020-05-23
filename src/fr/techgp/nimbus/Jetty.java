package fr.techgp.nimbus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.jetty.http.MultiPartFormInputStream;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
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
import fr.techgp.nimbus.server.impl.ServletUpload;

// https://www.eclipse.org/jetty/documentation/current/
// https://www.eclipse.org/jetty/documentation/current/embedding-jetty.html
public class Jetty {

	/** This {@link Handler} uses a {@link Router} to handle incomming request and associated answers. */
	public static final class JettyRouterHandler extends SessionHandler {

		private final Router router;
		private final String uploadFolder;

		public JettyRouterHandler(Router router, String uploadFolder) {
			this.router = router;
			this.uploadFolder = uploadFolder;
		}

		@Override
		public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			ServletRequest req = new JettyServletRequest(request, false, this.uploadFolder);
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

	/** This {@link ServletRequest} optimizes uploads by overriding {@link ServletRequest#loadUploads} */
	public static class JettyServletRequest extends ServletRequest {

		private final String uploadFolder;

		public JettyServletRequest(HttpServletRequest request, boolean checkProxy, String uploadFolder) {
			super(request, checkProxy);
			this.uploadFolder = uploadFolder;
		}

		@Override
		protected List<ServletUpload> loadUploads() {
			try {
				prepareUploadRequest(this, this.uploadFolder);
				Collection<Part> parts = this.raw().getParts();
				List<ServletUpload> uploads = new ArrayList<>();
				for (Part part : parts) {
					ServletUpload upload = new ServletUpload(part);
					configureMultiPartWithJettyInternal(upload, part);
					uploads.add(upload);
				}
				return uploads;
			} catch (IOException | ServletException ex) {
				throw new RuntimeException(ex);
			}
		}

	}

	@SuppressWarnings("resource")
	public static final Server init(int port, String keystore, String keystorePassword, Router router, String uploadFolder) throws Exception {
		// Create server
		Server server = new Server();

		// Add connector
		ServerConnector connector = createConnector(server, keystore, keystorePassword);
		// Utilisation de MultiPartFormInputStream (rapide) au lieu de MultiPartInputStreamParser (legacy)
		// https://webtide.com/fast-multipart-formdata/
		connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration().setMultiPartFormDataCompliance(MultiPartFormDataCompliance.RFC7578);
		connector.setPort(port);
		server.setConnectors(new Connector[] { connector });

		// Add handler
		JettyRouterHandler handler = new JettyRouterHandler(router, uploadFolder);
		handler.getSessionCookieConfig().setHttpOnly(true);
		server.setHandler(handler);

		// Start
		server.start();
		// server.join();
		return server;
	}

	/** This method configures the request's attribute called "org.eclipse.jetty.multipartConfig" to control upload behaviour */
	public static final void prepareUploadRequest(ServletRequest request, String uploadFolder) {
		long maxFileSize = -1L; // peu importe
		long maxRequestSize = -1L; // on vérifie plus loin le quota
		int fileSizeThreshold = 100 * 1024 * 1024; // en mémoire jusqu'à 100 Mo
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(uploadFolder, maxFileSize, maxRequestSize, fileSizeThreshold));
	}

	/** This method creates an HTTPS connector if a keystore is specified, or an HTTP connector otherwise. */
	private static final ServerConnector createConnector(Server server, String keystore, String keystorePassword) {
		if (keystore != null) {
			SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
			sslContextFactory.setKeyStorePath(keystore);
			if (keystorePassword != null)
				sslContextFactory.setKeyStorePassword(keystorePassword);
			return new ServerConnector(server, sslContextFactory);
		}
		return new ServerConnector(server);
	}

	/** This method exposes internal file or bytes from Jetty's {@link MultiPartFormInputStream.MultiPart} implementation to optimize uploads. */
	private static final boolean configureMultiPartWithJettyInternal(ServletUpload upload, Part part) {
		if (part instanceof MultiPartFormInputStream.MultiPart) {
			MultiPartFormInputStream.MultiPart mpart = (MultiPartFormInputStream.MultiPart) part;
			upload.setFile(mpart.getFile());
			upload.setBytes(mpart.getBytes());
			return true;
		}
		return false;
	}

	/** This method exposes internal file or bytes from Jetty's deprecated MultiPart implementation to optimize uploads. */
	@SuppressWarnings({ "unused", "deprecation" })
	private static final boolean configureMultiPartWithJettyDeprecated(ServletUpload upload, Part part) {
		if (part instanceof org.eclipse.jetty.util.MultiPartInputStreamParser.MultiPart) {
			org.eclipse.jetty.util.MultiPartInputStreamParser.MultiPart mpart = (org.eclipse.jetty.util.MultiPartInputStreamParser.MultiPart) part;
			upload.setFile(mpart.getFile());
			upload.setBytes(mpart.getBytes());
			return true;
		}
		return false;
	}

}

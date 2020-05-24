package fr.techgp.nimbus.server.render;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;

import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Response;

public class RenderGZip implements Render {

	private final Render delegate;
	private final boolean forced;

	public RenderGZip(Render delegate, boolean forced) {
		super();
		this.delegate = delegate;
		this.forced = forced;
	}

	@Override
	public void render(Request request, Response response, Charset charset, Supplier<OutputStream> stream)
			throws IOException {
		this.delegate.render(request, response, charset, () -> {
			// Check if browser supports "gzip"
			boolean acceptGzip = Optional.ofNullable(request.header("Accept-Encoding")).orElse("").contains("gzip");
			// Check if route handler asked for gzip
			boolean wantGzip = Optional.ofNullable(response.header("Content-Encoding")).orElse("").contains("gzip");
			// Check that Content-Length header is not set (because le length would change)
			boolean noContentLength = response.header("Content-Length") == null;
			// If everything is OK for gzip, let's do it
			if (acceptGzip && noContentLength && (wantGzip || this.forced)) {
				// Ensure that the "Content-Encoding" is present
				if (!wantGzip)
					response.header("Content-Encoding", "gzip");
				try {
					// Provide a GZIPOutputStream to the delegated Render
					return new GZIPOutputStream(stream.get(), true);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
			// If gzip is not possible, provide the default OutputStream to the delegated Render
			return stream.get();
		});
	}

}

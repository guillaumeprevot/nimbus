package fr.techgp.nimbus.server.render;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletResponse;

import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Response;

public class RenderThrowable implements Render {

	private final Throwable throwable;

	public RenderThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	public Throwable getSource() {
		return this.throwable;
	}

	@Override
	public void render(Request request, Response response, Charset charset, Supplier<OutputStream> stream)
			throws IOException {
		response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		response.type("text/plain");
		try (PrintWriter w = new PrintWriter(stream.get(), true, charset)) {
			this.throwable.printStackTrace(w);
		}
	}

}

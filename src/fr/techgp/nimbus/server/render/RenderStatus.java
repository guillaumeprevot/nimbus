package fr.techgp.nimbus.server.render;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Supplier;

import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Response;

public class RenderStatus implements Render {

	private final int status;
	private final String body;

	public RenderStatus(int status, String body) {
		super();
		this.status = status;
		this.body = body;
	}

	@Override
	public void render(Request request, Response response, Charset charset, Supplier<OutputStream> stream) throws IOException {
		response.status(this.status);
		byte[] bytes = this.body == null ? new byte[0] : this.body.getBytes(charset);
		response.length(bytes.length);
		try (OutputStream os = stream.get()) {
			os.write(bytes);
		}
	}

}

package fr.techgp.nimbus.server.render;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Supplier;

import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Response;

public class RenderString implements Render {

	private final String value;

	public RenderString(String value) {
		super();
		this.value = value;
	}

	@Override
	public void render(Request request, Response response, Charset charset, Supplier<OutputStream> stream) throws IOException {
		byte[] bytes = this.value.getBytes(charset);
		response.length(bytes.length);
		try (OutputStream os = stream.get()) {
			os.write(bytes);
		}
	}

}

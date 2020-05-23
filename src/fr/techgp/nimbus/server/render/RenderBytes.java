package fr.techgp.nimbus.server.render;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Supplier;

import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Response;

public class RenderBytes implements Render {

	private final byte[] value;
	private final String mimeType;
	private final String fileName;
	private final boolean download;

	public RenderBytes(byte[] value) {
		this(value, null, null, true);
	}

	public RenderBytes(byte[] value, String mimeType, String fileName, boolean download) {
		super();
		this.value = value;
		this.mimeType = mimeType;
		this.fileName = fileName;
		this.download = download;
	}

	@Override
	public void render(Request request, Response response, Charset charset, Supplier<OutputStream> stream) throws IOException {
		if (this.mimeType != null)
			response.type(this.mimeType);

		if (this.fileName != null) {
			if (this.download)
				response.header("Content-Disposition", "attachment; filename=\"" + this.fileName + "\"");
			else
				response.header("Content-Disposition", "inline; filename=\"" + this.fileName + "\"");
		}

		response.length(this.value.length);

		try (OutputStream os = stream.get()) {
			os.write(this.value);
		}
	}

}

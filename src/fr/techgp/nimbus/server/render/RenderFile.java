package fr.techgp.nimbus.server.render;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Supplier;

import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Response;

public class RenderFile implements Render {

	private final File file;
	private final String mimeType;
	private final String fileName;
	private final boolean download;
	private final boolean deleteAfter;

	public RenderFile(File file) {
		this(file, null, null, true, false);
	}

	public RenderFile(File file, String mimeType, String fileName, boolean download, boolean deleteAfter) {
		super();
		this.file = file;
		this.mimeType = mimeType;
		this.fileName = fileName;
		this.download = download;
		this.deleteAfter = deleteAfter;
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

		response.length(this.file.length());

		try (OutputStream os = stream.get()) {
			if (this.file.exists()) {
				try (InputStream is = new FileInputStream(this.file)) {
					this.copy(is, os);
				} finally {
					if (this.deleteAfter)
						this.file.delete();
				}
			}
		}
	}

}

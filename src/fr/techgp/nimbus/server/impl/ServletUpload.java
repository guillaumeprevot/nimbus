package fr.techgp.nimbus.server.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.Part;

import fr.techgp.nimbus.server.Upload;

public class ServletUpload implements Upload {

	private final Part part;

	public ServletUpload(Part part) {
		this.part = part;
	}

	public Part raw() {
		return this.part;
	}

	@Override
	public String name() {
		return this.part.getName();
	}

	@Override
	public String fileName() {
		return this.part.getSubmittedFileName();
	}

	@Override
	public String contentType() {
		return this.part.getContentType();
	}

	@Override
	public long contentLength() {
		return this.part.getSize();
	}

	@Override
	public File getFile() {
		return null;
	}

	@Override
	public byte[] getBytes() {
		return null;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return this.part.getInputStream();
	}

	@Override
	public void delete() throws IOException {
		this.part.delete();
	}

}

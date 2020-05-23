package fr.techgp.nimbus.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface Upload {

	/** The input's name in the HTML form */
	public String name();

	/** The file's name, as it was on the client side */
	public String fileName();

	/** The MIME type of the file content */
	public String contentType();

	/** The file's size in bytes */
	public long contentLength();

	/** Either get the content as a file */
	public File getFile();

	/** Or get the content as an in-memory byte array */
	public byte[] getBytes();

	/** Or get the content as an InputStream */
	public InputStream getInputStream() throws IOException;

	/** To clean-up resources, if any */
	public void delete() throws IOException;

}

package fr.techgp.nimbus.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@FunctionalInterface
public interface Body {

	public void render(OutputStream stream) throws IOException;

	public static Body empty() {
		return (os) -> { /* */ };
	}

	public static Body string(String value) {
		return (os) -> os.write(value.getBytes(StandardCharsets.UTF_8));
	}

	public static Body bytes(byte[] value) {
		return (os) -> os.write(value);
	}

}

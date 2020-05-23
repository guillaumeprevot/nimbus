package fr.techgp.nimbus.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@FunctionalInterface
public interface Render {

	public void render(OutputStream stream) throws IOException;

	public static Render empty() {
		return (os) -> { /* */ };
	}

	public static Render string(String value) {
		return (os) -> os.write(value.getBytes(StandardCharsets.UTF_8));
	}

	public static Render bytes(byte[] value) {
		return (os) -> os.write(value);
	}

}

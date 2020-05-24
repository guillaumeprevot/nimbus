package fr.techgp.nimbus.server.render;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Response;

public class RenderJSON implements Render {

	private Supplier<JsonElement> content;

	public RenderJSON(Supplier<JsonElement> content) {
		this.content = content;
	}

	public RenderJSON(JsonElement content) {
		this.content = () -> content;
	}

	public <T> RenderJSON(List<T> objects, Function<T, JsonElement> transformer) {
		this.content = () -> {
			JsonArray a = new JsonArray();
			for (T o : objects) {
				a.add(transformer.apply(o));
			}
			return a;
		};
	}

	@Override
	public void render(Request request, Response response, Charset charset, Supplier<OutputStream> stream) throws IOException {
		JsonElement e = this.content.get();
		byte[] bytes = e.toString().getBytes(charset);
		response.type("application/json");
		response.length(bytes.length);
		try (OutputStream os = stream.get()) {
			os.write(bytes);
		}
	}

}

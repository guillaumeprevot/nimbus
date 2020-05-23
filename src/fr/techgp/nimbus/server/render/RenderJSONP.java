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

/** TODO Dépendance vers Gson */
public class RenderJSONP implements Render {

	private String callback = "callback";
	private Supplier<JsonElement> content;

	public RenderJSONP(Supplier<JsonElement> content) {
		this.content = content;
	}

	public RenderJSONP(JsonElement content) {
		this.content = () -> content;
	}

	public <T> RenderJSONP(List<T> objects, Function<T, JsonElement> transformer) {
		this.content = () -> {
			JsonArray a = new JsonArray();
			for (T o : objects) {
				a.add(transformer.apply(o));
			}
			return a;
		};
	}

	public RenderJSONP withCallback(String callback) {
		this.callback = callback;
		return this;
	}

	@Override
	public void render(Request request, Response response, Charset charset, Supplier<OutputStream> stream) throws IOException {
		JsonElement e = this.content.get();
		String js = this.callback + '(' + e.toString() + ')';
		byte[] bytes = js.getBytes(charset);
		response.type("application/javascript");
		response.length(bytes.length);
		try (OutputStream os = stream.get()) {
			os.write(bytes);
		}
	}

}

package fr.techgp.nimbus.server.render;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import fr.techgp.nimbus.server.Render;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Response;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/** TODO Dépendance vers FreeMarker */
public class RenderFreeMarker implements Render {

	private static final Configuration defaultConfiguration = new Configuration(Configuration.VERSION_2_3_30);

	public static final Configuration defaultConfiguration() {
		return defaultConfiguration;
	}

	private Configuration configuration;
	private String name;
	private Map<String, Object> attributes;

	public RenderFreeMarker(String name, Object... paramAndValues) {
		this(defaultConfiguration, name, paramAndValues);
	}

	public RenderFreeMarker(Configuration configuration, String name, Object... paramAndValues) {
		this.configuration = configuration;
		this.name = name;
		this.attributes = new HashMap<>();
		for (int i = 0; i < paramAndValues.length; i += 2) {
			this.attributes.put((String) paramAndValues[i], paramAndValues[i + 1]);
		}
	}

	public RenderFreeMarker with(String name, Object attribute) {
		this.attributes.put(name, attribute);
		return this;
	}

	@Override
	public void render(Request request, Response response, Charset charset, Supplier<OutputStream> stream)
			throws IOException {
		// Génération en mémoire pour pouvoir renvoyer une erreur 500 si besoin
		StringWriter writer = new StringWriter();
		try {
			Template template = this.configuration.getTemplate(this.name);
			template.process(this.attributes, writer);
		} catch (TemplateException | IOException ex) {
			// En cas d'erreur de template, on renvoie une erreur 500
			Render.internalServerError().render(request, response, charset, stream);
			return;
		}
		// Si tout est OK, on génère
		try (OutputStream os = stream.get()) {
			os.write(writer.toString().getBytes(charset));
		}
	}

}

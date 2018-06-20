package fr.techgp.nimbus.facets;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

import org.bson.Document;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;

public class JavaZoomAudioFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "mp3".equals(extension) || "oga".equals(extension) || "ogg".equals(extension);
	}

	@Override
	public void loadMetadata(Document bson, JsonObject node) {
		node.addProperty("duration", bson.getLong("duration"));
		node.addProperty("author", bson.getString("author"));
		node.addProperty("year", bson.getString("year"));
		node.addProperty("album", bson.getString("album"));
		node.addProperty("title", bson.getString("title"));
		node.addProperty("track", bson.getString("track"));
		node.addProperty("genre", bson.getString("genre"));
		node.addProperty("audioChannels", bson.getInteger("audioChannels"));
		node.addProperty("audioCodec", bson.getString("audioCodec"));
		node.addProperty("audioBitRate", bson.getInteger("audioBitRate"));
		node.addProperty("audioSamplingRate", bson.getInteger("audioSamplingRate"));
	}

	@Override
	public void updateMetadata(File file, String extension, Document bson) throws Exception {
		AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(file);
		Map<String, Object> properties = baseFileFormat.properties();
		bson.put("duration", Optional.ofNullable((Long) properties.get("duration")).map((d) -> d.longValue() / 1000).orElse(null)); // en microsecond
		bson.put("author", properties.get("author")); // texte
		bson.put("year", properties.get("date")); // texte
		bson.put("album", properties.get("album")); // texte
		bson.put("title", properties.get("title")); // texte
		if ("mp3".equals(extension)) {
			// http://www.javazoom.net/mp3spi/documents.html
			bson.put("track", properties.get("mp3.id3tag.track")); // texte
			bson.put("genre", properties.get("mp3.id3tag.genre")); // texte
			bson.put("audioChannels", properties.get("mp3.channels")); // 1 (mono), 2 (stereo)
			bson.put("audioCodec", "MP3 " + properties.get("mp3.version.mpeg")); // MP3 1
			bson.put("audioBitRate", (Integer) properties.get("mp3.bitrate.nominal.bps") / 1000); // 64 (Kbps)
			bson.put("audioSamplingRate", properties.get("mp3.frequency.hz")); // 44100 (Hz)
		} else {
			// http://www.javazoom.net/vorbisspi/documents.html
			bson.put("track", properties.get("ogg.comment.track")); // texte
			bson.put("genre", properties.get("ogg.comment.genre")); // texte
			bson.put("audioChannels", properties.get("ogg.channels")); // 1 (mono), 2 (stereo)
			bson.put("audioCodec", "Ogg Vorbis " + properties.get("ogg.version")); // Ogg Vorbis 0
			bson.put("audioBitRate", (Integer) properties.get("ogg.bitrate.nominal.bps") / 1000); // 64 (Kbps)
			bson.put("audioSamplingRate", properties.get("ogg.frequency.hz")); // 44100 (Hz)
		}
	}

}

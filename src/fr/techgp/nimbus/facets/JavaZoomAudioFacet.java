package fr.techgp.nimbus.facets;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Configuration;
import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

public class JavaZoomAudioFacet implements Facet {

	private static String[] mp3genres = null;

	@Override
	public void init(Configuration configuration) {
		try {
			// Les genres dans le format ID3v1 sont représentés par un tableau. Par exemple, l'index 32 signifie "Classical".
			// JavaZoom a codé ce tableau mais le champ en question, MpegAudioFileReader.id3v1genres, est privé.
			// On utilise la réflection pour y accéder et nous éviter de recoder ce tableau.
			Field mp3genreField = MpegAudioFileReader.class.getDeclaredField("id3v1genres");
			mp3genreField.setAccessible(true);
			mp3genres = (String[]) mp3genreField.get(null);
		} catch (Exception ex) {
			ex.printStackTrace();
			mp3genres = null;
		}
	}

	@Override
	public boolean supports(String extension) {
		return "mp3".equals(extension) || "oga".equals(extension) || "ogg".equals(extension);
	}

	@Override
	public void loadMetadata(Metadatas metadatas, JsonObject node) {
		node.addProperty("duration", metadatas.getLong("duration"));
		node.addProperty("artist", metadatas.getString("artist"));
		node.addProperty("year", metadatas.getString("year"));
		node.addProperty("album", metadatas.getString("album"));
		node.addProperty("title", metadatas.getString("title"));
		node.addProperty("track", metadatas.getString("track"));
		node.addProperty("genre", metadatas.getString("genre"));
		node.addProperty("audioChannels", metadatas.getInteger("audioChannels"));
		node.addProperty("audioCodec", metadatas.getString("audioCodec"));
		node.addProperty("audioBitRate", metadatas.getInteger("audioBitRate"));
		node.addProperty("audioSamplingRate", metadatas.getInteger("audioSamplingRate"));
	}

	@Override
	public void updateMetadata(File file, String extension, Metadatas metadatas) throws Exception {
		AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(file);
		Map<String, Object> properties = baseFileFormat.properties();
		metadatas.put("duration", Optional.ofNullable((Long) properties.get("duration")).map((d) -> d.longValue() / 1000).orElse(null)); // microsecond => ms
		metadatas.put("artist", (String) properties.get("author"));
		metadatas.put("year", (String) properties.get("date"));
		metadatas.put("album", (String) properties.get("album"));
		metadatas.put("title", (String) properties.get("title"));
		if ("mp3".equals(extension)) {
			// http://www.javazoom.net/mp3spi/documents.html
			metadatas.put("track", (String) properties.get("mp3.id3tag.track"));
			metadatas.put("genre", mp3Genre((String) properties.get("mp3.id3tag.genre")));
			metadatas.put("audioChannels", (Integer) properties.get("mp3.channels")); // 1 (mono), 2 (stereo)
			metadatas.put("audioCodec", "MPEG Layer " + properties.get("mp3.version.layer")); // MPEG Layer 3
			metadatas.put("audioBitRate", (Integer) properties.get("mp3.bitrate.nominal.bps") / 1000); // 64 (Kbps)
			metadatas.put("audioSamplingRate", (Integer) properties.get("mp3.frequency.hz")); // 44100 (Hz)
		} else {
			// http://www.javazoom.net/vorbisspi/documents.html
			metadatas.put("track", (String) properties.get("ogg.comment.track"));
			metadatas.put("genre", (String) properties.get("ogg.comment.genre"));
			metadatas.put("audioChannels", (Integer) properties.get("ogg.channels")); // 1 (mono), 2 (stereo)
			metadatas.put("audioCodec", "Ogg Vorbis " + properties.get("ogg.version")); // Ogg Vorbis 0
			metadatas.put("audioBitRate", (Integer) properties.get("ogg.bitrate.nominal.bps") / 1000); // 64 (Kbps)
			metadatas.put("audioSamplingRate", (Integer) properties.get("ogg.frequency.hz")); // 44100 (Hz)
		}
	}

	private static final String mp3Genre(String genre) {
		// Bizarrement, JavaZoom renvoie par exemple le genre "(32)".
		// En fait, 32 est l'index dans le tableau "mp3genres" et signifie "Classical".
		// La liste est décrite ici : https://en.wikipedia.org/wiki/ID3#Genre_list_in_ID3v1[12]
		if (mp3genres != null && genre != null && genre.matches("\\(\\d+\\)")) {
			int index = Integer.parseInt(genre.substring(1, genre.length() - 1));
			if (index >= 0 && index < mp3genres.length)
				return mp3genres[index];
		}
		return genre;
	}

}

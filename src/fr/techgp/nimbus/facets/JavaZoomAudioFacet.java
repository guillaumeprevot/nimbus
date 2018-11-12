package fr.techgp.nimbus.facets;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

import org.bson.Document;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Configuration;
import fr.techgp.nimbus.Facet;
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
	public void loadMetadata(Document bson, JsonObject node) {
		node.addProperty("duration", bson.getLong("duration"));
		node.addProperty("artist", bson.getString("artist"));
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
		bson.put("artist", properties.get("author")); // texte
		bson.put("year", properties.get("date")); // texte
		bson.put("album", properties.get("album")); // texte
		bson.put("title", properties.get("title")); // texte
		if ("mp3".equals(extension)) {
			// http://www.javazoom.net/mp3spi/documents.html
			bson.put("track", properties.get("mp3.id3tag.track")); // texte
			bson.put("genre", mp3Genre((String) properties.get("mp3.id3tag.genre"))); // texte
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

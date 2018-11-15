package fr.techgp.nimbus.facets;

import java.io.File;
import java.util.Optional;
import java.util.function.Function;

import org.bson.Document;

import com.google.gson.JsonObject;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

import fr.techgp.nimbus.Facet;

public class MP3agicFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "mp3".equals(extension);
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
		Mp3File mp3file = new Mp3File(file);
		bson.put("duration", mp3file.getLengthInMilliseconds());
		bson.put("audioChannels", mp3file.getChannelMode().contains("tereo") ? 2 : 1); // 1 (mono), 2 (stereo)
		bson.put("audioCodec", "MPEG Layer " + mp3file.getLayer().length()); // MPEG Layer III/II/I => 3/2/1
		bson.put("audioBitRate", mp3file.getBitrate()); // 64 (Kbps)
		bson.put("audioSamplingRate", mp3file.getSampleRate()); // 44100 (Hz)

		Optional<ID3v1> v1 = Optional.ofNullable(mp3file.getId3v1Tag());
		Optional<ID3v2> v2 = Optional.ofNullable(mp3file.getId3v2Tag());
		Function<Function<ID3v1, String>, String> getter = (field) -> v2.map(field).orElseGet(() -> v1.map(field).orElse(null));
		bson.put("artist", getter.apply(ID3v1::getArtist));
		bson.put("year", getter.apply(ID3v1::getYear));
		bson.put("album", getter.apply(ID3v1::getAlbum));
		bson.put("title", getter.apply(ID3v1::getTitle));
		bson.put("track", getter.apply(ID3v1::getTrack));
		bson.put("genre", getter.apply(ID3v1::getGenreDescription));
	}

}

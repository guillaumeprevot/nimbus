package fr.techgp.nimbus.facets;

import java.io.File;
import java.util.Optional;
import java.util.function.Function;

import com.google.gson.JsonObject;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;

public class MP3agicFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return "mp3".equals(extension);
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
		Mp3File mp3file = new Mp3File(file);
		metadatas.put("duration", mp3file.getLengthInMilliseconds());
		metadatas.put("audioChannels", mp3file.getChannelMode().contains("tereo") ? 2 : 1); // 1 (mono), 2 (stereo)
		metadatas.put("audioCodec", "MPEG Layer " + mp3file.getLayer().length()); // MPEG Layer III/II/I => 3/2/1
		metadatas.put("audioBitRate", mp3file.getBitrate()); // 64 (Kbps)
		metadatas.put("audioSamplingRate", mp3file.getSampleRate()); // 44100 (Hz)

		Optional<ID3v1> v1 = Optional.ofNullable(mp3file.getId3v1Tag());
		Optional<ID3v2> v2 = Optional.ofNullable(mp3file.getId3v2Tag());
		Function<Function<ID3v1, String>, String> getter = (field) -> v2.map(field).orElseGet(() -> v1.map(field).orElse(null));
		metadatas.put("artist", getter.apply(ID3v1::getArtist));
		metadatas.put("year", getter.apply(ID3v1::getYear));
		metadatas.put("album", getter.apply(ID3v1::getAlbum));
		metadatas.put("title", getter.apply(ID3v1::getTitle));
		metadatas.put("track", getter.apply(ID3v1::getTrack));
		metadatas.put("genre", getter.apply(ID3v1::getGenreDescription));
	}

}

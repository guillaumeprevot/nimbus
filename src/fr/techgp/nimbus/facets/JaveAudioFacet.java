package fr.techgp.nimbus.facets;

import java.io.File;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.MultimediaInfo;

public class JaveAudioFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return ",aac,ac3,aiff,dts,flac,mp3,oga,ogg,wav,".contains("," + extension + ",");
	}

	@Override
	public void loadMetadata(Metadatas metadatas, JsonObject node) {
		node.addProperty("duration", metadatas.getLong("duration"));
		node.addProperty("audioChannels", metadatas.getInteger("audioChannels"));
		node.addProperty("audioCodec", metadatas.getString("audioCodec"));
		node.addProperty("audioBitRate", metadatas.getInteger("audioBitRate"));
		node.addProperty("audioSamplingRate", metadatas.getInteger("audioSamplingRate"));
	}

	@Override
	public void updateMetadata(File file, String extension, Metadatas metadatas) throws Exception {
		Encoder encoder = new Encoder();
		MultimediaInfo info = encoder.getInfo(file);
		metadatas.put("duration", info.getDuration()); // 12500 (ms)
		if (info.getAudio() != null) {
			metadatas.put("audioChannels", info.getAudio().getChannels()); // 2 (stereo)
			metadatas.put("audioCodec", info.getAudio().getDecoder()); // "vorbis" ou "mp3"
			metadatas.put("audioBitRate", info.getAudio().getBitRate()); // 64 (Kbps)
			metadatas.put("audioSamplingRate", info.getAudio().getSamplingRate()); // 44100 (Hz)
		}
	}

}

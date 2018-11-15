package fr.techgp.nimbus.facets;

import java.io.File;

import org.bson.Document;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.MultimediaInfo;

public class JaveAudioFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return ",aac,ac3,aiff,dts,flac,mp3,oga,ogg,wav,".contains("," + extension + ",");
	}

	@Override
	public void loadMetadata(Document bson, JsonObject node) {
		node.addProperty("duration", bson.getLong("duration"));
		node.addProperty("audioChannels", bson.getInteger("audioChannels"));
		node.addProperty("audioCodec", bson.getString("audioCodec"));
		node.addProperty("audioBitRate", bson.getInteger("audioBitRate"));
		node.addProperty("audioSamplingRate", bson.getInteger("audioSamplingRate"));
	}

	@Override
	public void updateMetadata(File file, String extension, Document bson) throws Exception {
		Encoder encoder = new Encoder();
		MultimediaInfo info = encoder.getInfo(file);
		bson.put("duration", info.getDuration()); // 12500 (ms)
		if (info.getAudio() != null) {
			bson.put("audioChannels", info.getAudio().getChannels()); // 2 (stereo)
			bson.put("audioCodec", info.getAudio().getDecoder()); // "vorbis" ou "mp3"
			bson.put("audioBitRate", info.getAudio().getBitRate()); // 64 (Kbps)
			bson.put("audioSamplingRate", info.getAudio().getSamplingRate()); // 44100 (Hz)
		}
	}

}

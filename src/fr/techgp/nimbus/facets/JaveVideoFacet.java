package fr.techgp.nimbus.facets;

import java.io.File;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.MultimediaInfo;

public class JaveVideoFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return ",asf,wma,wmv,avi,avs,flv,h261,h263,h264,m4v,mkv,mov,m4a,mp4,3gp,mpeg,mpg,ogv,rtsp,webm,".contains("," + extension + ",");
	}

	@Override
	public void loadMetadata(Metadatas metadatas, JsonObject node) {
		node.addProperty("duration", metadatas.getLong("duration"));
		node.addProperty("width", metadatas.getInteger("width"));
		node.addProperty("height", metadatas.getInteger("height"));
		node.addProperty("videoCodec", metadatas.getString("videoCodec"));
		node.addProperty("videoBitRate", metadatas.getInteger("videoBitRate"));
		node.addProperty("videoFrameRate", metadatas.getDouble("videoFrameRate"));
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
		if (info.getVideo() != null) {
			metadatas.put("width", info.getVideo().getSize().getWidth()); // 320 (px)
			metadatas.put("height", info.getVideo().getSize().getHeight()); // 240 (px)
			metadatas.put("videoCodec", info.getVideo().getDecoder()); // "theora"
			metadatas.put("videoBitRate", info.getVideo().getBitRate()); // -1
			metadatas.put("videoFrameRate", (double) info.getVideo().getFrameRate()); // 29 (frame/s)
		}
		if (info.getAudio() != null) {
			metadatas.put("audioChannels", info.getAudio().getChannels()); // 2 (stereo)
			metadatas.put("audioCodec", info.getAudio().getDecoder()); // "vorbis"
			metadatas.put("audioBitRate", info.getAudio().getBitRate()); // 64 (Kbps)
			metadatas.put("audioSamplingRate", info.getAudio().getSamplingRate()); // 44100 (Hz)
		}
	}

}

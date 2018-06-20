package fr.techgp.nimbus.facets;

import java.io.File;

import org.bson.Document;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Facet;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.MultimediaInfo;

public class JaveVideoFacet implements Facet {

	@Override
	public boolean supports(String extension) {
		return ",asf,wma,wmv,avi,avs,flv,h261,h263,h264,m4v,mkv,mov,m4a,mp4,3gp,mpeg,mpg,ogv,rtsp,webm,".contains("," + extension + ",");
	}

	@Override
	public void loadMetadata(Document bson, JsonObject node) {
		node.addProperty("duration", bson.getLong("duration"));
		node.addProperty("width", bson.getInteger("width"));
		node.addProperty("height", bson.getInteger("height"));
		node.addProperty("videoCodec", bson.getString("videoCodec"));
		node.addProperty("videoBitRate", bson.getInteger("videoBitRate"));
		node.addProperty("videoFrameRate", bson.getDouble("videoFrameRate"));
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
		bson.put("width", info.getVideo().getSize().getWidth()); // 320 (px)
		bson.put("height", info.getVideo().getSize().getHeight()); // 240 (px)
		bson.put("videoCodec", info.getVideo().getDecoder()); // "theora"
		bson.put("videoBitRate", info.getVideo().getBitRate()); // -1
		bson.put("videoFrameRate", info.getVideo().getFrameRate()); // 29 (frame/s)
		bson.put("audioChannels", info.getAudio().getChannels()); // 2 (stereo)
		bson.put("audioCodec", info.getAudio().getDecoder()); // "vorbis"
		bson.put("audioBitRate", info.getAudio().getBitRate()); // 64 (Kbps)
		bson.put("audioSamplingRate", info.getAudio().getSamplingRate()); // 44100 (Hz)
	}

}

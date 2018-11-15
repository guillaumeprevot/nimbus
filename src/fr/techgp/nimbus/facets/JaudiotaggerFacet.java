package fr.techgp.nimbus.facets;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.bson.Document;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.SupportedFileFormat;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Configuration;
import fr.techgp.nimbus.Facet;

public class JaudiotaggerFacet implements Facet {

	private static final Set<String> extensions = new HashSet<>();

	@Override
	public void init(Configuration configuration) {
		// https://bitbucket.org/ijabz/jaudiotagger/src/6b4d904df18b76e932085259d676c706dcc0cb2d/src/org/jaudiotagger/audio/AudioFileIO.java?at=master&fileviewer=file-view-default
		for (SupportedFileFormat format : SupportedFileFormat.values()) {
			extensions.add(format.getFilesuffix());
		}
		// SupportedFileFormat indicates OGG is supported but for "ogg" extension.
		// Adding "oga" too.
		extensions.add("oga");
	}

	@Override
	public boolean supports(String extension) {
		return extensions.contains(extension);
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
		// SupportedFileFormat indicates OGG is supported but for "ogg" extension.
		// Load "oga" as "ogg".
		String supportedExtension = "oga".equals(extension) ? "ogg" : extension;
		AudioFile f = AudioFileIO.readAs(file, supportedExtension);

		AudioHeader h = f.getAudioHeader();
		bson.put("duration", (long) (h.getTrackLength() * 1000)); // second => ms
		bson.put("audioChannels", "Stereo".equals(h.getChannels()) ? 2 : 1); // 1 (mono), 2 (stereo)
		bson.put("audioCodec", h.getFormat()); // MPEG-1 Layer 3
		bson.put("audioBitRate", (int) h.getBitRateAsNumber()); // 64 (Kbps)
		bson.put("audioSamplingRate", h.getSampleRateAsNumber()); // 44100 (Hz)

		Tag t = f.getTag();
		if (t != null) {
			bson.put("artist", t.getFirst(FieldKey.ARTIST));
			bson.put("year", t.getFirst(FieldKey.YEAR));
			bson.put("album", t.getFirst(FieldKey.ALBUM));
			bson.put("title", t.getFirst(FieldKey.TITLE));
			bson.put("track", t.getFirst(FieldKey.TRACK));
			bson.put("genre", t.getFirst(FieldKey.GENRE));
		}
	}

}

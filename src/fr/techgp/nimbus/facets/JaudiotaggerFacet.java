package fr.techgp.nimbus.facets;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.SupportedFileFormat;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import com.google.gson.JsonObject;

import fr.techgp.nimbus.Configuration;
import fr.techgp.nimbus.Facet;
import fr.techgp.nimbus.models.Metadatas;

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
		// SupportedFileFormat indicates OGG is supported but for "ogg" extension.
		// Load "oga" as "ogg".
		String supportedExtension = "oga".equals(extension) ? "ogg" : extension;
		AudioFile f = AudioFileIO.readAs(file, supportedExtension);

		AudioHeader h = f.getAudioHeader();
		metadatas.put("duration", (long) (h.getTrackLength() * 1000)); // second => ms
		metadatas.put("audioChannels", "Stereo".equals(h.getChannels()) ? 2 : 1); // 1 (mono), 2 (stereo)
		metadatas.put("audioCodec", h.getFormat()); // MPEG-1 Layer 3
		metadatas.put("audioBitRate", (int) h.getBitRateAsNumber()); // 64 (Kbps)
		metadatas.put("audioSamplingRate", h.getSampleRateAsNumber()); // 44100 (Hz)

		Tag t = f.getTag();
		if (t != null) {
			metadatas.put("artist", t.getFirst(FieldKey.ARTIST));
			metadatas.put("year", t.getFirst(FieldKey.YEAR));
			metadatas.put("album", t.getFirst(FieldKey.ALBUM));
			metadatas.put("title", t.getFirst(FieldKey.TITLE));
			metadatas.put("track", t.getFirst(FieldKey.TRACK));
			metadatas.put("genre", t.getFirst(FieldKey.GENRE));
		}
	}

}

package fr.techgp.nimbus.facets;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.SupportedFileFormat;
import org.jaudiotagger.audio.exceptions.CannotReadVideoException;
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
		// https://bitbucket.org/ijabz/jaudiotagger/src/master/src/org/jaudiotagger/audio/AudioFileIO.java
		for (SupportedFileFormat format : SupportedFileFormat.values()) {
			extensions.add(format.getFilesuffix());
		}
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
		// Some files will generate a trace from JAudioTagger (ni Mp4BoxHeader.seekWithinLevel) using "java.util.logging"
		// and bypassing the SLF4J configuration from Nimbus. The next line will remove this unwanted trace.
		Logger.getLogger("org.jaudiotagger.audio.mp4.atom").setLevel(Level.OFF);

		AudioFile f;
		try {
			f = AudioFileIO.readAs(file, extension);
		} catch (CannotReadVideoException ex) {
			// Some extension, like "mp4" can be either audio or video but JAudioTagger only supports audio files.
			// If the file contains a video, JAudioTagger will throw a CannotReadVideoException, and we will simply ignore the file.
			return;
		}

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

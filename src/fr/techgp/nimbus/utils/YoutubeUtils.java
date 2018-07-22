package fr.techgp.nimbus.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * L'idée originale vient de :
 *   https://github.com/cdown/yturl
 * La liste des itags a été ajustée avec les infos trouvées ici :
 *   https://en.wikipedia.org/wiki/YouTube#Quality_and_formats
 * Une vidéo de test :
 *   https://www.youtube.com/watch?v=8TCxE0bWQeQ&gl=FR&hl=fr
 * Les méta-données
 * - id = dans l'URL ou video_id = 8TCxE0bWQeQ
 * - titre = title = Jumping over a chair like a gangster (Bill Gates)
 * - miniature = thumbnail_url = https://i.ytimg.com/vi/8TCxE0bWQeQ/default.jpg
 * - image = iurl = https://i.ytimg.com/vi/8TCxE0bWQeQ/hqdefault.jpg
 * - image medium = iurlmq = https://i.ytimg.com/vi/8TCxE0bWQeQ/mqdefault.jpg
 * - image high = iurlhq = https://i.ytimg.com/vi/8TCxE0bWQeQ/hqdefault.jpg
 * - durée = length_seconds = 19
 * - auteur = author = Josh W
 * - vues = view_count = 1991026
 * - note = avg_rating = 4.97690843211
 * - formats = url_encoded_fmt_stream_map = (itag=int&url=url&type=mimetype+codecs)+
 */
public final class YoutubeUtils {

	private YoutubeUtils() {
		//
	}

	private static final String YOUTUBE_VIDEO_PATTERN = "youtube.com/watch?";

	public static final boolean isYoutubeVideo(String url) {
		return url.contains(YOUTUBE_VIDEO_PATTERN);
	}

	public static final String getYoutubeVideoId(String url) {
		// Check url
		int index = url.indexOf(YOUTUBE_VIDEO_PATTERN);
		if (index == -1)
			return null;
		// Extract video id from URL
		String videoId = Arrays.stream(url.substring(index + YOUTUBE_VIDEO_PATTERN.length()).split("&"))
				// Search "v" param
				.filter((param) -> param.startsWith("v="))
				// Extract "v" param value
				.map((param) -> param.substring(2)).findAny().orElse("");
		// Check video id is 11 characters long
		if (videoId.length() != 11)
			return null;
		// OK, got one
		return videoId;
	}

	public static final void iterateYoutubeVideoMetadata(String videoId, BiConsumer<String, String> consumer) {
		try {
			String metadatas = WebUtils.downloadURL("https://youtube.com/get_video_info?hl=en&video_id=" + videoId);
			if (metadatas == null)
				return;
			StringTokenizer st = new StringTokenizer(metadatas, "&");
			while (st.hasMoreTokens()) {
				String pair = st.nextToken();
				int index = pair.indexOf("=");
				String key = pair.substring(0,  index); //
				String value = URLDecoder.decode(pair.substring(index + 1), "UTF-8");
				consumer.accept(key, value);
			}
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
		}
	}

	public static final void iterateYoutubeVideoOptions(String formats, BiConsumer<YoutubeITag, String> consumer) {
		Map<Integer, YoutubeITag> itags = loadITags().stream().collect(Collectors.toMap((i) -> i.id, Function.identity()));
		try {
			for (String format : formats.split(",")) {
				String[] values = format.split("[&=]");
				int videoITag = 0;
				String videoUrl = null;
				for (int i = 0; i < values.length; i++) {
					if ("itag".equals(values[i]))
						videoITag = Integer.parseInt(values[i + 1]);
					else if ("url".equals(values[i]))
						videoUrl = URLDecoder.decode(values[i + 1], "UTF-8");
				}
				consumer.accept(itags.get(videoITag), videoUrl);
			}
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
		}
	}

	private static final List<YoutubeITag> loadITags() {
		// Les itags 6, 13, 34, 35, 37, 38, 44, 45 et 46 ne sont plus utilisés
		List<YoutubeITag> itags = new ArrayList<>();
		//itags.add(new YoutubeITag(5, 400, 240, 0.25, 64, 22.05, "h263", "mp3", "flv", "video/x-flv"));
		itags.add(new YoutubeITag(17, 176, 144, 0.05, 24, 22.05, "mp4v", "aac", "3gp", "video/3gpp"));
		itags.add(new YoutubeITag(18, 640, 360, 0.5, 96, 44.1, "h264", "aac", "mp4", "video/mp4"));
		itags.add(new YoutubeITag(22, 1280, 720, 2.9, 192, 44.1, "h264", "aac", "mp4", "video/mp4"));
		itags.add(new YoutubeITag(36, 320, 240, 0.17, 32, 44.1, "mp4v", "aac", "3gp", "video/3gpp"));
		itags.add(new YoutubeITag(43, 640, 360, 0.5, 128, 44.1, "vp8", "vorbis", "webm", "video/webm"));
		return itags;
	}

	public static final class YoutubeITag {

		public final int id;
		public final int width;
		public final int height;
		public final double videoBitrate;
		public final double audioBitrate;
		public final double audioSamplerate;
		public final String videoEncoding;
		public final String audioEncoding;
		public final String extension;
		public final String mimetype;

		public YoutubeITag(int id, int width, int height, double videoBitrate, double audioBitrate,
				double audioSamplerate, String videoEncoding, String audioEncoding, String extension, String mimetype) {
			this.id = id;
			this.width = width;
			this.height = height;
			this.videoBitrate = videoBitrate;
			this.audioBitrate = audioBitrate;
			this.audioSamplerate = audioSamplerate;
			this.videoEncoding = videoEncoding;
			this.audioEncoding = audioEncoding;
			this.extension = extension;
			this.mimetype = mimetype;
		}

	}

}

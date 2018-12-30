(function() {
	// Fonction utilitaire trouvée ici (licence MIT) : http://www.webvtt.org/
	function srt2webvtt(data) {
		// remove dos newlines
		var srt = data.replace(/\r+/g, '');
		// trim white space start and end
		srt = srt.replace(/^\s+|\s+$/g, '');
		// get cues
		var cuelist = srt.split('\n\n');
		var result = "WEBVTT\n\n";
		for (var i = 0; i < cuelist.length; i++) {
			result += convertSrtCue(cuelist[i]);
		}
		return result;
	}

	function convertSrtCue(caption) {
		// remove all html tags for security reasons
		// srt = srt.replace(/<[a-zA-Z\/][^>]*>/g, '');
		var cue = "";
		var s = caption.split(/\n/);
		// concatenate muilt-line string separated in array into one
		while (s.length > 3) {
			for (var i = 3; i < s.length; i++) {
				s[2] += "\n" + s[i]
			}
			s.splice(3, s.length - 3);
		}
		var line = 0;
		// detect identifier
		if (!s[0].match(/\d+:\d+:\d+/) && s[1].match(/\d+:\d+:\d+/)) {
			cue += s[0].match(/\w+/) + "\n";
			line += 1;
		}
		// get time strings
		if (s[line].match(/\d+:\d+:\d+/)) {
			// convert time string
			var m = s[1].match(/(\d+):(\d+):(\d+)(?:,(\d+))?\s*--?>\s*(\d+):(\d+):(\d+)(?:,(\d+))?/);
			if (m) {
				cue += m[1]+":"+m[2]+":"+m[3]+"."+m[4]+" --> "+m[5]+":"+m[6]+":"+m[7]+"."+m[8]+"\n";
				line += 1;
			} else {
				// Unrecognized timestring
				return "";
			}
		} else {
			// file format error or comment lines
			return "";
		}
		// get cue text
		if (s[line]) {
			cue += s[line] + "\n\n";
		}
		return cue;
	}

	NIMBUS.plugins.add({
		name: 'video',
		properties: [
			{ name: 'duration', caption: 'VideoPropertyDuration', align: 'right', sortBy: 'content.duration', format: (i) => NIMBUS.formatDuration(i.duration / 1000) },
			{ name: 'width', caption: 'VideoPropertyWidth', align: 'right', sortBy: 'content.width', format: (i) => NIMBUS.formatInteger(i.width, "px") },
			{ name: 'height', caption: 'VideoPropertyHeight', align: 'right', sortBy: 'content.height', format: (i) => NIMBUS.formatInteger(i.height, "px") },
		],
		facets: [{
			name: 'video',
			accept: NIMBUS.utils.isBrowserSupportedVideo,
			image: function(item, thumbnail) {
				return '<i class="material-icons">local_movies</i>';
			},
			describe: function describe(item) {
				var p = [];
				if (item.duration)
					p.push(NIMBUS.formatDuration(item.duration / 1000));
				if (item.width && item.height)
					p.push(item.width.toFixed(0) + "x" + item.height.toFixed(0) + "px");
				if (item.videoCodec && item.audioCodec)
					p.push(item.videoCodec + "+" + item.audioCodec);
				return p.join(', ');
			}
		}],
		actions: [{
			name: 'video-play',
			icon: 'play_circle_outline',
			caption: 'VideoPlay',
			accept: NIMBUS.utils.isBrowserSupportedVideo,
			execute: function(item) {
				window.open('/video.html?' + $.param({
					itemId: item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}
		}, {
			name: 'video-convert-to-webvtt',
			icon: 'transform',
			caption: 'VideoConvertToWebVTT',
			accept: function(item, extension) {
				return 'srt' === extension;
			},
			execute: function(item) {
				$.get('/files/stream/' + item.id).then(function(content) {
					var webVTT = srt2webvtt(content);
					var blob = new Blob([webVTT], { type: 'text/vtt' });
					var filename = item.name.replace(/\.srt$/, '.vtt');
					// console.log(webVTT, filename);
					NIMBUS.utils.uploadFile(item.parentId, blob, filename).then(function() {
						NIMBUS.navigation.refreshItems(false);
					});
				});
			}
		}],
		langs: {
			fr: {
				VideoPlay: "Lancer la vidéo",
				VideoConvertToWebVTT: "Convertir en WebVTT",
				VideoPropertyDuration: "Durée",
				VideoPropertyWidth: "Largeur",
				VideoPropertyHeight: "Hauteur",
				VideoTitle: "Lecteur vidéo",
				VideoPlayError: "Une erreur s'est produite.",
				VideoPause: "Mettre en pause",
				VideoResume: "Reprendre la lecture",
				VideoPlayMenu: "Options de lecture",
				VideoReplay5: "Rejouer 5 seconds",
				VideoReplay10: "Rejouer 10 seconds",
				VideoReplay30: "Rejouer 30 seconds",
				VideoForward5: "Passer 5 seconds",
				VideoForward10: "Passer 10 seconds",
				VideoForward30: "Passer 30 seconds",
				VideoSpeedSlower: "Ralentir",
				VideoSpeedInitial: "Vitesse normale",
				VideoSpeedFaster: "Accélérer",
				VideoVolumeOff: "Couper le son",
				VideoVolumeOn: "Rétablir le son",
				VideoVolumeMenu: "Réglage du volume",
				VideoSubtitles: "Sous-titres",
				VideoSubtitlesDisabled: "désactivés",
				VideoSubtitlesDefault: "par défaut",
				VideoAspectRatio: "Changer l'aspect",
				VideoFullscreen: "Passer en plein-écran",
				VideoExitFullscreen: "Quitter le plein-écran",
			},
			en: {
				VideoPlay: "Play video",
				VideoConvertToWebVTT: "Convert to WebVTT",
				VideoPropertyDuration: "Duration",
				VideoPropertyWidth: "Width",
				VideoPropertyHeight: "Height",
				VideoTitle: "Video player",
				VideoPlayError: "An error occurred.",
				VideoPause: "Pause video",
				VideoResume: "Resume video",
				VideoPlayMenu: "Play options",
				VideoReplay5: "Replay 5 seconds",
				VideoReplay10: "Replay 10 seconds",
				VideoReplay30: "Replay 30 seconds",
				VideoForward5: "Skip 5 seconds",
				VideoForward10: "Skip 10 seconds",
				VideoForward30: "Skip 30 seconds",
				VideoSpeedSlower: "Slower",
				VideoSpeedInitial: "Initial speed",
				VideoSpeedFaster: "Faster",
				VideoVolumeOff: "Mute",
				VideoVolumeOn: "Unmute",
				VideoVolumeMenu: "Volume level",
				VideoSubtitles: "Subtitles",
				VideoSubtitlesDisabled: "disabled",
				VideoSubtitlesDefault: "defaults",
				VideoAspectRatio: "Change aspect ratio",
				VideoFullscreen: "Fullscreen",
				VideoExitFullscreen: "Exit fullscreen mode",
			} 
		}
	});

})();
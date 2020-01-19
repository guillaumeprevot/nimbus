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

	NIMBUS.utils.srt2webvtt = srt2webvtt;

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
			icon: 'local_movies',
			thumbnail: null,
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
				VideoPause: "Mettre en pause (Espace)",
				VideoResume: "Reprendre la lecture (Espace)",
				VideoPlayMenu: "Options de lecture",
				VideoReplay5: "Rejouer 5 seconds (PagePréc.)",
				VideoReplay10: "Rejouer 10 seconds (Maj+PagePréc.)",
				VideoReplay30: "Rejouer 30 seconds",
				VideoForward5: "Passer 5 seconds (PageSuiv.)",
				VideoForward10: "Passer 10 seconds (Maj+PageSuiv.)",
				VideoForward30: "Passer 30 seconds",
				VideoSpeedSlower: "Ralentir (←)",
				VideoSpeedInitial: "Vitesse normale (Entrée)",
				VideoSpeedFaster: "Accélérer (→)",
				VideoVolumeOff: "Couper le son",
				VideoVolumeOn: "Rétablir le son",
				VideoVolumeMenu: "Réglage du volume (+ / -)",
				VideoVolume0Title: "Volume au minimum (Maj+-)",
				VideoVolume100Title: "Volume au maximum (Maj++)",
				VideoSubtitles: "Sous-titres",
				VideoSubtitlesDisabled: "désactivés",
				VideoSubtitlesDefault: "par défaut",
				VideoAspectRatio: "Changer l'aspect",
				VideoFullscreen: "Passer en plein-écran (F11)",
				VideoExitFullscreen: "Quitter le plein-écran (F11)",
				VideoOpenFileLabel: "Ouvrir une vidéo locale",
				VideoOpenFilePlaceholder: "Cliquer ici pour choisir un fichier vidéo",
				VideoOpenNimbusLabel: "Ouvrir une vidéo distante",
				VideoOpenNimbusPlaceholder: "Saisir une partie du nom du fichier vidéo à ouvrir",
				VideoOpenInputError: "Veuillez sélectionner l'une des options proposées.",
				VideoOpenSubtitlesLabel: "Sous-titres (optionnels)",
				VideoOpenSubtitlesPlaceholder: "Cliquer ici pour choisir un fichier de sous-titres",
				VideoOpenSubtitlesError: "Les sous-titres n'ont pu être chargés",
				VideoOpenBrowseButton: "Choisir",
				VideoOpenButton: "Lancer la vidéo"
			},
			en: {
				VideoPlay: "Play video",
				VideoConvertToWebVTT: "Convert to WebVTT",
				VideoPropertyDuration: "Duration",
				VideoPropertyWidth: "Width",
				VideoPropertyHeight: "Height",
				VideoTitle: "Video player",
				VideoPlayError: "An error occurred.",
				VideoPause: "Pause video (Space)",
				VideoResume: "Resume video (Space)",
				VideoPlayMenu: "Play options",
				VideoReplay5: "Replay 5 seconds (PageUp)",
				VideoReplay10: "Replay 10 seconds (Shift+PageUp)",
				VideoReplay30: "Replay 30 seconds",
				VideoForward5: "Skip 5 seconds (PageDown)",
				VideoForward10: "Skip 10 seconds (Shift+PageDown)",
				VideoForward30: "Skip 30 seconds",
				VideoSpeedSlower: "Slower (←)",
				VideoSpeedInitial: "Initial speed (Enter)",
				VideoSpeedFaster: "Faster (→)",
				VideoVolumeOff: "Mute",
				VideoVolumeOn: "Unmute",
				VideoVolumeMenu: "Volume level (+ / -)",
				VideoVolume0Title: "Set to minimum (Shift+-)",
				VideoVolume100Title: "Set to maximum (Shift++)",
				VideoSubtitles: "Subtitles",
				VideoSubtitlesDisabled: "disabled",
				VideoSubtitlesDefault: "defaults",
				VideoAspectRatio: "Change aspect ratio",
				VideoFullscreen: "Fullscreen (F11)",
				VideoExitFullscreen: "Exit fullscreen mode (F11)",
				VideoOpenFileLabel: "Play local video file",
				VideoOpenFilePlaceholder: "Select the video file to watch",
				VideoOpenNimbusLabel: "Play remote video file",
				VideoOpenNimbusPlaceholder: "Write the name of the video file to open",
				VideoOpenInputError: "Please select one of available options.",
				VideoOpenSubtitlesLabel: "Subtitles file",
				VideoOpenSubtitlesPlaceholder: "Select optional subtitles file",
				VideoOpenSubtitlesError: "Subtitles could not be loaded",
				VideoOpenBrowseButton: "Select",
				VideoOpenButton: "Watch video"
			}
		}
	});

})();
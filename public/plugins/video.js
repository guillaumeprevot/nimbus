(function() {
	var element = document.createElement("video");

	function accept(item, extension) {
		return item.mimetype.indexOf("video/") === 0 && element.canPlayType(item.mimetype); 
	}
/*
	function formatDuration(duration, roundAt, withPrecision) {
		if (!duration)
			return '';
		var parts = ["j", "h", "m", "s", "ms"];
		var modulo = [365, 24, 60, 60, 1000];
		var values = [0, 0, 0, 0, 0];
		var pos = 4, remain = duration;
		while (pos > 0 && remain > 0) {
			values[pos] = remain % modulo[pos];
			remain = Math.floor(remain / modulo[pos]);
			pos--;
		}
		return values.reduce(function(s, v, i) {
			if ((v === 0 && s === "") || i > roundAt)
				return s;
			var text;
			if (i === roundAt)
				text = (values[i] + (values[i + 1] / modulo[i + 1])).toFixed(withPrecision || 0);
			else
				text = values[i];
			if (s)
				return s + " " + text + parts[i];
			return text + parts[i];
		}, "");
	};
*/
	NIMBUS.plugins.add({
		name: 'video',
		properties: [
			{ name: 'duration', caption: 'VideoPropertyDuration', align: 'right', sortBy: 'content.duration', format: (i) => NIMBUS.formatDuration(i.duration / 1000) },
			{ name: 'width', caption: 'VideoPropertyWidth', align: 'right', sortBy: 'content.width', format: (i) => NIMBUS.formatInteger(i.width, "px") },
			{ name: 'height', caption: 'VideoPropertyHeight', align: 'right', sortBy: 'content.height', format: (i) => NIMBUS.formatInteger(i.height, "px") },
		],
		facets: [{
			name: 'video',
			accept: accept,
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
				p.push(NIMBUS.formatLength(item.length));
				p.push(item.mimetype);
				return p.join(', ');
			}
		}],
		actions: [{
			name: 'video-play',
			icon: 'play_circle_outline',
			caption: 'VideoPlay',
			accept: accept,
			execute: function(item) {
				window.open('/video.html?' + $.param({
					url: '/files/stream/' + item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}			
		}],
		langs: {
			fr: {
				VideoPlay: "Lancer la vidéo",
				VideoPropertyDuration: "Durée",
				VideoPropertyWidth: "Largeur",
				VideoPropertyHeight: "Hauteur",
				VideoTitle: "Lecteur vidéo",
				VideoPlayError: "Une erreur s'est produite.",
				VideoPlayMenu: "Lecture",
				VideoPause: "Pause",
				VideoResume: "Lecture",
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
				VideoVolumeMenu: "Volume",
				VideoFullscreen: "Passer en plein-écran",
				VideoExitFullscreen: "Quitter le plein-écran",
			},
			en: {
				VideoPlay: "Play video",
				VideoPropertyDuration: "Duration",
				VideoPropertyWidth: "Width",
				VideoPropertyHeight: "Height",
				VideoTitle: "Video player",
				VideoPlayError: "An error occurred.",
				VideoPlayMenu: "Play",
				VideoPause: "Pause",
				VideoResume: "Resume",
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
				VideoVolumeMenu: "Volume",
				VideoFullscreen: "Fullscreen",
				VideoExitFullscreen: "Exit fullscreen mode",
			} 
		}
	});

})();
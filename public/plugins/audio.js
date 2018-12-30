(function() {

	function execute(play, item) {
		localStorage.setItem('updatePlaylist', JSON.stringify({
			play: play,
			items:  item ? [item] : $('#items tr.audio').get().map((tr) => $(tr).data('item'))
		}));
		setTimeout(function() {
			var stillHere = !!localStorage.getItem('updatePlaylist');
			if (stillHere)
				window.open('/audio.html?' + $.param({
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
		}, 1000);
	}

	NIMBUS.plugins.add({
		name: 'audio',
		properties: [
			{ name: 'duration', caption: 'AudioPropertyDuration', align: 'right', sortBy: 'content.duration', format: (i) => NIMBUS.formatDuration(i.duration / 1000) },
			{ name: 'audioBitRate', caption: 'AudioPropertyBitRate', align: 'right', sortBy: 'content.audioBitRate', format: (i) => NIMBUS.formatInteger(i.audioBitRate, "kbit/s") },
			{ name: 'artist', caption: 'AudioPropertyArtist', sortBy: 'content.artist' },
			{ name: 'year', caption: 'AudioPropertyYear', sortBy: 'content.year' },
			{ name: 'album', caption: 'AudioPropertyAlbum', sortBy: 'content.album' },
			{ name: 'title', caption: 'AudioPropertyTitle', sortBy: 'content.title' },
			{ name: 'track', caption: 'AudioPropertyTrack', sortBy: 'content.track' },
			{ name: 'genre', caption: 'AudioPropertyGenre', sortBy: 'content.genre' },
		],
		facets: [{
			name: 'audio',
			accept: NIMBUS.utils.isBrowserSupportedAudio,
			image: function(item, thumbnail) {
				return '<i class="material-icons">audiotrack</i>';
			},
			describe: function describe(item) {
				var p = [];
				if (item.duration)
					p.push(NIMBUS.formatDuration(item.duration / 1000));
				if (item.artist)
					p.push(item.artist);
				if (item.year)
					p.push(item.year);
				if (item.album)
					p.push(item.album);
				return p.join(', ');
			}
		}],
		actions: [{
			name: 'audio-play',
			icon: 'music_note',
			caption: 'AudioActionPlay',
			accept: NIMBUS.utils.isBrowserSupportedAudio,
			execute: function(item) { execute(true, item); }
		}, {
			name: 'audio-play-folder',
			icon: 'music_note',
			caption: 'AudioActionPlayFolder',
			accept: NIMBUS.utils.isBrowserSupportedAudio,
			execute: function(item) { execute(true, null); }
		}, {
			name: 'audio-add',
			icon: 'queue_music',
			caption: 'AudioActionAdd',
			accept: NIMBUS.utils.isBrowserSupportedAudio,
			execute: function(item) { execute(false, item); }
		}, {
			name: 'audio-add-folder',
			icon: 'queue_music',
			caption: 'AudioActionAddFolder',
			accept: NIMBUS.utils.isBrowserSupportedAudio,
			execute: function(item) { execute(false, null); }
		}],
		langs: {
			fr: {
				AudioActionPlay: "Ecouter", 
				AudioActionPlayFolder: "Ecouter tout le dossier",
				AudioActionAdd: "Ajouter",
				AudioActionAddFolder: "Ajouter tout le dossier",
				AudioPropertyDuration: "Durée",
				AudioPropertyBitRate: "Débit",
				AudioPropertyArtist: "Artiste",
				AudioPropertyYear: "Année",
				AudioPropertyAlbum: "Album",
				AudioPropertyTitle: "Titre",
				AudioPropertyTrack: "Piste",
				AudioPropertyGenre: "Genre",
				AudioTitle: "Lecteur audio",
				AudioPlayFirst: "Ecouter la première piste",
				AudioPlayPrevious: "Ecouter la piste précédente",
				AudioPause: "Mettre en pause",
				AudioResume: "Reprendre la lecture",
				AudioPlayNext: "Ecouter la piste suivante",
				AudioPlayLast: "Ecouter la dernière piste",
				AudioMenu: "Menu",
				AudioLoop: "En boucle",
				AudioShuffle: "Aléatoire",
				AudioMute: "Muet",
				AudioClearPlaylist: "Vider la liste de lecture",
				AudioPlayError: "Une erreur s'est produite.",
			},
			en: {
				AudioActionPlay: "Play",
				AudioActionPlayFolder: "Play all",
				AudioActionAdd: "Add",
				AudioActionAddFolder: "Add all",
				AudioPropertyDuration: "Duration",
				AudioPropertyBitRate: "Bitrate",
				AudioPropertyArtist: "Artist",
				AudioPropertyYear: "Year",
				AudioPropertyAlbum: "Album",
				AudioPropertyTitle: "Title",
				AudioPropertyTrack: "Track",
				AudioPropertyGenre: "Genre",
				AudioTitle: "Audio player",
				AudioPlayFirst: "Play first track",
				AudioPlayPrevious: "Play previous track",
				AudioPause: "Pause",
				AudioResume: "Resume",
				AudioPlayNext: "Play nextTrack",
				AudioPlayLast: "Play last track",
				AudioMenu: "Menu",
				AudioLoop: "Loop",
				AudioShuffle: "Shuffle",
				AudioMute: "Mute",
				AudioClearPlaylist: "Clear playlist",
				AudioPlayError: "An error occurred.",
			} 
		}
	});

})();

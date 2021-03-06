(function() {
	var lastAudioFolderCheckItems = [];

	function isAudioFolder(item, extension) {
		if (!item.folder)
			return false;
		return $.get('/items/list?recursive=false&folders=false&deleted=false&parentId=' + item.id).then(function(items) {
			lastAudioFolderCheckItems = items.filter(function(item) {
				var extension = NIMBUS.utils.getFileExtensionFromItem(item);
				return NIMBUS.utils.isBrowserSupportedAudio(item, extension);
			});
			return lastAudioFolderCheckItems.length > 0;
		});
	}

	function execute(play, items) {
		localStorage.setItem('updatePlaylist', JSON.stringify({
			play: play,
			items: items
		}));
		setTimeout(function() {
			var stillHere = !!localStorage.getItem('updatePlaylist');
			if (stillHere)
				window.open('/audio.html');
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
			icon: 'audiotrack',
			thumbnail: null,
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
			execute: function(item) { execute(true, [item]); }
		}, {
			name: 'audio-play-folder',
			icon: 'music_note',
			caption: 'AudioActionPlayFolder',
			accept: NIMBUS.utils.isBrowserSupportedAudio,
			execute: function(item) { execute(true, $('#items tr.audio').get().map((tr) => $(tr).data('item'))); }
		}, {
			name: 'audio-play-on-folder',
			icon: 'music_note',
			caption: 'AudioActionPlayFolder',
			accept: isAudioFolder,
			execute: function(item) { execute(true, lastAudioFolderCheckItems); }
		}, {
			name: 'audio-add',
			icon: 'queue_music',
			caption: 'AudioActionAdd',
			accept: NIMBUS.utils.isBrowserSupportedAudio,
			execute: function(item) { execute(false, [item]); }
		}, {
			name: 'audio-add-folder',
			icon: 'queue_music',
			caption: 'AudioActionAddFolder',
			accept: NIMBUS.utils.isBrowserSupportedAudio,
			execute: function(item) { execute(false, $('#items tr.audio').get().map((tr) => $(tr).data('item'))); }
		}, {
			name: 'audio-add-on-folder',
			icon: 'queue_music',
			caption: 'AudioActionAddFolder',
			accept: isAudioFolder,
			execute: function(item) { execute(false, lastAudioFolderCheckItems); }
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
				AudioPlayFirst: "Ecouter la première piste (Début)",
				AudioPlayPrevious: "Ecouter la piste précédente (PagePréc.)",
				AudioPause: "Mettre en pause (Espace)",
				AudioResume: "Reprendre la lecture (Espace)",
				AudioPlayNext: "Ecouter la piste suivante (PageSuiv.)",
				AudioPlayLast: "Ecouter la dernière piste (Fin)",
				AudioMenu: "Menu",
				AudioAddPlaylist: "Ajouter ...",
				AudioLoop: "En boucle",
				AudioShuffle: "Aléatoire",
				AudioMute: "Muet",
				AudioClearPlaylist: "Vider la liste de lecture",
				AudioPlayError: "Une erreur s'est produite.",
				AudioAddModalFileTab: "Fichier",
				AudioAddModalTitleTab: "Titre",
				AudioAddModalAlbumTab: "Album",
				AudioAddModalArtistTab: "Artiste",
				AudioAddModalSearchPlaceholder: "Rechercher ...",
				AudioAddModalNoResultMessage: "Aucun résultat",
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
				AudioPlayFirst: "Play first track (Home)",
				AudioPlayPrevious: "Play previous track (PageUp)",
				AudioPause: "Pause (Space)",
				AudioResume: "Resume (Space)",
				AudioPlayNext: "Play next track (PageDown)",
				AudioPlayLast: "Play last track (End)",
				AudioMenu: "Menu",
				AudioAddPlaylist: "Add tracks...",
				AudioLoop: "Loop",
				AudioShuffle: "Shuffle",
				AudioMute: "Mute",
				AudioClearPlaylist: "Clear playlist",
				AudioPlayError: "An error occurred.",
				AudioAddModalFileTab: "File",
				AudioAddModalTitleTab: "Title",
				AudioAddModalAlbumTab: "Album",
				AudioAddModalArtistTab: "Artist",
				AudioAddModalSearchPlaceholder: "Search...",
				AudioAddModalNoResultMessage: "No result found.",
			}
		}
	});

})();

(function() {

	function accept(item, extension) {
		return 'epub' === extension;
	}

	NIMBUS.plugins.add({
		name: 'epub',
		properties: [],
		facets: [{
			name: 'epub',
			accept: accept,
			icon: 'chrome_reader_mode',
			thumbnail: null,
			describe: function describe(item) {
				return '';
			}
		}],
		actions: [{
			name: 'epub-read',
			icon: 'chrome_reader_mode',
			caption: 'EPUBRead',
			accept: accept,
			execute: function(item) {
				window.open('/epub.html?' + $.param({
					url: '/files/stream/' + item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}
		}],
		langs: {
			fr: {
				EPUBRead: "Lire",
				EPUBTitle: "Lecteur ePub",
				EPUBOptions: "Options",
				EPUBSpreadsOff: "Afficher en une colonne",
				EPUBSpreadsOn: "Afficher sur deux colonnes",
				EPUBSelectMode: "Mode de sélection de texte",
				EPUBSwipeMode: "Mode de navigation au doigt",
				EPUBPreviousPage: "Afficher la page précédente",
				EPUBNextPage: "Afficher la page suivante",
				EPUBPosition: "Position",
				EPUBChapters: "Chapitres",
			},
			en: {
				EPUBRead: "Read",
				EPUBTitle: "ePub reader",
				EPUBOptions: "Options",
				EPUBSpreadsOff: "Display as a single column",
				EPUBSpreadsOn: "Display on two columns",
				EPUBSelectMode: "Text selection mode",
				EPUBSwipeMode: "Swipe navigation mode",
				EPUBPreviousPage: "Show previous page",
				EPUBNextPage: "Show next page",
				EPUBPosition: "Position",
				EPUBChapters: "Chapters",
			}
		}
	});

})();
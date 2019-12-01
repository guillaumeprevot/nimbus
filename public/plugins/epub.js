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
				EPUBOpenFileLabel: "Ouvrir un fichier",
				EPUBOpenFilePlaceholder: "Cliquer ici pour choisir un fichier à ouvrir",
				EPUBOpenFileBrowse: "Choisir",
				EPUBOpenUrlLabel: "Ouvrir une URL",
				EPUBOpenUrlPlaceholder: "Saisir ici l'URL du fichier epub à ouvrir",
				EPUBOpenInputError: "Veuillez sélectionner un fichier ou une URL.",
				EPUBOpenButton: "Commencer ou reprendre la lecture"
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
				EPUBOpenFileLabel: "Read local file",
				EPUBOpenFilePlaceholder: "Select the local epub file to open",
				EPUBOpenFileBrowse: "Select",
				EPUBOpenUrlLabel: "Read file from URL",
				EPUBOpenUrlPlaceholder: "Input the URL of the epub file to open",
				EPUBOpenInputError: "Please select either file or URL.",
				EPUBOpenButton: "Start or continue reading"
			}
		}
	});

})();
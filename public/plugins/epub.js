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
				EPUBOpenFileLabel: "Ouvrir un fichier local",
				EPUBOpenFilePlaceholder: "Cliquer ici pour choisir un fichier EPUB à ouvrir",
				EPUBOpenFileBrowse: "Choisir",
				EPUBOpenUrlLabel: "Ouvrir une URL",
				EPUBOpenUrlPlaceholder: "Saisir ici l'URL du fichier EPUB à ouvrir",
				EPUBOpenNimbusLabel: "Ouvrir un fichier distant",
				EPUBOpenNimbusPlaceholder: "Saisir une partie du nom du fichier à ouvrir",
				EPUBOpenInputError: "Veuillez sélectionner l'une des options proposées.",
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
				EPUBOpenFilePlaceholder: "Select the local EPUB file to open",
				EPUBOpenFileBrowse: "Select",
				EPUBOpenUrlLabel: "Read file from URL",
				EPUBOpenUrlPlaceholder: "Write the URL of the EPUB file to open",
				EPUBOpenNimbusLabel: "Read remote file",
				EPUBOpenNimbusPlaceholder: "Write the name of the EPUB file to open",
				EPUBOpenInputError: "Please select one of available options.",
				EPUBOpenButton: "Start or continue reading"
			}
		}
	});

})();
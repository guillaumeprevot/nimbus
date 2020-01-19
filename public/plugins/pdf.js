(function() {

	var pdfImage = 'data:image/png;base64,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';

	function accept(item, extension) {
		return 'pdf' === extension;
	}

	NIMBUS.plugins.add({
		name: 'pdf',
		properties: [
			{ name: 'pageCount', caption: 'PDFPageCount', align: 'right', sortBy: 'content.pageCount', format: (i) => NIMBUS.formatInteger(i.pageCount) },
			{ name: 'pageSize', caption: 'PDFPageSize', align: 'right', sortBy: 'content.pageWidthInMillimeters', format: (i) => i.pageWidthInMillimeters ? (i.pageWidthInMillimeters + ' x ' + i.pageHeightInMillimeters + ' mm') : '' },
		],
		facets: [{
			name: 'pdf',
			accept: accept,
			icon: 'picture_as_pdf',
			thumbnail: function(item) { return item.thumbnail; },
			describe: function describe(item) {
				if (typeof item.pageCount !== 'number')
					return '';
				if (item.pageCount === 1)
					return NIMBUS.translate('PDFDescriptionSingle');
				return NIMBUS.translate('PDFDescriptionPlural', [item.pageCount]);
			}
		}],
		actions: [{
			name: 'pdf-read',
			icon: 'picture_as_pdf',
			caption: 'PDFRead',
			accept: accept,
			execute: function(item) {
				window.open('/pdf.html?' + $.param({
					url: '/files/stream/' + item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}
		}],
		langs: {
			fr: {
				PDFRead: "Lire",
				PDFPageCount: "Nombre de page",
				PDFPageSize: "Taille de page",
				PDFDescriptionSingle: "1 page",
				PDFDescriptionPlural: "{0} pages",
				PDFTitle: "Lecteur PDF",
				PDFShortcutFormat: "{0} ({1})",
				PDFFirstPage: "Afficher la première page (Début)",
				PDFPreviousPage: "Afficher la page précédente (PagePréc. ou glisser à droite)",
				PDFNextPage: "Afficher la page suivante (PageSuiv. ou glisser à gauche)",
				PDFLastPage: "Afficher la dernière page (Fin)",
				PDFPageNumbering: "Page {0} sur {1}",
				PDFPageSelect: "Choix de la page",
				PDFZoomOut: "Zoomer en arrière (Ctrl+-)",
				PDFZoomIn: "Zoomer en avant (Ctrl++)",
				PDFZoomMenu: "Réglage du zoom",
				PDFZoomFit: "Afficher la page entière (Ctrl+0)",
				PDFZoomWidth: "Ajuster en largeur",
				PDFZoomHeight: "Ajuster en hauteur",
				PDFRotateCCW: "Pivoter à gauche (Alt+←)",
				PDFRotateCW: "Pivoter à droite (Alt+→)",
				PDFAbout: "A Propos ...",
				PDFAboutProperty: "Propriété",
				PDFAboutValue: "Valeur",
				PDFAboutMetadata: {
					Title: "Titre",
					Author: "Auteur",
					Subject: "Sujet",
					Keywords: "Mots-clefs",
					CreationDate: "Créé le",
					ModDate: "Modifié le",
					Creator: "Créé par",
					Producer: "Logiciel",
					PDFFormatVersion: "Version",
				},
				PDFOpenFileLabel: "Ouvrir un fichier local",
				PDFOpenFilePlaceholder: "Cliquer ici pour choisir un fichier PDF à ouvrir",
				PDFOpenFileBrowse: "Choisir",
				PDFOpenUrlLabel: "Ouvrir une URL",
				PDFOpenUrlPlaceholder: "Saisir ici l'URL du fichier PDF à ouvrir",
				PDFOpenNimbusLabel: "Ouvrir un fichier distant",
				PDFOpenNimbusPlaceholder: "Saisir une partie du nom du fichier PDF à ouvrir",
				PDFOpenInputError: "Veuillez sélectionner l'une des options proposées.",
				PDFOpenButton: "Ouvrir le document PDF"
			},
			en: {
				PDFRead: "Read",
				PDFPageCount: "Page count",
				PDFPageSize: "Page size",
				PDFDescriptionSingle: "1 page",
				PDFDescriptionPlural: "{0} pages",
				PDFTitle: "PDF reader",
				PDFShorcutFormat: "{0} ({1})",
				PDFFirstPage: "Show first page (Home)",
				PDFPreviousPage: "Show previous page (PageUp or swipe right)",
				PDFNextPage: "Show next page (PageDown or swipe left)",
				PDFLastPage: "Show last page (End)",
				PDFPageNumbering: "Page {0} of {1}",
				PDFPageSelect: "Page selection",
				PDFZoomOut: "Zoom out (Ctrl+-)",
				PDFZoomIn: "Zoom in (Ctrl++)",
				PDFZoomMenu: "Zoom adjustment",
				PDFZoomFit: "Fit to page (Ctrl+0)",
				PDFZoomWidth: "Fit to width",
				PDFZoomHeight: "Fit to height",
				PDFRotateCCW: "Rotate left (Alt+←)",
				PDFRotateCW: "Rotate right (Alt+→)",
				PDFAbout: "About...",
				PDFAboutProperty: "Property",
				PDFAboutValue: "Value",
				PDFAboutMetadata: {
					Title: "Title",
					Author: "Author",
					Subject: "Subject",
					Keywords: "Keywords",
					CreationDate: "Creation",
					ModDate: "Modification",
					Creator: "Creator",
					Producer: "Producer",
					PDFFormatVersion: "Version",
				},
				PDFOpenFileLabel: "Open local file",
				PDFOpenFilePlaceholder: "Select the local PDF file to open",
				PDFOpenFileBrowse: "Select",
				PDFOpenUrlLabel: "Open file from URL",
				PDFOpenUrlPlaceholder: "Write the URL of the PDF file to open",
				PDFOpenNimbusLabel: "Open remote file",
				PDFOpenNimbusPlaceholder: "Write the name of the PDF file to open",
				PDFOpenInputError: "Please select one of available options.",
				PDFOpenButton: "Open the PDF document"
			}
		}
	});

})();
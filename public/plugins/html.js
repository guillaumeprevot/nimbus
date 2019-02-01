(function() {

	function accept(item, extension) {
		return 'html' === extension || 'htm' === extension || 'note' === extension;
	}

	NIMBUS.plugins.add({
		name: 'html',
		facets: [{
			name: 'html',
			accept: accept,
			image: function(item, thumbnail) {
				return '<i class="material-icons">art_track</i>';
			},
			describe: function describe(item) {
				if (typeof item.lines !== 'number')
					return '';
				if (item.lines === 0)
					return NIMBUS.translate('TextDescription0Line');
				if (item.lines === 1)
					return NIMBUS.translate('TextDescription1Line');
				return NIMBUS.translate('TextDescriptionNLines', [item.lines]);
			}
		}],
		actions: [{
			name: 'html-edit',
			icon: 'art_track',
			caption: 'HTMLActionEdit',
			accept: accept,
			execute: function(item) {
				window.open('/html-editor.html?' + $.param({
					itemId: item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}			
		}],
		langs: {
			fr: {
				HTMLActionEdit: "Ouvrir dans l'éditeur HTML",
				HTMLEditorTitle: "Editeur HTML",
				HTMLEditorSave: "Sauvegarder les modifications",
				HTMLEditorRemoveFormat: "Supprimer le formatage",
				HTMLEditorFontFamilyAgain: "Police de caractères",
				HTMLEditorFontFamilySelect: "Choisir la police de caractères",
				HTMLEditorFontSizeAgain: "Taille du texte",
				HTMLEditorFontSizeSelect: "Choisir la taille du texte",
				HTMLEditorFontSizeMinimal: "minimale",
				HTMLEditorFontSizeDefault: "par défaut",
				HTMLEditorFontSizeMaximal: "maximale",
				HTMLEditorForeColorAgain: "Couleur du texte",
				HTMLEditorForeColorSelect: "Choisir la couleur du texte",
				HTMLEditorBackColorAgain: "Couleur de remplissage",
				HTMLEditorBackColorSelect: "Choisir la couleur de remplissage",
				HTMLEditorBold: "Gras",
				HTMLEditorItalic: "Italic",
				HTMLEditorUnderline: "Souligné",
				HTMLEditorStrikeThrough: "Barré",
				HTMLEditorInsertUnorderedList: "Liste à puce",
				HTMLEditorInsertOrderedList: "Liste numérotée",
				HTMLEditorOutdent: "Diminuer le retrait",
				HTMLEditorIndent: "Augmenter le retrait",
				HTMLEditorQuote: "Citation",
				HTMLEditorInsertTable: "Insérer un tableau",
				HTMLEditorInsertTablePrompt: "Taille du tableau",
				HTMLEditorLink: "Créer un lien hypertexte",
				HTMLEditorLinkPrompt: "URL du lien",
				HTMLEditorUnlink: "Supprimer le lien hypertexte",
				HTMLEditorInsertImage: "Insérer une image",
				HTMLEditorInsertImagePrompt: "URL de l'image",
				HTMLEditorInsertVideo: "Insérer une vidéo",
				HTMLEditorInsertVideoPrompt: "URL de la vidéo",
				HTMLEditorInsertAudio: "Insérer une piste audio",
				HTMLEditorInsertAudioPrompt: "URL de la piste audio",
				HTMLEditorInsertHR: "Insérer une ligne horizontale",
				HTMLEditorOptions: "Options",
				HTMLEditorJustify: "Alignement",
				HTMLEditorJustifyLeft: "Aligné à gauche",
				HTMLEditorJustifyCenter: "Centré",
				HTMLEditorJustifyRight: "Aligné à droite",
				HTMLEditorJustifyFull: "Justifié",
				HTMLEditorSelection: "Sélection",
				HTMLEditorSubscript: "Afficher en indice",
				HTMLEditorSuperscript: "Afficher en exposant",
				HTMLEditorDecreaseFontSize: "Diminuer la taille du texte",
				HTMLEditorIncreaseFontSize: "Augmenter la taille du texte",
				HTMLEditorPasteAsHTML: "Activer le collage en HTML",
				HTMLEditorPasteAsText: "Activer le collage en texte brut",
				HTMLEditorPrint: "Imprimer",
				HTMLEditorSource: "Voir le source",
			},
			en: {
				HTMLActionEdit: "Open in HTML editor",
				HTMLEditorTitle: "HTML editor",
				HTMLEditorSave: "Save modifications",
				HTMLEditorRemoveFormat: "Remove formatting",
				HTMLEditorFontFamilyAgain: "Font family",
				HTMLEditorFontFamilySelect: "Select font family",
				HTMLEditorFontSizeAgain: "Font size",
				HTMLEditorFontSizeSelect: "Select font size",
				HTMLEditorFontSizeMinimal: "minimal",
				HTMLEditorFontSizeDefault: "default",
				HTMLEditorFontSizeMaximal: "maximal",
				HTMLEditorForeColorAgain: "Text color",
				HTMLEditorForeColorSelect: "Select text color",
				HTMLEditorBackColorAgain: "Background color",
				HTMLEditorBackColorSelect: "Select background color",
				HTMLEditorBold: "Bold",
				HTMLEditorItalic: "Italic",
				HTMLEditorUnderline: "Underline",
				HTMLEditorStrikeThrough: "Strike through",
				HTMLEditorInsertUnorderedList: "Unordered list",
				HTMLEditorInsertOrderedList: "Ordered list",
				HTMLEditorOutdent: "Outdent paragraph",
				HTMLEditorIndent: "Indent paragraph",
				HTMLEditorQuote: "Quote",
				HTMLEditorInsertTable: "Insert a table",
				HTMLEditorInsertTablePrompt: "Table size ROWSxCOLS",
				HTMLEditorLink: "Create a link",
				HTMLEditorLinkPrompt: "Link URL",
				HTMLEditorUnlink: "Remove link",
				HTMLEditorInsertImage: "Insert an image",
				HTMLEditorInsertImagePrompt: "Image URL",
				HTMLEditorInsertVideo: "Insert a video",
				HTMLEditorInsertVideoPrompt: "Video URL",
				HTMLEditorInsertAudio: "Insert an audio track",
				HTMLEditorInsertAudioPrompt: "Audio track URL",
				HTMLEditorInsertHR: "Insert an horizontal line",
				HTMLEditorOptions: "Options",
				HTMLEditorJustify: "Alignment",
				HTMLEditorJustifyLeft: "Left aligned",
				HTMLEditorJustifyCenter: "Centered",
				HTMLEditorJustifyRight: "Right aligned",
				HTMLEditorJustifyFull: "Justified",
				HTMLEditorSelection: "Selection",
				HTMLEditorSubscript: "Subscript",
				HTMLEditorSuperscript: "Superscript",
				HTMLEditorDecreaseFontSize: "Decrease font size",
				HTMLEditorIncreaseFontSize: "Increase font size",
				HTMLEditorPasteAsHTML: "Paste as formatted HTML",
				HTMLEditorPasteAsText: "Paste as plain text",
				HTMLEditorPrint: "Print",
				HTMLEditorSource: "Show source",
			} 
		}
	});

})();
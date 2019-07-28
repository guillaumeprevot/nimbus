(function() {

	function accept(item, extension) {
		return 'note' === extension;
	}

	NIMBUS.plugins.add({
		name: 'note',
		facets: [{
			name: 'note',
			accept: accept,
			icon: 'art_track',
			thumbnail: null,
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
			name: 'note-edit',
			icon: 'art_track',
			caption: 'NoteActionEdit',
			accept: accept,
			execute: function(item) {
				window.open('/note-editor.html?' + $.param({
					itemId: item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}			
		}],
		langs: {
			fr: {
				NoteActionEdit: "Ouvrir dans l'éditeur de notes",
				NoteEditorTitle: "Editeur de note",
				NoteEditorSave: "Sauvegarder les modifications",
				NoteEditorRemoveFormat: "Supprimer le formatage",
				NoteEditorFontFamilyAgain: "Police de caractères",
				NoteEditorFontFamilySelect: "Choisir la police de caractères",
				NoteEditorFontSizeAgain: "Taille du texte",
				NoteEditorFontSizeSelect: "Choisir la taille du texte",
				NoteEditorFontSizeMinimal: "minimale",
				NoteEditorFontSizeDefault: "par défaut",
				NoteEditorFontSizeMaximal: "maximale",
				NoteEditorForeColorAgain: "Couleur du texte",
				NoteEditorForeColorSelect: "Choisir la couleur du texte",
				NoteEditorBackColorAgain: "Couleur de remplissage",
				NoteEditorBackColorSelect: "Choisir la couleur de remplissage",
				NoteEditorBold: "Gras",
				NoteEditorItalic: "Italic",
				NoteEditorUnderline: "Souligné",
				NoteEditorStrikeThrough: "Barré",
				NoteEditorInsertUnorderedList: "Liste à puce",
				NoteEditorInsertOrderedList: "Liste numérotée",
				NoteEditorOutdent: "Diminuer le retrait",
				NoteEditorIndent: "Augmenter le retrait",
				NoteEditorQuote: "Citation",
				NoteEditorInsertTable: "Insérer un tableau",
				NoteEditorInsertTablePrompt: "Taille du tableau",
				NoteEditorLink: "Créer un lien hypertexte",
				NoteEditorLinkPrompt: "URL du lien",
				NoteEditorUnlink: "Supprimer le lien hypertexte",
				NoteEditorInsertImage: "Insérer une image",
				NoteEditorInsertImagePrompt: "URL de l'image",
				NoteEditorInsertVideo: "Insérer une vidéo",
				NoteEditorInsertVideoPrompt: "URL de la vidéo",
				NoteEditorInsertAudio: "Insérer une piste audio",
				NoteEditorInsertAudioPrompt: "URL de la piste audio",
				NoteEditorInsertHR: "Insérer une ligne horizontale",
				NoteEditorOptions: "Options",
				NoteEditorJustify: "Alignement",
				NoteEditorJustifyLeft: "Aligné à gauche",
				NoteEditorJustifyCenter: "Centré",
				NoteEditorJustifyRight: "Aligné à droite",
				NoteEditorJustifyFull: "Justifié",
				NoteEditorSelection: "Sélection",
				NoteEditorSubscript: "Afficher en indice",
				NoteEditorSuperscript: "Afficher en exposant",
				NoteEditorDecreaseFontSize: "Diminuer la taille du texte",
				NoteEditorIncreaseFontSize: "Augmenter la taille du texte",
				NoteEditorPasteAsHTML: "Activer le collage en HTML",
				NoteEditorPasteAsText: "Activer le collage en texte brut",
				NoteEditorPrint: "Imprimer",
				NoteEditorSource: "Voir le source",
			},
			en: {
				NoteActionEdit: "Open in note editor",
				NoteEditorTitle: "Note editor",
				NoteEditorSave: "Save modifications",
				NoteEditorRemoveFormat: "Remove formatting",
				NoteEditorFontFamilyAgain: "Font family",
				NoteEditorFontFamilySelect: "Select font family",
				NoteEditorFontSizeAgain: "Font size",
				NoteEditorFontSizeSelect: "Select font size",
				NoteEditorFontSizeMinimal: "minimal",
				NoteEditorFontSizeDefault: "default",
				NoteEditorFontSizeMaximal: "maximal",
				NoteEditorForeColorAgain: "Text color",
				NoteEditorForeColorSelect: "Select text color",
				NoteEditorBackColorAgain: "Background color",
				NoteEditorBackColorSelect: "Select background color",
				NoteEditorBold: "Bold",
				NoteEditorItalic: "Italic",
				NoteEditorUnderline: "Underline",
				NoteEditorStrikeThrough: "Strike through",
				NoteEditorInsertUnorderedList: "Unordered list",
				NoteEditorInsertOrderedList: "Ordered list",
				NoteEditorOutdent: "Outdent paragraph",
				NoteEditorIndent: "Indent paragraph",
				NoteEditorQuote: "Quote",
				NoteEditorInsertTable: "Insert a table",
				NoteEditorInsertTablePrompt: "Table size ROWSxCOLS",
				NoteEditorLink: "Create a link",
				NoteEditorLinkPrompt: "Link URL",
				NoteEditorUnlink: "Remove link",
				NoteEditorInsertImage: "Insert an image",
				NoteEditorInsertImagePrompt: "Image URL",
				NoteEditorInsertVideo: "Insert a video",
				NoteEditorInsertVideoPrompt: "Video URL",
				NoteEditorInsertAudio: "Insert an audio track",
				NoteEditorInsertAudioPrompt: "Audio track URL",
				NoteEditorInsertHR: "Insert an horizontal line",
				NoteEditorOptions: "Options",
				NoteEditorJustify: "Alignment",
				NoteEditorJustifyLeft: "Left aligned",
				NoteEditorJustifyCenter: "Centered",
				NoteEditorJustifyRight: "Right aligned",
				NoteEditorJustifyFull: "Justified",
				NoteEditorSelection: "Selection",
				NoteEditorSubscript: "Subscript",
				NoteEditorSuperscript: "Superscript",
				NoteEditorDecreaseFontSize: "Decrease font size",
				NoteEditorIncreaseFontSize: "Increase font size",
				NoteEditorPasteAsHTML: "Paste as formatted HTML",
				NoteEditorPasteAsText: "Paste as plain text",
				NoteEditorPrint: "Print",
				NoteEditorSource: "Show source",
			} 
		}
	});

})();
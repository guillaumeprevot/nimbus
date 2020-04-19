(function() {

	function accept(item, extension) {
		return 'note' === extension || 'html' === extension;
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
				NoteEditorSave: "Sauvegarder les modifications (Ctrl+S)",
				NoteEditorRemoveFormat: "Supprimer le formatage (Ctrl+M)",
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
				NoteEditorBold: "Gras (Ctrl+B)",
				NoteEditorItalic: "Italic (Ctrl+I)",
				NoteEditorUnderline: "Souligné (Ctrl+U)",
				NoteEditorStrikeThrough: "Barré (Ctrl+!)",
				NoteEditorInsertUnorderedList: "Liste à puce (Ctrl+Maj+U)",
				NoteEditorInsertOrderedList: "Liste numérotée (Ctrl+Maj+O)",
				NoteEditorOutdent: "Diminuer le retrait (Maj+Tab)",
				NoteEditorIndent: "Augmenter le retrait (Tab)",
				NoteEditorQuote: "Citation",
				NoteEditorInsertTable: "Insérer un tableau",
				NoteEditorInsertTablePrompt: "Taille du tableau (LxC)",
				NoteEditorLink: "Créer un lien hypertexte (Ctrl+K)",
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
				NoteEditorJustifyLeft: "Aligné à gauche (Ctrl+Maj+L)",
				NoteEditorJustifyCenter: "Centré (Ctrl+Maj+E)",
				NoteEditorJustifyRight: "Aligné à droite (Ctrl+Maj+R)",
				NoteEditorJustifyFull: "Justifié (Ctrl+Maj+J)",
				NoteEditorSelection: "Sélection",
				NoteEditorSubscript: "Afficher en indice",
				NoteEditorSuperscript: "Afficher en exposant",
				NoteEditorDecreaseFontSize: "Diminuer la taille du texte",
				NoteEditorIncreaseFontSize: "Augmenter la taille du texte",
				NoteEditorEnablePasteAsHTML: "Activer le collage en HTML",
				NoteEditorEnableObjectResizing: "Activer le redimensionnement",
				NoteEditorPrint: "Imprimer",
				NoteEditorSource: "Voir le source",
			},
			en: {
				NoteActionEdit: "Open in note editor",
				NoteEditorTitle: "Note editor",
				NoteEditorSave: "Save modifications (Ctrl+S)",
				NoteEditorRemoveFormat: "Remove formatting (Ctrl+M)",
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
				NoteEditorBold: "Bold (Ctrl+B)",
				NoteEditorItalic: "Italic (Ctrl+I)",
				NoteEditorUnderline: "Underline (Ctrl+U)",
				NoteEditorStrikeThrough: "Strike through (Ctrl+!)",
				NoteEditorInsertUnorderedList: "Unordered list (Ctrl+Shift+U)",
				NoteEditorInsertOrderedList: "Ordered list (Ctrl+Shift+O)",
				NoteEditorOutdent: "Outdent paragraph (Shift+Tab)",
				NoteEditorIndent: "Indent paragraph (Tab)",
				NoteEditorQuote: "Quote",
				NoteEditorInsertTable: "Insert a table",
				NoteEditorInsertTablePrompt: "Table size (ROWSxCOLS)",
				NoteEditorLink: "Create a link (Ctrl+K)",
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
				NoteEditorJustifyLeft: "Left aligned (Ctrl+Shift+L)",
				NoteEditorJustifyCenter: "Centered (Ctrl+Shift+E)",
				NoteEditorJustifyRight: "Right aligned (Ctrl+Shift+R)",
				NoteEditorJustifyFull: "Justified (Ctrl+Shift+J)",
				NoteEditorSelection: "Selection",
				NoteEditorSubscript: "Subscript",
				NoteEditorSuperscript: "Superscript",
				NoteEditorDecreaseFontSize: "Decrease font size",
				NoteEditorIncreaseFontSize: "Increase font size",
				NoteEditorEnablePasteAsHTML: "Enable paste as formatted HTML",
				NoteEditorEnableObjectResizing: "Enable object resizing anchors",
				NoteEditorPrint: "Print",
				NoteEditorSource: "Show source",
			}
		}
	});

})();
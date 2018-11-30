(function() {

	NIMBUS.plugins.add({
		name: 'text',
		properties: [
			{ name: 'lines', caption: 'TextPropertyLines', align: 'right', sortBy: 'content.lines', format: (i) => NIMBUS.formatInteger(i.lines, '') }
		],
		facets: [{
			name: 'text',
			accept: NIMBUS.utils.isTextFile,
			image: function(item, thumbnail) {
				return '<i class="material-icons">subject</i>';
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
			name: 'text-edit',
			icon: 'subject',
			caption: 'TextActionEdit',
			accept: NIMBUS.utils.isTextFile,
			execute: function(item) {
				window.open('/text-editor.html?' + $.param({
					itemId: item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}
		}],
		langs: {
			fr: {
				TextActionEdit: "Ouvrir dans l'éditeur de texte",
				TextPropertyLines: "Lignes",
				TextDescription0Line: "0 ligne",
				TextDescription1Line: "1 ligne",
				TextDescriptionNLines: "{0} lignes",
				TextEditorTitle: "Editeur de texte",
				TextEditorSave: "Sauvegarder les modifications",
				TextEditorSmaller: "Diminuer la taille du texte",
				TextEditorLarger: "Augmenter la taille du texte",
				TextEditorLineSeparator: "Sauts de ligne",
				TextEditorLineSeparatorCRLF: "Windows (CR+LF)",
				TextEditorLineSeparatorLF: "UNIX (LF)",
				TextEditorLineSeparatorCR: "Mac (CR)",
				TextEditorPlaceholder: "Le fichier est pour le moment vide..."
			},
			en: {
				TextActionEdit: "Open in text editor",
				TextPropertyLines: "Lines",
				TextDescription0Line: "0 line",
				TextDescription1Line: "1 line",
				TextDescriptionNLines: "{0} lines",
				TextEditorTitle: "Text editor",
				TextEditorSave: "Save modifications",
				TextEditorSmaller: "Decrease text size",
				TextEditorLarger: "Increase text size",
				TextEditorLineSeparator: "Line separator",
				TextEditorLineSeparatorCRLF: "Windows (CR+LF)",
				TextEditorLineSeparatorLF: "UNIX (LF)",
				TextEditorLineSeparatorCR: "Mac (CR)",
				TextEditorPlaceholder: "The file is currently empty..."
			} 
		}
	});

})();

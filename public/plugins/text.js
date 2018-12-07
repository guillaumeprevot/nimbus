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
			name: 'markdown-edit',
			icon: 'subject',
			caption: 'TextActionEditMarkdown',
			accept: function(item, extension) {
				return 'md' === extension || 'markdown' === extension;
			},
			execute: function(item) {
				window.open('/markdown-editor.html?' + $.param({
					itemId: item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}
		}, {
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
				TextActionEditMarkdown: "Ouvrir dans l'éditeur Markdown",
				TextActionEdit: "Ouvrir dans l'éditeur de texte",
				TextPropertyLines: "Lignes",
				TextDescription0Line: "0 ligne",
				TextDescription1Line: "1 ligne",
				TextDescriptionNLines: "{0} lignes",
				TextEditorTitle: "Editeur",
				TextEditorSave: "Sauvegarder les modifications",
				TextEditorSmaller: "Diminuer la taille du texte",
				TextEditorLarger: "Augmenter la taille du texte",
				TextEditorPreview: "Prévisualisation",
				TextEditorExport: "Exporter en HTML",
				TextEditorLineSeparator: "Sauts de ligne",
				TextEditorLineSeparatorCRLF: "Windows (CR+LF)",
				TextEditorLineSeparatorLF: "UNIX (LF)",
				TextEditorLineSeparatorCR: "Mac (CR)",
				TextEditorPlaceholder: "Le fichier est pour le moment vide..."
			},
			en: {
				TextActionEditMarkdown: "Open in Markdown editor",
				TextActionEdit: "Open in text editor",
				TextPropertyLines: "Lines",
				TextDescription0Line: "0 line",
				TextDescription1Line: "1 line",
				TextDescriptionNLines: "{0} lines",
				TextEditorTitle: "Editor",
				TextEditorSave: "Save modifications",
				TextEditorSmaller: "Decrease text size",
				TextEditorLarger: "Increase text size",
				TextEditorPreview: "Preview",
				TextEditorExport: "Export to HTML",
				TextEditorLineSeparator: "Line separator",
				TextEditorLineSeparatorCRLF: "Windows (CR+LF)",
				TextEditorLineSeparatorLF: "UNIX (LF)",
				TextEditorLineSeparatorCR: "Mac (CR)",
				TextEditorPlaceholder: "The file is currently empty..."
			} 
		}
	});

})();

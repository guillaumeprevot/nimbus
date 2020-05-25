(function() {

	NIMBUS.utils.textAPI = {
		/** Cette méthode recherche le séparateur de ligne utilisé et renvoie un code 'crlf', 'cr' ou 'lf' */
		getLineSeparator: (content) => (!content || content.indexOf('\r\n') >= 0) ? 'crlf' : (content.indexOf('\n') >= 0) ? 'lf' : 'cr',
		/** Cette méthode reformat le texte avec le séparateur de ligne donné, qui peut être 'crlf', 'cr' ou 'lf' */
		fixLineSeparator: (content, separator) => content.replace('\\r\\n', '\n').replace('\\r', '\n').split('\n').join(separator.replace('cr', '\r').replace('lf', '\n'))
	};

	NIMBUS.plugins.add({
		name: 'text',
		properties: [
			{ name: 'lines', caption: 'TextPropertyLines', align: 'right', sortBy: 'content.lines', format: (i) => NIMBUS.formatInteger(i.lines, '') }
		],
		facets: [{
			name: 'text',
			accept: NIMBUS.utils.isTextFile,
			icon: 'subject',
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
			name: 'text-edit',
			icon: 'subject',
			caption: 'TextActionEdit',
			accept: NIMBUS.utils.isTextFile,
			url: (item) => '/text-editor.html?itemId=' + item.id
		}],
		langs: {
			fr: {
				TextActionEdit: "Ouvrir dans l'éditeur de texte",
				TextPropertyLines: "Lignes",
				TextDescription0Line: "0 ligne",
				TextDescription1Line: "1 ligne",
				TextDescriptionNLines: "{0} lignes",
				TextEditorTitle: "Editeur",
				TextEditorSave: "Sauvegarder les modifications (Ctrl+S)",
				TextEditorSmaller: "Diminuer la taille du texte (Ctrl+-)",
				TextEditorLarger: "Augmenter la taille du texte (Ctrl++)",
				TextEditorLineSeparator: "Sauts de ligne",
				TextEditorLineSeparatorCRLF: "Windows (CR+LF)",
				TextEditorLineSeparatorLF: "Linux, Android, OS X (LF)",
				TextEditorLineSeparatorCR: "Mac OS (CR)",
				TextEditorPlaceholder: "Le fichier est pour le moment vide..."
			},
			en: {
				TextActionEdit: "Open in text editor",
				TextPropertyLines: "Lines",
				TextDescription0Line: "0 line",
				TextDescription1Line: "1 line",
				TextDescriptionNLines: "{0} lines",
				TextEditorTitle: "Editor",
				TextEditorSave: "Save modifications (Ctrl+S)",
				TextEditorSmaller: "Decrease text size (Ctrl+-)",
				TextEditorLarger: "Increase text size (Ctrl++)",
				TextEditorLineSeparator: "Line separator",
				TextEditorLineSeparatorCRLF: "Windows (CR+LF)",
				TextEditorLineSeparatorLF: "Linux, Android, OS X (LF)",
				TextEditorLineSeparatorCR: "Mac OS (CR)",
				TextEditorPlaceholder: "The file is currently empty..."
			}
		}
	});

})();

(function() {

	function accept(item, extension) {
		return 'md' === extension || 'mkd' === extension || 'markdown' === extension;
	}

	NIMBUS.plugins.add({
		name: 'markdown',
		facets: [{
			name: 'markdown',
			accept: accept,
			icon: 'list_alt',
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
			name: 'markdown-edit',
			icon: 'list_alt',
			caption: 'MarkdownActionEdit',
			accept: accept,
			url: (item) => '/markdown-editor.html?itemId=' + item.id
		}],
		langs: {
			fr: {
				MarkdownActionEdit: "Ouvrir dans l'éditeur Markdown",
				MarkdownEditorPreviewHTML: "Prévisualiser en HTML (Ctrl+P ou glisser horizontal)",
				MarkdownEditorEditMarkdown: "Repasser en Markdown (Ctrl+P ou glisser horizontal)",
				MarkdownEditorView: "Choisir la vue (Ctrl+P)",
				MarkdownEditorSplitView: "Vue côte à côte",
				MarkdownEditorEditView: "Éditeur Markdown",
				MarkdownEditorHTMLView: "Visualisation HTML",
				MarkdownEditorExportHTML: "Exporter en HTML (Ctrl+E)",
				MarkdownEditorWrapBold: "Gras (Ctrl+B)",
				MarkdownEditorWrapItalic: "Italique (Ctrl+I)",
				MarkdownEditorWrapUnderline: "Souligné (Ctrl+U)",
				MarkdownEditorWrapStrikeThrough: "Barré (Ctrl+-)",
				MarkdownEditorWrapMark: "Surligné (Ctrl+M)",
				MarkdownEditorWrapKeyword: "Mot-clef (Ctrl+,)",
				MarkdownEditorWrapSup: "Exposant",
				MarkdownEditorWrapSub: "Indice",
				MarkdownEditorInsertLink: "Générer un lien (Ctrl+K)",
				MarkdownEditorInsertLinkPrompt: "URL du lien",
				MarkdownEditorInsertImage: "Générer une image",
				MarkdownEditorInsertImagePrompt: "URL de l'image",
				MarkdownEditorInsertCode: "Générer un bloc de code",
				MarkdownEditorInsertCodePrompt: "Language",
				MarkdownEditorQuote: "Citation",
				MarkdownEditorEmoji: "Émoticônes",
				MarkdownEditorHR: "Insérer une ligne horizontale",
				MarkdownEditorUnorderedList: "Liste à puce (Ctrl+Maj+U)",
				MarkdownEditorOrderedList: "Liste numérotée (Ctrl+Maj+O)",
				MarkdownEditorOutdent: "Diminuer le retrait (Maj+Tab)",
				MarkdownEditorIndent: "Augmenter le retrait (Tab)",
			},
			en: {
				MarkdownActionEdit: "Open in Markdown editor",
				MarkdownEditorPreviewHTML: "Preview HTML (Ctrl+P ou horizontal swipe)",
				MarkdownEditorEditMarkdown: "Edit Markdown (Ctrl+P ou horizontal swipe)",
				MarkdownEditorView: "Select layout (Ctrl+P)",
				MarkdownEditorSplitView: "Side-by-side",
				MarkdownEditorEditView: "Markdown editor",
				MarkdownEditorHTMLView: "HTML preview",
				MarkdownEditorExportHTML: "Export to HTML (Ctrl+E)",
				MarkdownEditorWrapBold: "Bold (Ctrl+B)",
				MarkdownEditorWrapItalic: "Italic (Ctrl+I)",
				MarkdownEditorWrapUnderline: "Underline (Ctrl+U)",
				MarkdownEditorWrapStrikeThrough: "Strike through (Ctrl+-)",
				MarkdownEditorWrapMark: "Marker (Ctrl+M)",
				MarkdownEditorWrapKeyword: "Keyword (Ctrl+,)",
				MarkdownEditorWrapSup: "Superscript",
				MarkdownEditorWrapSub: "Subscript",
				MarkdownEditorInsertLink: "Insert link (Ctrl+K)",
				MarkdownEditorInsertLinkPrompt: "Target URL",
				MarkdownEditorInsertImage: "Insert image",
				MarkdownEditorInsertImagePrompt: "Image URL",
				MarkdownEditorInsertCode: "Insert code block",
				MarkdownEditorInsertCodePrompt: "Language",
				MarkdownEditorQuote: "Quote",
				MarkdownEditorEmoji: "Emoji",
				MarkdownEditorHR: "Insert an horizontal line",
				MarkdownEditorUnorderedList: "Unordered list (Ctrl+Shift+U)",
				MarkdownEditorOrderedList: "Ordered list (Ctrl+Shift+O)",
				MarkdownEditorOutdent: "Outdent paragraph (Shift+Tab)",
				MarkdownEditorIndent: "Indent paragraph (Tab)",
			}
		}
	});

})();

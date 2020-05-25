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
				MarkdownEditorView: "Choisir la vue",
				MarkdownEditorSplitView: "Vue côte à côte",
				MarkdownEditorEditView: "Éditeur Markdown",
				MarkdownEditorHTMLView: "Visualisation HTML",
				MarkdownEditorExportHTML: "Exporter en HTML",
			},
			en: {
				MarkdownActionEdit: "Open in Markdown editor",
				MarkdownEditorPreviewHTML: "Preview HTML (Ctrl+P ou horizontal swipe)",
				MarkdownEditorEditMarkdown: "Edit Markdown (Ctrl+P ou horizontal swipe)",
				MarkdownEditorView: "Select layout",
				MarkdownEditorSplitView: "Side-by-side",
				MarkdownEditorEditView: "Markdown editor",
				MarkdownEditorHTMLView: "HTML preview",
				MarkdownEditorExportHTML: "Export to HTML",
			}
		}
	});

})();

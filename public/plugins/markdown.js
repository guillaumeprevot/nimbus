(function() {

	function accept(item, extension) {
		return 'md' === extension || 'mkd' === extension || 'markdown' === extension;
	}

	NIMBUS.plugins.add({
		name: 'markdown',
		facets: [{
			name: 'markdown',
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
			name: 'markdown-edit',
			icon: 'art_track',
			caption: 'MarkdownActionEdit',
			accept: accept,
			execute: function(item) {
				window.open('/markdown-editor.html?' + $.param({
					itemId: item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}
		}],
		langs: {
			fr: {
				MarkdownActionEdit: "Ouvrir dans l'éditeur Markdown",
				MarkdownEditorPreview: "Prévisualisation",
				MarkdownEditorExport: "Exporter en HTML",
			},
			en: {
				MarkdownActionEdit: "Open in Markdown editor",
				MarkdownEditorPreview: "Preview",
				MarkdownEditorExport: "Export to HTML",
			} 
		}
	});

})();

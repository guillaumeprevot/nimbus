(function() {

	NIMBUS.plugins.add({
		name: 'windows-shortcut',
		properties: [
			{ name: 'url', caption: 'WindowsShortcutURL', sortBy: 'content.url' },
			{ name: 'iconURL', caption: 'WindowsShortcutIconURL', sortBy: 'content.iconURL' }
		],
		facets: [{
			name: 'windows-shorcut',
			accept: function(item, extension) {
				return 'url' === extension;
			},
			image: function(item, thumbnail) {
				if (thumbnail && item.iconURL)
					return '<img src="' + item.iconURL + '" style="width: 24px; height: 24px;" />';
				return '<i class="material-icons">link</i>';
			},
			describe: function(item) {
				return item.url || '';
			}
		}],
		actions: [{
			name: 'open',
			icon: 'open_in_new',
			caption: 'WindowsShortcutOpen',
			accept: function(item, extension) {
				return item.url && ('url' === extension);
			},
			execute: function(item) {
				window.open(item.url);
			}			
		}],
		langs: {
			fr: {
				WindowsShortcutOpen: "Ouvrir ce raccourci",
				WindowsShortcutURL: "URL",
				WindowsShortcutIconURL: "URL de l'icône"
			},
			en: {
				WindowsShortcutOpen: "Open this URL",
				WindowsShortcutURL: "URL",
				WindowsShortcutIconURL: "Icon URL"
			}
		}
	});

})();
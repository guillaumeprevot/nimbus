(function() {

	NIMBUS.plugins.add({
		name: 'windows-shortcut',
		properties: [
			{ name: 'url', caption: 'WindowsShortcutURL', sortBy: 'content.url', format: (i) => i.url || '' },
			{ name: 'iconURL', caption: 'WindowsShortcutIconURL', sortBy: 'content.iconURL', format: (i) => i.iconURL || '' }
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
				var length, mimetype;
				if (item.url)
					return item.url;
				return NIMBUS.plugins.facets[NIMBUS.plugins.facets.length - 1].describe(item);
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
(function() {

	NIMBUS.utils.textFileExtensions.push('url');

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
			icon: 'link',
			thumbnail: function(item) { return item.iconURL; },
			describe: function(item) {
				return item.url || '';
			}
		}],
		actions: [{
			name: 'open',
			icon: 'open_in_new',
			caption: 'WindowsShortcutOpen',
			accept: function(item, extension) {
				return !!item.url && ('url' === extension);
			},
			url: (item) => item.url
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
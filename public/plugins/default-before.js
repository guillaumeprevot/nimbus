(function() {
	NIMBUS.plugins.add({
		name: 'default-before',
		properties: [
			{ name: 'id', caption: 'CommonPropertyId', align: 'right', sortBy: '_id', format: (i) => NIMBUS.formatInteger(i.id) },
			{ name: 'folder', caption: 'CommonPropertyFolder', align: 'center', sortBy: 'folder', format: (i) => NIMBUS.formatBoolean(i.folder, 'folder') },
			{ name: 'length', caption: 'CommonPropertyLength', align: 'right', width: 100, sortBy: 'content.length', format: (i) => NIMBUS.formatLength(i.length) },
			{ name: 'createDate', caption: 'CommonPropertyCreateDate', align: 'right', width: NIMBUS.translate('CommonDateTimeColumnWidth'), sortBy: 'createDate', format: (i) => NIMBUS.formatDatetime(i.createDate) },
			{ name: 'updateDate', caption: 'CommonPropertyUpdateDate', align: 'right', width: NIMBUS.translate('CommonDateTimeColumnWidth'), sortBy: 'updateDate', format: (i) => NIMBUS.formatDatetime(i.updateDate) },
			{ name: 'tags', caption: 'CommonPropertyTags', format: (i) => i.tags },
			{ name: 'description', caption: 'CommonPropertyDescription', format: (i, facet) => facet.describe(i) },
			{ name: 'itemCount', caption: 'CommonPropertyItemCount', align: 'right', sortBy: 'content.itemCount', format: (i) => NIMBUS.formatInteger(i.itemCount) },
			{ name: 'iconURL', caption: 'CommonPropertyIconURL', sortBy: 'content.iconURL', format: (i) => i.iconURL || '' },
			{ name: 'mimetype', caption: 'CommonPropertyMimetype', width: 120, format: (i) => i.mimetype || '' },
			{ name: 'progress', caption: 'CommonPropertyProgress', align: 'right', sortBy: 'content.progress', format: (i) => i.progress ? NIMBUS.formatInteger(i.progress) : '' },
			{ name: 'status', caption: 'CommonPropertyStatus', sortBy: 'content.status', format: (i) => i.status || '' },
			{ name: 'sourceURL', caption: 'CommonPropertySourceURL', sortBy: 'content.sourceURL', format: (i) => i.sourceURL || '' },
			{ name: 'shared', caption: 'CommonPropertyShared', align: 'center', width: 80, sortBy: 'sharedPassword', format: (i) => NIMBUS.formatBoolean(!!i.sharedPassword, 'share') },
			{ name: 'sharedDate', caption: 'CommonPropertySharedDate', align: 'right', width: NIMBUS.translate('CommonDateTimeColumnWidth'), sortBy: 'sharedDate', format: (i) => NIMBUS.formatDatetime(i.sharedDate) },
			{ name: 'sharedDuration', caption: 'CommonPropertySharedDuration', align: 'right', sortBy: 'sharedDuration', format: (i) => !i.sharedPassword ? '' : i.sharedDuration ? (NIMBUS.formatInteger(i.sharedDuration) + 'min') : '∞' },
		],
		facets: [
			{
				name: 'folder',
				accept: function(item, extension) {
					return !!item.folder;
				},
				image: function(item, thumbnail) {
					if (thumbnail && item.iconURL)
						return '<img src="' + (item.iconURLCache || item.iconURL) + '" style="width: 24px; height: 24px;" />';
					return '<i class="material-icons">folder</i>';
				},
				describe: function(item) {
					if (item.itemCount === 0)
						return NIMBUS.translate('CommonFolderDescriptionEmpty');
					if (item.itemCount === 1)
						return NIMBUS.translate('CommonFolderDescriptionOneChild');
					return NIMBUS.translate('CommonFolderDescriptionMultipleChildren', [item.itemCount]);
				}
			}
		],
		actions: [
			{
				name: 'navigate',
				icon: 'folder_open',
				caption: 'ActionNavigate',
				accept: function(item, extension) {
					// La navigation ne se fait que dans des dossiers
					return item.folder;
				},
				execute: function(item) {
					NIMBUS.navigation.goToFolderAndRefreshItems(item);
				}
			}, {
				name: 'navigate-tab',
				icon: 'open_in_new',
				caption: 'ActionNavigateTab',
				accept: function(item, extension) {
					// Cette action ouvre un dossier dans un nouvel onglet
					return item.folder;
				},
				execute: function(item) {
					window.open('/main.html#' + item.path + item.id);
				}
			}
		]
	});
})();
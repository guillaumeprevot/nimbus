(function() {
	NIMBUS.plugins.add({
		name: 'default-before',
		properties: [
			{ name: 'id', caption: 'CommonPropertyId', align: 'right', width: 60, sortBy: 'id', format: (i) => NIMBUS.formatInteger(i.id) },
			{ name: 'folder', caption: 'CommonPropertyFolder', align: 'center', width: 80, sortBy: 'folder', format: (i) => NIMBUS.formatBoolean(i.folder, 'folder') },
			{ name: 'length', caption: 'CommonPropertyLength', align: 'right', width: 100, sortBy: 'content.length', format: (i) => NIMBUS.formatLength(i.length) },
			{ name: 'createDate', caption: 'CommonPropertyCreateDate', align: 'right', width: NIMBUS.translate('CommonDateTimeColumnWidth'), sortBy: 'createDate', format: (i) => NIMBUS.formatDatetime(i.createDate) },
			{ name: 'updateDate', caption: 'CommonPropertyUpdateDate', align: 'right', width: NIMBUS.translate('CommonDateTimeColumnWidth'), sortBy: 'updateDate', format: (i) => NIMBUS.formatDatetime(i.updateDate) },
			{ name: 'tags', caption: 'CommonPropertyTags' },
			{ name: 'description', caption: 'CommonPropertyDescription', format: (i, facet) => facet.describe(i) },
			{ name: 'itemCount', caption: 'CommonPropertyItemCount', align: 'right', sortBy: 'content.itemCount', format: (i) => NIMBUS.formatInteger(i.itemCount) },
			{ name: 'iconURL', caption: 'CommonPropertyIconURL', sortBy: 'content.iconURL' },
			{ name: 'extension', caption: 'CommonPropertyExtension', width: 80, sortBy: (i1, i2) => NIMBUS.utils.getFileExtensionFromItem(i1).localeCompare(NIMBUS.utils.getFileExtensionFromItem(i2)), format: (i) => NIMBUS.utils.getFileExtensionFromItem(i) },
			{ name: 'mimetype', caption: 'CommonPropertyMimetype', width: 120, sortBy: (i1, i2) => (i1.mimetype || '').localeCompare(i2.mimetype || '') },
			{ name: 'progress', caption: 'CommonPropertyProgress', align: 'right', sortBy: 'content.progress', format: (i) => i.progress ? NIMBUS.formatInteger(i.progress) : '' },
			{ name: 'status', caption: 'CommonPropertyStatus', sortBy: 'content.status' },
			{ name: 'sourceURL', caption: 'CommonPropertySourceURL', sortBy: 'content.sourceURL' },
			{ name: 'shared', caption: 'CommonPropertyShared', align: 'center', width: 80, sortBy: 'sharedPassword', format: (i) => NIMBUS.formatBoolean(!!i.sharedPassword, 'share') },
			{ name: 'sharedDate', caption: 'CommonPropertySharedDate', align: 'right', width: NIMBUS.translate('CommonDateTimeColumnWidth'), sortBy: 'sharedDate', format: (i) => NIMBUS.formatDatetime(i.sharedDate) },
			{ name: 'sharedDuration', caption: 'CommonPropertySharedDuration', align: 'right', sortBy: 'sharedDuration', format: (i) => !i.sharedPassword ? '' : i.sharedDuration ? (NIMBUS.formatInteger(i.sharedDuration) + 'min') : '∞' },
			{ name: 'hidden', caption: 'CommonPropertyHidden', align: 'center', width: 80, sortBy: 'hidden', format: (i) => NIMBUS.formatBoolean(i.hidden, 'visibility_off') },
		],
		facets: [
			{
				name: 'folder',
				accept: (item, _extension) => item.folder,
				icon: 'folder',
				thumbnail: function(item) { return item.iconURL ? (item.iconURLCache || item.iconURL) : null; },
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
				// La navigation ne se fait que dans des dossiers
				accept: (item, _extension) => item.folder,
				// Le support de l'URL vers le dossier pour l'ouverture dans un onglet
				url: (item) => '/nav/' + item.path.replace(',', '/') + item.id,
				// Le support de "execute", pour naviguer dans le dossier sans recharger la page
				execute: (item) => NIMBUS.navigation.goToFolderAndRefreshItems(item)
			}, {
				name: 'usage',
				icon: 'functions',
				caption: 'ActionCalculateStatistics',
				// Cette action mesure l'occupation d'un dossier
				accept: (item, _extension) => item.folder,
				// Le support de "execute" pour donner les infos à l'utilisateur
				execute: (item) => {
					$.get('/items/folder/statistics?recursive=false&parentId=' + item.id).done(function(data1) {
						$.get('/items/folder/statistics?recursive=true&parentId=' + item.id).done(function(data2) {
							var size1 = NIMBUS.formatLength(data1.size);
							var part1 = NIMBUS.translate('CommonFolderStatisticsMessagePart', data1.folders, data1.files, size1);
							var size2 = NIMBUS.formatLength(data2.size);
							var part2 = NIMBUS.translate('CommonFolderStatisticsMessagePart', data2.folders, data2.files, size2);
							NIMBUS.message(NIMBUS.translate('CommonFolderStatisticsMessageContent', part1, part2), false/*error=false*/, true/*html=true*/);
						});
					});
				}
			}
		]
	});
})();
(function() {
	NIMBUS.plugins.add({
		name: 'default-after',
		facets: [
			{
				name: 'file',
				accept: function(item, extension) {
					return !item.folder;
				},
				icon: 'insert_drive_file',
				thumbnail: null,
				describe: function(item) {
					return '';
				}
			}
		],
		actions: [
			{
				name: 'download-refresh',
				icon: 'refresh',
				caption: 'ActionDownloadRefresh',
				accept: function(item, extension) {
					// Fonction dispo pour les fichiers téléchargés depuis une URL "source"
					return !item.folder && !!item.sourceURL;
				},
				execute: function(item) {
					$.post('/download/refresh?itemId=' + item.id).done(function() {
						NIMBUS.navigation.refreshItems(false);
					});
				}
			}, {
				name: 'download-done',
				icon: 'check',
				caption: 'ActionDownloadDone',
				accept: function(item, extension) {
					// Fonction dispo pour les fichiers téléchargés dont le statut a changé
					return !item.folder && !!item.sourceURL && !!item.status;
				},
				execute: function(item) {
					$.post('/download/done?itemId=' + item.id).done(function() {
						NIMBUS.navigation.refreshItems(false);
					});
				}
			}, {
				name: 'locate',
				icon: 'location_searching',
				caption: 'ActionLocate',
				accept: function(item, extension) {
					// La fonction "Localiser" n'est disponible que pendant une recherche récursive
					return $('#search-option-recursive').is('.active') && !!$('#search-input').val();
				},
				execute: function(item) {
					// item.path is '' or 'pid,' or 'pid1,pid2,' ...
					var pathArray = item.path.split(',');
					// remove last element which is ''
					pathArray.pop();
					// open path in new window
					window.open('/nav/' + pathArray.join('/'));
				}
			}, {
				name: 'rename',
				icon: 'edit',
				caption: 'ActionRename',
				accept: function(item, extension) {
					return true;
				},
				execute: function(item) {
					$('#rename-dialog').data('item', item).modal();
				}
			}, {
				name: 'refresh',
				icon: 'restore_page',
				caption: 'ActionRefresh',
				accept: function(item, extension) {
					return true;
				},
				execute: function(item) {
					$.post('/items/refresh?itemId=' + item.id).done(function() {
						NIMBUS.navigation.refreshItems(false);
					});
				}
			}, {
				name: 'share',
				icon: 'share',
				caption: 'ActionShare',
				accept: function(item, extension) {
					// Le création de partage n'est dispo que pour les fichiers pas encore partagés
					return !item.folder && !item.sharedPassword;
				},
				execute: function(item) {
					$('#share-dialog').data('item', item).modal();
				}
			}, {
				name: 'share-update',
				icon: 'share',
				caption: 'ActionShareUpdate',
				accept: function(item, extension) {
					// Le modification d'un partage n'est dispo que pour les fichiers déjà partagés
					return !item.folder && !!item.sharedPassword;
				},
				execute: function(item) {
					$('#share-dialog').data('item', item).modal();
				}
			}, {
				name: 'duplicate',
				icon: 'add_to_photos',
				caption: 'ActionDuplicate',
				accept: function(item, extension) {
					return true;
				},
				execute: function(item) {
					$.post('/items/duplicate', {
						itemId: item.id,
						firstPattern: NIMBUS.translate('CommonDuplicateFirst'),
						nextPattern: NIMBUS.translate('CommonDuplicateNext')
					}).fail(function(error) {
						if (error.status === 507) // Insufficient Storage
							NIMBUS.message(NIMBUS.translate('CommonDuplicateInsufficientStorage'), true);
					}).done(function() {
						NIMBUS.navigation.refreshItems(false);
					});
				}
			}, {
				name: 'move',
				icon: 'redo',
				caption: 'ActionMove',
				accept: function(item, extension) {
					return true;
				},
				execute: function(item) {
					NIMBUS.navigation.moveItems([item.id]);
				}
			}, {
				name: 'delete',
				icon: 'delete',
				caption: 'ActionDelete',
				accept: function(item, extension) {
					return true;
				},
				execute: function(item) {
					NIMBUS.navigation.deleteItems([item.id]);
				}
			}, {
				name: 'download',
				icon: 'cloud_download',
				caption: 'ActionDownload',
				accept: function(item, extension) {
					return true;
				},
				execute: function(item) {
					NIMBUS.navigation.downloadItems([item.id], item.folder);
				}
			}
		]
	});
})();
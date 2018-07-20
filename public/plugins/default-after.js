(function() {
	NIMBUS.plugins.add({
		name: 'default-after',
		facets: [
			{
				name: 'file',
				accept: function(item, extension) {
					return !item.folder;
				},
				image: function(item, thumbnail) {
					return '<i class="material-icons">insert_drive_file</i>';
				},
				describe: function(item) {
					var length = NIMBUS.formatLength(item.length);
					var mimetype = item.mimetype || NIMBUS.translate('CommonFileUnknownMimeType');
					return NIMBUS.translate('CommonFileDescription', [length, mimetype]);
				},
				open: function(item) {
					window.open('/files/stream/' + item.id);
				}
			}
		],
		actions: [
			{
				name: 'open',
				icon: 'open_in_new',
				caption: 'ActionOpen',
				accept: function(item, extension) {
					// L'ouverture n'est disponible que pour les fichiers
					return !item.folder;
				},
				url: function(item) {
					return '/files/stream/' + item.id;
				},
				execute: function(item) {
					var extension = item.folder ? '' : item.name.substring(item.name.lastIndexOf('.') + 1).toLowerCase();
					var facet = NIMBUS.plugins.facets.find(function(facet) {
						return facet.accept(item, extension);
					});
					if (facet)
						facet.open(item);
					else
						window.open('/files/stream/' + item.id);
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
					window.open('/main.html#' + pathArray.join(','));
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
				icon: 'dns',
				caption: 'ActionDuplicate',
				accept: function(item, extension) {
					return true;
				},
				execute: function(item) {
					var i = 2;
					var duplicate = function(name) {
						return $.post('/items/duplicate', {
							itemId: item.id,
							name: name
						}).fail(function(error) {
							if (error.status === 409) // Conflict
								return duplicate(NIMBUS.translate('CommonDuplicateNext', i++, item.name));
						}).done(function() {
							NIMBUS.navigation.refreshItems(false);
						});
					};
					duplicate(NIMBUS.translate('CommonDuplicateFirst', item.name));
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
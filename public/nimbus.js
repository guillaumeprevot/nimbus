var NIMBUS = (function() {
	"use strict";

	var NIMBUS = {};

	NIMBUS.message = function(text, isError) {
		return $('<div class="alert" />')
			.toggleClass('alert-danger', isError)
			.toggleClass('alert-primary', !isError)
			.text(text)
			.appendTo('#alert-container');
	},

	// (key) ou (key, p1, p2, ...) ou (key, [p1, p2, ...])
	NIMBUS.translate = function(key) {
		var text = (key in NIMBUS.lang) ? NIMBUS.lang[key] : key;
		if (arguments.length > 1) {
			var params = $.isArray(arguments[1]) ? arguments[1] : Array.prototype.slice.call(arguments, 1, arguments.length);
			text = NIMBUS.format(text, params);
		}
		return text;
	};

	// (text) ou (text, p1, p2, ...) ou (text, [p1, p2, ...])
	NIMBUS.format = function(text) {
		if (arguments.length > 1) {
			var params = $.isArray(arguments[1]) ? arguments[1] : Array.prototype.slice.call(arguments, 1, arguments.length);
			for (var i = 0; i < params.length; i++) {
				text = text.replace('{' + i + '}', params[i]);
			}
		}
		return text;
	};

	// Formatte une date exprimée en milliseconds depuis epoch avec partie date et partie heure
	NIMBUS.formatDatetime = function(ms) {
		return NIMBUS.lang.formatDatetime(new Date(ms));
	};

	// Formatte un booléen en affichant une image quand le booléen en vrai et rien sinon
	NIMBUS.formatBoolean = function(test, icon) {
		return test ? ('<i class="material-icons">' + icon + '</i>') : '';
	};

	// Formatte un nombre en entier si celui-ci est différent de 0
	NIMBUS.formatInteger = function(value) {
		return value ? value.toFixed(0) : '';
	};

	// Formatte une taille de fichier exprimée en octets en un texte "lisible"
	NIMBUS.formatLength = function(length) {
		if (typeof length !== 'number')
			return NIMBUS.translate('CommonFolder');
		if (length === 0)
			return NIMBUS.translate('CommonFileLength0');
		if (length < 1024)
			return NIMBUS.translate('CommonFileLengthB', length);
		length = length / 1024;
		if (length < 1024)
			return NIMBUS.translate('CommonFileLengthKB', length.toFixed(1));
		length = length / 1024;
		if (length < 1024)
			return NIMBUS.translate('CommonFileLengthMB', length.toFixed(1));
		length = length / 1024;
		return NIMBUS.translate('CommonFileLengthGB', length.toFixed(1));
	};

	// Propriétés disponibles dans la grille
	NIMBUS.getProperties = function() {
		return [
			{ name: 'folder', caption: 'CommonPropertyFolder', align: 'center', sortBy: 'folder', format: (i) => NIMBUS.formatBoolean(i.folder, 'folder') },
			{ name: 'length', caption: 'CommonPropertyLength', align: 'right', width: 100, sortBy: 'content.length', format: (i) => NIMBUS.formatLength(i.length) },
			{ name: 'createDate', caption: 'CommonPropertyCreateDate', align: 'right', width: NIMBUS.translate('CommonDateTimeColumnWidth'), sortBy: 'createDate', format: (i) => NIMBUS.formatDatetime(i.createDate) },
			{ name: 'updateDate', caption: 'CommonPropertyUpdateDate', align: 'right', width: NIMBUS.translate('CommonDateTimeColumnWidth'), sortBy: 'updateDate', format: (i) => NIMBUS.formatDatetime(i.updateDate) },
			{ name: 'tags', caption: 'CommonPropertyTags', format: (i) => i.tags },
			{ name: 'description', caption: 'CommonPropertyDescription', format: (i) => i.description },
			{ name: 'itemCount', caption: 'CommonPropertyItemCount', align: 'right', sortBy: 'content.itemCount', format: (i) => NIMBUS.formatInteger(i.itemCount) },
			{ name: 'iconURL', caption: 'CommonPropertyIconURL', sortBy: 'content.iconURL', format: (i) => i.iconURL || '' },
			{ name: 'mimetype', caption: 'CommonPropertyMimetype', width: 120, format: (i) => i.mimetype || '' },
		];
	};

	// Function d'initialisation de la page
	NIMBUS.init = function(callback) {
		// Attendre le chargement de la page
		$(function() {
			// Fermeture auto des "alertes" en cliquant dessus
			$('body').on('click', '.alert', function(event) {
				$(event.target).closest('.alert').slideUp(function() {
					$(this).remove();
				});
			});

			// Traduction de la page
			$('[data-translate]').each(function(i, e) {
				var properties = e.getAttribute('data-translate').split(' ');
				for (var i = 0; i  < properties.length; i++) {
					if (properties[i] === 'text')
						e.textContent = NIMBUS.translate(e.textContent);
					if (properties[i] === 'title')
						e.setAttribute('title', NIMBUS.translate(e.getAttribute('title')));
					if (properties[i] === 'label')
						e.setAttribute('label', NIMBUS.translate(e.getAttribute('label')));
					if (properties[i] === 'placeholder')
						e.setAttribute('placeholder', NIMBUS.translate(e.getAttribute('placeholder')));
				}
				e.removeAttribute('data-translate');
			});

			// Finalisation spécifique à la page en cours
			callback();
		});
	};

	return NIMBUS;
})();

NIMBUS.navigation = (function() {
	//Le div dans lequel on affiche les éléments
	var container = $('#items > tbody');
	// Le chemin actuel dans l'arborescence
	var currentPath = [];
	// Conserve le nombre d'éléments actuellement sélectionnés (plus rapide que de compter les row.active)
	var currentSelectionCount = 0;
	// indique la propriété du tri en cours
	var currentSortBy = '';
	// Indique si le tri est ascendant ou descendant
	var currentSortAscending = true;

	/** Initialiser le comportement pour l'ajout de dossier */
	function prepareAddFolder() {
		// Récupérer les composants concernés
		var dialog = $('#add-folder-dialog');
		var input = $('#add-folder-name');
		var validateButton = $('#add-folder-validate-button');
		// Initialiser le statut de la fenêtre à l'ouverture
		dialog.on('show.bs.modal', function() {
			input.val('').removeClass('is-invalid');
			validateButton.prop('disabled', true);
		});
		// Désactiver le bouton de validation quand le nom est vide
		input.on('input', function() {
			validateButton.prop('disabled', input.val().trim().length == 0);
		});
		// Validation de la fenêtre
		validateButton.click(function() {
			$.post('/items/add/folder', {
				parentId: getCurrentPathId(),
				name: input.val()
			}).fail(function() {
				input.addClass('is-invalid');
			}).done(function() {
				refreshItems(false);
				dialog.modal('hide');
			});
		});
	}

	/** Initialiser le comportement pour l'upload de fichier(s) */
	function prepareFileUpload() {
		// Ajout de fichier local en utilisant le plugin jquery fileupload
		$('#add-file-input').hide().fileupload({
			url : '/files/upload',
			// method: 'POST',
			// dropSelector: document,
			// abortOnEscape: true,
			extraParams: function(files) {
				return { parentId: getCurrentPathId() };
			},
			onstart: function(files) {
				// Mettre de côté l'id de l'élément dans lequel les fichiers seront ajoutés
				var parentId = getCurrentPathId();
				// Concaténer les noms de fichiers en une chaine
				var names = Array.prototype.map.apply(files, [function(file) {
					return file.name;
				}]);
				// On retournera une "promise" qui sera rejettée si des fichiers existent déjà avec ce nom et que l'utilisateur ne souhaite pas les écraser.
				var defer = $.Deferred();
				// Demander au serveur si l'un de ces fichiers existe déjà
				$.get('/items/exists', {
					parentId: parentId,
					names: names
				}).done(function(r) {
					// Si c'est le cas, demander confirmer avant d'écraser
					var ok = r === 'false' || window.confirm(NIMBUS.translate('MainUploadFileOverrideMessage'));
					if (ok) {
						// Si confirmation inutile ou confirmation acceptée, l'upload va commencer, on affiche la progression
						$('#progress').css('display', 'flex')
							.find('>div>div').removeClass('progress-bar-striped progress-bar-animated');
						defer.resolve();
					} else {
						// Si confirmation refusée, on stoppe l'upload
						defer.reject();
					}
				});
				return defer;
			},
			onprogress: function(files, total, duration, loaded, percent) {
				$('#progress > div > div').css('width', percent + '%').toggleClass('progress-bar-striped progress-bar-animated', percent === 100);
				$('#progress > span').text(percent + ' %');
			},
			onerror: function(files) {
				if (files.length === 1)
					NIMBUS.message(NIMBUS.translate('MainUploadFileErrorSingleFile', files[0].name), true);
				else
					NIMBUS.message(NIMBUS.translate('MainUploadFileErrorMultipleFiles', files.length), true);
			},
			onstop: function(files, total, duration) {
				$('#progress').css('display', 'none');
				$('#progress > div > div').css('width', '0%');
				$('#progress > span').text('');
				refreshItems(false);
			}
		});
	}

	/** Initialiser le comportement pour l'ajout de fichier texte vide */
	function prepareTouchFile() {
		// Récupérer les composants concernés
		var dialog = $('#touch-file-dialog');
		var input = $('#touch-file-name');
		var validateButton = $('#touch-file-validate-button');
		// Initialiser le statut de la fenêtre à l'ouverture
		dialog.on('show.bs.modal', function() {
			input.val('').removeClass('is-invalid');
			validateButton.prop('disabled', true);
		});
		// Désactiver le bouton de validation quand le nom est vide
		input.on('input', function() {
			validateButton.prop('disabled', input.val().trim().length == 0);
		});
		// Validation de la fenêtre
		validateButton.click(function() {
			$.post('/files/touch', {
				parentId: getCurrentPathId(),
				name: input.val()
			}).fail(function() {
				input.addClass('is-invalid');
			}).done(function(idString) {
				refreshItems(false);
				dialog.modal('hide');
			});
		});
	}

	/** Afficher le nombre d'élément dans la corbeille */
	function updateTrashMenu(count) {
		var n = (typeof count === 'number') ? count : parseInt(count),
			span = $('#trash-menu-entry').children(':last-child');
		if (n === 0)
			span.text(NIMBUS.translate('MainToolbarTrashEmpty'));
		else if (n === 1)
			span.text(NIMBUS.translate('MainToolbarTrashOneItem'));
		else
			span.text(NIMBUS.translate('MainToolbarTrashMultipleItem', n));
	}

	/** Afficher le quota dans le menu */
	function updateUsageMenu(usedSpace, freeSpace) {
		var usedPct = Math.round(usedSpace * 100.0 / (usedSpace + freeSpace));
		var freePct = Math.round(freeSpace * 100.0 / (usedSpace + freeSpace));
		$('#usage-menu-entry').children()
			.first()
				.css('width', usedPct + '%')
				.text(usedPct >= 20 ? NIMBUS.translate('MainToolbarUsageUsed', NIMBUS.formatLength(usedSpace)) : '')
			.next()
				.css('width', freePct + '%')
				.text(freePct >= 20 ? NIMBUS.translate('MainToolbarUsageFree', NIMBUS.formatLength(freeSpace)) : '');
	}

	/** Préparer la zone de choix du thème */
	function prepareThemeMenu(theme) {
		$('#theme-selector').on('click', '[data-theme]', function(event) {
			$.get('/preferences/theme', { theme: $(event.target).closest('button').attr('data-theme') }).then(function() {
				window.location.reload();
			});
		}).find('[data-theme="' + theme + '"]').removeClass('btn-secondary').addClass('btn-primary');
	}

	/** Obtenir l'id du dossier actuellement affiché */
	function getCurrentPathId() {
		return currentPath.length === 0 ? null : currentPath[currentPath.length - 1].id;
	}

	/** Au chargement de la page, se positionner sur le dossier inidqué par "location.hash" */
	function goToLocationHashAndRefreshItems() {
		var hash = location.hash;
		var ids = (hash && hash != '#') ? hash.substring(1).split(',') : [];
		getItemsByIds(ids, function(items) {
			// On change le chemin en ne gardant qu'un élément, la racine et en ajoutant "items"
			updatePath(0, items);
		});
	}

	/** L'utilisateur clique sur le bouton "Home" */
	function goToHomeAndRefreshItems(event) {
		event.preventDefault();
		if (currentPath.length === 0)
			return;
		var newPathLength;
		if ($('#path > :not(:first-child)').is(':visible')) {
			// fil d'ariane complet, "Home" amène à la racine
			newPathLength = 0;
		} else {
			// fil d'ariane non affiché, "Home" remonte d'un niveau
			newPathLength = currentPath.length - 1;
		}
		updatePath(newPathLength, []);
	}

	/** L'utilisateur clique sur l'un des éléménts du fil d'ariane pour remonter à un niveau en particulier */
	function goToPathAndRefreshItems(event) {
		event.preventDefault();
		updatePath($(event.target).closest('li').index(), []);
	}

	/** Mettre à jour "currentPath" pour se positionner sur le dossier "folder" */
	function goToFolderAndRefreshItems(folder) {
		// item.path is '' or 'pid,' or 'pid1,pid2,' ...
		var pathArray = folder.path.split(',');
		// remove last element which is ''
		pathArray.pop();
		// get first index to load from server
		var index = 0;
		while ((index < currentPath.length) && (index < pathArray.length) && (currentPath[index].id === parseInt(pathArray[index]))) {
			 index++;
		}
		// get path et show it to user
		getItemsByIds(pathArray.splice(index), function(items) {
			updatePath(index, items.concat([folder]));
		});
	}

	/** Mettre à jour "currentPath" comme demandé, en conservant "keepCount" dossier et en ajoutant "appendItems" */
	function updatePath(keepCount, appendItems) {
		currentPath.length = keepCount;
		appendItems.forEach(function(item) {
			currentPath.push(item);
		});
		$('#search-input').val('');
		$('#search-clear').addClass('nimbus-hidden');
		refreshItems(true);
	}

	/** Charger de manière asynchron un ensemble d'éléments (ids) et appeler une callback une fois terminé */
	function getItemsByIds(ids, callback) {
		var items = [];
		var loop = function() {
			if (ids.length == 0)
				callback(items);
			else
				$.get('/items/info/' + ids[0]).done(function(data) {
					items.push(data);
					ids.shift();
					loop();
				});
		};
		loop();
	}

	/** Préparer la zone de recherche */
	function prepareSearch() {
		// Le texte de la zone de recherche change
		$('#search-input').change(function() {
			var self = $(this);
			// Raffraichir la liste des éléments
			refreshItems(false);
			// Sélectionner le texte de l'input pour faciliter une seconde recherche
			self.select();
			// Ajuster la visibilité du bouton vidant la recherche
			$('#search-clear').toggleClass('nimbus-hidden', self.val() === '');
		});
		// Le bouton pour vider la recherche
		$('#search-clear').toggleClass('nimbus-hidden', !$('#search-input').val()).click(function() {
			// Vider la rechercher et lancer "change" pour raffraichir la liste des éléments
			$('#search-input').val('').change();
		});
		// Choix des options de recherche
		$('#search-group').on('click', '.dropdown-menu > a', function(event) {
			// Inverser la sélection sur l'option cliquée
			$(event.target).closest('a').toggleClass('active')
				.blur(); // pb sous les thèmes autres que celui par défaut. L'entrée garde le focus sinon
			// Raffraichir la liste des éléments
			refreshItems(false);
			// Ne pas fermer le menu
			return false;
		});
	}

	/** Mettre à jour le nombre d'éléments sélectionnés et le statut de la barre d'outils */
	function updateSelectionCount(value) {
		if (currentSelectionCount == 0 && value > 0)
			$('#all-selected-checkbox').prop('checked', false);
		currentSelectionCount = value;
		$(document.body).toggleClass('nimbus-selecting', currentSelectionCount > 0);
	}

	/** Inverser la sélection d'une ligne */
	function toggleSelection(row) {
		row.toggleClass('active');
		updateSelectionCount(currentSelectionCount + (row.hasClass('active') ? 1 : -1));
	}

	/** Tout désélectionner */
	function clearSelection() {
		$('#items tbody tr.active').removeClass('active');
		updateSelectionCount(0);
	}

	/** Récupérer via callback les ids des éléments sélectionnées */
	function getSelectedItemIds(doneCallback) {
		var folder = false;
		var itemIds = [];
		$('#items tbody tr.active').each(function(i, row) {
			var item = $(row).data('item');
			if (item.folder)
				folder = true;
			itemIds.push(item.id);
		});
		doneCallback(itemIds, folder);
	}

	/** Trier les éléments quand l'utilisateur clique sur l'en-tête d'une colonne*/
	function sortItems(event) {
		var th = $(event.target).closest('th'); // this is a "th"
		if (!currentSortBy || currentSortBy != th.attr('data-sort')) {
			// Change sorted column
			currentSortBy = th.attr('data-sort');
			currentSortAscending = true;
			th.closest('tr').find('i.material-icons').remove();
			$('<i class="material-icons">keyboard_arrow_up</i>').prependTo(th);
		} else if (currentSortAscending) {
			// Change sort order but on the same column
			currentSortAscending = false;
			th.find('i.material-icons').text('keyboard_arrow_down');
		} else {
			// Cancel sort to restore default order
			currentSortBy = '';
			currentSortAscending = true;
			th.find('i.material-icons').remove();
		}
		refreshItems(false);
	}

	// Clic sur une ligne d'un élément, on navigue dedans (dossier) ou on l'ouvre (fichier)
	function clickItem(event) {
		// Prevent click on 'rename' button to be interpreted as tap on row
		if (event.target.tagName == 'BUTTON')
			return;
		var row = $(event.target).closest('tr');
		if (currentSelectionCount > 0 || event.ctrlKey) {
			// Toggle selection (1) if multi-selection has started or (2) if control is pressed when item is clicked
			toggleSelection(row);
		} else {
			// Récupérer l'élément cliqué
			var item = row.data('item');
			if (item.folder)
				// Ouvrir le dossier
				goToFolderAndRefreshItems(item);
			else
				// Action sur l'élément cliqué
				showActionsForItem(item);
		}
	}

	/** Préparer la table principale */
	function prepareTable(columns) {
		// Liste des colonnes disponibles
		$('#items-options').next().append(NIMBUS.getProperties().map(function(p) {
			return $('<a class="dropdown-item" href="#"></a>')
				.attr('data-property', p.name)
				.text(NIMBUS.translate(p.caption))
				.toggleClass('active', columns.indexOf(p.name) >= 0)
				.get(0);
		}));
		// Clic sur une des options de la grille
		$('#items-options').next().on('click', '.dropdown-item', function(event) {
			// Ne pas toucher au hash de l'URL
			event.preventDefault();
			// Inverser la sélection de l'option
			$(event.target).closest('.dropdown-item').toggleClass('active');
			// Raffraichir la grille avec les nouvelles options
			refreshItems(true);
			// Laisser le menu ouvert pour permettre de changer d'autres options facilement
			return false;
		});
		// Clic sur la checkbox tout/rien sélectionner
		$('#all-selected-checkbox').click(function() {
			if ($(this).prop('checked')) {
				// Cochée -> on sélectionne tout 
				var rows = $('#items tbody tr:not(.active)').addClass('active');
				updateSelectionCount(currentSelectionCount + rows.length);
			} else {
				// Décochée -> on désélectionne tout 
				$('#items tbody tr.active').removeClass('active');
				updateSelectionCount(0);
			}
		});
		// Clic sur les en-têtes de colonnes pour modifier le tri
		$('#items > thead').on('click', 'th[data-sort]', sortItems);
		// Clic sur le picto d'un élément (en fait la cellule du picto car plus pratique), on le sélectionne
		$('#items').on('click', 'tbody tr td.icon', function(event) {
			// Sélection
			toggleSelection($(event.target).closest('tr'));
			// Ne pas remonter sur le clic de la ligne, qui désélectionnerait du coup la ligne
			return false;
		});
		// Clic sur le bouton des actions d'un item, on ouvre une modale avec les différentes actions possibles
		$('#items').on('click', 'tbody tr td.actions button', function(event) {
			// Afficher les actions
			showActionsForItem($(event.target).closest('tr').data('item'));
			// Ne pas remonter sur le clic de la ligne, qui désélectionnerait du coup la ligne
			return false;
		});
		// Clic sur une ligne d'un élément, on navigue dedans (dossier) ou on l'ouvre (fichier)
		$('#items').on('click', 'tbody tr', clickItem);
	}

	/** Raffraichir la grille */
	function refreshItems(withHeaders) {
		// Clear previous content
		$('#items tbody').empty();
		updateSelectionCount(0);

		// Update path in toolbar && url hash
		var pathDiv = $('#path');
		var hash = '';
		pathDiv.children(':not(:first-child)').remove();
		currentPath.forEach(function(item, index) {
			hash = hash + (hash ? ',' : '#') + item.id;
			$('<li class="nav-item" />').appendTo(pathDiv)
				.append($('<a class="nav-link" />').text(item.name).attr('href', hash));
		});
		location.hash = hash;

		// Get search options
		var searchText = $('#search-input').val();
		var searchFolders = !!searchText && $('#search-option-folders').is('.active');
		var searchFiles = !!searchText && $('#search-option-files').is('.active');
		var searchExtensions = (searchFiles || !searchText) ? null : $('#search-group .active[data-extensions]').get().map(function(a) { return a.getAttribute('data-extensions'); }).join(',');
		searchFiles = searchFiles || !!searchExtensions;
		searchFolders = (searchFolders === searchFiles) ? null : searchFolders ? true : false;

		// Get items from server
		$.get('/items/list', {
			parentId: getCurrentPathId(),
			recursive: $('#search-input').val() ? $('#search-option-recursive').is('.active') : false,
			sortBy: currentSortBy || 'name',
			sortAscending: currentSortAscending,
			searchBy: null,
			searchText: searchText,
			folders: searchFolders,
			deleted: false,
			extensions: searchExtensions
		}).done(function(items) {
			$('#noitems').toggle(items.length == 0);
			$('#itemcount').text(items.length == 0 ? '' : items.length.toString());
			var optionsMenu = $('#items-options').next();
			var showItemTags = optionsMenu.children('[data-option=showItemTags]').is('.active');
			var showItemDescription = optionsMenu.children('[data-option=showItemDescription]').is('.active');
			var showItemThumbnail = optionsMenu.children('[data-option=showItemThumbnail]').is('.active');
			var properties = NIMBUS.getProperties().filter(function(p) { return optionsMenu.children('[data-property="' + p.name + '"]').is('.active'); });

			// Mise à jour des en-têtes
			if (withHeaders) {
				$('#items thead > tr > th.name').nextAll(':not(.actions)').remove();
				$(properties.map(function(p) {
					var th = $('<th />').addClass(p.name).text(NIMBUS.translate(p.caption));
					if (p.align !== 'left')
						th.css('text-align', p.align); 
					if (typeof p.width === 'number')
						th.css('width', p.width + 'px');
					if (p.sortBy)
						th.attr('data-sort', p.sortBy);
					if (p.sortBy && (p.sortBy === currentSortBy))
						$('<i class="material-icons" />').text(currentSortAscending ? 'keyboard_arrow_up' : 'keyboard_arrow_down').prependTo(th);
					return th[0];
				})).insertBefore($('#items thead > tr > th.actions'));
			}

			// Fill table
			var tableBody = $('#items tbody');
			for (var i = 0; i < items.length; i++) {
				var item = items[i];
				// 1ère colonne : icône personnalisable
				var icon = item.folder ? 'folder' : 'insert_drive_file';
				icon = '<i class="material-icons">' + icon + '</i>';
				// 2ème colonne : nom personnalisable
				var name = $('<span />').html(item.name);
				// + les tags de manière facultative
				var tags = (showItemTags && item.tags) ? $.map(item.tags.split(','), function(term) {
					return term ? (' <span class="badge badge-primary">' + term + '</span>') : '';
				}).join('') : '';
				// + la description entre parenthèses de manière facultative
				var description = showItemDescription ? (item.folder ? 'folder' : 'file') : '';
				if (description)
					description = $('<span class="description text-muted" />').html(' (' + description + ')');
				// Ensuite, les colonnes demandée
				var cells = properties.map(function(p) {
					var cell = $('<td />');
					if (p.align !== 'left')
						cell.css('text-align', p.align); 
					if (typeof p.width === 'number')
						cell.css('width', p.width + 'px');
					cell.append(p.format(item));
					return cell[0];
				});
				// Dernière colonne, le bouton pour le menu des actions
				var actionsButton = $('<button class="btn btn-link btn-sm"><i class="material-icons">format_list_bulleted</i></a>').attr('title', item.id);

				// Ajout de la ligne
				$('<tr />').data('item', item)
					.append($('<td class="icon"><i class="material-icons text-primary">check</i></td>').append(icon))
					.append($('<td class="name" />').append(tags).append(name).append(description))
					.append(cells)
					.append($('<td class="actions" />').append(actionsButton))
					.appendTo(tableBody);
			}
		});
	}

	// Clic sur les boutons "...", on affiche les actions possibles
	function showActionsForItem(item) {
		// Récupérer la fenêtre modale qui donnera la liste des actions possibles 
		var dialog = $('#actions-dialog');
		// Ouvrir la "modal" donnant la liste des actions
		dialog.modal({keyboard: true}).off('click', 'a.list-group-item').on('click', 'a.list-group-item', function(event) {
			// Ne pas toucher au hash de l'URL
			event.preventDefault();
			// Exécuter l'action demandée
			todo();
			// Et on ferme la fenêtre
			dialog.modal('hide');
		});;
	}

	/** Initialiser la page principale */
	function init(columns, trashCount, usedSpace, freeSpace, theme) {
		// Préparation de la grille
		prepareTable(columns);
		// Après chargement de la page, se positionner sur l'élément demandé
		goToLocationHashAndRefreshItems();
		// Le bouton 'home' en Ajax pour éviter un rechargement de la page
		$('#path').on('click', 'li:first-child', goToHomeAndRefreshItems);
		// Les autres boutons du fil d'ariane permettent de remonter à un niveau en particulier
		$('#path').on('click', 'li:not(:first-child)', goToPathAndRefreshItems);
		// Préparer la zone de recherche
		prepareSearch();
		// Initialiser le comportement pour l'ajout de dossier
		prepareAddFolder();
		// Initialiser le comportement pour l'upload de fichier(s)
		prepareFileUpload();
		// Initialiser le comportement pour l'ajout de fichier texte vide
		prepareTouchFile();
		// Affichage du nombre d'élément dans la corbeille
		updateTrashMenu(trashCount);
		// Affichage du quota dans le menu
		updateUsageMenu(usedSpace, freeSpace);
		// Choix du thème
		prepareThemeMenu(theme);
		// Clic sur le bouton "Supprimer", on prépare et on affiche la fenêtre
		$('#delete-button').click(function(event) { getSelectedItemIds(todo); });
		// Clic sur le bouton "Télécharger" afin de télécharger un fichier ou un zip des éléments sélectionnés
		$('#download-button').click(function(event) { getSelectedItemIds(todo); });
		// Clic sur le bouton "Déplacer", on prépare et on affiche la fenêtre
		$('#move-button').click(function(event) { getSelectedItemIds(todo); });
	}

	function todo() {
		alert('TODO');
	}

	return {
		init: init
	}
})();
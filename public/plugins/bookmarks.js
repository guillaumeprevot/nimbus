(function() {

	/** Cette classe représente un favori */
	function Bookmark(data) {
		// URL pointée par ce favori
		this.url = data.url;
		// Nom de ce favori
		this.name = data.name;
		// URL de l'icone, par exemple "http://" ou "data:"
		this.iconURL = data.iconURL;
		// Description longue, slogan, ...
		this.description = data.description;
		// Liste de mots-clefs séparés par ","
		this.keywords = data.keywords;
		// Liste des sous-liens, un tableau de BookmarkExtension
		this.extensions = data.extensions || [];
	}

	/** Cette classe représente un sous-lien d'un favori */
	function BookmarkExtension(data) {
		// URL pointée par ce sous-lien
		this.url = data.url;
		// Nom de ce sous-lien
		this.name = data.name;
	}

	/** Cette classe représente un dossier de favoris */
	function BookmarkFolder(data) {
		// Nom de l'icone parmi ceux de Material Icons
		this.icon = data.icon;
		// Nom du dossier
		this.name = data.name;
		// Liste des favoris dans ce dossier, un tableau de Bookmark
		this.bookmarks = data.bookmarks || [];
	}

	BookmarkFolder.prototype.sort = function() {
		this.bookmarks.sort((b1, b2) => (b1.name || b1.url || '').localeCompare(b2.name || b2.url || ''));
		this.bookmarks.filter((b) => b.extensions.length > 1).forEach(function(b) {
			b.extensions.sort((e1, e2) => (e1.name || e1.url || '').localeCompare(e2.name || e2.url || ''));
		});
	};

	/** Cette classe représente un fichier de favoris (*.bookmarks) */
	function BookmarkSource(item) {
		// Élément de Nimbus contenant les données de cette source
		this.item = item;
		// Nom choisi par l'utilisateur pour cette source (sinon, il sera dérivé de item.name dans l'IHM)
		this.name = null; //item.name.replace(/.bookmarks/gi, '');
		// Liste des dossiers de cette source, un tableau de BookmarkFolder
		this.folders = [];
		// Indique si les favoris sont actuellement triés
		this.sorted = false;
	}

	BookmarkSource.prototype.getNameOrDefault = function() {
		return this.name || this.item.name.replace(/.bookmarks/gi, '');
	};

	BookmarkSource.prototype.getBookmarkCount = function() {
		return this.folders.reduce((n, f) => n + f.bookmarks.length, 0);
	};

	BookmarkSource.prototype.load = function() {
		var self = this;
		return $.get('/files/stream/' + self.item.id).then(undefined, function(result) {
			return { name: null, folders: [] };
		}).then(function(result) {
			self.name = result.name;
			self.folders = result.folders.map(function(f) {
				var folder = new BookmarkFolder(f);
				if (folder.bookmarks.length > 0)
					folder.bookmarks = folder.bookmarks.map(function(b) {
						var bookmark = new Bookmark(b);
						if (bookmark.extensions.length > 0)
							bookmark.extensions = bookmark.extensions.map((e) => new BookmarkExtension(e));
						return bookmark;
					});
				return folder;
			});
		});
	};

	BookmarkSource.prototype.sort = function() {
		this.folders.sort((b1, b2) => (b1.name || '').localeCompare(b2.name || ''));
		this.folders.filter((f) => f.bookmarks.length > 1).forEach((f) => f.sort());
		this.sorted = true;
	};

	BookmarkSource.prototype.save = function() {
		return NIMBUS.utils.updateFileJSON(this.item.id, true, {
			name: this.name,
			folders: this.folders.map(function(f) {
				return {
					icon: f.icon || undefined,
					name: f.name || undefined,
					bookmarks: (f.bookmarks.length === 0) ? undefined : f.bookmarks.map(function(b) {
						return {
							url: b.url || undefined,
							name: b.name || undefined,
							iconURL: b.iconURL || undefined,
							description: b.description || undefined,
							keywords: b.keywords || undefined,
							extensions: (b.extensions.length === 0) ? undefined : b.extensions.map(function(e) {
								return {
									url: e.url || undefined,
									name: e.name || undefined
								};
							})
						};
					})
				};
			})
		});
	};

	/** Cette méthode restreindra ce plugin aux fichiers dont l'extension est ".bookmarks" */
	function accept(item, extension) {
		return 'bookmarks' === extension;
	}

	/** Cette méthode vérifiera si le favori correspond au texte recherché */
	function matchBookmark(bookmark, searchTextLC, searchAllFields, searchExtensions) {
		function match(value) {
			return !!value && value.toLowerCase().includes(searchTextLC);
		}
		function matchExtensions() {
			return bookmark.extensions.some((e) => match(e.name) || (searchAllFields && match(e.url)));
		}
		function matchOtherFields() {
			return match(bookmark.url) || match(bookmark.iconURL) || match(bookmark.description) || match(bookmark.keywords);
		}
		return match(bookmark.name) || (searchAllFields && matchOtherFields()) || (searchExtensions && matchExtensions());
	}

	// Exposer l'API pour les favoris
	NIMBUS.utils.bookmarkAPI = {
		Bookmark: Bookmark,
		BookmarkExtension: BookmarkExtension,
		BookmarkFolder: BookmarkFolder,
		BookmarkSource: BookmarkSource,
		matchBookmark: matchBookmark,
		createLink: (l) => $('<a target="_blank" />').text(l.name || l.url).attr('href', l.url)
	};

	// Permettre l'ouverture des fichiers ".bookmarks" dans les différents éditeurs de texte
	NIMBUS.utils.textFileExtensions.push('bookmarks');

	NIMBUS.plugins.add({
		name: 'bookmarks',
		properties: [],
		facets: [{
			name: 'bookmarks',
			accept: accept,
			icon: 'bookmarks',
			thumbnail: null,
			describe: (item) => {
				var p = [];
				if (item.displayName)
					p.push(item.displayName);
				if (typeof item.bookmarkCount === 'number') {
					if (item.folderCount === 0)
						p.push(NIMBUS.translate('BookmarksDescription0Folder'));
					else if (item.folderCount === 1)
						p.push(NIMBUS.translate('BookmarksDescription1Folder'));
					else
						p.push(NIMBUS.translate('BookmarksDescriptionNFolders', item.folderCount));

					if (item.bookmarkCount === 0)
						p.push(NIMBUS.translate('BookmarksDescription0Bookmark'));
					else if (item.bookmarkCount === 1)
						p.push(NIMBUS.translate('BookmarksDescription1Bookmark'));
					else
						p.push(NIMBUS.translate('BookmarksDescriptionNBookmarks', item.bookmarkCount));
				}
				return p.join(', ');
			}
		}],
		actions: [{
			name: 'bookmarks-open',
			icon: 'bookmarks',
			caption: 'BookmarksOpen',
			accept: accept,
			execute: function(item) {
				window.location.assign('/bookmarks.html?' + $.param({
					itemId: item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}
		}],
		langs: {
			fr: {
				BookmarksOpen: "Ouvrir dans le gestionnaire de favoris",
				BookmarksDescription0Bookmark: "aucun favori",
				BookmarksDescription1Bookmark: "1 favori",
				BookmarksDescriptionNBookmarks: "{0} favoris",
				BookmarksDescription0Folder: "aucun dossier",
				BookmarksDescription1Folder: "1 dossier",
				BookmarksDescriptionNFolders: "{0} dossiers",
				BookmarksTitle: "Favoris",
				BookmarksSave: "Sauvegarder les modifications (Ctrl+S)",
				BookmarksSaveError: "Une erreur est survenue. Veuillez vérifier que le serveur est accessible et réessayer.",
				BookmarksSearchPlaceholder: "Rechercher des favoris par nom, url, mot-clef...",
				BookmarksSearchTitle: "Rechercher des favoris par nom, url, icône, description, mot-clef ou détails (Ctrl+F)",
				BookmarksSearchInNames: "Rechercher dans le nom affiché",
				BookmarksSearchInAllFields: "Rechercher dans tous les champs",
				BookmarksSearchForRootBookmarks: "Rechercher au premier niveau",
				BookmarksSearchForAllBookmarks: "Rechercher dans tous les favoris",
				BookmarksSaveChangesManually: "Sauvegarder manuellement",
				BookmarksSaveChangesAutomatically: "Sauvegarder automatiquement",
				BookmarksOptionsMenu: "Options",
				BookmarksAddBookmark: "Ajouter un favori",
				BookmarksRenameSourceTitle: "Renommer la source",
				BookmarksRenameSourcePrompt: "Choisir le nouveau nom de la source",
				BookmarksAddFolderTitle: "Ajouter un dossier",
				BookmarksAddFolderPrompt: "Choisir le nom du nouveau dossier",
				BookmarksSelectFolderIconTitle: "Changer l'icône du dossier",
				BookmarksSelectFolderIconPrompt: "Choisir un nom dans Materiel Icons",
				BookmarksRenameFolderTitle: "Renommer le dossier",
				BookmarksRenameFolderPrompt: "Choisir le nouveau nom du dossier",
				BookmarksDeleteFolderTitle: "Supprimer le dossier et son contenu",
				BookmarksDeleteFolderConfirmation: "Êtes-vous sûr de vouloir supprimer le dossier et son contenu ?",
				BookmarksEditBookmark: "Modifier ce favori",
				BookmarkModalTitle: "Propriétés du favori",
				BookmarkNameLabel: "Nom",
				BookmarkNamePlaceholder: "(recommandé)",
				BookmarkURLLabel: "URL",
				BookmarkURLPlaceholder: "(recommandé)",
				BookmarkIconURLLabel: "URL de l'icône",
				BookmarkIconURLPlaceholder: "(facultatif)",
				BookmarkDescriptionLabel: "Description",
				BookmarkDescriptionPlaceholder: "(facultatif. Description, slogan, ...)",
				BookmarkKeywordsLabel: "Mots-clefs",
				BookmarkKeywordsPlaceholder: "Ajouter",
				BookmarkExtensionsLabel: "Autres liens",
				BookmarkExtensionURL: "URL",
				BookmarkExtensionLabel: "Libellé",
				BookmarkExtensionAdd: "Ajouter",
				BookmarkExtensionOpen: "Ouvrir ce lien",
				BookmarkExtensionCopy: "Copier dans le presse-papier",
				BookmarkExtensionMoveToFirstPosition: "Remonter en premier",
				BookmarkExtensionMoveToPreviousPosition: "Remonter d'une ligne",
				BookmarkExtensionMoveToNextPosition: "Descendre d'une ligne",
				BookmarkExtensionMoveToLastPosition: "Descendre en dernier",
				BookmarkExtensionRemove: "Supprimer",
				BookmarkDeleteButton: "Supprimer ce favori",
				BookmarkCancelButton: "Annuler",
				BookmarkAddButton: "Créer ce favori",
				BookmarkApplyButton: "Appliquer les changements",
			},
			en: {
				BookmarksOpen: "Open in bookmark manager",
				BookmarksDescription0Bookmark: "no bookmark",
				BookmarksDescription1Bookmark: "1 bookmark",
				BookmarksDescriptionNBookmarks: "{0} bookmarks",
				BookmarksDescription0Folder: "no folder",
				BookmarksDescription1Folder: "1 folder",
				BookmarksDescriptionNFolders: "{0} folders",
				BookmarksTitle: "Bookmarks",
				BookmarksSave: "Save modifications (Ctrl+S)",
				BookmarksSaveError: "An error occurred. Please check your network access and try again.",
				BookmarksSearchPlaceholder: "Search bookmarks by name, URL, keyword...",
				BookmarksSearchTitle: "Search bookmarks by name, URL, icon, description, keyword or details (Ctrl+F)",
				BookmarksSearchInNames: "Search in names only",
				BookmarksSearchInAllFields: "Search in multiple fields",
				BookmarksSearchForRootBookmarks: "Search for root bookmarks",
				BookmarksSearchForAllBookmarks: "Search for all bookmarks",
				BookmarksSaveChangesManually: "Save changes manually",
				BookmarksSaveChangesAutomatically: "Save changes automatically",
				BookmarksOptionsMenu: "Options",
				BookmarksAddBookmark: "Add a new bookmark",
				BookmarksRenameSourceTitle: "Rename source",
				BookmarksRenameSourcePrompt: "Enter the new name of the source",
				BookmarksAddFolderTitle: "Add a new folder",
				BookmarksAddFolderPrompt: "Enter the name of the new folder",
				BookmarksSelectFolderIconTitle: "Select folder icon",
				BookmarksSelectFolderIconPrompt: "Select icon's name from Materiel Icons",
				BookmarksRenameFolderTitle: "Rename folder",
				BookmarksRenameFolderPrompt: "Enter the new name of the folder",
				BookmarksDeleteFolderTitle: "Delete this folder and all bookmark in this folder",
				BookmarksDeleteFolderConfirmation: "Are you sure you want to delete this folder and all of it's bookmarks?",
				BookmarksEditBookmark: "Edit this bookmark",
				BookmarkModalTitle: "Bookmark information",
				BookmarkNameLabel: "Name",
				BookmarkNamePlaceholder: "(recommended)",
				BookmarkURLLabel: "URL",
				BookmarkURLPlaceholder: "(recommended)",
				BookmarkIconURLLabel: "Name",
				BookmarkIconURLPlaceholder: "(optionnal)",
				BookmarkDescriptionLabel: "Description",
				BookmarkDescriptionPlaceholder: "(optionnal. Description, headline, ...)",
				BookmarkKeywordsLabel: "Keywords",
				BookmarkKeywordsPlaceholder: "Add",
				BookmarkExtensionsLabel: "Other links",
				BookmarkExtensionURL: "URL",
				BookmarkExtensionLabel: "Label",
				BookmarkExtensionAdd: "Add",
				BookmarkExtensionOpen: "Open URL",
				BookmarkExtensionCopy: "Copy URL to clipboard",
				BookmarkExtensionMoveToFirstPosition: "Move to first position",
				BookmarkExtensionMoveToPreviousPosition: "Move to previous position",
				BookmarkExtensionMoveToNextPosition: "Move to next position",
				BookmarkExtensionMoveToLastPosition: "Move to last position",
				BookmarkExtensionRemove: "Remove",
				BookmarkDeleteButton: "Delete this bookmark",
				BookmarkCancelButton: "Cancel",
				BookmarkAddButton: "Create bookmark",
				BookmarkApplyButton: "Apply modifications",
			}
		}
	});

})();
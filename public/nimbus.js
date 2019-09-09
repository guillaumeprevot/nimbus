var NIMBUS = (function() {
	"use strict";

	var NIMBUS = {};

	NIMBUS.plugins = {
		// Contiendra la liste des noms de plugins chargés 
		names: [],
		// Contiendra la liste des propriétés des éléments
		properties: [],
		// Contiendra la liste des actions possible sur les éléments
		actions: [],
		// Contiendra les différentes manières d'afficher les éléments
		facets: [],
		// Cette méthode enregistre un plugin chargé dynamiquement
		add: function(plugin) {
			// console.log('adding plugin', plugin);
			// On enregistre le nom du plugin chargé
			NIMBUS.plugins.names.push(plugin.name);
			// On enregistre les propriétés gérées par le plugin, en révitant les doublons
			// Si 2 plugins déclarent une propriété de même nom ('width' par exemple), on
			// considère que c'est une seule propriétés avec le même comportement dans les 2 cas
			if ($.isArray(plugin.properties)) {
				var names = NIMBUS.plugins.properties.map(function(p) { return p.name; });
				plugin.properties.forEach(function(p) {
					if (names.indexOf(p.name) === -1) {
						NIMBUS.plugins.properties.push(p);
						names.push(p.name);
					}
				});
			}
			if ($.isArray(plugin.facets))
				$.merge(NIMBUS.plugins.facets, plugin.facets);
			if ($.isArray(plugin.actions))
				$.merge(NIMBUS.plugins.actions, plugin.actions);
			if (plugin.langs)
				$.extend(NIMBUS.lang, plugin.langs[NIMBUS.lang['Name']] || {});
		},
		// Cette méthode charge un ensemble de plugins et retourne une promise
		load: function(plugins) {
			var defer = $.Deferred();
			var chain = defer;
			plugins.forEach(function(name) {
				chain = chain.then(function() {
					// $.getScript ne permet pas la mise en cache. On utilise $.ajax plutôt
					// Plus d'infos ici : https://api.jquery.com/jQuery.getScript/#caching-requests
					// Fonctionne sous Firefox mais pas sous Chrome car il n'envoie pas les en-tête HTTP nécessaires
					return $.ajax({
						dataType: 'script',
						cache: true,
						url: '/plugins/' + name
					});
				});
			});
			defer.resolve('Go!');
			return chain;
		}
	};

	NIMBUS.message = function(text, isError) {
		// https://getbootstrap.com/docs/4.2/components/alerts/
		return $('<div class="alert" />')
			.addClass(isError ? 'alert-danger' : 'alert-primary')
			.attr('role', isError ? 'alert' : 'status')
			.attr('aria-live', isError ? 'assertive' : 'polite')
			.text(text)
			.appendTo('#alert-container');
	};

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
		return ms ? NIMBUS.lang.formatDatetime(new Date(ms)) : '';
	};

	// Formatte un booléen en affichant une image quand le booléen en vrai et rien sinon
	NIMBUS.formatBoolean = function(test, icon) {
		return test ? ('<i class="material-icons">' + icon + '</i>') : '';
	};

	// Formatte un nombre en entier si celui-ci est différent de 0
	NIMBUS.formatInteger = function(value, suffix) {
		return (typeof value === 'number') ? (value.toFixed(0) + (suffix || '')) : '';
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

	// Formatte une durée exprimée en secondes
	NIMBUS.formatDuration = function(seconds) {
		if (typeof seconds !== 'number' || Number.isNaN(seconds))
			return '?';
		var h = Math.trunc(seconds / 3600);
		var m = Math.trunc(seconds / 60 % 60);
		var s = Math.round(seconds) % 60;
		return (h > 0 ? h + ":" : '') + (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
	}

	// Function d'initialisation de la page
	NIMBUS.init = function(plugins, callback) {
		// Chargement des plugins demandés
		function loadPlugins() {
			return NIMBUS.plugins.load(plugins);
		}

		// Traduction de la page
		function translatePage() {
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
			return $.Deferred().resolve();
		}

		// Modification offscreen à effectuer en dernier
		function finishOffscreen() {
			// Fermeture auto des "alertes" en cliquant dessus
			$('body').on('click', '.alert', function(event) {
				$(event.target).closest('.alert').slideUp(function() {
					$(this).remove();
				});
			});

			// Gestion auto du focus dans les boîtes modales de Bootstrap
			if ($.fn.autofocusModal)
				$('.modal').autofocusModal();

			// Validation auto en appuyent sur "entrée" dans les boîtes modales de Bootstrap
			if ($.fn.autovalidateModal)
				$('.modal-body').autovalidateModal();

			// Ajout d'un bouton "Back to top" en fils de document.body
			if ($.fn.backToTop) {
				$(document.body).backToTop({
					title: NIMBUS.translate('CommonBackToTop'),
					contentHTML: '<i class="material-icons">keyboard_arrow_up</i>'
				});
			}
		}

		// Attendre le chargement de la page
		$(function() {
			loadPlugins()
				.then(translatePage)
				.then(callback) // Finalisation spécifique à la page en cours
				.then(finishOffscreen);
		});
	};

	return NIMBUS;
})();

NIMBUS.utils = (function() {
	// Configuration des extensions configurées comme étant des fichiers texte
	function isTextFile(item, extension) {
		return !item.folder && (item.mimetype.indexOf("text/") === 0 || NIMBUS.utils.textFileExtensions.indexOf(extension) >= 0);
	}

	// Détection du support audio
	// - MDN : https://developer.mozilla.org/en-US/docs/Web/HTML/Supported_media_formats
	// - Modernizr : https://github.com/Modernizr/Modernizr/blob/master/feature-detects/audio.js
	// - W3Schools : https://www.w3schools.com/tags/av_met_canplaytype.asp
	// - Fichiers de test : https://hpr.dogphilosophy.net/test/
	var audioElement = document.createElement("audio");
	function isBrowserSupportedAudio(item, extension) {
		return !item.folder && (extension === 'ogg' && !!audioElement.canPlayType('audio/ogg; codecs="vorbis"')
			|| item.mimetype.indexOf("audio/") === 0 && !!audioElement.canPlayType(item.mimetype)); 
	}

	// Détection du support video
	// - MDN : https://developer.mozilla.org/en-US/docs/Web/HTML/Supported_media_formats
	// - Modernizr : https://github.com/Modernizr/Modernizr/blob/master/feature-detects/video.js
	// - W3Schools : https://www.w3schools.com/tags/av_met_canplaytype.asp
	// - Fichiers de test : http://samplephotovideo.com/ ou https://www.sample-videos.com/ 
	var videoElement = document.createElement("video");
	function isBrowserSupportedVideo(item, extension) {
		return !item.folder && item.mimetype.indexOf('video/') === 0 && !!videoElement.canPlayType(item.mimetype); 
	}

	// Détection du support des images
	// - MDN : https://developer.mozilla.org/en-US/docs/Web/HTML/Element/Img
	// - Modernizr : https://github.com/Modernizr/Modernizr/tree/master/feature-detects/img
	// - W3Schools : https://www.w3schools.com/tags/tag_img.asp
	// Pour les fichiers image, il faut un peu de code. Après test (10/2018) :
	// - Chrome accepte bmp, gif, ico, jpg, png, svg et webp MAIS pas tiff
	// - Firefox accepte bmp, gif, ico, jpg, png, svg MAIS ni tiff ni webp
	// - Edge accepte bmp, gif, ico, jpg, png, svg, webp MAIS pas tiff
	// Du coup, plutôt que tout tester :
	// - on considère quelques extensions comme OK (bmp, gif, ico, jpg, png)
	// - on considère tiff comme pas bon 
	// - on utilise un test pour "svg" qui prend une ligne (idée trouvée chez Modernizr)
	// - on utilise un test pour "webp" qui pourra être généralisé
	var imageExtensions = ['bmp', 'gif', 'ico', 'jpg', 'jpeg', 'png'/*, 'svg', 'tif', 'tiff', 'webp'*/];
	if (document.implementation.hasFeature('https://www.w3.org/TR/SVG11/feature#Image', '1.1'))
		imageExtensions.push('svg');
	function testImage(extensions, dataURL) {
		// Une méthode alternative existe en utilisant "fetch" et "createImageBitmap" mais Edge ne connait pas la seconde
		// https://ourcodeworld.com/articles/read/630/how-to-detect-if-the-webp-image-format-is-supported-in-the-browser-with-javascript
		var img = new Image();
		img.onload = function () {
			if (img.width === 1 && img.height === 1)
				imageExtensions.push.apply(imageExtensions, extensions);
		};
		img.src = dataURL;
	}
	// Image maison contenant un pixel rouge
	//testImage(['bmp'], 'data:image/bmp;base64,Qk1+AAAAAAAAAHoAAABsAAAAAQAAAAEAAAABABgAAAAAAAQAAAATCwAAEwsAAAAAAAAAAAAAQkdScwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAAP8A');
	//testImage(['gif'], 'data:image/gif;base64,R0lGODlhAQABAIABAP8AAP///yH+EUNyZWF0ZWQgd2l0aCBHSU1QACwAAAAAAQABAAACAkQBADs=');
	//testImage(['ico'], 'data:image/x-icon;base64,AAABAAEAAQEAAAEAIAAwAAAAFgAAACgAAAABAAAAAgAAAAEAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAP//AAAAAA==');
	//testImage(['jpg', 'jpeg'], 'data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoHBwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQMEBAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT/wgARCAABAAEDAREAAhEBAxEB/8QAFAABAAAAAAAAAAAAAAAAAAAAB//EABUBAQEAAAAAAAAAAAAAAAAAAAYI/9oADAMBAAIQAxAAAAE5C1T/AP/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAQUCf//EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQMBAT8Bf//EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQIBAT8Bf//EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEABj8Cf//EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAT8hf//aAAwDAQACAAMAAAAQ/wD/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAEDAQE/EH//xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAECAQE/EH//xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAE/EH//2Q==');
	//testImage(['png'], 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH4ggaDis3XN+ZfwAAAAxJREFUCNdj+M/AAAADAQEAGN2NsAAAAABJRU5ErkJggg==');
	//testImage(['tif', 'tiff'], 'data:image/tiff;base64,SUkqAAwAAAD/AAAAEAD+AAQAAQAAAAAAAAAAAQMAAQAAAAEAAAABAQMAAQAAAAEAAAACAQMAAwAAAOIAAAADAQMAAQAAAAEAAAAGAQMAAQAAAAIAAAANAQIAIgAAAOgAAAARAQQAAQAAAAgAAAASAQMAAQAAAAEAAAAVAQMAAQAAAAMAAAAWAQMAAQAAAEAAAAAXAQQAAQAAAAMAAAAaAQUAAQAAANIAAAAbAQUAAQAAANoAAAAcAQMAAQAAAAEAAAAoAQMAAQAAAAIAAAAAAAAA/////8EbjgP/////wRuOAwgACAAIAEQ6XHRlbXBcZGV2Y2xvdWRcaW1hZ2VccGl4ZWwudGlmZgA=');
	// Image proposée ici pour SVG : https://css-tricks.com/test-support-svg-img/
	//testImage(['svg'], 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMSIgaGVpZ2h0PSIxIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjwvc3ZnPg==');
	// Image trouvée ici pour WEBP (https://developers.google.com/speed/webp/faq)
	testImage(['webp'], 'data:image/webp;base64,UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA');
	function isBrowserSupportedImage(item, extension) {
		return !item.folder && imageExtensions.indexOf(extension) >= 0;
	}

	function getFileExtensionFromItem(item) {
		return item.folder ? '' : getFileExtensionFromString(item.name);
	}

	function getFileExtensionFromString(name) {
		return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
	}

	function getFileNameFromContentDisposition(jqXHR) {
		// inline; filename="toto.txt"
		var cd = jqXHR.getResponseHeader('Content-Disposition');
		// 16
		var i = cd.indexOf('=');
		// toto.txt
		return cd.substring(i + 2, cd.length - 1);
	}

	function uploadFile(parentId, blob, filename) {
		var formData = new FormData();
		formData.append("parentId", parentId ? parentId.toString() : "");
		formData.append("files", blob, filename);
		return $.ajax({
			method: "POST",
			url: "/files/upload",
			data: formData,
			dataType: "json",
			contentType: false,
			processData: false,
			cache: false,
			timeout: 0
		});
	}

	function updateFile(itemId, blob) {
		var formData = new FormData();
		formData.append("file", blob, "unused.filename");
		return $.ajax({
			method: "POST",
			url: "/files/update/" + itemId,
			data: formData,
			dataType: "text",
			contentType: false,
			processData: false,
			cache: false,
			timeout: 0
		});
	}

	return {
		isTextFile: isTextFile,
		textFileExtensions: [],
		isBrowserSupportedAudio: isBrowserSupportedAudio,
		isBrowserSupportedVideo: isBrowserSupportedVideo,
		isBrowserSupportedImage: isBrowserSupportedImage,
		getFileExtensionFromItem: getFileExtensionFromItem,
		getFileExtensionFromString: getFileExtensionFromString,
		getFileNameFromContentDisposition: getFileNameFromContentDisposition,
		uploadFile: uploadFile,
		updateFile: updateFile 
	};
})();

NIMBUS.navigation = (function() {
	// Le div dans lequel on affiche les éléments
	var container = $('#items > tbody');
	// Le chemin actuel dans l'arborescence
	var currentPath = [];
	// Le nombre d'éléments actuellement sélectionnés (plus rapide que de compter les row.active)
	var currentSelectionCount = 0;
	// La propriété du tri en cours
	var currentSortProperty = null;
	// L'ordre du tri en cours, ascendant=true ou descendant=false
	var currentSortAscending = true;
	// Le IntersectionObserver qui optimisera le chargement des miniatures au moment où elles deviennent visibles
	// https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserver
	var thumbnailObserver = null;

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
				var params = { parentId: getCurrentPathId() };
				for (var i = 0; i < files.length; i++) {
					params['updateDate' + i] = files[i].lastModified;
				}
				return params;
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
				$.post('/items/exists', {
					parentId: parentId,
					names: names
				}).done(function(r) {
					// Si c'est le cas, demander confirmer avant d'écraser
					var ok = r === 'false' || window.confirm(NIMBUS.translate('MainUploadFileOverrideMessage'));
					if (ok) {
						// Si confirmation inutile ou confirmation acceptée, l'upload va commencer, on affiche la progression
						$('#items').hide();
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
				$('#progress').css('display', '');
				$('#progress > div > div').css('width', '0%');
				$('#progress > span').text('');
				$('#items').show();
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
		var touchExtension;
		var touchEditor;
		// Initialiser le statut de la fenêtre à l'ouverture
		dialog.on('show.bs.modal', function() {
			input.val('').removeClass('is-invalid');
			validateButton.prop('disabled', true);
		});
		// Désactiver le bouton de validation quand le nom est vide
		input.on('input', function() {
			validateButton.prop('disabled', input.val().trim().length === 0);
		});
		// Validation de la fenêtre
		validateButton.click(function() {
			var name = input.val();
			if (touchExtension)
				name = name + '.' + touchExtension;
			$.post('/files/touch', {
				parentId: getCurrentPathId(),
				name: name
			}).fail(function() {
				input.addClass('is-invalid');
			}).done(function(idString) {
				if (touchEditor) {
					window.location.assign('/' + touchEditor + '?' + $.param({
						itemId: idString,
						fromUrl: window.location.href,
						fromTitle: $('title').text()
					}));
				} else {
					refreshItems(false);
					dialog.modal('hide');
				}
			});
		});
		$('#menu-button').next().on('click', '[data-target="#touch-file-dialog"]', function(event) {
			var entry = $(event.target).closest('[data-target]');
			touchExtension = entry.attr('data-touch-extension');
			touchEditor = entry.attr('data-touch-editor');
			if (touchExtension) {
				input.parent().addClass('input-group').find('.input-group-text').text('.' + touchExtension).parent().show();
			} else {
				input.parent().removeClass('input-group').find('.input-group-append').hide();
			}
			dialog.find('.modal-title').text(entry.children('span').text());
			dialog.modal('show');
		});
	}

	/** Initialiser le comportement pour l'ajout de fichier depuis une URL */
	function prepareAddURL() {
		// Récupérer les composants concernés
		var dialog = $('#add-url-dialog');
		var nameInput = $('#add-url-name');
		var urlInput = $('#add-url-input');
		var validateButton = $('#add-url-validate-button');
		// Initialiser le statut de la fenêtre à l'ouverture
		dialog.on('show.bs.modal', function() {
			nameInput.val('').removeClass('is-invalid');
			urlInput.val('').removeClass('is-invalid');
			validateButton.prop('disabled', true);
		});
		// Désactiver le bouton de validation quand le nom est vide
		urlInput.on('input', function() {
			validateButton.prop('disabled', urlInput.val().trim().length === 0);
		});
		// Profiter de l'auto-complétion des URLs (Youtube pour l'instant)
		urlInput.autocomplete({
			min: 22,
			bold: false,
			input: 'update',
			menu: 'close',
			query: function(term, callback) {
				$.get('/download/autocomplete', { url: term }, callback);
			},
			select: function(option) {
				nameInput.val(option.name);
			}
		});
		// Validation de la fenêtre
		validateButton.click(function() {
			$.post('/download/add', {
				parentId: getCurrentPathId(),
				url: urlInput.val(),
				name: nameInput.val()
			}).fail(function(error) {
				if (error.status === 409) // Conflict
					nameInput.addClass('is-invalid');
				else if (error.status === 507) // Insufficient Storage
					urlInput.addClass('is-invalid');
				else if (console && console.log)
					console.log(error);
			}).done(function(idString) {
				refreshItems(false);
				dialog.modal('hide');
			});
		});
	}

	/** Initialiser le comportement pour le renommage de fichier/dossier */
	function prepareRenameItem() {
		// Récupérer les composants concernés
		var dialog = $('#rename-dialog');
		var nameInput = $('#rename-name');
		var iconUrlInput = $('#rename-icon-url');
		var tagsInput = $('#rename-tags');
		var validateButton = $('#rename-validate-button');
		// Composant d'edition d'une liste de tags
		tagsInput.tagsinput({
			url: '/items/tags',
			label: tagsInput.prev(),
			inline: false
		});
		// Initialiser le statut de la fenêtre à l'ouverture
		dialog.on('show.bs.modal', function() {
			var item = dialog.data('item');
			nameInput.val(item.name).removeClass('is-invalid');
			iconUrlInput.val(item.iconURL || '').closest('.form-group').toggle(!!item.folder);
			tagsInput.val(item.tags || '').tagsinput().refreshTags();
		});
		// Désactiver le bouton de validation quand le nom est vide
		nameInput.on('input', function() {
			validateButton.prop('disabled', nameInput.val().trim().length == 0);
		});
		// Validation de la fenêtre
		validateButton.click(function() {
			var item = dialog.data('item');
			$.post('/items/rename', {
				itemId: item.id,
				name: nameInput.val(),
				iconURL: iconUrlInput.val(),
				tags: tagsInput.val()
			}).fail(function() {
				nameInput.addClass('is-invalid');
			}).done(function() {
				refreshItems(false);
				dialog.modal('hide');
			});
		});
	}

	/** Initialiser le comportement pour le partage de fichier */
	function prepareShareItem() {
		// Récupérer les composants concernés
		var dialog = $('#share-dialog');
		var durationSelect = $('#share-duration');
		var passwordInput = $('#share-password');
		var urlInput = $('#share-url');
		var removeButton = $('#share-remove-button');
		var validateButton = $('#share-validate-button');
		// Initialiser le statut de la fenêtre à l'ouverture
		dialog.on('show.bs.modal', function() {
			var item = dialog.data('item');
			if (!!item.sharedPassword) {
				durationSelect.val(item.sharedDuration);
				passwordInput.val(item.sharedPassword).parent().show();
				urlInput.val(window.location.origin + '/share/get/' + item.id + '?password=' + item.sharedPassword).parent().show();
				removeButton.show();
			} else {
				durationSelect.val('');
				passwordInput.val('').parent().hide();
				urlInput.val('').parent().hide();
				removeButton.hide();
			}
		});
		// Suppression du partage
		removeButton.click(function() {
			var item = dialog.data('item');
			$.post('/share/delete', {
				itemId: item.id
			}).done(function() {
				delete item.sharedPassword;
				delete item.sharedDuration;
				delete item.sharedDate;
				refreshItems(false);
				dialog.modal('hide');
			});
		});
		// Création/Modification du partage
		validateButton.click(function() {
			var item = dialog.data('item');
			var wasShared = !!item.sharedPassword;
			var duration = durationSelect.val() ? parseInt(durationSelect.val()) : undefined;
			$.post('/share/add', {
				itemId: item.id,
				duration: duration
			}).done(function(data) {
				item.sharedPassword = data;
				item.sharedDuration = duration;
				item.sharedDate = Date.now();
				refreshItems(false);
				if (wasShared) {
					// Update share and close dialog
					dialog.modal('hide');
				} else {
					// Create share and keep dialog opened to show URL
					passwordInput.val(item.sharedPassword).parent().show();
					urlInput.val(window.location.origin + '/share/get/' + item.id + '?password=' + item.sharedPassword).parent().show();
					removeButton.show();
				}
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
	function updateUsageMenu() {
		var usedProgress = $('#usage-menu-entry > :first-child');
		var freeProgress = $('#usage-menu-entry > :last-child');
		$('.nimbus-menu').on('show.bs.dropdown', function() {
			usedProgress.text('')/*.css('width', '100%')*/.removeClass('bg-danger bg-warning bg-primary').addClass('progress-bar-striped progress-bar-animated');
			freeProgress.text('')/*.css('width', '0')*/;
			$.get('/items/quota').then(function(result) {
				var usedPct = Math.round(result.usedSpace * 100.0 / result.maxSpace);
				var freePct = Math.round(result.freeSpace * 100.0 / result.maxSpace);
				var statusClass = usedPct > result.clientQuotaDanger ? 'bg-danger' : usedPct > result.clientQuotaWarning ? 'bg-warning' : 'bg-primary';
				usedProgress
					.css('width', usedPct + '%')
					.text(usedPct >= 20 ? NIMBUS.translate('MainToolbarUsageUsed', NIMBUS.formatLength(result.usedSpace)) : '')
					.addClass(statusClass).removeClass('progress-bar-striped progress-bar-animated');
				freeProgress
					.css('width', freePct + '%')
					.text(freePct >= 20 ? NIMBUS.translate('MainToolbarUsageFree', NIMBUS.formatLength(result.freeSpace)) : '');
			});
		});
	}

	/** Préparer la zone de choix du thème */
	function prepareThemeMenu() {
		$('.nimbus-menu [data-theme]').click(function(event) {
			var theme = $(event.target).closest('[data-theme]').attr('data-theme');
			$.get('/preferences/theme', { theme: theme }).then(function() {
				window.location.reload();
			});
		});
	}

	/** Obtenir l'id du dossier actuellement affiché */
	function getCurrentPathId() {
		return currentPath.length === 0 ? null : currentPath[currentPath.length - 1].id;
	}

	/** Au chargement de la page, se positionner sur le dossier indiqué par "location.pathname" */
	function goToLocationAndRefreshItems() {
		// "location.pathname" est de la forme "/nav/id1/id2". On retire le "/nav"
		var path = location.pathname.substring(4);
		// S'il reste plus que "/", par exemple "/id1/id2", on le transforme en tableau [id1, id2]
		var ids = (path && path != '/') ? path.substring(1).split('/') : [];
		getItemsByIds(ids, function(items) {
			// On change le chemin en ne gardant qu'un élément, la racine et en ajoutant "items"
			updatePath(0, items);
		});
		// En cas d'utilisation des bouton Précédent / Suivant, ajuster l'IHM en fonction de "location.pathname"
		window.onpopstate = function(event) {
			goToLocationAndRefreshItems();
		};
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
		// Keep the first "keepCount" folders, if asked
		currentPath.length = keepCount;
		// Append specified folders "appendItems" in path "currentPath"
		Array.prototype.push.apply(currentPath, appendItems);
		// Clear search
		$('#search-input').val('');
		$('#search-clear').addClass('nimbus-hidden');
		// Update path in toolbar
		var pathDiv = $('#path');
		var location = '/nav';
		pathDiv.children(':not(:first-child)').remove();
		currentPath.forEach(function(item, index) {
			location = location + '/' + item.id;
			$('<li class="nav-item" />').appendTo(pathDiv)
				.append($('<a class="nav-link" />').text(item.name).attr('href', location));
		});
		// Update path in browser using "pushState", if needed
		if (window.location.pathname !== location)
			history.pushState(
					{ folder: currentPath.length === 0 ? null : currentPath[currentPath.length - 1].id  },
					{ folder: currentPath.length === 0 ? 'Nimbus' : currentPath[currentPath.length - 1].name  },
					location);
		// Update table content
		refreshItems(true);
	}

	/** Charger de manière asynchrone un ensemble d'éléments (ids) et appeler une callback une fois terminé */
	function getItemsByIds(ids, callback) {
		if (ids.length === 0)
			callback([]);
		else
			$.get('/items/infos?itemIds=' + ids.join(',')).done(callback);
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

	// Supprimer un ou plusieurs éléments
	function deleteItems(itemIds) {
		$('#delete-dialog').modal();
		$('#delete-validate-button').off('click').on('click', function() {
			$.post('/trash/delete', {
				itemIds: itemIds.join(',')
			}).done(function() {
				refreshItems(false);
				$.get('/trash/count').then(updateTrashMenu);
				$('#delete-dialog').modal('hide');
			});
		});
	}

	// Télécharger si c'est un fichier unique ou récupérer un zip sinon
	function downloadItems(itemIds, hasFolder) {
		var url;
		if (itemIds.length === 1 && !hasFolder)
			// Si seul 1 fichier est sélectionné, on le télécharge directement
			url = '/files/download/' + itemIds[0];
		else
			// Sinon, on récupère un zip du (ou des) élément(s) sélectionné(s)
			url = '/items/zip?itemIds=' + itemIds.join(',');
		// Ouvrir l'URL obtenue (zip ou download)
		window.open(url);
		// Tout désélectionner
		clearSelection();
	}

	// Déplacer un ou plusieurs éléments
	function moveItems(itemIds) {
		var dialog = $('#move-dialog');
		var validateButton = dialog.find('.btn-primary').prop('disabled', true);
		var conflictSelect = dialog.find('#move-conflict').val('skip');
		var rootLI = dialog.find('.modal-body ul > li:first-child').removeClass('list-group-item-info');

		// Méthode qui charge en AJAX les sous-dossiers
		function load(ul) {
			$.get('/items/list', {
				parentId: ul.parent().attr('data-itemId'),
				folders: true,
				deleted: false
			}).done(function (data) {
				if (data.length == 0)
					return;
				ul.append(data.map(function(item) {
					var li = $('<li class="list-group-item list-group-item-action" />').attr('data-itemId', item.id);
					var div = $('<div />').text(item.name).append('<span class="badge badge-primary badge-pill">' + (item.itemCount || 0) + '</span>');
					return li.append(div)[0];
				}));
			});
		}

		// Sélection du dossier dans lequel déplacer les élémnts
		dialog.on('click', 'li:not(.list-group-item-info) > div', function(event) {
			var li = $(this).closest('li');
			// S'assurer que le parent n'est plus actif
			li.parent('ul').parent('li').removeClass('list-group-item-info');
			// S'assurer que tout le sous-arbre n'est ni actif, ni déployé
			li.parent('ul').find('li').removeClass('expanded list-group-item-info');
			// Sélection de l'élément cliqué
			li.addClass('list-group-item-info');
			// Activer le bouton de validation
			validateButton.prop('disabled', false);
			if (! li.is(rootLI)) {
				// Sélection de l'élément cliqué
				li.addClass('expanded');
				// Charger le contenu dynamiquement la première fois qu'on déploie l'entrée
				if (! li.hasClass('loaded')) {
					li.addClass('loaded');
					load($('<ul class="list-group list-group-flush" />').appendTo(li));
				}
			}
		});

		// Vider les éléments précédemment chargés
		rootLI.siblings().remove();
		// Affichage de la fenêtre de sélection
		dialog.modal();
		// Chargement des éléments à la racine
		load(rootLI.parent());

		// Connexion du bouton de validation qui déplacera les fichiers
		validateButton.off('click').on('click', function validate() {
			// Récupération de l'ID du dossier cible
			var targetId = dialog.find('li.list-group-item-info').attr('data-itemId');
			// Déplacement des éléments
			$.post('/items/move', {
				itemIds: itemIds.join(','),
				targetParentId: targetId,
				conflict: conflictSelect.val(),
				firstConflictPattern: NIMBUS.translate('MainMoveConflictFirstPattern'),
				nextConflictPattern: NIMBUS.translate('MainMoveConflictNextPattern')
			}).done(function() {
				refreshItems(false);
				$('#move-dialog').modal('hide');
			}).fail(function(result) {
				if (result.status === 409) { // Conflict
					refreshItems(false);
					$('#move-dialog').modal('hide');
				} else if (result.responseText)
					NIMBUS.message(result.responseText, true);
			});
		});
	}

	/** Trier les éléments quand l'utilisateur clique sur l'en-tête d'une colonne*/
	function sortItems(event) {
		var th = $(event.target).closest('th'); // this is a "th"
		var p = th.data('property');
		if (currentSortProperty === null || currentSortProperty !== p) {
			// Change sorted column
			currentSortProperty = p;
			currentSortAscending = true;
			th.closest('tr').find('i.material-icons').remove();
			$('<i class="material-icons">keyboard_arrow_up</i>').prependTo(th);
		} else if (currentSortAscending) {
			// Change sort order but on the same column
			currentSortAscending = false;
			th.find('i.material-icons').text('keyboard_arrow_down');
		} else {
			// Cancel sort to restore default order
			currentSortProperty = null;
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
	function prepareTable(options, autosave) {
		// Chargement des préférences d'affichage éventuellement sauvegardées localement dans le navigateur
		if (autosave)
			options = JSON.parse(localStorage.getItem('navigation') || 'null') || options;
		// Options d'affichage
		var optionsMenu = $('#items-options').next();
		$('#show-hidden-items').toggleClass('active', options.showHiddenItems);
		$('#show-item-tags').toggleClass('active', options.showItemTags);
		$('#show-item-description').toggleClass('active', options.showItemDescription);
		$('#show-item-thumbnail').toggleClass('active', options.showItemThumbnail);
		// Liste des colonnes disponibles
		optionsMenu.append(NIMBUS.plugins.properties.map(function(p) {
			return $('<a class="dropdown-item" href="#"></a>')
				.attr('data-property', p.name)
				.text(NIMBUS.translate(p.caption))
				.toggleClass('active', options.columns.indexOf(p.name) >= 0)
				.get(0);
		}));
		// Clic sur une des options de la grille
		optionsMenu.on('click', '.dropdown-item', function(event) {
			// Ne pas toucher au hash de l'URL
			event.preventDefault();
			// Inverser la sélection de l'option
			$(event.target).closest('.dropdown-item').toggleClass('active');
			// Raffraichir la grille avec les nouvelles options
			refreshItems(true);
			// Sauvegarder les préférences
			if (autosave) {
				localStorage.setItem('navigation', JSON.stringify({
					columns: optionsMenu.children('[data-property].active').map(function() { return this.dataset.property; }).get(),
					showHiddenItems: $('#show-hidden-items').is('.active'),
					showItemTags: $('#show-item-tags').is('.active'),
					showItemDescription: $('#show-item-description').is('.active'),
					showItemThumbnail: $('#show-item-thumbnail').is('.active'),
				}));
			}
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
		$('#items > thead').on('click', 'th.sortable', sortItems);
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

		// Clear previous IntersectionObserver
		thumbnailObserver.disconnect();

		// Get search options
		var searchText = $('#search-input').val();
		var searchFolders = !!searchText && $('#search-option-folders').is('.active');
		var searchFiles = !!searchText && $('#search-option-files').is('.active');
		var searchExtensions = (searchFiles || !searchText) ? null : $('#search-group .active[data-extensions]').get().map(function(a) { return a.getAttribute('data-extensions'); }).join(',');
		searchFiles = searchFiles || !!searchExtensions;
		searchFolders = (searchFolders === searchFiles) ? null : searchFolders ? true : false;

		// Prise en compte de l'option pour masquer des éléments
		var showHiddenItems = $('#show-hidden-items').is('.active') ? undefined : false;

		// Prise en compte du tri (sortBy côté MongoDB ou sortComparator côté client)
		var sortBy = (currentSortProperty && typeof currentSortProperty.sortBy === 'string') ? currentSortProperty.sortBy : 'name';
		var sortComparator = (currentSortProperty && typeof currentSortProperty.sortBy === 'function') ? currentSortProperty.sortBy : undefined;

		// Get items from server
		$.get('/items/list', {
			parentId: getCurrentPathId(),
			recursive: $('#search-input').val() ? $('#search-option-recursive').is('.active') : false,
			sortBy: sortBy,
			sortAscending: currentSortAscending,
			searchBy: null,
			searchText: searchText,
			folders: searchFolders,
			hidden: showHiddenItems,
			deleted: false,
			extensions: searchExtensions
		}).done(function(items) {
			// Prise en compte du tri (sortBy côté MongoDB ou sortComparator côté client)
			if (sortComparator) {
				items.sort(function(i1, i2) {
					if (i1.folder && !i2.folder)
						return -1;
					if (i2.folder && !i1.folder)
						return 1;
					var r = sortComparator(i1, i2);
					return currentSortAscending ? r : -r;
				});
			}
			$('#noitems').toggleClass('nimbus-hidden', items.length > 0);
			$('#itemcount').text(items.length == 0 ? '' : items.length.toString());
			var optionsMenu = $('#items-options').next();
			var showItemTags = $('#show-item-tags').is('.active');
			var showItemDescription = $('#show-item-description').is('.active');
			var showItemThumbnail = $('#show-item-thumbnail').is('.active');
			var properties = NIMBUS.plugins.properties.filter(function(p) { return optionsMenu.children('[data-property="' + p.name + '"]').is('.active'); });
			var addLengthToDescription = ! properties.some(function(p) { return p.name === 'length'; });

			// Mise à jour des en-têtes
			if (withHeaders) {
				$('#items thead > tr > th.name').nextAll(':not(.actions)').remove();
				$(properties.map(function(p) {
					var th = $('<th />').data('property', p).text(NIMBUS.translate(p.caption));
					if (p.align && (p.align !== 'left'))
						th.css('text-align', p.align); 
					if (typeof p.width === 'number')
						th.css('width', p.width + 'px');
					if (p.sortBy)
						th.addClass('sortable');
					if (p === currentSortProperty)
						$('<i class="material-icons" />').text(currentSortAscending ? 'keyboard_arrow_up' : 'keyboard_arrow_down').prependTo(th);
					return th[0];
				})).insertBefore($('#items thead > tr > th.actions'));
			}

			// Fill table
			var tableBody = $('#items tbody');
			for (var i = 0; i < items.length; i++) {
				// L'élément à afficher
				var item = items[i];
				// L'extension associée, si c'est un fichier
				var extension = NIMBUS.utils.getFileExtensionFromItem(item);
				// La facet qui gère l'affichage de l'élément. Par défaut, on tombera au moins sur "folder" ou "file" 
				var facet = NIMBUS.plugins.facets.find(function(facet) {
					return facet.accept(item, extension);
				});
				// console.log(item.id, item.name, extension, facet.name);

				// 1ère colonne : icône personnalisable
				var icon = $('<i class="material-icons">' + ((typeof facet.icon === 'function') ? facet.icon(item) : facet.icon) + '</i>');
				var thumbnail = (showItemThumbnail && typeof facet.thumbnail === 'function') ? facet.thumbnail(item) : null;
				if (thumbnail) {
					// + affichage d'une miniature vers "thumbnail", en "lazy", si la facet le demande, qui remplacera l'icône si le chargement a fonctionné
					icon.addClass('lazy').attr('data-thumbnail', thumbnail);
					thumbnailObserver.observe(icon[0]);
				}

				// 2ème colonne : nom personnalisable
				var name = $('<span />').html(item.name);
				if (item.status === 'download')
					name.addClass('text-info');
				else if (item.status === 'error')
					name.addClass('text-danger');
				else if (item.status === 'success')
					name.addClass('text-success');

				// + les tags de manière facultative
				var tags = (showItemTags && item.tags) ? $.map(item.tags.split(','), function(term) {
					return term ? (' <span class="badge badge-primary">' + term + '</span>') : '';
				}).join('') : '';

				// + la description entre parenthèses de manière facultative
				var description = '';
				if (showItemDescription) {
					description = facet.describe(item) || '';
					if (addLengthToDescription && !item.folder)
						description = description + (description ? ', ' : '') + NIMBUS.formatLength(item.length)
					if (description)
						description = $('<span class="description text-muted" />').html(' (' + description + ')');
				}

				// Ensuite, les colonnes demandée
				var cells = properties.map(function(p) {
					var cell = $('<td />');
					if (p.align && (p.align !== 'left'))
						cell.css('text-align', p.align); 
					if (typeof p.width === 'number')
						cell.css('width', p.width + 'px');
					cell.append((p.format ? p.format(item, facet) : item[p.name]) || '');
					return cell[0];
				});

				// Dernière colonne, le bouton pour le menu des actions
				var actionsButton = $('<button class="btn btn-link btn-sm"><i class="material-icons">format_list_bulleted</i></a>').attr('title', item.id);

				// Ajout de la ligne
				$('<tr />').data('item', item)
					.addClass(facet.name)
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
		// Récupérer l'extension de l'élement concerné
		var extension = NIMBUS.utils.getFileExtensionFromItem(item);
		// Récupérer la fenêtre modale qui donnera la liste des actions possibles 
		var dialog = $('#actions-dialog');
		// Récupérer le parent des entrées de menu qui vont être ajoutées 
		var list = dialog.find('.list-group');
		if (list.is(':empty')) {
			// La 1ère fois, générer la liste des actions
			list.append(NIMBUS.plugins.actions.map(function(action) {
				// <a href="#" class="list-group-item list-group-item-action" id="action-navigate"><i class="material-icons">folder_open</i> <span data-translate="text">ActionNavigate</span></a>
				return $('<a href="#" class="list-group-item list-group-item-action nimbus-hidden" />')
					.attr('id', 'action-' + action.name)
					.data('action', action)
					.append($('<i class="material-icons" />').text(action.icon))
					.append('&nbsp;')
					.append($('<span />').text(NIMBUS.translate(action.caption)))
					[0];
			}));
		} else {
			// Les fois suivantes, commencer par cacher toutes les actions
			list.children().addClass('nimbus-hidden');
		}
		// Jouer ensuite sur la visibilité des actions
		NIMBUS.plugins.actions.forEach(function(action) {
			// La méthode "accept" peut retourner un booléen ou un "thenable" (Promise, Deferred, ...)
			var accept = action.accept(item, extension);
			if (typeof accept === 'boolean')
				// Si c'est un booléen, l'IHM est réactive car on peut ajuster la visibilité dès maintenant
				list.children('#action-' + action.name).toggleClass('nimbus-hidden', !accept);
			else
				// Si c'est une Promise, elle sera résolue avec un booléen indiquant si l'élément est accepté
				accept.then(function(accepted) {
					list.children('#action-' + action.name).toggleClass('nimbus-hidden', !accepted);
				});
		});
		// Ouvrir la "modal" donnant la liste des actions
		dialog.modal({keyboard: true}).off('click', 'a.list-group-item').on('click', 'a.list-group-item', function(event) {
			var id = $(event.target).closest('.list-group-item').attr('id');
			// Ne pas toucher au hash de l'URL
			event.preventDefault();
			// Et fermer la fenêtre
			dialog.modal('hide').one('hidden.bs.modal', function() {
				// Exécuter l'action demandée après avoir attendu que la fenêtre modale listant les actions ait disparue.
				// L'action cliquée ("Renommer", "Déplacer", ...) peut ainsi gérer correctement l'apparition des scrollbars.
				$(event.target).closest('.list-group-item').data('action').execute(item);
			})
		});;
	}

	/** Initialiser la page principale */
	function init(options, trashCount) {
		// Préparation du IntersectionObserver qui optimisera le chargement des miniatures au moment où elles deviennent visibles
		thumbnailObserver = new IntersectionObserver(function(entries) {
			entries.forEach(entry => {
				var img;
				// Chargement "lazy" dès que la moitié de l'image devient visible 
				if (entry.isIntersecting && entry.intersectionRatio >= 0.5 && entry.target.classList.contains('lazy')) {
					// Pour ne pas recommencer si l'utilisateur scroll pendant le chargement
					entry.target.classList.remove('lazy');
					// Création de l'image
					img = $('<img />').attr('src', entry.target.dataset.thumbnail);
					// Affichage de l'image à la place de l'icône si le chargement fonctionne
					img.on('load', function() {
						$(entry.target).replaceWith(img);
					});
				}
			});
		}, {
			root: null, // body is scrolling
			rootMargin: '0px', // body has no margin
			threshold: 0.5 // trigger callback when at least half the image gets visible
		});

		// Préparation de la grille
		prepareTable(options, true/*autosave*/);
		// Après chargement de la page, se positionner sur l'élément demandé
		goToLocationAndRefreshItems();
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
		// Initialiser le comportement pour l'ajout de fichier depuis une URL
		prepareAddURL();
		// Initialiser le comportement pour le renommage de fichier/dossier
		prepareRenameItem();
		// Initialiser le comportement pour le partage de fichier
		prepareShareItem();
		// Affichage du nombre d'élément dans la corbeille
		updateTrashMenu(trashCount);
		// Affichage du quota dans le menu
		updateUsageMenu();
		// Choix du thème
		prepareThemeMenu();
		// Clic sur le bouton "Supprimer", on prépare et on affiche la fenêtre
		$('#delete-button').click(function(event) { getSelectedItemIds(deleteItems); });
		// Clic sur le bouton "Télécharger" afin de télécharger un fichier ou un zip des éléments sélectionnés
		$('#download-button').click(function(event) { getSelectedItemIds(downloadItems); });
		// Clic sur le bouton "Déplacer", on prépare et on affiche la fenêtre
		$('#move-button').click(function(event) { getSelectedItemIds(moveItems); });
	}

	return {
		init: init,
		refreshItems: refreshItems,
		goToFolderAndRefreshItems: goToFolderAndRefreshItems,
		moveItems: moveItems,
		deleteItems: deleteItems,
		downloadItems: downloadItems
	}
})();
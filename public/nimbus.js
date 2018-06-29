var NIMBUS = (function() {
	"use strict";

	var NIMBUS = {};

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

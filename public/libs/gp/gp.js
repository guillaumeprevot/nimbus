(function($) {
	"use strict";

	$.addPlugin = function(name, Constructor) {
		// Composant en jQuery, proposant 3 appels possibles
		$.fn[name] = function(params) {
			// 1. Création avec .name({ ... });
			if (typeof params === 'object') {
				return this.each(function() {
					$(this).data(name, new Constructor(this, params));
				});
			}

			// 2. Récupération avec .name();
			if (typeof params === 'undefined') {
				return this.data(name);
			}

			// 3. Destruction avec .xxx('destroy');
			if (params === 'destroy') {
				return this.each(function() {
					var self = $(this), component = self.data(name);
					if (component) {
						component.destroy();
						self.removeData(name);
					}
				});
			}

			// Erreur sinon
			throw new Error('Unknown parameter with type "' + (typeof params) + '" for component "' + name + '" : ' + params);
		};
	}

	/*
	 * C'est un plugin jQuery qui donnera le focus à l'input active marquée "autofocus" lors de l'ouverture de la fenêtre modale Bootstrap
	 */
	$.fn.autofocusModal = function() {
		return this.on('shown.bs.modal', function(event) {
			$(this).find('[autofocus]:not([disabled])').first().select().focus();
		});
	};

	/*
	 * C'est un plugin jQuery qui simulera le clic sur le ".btn-primary" (si unique) lorsque l'utilisateur appuie sur "Entrée" dans une fenêtre modale Bootstrap
	 */
	$.fn.autovalidateModal = function() {
		return this.on('keyup', function(event) {
			if (event.key === 'Enter') {
				var btn = $(this).closest('.modal').find('.modal-footer .btn-primary:not([disabled])');
				if (btn.length == 1)
					btn.click();
			}
		});
	};

})(jQuery);

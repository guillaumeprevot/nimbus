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

})(jQuery);
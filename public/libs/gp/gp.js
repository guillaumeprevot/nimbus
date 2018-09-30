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

	/*
	 * C'est un plugin jQuery qui écoute la manipulation au doigt et envoie des évènements "swipe", "", "" et ""
	 */
	$.fn.swipe = function() {
		return this.addClass('swipe').on('touchstart', function(startEvent) {
			var startX, startY, startTime, moveX, moveY;
			var target = $(startEvent.target).closest('.swipe');
			function touchmove(event) {
				var t = event.originalEvent.touches;
				if (t) {
					moveX = t[0].clientX;
					moveY = t[0].clientY;
				}
			}
			var t = startEvent.originalEvent.touches;
			if (t) {
				startX = t[0].clientX;
				startY = t[0].clientY;
				startTime = Date.now();
				$(document).on('touchmove', touchmove).one('touchend', function() {
					$(document).off('touchmove', touchmove);
					var radians = Math.atan2((startY - moveY), (moveX - startX));
					var directions = ['left', 'bottomleft', 'bottom', 'bottomright', 'right', 'topright', 'top', 'topleft'];
					var index = Math.floor((radians + Math.PI + Math.PI / 8) / (Math.PI / 4)) % 8;
					target.trigger('swipe', [{ direction: directions[index], duration: Date.now() - startTime }]);
					// target.trigger('swipe.' + directions[index]);
				});
			}
		});
	};

})(jQuery);

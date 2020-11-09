(function($) {
	"use strict";

	window.GP = {};

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
				if (btn.length == 1 && !btn.hasClass('noautovalidate'))
					btn.click();
			}
		});
	};

	/*
	 * C'est un plugin jQuery qui ajuste la hauteur d'une textarea automatiquement
	 */
	$.fn.autoExpandTextarea = function(collapseHeight) {
		return this.each(function() {
			var self = $(this);

			function autoExpand() {
				// Comme "scrollHeight" ne diminue pas quand on réduit le texte, le seul moyen d'avoir
				// un scrollHeight correct est de réduire la textarea pour la réajuster ensuite
				if (collapseHeight)
					self.innerHeight(collapseHeight);
				// scrollHeight (standard) donne la hauteur du contenu avec padding et ni margin, ni border
				// innerHeight (jquery) change la hauteur en incluant padding mais ni margin, ni border
				self.innerHeight(self[0].scrollHeight);
			}

			function waitForInsert() {
				if (self.parents('body').length)
					autoExpand();
				else
					setTimeout(waitForInsert, 100);
			}

			self.css('overflow-y', 'hidden').on('input', autoExpand);
			waitForInsert();
		});
	};

	/*
	 * C'est un plugin jQuery qui écoute la manipulation au doigt et envoie des évènements "swipe" en indiquant la direction et la durée du mouvement.
	 */
	$.fn.gpswipe = function() {
		return this.addClass('gp-swipe').on('touchstart', function(startEvent) {
			var startX, startY, startTime, moveX, moveY;
			var target = $(startEvent.target).closest('.gp-swipe');
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
					target.trigger('gp.swipe', [{ direction: directions[index], duration: Date.now() - startTime }]);
					// target.trigger('gp.swipe.' + directions[index]);
				});
			}
		});
	};

	$.fn.gpkeystrokes = function(options, filter) {
		// Map pour transformer les valeurs de "event.key" en nom utilisés ici
		var keyMap = {
			' ': 'Space',
			'Control': 'Ctrl',
		};
		// Retourne une chaine représentant un raccourci, de la forme '[Shift-][Meta-][Ctrl-][Alt-]key'
		function keystrokeFromInfo(key, alt, ctrl, meta, shift) {
			var result = key;
			if (key !== 'Alt' && alt) result = 'Alt-' + result;
			if (key !== 'Ctrl' && ctrl) result = 'Ctrl-' + result;
			if (key !== 'Meta' && meta) result = 'Cmd-' + result;
			if (key !== 'Shift' && shift) result = 'Shift-' + result;
			return result;
		}
		// Retourne une chaine représentant l'évènement, de la forme '[Shift-][Meta-][Ctrl-][Alt-]key'
		function keystrokeFromEvent(event) {
			var base = keyMap[event.key] || event.key;
			return keystrokeFromInfo(base, event.altKey, event.ctrlKey, event.metaKey, event.shiftKey);
		}
		// Reformate une chaine représentant un raccourci dans l'ordre Shift -> Meta -> Ctrl -> Alt -> key
		function normalize(keystroke) {
			var base = keystroke.endsWith('-') ? '-' : keystroke.substring(keystroke.lastIndexOf('-') + 1);
			var alt = keystroke.indexOf('Alt-') >= 0;
			var ctrl = keystroke.indexOf('Ctrl-') >= 0;
			var meta = keystroke.indexOf('Cmd-') >= 0;
			var shift = keystroke.indexOf('Shift-') >= 0;
			return keystrokeFromInfo(base, alt, ctrl, meta, shift);
		}
		// Génère la liste des raccourcis, en réordonnant correctement les modificateurs
		var keystrokes = {};
		for (var p in options) {
			if (options.hasOwnProperty(p)) {
				keystrokes[normalize(p)] = options[p];
			}
		}
		return this.addClass('gp-keystrokes').keydown(function(event) {
			var keystroke = keystrokeFromEvent(event);
			var callback = keystrokes[keystroke];
			if (callback && (!filter || filter(event))) {
				callback();
				return false;
			}
		});
	};
})(jQuery);

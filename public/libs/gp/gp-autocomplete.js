(function($) {
	"use strict";

	function AutoComplete(target, options) {
		this.target = $(target);
		this.options = $.extend({}, AutoComplete.defaultOptions, options);
		this.update = this.update.bind(this);
		this.select = this.select.bind(this);
		this.autoclose = this.autoclose.bind(this);
		this.target
			.wrap('<div class="dropdown gp-autocomplete" />')
			.on('input', this.update)
			.on('focus', this.update)
			.on('blur', this.autoclose);
		this.container = this.target.parent();
		this.menu = $('<div class="dropdown-menu" />')
			.appendTo(this.container)
			.css('width', '100%')
			.css('overflow', 'hidden')
			.css('text-overflow', 'ellipsis')
			.on('mousedown', 'a', this.select); // using "mousedown" to prevent "a" from taking focus
	}

	$.extend(AutoComplete.prototype, {
		destroy: function() {
			this.target
				.unwrap()
				.off('input', this.update)
				.off('focus', this.update)
				.off('blur', this.autoclose);
			this.container.remove();
		},
		update: function() {
			var self = this;
			var term = self.target.val();
			var call = {};
			self.call = call;
			if ((term !== self.options.wildcard) && self.options.min > 0 && term.length < self.options.min) {
				self.toggleMenu(false);
				return;
			}
			var searchingTimeout;
			if (self.options.searching) {
				searchingTimeout = setTimeout(function() {
					if (self.call === call) { // make sure no other search has started since the call was made
						self.toggleMenu(true);
						self.menu.empty().append($('<span class="dropdown-item"></span>').text(self.options.searching));
					}
				}, 500);
			}
			self.options.query((term === self.options.wildcard) ? '' : term, function(results) {
				if (self.call !== call)
					return; // make sure no other search has started since the call was made
				if (searchingTimeout)
					clearTimeout(searchingTimeout);
				if (results.length === 0) {
					if (self.options.noResult)
						self.menu.empty().append($('<span class="dropdown-item"></span>').text(self.options.noResult));
					self.toggleMenu(!!self.options.noResult);
				} else {
					var expr = (term === self.options.wildcard) ? null : new RegExp(term, 'gi');
					self.menu.empty().append(results.map(function(result) {
						var a = $('<a class="dropdown-item" href="#"></a>').data('result', result);
						var label = result.label;
						if (term === self.options.wildcard || !self.options.bold)
							a.text(label);
						else {
							a.html(label.replace(expr, function(s) {
								return '<b>' + s + '</b>';
							}));
						}
						return a[0];
					}));
					self.toggleMenu(true);
				}
			});
		},
		toggleMenu: function(visible) {
			this.container.toggleClass('show', visible);
			this.menu.toggleClass('show', visible);
		},
		select: function(event) {
			// Get selected value
			var a = $(event.target).closest('a');
			var result = a.data('result');

			// Alert external code
			if (this.options.select)
				this.options.select(result);

			// Update input content
			if (this.options.input === 'clear')
				this.target.val('')
			else if (this.options.input === 'update')
				this.target.val(result.value || result.label);

			// Reload is necessary
			if (this.options.menu === 'close') {
				this.menu.empty();
				this.toggleMenu(false);
			} else if (this.options.menu === 'remove') {
				a.remove();
				if (this.menu.is(':empty'))
					this.toggleMenu(false);
			} else if (this.options.menu === 'reload') {
				this.target.trigger('input');
			}
			return false;
		},
		autoclose: function(event) {
			if (event.relatedTarget && this.menu.find(event.relatedTarget).length > 0)
				return;
			this.menu.empty();
			this.toggleMenu(false);
		}
	});

	AutoComplete.defaultOptions = {
		// Nombre de caractères avant de lance la recherche
		min: 2,
		// Affiche-t-on en gras la parties des propositions correspondant à la recherche
		bold: true,
		// Un texte pour afficher tous les résultats
		wildcard: '*',
		// Que faire de l'input une fois qu'on sélectionne une proposition ? (clear / preserve / update)
		input: 'update',
		// Que faire du menu une fois qu'on sélectionne une proposition ? (close / remove / reload)
		menu: 'close',
		// Un texte optionnel qui sera affiché quand la recherche démarre
		searching: '...',
		// Un texte optionnel qui sera affiché quand la recherche le remonte aucun résultat
		noResult: '',
	};

	AutoComplete.test = function() {
		//<input id="autocomplete-test" type="text" placeholder="Rechercher une langue" />
		var options = ['Français', 'Anglais', 'Allemand', 'Turque', 'Espagnol', 'Finlandais', 'Néerlandais', 'Australien', 'Russe', 'Chinois'].map(function(l) {
			return { label: l };
		});
		var selection = [];
		new AutoComplete(document.getElementById('autocomplete-test'), {
			// min: 3,
			// bold: false,
			// wildcard: '?',
			input: 'preserve',
			menu: 'reload',
			searching: 'Recherche en cours...',
			noResult: 'Aucun résultat',
			query: function(term, callback) {
				var t = term.toLowerCase();
				callback(options.filter(function(o) { return selection.indexOf(o.label) === -1 && o.label.toLowerCase().indexOf(t) >= 0; }));
			},
			select: function(option) {
				selection.push(option.label);
			}
		});
	};

	$.addPlugin('gpautocomplete', AutoComplete);

})(jQuery);

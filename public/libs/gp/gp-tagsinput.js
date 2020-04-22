(function($) {
	"use strict";

	function TagsInput(target, options) {
		var self = this;
		this.target = $(target);
		this.options = $.extend({}, TagsInput.defaultOptions, options);
		this.internalTargetChange = this.internalTargetChange.bind(this);
		this.internalClickBadge = this.internalClickBadge.bind(this);
		this.internalKeyUp = this.internalKeyUp.bind(this);

		this.target.hide()
			.wrap('<div class="gp-tagsinput"></div>')
			.on('change', this.internalTargetChange);
		this.container = this.target.parent()
			.on('click', '.badge', this.internalClickBadge);
		this.tagInput = $('<input class="form-control" type="text" />')
			.attr('id', this.target.attr('id') + '_tag')
			.attr('placeholder', this.target.attr('placeholder'))
			.on('keyup', this.internalKeyUp)
			.insertBefore(this.target);

		// Auto-complete using either an URL, an array or a function
		var autocompleteQuery;
		if (this.options.autocompleteURL)
			autocompleteQuery = function(term) {
				return $.getJSON(self.options.autocompleteURL, { term: term });
			};
		else if (this.options.autocompleteValues)
			autocompleteQuery = function(term) {
				var results =  self.options.autocompleteValues;
				if (term)
					results = results.filter(function(t) {
						return t.toLowerCase().includes(term.toLowerCase());
					});
				return $.Deferred().resolve(results);
			};
		else if (this.options.autocompleteFunction)
			autocompleteQuery = this.options.autocompleteFunction;

		if (autocompleteQuery) {
			this.tagInput.gpautocomplete({
				min: 1,
				bold: true,
				input: 'preserve',
				menu: 'remove',
				query: function(term, callback) {
					autocompleteQuery(term).then(function(tags) {
						var value = ',' + self.target.val() + ',';
						callback($.map(tags, function(tag) {
							if (value.indexOf(',' + tag + ',') === -1)
								return { label: tag, value: tag };
						}));
					});
				},
				select: function(tag) {
					self.addTag(tag.value);
				}
			});
		}

		if (this.options.inline) {
			// Display flex en horizontal avec les mots-clef à gauche et l'input ensuite
			this.container.css({
				'display': 'flex',
				'align-items': 'center',
				'flex-wrap': 'wrap'
			});
			(autocompleteQuery ? this.tagInput.parent() : this.tagInput).css({
				'flex': 'auto',
				'min-width': '50%',
				'width': 'auto'
			}).addClass('mb-1');
		}

		if (this.options.label)
			this.options.label.attr('for', this.tagInput.attr('id'));
	}

	$.extend(TagsInput.prototype, {
		destroy: function() {
			if (this.options.label)
				this.options.label.attr('for', this.target.attr('id'));
			this.target.off('change', this.internalTargetChange).unwrap().show();
			this.container.remove();
		},
		buildTag: function(tag) {
			return $('<span class="badge badge-primary mr-1 mb-1" />')
				.text(tag)
				.data('tag', tag)
				.append('<a href="#" class="btn btn-link btn-sm text-light p-0 pb-1"><i class="material-icons material-icons-16">clear</i></a>');
		},
		refreshTags: function() {
			this.tagInput.val('').trigger('input');
			this.container.children('.badge').remove();
			this.container.prepend((this.target.val() || '').split(',').map(function(tag) {
				if (tag)
					return this.buildTag(tag)[0];
			}.bind(this)));
		},
		addTag: function(tag) {
			var tags = (this.target.val() || '').split(',').filter(function(t) { return !!t; });
			if (tags.indexOf(tag) === -1) {
				tags.push(tag);
				this.target.val(tags.join(','));
				this.refreshTags();
			}
		},
		removeTag: function(tag) {
			var tags = (this.target.val() || '').split(',').filter(function(t) { return !!t; });
			var index = tags.indexOf(tag);
			if (index >= 0) {
				tags.splice(index, 1);
				this.target.val(tags.join(','));
				this.refreshTags();
			}
		},
		internalTargetChange: function() {
			this.refreshTags();
		},
		internalClickBadge: function(event) {
			var badge = $(event.target).closest('.badge');
			this.removeTag(badge.data('tag'));
			return false;
		},
		internalKeyUp: function(event) {
			var tag;
			if (event.key === 'Enter' && (tag = this.tagInput.val().trim())) {
				this.addTag(tag);
				return false;
			}
		}
	});

	TagsInput.defaultOptions = {
		// {boolean} indiquer si les mots-clefs se positionnent à gauche de l'input si la largeur le permet
		inline: false,
		// {DOM} un <label /> optionnel dont l'attribut 'for' pourra être ajusté pour activer la zone d'ajout de tag
		label: null,
		// {Array} un tableau de mots-clefs prédéfinis avec lesquels proposer l'auto-complétion
		autocompleteValues: null,
		// {Function} une fonction prenant en paramètre le texte en cours et renvoyant une promesse d'un tableau de mots-clef
		autocompleteFunction: null,
		// {String} une URL prenant en paramètre un 'term' et renvoyant en JSON un tableau de tags
		autocompleteURL: null,
	};

	$.addPlugin('gptagsinput', TagsInput);

})(jQuery);

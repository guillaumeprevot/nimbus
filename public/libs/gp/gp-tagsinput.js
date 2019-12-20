(function($) {
	"use strict";

	function TagsInput(originalInput, options) {
		var self = this;
		this.originalInput = $(originalInput).hide();
		this.container = this.originalInput.wrap('<div class="tagsinput" />').parent();
		this.tagInput = $('<input class="form-control" type="text" />')
			.attr('id', this.originalInput.attr('id') + '_tag')
			.attr('placeholder', this.originalInput.attr('placeholder'))
			.insertBefore(this.originalInput);

		this.originalInput.on('change', function() {
			self.refreshTags();
		});

		this.container.on('click', 'a', function(event) {
			var badge = $(event.target).closest('.badge');
			self.removeTag(badge.data('tag'));
			return false;
		});

		this.tagInput.on('keyup', function(event) {
			var tag;
			if (event.key === 'Enter' && (tag = self.tagInput.val().trim())) {
				self.addTag(tag);
				return false;
			}
		});

		// Auto-complete using either an URL, an array or a function
		var autocompleteQuery;
		if (options.autocompleteURL)
			autocompleteQuery = (term) => $.getJSON(options.autocompleteURL, { term: term });
		else if (options.autocompleteValues)
			autocompleteQuery = (term) => $.Deferred().resolve(term
					? options.autocompleteValues.filter((t) => t.label.toLowerCase().includes(term.toLowerCase()))
					: options.autocompleteValues);
		else if (options.autocompleteFunction)
			autocompleteQuery = options.autocompleteFunction;

		if (autocompleteQuery) {
			this.tagInput.autocomplete({
				min: 1,
				bold: true,
				input: 'preserve',
				menu: 'remove',
				query: function(term, callback) {
					autocompleteQuery(term).then(function(tags) {
						var value = ',' + self.originalInput.val() + ',';
						callback($.map(tags, function(tag) {
							if (value.indexOf(',' + tag.value + ',') === -1)
								return tag;
						}));
					});
				},
				select: function(tag) {
					self.addTag(tag.value);
				}
			});
		}

		if (options.inline) {
			this.container.css({
				'display': 'flex',
				'align-items': 'center',
				'flex-wrap': 'wrap'
			});
			(options.url ? this.tagInput.parent() : this.tagInput).css({
				'flex': 'auto',
				'min-width': '50%',
				'width': 'auto'
			}).addClass('mb-1');
		}

		if (options.label)
			options.label.attr('for', this.tagInput.attr('id'));
	}

	$.extend(TagsInput.prototype, {
		buildTag: function(tag) {
			return $('<span class="badge badge-primary mr-1 mb-1" />')
				.text(tag)
				.data('tag', tag)
				.append('<a href="#" class="btn btn-link btn-sm text-light p-0 pb-1"><i class="material-icons material-icons-16">clear</i></a>');
		},
		refreshTags: function() {
			this.tagInput.val('').trigger('input');
			this.container.children('.badge').remove();
			this.container.prepend((this.originalInput.val() || '').split(',').map(function(tag) {
				if (tag)
					return this.buildTag(tag)[0];
			}.bind(this)));
		},
		addTag: function(tag) {
			var tags = (this.originalInput.val() || '').split(',').filter((t) => !!t);
			if (tags.indexOf(tag) === -1) {
				tags.push(tag);
				this.originalInput.val(tags.join(','));
				this.refreshTags();
			}
		},
		removeTag: function(tag) {
			var tags = (this.originalInput.val() || '').split(',').filter((t) => !!t);
			tags.splice(tags.indexOf(tag), 1);
			this.originalInput.val(tags.join(','));
			this.refreshTags();
		},
	});

	$.addPlugin('tagsinput', TagsInput);

})(jQuery);

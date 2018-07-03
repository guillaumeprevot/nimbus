(function($) {
	"use strict";

	function TagsInput(originalInput, options) {
		var self = this;
		this.originalInput = $(originalInput).hide();
		this.container = this.originalInput.wrap('<div class="tagsinput" />').parent();
		this.tagsContainer = $('<div class="mb-1" />')
			.insertBefore(this.originalInput);
		this.tagInput = $('<input class="form-control" type="text" />')
			.attr('id', this.originalInput.attr('id') + '_tag')
			.attr('placeholder', this.originalInput.attr('placeholder'))
			.insertBefore(this.originalInput);

		this.tagsContainer.on('click', 'a', function(event) {
			var badge = $(event.target).closest('.badge');
			self.removeTag(badge.data('tag'));
			return false;
		});

		this.tagInput.on('keydown', function(event) {
			if (event.key === 'Enter') {
				self.addTag(self.tagInput.val().trim());
				return false;
			}
		});

		if (options.url) {
			this.tagInput.autocomplete({
				min: 1,
				bold: true,
				input: 'preserve',
				menu: 'remove',
				query: function(term, callback) {
					$.getJSON(options.url, { term: term }, function(tags) {
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

		if (options.label)
			options.label.attr('for', this.tagInput.attr('id'));
	}

	$.extend(TagsInput.prototype, {
		buildTag: function(tag) {
			return $('<span class="badge badge-primary mr-1" />')
				.text(tag)
				.data('tag', tag)
				.append('<a href="#" class="btn btn-link btn-sm text-light p-0 pb-1"><i class="material-icons material-icons-16">clear</i></a>');
		},
		refreshTags: function() {
			this.tagInput.val('');
			this.tagsContainer.empty().append((this.originalInput.val() || '').split(',').map(function(tag) {
				if (tag)
					return this.buildTag(tag)[0];
			}.bind(this)));
		},
		addTag: function(tag) {
			var tags = (this.originalInput.val() || '').split(',');
			if (tags.indexOf(tag) === -1) {
				tags.push(tag);
				this.originalInput.val(tags.join(','));
				this.refreshTags();
			}
		},
		removeTag: function(tag) {
			var tags = (this.originalInput.val() || '').split(',');
			tags.splice(tags.indexOf(tag), 1);
			this.originalInput.val(tags.join(','));
			this.refreshTags();
		},
	});

	$.addPlugin('tagsinput', TagsInput);

})(jQuery);
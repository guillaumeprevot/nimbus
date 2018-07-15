(function($) {
	"use strict";

	function BackToTop(container, options) {
		this.options = $.extend({}, BackToTop.defaultOptions, options);
		this.scroll = this.scroll.bind(this);
		this.click = this.click.bind(this);
		// The new button
		this.button = $('<button type="button" class="btn btn-default backtotop" />')
			.css('position', 'fixed')
			.css('z-index', '' + this.options.zIndex)
			.css('bottom', this.options.distance + 'em')
			.css('right', this.options.distance + 'em')
			.attr('title', this.options.title)
			.html(this.options.contentHTML)
			.appendTo(container)
			.hide();
		// Les listeners
		$(document).on('scroll', this.scroll);
		this.button.on('click', this.click);
	}

	$.extend(BackToTop.prototype, {
		destroy: function() {
			$(document).off('scroll', this.scroll);
			this.button.remove();
		},
		scroll: function(event) {
			// scrollTop se trouve sur 'document.documentElement' (=html) ou 'document.body' selon les navigateurs
			if ((document.documentElement.scrollTop || document.body.scrollTop || 0) > 0)
				this.button.filter(':not(:visible)').fadeIn();
			else
				this.button.filter(':visible').fadeOut();
		},
		click: function(event) {
			event.preventDefault();
			if (document.documentElement.scrollTop)
				document.documentElement.scrollTop = 0;
			if (document.body.scrollTop)
				document.body.scrollTop = 0;
		}
	});

	BackToTop.defaultOptions = {
		// propriété CSS "z-index" pour le bouton afin qu'il apparaisse correctement devant le reste
		zIndex: 1,
		// propriété CSS indiquant la distance en "em" du bouton par rapport au bord inférieur droit 
		distance: 1,
		// le titre HTML du bouton
		title: 'Back to top',
		// le contenu HTML du boutn, par défaut une flèche vers le haut
		contentHTML: '&uarr;'
	};

	$.addPlugin('backToTop', BackToTop);

})(jQuery);

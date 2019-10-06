(function() {

	function accept(item, extension) {
		return 'calendar' === extension;
	}

	NIMBUS.plugins.add({
		name: 'calendar',
		properties: [],
		facets: [{
			name: 'calendar',
			accept: accept,
			icon: 'event',
			thumbnail: null,
			describe: (item) => ''
		}],
		actions: [{
			name: 'calendar-open',
			icon: 'event',
			caption: 'CalendarOpen',
			accept: accept,
			execute: function(item) {
				window.location.assign('/calendar.html?' + $.param({
					itemId: item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}
		}],
		langs: {
			fr: {
				CalendarOpen: "Ouvrir dans l'agenda",
				CalendarTitle: "Agenda",
				CalendarSave: "Sauvegarder mes modifications",
				CalendarClose: "Fermer l'agenda",
			},
			en: {
				CalendarOpen: "Open in calendar",
				CalendarTitle: "Calendar",
				CalendarSave: "Save modifications",
				CalendarClose: "Close calendar",
			}
		}
	});

})();
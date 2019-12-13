(function() {

	function accept(item, extension) {
		return 'contacts' === extension;
	}

	NIMBUS.plugins.add({
		name: 'contacts',
		properties: [],
		facets: [{
			name: 'contacts',
			accept: accept,
			icon: 'contacts',
			thumbnail: null,
			describe: (item) => ''
		}],
		actions: [{
			name: 'contacts-open',
			icon: 'contacts',
			caption: 'ContactsOpen',
			accept: accept,
			execute: function(item) {
				window.location.assign('/contacts.html?' + $.param({
					itemId: item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}
		}],
		langs: {
			fr: {
				ContactsOpen: "Ouvrir le carnet d'adresse",
				ContactsTitle: "Contacts",
			},
			en: {
				ContactsOpen: "Open address book",
				ContactsTitle: "Contacts",
			}
		}
	});

})();
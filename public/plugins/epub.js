(function() {

	function accept(item, extension) {
		return 'epub' === extension; 
	}

	NIMBUS.plugins.add({
		name: 'epub',
		properties: [],
		facets: [{
			name: 'epub',
			accept: accept,
			image: function(item, thumbnail) {
				return '<i class="material-icons">chrome_reader_mode</i>';
			},
			describe: function describe(item) {
				return NIMBUS.plugins.facets[NIMBUS.plugins.facets.length - 1].describe(item);
			}
		}],
		actions: [{
			name: 'epub-read',
			icon: 'chrome_reader_mode',
			caption: 'EPUBRead',
			accept: accept,
			execute: function(item) {
				window.open('/epub.html?' + $.param({
					url: '/files/stream/' + item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}			
		}],
		langs: {
			fr: {
				EPUBRead: "Lire",
				EPUBTitle: 'Lecteur ePub',
				EPUBSpreadsOff: "Afficher en une colonne",
				EPUBSpreadsOn: "Afficher sur deux colonnes",
				EPUBFirstPage: "Première page",
				EPUBPreviousPage: "Page précédente",
				EPUBNextPage: "Page suivante",
				EPUBLastPage: "Dernière page",
				EPUBCLose: "Fermer",
			},
			en: {
				EPUBRead: "Read",
				EPUBTitle: 'ePub reader',
				EPUBSpreadsOff: "Display as a single column",
				EPUBSpreadsOn: "Display on two columns",
				EPUBFirstPage: "First page",
				EPUBPreviousPage: "Previous page",
				EPUBNextPage: "Next page",
				EPUBLastPage: "Last page",
				EPUBCLose: "Close",
			} 
		}
	});

})();
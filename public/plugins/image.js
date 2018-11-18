(function() {
	// https://developer.mozilla.org/en-US/docs/Web/HTML/Element/Img
	var imageExtensions = ['bmp', 'gif', 'ico', 'jpg', 'jpeg', 'png'/*, 'svg', 'tif', 'tiff', 'webp'*/];
	if (document.implementation.hasFeature('https://www.w3.org/TR/SVG11/feature#Image', '1.1'))
		imageExtensions.push('svg');
	// TODO webp detection
	// TODO factorisation avec default-open

	function formatGPS(value, positive, negative) {
		if (!value)
			return '';
		// https://www.gps-coordinates.net/
		var suffix = value > 0 ? positive : negative;
		value = Math.abs(value);
		var s = Math.floor(value) + '°';
		value = (value - Math.trunc(value)) * 60;
		s += Math.floor(value) + '\'';
		value = (value - Math.trunc(value)) * 60;
		s += value.toFixed(3) + '\'\'';
		return s + '' + suffix;
	}

	function accept(item, extension) {
		return imageExtensions.indexOf(extension) >= 0; 
	}

	function execute(play, item) {
		window.open('/diaporama.html?' + $.param({
			ids: $('#items tr.image').get().map((tr) => $(tr).data('item').id).join(','),
			play: play,
			selection: item ? item.id : undefined,
			fromUrl: window.location.href,
			fromTitle: $('title').text()
		}));
	}

	NIMBUS.plugins.add({
		name: 'image',
		properties: [
			{ name: 'width', caption: 'ImagePropertyWidth', align: 'right', width: 50, sortBy: 'content.width', format: (i) => NIMBUS.formatInteger(i.width, "px") },
			{ name: 'height', caption: 'ImagePropertyHeight', align: 'right', width: 50, sortBy: 'content.height', format: (i) => NIMBUS.formatInteger(i.height, "px") },
			{ name: 'depth', caption: 'ImagePropertyDepth', align: 'right', width: 50, sortBy: 'content.depth', format: (i) => NIMBUS.formatInteger(i.depth) },
			{ name: 'latitude', caption: 'ImagePropertyLatitude', align: 'right', width: 120, sortBy: 'content.latitude', format: (i) => formatGPS(i.latitude, 'N', 'S') },
			{ name: 'longitude', caption: 'ImagePropertyLongitude', align: 'right', width: 120, sortBy: 'content.longitude', format: (i) => formatGPS(i.longitude, 'E', 'W') },
			{ name: 'date', caption: 'ImagePropertyDate', align: 'right', width: NIMBUS.translate('CommonDateTimeColumnWidth'), sortBy: 'content.date', format: (i) => NIMBUS.formatDatetime(i.date) },
		],
		facets: [{
			name: 'image',
			accept: accept,
			image: function(item, thumbnail) {
				if (thumbnail)
					return '<img src="/files/thumbnail/' + item.id + '?size=24" />'; // style="width: 24px; height: 24px;"
				return '<i class="material-icons">image</i>';
			},
			describe: function describe(item) {
				var p = [];
				if (item.width && item.height)
					p.push(item.width + ' x ' + item.height + ' px');
				if (item.depth)
					p.push(item.depth + ' bits');
				return p.join(', ');
			}
		}],
		actions: [
			{
				name: 'show',
				icon: 'image',
				caption: 'ImageActionShow',
				accept: accept,
				execute: function(item) {
					// Lancement du diaporama sans lecture automatique
					execute(false, item);
				}
			}, {
				name: 'slideshow',
				icon: 'slideshow',
				caption: 'ImageActionSlideshow',
				accept: accept,
				execute: function(item, extension) {
					// Lancement du diaporama avec lecture automatique
					execute(true, item);
				}
			}
		],
		langs: {
			fr: {
				ImageActionShow: "Afficher l'image",
				ImageActionSlideshow: "Lancer le diaporama",
				ImagePropertyWidth: "Largeur (px)",
				ImagePropertyHeight: "Hauteur (px)",
				ImagePropertyDepth: "Profondeur de couleur",
				ImagePropertyLatitude: "Latitude",
				ImagePropertyLongitude: "Longitude",
				ImagePropertyDate: "Prise le",
				DiaporamaTitle: "Diaporama",
				DiaporamaFirst: "Première image",
				DiaporamaPrevious: "Image précédente",
				DiaporamaNext: "Image suivante",
				DiaporamaLast: "Dernière image",
				DiaporamaPlay: "Démarrage",
				DiaporamaPause: "Arrêt",
				DiaporamaAccelerate: "Accélérer",
				DiaporamaDecelerate: "Décélérer",
				DiaporamaDimensions: "{0}x{1} px",
				DiaporamaDepth: "{0} bits",
			},
			en: {
				ImageActionShow: "Show image",
				ImageActionSlideshow: "Start slideshow",
				ImagePropertyWidth: "Width (px)",
				ImagePropertyHeight: "Height (px)",
				ImagePropertyDepth: "Color depth",
				ImagePropertyLatitude: "Latitude",
				ImagePropertyLongitude: "Longitude",
				ImagePropertyDate: "GPS Date",
				DiaporamaTitle: "Diaporama",
				DiaporamaFirst: "First image",
				DiaporamaPrevious: "Previous image",
				DiaporamaNext: "Next image",
				DiaporamaLast: "Last image",
				DiaporamaPlay: "Start",
				DiaporamaPause: "Pause",
				DiaporamaAccelerate: "Accelerate",
				DiaporamaDecelerate: "Decelerate",
				DiaporamaDimensions: "{0}x{1} px",
				DiaporamaDepth: "{0} bits",
			} 
		}
	});
})();
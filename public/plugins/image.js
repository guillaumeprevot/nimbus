(function() {

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
			accept: NIMBUS.utils.isBrowserSupportedImage,
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
				accept: NIMBUS.utils.isBrowserSupportedImage,
				execute: function(item) {
					// Lancement du diaporama sans lecture automatique
					execute(false, item);
				}
			}, {
				name: 'slideshow',
				icon: 'slideshow',
				caption: 'ImageActionSlideshow',
				accept: NIMBUS.utils.isBrowserSupportedImage,
				execute: function(item, extension) {
					// Lancement du diaporama avec lecture automatique
					execute(true, item);
				}
			}, {
				name: 'use-as-folder-icon',
				icon: 'folder_special',
				caption: 'ImageActionUseAsFolderIcon',
				accept: function(item, extension) {
					return NIMBUS.utils.isBrowserSupportedImage(item, extension) && (typeof item.parentId === 'number');
				},
				execute: function(item) {
					$.post('/files/useAsFolderIcon/' + item.id + '?size=24');
				}
			}
		],
		langs: {
			fr: {
				ImageActionShow: "Afficher l'image",
				ImageActionSlideshow: "Lancer le diaporama",
				ImageActionUseAsFolderIcon: "Utiliser comme image du dossier",
				ImagePropertyWidth: "Largeur (px)",
				ImagePropertyHeight: "Hauteur (px)",
				ImagePropertyDepth: "Profondeur de couleur",
				ImagePropertyLatitude: "Latitude",
				ImagePropertyLongitude: "Longitude",
				ImagePropertyDate: "Prise le",
				DiaporamaTitle: "Diaporama",
				DiaporamaRotateLeft: "Pivoter à gauche",
				DiaporamaFirst: "Afficher la première image",
				DiaporamaPrevious: "Afficher l'image précédente",
				DiaporamaNext: "Afficher l'image suivante",
				DiaporamaLast: "Afficher la dernière image",
				DiaporamaPlay: "Démarrer le diaporama",
				DiaporamaPause: "Mettre en pause",
				DiaporamaAccelerate: "Accélérer",
				DiaporamaDecelerate: "Décélérer",
				DiaporamaOptions: "Options",
				DiaporamaAspectScaleDown: "Centrer (par défaut)",
				DiaporamaAspectContain: "Ajuster (bandes possibles)",
				DiaporamaAspectCover: "Remplir (peut tronquer l'image)",
				DiaporamaModeDefault: "Mode par défaut",
				DiaporamaModeRepeat: "Mode répétition",
				DiaporamaModeRandom: "Mode aléatoire",
				DiaporamaFullscreen: "Passer en plein-écran",
				DiaporamaRestore: "Quitter le plein-écran",
				DiaporamaDimensions: "{0}x{1} px",
				DiaporamaDepth: "{0} bits",
			},
			en: {
				ImageActionShow: "Show image",
				ImageActionSlideshow: "Start slideshow",
				ImageActionUseAsFolderIcon: "Use as folder icon",
				ImagePropertyWidth: "Width (px)",
				ImagePropertyHeight: "Height (px)",
				ImagePropertyDepth: "Color depth",
				ImagePropertyLatitude: "Latitude",
				ImagePropertyLongitude: "Longitude",
				ImagePropertyDate: "GPS Date",
				DiaporamaTitle: "Diaporama",
				DiaporamaRotateLeft: "Rotate left",
				DiaporamaFirst: "Show first image",
				DiaporamaPrevious: "Show previous image",
				DiaporamaNext: "Show next image",
				DiaporamaLast: "Show last image",
				DiaporamaPlay: "Start diaporama",
				DiaporamaPause: "Pause diaporama",
				DiaporamaAccelerate: "Accelerate",
				DiaporamaDecelerate: "Decelerate",
				DiaporamaOptions: "Options",
				DiaporamaAspectScaleDown: "Centered (defaut mode)",
				DiaporamaAspectContain: "Adjust (letter-boxed)",
				DiaporamaAspectCover: "Cover (may be clipped)",
				DiaporamaModeDefault: "Default mode",
				DiaporamaModeRepeat: "Repeat mode",
				DiaporamaModeRandom: "Rndom mode",
				DiaporamaFullscreen: "Switch to fullscreen mode",
				DiaporamaRestore: "Exit fullscreen mode",
				DiaporamaDimensions: "{0}x{1} px",
				DiaporamaDepth: "{0} bits",
			} 
		}
	});

})();
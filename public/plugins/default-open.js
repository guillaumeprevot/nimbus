(function() {

	// Ce plugin n'ajoute qu'une seule action "Ouvrir" pour les fichiers que le navigateur pourra "a priori" ouvrir
	var actions = [];
	function addOpenAction(name, icon, accept) {
		actions.push({
			name: name,
			icon: /*icon ||*/ 'open_in_new',
			caption: 'DefaultOpen',
			accept: accept,
			execute: function(item) {
				window.open('/files/stream/' + item.id);
			}
		});
	}

	// Pour les fichiers audio, NIMBUS partage la méthode détection
	addOpenAction('audio-open', 'play_arrow', function(item, extension) {
		return NIMBUS.utils.isBrowserSupportedAudio(item, extension)
	});

	// Pour les fichiers vidéo, NIMBUS partage la méthode détection
	addOpenAction('video-open', 'videocam', function(item, extension) {
		return NIMBUS.utils.isBrowserSupportedVideo(item, extension)
	});

	// Pour les images, NIMBUS partage la méthode détection
	addOpenAction('image-open', 'image', function(item, extension) {
		return NIMBUS.utils.isBrowserSupportedImage(item, extension)
	});

	// Les fichiers PDF semblent bien supportés pas les navigateur.
	// En tout cas au moins par Firefox, Chrome et Edge.
	addOpenAction('pdf-open', 'picture_as_pdf', function(item, extension) {
		return !item.folder && extension === 'pdf';
	});

	// Les fichiers ePub sont supportés par Edge nativement.
	// Il existe des extensions pour Firefox et Chrome.
	addOpenAction('epub-open', 'chrome_reader_mode', function(item, extension) {
		return !item.folder && extension === 'epub';
	});

	// Pour les fichiers texte, on considérera tous les types MIME commençant par "text/" + une liste de type MIME supplémentaires
	var textExtensions = ['js', 'json', 'srt', 'ts', 'xml'];
	addOpenAction('text-open', 'text_fields', function(item, extension) {
		return !item.folder && (item.mimetype.indexOf('text/') === 0 || textExtensions.indexOf(extension) >= 0);
	});

	// Enregistrement du plugin
	NIMBUS.plugins.add({
		name: 'default-open',
		actions: actions,
		langs: {
			fr: {
				DefaultOpen: "Ouvrir"
			},
			en: {
				DefaultOpen: "Open"
			}
		}
	});
})();

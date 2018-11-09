(function() {
	// Ce plugin n'ajoute qu'une seule action "Ouvrir" pour les fichiers que le navigateur pourra "a priori" ouvrir
	var actions = [];
	function addOpenAction(name, icon, accept) {
		actions.push({
			name: name,
			icon: /*icon ||*/ 'open_in_new',
			caption: 'DefaultOpen',
			accept: function(item, extension) {
				return !item.folder && accept(item, extension);
			},
			execute: function(item) {
				window.open('/files/stream/' + item.id);
			}
		});
	}

	// Pour les fichiers audio/video, on utilisera "canPlayType" :
	// - https://www.w3schools.com/tags/av_met_canplaytype.asp
	// Modernizr propose aussi cette API mais différement :
	// - audio : https://github.com/Modernizr/Modernizr/blob/master/feature-detects/audio.js
	// - video : https://github.com/Modernizr/Modernizr/blob/master/feature-detects/video.js
	// Mozilla fournit aussi de nombreuses informations
	// - https://developer.mozilla.org/en-US/docs/Web/HTML/Supported_media_formats
	// Des échantillons peuvent être trouvés ici
	// - https://hpr.dogphilosophy.net/test/
	// - http://samplephotovideo.com/
	// - https://www.sample-videos.com/
	var audioElement = document.createElement('audio');
	addOpenAction('audio-open', 'play_arrow', function(item, extension) {
		return item.mimetype.indexOf('audio/') === 0 && audioElement.canPlayType(item.mimetype);
	});
	var videoElement = document.createElement('video');
	addOpenAction('video-open', 'videocam', function(item, extension) {
		return item.mimetype.indexOf('video/') === 0 && videoElement.canPlayType(item.mimetype);
	});

	// Pour les fichiers image, il faut un peu de code. Après test :
	// - Chrome accepte bmp, gif, ico, jpg, png, svg et webp MAIS pas tiff
	// - Firefox accepte bmp, gif, ico, jpg, png, svg MAIS ni tiff ni webp
	// - Edge accepte bmp, gif, ico, jpg, png, svg, webp MAIS pas tiff
	// Du coup, plutôt que tout tester :
	// - on considère quelques extensions comme OK (bmp, gif, ico, jpg, png)
	// - on considère tiff comme pas bon 
	// - on utilise un test pour "svg" qui prend une ligne (idée trouvée chez Modernizr)
	// - on utilise un test pour "webp" qui pourra être généralisé
	var imageExtensions = ['bmp', 'gif', 'ico', 'jpg', 'jpeg', 'png'/*, 'svg', 'tif', 'tiff', 'webp'*/];
	if (document.implementation.hasFeature('https://www.w3.org/TR/SVG11/feature#Image', '1.1'))
		imageExtensions.push('svg');
	function testImage(extensions, dataURL) {
		// Une méthode alternative existe en utilisant "fetch" et "createImageBitmap" mais Edge ne connait pas la seconde
		// https://ourcodeworld.com/articles/read/630/how-to-detect-if-the-webp-image-format-is-supported-in-the-browser-with-javascript
		var img = new Image();
		img.onload = function () {
			if (img.width === 1 && img.height === 1)
				imageExtensions.push.apply(imageExtensions, extensions);
		};
		img.src = dataURL;
	}
	// Image maison contenant un pixel rouge
	//testImage(['bmp'], 'data:image/bmp;base64,Qk1+AAAAAAAAAHoAAABsAAAAAQAAAAEAAAABABgAAAAAAAQAAAATCwAAEwsAAAAAAAAAAAAAQkdScwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAAP8A');
	//testImage(['gif'], 'data:image/gif;base64,R0lGODlhAQABAIABAP8AAP///yH+EUNyZWF0ZWQgd2l0aCBHSU1QACwAAAAAAQABAAACAkQBADs=');
	//testImage(['ico'], 'data:image/x-icon;base64,AAABAAEAAQEAAAEAIAAwAAAAFgAAACgAAAABAAAAAgAAAAEAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAP//AAAAAA==');
	//testImage(['jpg', 'jpeg'], 'data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoHBwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQMEBAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT/wgARCAABAAEDAREAAhEBAxEB/8QAFAABAAAAAAAAAAAAAAAAAAAAB//EABUBAQEAAAAAAAAAAAAAAAAAAAYI/9oADAMBAAIQAxAAAAE5C1T/AP/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAQUCf//EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQMBAT8Bf//EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQIBAT8Bf//EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEABj8Cf//EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAT8hf//aAAwDAQACAAMAAAAQ/wD/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAEDAQE/EH//xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAECAQE/EH//xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAE/EH//2Q==');
	//testImage(['png'], 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH4ggaDis3XN+ZfwAAAAxJREFUCNdj+M/AAAADAQEAGN2NsAAAAABJRU5ErkJggg==');
	//testImage(['tif', 'tiff'], 'data:image/tiff;base64,SUkqAAwAAAD/AAAAEAD+AAQAAQAAAAAAAAAAAQMAAQAAAAEAAAABAQMAAQAAAAEAAAACAQMAAwAAAOIAAAADAQMAAQAAAAEAAAAGAQMAAQAAAAIAAAANAQIAIgAAAOgAAAARAQQAAQAAAAgAAAASAQMAAQAAAAEAAAAVAQMAAQAAAAMAAAAWAQMAAQAAAEAAAAAXAQQAAQAAAAMAAAAaAQUAAQAAANIAAAAbAQUAAQAAANoAAAAcAQMAAQAAAAEAAAAoAQMAAQAAAAIAAAAAAAAA/////8EbjgP/////wRuOAwgACAAIAEQ6XHRlbXBcZGV2Y2xvdWRcaW1hZ2VccGl4ZWwudGlmZgA=');
	// Image proposée ici pour SVG : https://css-tricks.com/test-support-svg-img/
	//testImage(['svg'], 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMSIgaGVpZ2h0PSIxIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjwvc3ZnPg==');
	// Image trouvée ici pour WEBP (https://developers.google.com/speed/webp/faq)
	testImage(['webp'], 'data:image/webp;base64,UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA');
	addOpenAction('image-open', 'image', function(item, extension) {
		return imageExtensions.indexOf(extension) >= 0;
	});

	// Pour les fichiers texte, on considérera tous les types MIME commençant par "text/" + une liste de type MIME supplémentaires
	var textExtensions = ['js', 'json', 'srt', 'ts', 'xml'];
	addOpenAction('text-open', 'text_fields', function(item, extension) {
		return item.mimetype.indexOf('text/') === 0 || textExtensions.indexOf(extension) >= 0;
	});

	// Enfin, on traite à part quelques types de fichiers
	// - les fichiers PDF sont supportés pas les navigateurs (au moins Firefos, Chrome et Edge)
	addOpenAction('pdf-open', 'picture_as_pdf', function(item, extension) {
		return extension === 'pdf';
	});
	// - les fichiers ePub sont supportés par Edge nativement et il existe des extensions pour Firefox et Chrome
	addOpenAction('epub-open', 'chrome_reader_mode', function(item, extension) {
		return extension === 'epub';
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

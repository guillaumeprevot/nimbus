(function() {

	const extensions = {
		md5: 'MD5',
		sha1: 'SHA-1',
		sha256: 'SHA-256',
		sha512: 'SHA-512',
	}
	// const extensions = ['md5', 'sha1', 'sha256', 'sha512'];
	// [].push.apply(NIMBUS.utils.textFileExtensions, extensions);

	NIMBUS.plugins.add({
		name: 'digest',
		properties: [],
		facets: [],
		actions: [{
			name: 'checkDigest',
			icon: 'fact_check',
			caption: 'DigestCheck',
			accept: (_item, extension) => extensions.hasOwnProperty(extension),
			execute: function(item) {
				const extension = NIMBUS.utils.getFileExtensionFromItem(item);
				const algorithm = extensions[extension];
				$.get('/digest/check?itemId=' + item.id + '&algorithm=' + algorithm).then(function(results) {
					const ul = $('<ul></ul>');
					// console.log(results);
					results.forEach(r => {
						if (!r.actual)
							$('<li class="text-warning"></li>').text(NIMBUS.translate('DigestResultMissing', r.name)).appendTo(ul);
						else if (r.actual !== r.expected)
							$('<li class="text-danger"></li>').text(NIMBUS.translate('DigestResultInvalid', r.name)).appendTo(ul);
						else
							$('<li></li>').text(NIMBUS.translate('DigestResultOK', r.name)).appendTo(ul);
					});
					NIMBUS.message(ul.html(), false, true);
				});
			}
		}],
		langs: {
			fr: {
				DigestCheck: "Vérifier l'intégrité des fichiers",
				DigestResultOK: "{0} : OK",
				DigestResultMissing: "{0} : manquant",
				DigestResultInvalid: "{0} : non valide",
			},
			en: {
				DigestCheck: "Check referenced file integrity",
				DigestResultOK: "{0}: OK",
				DigestResultMissing: "{0}: missing",
				DigestResultInvalid: "{0}: invalid",
			}
		}
	});

})();
(function() {

	function accept(item, extension) {
		return 'contacts' === extension;
	}

	/**
	 * Cette méthode export un ensemble de carnets d'adresses en un texte au format VCF 2.1.
	 * Au départ prévu en 3.0 et 4.0, j'ai ensuite remarqué que Thunderbird et Android prennent plutôt du 2.1.
	 *
	 * Quelques informations utiles :
	 * - type MIME : text/vcard
	 * - extension : .cvf ou .vcard
	 * - spécification 4.0 : https://tools.ietf.org/html/rfc6350 (format) + https://tools.ietf.org/html/rfc6868#section-3.2 (encodage)
	 * - spécification 3.0 : https://tools.ietf.org/html/rfc2426
	 * - Wikipédia FR : https://fr.wikipedia.org/wiki/VCard
	 * - Wikipédia EN : https://en.wikipedia.org/wiki/VCard
	 * - vCard-js (JS) : https://github.com/enesser/vCards-js
	 * - ez-vcard (Java) : https://github.com/mangstadt/ez-vcard/tree/master/src/main/java/ezvcard
	 *
	 * Quelques champs VCard qui pourraient s'avérer interressant :
	 * - PHOTO quand c.picture est une photo (mais il faut le type MIME)
	 * - LOGO quand c.picture est un logo (mais il faut le type MIME)
	 * - KIND pour gérer les types de contacts (individual, org, ...)
	 * - RELATED pour gérer les relations entre contact
	 *
	 * Principales différences 2.1 / 3.0 : https://tools.ietf.org/html/rfc2426#section-5
	 * - passage des propriétés VERSION / N / FN en obligatoires
	 * - ajout des propriétés CATEGORIES / CLASS / NICKNAME / PRODID / SORT-STRING et SOURCE / NAME / PROFILE
	 * - suppression de QUOTED-PRINTABLE
	 * - échappement de CRLF en '\n' ou '\N' au lieu de '^n'
	 * - échappement de ',' et ';' en '\,' et '\;'
	 * - restriction du paramètre CHARSET à Content-Type
	 * - le préfixe "TYPE=" doit être spécifié pour le type
	 *
	 * Principales différences 3.0 / 4.0 : https://tools.ietf.org/html/rfc6350#appendix-A
	 * - UTF-8 obligatoirement
	 * - ajout des propriétés KIND / GENDER / LANG / ANNIVERSARY / XML / CLIENTPIDMAP et des paramètres ALTID / PID
	 * - suppression des paramètres CONTEXT / CHARSET, des propriétés NAME / MAILER / LABEL / CLASS et de certains types d'adresse
	 * - remplacement de TYPE par MEDIATYPE si c'était son sens (pour les images par exemple)
	 * - extension des types "work" and "home"
	 * - passage du paramètre "pref" en entier >= 1
	 *
	 * @param {Array} contacts - la liste des contacts à exporter
	 * @param {string} version - exporter au format demandé , "2.1", "3.0" ou "4.0"
	 * @param {boolean} onlyFavorite - n'exporter que les contacts favoris, si true, ou tous les contacts sinon
	 * @param {string} onlyKeyword - n'exporter que les contacts marqués de ce mot-clef, si précisé, ou tous les contacts sinon
	 * @return une chaine de caractère contenant les VCard de tous les contacts demandés
	 */
	function exportVCF(contacts, version, onlyFavorite, onlyKeyword) {
		// var contacts = Array.prototype.concat.apply([], sources.map((s) => s.contacts));
		if (onlyFavorite)
			contacts = contacts.filter((c) => c.favorite);
		if (onlyKeyword)
			contacts = contacts.filter((c) => c.keywords && c.keywords.includes(onlyKeyword));
		var s = '';
		var rev = (new Date()).toISOString();
		var add = (field, value, utf8) => s += field + ((utf8 && version === '2.1') ? ';CHARSET=UTF-8' : '') + ':' + value + '\r\n';
		var multiValue = (values) => values.map(v => v || '').join(';');
		var escapeValue = (value) => version === '2.1'
			? value.replace(/\n/g, '^n')
			: value.replace(/\n/g, '\\n').replace(/,/g, '\\,').replace(/;/g, '\\;');
		var mappedValue = (value, map, defaultValue) => value && map[value] || defaultValue || '';
		var dateValue = (year, month, date) => (year || '--') + ('0' + (month || '--')).slice(-2) + ('0' + (date || '--')).slice(-2);
		var pref = (i) => (version === '2.1') ? (i == 0 ? ';PREF' : '') : (';PREF=' + (i + 1));
		var type = (t) => !t ? '' : (version === '2.1') ? (';' + t) : (';TYPE=' + t);
		var prefAndType = (i, t) => pref(i) + type(t);
		contacts.forEach(function(c, i) {
			if (i > 0)
				s += '\r\n';
			add('BEGIN', 'VCARD');
			add('VERSION', version);

			if (c.displayName)
				add('FN', c.displayName, true);
			else if (version !== '2.1') // requis en 3.0+
				add('FN', c.nickname || [c.firstName, c.lastName].join(' '), true);

			if (c.lastName || c.firstName || c.middleName || c.prefix || c.suffix)
				add('N', multiValue([c.lastName, c.firstName, c.middleName, c.prefix, c.suffix]), true);
			else if (version !== '2.1') // requis en 3.0+
				add('N', multiValue(['', c.displayName, '', '', '']), true);

			if (version === '4.0' && c.gender)
				add('GENDER', mappedValue(c.gender, { male:'M', female:'F', other:'O'}, 'U'));
			if (version !== '2.1' && c.nickname)
				add('NICKNAME', c.nickname, true);
			if (c.companyName || c.companyUnit)
				add('ORG', multiValue([c.companyName, c.companyUnit]), true);
			if (c.companyFunction)
				add('ROLE', c.companyFunction, true);
			if (version !== '2.1' && c.keywords)
				add('CATEGORIES', c.keywords, true);
			if (c.note)
				add('NOTE', escapeValue(c.note), true);

			if (c.addresses && c.addresses.length > 0) {
				c.addresses.filter((a) => a.type !== 'archive').forEach((a, i) => {
					var formattedValue = NIMBUS.utils.contactAPI.formatAddress(a);
					var type = mappedValue(a.type, { home: 'HOME', work: 'WORK' }, '');
					var params = prefAndType(i, type);
					if (version === '4.0')
						params += ';LABEL="' + formattedValue + '"';
					add('ADR' + params, multiValue(['', a.address2, a.address, a.city, a.state, a.zipCode, a.country]), true);
					if (version !== '4.0')
						add('LABEL', formattedValue, true);
				});
			}

			if (c.emails && c.emails.length > 0) {
				c.emails.filter((e) => e.type !== 'archive').forEach((e, i) => {
					var type = version === '4.0' ? mappedValue(e.type, { home: 'HOME', work: 'WORK' }, '') : 'INTERNET';
					var params = prefAndType(i, type);
					add('EMAIL' + params, e.email);
				});
			}

			if (c.phones && c.phones.length > 0) {
				c.phones.filter((p) => p.type !== 'archive').forEach((p, i) => {
					var v21Map = { home: 'HOME', work: 'WORK', mobile: 'CELL', workMobile: 'CELL', fax: 'FAX', workFax: 'FAX', pager: 'PAGER', workPager: 'PAGER', workStandard: 'OTHER', other: 'OTHER' };
					var v30Map = { home: 'HOME,VOICE', work: 'WORK,VOICE', mobile: 'HOME,CELL', workMobile: 'WORK,CELL', fax: 'HOME,FAX', workFax: 'WORK,FAX', pager: 'HOME,PAGER', workPager: 'WORK,PAGER', workStandard: 'OTHER', other: 'OTHER' };
					var v40Map = { home: 'home', work: 'work', mobile: '"cell,home"', workMobile: '"cell,work"', fax: '"fax,home"', workFax: '"fax,work"', pager: 'pager', workPager: 'pager', workStandard: '"voice,other"', other: '"voice,other"' };
					var type = mappedValue(p.type, version === '4.0' ? v40Map : version === '3.0' ? v30Map : v21Map, '');
					var params = prefAndType(i, type);
					if (version === '4.0')
						add('TEL' + params + ';VALUE=uri', 'tel:' + p.phone);
					else
						add('TEL' + params, p.phone);
				});
			}

			if (c.urls && c.urls.length > 0) {
				c.urls.filter((u) => u.type !== 'archive').forEach((u, i) => {
					var type = mappedValue(u.type, { home: 'HOME', work: 'WORK' }, '');
					var params = prefAndType(i, type);
					add('URL' + params, u.url, true);
				});
			}

			var bday = c.dates && c.dates.find((d) => d.type === 'birthday');
			if (bday)
				add('BDAY', dateValue(bday.year, bday.month, bday.date));
			var anniversary = (version === '4.0') && c.dates && c.dates.find((d) => d.type === 'wedding');
			if (anniversary)
				add('ANNIVERSARY', dateValue(anniversary.year, anniversary.month, anniversary.date));

			add('REV', rev);
			if (version !== '2.1')
				add('PROGID', 'Nimbus');
			add('END', 'VCARD');
		});
		return s;
	}

	NIMBUS.plugins.add({
		name: 'vcard',
		properties: [],
		facets: [{
			name: 'vcard',
			accept: accept,
			icon: 'contacts',
			thumbnail: null,
			describe: (item) => ''
		}],
		actions: [{
			name: 'vcard-download',
			icon: 'cloud_download',
			caption: 'VCardDownload',
			accept: accept,
			execute: function(item) {
				$.get('/files/stream/' + item.id).then(function(content) {
					var vcf = exportVCF(content.contacts, '2.1', true, '');
					var blob = new Blob([vcf], { type: 'text/vcard' });
					var filename = item.name.replace(/\.contacts$/, '.vcf');
					NIMBUS.utils.downloadFile(blob, filename);
				});
			}
		}, {
			name: 'vcard-convert',
			icon: 'transform',
			caption: 'VCardConvert',
			accept: accept,
			execute: function(item) {
				$.get('/files/stream/' + item.id).then(function(content) {
					var vcf = exportVCF(content.contacts, '2.1', true, '');
					var blob = new Blob([vcf], { type: 'text/vcard' });
					var filename = item.name.replace(/\.contacts$/, '.vcf');
					NIMBUS.utils.uploadFile(item.parentId, blob, filename).then(function() {
						NIMBUS.navigation.refreshItems(false);
					});
				});
			}
		}],
		langs: {
			fr: {
				VCardDownload: "Télécharger en vCard les contacts favoris (*.cvf)",
				VCardConvert: "Convertir en vCard les contacts favoris (*.cvf)",
			},
			en: {
				VCardDownload: "Download favorite contacts to vCard (*.cvf)",
				VCardConvert: "Convert favorite contacts to vCard (*.cvf)",
			}
		}
	});

})();
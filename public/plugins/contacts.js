(function() {

	function ContactAddress(data) {
		this.type = data.type;
		this.label = data.label;
		this.address = data.address;
		this.address2 = data.address2;
		this.zipCode = data.zipCode;
		this.city = data.city;
		this.state = data.state;
		this.country = data.country;
	}

	ContactAddress.types = {
		home: "ContactAddressTypeHome",
		work: "ContactAddressTypeWork",
		archive: "ContactAddressTypeArchive",
		other: "ContactAddressTypeOther"
	};

	function ContactEmail(data) {
		this.type = data.type;
		this.label = data.label;
		this.email = data.email;
	}

	ContactEmail.types = {
		home: "ContactEmailTypeHome",
		work: "ContactEmailTypeWork",
		archive: "ContactEmailTypeArchive",
		other: "ContactEmailTypeOther"
	};

	function ContactPhone(data) {
		this.type = data.type;
		this.label = data.label;
		this.phone = data.phone;
	}

	ContactPhone.types = {
		home: "ContactPhoneTypeHome",
		work: "ContactPhoneTypeWork",
		mobile: "ContactPhoneTypeMobile",
		workMobile: "ContactPhoneTypeWorkMobile",
		fax: "ContactPhoneTypeFax",
		workFax: "ContactPhoneTypeWorkFax",
		pager: "ContactPhoneTypePager",
		workPager: "ContactPhoneTypeWorkPager",
		workStandard: "ContactPhoneTypeWorkStandard",
		archive: "ContactPhoneTypeArchive",
		other: "ContactPhoneTypeOther"
	};

	function ContactURL(data) {
		this.type = data.type;
		this.label = data.label;
		this.url = data.url;
	}

	ContactURL.types = {
		home: "ContactURLTypeHome",
		work: "ContactURLTypeWork",
		blog: "ContactURLTypeBlog",
		website: "ContactURLTypeWebsite",
		archive: "ContactURLTypeArchive",
		other: "ContactURLTypeOther"
	};

	function ContactDate(data) {
		this.type = data.type;
		this.label = data.label;
		this.year = data.year;
		this.month = data.month;
		this.date = data.date;
	}

	ContactDate.types = {
		birthday: "ContactDateTypeBirthday",
		feast: "ContactDateTypeFeast",
		wedding: "ContactDateTypeWedding",
		divorce: "ContactDateTypeDivorce",
		death: "ContactDateTypeDeath",
		other: "ContactDateTypeOther"
	};

	function ContactField(data) {
		this.label = data.label;
		this.value = data.value;
	}

	function Contact(data) {
		// Indicateur pour marquer un contact comme favori
		this.favorite = data.favorite;
		// Nom utilisé pour afficher le contact dans l'IHM
		this.displayName = data.displayName;
		// Dénomination complète (Mr Professor John Smart Doe Senior)
		this.gender = data.gender;
		this.prefix = data.prefix;
		this.firstName = data.firstName;
		this.middleName = data.middleName;
		this.lastName = data.lastName;
		this.suffix = data.suffix;
		// Surnom
		this.nickname = data.nickname;
		// Infos professionnelles (société, fonction et service)
		this.companyName = data.companyName;
		this.companyFunction = data.companyFunction;
		this.companyUnit = data.companyUnit;
		// id d'une photo hébergé dans Nimbus
		this.picture = data.picture;
		// Liste de mots-clefs séparés par ","
		this.keywords = data.keywords;
		// Note personnalisée
		this.note = data.note;
		// Liste de données typées (adresse, email, numéro, url et date)
		this.addresses = data.addresses;
		this.emails = data.emails;
		this.phones = data.phones;
		this.urls = data.urls;
		this.dates = data.dates;
		// Liste de champs personnalisés
		this.fields = data.fields;
	}

	Contact.genderTypes = {
		unspecified: "ContactGenderUnspecified",
		male: "ContactGenderMale",
		female: "ContactGenderFemale",
		other: "ContactGenderOther"
	};

	/** Cette classe représente un fichier de contacts (*.contacts) */
	function ContactSource(item) {
		// L'élément de Nimbus contenant les données de cette source
		this.item = item;
		// Le nom de ce carnet d'adresse choisi par l'utilisateur (sinon, il sera dérivé de item.name dans l'IHM)
		this.name = null; //item.name.replace(/.contacts/gi, '');
		// la liste des contacts de cette source, un tableau de Contact
		this.contacts = [];
		// indique si les contacts sont actuellement triés
		this.sorted = false;
	}

	ContactSource.prototype.getNameOrDefault = function() {
		return this.name || this.item.name.replace(/.contacts/gi, '');
	};

	ContactSource.prototype.load = function() {
		var self = this;
		return $.get('/files/stream/' + self.item.id).then(undefined, function(result) {
			return { name: null, contacts: [] };
		}).then(function(result) {
			self.name = result.name;
			self.contacts = result.contacts.map(function(c) {
				var contact = new Contact(c);
				if (contact.addresses)
					contact.addresses = contact.addresses.map((a) => new ContactAddress(a));
				if (contact.emails)
					contact.emails = contact.emails.map((e) => new ContactEmail(e));
				if (contact.phones)
					contact.phones = contact.phones.map((p) => new ContactPhone(p));
				if (contact.urls)
					contact.urls = contact.urls.map((u) => new ContactURL(u));
				if (contact.dates)
					contact.dates = contact.dates.map((d) => new ContactDate(d));
				if (contact.fields)
					contact.fields = contact.fields.map((f) => new ContactField(f));
				return contact;
			});
		});
	};

	ContactSource.prototype.sort = function(formatContact) {
		this.contacts.sort((c1, c2) => formatContact(c1).localeCompare(formatContact(c2)));
		this.sorted = true;
	};

	ContactSource.prototype.save = function() {
		return NIMBUS.utils.updateFileJSON(this.item.id, true, {
			name: this.name,
			contacts: this.contacts.map(function(c) {
				return {
					favorite: c.favorite || undefined,
					displayName: c.displayName || undefined,
					gender: c.gender || undefined,
					prefix: c.prefix || undefined,
					firstName: c.firstName || undefined,
					middleName: c.middleName || undefined,
					lastName: c.lastName || undefined,
					suffix: c.suffix || undefined,
					nickname: c.nickname || undefined,
					companyName: c.companyName || undefined,
					companyFunction: c.companyFunction || undefined,
					companyUnit: c.companyUnit || undefined,
					picture: c.picture || undefined,
					keywords: c.keywords || undefined,
					note: c.note || undefined,
					addresses: (c.addresses && c.addresses.length) ? c.addresses : undefined,
					emails: (c.emails && c.emails.length) ? c.emails : undefined,
					phones: (c.phones && c.phones.length) ? c.phones : undefined,
					urls: (c.urls && c.urls.length) ? c.urls : undefined,
					dates: (c.dates && c.dates.length) ? c.dates : undefined,
					fields: (c.fields && c.fields.length) ? c.fields : undefined,
				};
			})
		});
	};

	function accept(item, extension) {
		return 'contacts' === extension;
	}

	// Cette méthode vérifier si le contact correspond au texte recherché
	function matchContact(contact, searchTextLC, searchAllFields, searchAllContacts, formatContact) {
		// Si on ne cherche que dans les favoris, on peut exclure ceux qui n'en sont pas
		if (!searchAllContacts && !contact.favorite)
			return false;
		// Si on matche sur le libellé, on peut déjà accepter le contact
		if (formatContact(contact).toLowerCase().includes(searchTextLC))
			return true;
		// Si on matche pas sur le libellé, on peut s'arrêter là en recherche simple
		if (!searchAllFields)
			return false;
		// Sinon, il nous reste à tester les autres champs
		function matchValue(value) {
			return !!value && value.toLowerCase().includes(searchTextLC);
		}
		function matchList(list, property) {
			return !!list && list.some((e) => (e.label && e.label.toLowerCase().includes(searchTextLC)) || (e[property] && e[property].toLowerCase().includes(searchTextLC)));
		}
		return matchValue(contact.firstName) || matchValue(contact.lastName)
			|| matchValue(contact.nickname) || matchValue(contact.companyName)
			|| matchValue(contact.keywords) || matchValue(contact.note)
			|| matchList(contact.emails, 'email') || matchList(contact.phones, 'phone')
			|| matchList(contact.urls, 'url') || matchList(contact.fields, 'value');
	}

	function formatContactFirstLast(contact) {
		if (contact.displayName)
			return contact.displayName;
		var s = contact.firstName || '';
		if (contact.middleName)
			s = (s + ' ' + contact.middleName).trim();
		if (contact.lastName)
			s = (s + ' ' + contact.lastName).trim();
		return s;
	}

	function formatContactLastFirst(contact) {
		if (contact.displayName)
			return contact.displayName;
		var s = contact.firstName || '';
		if (contact.middleName)
			s = (s + ' ' + contact.middleName).trim();
		if (contact.lastName)
			s = (contact.lastName + ' ' + s).trim();
		return s;
	}

	function formatAddress(address) {
		var parts = [];
		if (address.address) parts.push(address.address);
		if (address.address2) parts.push(address.address2);
		if (address.zipCode || address.city) parts.push([address.zipCode || '', address.city || ''].join(' ').trim());
		if (address.state) parts.push(address.state);
		if (address.country) parts.push(address.country);
		return parts.join(', ');
	}

	function formatPhone(phone) {
		if (phone && phone.length === 10)
			return (phone[0] + phone[1] + '.' + phone[2] + phone[3] + '.' + phone[4] + phone[5] + '.' + phone[6] + phone[7] + '.' + phone[8] + phone[9]);
		return phone;
	}

	function generateContactsFromThunderbirdExport(csv) {
		var supportedFields = [];
		var supportedField = function(name, consumer) { supportedFields.push({ name: name, consumer: consumer }); };
		var currentAddress = {}, birthday = { type: 'birthday' };
		supportedField('Prénom', (c, t) => c.firstName = t);
		supportedField('Nom de famille', (c, t) => c.lastName = t);
		supportedField('Nom à afficher', (c, t) => c.displayName = t);
		supportedField('Surnom', (c, t) => c.nickname = t);
		supportedField('Adresse électronique principale', (c, t) => c.emails.push(new ContactEmail({ email: t, type: 'home' })));
		supportedField('Adresse électronique secondaire', (c, t) => c.emails.push(new ContactEmail({ email: t, type: 'other' })));
		supportedField('Nom de l’écran', (c, t) => c.fields.push(new ContactField({ value: t, label: 'Pseudo IM' })));
		supportedField('Tél. professionnel', (c, t) => c.phones.push(new ContactPhone({ phone: t, type: 'work' })));
		supportedField('Tél. personnel', (c, t) => c.phones.push(new ContactPhone({ phone: t, type: 'home' })));
		supportedField('Fax', (c, t) => c.phones.push(new ContactPhone({ phone: t, type: 'fax' })));
		supportedField('Pager', (c, t) => c.phones.push(new ContactPhone({ phone: t, type: 'pager' })));
		supportedField('Portable', (c, t) => c.phones.push(new ContactPhone({ phone: t, type: 'mobile' })));
		supportedField('Adresse privée', (c, t) => currentAddress.address = t);
		supportedField('Adresse privée 2', (c, t) => currentAddress.address2 = t);
		supportedField('Pays/Région (domicile)', (c, t) => currentAddress.country = t);
		supportedField('Adresse professionnelle', (c, t) => currentAddress.address = t);
		supportedField('Adresse professionnelle 2', (c, t) => currentAddress.address2 = t);
		supportedField('Pays/Région (bureau)', (c, t) => currentAddress.country = t);
		supportedField('Ville', (c, t) => currentAddress.city = t);
		supportedField('Pays/État', (c, t) => currentAddress.state = t);
		supportedField('Code postal', (c, t) => currentAddress.zipCode = t);
		supportedField('Profession', (c, t) => c.companyFunction = t);
		supportedField('Service', (c, t) => c.companyUnit = t);
		supportedField('Société', (c, t) => c.companyName = t);
		supportedField('Site web 1', (c, t) => c.urls.push(new ContactURL({ url: t, type: 'website' })));
		supportedField('Site web 2', (c, t) => c.urls.push(new ContactURL({ url: t, type: 'website' })));
		supportedField('Année de naissance', (c, t) => birthday.year = parseInt(t));
		supportedField('Mois', (c, t) => birthday.month = parseInt(t));
		supportedField('Jour', (c, t) => birthday.date = parseInt(t));
		supportedField('Divers 1', (c, t) => c.fields.push(new ContactField({ value: t, label: 'Divers 1' })));
		supportedField('Divers 2', (c, t) => c.fields.push(new ContactField({ value: t, label: 'Divers 2' })));
		supportedField('Divers 3', (c, t) => c.fields.push(new ContactField({ value: t, label: 'Divers 3' })));
		supportedField('Divers 4', (c, t) => c.fields.push(new ContactField({ value: t, label: 'Divers 4' })));
		supportedField('Notes', (c, t) => c.note = t);

		var index = csv.indexOf('\n');
		var fields = csv.substring(0, index).split(',').map(function(h) {
			var f = supportedFields.find(field => field.name === h);
			if (!f)
				throw new Error('Champ inconnu : ' + h);
			return f;
		});
		// console.log(fields);

		index++;
		function next() {
			var endIndex, result;
			if (csv[index] === '"') {
				endIndex = csv.indexOf('"', index + 1);
				while (csv[endIndex + 1] === '"') {
					// Take care of "....""quoted text""..."
					endIndex = csv.indexOf('""', endIndex + 2);
					endIndex = csv.indexOf('"', endIndex + 2);
				}
				result = csv.substring(index + 1, endIndex).replace(/""/g, '"');
				index = endIndex + 2;
			} else {
				endIndex = index;
				while (csv[endIndex] !== ',' && csv[endIndex] !== '\n') {
					endIndex++;
				}
				result = csv.substring(index, endIndex);
				index = endIndex + 1;
			}
			return result ? result : undefined;
		}

		var contacts = [];
		while (index < csv.length) {
			var contact = new Contact({});
			contact.addresses = [];
			contact.emails = [];
			contact.phones = [];
			contact.urls = [];
			contact.dates = [];
			contact.fields = [];
			contacts.push(contact);

			fields.forEach(function(f) {
				var t = next();
				if (t)
					f.consumer(contact, t);
				if (f.name === 'Jour' && (birthday.year || birthday.month || birthday.date)) {
					contact.dates.push(new ContactDate(birthday));
					birthday = { type: 'birthday' };
				} else if (f.name === 'Pays/Région (domicile)' && formatAddress(currentAddress)) {
					currentAddress.type = 'home';
					contact.addresses.push(new ContactAddress(currentAddress));
					currentAddress = {};
				} else if (f.name === 'Pays/Région (bureau)' && formatAddress(currentAddress)) {
					currentAddress.type = 'work';
					contact.addresses.push(new ContactAddress(currentAddress));
					currentAddress = {};
				}
			});
		}
		// console.log(contacts);
		return contacts;
	}

	NIMBUS.utils.contactAPI = {
		ContactAddress: ContactAddress,
		ContactEmail: ContactEmail,
		ContactPhone: ContactPhone,
		ContactURL: ContactURL,
		ContactDate: ContactDate,
		ContactField: ContactField,
		Contact: Contact,
		ContactSource: ContactSource,

		matchContact: matchContact,
		formatContactFirstLast: formatContactFirstLast,
		formatContactLastFirst: formatContactLastFirst,
		formatAddress: formatAddress,
		formatPhone: formatPhone,
		generateContactsFromThunderbirdExport: generateContactsFromThunderbirdExport,
		createMappyURL: (text) => 'https://fr.mappy.com/#/1/M2/TSearch/S' + encodeURI(text),
		createGoogleMapsURL: (text) => 'https://www.google.com/maps/place/' + encodeURI(text),
		createOpenStreetMapURL: (text) => 'https://www.openstreetmap.org/search?query=' + encodeURI(text),
		createEmailLink: (e) => $('<a />').text(e.email).attr('href', 'mailto:' + e.email),
		createPhoneLink: (p) => $('<a />').text(p.phone).attr('href', 'tel:' + p.phone),
		createURLLink: (u) => $('<a target="_blank" />').text(u.label || NIMBUS.translate(ContactURL.types[u.type])).attr('href', u.url)
	};

	NIMBUS.utils.textFileExtensions.push('contacts');

	NIMBUS.plugins.add({
		name: 'contacts',
		properties: [],
		facets: [{
			name: 'contacts',
			accept: accept,
			icon: 'contacts',
			thumbnail: null,
			describe: (item) => {
				var p = [];
				if (item.displayName)
					p.push(item.displayName);
				if (typeof item.contactCount === 'number') {
					if (item.contactCount === 0)
						p.push(NIMBUS.translate('ContactsDescription0Contact'));
					else if (item.contactCount === 1)
						p.push(NIMBUS.translate('ContactsDescription1Contact'));
					else
						p.push(NIMBUS.translate('ContactsDescriptionNContacts', item.contactCount));

					if (item.favoriteCount === 0)
						p.push(NIMBUS.translate('ContactsDescription0Favorite'));
					else if (item.favoriteCount === 1)
						p.push(NIMBUS.translate('ContactsDescription1Favorite'));
					else
						p.push(NIMBUS.translate('ContactsDescriptionNFavorites', item.favoriteCount));
				}
				return p.join(', ');
			}
		}],
		actions: [{
			name: 'contacts-open',
			icon: 'contacts',
			caption: 'ContactsOpen',
			accept: accept,
			url: (item) => '/contacts.html?itemId=' + item.id
		}],
		langs: {
			fr: {
				ContactsOpen: "Ouvrir dans l'application Contacts",
				ContactsDescription0Contact: "aucun contact",
				ContactsDescription1Contact: "1 contact",
				ContactsDescriptionNContacts: "{0} contacts",
				ContactsDescription0Favorite: "aucun favori",
				ContactsDescription1Favorite: "1 favori",
				ContactsDescriptionNFavorites: "{0} favoris",
				ContactsTitle: "Contacts",
				ContactsSave: "Sauvegarder les modifications (Ctrl+S)",
				ContactsSearchPlaceholder: "Rechercher des contacts par nom, numéro, adresse mail...",
				ContactsSearchTitle: "Rechercher des contacts (favoris) par noms, société, mot-clef, commentaire, email, numéro ou URL (Ctrl+F)",
				ContactsSearchDisplayName: "Rechercher dans le nom affiché",
				ContactsSearchAllFields: "Rechercher dans tous les champs",
				ContactsSearchFavoriteContacts: "Rechercher dans les favoris",
				ContactsSearchAllContacts: "Rechercher dans tous les contacts",
				ContactsShowCompactGrid: "Afficher la grille compacte",
				ContactsShowDefaultGrid: "Afficher la grille par défaut",
				ContactsNameFormatLastNameFirstName: "Afficher les contacts en Nom Prénom",
				ContactsNameFormatFirstNameLastName: "Afficher les contacts en Prénom Nom",
				ContactsSaveChangesManually: "Sauvegarder manuellement",
				ContactsSaveChangesAutomatically: "Sauvegarder automatiquement",
				ContactsOptionsMenu: "Options",
				ContactsRename: "Renommer le carnet d'adresses actif",
				ContactsRenameTitle: "Renommer en ",
				ContactAddButton: "Ajouter un contact au carnet d'adresses actif",
				ContactImportButton: "Importer un carnet d'adresses Thunderbird",
				ContactEditButton: "Modifier ce contact",
				ContactMarkAsFavoriteButton: "Marquer comme favori",
				ContactUnmarkAsFavoriteButton: "Ne plus marquer comme favori",
				ContactMoveButton: "Déplacer vers \"{0}\"",
				ContactDeleteButton: "Supprimer ce contact",
				ContactModalTitle: "Propriétés du contact",
				ContactModalDisplayNameLabel: "Nom affiché",
				ContactModalDisplayNamePlaceholder: "(désignation du contact dans l'application)",
				ContactModalPictureLabel: "Image",
				ContactModalPicturePlaceholder: "(numéro)",
				ContactModalFavoriteLabel: "Marquer ce contact comme favori",
				ContactModalNamesLegend: "Identité",
				ContactModalFirstNamePlaceholder: "Prénom",
				ContactModalLastNamePlaceholder: "Nom",
				ContactModalNickamePlaceholder: "Surnom",
				ContactModalCompanyLegend: "Société",
				ContactModalCompanyNamePlaceholder: "Société",
				ContactModalCompanyUnitPlaceholder: "Service",
				ContactModalCompanyFunctionPlaceholder: "Fonction",
				ContactModalGenderPlaceholder: "Genre",
				ContactModalPrefixPlaceholder: "Préfixe",
				ContactModalMiddleNamePlaceholder: "2ème prénom",
				ContactModalSuffixPlaceholder: "Suffixe",
				ContactModalKeywordsLabel: "Mots-clefs",
				ContactModalKeywordsPlaceholder: "Ajouter",
				ContactModalNoteLabel: "Note",
				ContactModalNotePlaceholder: "(facultatif)",
				ContactModalCancelButton: "Annuler",
				ContactModalAddButton: "Créer ce contact",
				ContactModalApplyButton: "Appliquer les changements",

				Contact: "Contact",
				ContactFavorite: "Favori",
				ContactDisplayName: "Nom affiché",
				ContactPrefix: "Préfixe",
				ContactFirstName: "Prénom",
				ContactMiddleName: "2ème prénom",
				ContactLastName: "Nom",
				ContactSuffix: "Suffixe",
				ContactNickname: "Surnom",
				ContactCompanyName: "Société",
				ContactCompanyFunction: "Fonction",
				ContactCompanyUnit: "Service",
				ContactPicture: "Image",
				ContactKeywords: "Mots-clefs",
				ContactNote: "Note",

				ContactAddress: "Adresse",
				ContactAddressAdd: "Ajouter une adresse",
				ContactAddressOpenMappy: "Ouvrir avec Mappy",
				ContactAddressOpenGoogleMaps: "Ouvrir avec Google Maps",
				ContactAddressOpenOpenStreetMap: "Ouvrir avec OpenStreetMap",
				ContactAddressAddress: "Adresse",
				ContactAddressAddress2: "Adresse complémentaire",
				ContactAddressZipCode: "Code postal",
				ContactAddressCity: "Ville",
				ContactAddressState: "Région / Province",
				ContactAddressCountry: "Pays",
				ContactAddressClear: "Vider l'adresse",
				ContactAddressTypeHome: "Domicile",
				ContactAddressTypeWork: "Société",
				ContactAddressTypeArchive: "Ancienne adresse",
				ContactAddressTypeOther: "Autre adresse",

				ContactEmail: "Email",
				ContactEmailAdd: "Ajouter un email",
				ContactEmailSend: "Envoyer un email",
				ContactEmailTypeHome: "Personnel",
				ContactEmailTypeWork: "Professionnel",
				ContactEmailTypeArchive: "Ancien email",
				ContactEmailTypeOther: "Autre email",

				ContactPhone: "Numéro",
				ContactPhoneAdd: "Ajouter un numéro",
				ContactPhoneCall: "Appeler ce numéro",
				ContactPhoneTypeHome: "Domicile",
				ContactPhoneTypeWork: "Bureau",
				ContactPhoneTypeMobile: "Mobile",
				ContactPhoneTypeWorkMobile: "Mobile pro",
				ContactPhoneTypeFax: "Fax",
				ContactPhoneTypeWorkFax: "Fax pro",
				ContactPhoneTypePager: "Pager",
				ContactPhoneTypeWorkPager: "Pager pro",
				ContactPhoneTypeWorkStandard: "Standard",
				ContactPhoneTypeArchive: "Ancien numéro",
				ContactPhoneTypeOther: "Autre numéro",

				ContactURL: "Lien",
				ContactURLAdd: "Ajouter un lien",
				ContactURLOpen: "Ouvrir ce lien",
				ContactURLTypeHome: "Domicile",
				ContactURLTypeWork: "Société",
				ContactURLTypeBlog: "Blog",
				ContactURLTypeWebsite: "Site perso",
				ContactURLTypeArchive: "Ancienne URL",
				ContactURLTypeOther: "Autre URL",

				ContactDate: "Date",
				ContactDateAdd: "Ajouter une date",
				ContactDateYear: "AAAA",
				ContactDateMonth: "MM",
				ContactDateDay: "JJ",
				ContactDateFormat: "DD/MM/YYYY",
				ContactDateClear: "Vider la date",
				ContactDateTypeBirthday: "Anniversaire",
				ContactDateTypeFeast: "Fête",
				ContactDateTypeWedding: "Mariage",
				ContactDateTypeDivorce: "Divorce",
				ContactDateTypeDeath: "Décès",
				ContactDateTypeOther: "Autre date",

				ContactGender: "Genre",
				ContactGenderUnspecified: "Non précisé",
				ContactGenderMale: "Homme",
				ContactGenderFemale: "Femme",
				ContactGenderOther: "Autre",

				ContactFieldAdd: "Ajouter un champ personnalisé",
				ContactFieldLabel: "Libellé",
				ContactFieldValue: "Valeur",

				ContactListCopy: "Copier dans le presse-papier",
				ContactListMoveToFirstPosition: "Remonter en premier",
				ContactListMoveToPreviousPosition: "Remonter d'une ligne",
				ContactListMoveToNextPosition: "Descendre d'une ligne",
				ContactListMoveToLastPosition: "Descendre en dernier",
				ContactListRemove: "Supprimer",
			},
			en: {
				ContactsOpen: "Open in Contacts application",
				ContactsDescription0Contact: "no contact",
				ContactsDescription1Contact: "1 contact",
				ContactsDescriptionNContacts: "{0} contacts",
				ContactsDescription0Favorite: "no favorite",
				ContactsDescription1Favorite: "1 favorite",
				ContactsDescriptionNFavorites: "{0} favorites",
				ContactsTitle: "Contacts",
				ContactsSave: "Save modifications (Ctrl+S)",
				ContactsSearchPlaceholder: "Search contacts by name, phone number, email address...",
				ContactsSearchTitle: "Search (favorite) contacts by name, company, keyword, note, email, number or link (Ctrl+F)",
				ContactsSearchDisplayName: "Search in display name only",
				ContactsSearchAllFields: "Search in multiple fields",
				ContactsSearchFavoriteContacts: "Search for favorites only",
				ContactsSearchAllContacts: "Search for any contact",
				ContactsShowCompactGrid: "Show compact grid",
				ContactsShowDefaultGrid: "Show default grid",
				ContactsNameFormatLastNameFirstName: "Show contacts as Lastname Firstname",
				ContactsNameFormatFirstNameLastName: "Show contacts as Firstname Lastname",
				ContactsSaveChangesManually: "Save changes manually",
				ContactsSaveChangesAutomatically: "Save changes automatically",
				ContactsOptionsMenu: "Options",
				ContactsRename: "Rename the selected address book",
				ContactsRenameTitle: "Rename to ",
				ContactAddButton: "Add a new contact to the selected address book",
				ContactImportButton: "Import address book from Thunderbird",
				ContactEditButton: "Edit this contact",
				ContactMarkAsFavoriteButton: "Mark as favorite",
				ContactUnmarkAsFavoriteButton: "Remove favorite indicator",
				ContactMoveButton: "Move to address book \"{0}\"",
				ContactDeleteButton: "Delete this contact",
				ContactModalTitle: "Contact information",
				ContactModalDisplayNameLabel: "Display name",
				ContactModalDisplayNamePlaceholder: "(used to name contact in the application)",
				ContactModalPictureLabel: "Picture",
				ContactModalPicturePlaceholder: "(number)",
				ContactModalFavoriteLabel: "Mark this contact as favorite",
				ContactModalNamesLegend: "Identity",
				ContactModalFirstNamePlaceholder: "Firstname",
				ContactModalLastNamePlaceholder: "Lastname",
				ContactModalNickamePlaceholder: "Nickname",
				ContactModalCompanyLegend: "Company",
				ContactModalCompanyNamePlaceholder: "Company",
				ContactModalCompanyUnitPlaceholder: "Organisation unit",
				ContactModalCompanyFunctionPlaceholder: "Function",
				ContactModalGenderPlaceholder: "Gender",
				ContactModalPrefixPlaceholder: "Prefix",
				ContactModalMiddleNamePlaceholder: "Middle name",
				ContactModalSuffixPlaceholder: "Suffix",
				ContactModalKeywordsLabel: "Keywords",
				ContactModalKeywordsPlaceholder: "Add",
				ContactModalNoteLabel: "Note",
				ContactModalNotePlaceholder: "(optional)",
				ContactModalCancelButton: "Cancel",
				ContactModalAddButton: "Create contact",
				ContactModalApplyButton: "Apply modifications",

				Contact: "Contact",
				ContactFavorite: "Favorite",
				ContactDisplayName: "Display name",
				ContactPrefix: "Prefix",
				ContactFirstName: "First name",
				ContactMiddleName: "Middle name",
				ContactLastName: "Last name",
				ContactSuffix: "Suffix",
				ContactNickname: "Nickname",
				ContactCompanyName: "Company",
				ContactCompanyFunction: "Function",
				ContactCompanyUnit: "Unit",
				ContactPicture: "Picture",
				ContactKeywords: "Keywords",
				ContactNote: "Note",

				ContactAddress: "Address",
				ContactAddressAdd: "Add another address",
				ContactAddressOpenMappy: "Open with Mappy",
				ContactAddressOpenGoogleMaps: "Open with Google Maps",
				ContactAddressOpenOpenStreetMap: "Open with OpenStreetMap",
				ContactAddressAddress: "Address",
				ContactAddressAddress2: "Complementary address",
				ContactAddressZipCode: "Zip code",
				ContactAddressCity: "City",
				ContactAddressState: "State",
				ContactAddressCountry: "Country",
				ContactAddressClear: "Clear address",
				ContactAddressTypeHome: "Home",
				ContactAddressTypeWork: "Company",
				ContactAddressTypeArchive: "Archive",
				ContactAddressTypeOther: "Other",

				ContactEmail: "Email",
				ContactEmailAdd: "Add another email",
				ContactEmailSend: "Send email to this address",
				ContactEmailTypeHome: "Home",
				ContactEmailTypeWork: "Work",
				ContactEmailTypeArchive: "Archive",
				ContactEmailTypeOther: "Other",

				ContactPhone: "Number",
				ContactPhoneAdd: "Add another number",
				ContactPhoneCall: "Call this number",
				ContactPhoneTypeHome: "Home",
				ContactPhoneTypeWork: "Work",
				ContactPhoneTypeMobile: "Mobile",
				ContactPhoneTypeWorkMobile: "Work mobile",
				ContactPhoneTypeFax: "Fax",
				ContactPhoneTypeWorkFax: "Work fax",
				ContactPhoneTypePager: "Pager",
				ContactPhoneTypeWorkPager: "Work pager",
				ContactPhoneTypeWorkStandard: "Standard",
				ContactPhoneTypeArchive: "Archive",
				ContactPhoneTypeOther: "Other",

				ContactURL: "Link",
				ContactURLAdd: "Add another link",
				ContactURLOpen: "Open this link",
				ContactURLTypeHome: "Home",
				ContactURLTypeWork: "Company",
				ContactURLTypeBlog: "Blog",
				ContactURLTypeWebsite: "Website",
				ContactURLTypeArchive: "Archive",
				ContactURLTypeOther: "Other",

				ContactDate: "Date",
				ContactDateAdd: "Add another date",
				ContactDateYear: "YYYY",
				ContactDateMonth: "MM",
				ContactDateDay: "DD",
				ContactDateFormat: "YYYY-MM-DD",
				ContactDateClear: "Clear date",
				ContactDateTypeBirthday: "Birthday",
				ContactDateTypeFeast: "Feast",
				ContactDateTypeWedding: "Wedding",
				ContactDateTypeDivorce: "Divorce",
				ContactDateTypeDeath: "Death",
				ContactDateTypeOther: "Other",

				ContactGender: "Gender",
				ContactGenderUnspecified: "Unspecified",
				ContactGenderMale: "Male",
				ContactGenderFemale: "Female",
				ContactGenderOther: "Other",

				ContactFieldAdd: "Add another field",
				ContactFieldLabel: "Label",
				ContactFieldValue: "Value",

				ContactListCopy: "Copy to clipboard",
				ContactListMoveToFirstPosition: "Move to first position",
				ContactListMoveToPreviousPosition: "Move to previous position",
				ContactListMoveToNextPosition: "Move to next position",
				ContactListMoveToLastPosition: "Move to last position",
				ContactListRemove: "Remove",
			}
		}
	});

})();
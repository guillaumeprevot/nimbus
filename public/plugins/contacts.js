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
		this.name = data.name;
		this.value = data.value;
	}

	function Contact(data) {
		// Indicateur pour marquer un contact comme favori
		this.favorite = data.favorite;
		// Nom utilisé pour afficher le contact dans l'IHM
		this.displayName = data.displayName;
		// Dénomination complète (Mr Professor John Smart Doe Senior)
		this.gender = data.gender;
		this.title = data.title;
		this.firstName = data.firstName;
		this.middleName = data.middleName;
		this.lastName = data.lastName;
		this.suffix = data.suffix;
		// Surnom
		this.nickname = data.nickname;
		// Infos professionnelles (soiété, fonction et service)
		this.companyName = data.companyName;
		this.companyFunction = data.companyFunction;
		this.companyUnit = data.companyUnit;
		// id d'une photo hébergé dans Nimbus
		this.picture = data.picture;
		// Liste de mots-clefs
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

	ContactSource.prototype.sort = function() {
		this.contacts.sort((c1, c2) => formatContact(c1).localeCompare(formatContact(c2)));
		this.sorted = true;
	};

	ContactSource.prototype.save = function() {
		return NIMBUS.utils.updateFileJSON(this.item.id, true, {
			name: this.name,
			contacts: this.contacts
		});
	};

	function accept(item, extension) {
		return 'contacts' === extension;
	}

	// Cette méthode vérifier si le contact correspond au texte recherché
	function matchContact(contact, searchTextLC) {
		return formatContact(contact).toLowerCase().indexOf(searchTextLC) >= 0;
	}

	function formatContact(contact) {
		if (contact.displayName)
			return contact.displayName;
		return (contact.firstName + ' ' + contact.lastName).trim();
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
		formatContact: formatContact,
		formatAddress: formatAddress,
		createMappyLink: (a) => 'https://fr.mappy.com/#/1/M2/TSearch/S' + encodeURI(formatAddress(a)),
		createGoogleMapsLink: (a) => 'https://www.google.com/maps/place/' + encodeURI(formatAddress(a)),
		createOpenStreetMapLink: (a) => 'https://www.openstreetmap.org/search?query=' + encodeURI(formatAddress(a)),
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
				ContactsSave: "Sauvegarder les modifications",
				ContactsSaveError: "Une erreur est survenue. Veuillez vérifier que le serveur est accessible et réessayer.",
				ContactsSearchPlaceholder: "Rechercher des contacts par nom, numéro, adresse mail, ...",
				ContactsRename: "Renommer le carnet d'adresses actif",
				ContactsRenameTitle: "Renommer en ",
				ContactAddButton: "Ajouter un contact au carnet d'adresses actif",
				ContactEditButton: "Modifier ce contact",
				ContactDeleteButton: "Supprimer ce contact",
				ContactModalTitle: "Propriétés du contact",
				ContactModalDisplayNameLabel: "Nom affiché",
				ContactModalDisplayNamePlaceholder: "(utilisé pour désigner le contact dans l'application)",
				ContactModalFavoriteLabel: "Marquer ce contact comme favori",
				ContactModalGenderLabel: "Genre",
				ContactModalNoteLabel: "Note",
				ContactModalNotePlaceholder: "(facultatif)",
				ContactModalCancelButton: "Annuler",
				ContactModalAddButton: "Créer ce contact",
				ContactModalApplyButton: "Appliquer les changements",

				Contact: "Contact",
				ContactFavorite: "Favori",
				ContactDisplayName: "Nom affiché",
				ContactTitle: "Titre",
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
				ContactAddressList: "Adresses",
				ContactAddressAddress: "Adresse",
				ContactAddressAddress2: "Adresse complémentaire",
				ContactAddressZipCode: "Code postal",
				ContactAddressCity: "Ville",
				ContactAddressState: "Région / Province",
				ContactAddressCountry: "Pays",
				ContactAddressTypeHome: "Domicile",
				ContactAddressTypeWork: "Société",
				ContactAddressTypeArchive: "Ancienne adresse",
				ContactAddressTypeOther: "...",

				ContactEmail: "Email",
				ContactEmailList: "Emails",
				ContactEmailTypeHome: "Personnel",
				ContactEmailTypeWork: "Professionnel",
				ContactEmailTypeArchive: "Ancien email",
				ContactEmailTypeOther: "Autre email",

				ContactPhone: "Numéro",
				ContactPhoneList: "Numéros",
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
				ContactURLList: "Liens",
				ContactURLTypeHome: "Domicile",
				ContactURLTypeWork: "Société",
				ContactURLTypeBlog: "Blog",
				ContactURLTypeWebsite: "Site perso",
				ContactURLTypeArchive: "Ancienne URL",
				ContactURLTypeOther: "Autre URL",

				ContactDate: "Date",
				ContactDateList: "Dates",
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

				ContactFieldList: "Champs personnalisés",
				ContactFieldName: "Champ",
				ContactFieldValue: "Valeur",
			},
			en: {
				ContactsOpen: "Open address book",
				ContactsTitle: "Contacts",
				ContactsSave: "Save modifications",
				ContactsSaveError: "An error occurred. Please check your network access and try again.",
				ContactsSearchPlaceholder: "Search contacts by name, phone number, email address, ...",
				ContactsRename: "Rename the selected address book",
				ContactsRenameTitle: "Rename to ",
				ContactAddButton: "Add a new contact to the selected address book",
				ContactEditButton: "Edit this contact",
				ContactDeleteButton: "Delete this contact",
				ContactModalTitle: "Contact information",
				ContactModalDisplayNameLabel: "Display name",
				ContactModalDisplayNamePlaceholder: "(used to name contact in the application)",
				ContactModalFavoriteLabel: "Mark this contact as favorite",
				ContactModalGenderLabel: "Gender",
				ContactModalNoteLabel: "Note",
				ContactModalNotePlaceholder: "(optional)",
				ContactModalCancelButton: "Cancel",
				ContactModalAddButton: "Create contact",
				ContactModalApplyButton: "Apply modifications",

				Contact: "Contact",
				ContactFavorite: "Favorite",
				ContactDisplayName: "Display name",
				ContactTitle: "Title",
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
				ContactAddressList: "Addresses",
				ContactAddressAddress: "Address",
				ContactAddressAddress2: "Complementary address",
				ContactAddressZipCode: "Zip code",
				ContactAddressCity: "City",
				ContactAddressState: "State",
				ContactAddressCountry: "Country",
				ContactAddressTypeHome: "Home",
				ContactAddressTypeWork: "Company",
				ContactAddressTypeArchive: "Archive",
				ContactAddressTypeOther: "Other",

				ContactEmail: "Email",
				ContactEmailList: "Emails",
				ContactEmailTypeHome: "Home",
				ContactEmailTypeWork: "Work",
				ContactEmailTypeArchive: "Archive",
				ContactEmailTypeOther: "Other",

				ContactPhone: "Number",
				ContactPhoneList: "Numbers",
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
				ContactURLList: "Links",
				ContactURLTypeHome: "Home",
				ContactURLTypeWork: "Company",
				ContactURLTypeBlog: "Blog",
				ContactURLTypeWebsite: "Website",
				ContactURLTypeArchive: "Archive",
				ContactURLTypeOther: "Other",

				ContactDate: "Date",
				ContactDateList: "Dates",
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

				ContactFieldList: "Custom fields",
				ContactFieldName: "Label",
				ContactFieldValue: "Value",
			}
		}
	});

})();
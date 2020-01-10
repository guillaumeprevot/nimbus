(function() {

	/** Cette classe est une version allégée de date, telle qu'utilisée dans le calendrier */
	function CalendarDate(year, month, date) {
		// L'année de la date, par exemple 1979 ou 2019
		this.year = year;
		// Le mois de la date, de 0 pour janvier à 11 pour décembre
		this.month = month;
		// Le jour du mois de la date, de 1 à 31
		this.date = date;
	}

	CalendarDate.from = function(year, month, date) {
		if (typeof year === 'string')
			return new CalendarDate(parseInt(year.substr(0, 4)), parseInt(year.substr(5, 2)) - 1, parseInt(year.substr(8, 2)));
		if (year instanceof moment)
			return new CalendarDate(year.year(), year.month(), year.date());
		if (year instanceof Date)
			return new CalendarDate(year.getFullYear(), year.getMonth(), year.getDate());
		if (typeof year === 'object')
			return new CalendarDate(year.year, year.month, year.date);
		return new CalendarDate(year, month, date);
	};

	CalendarDate.prototype.toMoment = function() {
		return moment().year(this.year).month(this.month).date(this.date).startOf('day');
	};

	CalendarDate.prototype.toString = function() {
		return '' + this.year + "-" + (this.month + 1 < 10 ? '0' : '') + (this.month + 1) + '-' + (this.date < 10 ? '0' : '') + this.date;
	};

	/** Cette classe représente un type d'évènement dans le calendrier, avec principalement un nom et une couleur */
	function CalendarEventType(label, color, active, defaultRepeat, editOnCreation) {
		// Le libellé du type d'évènement, pour sélection dans l'IHM
		this.label = label || '';
		// La couleur du type d'évènement, pour affichage dans le calendrier
		this.color = color || '';
		// Un booléen pour indiquer si ce type est encore actif. Sinon, il n'est pas proposé à l'ajout par exemple
		this.active = (typeof active === 'boolean') ? active : true;
		// Un booléen pour indiquer si on ouvre la zone de personnalisation lors de la création
		this.editOnCreation = (typeof editOnCreation === 'boolean') ? editOnCreation : true;
		// La répétition par défaut pour les évènements créés avec ce type. Par exemple "yearly" pour un type anniversaire
		this.defaultRepeat = (typeof defaultRepeat === 'string') ? defaultRepeat : null;
	}

	/** Cette classe représente un évènement dans le calendrier, avec principalement un type, une date avec/sans répétition et un libellé */
	function CalendarEvent(data) {
		// Le type de l'évènement, un objet CalendarEventType
		this.type = data.type;
		// La date de l'évènement, un objet CalendarDate
		this.date = CalendarDate.from(data.date);
		// Le type de répétition, parmi null, 'yearly', 'quarterly', 'monthly', 'weekly' et 'daily'
		this.repeat = data.repeat || null;
		// Le libellé de l'évènement, pour affichage dans le calendrier
		this.label = data.label;
		// Une date de fin, si l'évènement s'étale sur plus d'un jour ou si la répétition doit s'arrêter
		this.endDate = data.endDate ? CalendarDate.from(data.endDate) : null;
		// Un booléen pour indiquer si l'évènement est terminé
		this.done = data.done === true;
		// Un booléen pour indiquer si l'évènement est important
		this.important = data.important === true;
		// Un commentaire libre, s'il l'on souhaite associer d'autres infos à l'évènement
		this.note = data.note || null;
	}

	/** Cette classe représente une vue possible du calendrier (Semaine, Mois, Année ou autres) */
	function CalendarView(data) {
		// le nom de la vue, normalement unique, qui sera utilisé dans l'IHM
		this.name = data.name;
		// function(currentMoment, count) qui ajoute "count" périodes à "currentMoment"
		this.offset = data.offset;
		// function(currentMoment) => moment qui renvoie un nouveau moment représentant le début de la période (de la semaine, du mois, ...)
		this.startMoment = data.startMoment;
		// function(currentMoment) => moment qui renvoie un nouveau moment représentant la fin de la période (de la semaine, du mois, ...)
		this.endMoment = data.endMoment;
		// function(startMoment, endMoment, isLong) => String qui décrit la période allant de startMoment à endMoment (en court ou en long)
		this.formatPeriod = data.formatPeriod;
	}

	/** Cette classe représente une source de données du calendrier (fichiers .calendar mais aussi une source pour les jours fériés ou autres) */
	function CalendarSource(data) {
		// le nom de la source de donnée, normalement unique, qui sera utilisé dans l'IHM
		this.name = data.name;
		// function(name)=>void qui renomme cette source
		this.rename = data.rename;
		// un booléen indiquant si la source est actuellement affichée dans le calendrier (l'utilisateur peut ainsi choisir un sous-ensemble des sources)
		this.active = data.active;
		// function()=>void qui active ou désactive la source
		this.toggle = data.toggle;
		// un booléen indiquant si la source est modifiable (ajout/modification/suppression d'évènements ou de types d'évènement)
		this.readonly = data.readonly;
		// la liste des types d'évènements de cette source de donnée, un tableau de CalendarEventType
		this.types = data.types;
		// function(type)=>int qui compte le nombre d'évènement du type donné
		this.count = data.count;
		// function(searchTextLC)=>CalendarEvent[] qui recherche les évènements correspondant à la recherche
		this.search = data.search;
		// function(startMoment, endMoment)=>Promise<CalendarEvent[]> qui sera appelée quand la période visible doit être calculée et renvoyant les évènements dans la période
		this.populate = data.populate;
		// function(event)=>void qui sera appelée quand un nouvel évènement est créé
		this.add = data.add;
		// function(event)=>void qui sera appelée quand un évènement est supprimé
		this.remove = data.remove;
		// function(type)=>void qui sera appelée pour supprimé tous les évènements d'un type
		this.removeAll = data.removeAll;
		// function()=>Promise qui sera appelée quand la source doit être sauvegardée
		this.save = data.save;
	}

	/** Cette classe représente le calendrier affiché */
	function Calendar() {
		// Un tableau de CalendarView représentant les vues disponibles (par défaut "Semaine" + "Mois" + "Année")
		this.views = [];
		// L'objet CalendarView indiquant la vue sélectionnée (par défaut la première)
		this.view = undefined;
		// Un tableau de CalendarSource représentant les sources disponibles (vide par défaut)
		this.sources = [];
		// Un moment représentant la date en cours (aujourd'hui par défaut)
		this.currentMoment = moment();
	}

	/** Cette méthode permet de reculer d'une période, telle que définie par la vue sélectionnée */
	Calendar.prototype.showPrevious = function() {
		this.view.offset(this.currentMoment, -1);
		this.update();
	};

	/** Cette méthode permet d'avancer d'une période, telle que définie par la vue sélectionnée */
	Calendar.prototype.showNext = function() {
		this.view.offset(this.currentMoment, +1);
		this.update();
	};

	/** Cette méthode cale la période en cours sur la date demandée, selon la vue sélectionnée */
	Calendar.prototype.showMoment = function(m) {
		this.currentMoment = moment(m);
		this.update();
	};

	/** Cette méthode modifie la vue affichée (semaine, mois, année, ...) */
	Calendar.prototype.showView = function(index) {
		this.view = this.views[index];
		this.update();
	};

	/** Cette méthode active une source inactive ou inversement */
	Calendar.prototype.toggleSource = function(source) {
		source.toggle();
		this.update();
	};

	/** Cette méthode indique ajuste le calendrier et prévient ceux qui le souhaite qu'un changement a eu lieu */
	Calendar.prototype.update = function() {
		var startMoment = this.view.startMoment(this.currentMoment);
		var endMoment = this.view.endMoment(this.currentMoment);
		var startOfWeek = moment(startMoment).startOf('week');
		var endOfWeek = moment(endMoment).endOf('week');
		var promises = this.sources.filter((s) => s.active).map((s) => s.populate(startOfWeek, endOfWeek));
		Promise.all(promises).then((values) => {
			$(this).trigger('calendarupdate', {
				currentMoment: this.currentMoment,
				startMoment: startMoment,
				endMoment: endMoment,
				startOfWeek: startOfWeek,
				endOfWeek: endOfWeek,
				view: this.view,
				events: Array.prototype.concat.apply([], values)
			});
		});
	};

	/** Cette méthode retourne la source à laquelle le type donné appartient, ou undefined si non trouvée */
	Calendar.prototype.findSourceOfType = function(type) {
		return this.sources.find(function(source) {
			return source.types.indexOf(type) >= 0;
		});
	};

	/** Cette méthode retourne la source à laquelle l'évènement appartient, ou undefined si non trouvée */
	Calendar.prototype.findSourceOfEvent = function(event) {
		return this.findSourceOfType(event.type);
	};

	/** La vue "Semaine", affichant une semaine à la fois et augmentant / diminuant de 1 semaine */
	function createWeekCalendarView(name) {
		return new CalendarView({
			name: name,
			offset: (currentMoment, count) => currentMoment.add(count, 'weeks'),
			startMoment: (currentMoment) => moment(currentMoment).startOf('week'),
			endMoment: (currentMoment) => moment(currentMoment).endOf('week'),
			formatPeriod: (startMoment, endMoment, isLong) => {
				var format = isLong ? 'LL' : 'L';
				return startMoment.format(format) + ' - ' + endMoment.format(format);
			}
		});
	}

	/** La vue "Multi-semaines", affichant "weekBefore" / "weekAfter" semaines avant / après et augmentant / diminuant de "weekIncrement" semaines */
	function createWeeksCalendarView(name, weekBefore, weekAfter, weekIncrement) {
		return new CalendarView({
			name: name,
			offset: (currentMoment, count) => currentMoment.add(count * weekIncrement, 'weeks'),
			startMoment: (currentMoment) => moment(currentMoment).subtract(weekBefore, 'weeks').startOf('week'),
			endMoment: (currentMoment) => moment(currentMoment).add(weekAfter, 'weeks').endOf('week'),
			formatPeriod: (startMoment, endMoment, isLong) => {
				var format = isLong ? 'LL' : 'L';
				return startMoment.format(format) + ' - ' + endMoment.format(format);
			}
		});
	}

	/** La vue "Mois", affichant un mois à la fois et augmentant / diminuant de 1 mois */
	function createMonthCalendarView(name) {
		return new CalendarView({
			name: name,
			offset: (currentMoment, count) => currentMoment.add(count, 'months'),
			startMoment: (currentMoment) => moment(currentMoment).startOf('month'),
			endMoment: (currentMoment) => moment(currentMoment).endOf('month'),
			formatPeriod: (startMoment, endMoment, isLong) => {
				var format = isLong ? 'MMMM YYYY' : 'MM/YYYY';
				return startMoment.format(format);
			}
		});
	}

	/** La vue "Multi-mois", affichant "monthBefore" / "monthAfter" mois avant / après et augmentant / diminuant de "monthIncrement" mois */
	function createMonthsCalendarView(name, monthBefore, monthAfter, monthIncrement) {
		return new CalendarView({
			name: name,
			offset: (currentMoment, count) => currentMoment.add(count * monthIncrement, 'months'),
			startMoment: (currentMoment) => moment(currentMoment).startOf('month').subtract(monthBefore, 'months'),
			endMoment: (currentMoment) => moment(currentMoment).startOf('month').add(monthAfter, 'months').endOf('month'),
			formatPeriod: (startMoment, endMoment, isLong) => {
				var format = isLong ? 'MMMM YYYY' : 'MM/YYYY';
				return startMoment.format(format) + ' - ' + endMoment.format(format);
			}
		});
	}

	/** La vue "Année", affichant une année complète et se déplaçant d'année en année */
	function createYearCalendarView(name) {
		return new CalendarView({
			name: name,
			offset: (currentMoment, count) => currentMoment.add(count, 'years'),
			startMoment: (currentMoment) => moment(currentMoment).startOf('year'),
			endMoment: (currentMoment) => moment(currentMoment).endOf('year'),
			formatPeriod: (startMoment, endMoment, isLong) => startMoment.format('YYYY')
		});
	}

	/** Cette méthode renvoie le jour de Pâques de l'année donnée, soit la forme d'un tableau [année, mois, jour] */
	function calculateEasterDateArray(year) {
		// https://fr.wikipedia.org/wiki/Calcul_canonique_de_la_date_de_P%C3%A2ques_gr%C3%A9gorienne
		// http://www.developpez.net/forums/d198031/logiciels/microsoft-office/access/access-2000-calcul-jours-ouvr-s-champ-formulaire/
		var a = Math.floor(year % 19)
			b = Math.floor(year / 100)
			c = Math.floor(year % 100)
			d = Math.floor(b / 4)
			e = b % 4
			f = Math.floor((b + 8) / 25)
			g = Math.floor((b - f + 1) / 3)
			h = (19 * a + b - d - g + 15) % 30
			i = Math.floor(c / 4)
			k = c % 4
			l = (32 + 2 * e + 2 * i - h - k) % 7
			m = Math.floor((a + 11 * h + 22 * l) / 451)
			n = Math.floor((h + l - 7 * m + 114) / 31)
			p = (h + l - 7 * m + 114) % 31;
		return [year, n - 1, p + 1];
	}

	/** Une source de données en lecture-seule représentant les jours fériés en France */
	function createFrenchSpecialDaysCalendarSource() {
		var name = NIMBUS.translate('CalendarFrenchSpecialDays');
		var type = new CalendarEventType(name, '#eee', true, 'yearly', true);
		var customActiveKey = 'nimbus-calendar-french-special-days-active';
		var customNameKey = 'nimbus-calendar-french-special-days-name';
		return new CalendarSource({
			name: localStorage.getItem(customNameKey) || name,
			rename: function(newName) {
				this.name = newName || name;
				localStorage.setItem(customNameKey, newName);
			},
			active: localStorage.getItem(customActiveKey) !== 'false',
			toggle: function() {
				this.active = !this.active;
				localStorage.setItem(customActiveKey, this.active.toString());
			},
			readonly: true,
			types: [type],
			count: (type) => 11, /* 11 jours fériés */
			search: (searchTextLC) => [], /*exclus de la recherche*/
			populate: function(startMoment, endMoment) {
				var results = [], startYear = startMoment.year(), endYear = endMoment.year(), year, easter;
				function add(date, label, repeated) {
					results.push(new CalendarEvent({ type: type, date: date, label: label, repeat: repeated ? 'yearly' : null}));
				}
				add(CalendarDate.from(startYear, 0, 1), "Jour de l'an", true);
				add(CalendarDate.from(startYear, 4, 1), "Fête du Travail", true);
				add(CalendarDate.from(startYear, 4, 8), "8 Mai 1945", true);
				add(CalendarDate.from(startYear, 6, 14), "Fête Nationale", true);
				add(CalendarDate.from(startYear, 7, 15), "Assomption", true);
				add(CalendarDate.from(startYear, 10, 1), "Toussaint", true);
				add(CalendarDate.from(startYear, 10, 11), "Armistice 1918", true);
				add(CalendarDate.from(startYear, 11, 25), "Noël", true);
				for (year = startYear; year <= endYear; year++) {
					easter = calculateEasterDateArray(year);
					add(CalendarDate.from(moment(easter).add(1, 'days')), "Lundi de Pâques", false);
					add(CalendarDate.from(moment(easter).add(39, 'days')), "Ascension", false);
					add(CalendarDate.from(moment(easter).add(50, 'days')), "Lundi de Pentecôte", false);
				}
				return Promise.resolve(results);
			},
			add: $.noop,
			remove: $.noop,
			removeAll: $.noop,
			save: $.noop
		});
	}

	/** Une source de données éditable contenue dans un fichier ".calendar" de Nimbus */
	function createNimbusFileCalendarSource(item, forceActive) {
		return $.get('/files/stream/' + item.id).then(undefined, function(result) {
			return { types: [ new CalendarEventType('Type', '#eeeeee', true, null, true) ], events: [] };
		}).then(function(result) {
			var types = result.types.map((t) => new CalendarEventType(t.label, t.color, t.active, t.defaultRepeat, t.editOnCreation));
			var events = result.events.map((e) => new CalendarEvent({
				type: types[e.type],
				date: e.date,
				repeat: e.repeat,
				label: e.label,
				endDate: e.endDate,
				done: e.done,
				important: e.important,
				note: e.note,
			}));
			var defaultName = item.name.replace(/.calendar/gi, '');
			return new CalendarSource({
				name: result.name || defaultName,
				rename: function(newName) {
					this.name = newName || defaultName;
				},
				active: forceActive ? true : (result.active !== false),
				toggle: function() {
					this.active = !this.active;
				},
				readonly: false,
				types: types,
				count: (type) => events.filter((e) => e.type === type).length,
				search: (searchTextLC) => searchTextLC ? events.filter((e) => ((e.label || '') + '_' + (e.type.label)).toLowerCase().indexOf(searchTextLC) >= 0) : events,
				populate: function(startMoment, endMoment) {
					return Promise.resolve(events);
				},
				add: function(event) {
					events.push(event);
				},
				remove: function(event) {
					events.splice(events.indexOf(event), 1);
				},
				removeAll: function(type) {
					events = events.filter((e) => e.type !== type);
				},
				save: function() {
					return NIMBUS.utils.updateFileJSON(item.id, true, {
						name: this.name,
						active: this.active,
						types: types,
						events: events.map((e) => {
							return {
								type: types.indexOf(e.type),
								date: e.date.toString(),
								repeat: e.repeat ? e.repeat : undefined,
								label: e.label ? e.label : undefined,
								endDate: e.endDate ? e.endDate.toString() : undefined,
								done: e.done ? e.done : undefined,
								important: e.important ? e.important : undefined,
								note: e.note ? e.note : undefined,
							};
						})
					});
				}
			});
		});
	}

	function accept(item, extension) {
		return 'calendar' === extension;
	}

	NIMBUS.utils.calendar = {
		Calendar: Calendar,
		CalendarDate: CalendarDate,
		CalendarEvent: CalendarEvent,
		CalendarEventType: CalendarEventType,
		createWeekCalendarView: createWeekCalendarView,
		createWeeksCalendarView: createWeeksCalendarView,
		createMonthCalendarView: createMonthCalendarView,
		createMonthsCalendarView: createMonthsCalendarView,
		createYearCalendarView: createYearCalendarView,
		createFrenchSpecialDaysCalendarSource: createFrenchSpecialDaysCalendarSource,
		createNimbusFileCalendarSource: createNimbusFileCalendarSource,
	};

	NIMBUS.utils.textFileExtensions.push('calendar');

	NIMBUS.plugins.add({
		name: 'calendar',
		properties: [],
		facets: [{
			name: 'calendar',
			accept: accept,
			icon: 'event',
			thumbnail: null,
			describe: (item) => {
				var p = [];
				if (item.displayName)
					p.push(item.displayName);
				if (typeof item.eventCount === 'number') {
					if (item.eventCount === 0)
						p.push(NIMBUS.translate('CalendarDescription0Event'));
					else if (item.eventCount === 1)
						p.push(NIMBUS.translate('CalendarDescription1Event'));
					else
						p.push(NIMBUS.translate('CalendarDescriptionNEvents', item.eventCount));

					if (item.typeCount === 0)
						p.push(NIMBUS.translate('CalendarDescription0Type'));
					else if (item.typeCount === 1)
						p.push(NIMBUS.translate('CalendarDescription1Type'));
					else
						p.push(NIMBUS.translate('CalendarDescriptionNTypes', item.typeCount));
				}
				return p.join(', ');
			}
		}],
		actions: [{
			name: 'calendar-open',
			icon: 'event',
			caption: 'CalendarOpen',
			accept: accept,
			execute: function(item) {
				window.location.assign('/calendar.html?' + $.param({
					itemId: item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}
		}],
		langs: {
			fr: {
				CalendarOpen: "Ouvrir dans l'agenda",
				CalendarDescription0Event: "aucun évènement",
				CalendarDescription1Event: "1 évènement",
				CalendarDescriptionNEvents: "{0} évènements",
				CalendarDescription0Type: "aucun type",
				CalendarDescription1Type: "1 type",
				CalendarDescriptionNTypes: "{0} types",
				CalendarTitle: "Agenda",
				CalendarSave: "Sauvegarder mes modifications",
				CalendarSaveError: "Une erreur est survenue. L'agenda n'est peut-être pas sauvegardé correctement.",
				CalendarSelectDate: "Choisir une date",
				CalendarPreviousPeriod: "Période précédente",
				CalendarNextPeriod: "Période suivante",
				CalendarSelectView: "Choisir la vue",
				CalendarSelectViewWeek: "Semaine",
				CalendarSelectViewWeeks: "Multi-semaines",
				CalendarSelectViewMonth: "Mois",
				CalendarSelectViewMonths: "Multi-mois",
				CalendarSelectViewYear: "Année",
				CalendarSearch: "Rechercher",
				CalendarSearchPlaceholder: "Recherche d'évènements...",
				CalendarSearchNoResult: "Aucun résultat",
				CalendarSelectSource: "Choisir les sources",
				CalendarRenameSourceTitle: "Renommer cette source...",
				CalendarRenameSourcePrompt: "Renommer en",
				CalendarFrenchSpecialDays: "Jours fériés français",
				CalendarRepeatNone: "pas de répétition",
				CalendarRepeatDaily: "chaque jour",
				CalendarRepeatWeekly: "chaque semaine",
				CalendarRepeatMonthly: "chaque mois",
				CalendarRepeatQuarterly: "chaque trimestre",
				CalendarRepeatYearly: "chaque année",
				CalendarAddCustomEventButton: "Ajouter un évènement personnalisé...",
				CalendarArchiveEventButton: "Archiver l'évènement",
				CalendarArchiveEventTitle: "Marque l'évènement comme terminé, supprime l'indicateur d'importance et, pour les évènements répétés, alimente la date de fin",
				CalendarEditEventButton: "Modifier l'évènement...",
				CalendarDeleteEventButton: "Supprimer l'évènement",
				CalendarAddEventTypeTitle: "Ajouter un type d'évènement...",
				CalendarUpEventTypeTitle: "Déplacer vers le haut",
				CalendarDownEventTypeTitle: "Déplacer vers le bas",
				CalendarEditEventTypeTitle: "Modifier le type d'évènement...",
				CalendarDeleteEventTypeTitle: "Supprimer le type d'évènement",
				CalendarDeleteEventTypeUsedOneTimeMessage: "Supprimer ce type et l'évènement associé ?",
				CalendarDeleteEventTypeUsedMultipleTimesMessage: "Supprimer ce type et les {0} évènements associés ?",
				CalendarAutoSave: "Sauvegarde automatique",
				CalendarEventTypeModalTitle: "Type d'évènement",
				CalendarEventTypeLabelLabel: "Intitulé",
				CalendarEventTypeLabelPlaceholder: "(obligatoire)",
				CalendarEventTypeColorLabel: "Couleur",
				CalendarEventTypeColorPlaceholder: "(pensez au mode clair et au mode sombre)",
				CalendarEventTypeRepeatLabel: "Répétition par défaut",
				CalendarEventTypeActiveLabel: "Activer ce type",
				CalendarEventTypeEditOnCreationLabel: "Editer lors de la création",
				CalendarEventTypeCancelButton: "Annuler",
				CalendarEventTypeAddButton: "Ajouter ce type",
				CalendarEventTypeApplyButton: "Appliquer",
				CalendarEventModalTitle: "Evènement",
				CalendarEventModalTypeLabel: "Type d'évènement",
				CalendarEventModalDateLabel: "Date",
				CalendarEventModalDatePlaceholder: "(format aaaa-mm-jj)",
				CalendarEventModalRepeatLabel: "Répétition",
				CalendarEventModalLabelLabel: "Intitulé",
				CalendarEventModalLabelPlaceholder: "(facultatif)",
				CalendarEventModalEndDateLabel: "Date de fin",
				CalendarEventModalEndDatePlaceholder: "(format aaaa-mm-jj)",
				CalendarEventModalDoneLabel: "Marquer comme terminé",
				CalendarEventModalImportantLabel: "Marquer comme important",
				CalendarEventModalNoteLabel: "Note",
				CalendarEventModalNotePlaceholder: "(facultatif)",
				CalendarEventModalCancelButton: "Annuler",
				CalendarEventModalAddButton: "Ajouter",
				CalendarEventModalApplyButton: "Appliquer",

				formatCalendarEventDates: function(event) {
					var d = event.date.toMoment().format('L');
					var r = event.repeat;
					var e = event.endDate ? event.endDate.toMoment().format('L') : null;
					r = (r === 'daily') ? 'jour' : (r === 'weekly') ? 'semaine' : (r === 'monthy') ? 'mois' : (r === 'quarterly') ? 'trimestre' : (r === 'yearly') ? 'année' : '';
					if (e && r)
						return 'chaque ' + r + ' du ' + d + ' au ' + e;
					if (e)
						return 'du ' + d + ' au ' + e;
					if (r)
						return 'chaque ' + r + ' depuis le ' + d;
					return 'le ' + d;
				},
			},
			en: {
				CalendarOpen: "Open in calendar",
				CalendarDescription0Event: "no event",
				CalendarDescription1Event: "1 event",
				CalendarDescriptionNEvents: "{0} events",
				CalendarDescription0Type: "no type",
				CalendarDescription1Type: "1 type",
				CalendarDescriptionNTypes: "{0} types",
				CalendarTitle: "Calendar",
				CalendarSave: "Save modifications",
				CalendarSaveError: "An error occurred. Calendar may not be saved correctly.",
				CalendarSelectDate: "Select date",
				CalendarPreviousPeriod: "Previous period",
				CalendarNextPeriod: "Next period",
				CalendarSelectView: "Select view",
				CalendarSelectViewWeek: "Week",
				CalendarSelectViewWeeks: "Weeks",
				CalendarSelectViewMonth: "Month",
				CalendarSelectViewMonths: "Months",
				CalendarSelectViewYear: "Year",
				CalendarSearch: "Search",
				CalendarSearchPlaceholder: "Search for events...",
				CalendarSearchNoResult: "No result",
				CalendarSelectSource: "Select sources",
				CalendarRenameSourceTitle: "Rename source...",
				CalendarRenameSourcePrompt: "Rename to",
				CalendarFrenchSpecialDays: "French special days",
				CalendarRepeatNone: "do not repeat",
				CalendarRepeatDaily: "repeat each day",
				CalendarRepeatWeekly: "repeat each week",
				CalendarRepeatMonthly: "repeat each month",
				CalendarRepeatQuarterly: "repeat each quarter",
				CalendarRepeatYearly: "repeat each year",
				CalendarAddCustomEventButton: "New custom event...",
				CalendarArchiveEventButton: "Archive event",
				CalendarArchiveEventTitle: "This action marks the event as done and not important. For repeated events, it also fills it's end date.",
				CalendarEditEventButton: "Edit event...",
				CalendarDeleteEventButton: "Delete event",
				CalendarAddEventTypeTitle: "New event type...",
				CalendarUpEventTypeTitle: "Move event type up",
				CalendarDownEventTypeTitle: "Move event type down",
				CalendarEditEventTypeTitle: "Edit event type...",
				CalendarDeleteEventTypeTitle: "Delete event type",
				CalendarDeleteEventTypeUsedOneTimeMessage: "This type is used by one event. Delete anyway, including this event ?",
				CalendarDeleteEventTypeUsedMultipleTimesMessage: "This type is used by {0} events. Delete anyway, including these events ?",
				CalendarAutoSave: "Auto-save changes",
				CalendarEventTypeModalTitle: "Event type properties",
				CalendarEventTypeLabelLabel: "Label",
				CalendarEventTypeLabelPlaceholder: "(required)",
				CalendarEventTypeColorLabel: "Color",
				CalendarEventTypeColorPlaceholder: "(for dark and light mode)",
				CalendarEventTypeRepeatLabel: "Default repeat behaviour",
				CalendarEventTypeActiveLabel: "Enable this type",
				CalendarEventTypeEditOnCreationLabel: "Customize event on creation",
				CalendarEventTypeCancelButton: "Cancel",
				CalendarEventTypeAddButton: "Add type",
				CalendarEventTypeApplyButton: "Apply",
				CalendarEventModalTitle: "Event properties",
				CalendarEventModalTypeLabel: "Event type",
				CalendarEventModalDateLabel: "Date",
				CalendarEventModalDatePlaceholder: "(formated as yyyy-mm-dd)",
				CalendarEventModalRepeatLabel: "Repeat",
				CalendarEventModalLabelLabel: "Label",
				CalendarEventModalLabelPlaceholder: "(optional)",
				CalendarEventModalEndDateLabel: "End date",
				CalendarEventModalEndDatePlaceholder: "(formated as yyyy-mm-dd)",
				CalendarEventModalDoneLabel: "Mark as done",
				CalendarEventModalImportantLabel: "Mark as important",
				CalendarEventModalNoteLabel: "Note",
				CalendarEventModalNotePlaceholder: "(optional)",
				CalendarEventModalCancelButton: "Cancel",
				CalendarEventModalAddButton: "Add event",
				CalendarEventModalApplyButton: "Apply",

				formatCalendarEventDates: function(event) {
					var d = event.date.toMoment().format('L');
					var r = event.repeat;
					var e = event.endDate ? event.endDate.toMoment().format('L') : null;
					if (e && r)
						return 'each ' + r + ' from ' + d + ' until ' + e;
					if (e)
						return 'from ' + d + ' until ' + e;
					if (r)
						return 'each ' + r + ' from ' + d;
					return 'on ' + d;
				},
			}
		}
	});

})();
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
		if (year instanceof moment)
			return new CalendarDate(year.year(), year.month(), year.date());
		if (year instanceof Date)
			return new CalendarDate(year.getFullYear(), year.getMonth(), year.getDate());
		if (year instanceof CalendarDate)
			return new CalendarDate(year.year, year.month, year.date);
		return new CalendarDate(year, month, date);
	};

	/** Cette classe représente un type d'évènement dans le calendrier, avec principalement un nom et une couleur */
	function CalendarEventType(label, color, active, defaultRepeat) {
		// Le libellé du type d'évènement, pour sélection dans l'IHM
		this.label = label;
		// La couleur du type d'évènement, pour affichage dans le calendrier
		this.color = color;
		// Un booléen pour indiquer si ce type est encore actif. Sinon, il n'est pas proposé à l'ajout par exemple
		this.active = (typeof active === 'boolean') ? active : true;
		// La répétition par défaut pour les évènements créés avec ce type. Par exemple "yearly" pour un type anniversaire
		this.defaultRepeat = (typeof defaultRepeat === 'string') ? defaultRepeat : null;
	}

	/** Cette classe représente un évènement dans le calendrier, avec principalement un type, une date avec/sans répétition et une libellé */
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
		this.endDate = CalendarDate.from(data.endDate);
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

	/** Cette classe représente une source de données du calendrier (Fichiers .calendar mais aussi une source pour les jours fériés ou autres) */
	function CalendarSource(data) {
		// le nom de la source de donnée, normalement unique, qui sera utilisé dans l'IHM 
		this.name = data.name;
		// l'icône de la source de donnée, pas forçément unique, qui sera aussi utilisé dans l'IHM
		this.icon = data.icon;
		// un booléen indiquant si la source est actuellement affichée dans le calendrier (l'utilisateur peut ainsi choisir un sous-ensemble des sources) 
		this.active = data.active;
		// un booléen indiquant si la source est modifiable (ajout/modification/suppression d'évènements ou de types d'évènement)
		this.readonly = data.readonly;
		// la liste des types d'évènements de cette source de donnée, un tableau de CalendarEventType
		this.types = data.types || [];
		// function(startMoment, endMoment)=>Deferred<CalendarEvent[]> qui sera appelée quand la période visible doit être calculée et renvoyant les évènements dans la période
		this.populate = data.populate || null;
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

	/** Cette méthode indique ajuste le calendrier et prévient ceux qui le souhaite qu'un changement a eu lieu */
	Calendar.prototype.update = function() {
		this.startMoment = this.view.startMoment(this.currentMoment);
		this.endMoment = this.view.endMoment(this.currentMoment);

		$(this).trigger('calendarupdate', {
			currentMoment: this.currentMoment,
			startMoment: this.startMoment,
			endMoment: this.endMoment,
			view: this.view,
		});
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

	/** La vue "Multi-semaines", affichant "weekCount" semaine à la fois et augmentant / diminuant de "weekIncrement" semaines */
	function createWeeksCalendarView(name, weekCount, weekIncrement) {
		return new CalendarView({
			name: name,
			offset: (currentMoment, count) => currentMoment.add(count * weekIncrement, 'weeks'),
			startMoment: (currentMoment) => moment(currentMoment).startOf('week'),
			endMoment: (currentMoment) => moment(currentMoment).add(weekCount - 1, 'weeks').endOf('week'),
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

	/** La vue "Multi-mois", affichant "monthCount" mois à la fois et augmentant / diminuant de "monthIncrement" mois */
	function createMonthsCalendarView(name, monthCount, monthIncrement) {
		return new CalendarView({
			name: name,
			offset: (currentMoment, count) => currentMoment.add(count * monthIncrement, 'months'),
			startMoment: (currentMoment) => moment(currentMoment).startOf('month'),
			endMoment: (currentMoment) => moment(currentMoment).startOf('month').add(monthCount, 'months').subtract(1, 'days'),
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

	/** Une source de données éditable contenue dans un fichier ".calendar" de Nimbus */
	function createNimbusFileCalendarSource(item, active) {
		return $.get('/files/stream/' + item.id).then(function(result) {
			console.log(item.name, result);
			return new CalendarSource({
				name: item.name.replace(/.calendar/gi, ''),
				icon: 'event',
				active: active,
				readonly: false,
				types: [], // TODO
				populate: function(startMoment, endMoment) {
					return $.Deferred().resolve([]); // TODO
				},
			});
		});
	}

	function accept(item, extension) {
		return 'calendar' === extension;
	}

	NIMBUS.utils.calendar = {
		Calendar: Calendar,
		createWeekCalendarView: createWeekCalendarView,
		createWeeksCalendarView: createWeeksCalendarView,
		createMonthCalendarView: createMonthCalendarView,
		createMonthsCalendarView: createMonthsCalendarView,
		createYearCalendarView: createYearCalendarView,
		createNimbusFileCalendarSource: createNimbusFileCalendarSource,
	};

	NIMBUS.plugins.add({
		name: 'calendar',
		properties: [],
		facets: [{
			name: 'calendar',
			accept: accept,
			icon: 'event',
			thumbnail: null,
			describe: (item) => ''
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
				CalendarTitle: "Agenda",
				CalendarSave: "Sauvegarder mes modifications",
				CalendarClose: "Fermer l'agenda",
			},
			en: {
				CalendarOpen: "Open in calendar",
				CalendarTitle: "Calendar",
				CalendarSave: "Save modifications",
				CalendarClose: "Close calendar",
			}
		}
	});

})();
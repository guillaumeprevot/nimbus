(function($) {
	"use strict";

	function TextEditor(property, placeholder, inputType, format) {
		this.build = (item) => $('<input class="form-control" style="flex-basis: 0; color: black; " />')
			.attr('type', inputType || 'text')
			.attr('placeholder', placeholder || '...')
			.val(item[property] || '')
			.on('change', (event) => void (format && (event.target.value = format(event.target.value))));
		this.empty = (editor) => !editor.val();
		this.apply = (item, editor) => item[property] = editor.val() || undefined;
		this.text = (editor) => editor.val() || '';
	}

	function ButtonEditor(text, onclick, onempty, onapply) {
		this.build = (item) => $('<button type="button" class="btn btn-outline-secondary form-control-plaintext" style="border-radius: 0; text-align: left; white-space: nowrap; color: black; " />')
			.text((typeof text === 'function') ? text(item) : text)
			.on('click', (event) => onclick(item, event.target.closest('button')));
		this.empty = (editor) => onempty(editor);
		this.apply = (item, editor) => onapply(item, editor);
	}

	function DateEditor(format, placeholder, clearButton, yearPlaceholder, monthPlaceholder, datePlaceholder) {
		function showValue(item, editor) {
			var y = '' + (item.year || '');
			var m = '' + (item.month || '');
			var d = '' + (item.date || '');
			var text = format
				.replace('YYYY', y)
				.replace('MM', (m.length === 1 ? '0' : '') + m).replace('M', m)
				.replace('DD', (d.length === 1 ? '0' : '') + d).replace('D', d)
				.replace(/\/\//, '/').replace(/^\//, '').replace(/\/$/, '')
				.replace(/--/, '-').replace(/^-/, '').replace(/-$/, '');
			editor.children('span').text(text || placeholder).css('color', text ? 'black' : '');
		}
		this.build = (item) => {
			var editor = $(''
					+ '<div class="button-group" style="position: relative; flex-basis: 1.5rem; ">'
					+ '  <span class="form-control" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" style="position: absolute; cursor: pointer; border-radius: 0; text-align: left; white-space: nowrap; overflow: hidden; color: black; "></span>'
					+ '  <div class="dropdown-menu">'
					+ '    <div class="p-2 form-row">'
					+ '      <div class="col-md-3"><input type="text" min="1" max="31" class="form-control gp-valuelist-date" placeholder="JJ"></div>'
					+ '      <div class="col-md-3"><input type="text" min="1" max="12" class="form-control gp-valuelist-month" placeholder="MM"></div>'
					+ '      <div class="col-md-4"><input type="text" min="1900" class="form-control gp-valuelist-year" placeholder="AAAA"></div>'
					+ '      <div class="col-md-2"><button type="button" class="btn btn-link btn-sm btn-block px-0 text-danger" title="Clear"><i class="material-icons">delete</i></button></div>'
					+ '    </div>'
					+ '  </div>'
					+ '</div>');
			showValue(item, editor);
			editor.find('.gp-valuelist-date').val(item.date || '').attr('placeholder', datePlaceholder);
			editor.find('.gp-valuelist-month').val(item.month || '').attr('placeholder', monthPlaceholder);
			editor.find('.gp-valuelist-year').val(item.year || '').attr('placeholder', yearPlaceholder);
			editor.find('.text-danger').attr('title', clearButton).on('click', () => {
				editor.find('input').val('');
				editor.children('span').text(placeholder);
			});
			editor.on('hidden.bs.dropdown', function() {
				var item = {};
				this.apply(item, editor);
				showValue(item, editor);
			}.bind(this));
			return editor;
		};
		this.empty = (editor) => {
			return !editor.find('input').get().map((input) => input.value || '').join('');
		};
		this.apply = (item, editor) => {
			item.year = editor.find('.gp-valuelist-year').val();
			item.year = item.year ? parseInt(item.year) : undefined;
			item.month = editor.find('.gp-valuelist-month').val();
			item.month = item.month ? parseInt(item.month) : undefined;
			item.date = editor.find('.gp-valuelist-date').val();
			item.date = item.date ? parseInt(item.date) : undefined;
		};
		this.text = (editor) => {
			var t = editor.children('span').text();
			return t === placeholder ? '' : t;
		};
	}

	function CopyToClipboardAction(label) {
		this.enabled = document.queryCommandSupported('copy');
		this.build = (inputGroup) => {
			var button = $('<button type="button" class="dropdown-item"><i class="material-icons">assignment</i> <span></span></button>');
			button.children('span').text(label);
			return button;
		};
		this.click = (event, text) => {
			var inputGroup, addButton, input;
			if (text) {
				inputGroup = $(event.target).closest('.input-group');
				addButton = inputGroup.siblings('button');
				input = $('<input type="text" />').val(text.trim()).insertAfter(addButton);
				input.select().focus();
				document.execCommand('copy');
				input.remove();
				inputGroup.find('button.input-group-text').focus();
			}
		};
	}

	function LinkAction(icon, label, format) {
		this.enabled = true;
		this.build = (inputGroup) => {
			var a = $('<a href="#" target="_blank" class="dropdown-item"><i class="material-icons"></i> <span></span></button>');
			a.children('i').text(icon);
			a.children('span').text(label);
			return a;
		};
		this.click = (event, text) => {
			var a = $(event.target).closest('a');
			if (!text)
				a.attr('href', '#');
			else if (typeof format === 'string')
				a.attr('href', encodeURI(format.replace('%VALUE%', text)));
			else
				a.attr('href' , format(text));
			if (! text)
				return false;
		};
	}

	var draggable = null;

	function ValueList(target, options) {
		var self = this;
		this.target = $(target).addClass('gp-valuelist');
		this.options = $.extend({}, ValueList.defaultOptions, options);
		// Ajout d'un bouton pour ajouter une entrée
		this.addButton = $('<button type="button" class="btn btn-link" />')
			.text(options.addText || '')
			.appendTo(target)
			.toggle(!!options.addText)
			.on('click', () => self.append({}));
		// Ajout d'une ligne pour chaque entrée existante
		if (options.items)
			options.items.forEach((value) => self.append(value));
		// Ajout d'une ligne par défaut pour faciliter la création, si demandé
		if (this.options.addDefault === 'always' || (this.options.addDefault === 'empty' && (!options.items || options.items.length === 0)))
			this.append({});
		// Gestion des actions personnalisées
		if (this.options.actions)
			this.target.on('click', '.input-group .gp-valuelist-action', this.executeAction.bind(this));
		// Gestion du bouton pour remonter en premier
		this.target.on('click', '.input-group:not(:first-child) .gp-valuelist-first', this.moveEntryFirst);
		// Gestion du bouton pour remonter d'une ligne
		this.target.on('click', '.input-group:not(:first-child) .gp-valuelist-previous', this.moveEntryPrevious);
		// Gestion du bouton pour descendre d'une ligne
		this.target.on('click', '.input-group:not(:last-of-type) .gp-valuelist-next', this.moveEntryNext);
		// Gestion du bouton pour descendre en dernier
		this.target.on('click', '.input-group:not(:last-of-type) .gp-valuelist-last', this.moveEntryLast);
		// Gestion du bouton pour supprimer une entrée
		this.target.on('click', '.input-group .gp-valuelist-remove', this.removeEntry);
		// Gestion du tri par DnD de l'icone en 'prepend'
		this.enableDnD();
	}

	$.extend(ValueList.prototype, {
		destroy: function() {
			this.target.empty().removeClass('gp-valuelist').off('click dragstart dragover drop dragend', '**');
		},
		executeAction: function(event) {
			var button = $(event.target).closest('.gp-valuelist-action');
			var action = button.data('action');
			var entry = button.closest('.input-group');
			var editor = entry.children().eq(1);
			var text = this.options.editor.text(editor);
			action.click(event, text);
		},
		moveEntryFirst: function(event) {
			var entry = $(event.target).closest('.input-group');
			if (entry.index() > 0)
				entry.insertBefore(entry.siblings(':first-child'));
		},
		moveEntryPrevious: function(event) {
			var entry = $(event.target).closest('.input-group');
			if (entry.index() > 0)
				entry.insertBefore(entry.prev());
		},
		moveEntryNext: function(event) {
			var entry = $(event.target).closest('.input-group');
			if (entry.next().length > 0)
				entry.insertAfter(entry.next());
		},
		moveEntryLast: function(event) {
			var entry = $(event.target).closest('.input-group');
			if (entry.next().length > 0)
				entry.insertBefore(entry.siblings(':last-child'));
		},
		removeEntry: function(event) {
			$(event.target).closest('.input-group').remove();
		},
		enableDnD: function() {
			this.target.on('dragstart', '.input-group-prepend', function(event) {
				draggable = event.target;
			}).on('dragover', '.input-group-prepend', function(event) {
				if (draggable === null)
					return;
				var dropTarget = $(event.target).closest('.gp-valuelist');
				if (dropTarget.length !== 1 || !dropTarget.has(draggable).length)
					return;
				return false;
			}).on('drop', '.input-group-prepend', function(event) {
				var drag = $(draggable).closest('.input-group');
				var drop = $(event.target).closest('.input-group');
				var dragIndex = drag.index();
				var dropIndex = drop.index();
				if (dragIndex > dropIndex)
					drag.insertBefore(drop);
				else if (dragIndex < dropIndex)
					drag.insertAfter(drop);
				draggable = null;
				return false;
			}).on('dragend', '.input-group-prepend', function(event) {
				draggable = null;
			});
		},
		append: function(item) {
			// Le conteneur
			var div = $(''
					+ '<div class="input-group">'
					+ '  <div class="input-group-prepend" draggable="true">'
					+ '    <span class="input-group-text"><i class="material-icons material-icons-16"></i></span>'
					+ '  </div>'
					+ '  <input type="text" class="form-control" style="flex: 1 1 0; "></input>'
					+ '  <span class="form-control" style="flex: 1 1 0; display: none; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; "></span>'
					+ '  <select class="custom-select" style="width: auto; flex: 0 0 auto; width: 0; color: transparent; "></select>'
					+ '  <div class="input-group-append">'
					+ '    <button type="button" class="input-group-text btn btn-link" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false"><i class="material-icons material-icons-16">expand_more</i></button>'
					+ '    <div class="dropdown-menu dropdown-menu-right">'
					+ '      <button type="button" class="dropdown-item gp-valuelist-first"><i class="material-icons">expand_less</i> <span>' + this.options.moveToFirstPositionText + '</span></button>'
					+ '      <button type="button" class="dropdown-item gp-valuelist-previous"><i class="material-icons">arrow_drop_up</i> <span>' + this.options.moveToPreviousPositionText + '</span></button>'
					+ '      <button type="button" class="dropdown-item gp-valuelist-next"><i class="material-icons">arrow_drop_down</i> <span>' + this.options.moveToNextPositionText + '</span></button>'
					+ '      <button type="button" class="dropdown-item gp-valuelist-last"><i class="material-icons">expand_more</i> <span>' + this.options.moveToLastPositionText + '</span></button>'
					+ '      <div role="separator" class="dropdown-divider"></div>'
					+ '      <button type="button" class="dropdown-item gp-valuelist-remove text-danger"><i class="material-icons">delete</i> <span>' + this.options.removeText + '</span></button>'
					+ '    </div>'
					+ '  </div>'
					+ '</div>');
			// Tout à gauche, l'icone, servant aussi au tri
			var icon = div.find('.input-group-prepend i').text(this.options.icon);
			// Ensuite, la zone de saisie gérée par l'éditeur
			var editor = this.options.editor.build(item).css('flex-grow', '2.5').css('flex-shrink', '2.5').insertAfter(div.children(':first-child'));
			// Ensuite, la zone de libellé personnalisé
			var labelInput = editor.next().attr('placeholder', this.options.labelPlaceholder).val(item[this.options.labelProperty] || '');
			// Ensuite, le "span" affichant le type prédéfini, s'il est sélectionné
			var defaultTypeSpan = labelInput.next();
			// Enfin, le select pour choisir un type prédéfini
			var typeSelect = defaultTypeSpan.next();
			// Et le menu pour les actions
			var actionMenu = typeSelect.next().children('.dropdown-menu');
			var actionMenuSeparator;

			if (this.options.actions) {
				actionMenuSeparator = $('<div role="separator" class="dropdown-divider"></div>').prependTo(actionMenu);
				this.options.actions.filter((action) => action.enabled).forEach((action, index) => action.build(div).addClass('gp-valuelist-action').data('action', action).insertBefore(actionMenuSeparator));
			}
			if (this.options.types) {
				// Ajouter les types prédéfinis dans le select
				this.options.types.forEach((type) => $('<option style="color: initial; "/>').attr('value', type.value).text(type.text).data('type', type).prop('selected', type.label).appendTo(typeSelect));
				// Ajuster la visibilité des zones pour le type (en lecture-seule ou en saisie)
				typeSelect.on('change', function() {
					var option = typeSelect.children(':selected');
					var type = option.data('type');
					defaultTypeSpan.text(type.text).toggle(!type.label);
					labelInput.toggle(!!type.label);
				});
				// Positionner par défaut sur le type en cours
				if (item[this.options.typeProperty])
					typeSelect.val(item[this.options.typeProperty]).change();
			} else {
				// Marquer les composants servant à gérer des types prédéfinis, s'il n'y e na pas
				typeSelect.hide();
			}
			// Ajout du div dans la liste
			div.insertBefore(this.addButton);
		},
		extract: function() {
			return this.target.children('.input-group').map(function(index, div) {
				var editorClass = this.options.editor;
				var editorElement = $(div).children(':eq(1)');
				if (editorClass.empty(editorElement))
					return;
				var result = {};
				// La valeur spécifique
				editorClass.apply(result, editorElement);
				// Le type prédéfini
				var typeSelect = editorElement.siblings('select');
				if (this.options.types)
					result[this.options.typeProperty] = typeSelect.val();
				// Le libellé personnalisé
				var labelInput = editorElement.siblings('input');
				if (labelInput.is(':visible'))
					result[this.options.labelProperty] = labelInput.val();
				return result;
			}.bind(this)).get();
		}
	});

	ValueList.TextEditor = TextEditor;
	ValueList.ButtonEditor = ButtonEditor;
	ValueList.DateEditor = DateEditor;
	ValueList.CopyToClipboardAction = CopyToClipboardAction;
	ValueList.LinkAction = LinkAction;

	ValueList.defaultOptions = {
		// Material icon affiché en début de ligne (email, phone, ...)
		icon: 'drag_handle',
		// Liste d'élément pour alimenter la liste initiale
		items: [],
		// Edition (par défaut de la propriété texte 'value')
		editor: new TextEditor('value', 'Value', 'text'),
		// Placeholder de la zone de libellé personnalisé
		labelPlaceholder: 'Label',
		// Nom de la propriété contenant le libellé personnalisé, si défini
		labelProperty: 'label',
		// Liste des types prédéfinis (text:string, value:string, label:boolean)
		types: undefined,
		// Nom de la propriété contenant le type sélectionné
		typeProperty: 'type',
		// Traduction du bouton d'ajout
		addText: 'Add',
		// Indique quand ajouter une par défaut (au choix parmi 'never', 'empty' et 'always')
		addDefault: 'empty',
		// Liste des actions personnalisées
		actions: [],
		// Traduction des boutons servant à organiser la liste
		moveToFirstPositionText: 'Move to first position',
		moveToPreviousPositionText: 'Move to previous position',
		moveToNextPositionText: 'Move to next position',
		moveToLastPositionText: 'Move to last position',
		// Traduction du bouton permettant de supprimer une entrée
		removeText: 'Remove',
	};

	window.GP.ValueList = ValueList;

	$.addPlugin('gpvaluelist', ValueList);

})(jQuery);

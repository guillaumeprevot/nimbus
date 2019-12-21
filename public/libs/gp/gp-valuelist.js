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
	}

	function ButtonEditor(text, onclick, onempty, onapply) {
		this.build = (item) => $('<button type="button" class="btn btn-outline-secondary form-control-plaintext" style="border-radius: 0; text-align: left; white-space: nowrap; color: black; " />')
			.text((typeof text === 'function') ? text(item) : text)
			.on('click', (event) => onclick(item, event.target.closest('button')));
		this.empty = (editor) => onempty(editor);
		this.apply = (item, editor) => onapply(item, editor)
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
					+ '  <span class="form-control" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" style="position: absolute; cursor: pointer; border-radius: 0; text-align: left; white-space: nowrap; overflow: hidden; color: black; " />'
					+ '  <div class="dropdown-menu">'
					+ '    <div class="p-2 form-row">'
					+ '      <div class="col-md-3"><input type="text" min="1" max="31" class="form-control valuelist-date" placeholder="JJ"></div>'
					+ '      <div class="col-md-3"><input type="text" min="1" max="12" class="form-control valuelist-month" placeholder="MM"></div>'
					+ '      <div class="col-md-4"><input type="text" min="1900" class="form-control valuelist-year" placeholder="AAAA"></div>'
					+ '      <div class="col-md-2"><button type="button" class="btn btn-link btn-sm btn-block px-0 text-danger" title="Clear"><i class="material-icons">delete</i></button></div>'
					+ '    </div>'
					+ '  </div>'
					+ '</div>');
			showValue(item, editor);
			editor.find('.valuelist-date').val(item.date || '').attr('placeholder', datePlaceholder);
			editor.find('.valuelist-month').val(item.month || '').attr('placeholder', monthPlaceholder);
			editor.find('.valuelist-year').val(item.year || '').attr('placeholder', yearPlaceholder);
			editor.find('.btn-outline-danger').attr('title', clearButton).on('click', () => {
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
			item.year = editor.find('.valuelist-year').val();
			item.year = item.year ? parseInt(item.year) : undefined;
			item.month = editor.find('.valuelist-month').val();
			item.month = item.month ? parseInt(item.month) : undefined;
			item.date = editor.find('.valuelist-date').val();
			item.date = item.date ? parseInt(item.date) : undefined;
		};
	}

	var draggable = null;

	function ValueList(target, options) {
		var self = this;
		this.target = $(target).addClass('valuelist').css('margin-bottom', '1rem');
		this.values = this.target.data('valuelist');
		this.options = $.extend({}, ValueList.defaultOptions, options);
		// Ajout d'un bouton pour ajouter une entrée
		this.addButton = $('<button type="button" class="btn btn-link" />')
			.text(options.addText || '')
			.appendTo(target)
			.toggle(!!options.addText)
			.on('click', () => self.append({}));
		// Ajout d'une ligne pour chaque entrée existante
		this.values.forEach((value) => self.append(value));
		// Ajout d'une ligne par défaut pour faciliter la création, si demandé
		if (this.options.addDefault === 'always' || (this.options.addDefault === 'empty' && this.values.length === 0))
			this.append({});
		// Gestion du bouton pour remonter une entrée
		this.target.on('click', '.input-group:gt(0) .valuelist-up', function(event) {
			var entry = $(event.target).closest('.input-group');
			if (entry.index() > 0)
				entry.insertBefore(entry.prev());
		});
		// Gestion du bouton pour descendre une entrée
		this.target.on('click', '.input-group:lt(-1) .valuelist-down', function(event) {
			var entry = $(event.target).closest('.input-group');
			if (entry.next().length > 0)
				entry.insertAfter(entry.next());
		});
	}

	$.extend(ValueList.prototype, {
		destroy: function() {
			this.target.empty().removeClass('valuelist');
		},
		append: function(item) {
			// Le conteneur
			var div = $(''
					+ '<div class="input-group">'
					+ '  <div class="input-group-prepend">'
					+ '    <span class="input-group-text"><i class="material-icons material-icons-16"></i></span>'
					+ '  </div>'
					+ '  <input type="text" class="form-control" style="flex: 1 1 0; " />'
					+ '  <span class="form-control" style="flex: 1 1 0; display: none; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; "></span>'
					+ '  <select class="custom-select" style="width: auto; flex: 0 0 auto; width: 0; color: transparent; "></select>'
					+ '  <div class="input-group-append">'
					+ '    <button type="button" class="input-group-text btn btn-link valuelist-up"><i class="material-icons material-icons-16">keyboard_arrow_up</i></button>'
					+ '    <button type="button" class="input-group-text btn btn-link valuelist-down"><i class="material-icons material-icons-16">keyboard_arrow_down</i></button>'
					+ '  </div>'
					+ '</div>');
			// Tout à gauche, l'icone, servant aussi au tri
			var icon = div.find('.input-group-prepend i').text(this.options.icon);
			// Ensuite, la zone de saisie gérée par l'éditeur
			var editor = this.options.editor.build(item).css('flex-grow', '3').css('flex-shrink', '3').insertAfter(div.children(':first-child'));
			// Ensuite, la zone de libellé personnalisé
			var labelInput = editor.next().attr('placeholder', this.options.labelPlaceholder).val(item[this.options.labelProperty] || '');
			// Ensuite, le "span" affichant le type prédéfini, s'il est sélectionné
			var defaultTypeSpan = labelInput.next();
			// Enfin, le select pour choisir un type prédéfini
			var typeSelect = defaultTypeSpan.next();

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

	ValueList.defaultOptions = {
		// Material icon affiché en début de ligne (email, phone, ...)
		icon: 'drag_handle',
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
		// addText: 'Add',
		// Indique quand ajouter une par défaut (au choix parmi 'never', 'empty' et 'always')
		addDefault: 'empty',
	};

	window.ValueList = ValueList;

	$.addPlugin('valueList', ValueList);

})(jQuery);

(function() {

	var supports = {
		application: { mode: 'xml,javascript,css,htmlmixed', mime: 'text/html', icon: 'settings_applications' },
		asc: { mode: 'asciiarmor', mime: 'application/pgp', alias: 'pgp,sig' },
		c: { mode: 'clike', mime: 'text/x-csrc', alias: 'h,ino' },
		calendar: { mode: 'javascript', mime: 'application/json', icon: 'event' },
		coffee: { mode: 'coffeescript', mime: 'application/vnd.coffeescript' },
		contacts: { mode: 'javascript', mime: 'application/json', icon: 'contacts' },
		cpp: { mode: 'clike', mime: 'text/x-c++src', alias: 'c++,cc,cxx,hpp,h++,hh,hxx' },
		cs: { mode: 'clike', mime: 'text/x-csharp' },
		css: { mode: 'css', mime: 'text/css' },
		diff: { mode: 'diff', mime: 'text/x-diff', alias: 'patch' },
		dockerfile: { mode: 'dockerfile', mime: 'text/x-dockerfile' },
		dtd: { mode: 'dtd', mime: 'application/xml-dtd' },
		go: { mode: 'go', mime: 'text/x-go' },
		groovy: { mode: 'groovy', mime: 'text/x-groovy', alias: 'gradle' },
		html: { mode: 'xml,javascript,css,htmlmixed', mime: 'text/html', alias: 'htm' },
		ini: { mode: 'properties', mime: 'text/x-ini' },
		java: { mode: 'clike', mime: 'text/x-java' },
		js: { mode: 'javascript', mime: 'text/javascript' },
		json: { mode: 'javascript', mime: 'application/json', alias: 'map' },
		jsp: { mode: 'htmlembedded', mime: 'application/x-jsp' },
		kt: { mode: 'clike', mime: 'text/x-kotlin' },
		less: { mode: 'css', mime: 'text/x-less' },
		lua: { mode: 'lua', mime: 'text/x-lua' },
		md: { mode: 'markdown', mime: 'text/x-markdown', icon: 'list_alt', alias: 'markdown' },
		note: { mode: 'xml,htmlmixed', mime: 'text/html', icon: 'art_track' },
		nsh: { mode: 'nsis', mime: 'text/x-nsis' },
		pas: { mode: 'pascal', mime: 'text/x-pascal' },
		php: { mode: 'xml,javascript,css,htmlmixed,clike,php', mime: 'text/x-php' },
		pl: { mode: 'perl', mime: 'text/x-perl' },
		properties: { mode: 'properties', mime: 'text/x-properties' },
		ps1: { mode: 'powershell', mime: 'application/x-powershell', alias: 'psd1,psm1' },
		py: { mode: 'python', mime: 'text/x-python', alias: 'pyw,bzl' },
		rb: { mode: 'ruby', mime: 'text/x-ruby', alias: 'jruby,macruby,rake,rbx' },
		rs: { mode: 'rust', mime: 'text/x-rustsrc' },
		sass: { mode: 'sass', mime: 'text/x-sass' },
		scala: { mode: 'clike', mime: 'text/x-scala' },
		scss: { mode: 'css', mime: 'text/x-scss' },
		sh: { mode: 'shell', mime: 'text/x-sh', alias: 'ksh,bash,zsh' },
		sql: { mode: 'sql', mime: 'text/x-sql', alias: 'pls' },
		stex: { mode: 'stex', mime: 'text/x-stex' },
		tex: { mode: 'stex', mime: 'text/x-latex', alias: 'ltx' },
		ts: { mode: 'javascript', mime: 'application/typescript' },
		txt: { mode: 'null', mime: 'text/plain', icon: 'subject', alias: 'conf,log' },
		vue: { mode: 'vue', mime: 'script/x-vue' },
		xml: { mode: 'xml', mime: 'application/xml', alias: 'xsl,xsd,svg,rss,atom,wsdl' },
		yml: { mode: 'yaml', mime: 'text/x-yaml' },
	};

	for (var ext in supports) {
		if (! supports.hasOwnProperty(ext))
			continue;
		var def = supports[ext];
		if (! def.alias)
			continue;
		var aliases = def.alias;
		delete def.alias;
		aliases.split(',').forEach(function(alias) {
			supports[alias] = def;
		});
	}

	function accept(item, extension) {
		return supports.hasOwnProperty(extension);
	}

	function loadMode(mode) {
		var src, element;
		if (mode === 'null')
			return;
		src = '/libs/codemirror/mode/' + mode + '/' + mode + '.js';
		element = document.querySelector('script[src="' + src + '"]');
		if (element)
			return;
		$('<script type="text/javascript" />').attr('src', src).appendTo(document.head);
	}

	function loadSupport(extension) {
		// Get file support definition (shared by 'code.js' into the NIMBUS.utils object)
		var support = supports[extension];
		// Load CodeMirror mode scripts
		if (support)
			support.mode.split(',').forEach(loadMode);
		return support;
	}

	function createOptions(mime, content, theme) {
		// https://codemirror.net/doc/manual.html#config
		var options = {
			value: content,
			mode: mime || '',
			theme: 'nimbus-' + theme,
			indentUnit: 4,
			indentWithTabs: true,
			// direction: 'ltr' 'rtl',
			// rtlMoveVisually: true false,
			// lineWrapping: false,
			lineNumbers: true,
			// firstLineNumber: 1,
			// lineNumberFormatter: fn,
			gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter'],
			foldGutter: true,
			// dragDrop: false,
			cursorScrollMargin: 40, // pour avoir 2 lignes au dessus et en dessous de la sélection lors du scroll
			viewportMargin: Infinity,
			autofocus: true,

			styleActiveLine: true,
			continueComments: true,
			matchBrackets: true,
			autoCloseBrackets: true,
			matchTags: true,
			autoCloseTags: true,
			showTrailingSpace: true,

			// Defaults keymap : https://github.com/codemirror/CodeMirror/blob/master/src/input/keymap.js
			// keyMap: 'default',
			extraKeys: {
				'Tab': 'indentMore',
				'Shift-Tab': 'indentLess',
				'Ctrl-Space': 'autocomplete',
				'Ctrl-Alt-C': 'toggleComment',
				'Ctrl-L': 'jumpToLine',
				'Ctrl-Q': function(cm) { cm.foldCode(cm.getCursor()); }
			},
			phrases: {
				'Jump to line:': NIMBUS.translate('CodeEditorJumpToLinePrompt'),
				'(Use line:column or scroll% syntax)': NIMBUS.translate('CodeEditorJumpToLineFormat')
			}
		};
		if (mime === 'text/x-markdown')
			options.extraKeys['Enter'] = 'newlineAndIndentContinueMarkdownList';
		return options;
	}

	NIMBUS.utils.codeMirrorLoadSupport = loadSupport;
	NIMBUS.utils.codeMirrorCreateOptions = createOptions;

	NIMBUS.plugins.add({
		name: 'code',
		facets: [{
			name: 'code',
			accept: function(item, extension) {
				// Deal with CodeMirror supported files and files configured as text
				return accept(item, extension) || NIMBUS.utils.isTextFile(item, extension);
			},
			icon: function(item) {
				var extension = NIMBUS.utils.getFileExtensionFromItem(item);
				var support = supports[extension];
				if (!support)
					return 'subject'; // defaults to text file
				if (support.icon)
					return support.icon; // customized icons
				return 'code';

			},
			thumbnail: null,
			describe: function describe(item) {
				if (typeof item.lines !== 'number')
					return '';
				if (item.lines === 0)
					return NIMBUS.translate('TextDescription0Line');
				if (item.lines === 1)
					return NIMBUS.translate('TextDescription1Line');
				return NIMBUS.translate('TextDescriptionNLines', [item.lines]);
			}
		}],
		actions: [{
			name: 'code-edit',
			icon: 'code',
			caption: 'CodeActionEdit',
			accept: accept,
			url: (item) => '/code-editor.html?itemId=' + item.id
		}],
		langs: {
			fr: {
				CodeActionEdit: "Ouvrir dans l'éditeur de code",
				CodeEditorTitle: "Editeur de code",
				CodeEditorOptions: "Options",
				CodeEditorOptionsIndentWithTabs: "Indentation avec tabulation",
				CodeEditorOptionsRTLDirection: "Texte de droite à gauche",
				CodeEditorOptionsLineWrapping: "Retour automatique à la ligne",
				CodeEditorOptionsLineNumbers: "Numérotation des lignes",
				CodeEditorOptionsFoldGutter: "Afficher les blocs dans la marge",
				CodeEditorJumpToLinePrompt: "Aller à la ligne :",
				CodeEditorJumpToLineFormat: "(Format: ligne:colonne ou scroll%)",
			},
			en: {
				CodeActionEdit: "Open in code editor",
				CodeEditorTitle: "Code editor",
				CodeEditorOptions: "Options",
				CodeEditorOptionsIndentWithTabs: "Indent with tabs",
				CodeEditorOptionsRTLDirection: "Right-to-left text",
				CodeEditorOptionsLineWrapping: "Retour automatique à la ligne",
				CodeEditorOptionsLineNumbers: "Numérotation des lignes",
				CodeEditorOptionsFoldGutter: "Expand/Collapse blocs with gutter",
				CodeEditorJumpToLinePrompt: "Jump to line:",
				CodeEditorJumpToLineFormat: "(Use line:column or scroll% syntax)",
			}
		}
	});

})();

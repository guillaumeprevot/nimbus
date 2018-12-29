(function() {

	var support = {
		asc: { mode: 'asciiarmor', mime: 'application/pgp', alias: 'pgp,sig' },
		c: { mode: 'clike', mime: 'text/x-csrc', alias: 'h,ino' },
		coffee: { mode: 'coffeescript', mime: 'application/vnd.coffeescript' },
		cpp: { mode: 'clike', mime: 'text/x-c++src', alias: 'c++,cc,cxx,hpp,h++,hh,hxx' },
		cs: { mode: 'clike', mime: 'text/x-csharp' },
		css: { mode: 'css', mime: 'text/css' },
		diff: { mode: 'diff', mime: 'text/x-diff', alias: 'patch' },
		dockerfile: { mode: 'dockerfile', mime: 'text/x-dockerfile' },
		dtd: { mode: 'dtd', mime: 'application/xml-dtd' },
		go: { mode: 'go', mime: 'text/x-go' },
		groovy: { mode: 'groovy', mime: 'text/x-groovy', alias: 'gradle' },
		html: { mode: 'xml,javascript,css,htmlmixed', mime: 'text/html', alias: 'htm,xhtml' },
		ini: { mode: 'properties', mime: 'text/x-ini' },
		java: { mode: 'clike', mime: 'text/x-java' },
		js: { mode: 'javascript', mime: 'text/javascript' },
		json: { mode: 'javascript', mime: 'application/json', alias: 'map' },
		jsp: { mode: 'htmlembedded', mime: 'application/x-jsp' },
		kt: { mode: 'clike', mime: 'text/x-kotlin' },
		less: { mode: 'css', mime: 'text/x-less' },
		lua: { mode: 'lua', mime: 'text/x-lua' },
		md: { mode: 'markdown', mime: 'text/x-markdown', icon: 'art_track', alias: 'markdown,mkd' },
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

	for (var ext in support) {
		if (! support.hasOwnProperty(ext))
			continue;
		var def = support[ext];
		if (! def.alias)
			continue;
		var aliases = def.alias;
		delete def.alias;
		aliases.split(',').forEach(function(alias) {
			support[alias] = def;
		});
	}

	function accept(item, extension) {
		return support.hasOwnProperty(extension);
	}

	NIMBUS.utils.codeMirrorSupport = support;

	NIMBUS.plugins.add({
		name: 'code',
		facets: [{
			name: 'code',
			accept: function(item, extension) {
				// Deal with CodeMirror supported files and files configured as text
				return accept(item, extension) || NIMBUS.utils.isTextFile(item, extension);
			},
			image: function(item, thumbnail) {
				var extension = item.name.substring(item.name.lastIndexOf('.') + 1).toLowerCase();
				var support = NIMBUS.utils.codeMirrorSupport[extension];
				if (!support)
					return '<i class="material-icons">subject</i>'; // defaults to text file
				if (support && support.icon)
					return '<i class="material-icons">' + support.icon + '</i>'; // customized icons
				return '<i class="material-icons">code</i>';
			},
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
			execute: function(item) {
				window.open('/code-editor.html?' + $.param({
					itemId: item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}
		}],
		langs: {
			fr: {
				CodeActionEdit: "Ouvrir dans l'éditeur de code",
				CodeEditorTitle: "Editeur de code",
				CodeEditorTheme: "Thèmes",
				CodeEditorThemeNone: "Sans thème",
				CodeEditorThemeDefault: "Thème par défaut",
				CodeEditorOptions: "Options",
				CodeEditorOptionsIndentWithTabs: "Indentation avec tabulation",
				CodeEditorOptionsRTLDirection: "Texte de droite à gauche",
				CodeEditorOptionsLineWrapping: "Retour automatique à la ligne",
				CodeEditorOptionsLineNumbers: "Numérotation des lignes",
			},
			en: {
				CodeActionEdit: "Open in code editor",
				CodeEditorTitle: "Code editor",
				CodeEditorTheme: "Themes",
				CodeEditorThemeNone: "No theme",
				CodeEditorThemeDefault: "Default theme",
				CodeEditorOptions: "Options",
				CodeEditorOptionsIndentWithTabs: "Indent with tabs",
				CodeEditorOptionsRTLDirection: "Right-to-left text",
				CodeEditorOptionsLineWrapping: "Retour automatique à la ligne",
				CodeEditorOptionsLineNumbers: "Numérotation des lignes",
			}
		}
	});

})();

(function() {

	function accept(item, extension) {
		return 'secret' === extension; 
	}

	NIMBUS.plugins.add({
		name: 'secret',
		properties: [],
		facets: [{
			name: 'secret',
			accept: accept,
			icon: 'lock',
			thumbnail: null,
			describe: function describe(item) {
				return '';
			}
		}],
		actions: [{
			name: 'secret-edit',
			icon: 'lock_open',
			caption: 'SecretActionEdit',
			accept: accept,
			execute: function(item) {
				window.location.assign('/secret-editor.html?' + $.param({
					itemId: item.id,
					fromUrl: window.location.href,
					fromTitle: $('title').text()
				}));
			}
		}],
		langs: {
			fr: {
				SecretActionEdit: "Afficher ou modifier le contenu",
				SecretEditorTitle: "Note privée",
				SecretEditorSave: "Sauvegarder les modifications",
				SecretEditorPromptPassphrase: "Phrase de passe",
				SecretEditorInvalidPassphrase: "Phrase de passe non valide",
				SecretEditorDifferentPassphrase: "Phrases de passe différentes",
				SecretEditorPassphrasePlaceholder: "Entrer votre passphrase",
				SecretEditorPassphraseConfirmationPlaceholder: "Confirmer votre passphrase",
				SecretEditorCancelButton: "Annuler",
				SecretEditorValidateButton: "Valider",
			},
			en: {
				SecretActionEdit: "View or edit secret content",
				SecretEditorTitle: "Private note",
				SecretEditorSave: "Save modifications",
				SecretEditorPromptPassphrase: "Passphrase",
				SecretEditorInvalidPassphrase: "Invalid passphrase",
				SecretEditorDifferentPassphrase: "Passphrase must be identical",
				SecretEditorPassphrasePlaceholder: "Enter passphrase here",
				SecretEditorPassphraseConfirmationPlaceholder: "Confirm passphrase here",
				SecretEditorCancelButton: "Cancel",
				SecretEditorValidateButton: "Validate",
			} 
		}
	});

})();
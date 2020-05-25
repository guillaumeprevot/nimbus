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
			url: (item) => '/secret-editor.html?itemId=' + item.id
		}],
		langs: {
			fr: {
				SecretActionEdit: "Afficher ou modifier le contenu",
				SecretEditorTitle: "Note privée",
				SecretEditorSave: "Sauvegarder les modifications (Ctrl+S)",
				SecretEditorClearPassphrase: "Choisir une nouvelle phrase de passe lors de la sauvegarde",
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
				SecretEditorSave: "Save modifications (Ctrl+S)",
				SecretEditorClearPassphrase: "Choose a new the passphrase during the next save event",
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
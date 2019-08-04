(function() {

	function accept(item, extension) {
		return 'application' === extension;
	}

	NIMBUS.plugins.add({
		name: 'application',
		properties: [],
		facets: [{
			name: 'application',
			accept: accept,
			icon: 'settings_applications',
			thumbnail: function(item) {
				return item.iconURL || '';
			},
			describe: function(item) {
				if (!item.runCount)
					return NIMBUS.translate('ApplicationNewApplication');
				if (item.runCount === 1)
					return NIMBUS.translate('ApplicationSingleExecution', NIMBUS.formatDatetime(item.runLast));
				return NIMBUS.translate('ApplicationMultipleExecutions', NIMBUS.formatDatetime(item.runLast), item.runCount);
			}
		}],
		actions: [{
			name: 'run',
			icon: 'settings_applications',
			caption: 'ApplicationExecute',
			accept: accept,
			execute: function(item) {
				// Record usage
				$.post("/items/metadata?itemId=" + item.id + "&metadata=" + JSON.stringify([
					{ action:"set", name:"runCount", type:"integer", value:(item.runCount || 0)+1 },
					{ action:"set", name:"runLast", type:"long", value:Date.now() }
				]));
				// Launch application
				window.open('/files/browseTo/' + item.id);
			}
		}],
		langs: {
			fr: {
				ApplicationExecute: "Lancer cette application",
				ApplicationNewApplication: "Nouvelle application",
				ApplicationSingleExecution: "Lancée une fois le {0}",
				ApplicationMultipleExecutions: "Lancée {1} fois et le {0} en dernier",
			},
			en: {
				ApplicationExecute: "Launch application",
				ApplicationNewApplication: "New application",
				ApplicationSingleExecution: "Launched one time on {0}",
				ApplicationMultipleExecutions: "Launched on {0}, {1} times in total",
			}
		}
	});

})();
package fr.techgp.nimbus.facets;

import java.util.List;

import fr.techgp.nimbus.Facet;

public class DefaultOpenFacet implements Facet {

	@Override
	public void fillClientPlugins(List<String> plugins) {
		plugins.add("default-open.js");
	}

}

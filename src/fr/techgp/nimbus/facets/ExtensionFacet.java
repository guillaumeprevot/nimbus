package fr.techgp.nimbus.facets;

import java.util.List;

import fr.techgp.nimbus.Facet;

public class ExtensionFacet implements Facet {

	@Override
	public void fillClientPlugins(List<String> plugins) {
		plugins.add("epub.js");
		plugins.add("pdf.js");
	}

}

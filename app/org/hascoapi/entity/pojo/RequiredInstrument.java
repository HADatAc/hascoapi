package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.Utils;
import org.hascoapi.vocabularies.FOAF;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.SCHEMA;
import org.hascoapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hascoapi.Constants.*;

@JsonFilter("requiredInstrumentFilter")
public class RequiredInstrument extends HADatAcThing {

	private static final Logger log = LoggerFactory.getLogger(RequiredInstrument.class);

	@PropertyField(uri="vstoi:usesInstrument")
    protected String usesInstrument;

	@PropertyField(uri="vstoi:hasRequiredComponent")
	private List<String> hasRequiredComponentURIs;

	public String getUsesInstrument() {
		return usesInstrument;
	}

	public void setUsesInstrument(String usesInstrument) {
		this.usesInstrument = usesInstrument;
	}

	public Instrument getInstrument() {
		if (usesInstrument == null || usesInstrument.isEmpty()) {
			return null;
		}
		return Instrument.find(usesInstrument);
	} 

	public List<String> getHasRequiredComponents() {
		return hasRequiredComponentURIs;
	}

	public void setHasRequiredComponent(List<String> hasRequiredComponentURIs) {
		this.hasRequiredComponentURIs = hasRequiredComponentURIs;
	}

	public void addHasRequiredComponent(String hasRequiredComponent) {
		this.hasRequiredComponentURIs.add(hasRequiredComponent);
	}

	public List<RequiredComponent> getRequiredComponents() {
		if (hasRequiredComponentURIs == null || hasRequiredComponentURIs.isEmpty()) {
			return null;
		}
		List<RequiredComponent> list = new ArrayList<RequiredComponent>();
		for (String hasRequiredComponentURI : hasRequiredComponentURIs) {
			if (hasRequiredComponentURI != null && !hasRequiredComponentURI.isEmpty()) {
				RequiredComponent requiredComponent = RequiredComponent.find(hasRequiredComponentURI);
				if (requiredComponent != null) {
					list.add(requiredComponent);
				}
			}
		}
		return list;

	}

	public RequiredInstrument() {
		this.setTypeUri(VSTOI.REQUIRED_INSTRUMENT);
		this.setHascoTypeUri(VSTOI.REQUIRED_INSTRUMENT);
		hasRequiredComponentURIs = new ArrayList<String>();
	}

	public static RequiredInstrument find(String uri) {
		//System.out.println("RequiredInstrumentation.find(): uri = [" + uri + "]");
		RequiredInstrument requiredInstrument;
		String hascoTypeUri = Utils.retrieveHASCOTypeUri(uri);
		if (hascoTypeUri == null) {
			System.out.println("[ERROR] RequiredInstrument.java: URI [" + uri + "] has no HASCO TYPE.");
			return null;
		}
		//System.out.println("Place.find(): typeUri = [" + typeUri + "]");
		if (hascoTypeUri.equals(VSTOI.REQUIRED_INSTRUMENT)) {
			requiredInstrument = new RequiredInstrument();
		} else {
			return null;
		}

	    Statement statement;
	    RDFNode object;
	    
	    String queryString = "DESCRIBE <" + uri + ">";
	    Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);
		
		StmtIterator stmtIterator = model.listStatements();

		if (!stmtIterator.hasNext()) {
			return null;
		} 
		
		while (stmtIterator.hasNext()) {
		    statement = stmtIterator.next();
		    object = statement.getObject();
			String str = URIUtils.objectRDFToString(object);
			if (uri != null && !uri.isEmpty()) {
				if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
					requiredInstrument.setLabel(str);
				} else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
					requiredInstrument.setTypeUri(str); 
				} else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
					requiredInstrument.setComment(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
					requiredInstrument.setHascoTypeUri(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.USES_INSTRUMENT)) {
					requiredInstrument.setUsesInstrument(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_REQUIRED_COMPONENT)) {
					requiredInstrument.addHasRequiredComponent(str);
				}
			}
		}

		requiredInstrument.setUri(uri);
		
		return requiredInstrument;
	}

    @Override public void save() {
		saveToTripleStore();
	}

    @Override public void delete() {
		deleteFromTripleStore();
	}

}

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

@JsonFilter("requiredInstrumentationFilter")
public class RequiredInstrumentation extends HADatAcThing {

	private static final Logger log = LoggerFactory.getLogger(RequiredInstrumentation.class);

	@PropertyField(uri="vstoi:usesInstrument")
    protected String usesInstrument;

	@PropertyField(uri="vstoi:hasRequiredDetector")
	private List<String> hasRequiredDetectors;

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

	public List<String> getHasRequiredDetector() {
		return hasRequiredDetectors;
	}

	public List<Detector> getDetectors() {
		if (hasRequiredDetectors == null || hasRequiredDetectors.isEmpty()) {
			return null;
		}
		List<Detector> list = new ArrayList<Detector>();
		for (String detectorUri : hasRequiredDetectors) {
			if (detectorUri != null && !detectorUri.isEmpty()) {
				Detector detector = Detector.find(detectorUri);
				if (detector != null) {
					list.add(detector);
				}
			}
		}
		return list;

	}

	public void setHasRequiredDetector(List<String> hasRequiredDetectors) {
		this.hasRequiredDetectors = hasRequiredDetectors;
	}

	public void addHasRequiredDetector(String hasRequiredDetector) {
		this.hasRequiredDetectors.add(hasRequiredDetector);
	}

	public RequiredInstrumentation() {
		this.setTypeUri(VSTOI.REQUIRED_INSTRUMENTATION);
		this.setHascoTypeUri(VSTOI.REQUIRED_INSTRUMENTATION);
		hasRequiredDetectors = new ArrayList<String>();
	}

	public static RequiredInstrumentation find(String uri) {
		//System.out.println("RequiredInstrumentation.find(): uri = [" + uri + "]");
		RequiredInstrumentation requiredInstrumentation;
		String hascoTypeUri = Utils.retrieveHASCOTypeUri(uri);
		if (hascoTypeUri == null) {
			System.out.println("[ERROR] RequiredInstrumentation.java: URI [" + uri + "] has no HASCO TYPE.");
			return null;
		}
		//System.out.println("Place.find(): typeUri = [" + typeUri + "]");
		if (hascoTypeUri.equals(VSTOI.REQUIRED_INSTRUMENTATION)) {
			requiredInstrumentation = new RequiredInstrumentation();
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
					requiredInstrumentation.setLabel(str);
				} else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
					requiredInstrumentation.setTypeUri(str); 
				} else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
					requiredInstrumentation.setComment(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
					requiredInstrumentation.setHascoTypeUri(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.USES_INSTRUMENT)) {
					requiredInstrumentation.setUsesInstrument(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_REQUIRED_DETECTOR)) {
					requiredInstrumentation.addHasRequiredDetector(str);
				}
			}
		}

		requiredInstrumentation.setUri(uri);
		
		return requiredInstrumentation;
	}

    @Override public void save() {
		saveToTripleStore();
	}

    @Override public void delete() {
		deleteFromTripleStore();
	}

}

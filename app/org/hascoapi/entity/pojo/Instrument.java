package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.query.QuerySolution;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.Utils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hascoapi.Constants.*;

@JsonFilter("instrumentFilter")
public class Instrument extends Container {

	private static final Logger log = LoggerFactory.getLogger(Instrument.class);

	public Instrument() {
		super();
    }
    
	public Instrument(String className) {
		super(className);
    }
    
	@Override
	public boolean equals(Object o) {
		if((o instanceof Instrument) && (((Instrument)o).getUri().equals(this.getUri()))) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return getUri().hashCode();
	}

	public static Instrument find(String uri) {
		Instrument instrument = null;

		// Construct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
			instrument = new Instrument(VSTOI.INSTRUMENT);
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				instrument.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);

				if (predicate.equals(RDFS.LABEL)) {
					instrument.setLabel(object);
				} else if (predicate.equals(RDFS.SUBCLASS_OF)) {
					instrument.setSuperUri(object);
				} else if (predicate.equals(HASCO.HASCO_TYPE)) {
					instrument.setHascoTypeUri(object);
				} else if (predicate.equals(VSTOI.HAS_STATUS)) {
					instrument.setHasStatus(object);
				} else if (predicate.equals(HASCO.HAS_IMAGE)) {
					instrument.setHasImageUri(object);
				} else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
					instrument.setHasWebDocument(object);
				} else if (predicate.equals(VSTOI.HAS_FIRST)) {
					instrument.setHasFirst(object);
				} else if (predicate.equals(VSTOI.HAS_INFORMANT)) {
					instrument.setHasInformant(object);
				} else if (predicate.equals(RDFS.COMMENT)) {
					instrument.setComment(object);
				} else if (predicate.equals(VSTOI.HAS_SHORT_NAME)) {
					instrument.setHasShortName(object);
				} else if (predicate.equals(VSTOI.HAS_MAKER)) {
					instrument.setHasMakerUri(object);
				} else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
					instrument.setHasLanguage(object);
				} else if (predicate.equals(VSTOI.HAS_VERSION)) {
					instrument.setHasVersion(object);
				} else if (predicate.equals(VSTOI.HAS_REVIEW_NOTE)) {
					instrument.setHasReviewNote(object);
				} else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					instrument.setHasSIRManagerEmail(object);
				} else if (predicate.equals(VSTOI.HAS_EDITOR_EMAIL)) {
					instrument.setHasEditorEmail(object);
				}
			}
		}

		instrument.setUri(uri);
		return instrument;
	}


	public static List<Instrument> findAvailable() {
		List<Instrument> instruments = new ArrayList<Instrument>();
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
		    " SELECT ?uri WHERE { " +
		    "   { ?instModel rdfs:subClassOf* vstoi:Instrument . " +
		    "     ?uri a ?instModel ." + 
		    "   } MINUS { " + 
		    "     ?dep_uri a vstoi:Deployment . " + 
		    "     ?dep_uri hasco:hasInstrument ?uri .  " +
		    "     FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " + 
		    "    } " + 
		    "} " + 
		    "ORDER BY DESC(?datetime) ";
		
		ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
		
		while (resultsrw.hasNext()) {
		    QuerySolution soln = resultsrw.next();
		    Instrument instrument = find(soln.getResource("uri").getURI().trim());
			instruments.add(instrument);
		}			
		
		java.util.Collections.sort((List<Instrument>) instruments);
		return instruments;
	}
	
	public static List<Instrument> findDeployed() {
		List<Instrument> instruments = new ArrayList<Instrument>();
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
		    " SELECT ?uri WHERE { " +
		    "   ?instModel rdfs:subClassOf* vstoi:Instrument . " +
		    "   ?uri a ?instModel ." + 
		    "   ?dep_uri a vstoi:Deployment . " + 
		    "   ?dep_uri hasco:hasInstrument ?uri .  " +
		    "   FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " + 
		    "} " + 
		    "ORDER BY DESC(?datetime) ";
		
		ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
		
		while (resultsrw.hasNext()) {
		    QuerySolution soln = resultsrw.next();
		    Instrument instrument = find(soln.getResource("uri").getURI().trim());
		    instruments.add(instrument);
		}			

		java.util.Collections.sort((List<Instrument>) instruments);
		return instruments;
	}
	
    @Override public void save() {
		//System.out.println("Instrument.java: Saving " + getUri() + " into triple store.");
		saveToTripleStore();
	}

    @Override public void delete() {
		deleteFromTripleStore();
	}

}

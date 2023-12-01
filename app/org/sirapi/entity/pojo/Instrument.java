package org.sirapi.entity.pojo;

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
import org.sirapi.annotations.PropertyField;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.NameSpaces;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.utils.URIUtils;
import org.sirapi.utils.Utils;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.RDF;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sirapi.Constants.*;

@JsonFilter("instrumentFilter")
public class Instrument extends Container {

	private static final Logger log = LoggerFactory.getLogger(Instrument.class);

    
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
		System.out.println("Instrument.java : in find(): uri = [" + uri + "]");
	    Instrument instrument = null;
	    Statement statement;
	    RDFNode object;
	    
	    String queryString = "DESCRIBE <" + uri + ">";
	    Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);
		
		StmtIterator stmtIterator = model.listStatements();

		if (!stmtIterator.hasNext()) {
			return null;
		} else {
			instrument = new Instrument();
		}
		
		while (stmtIterator.hasNext()) {
		    statement = stmtIterator.next();
		    object = statement.getObject();
			String str = URIUtils.objectRDFToString(object);
			if (uri != null && !uri.isEmpty()) {
				if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
					instrument.setLabel(str);
				} else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
					instrument.setTypeUri(str); 
				} else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
					instrument.setHascoTypeUri(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
					instrument.setHasStatus(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SERIAL_NUMBER)) {
					instrument.setSerialNumber(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_INFORMANT)) {
					instrument.setHasInformant(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_IMAGE)) {
					instrument.setImage(str);
				} else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
					instrument.setComment(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SHORT_NAME)) {
					instrument.setHasShortName(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
					instrument.setHasLanguage(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
         			instrument.setHasVersion(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					instrument.setHasSIRManagerEmail(str);
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
		saveToTripleStore();
	}

    @Override public void delete() {
		deleteFromTripleStore();
	}

    @Override
    public boolean saveToSolr() {
        return false;
    }
    
    @Override
    public int deleteFromSolr() {
        return 0;
    }
}

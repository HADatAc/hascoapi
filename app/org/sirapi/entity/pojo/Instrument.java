package org.sirapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.fasterxml.jackson.annotation.JsonFilter;
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
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.RDF;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonFilter("instrumentFilter")
public class Instrument extends HADatAcThing implements Comparable<Instrument> {

	private static final Logger log = LoggerFactory.getLogger(Instrument.class);

	@PropertyField(uri="vstoi:hasSerialNumber")
	private String serialNumber;

	@PropertyField(uri="hasco:hasImage")
	private String image;

	@PropertyField(uri="vstoi:hasShortName")
	private String hasShortName;

	@PropertyField(uri="vstoi:hasInstruction")
	private String hasInstruction;

	@PropertyField(uri="vstoi:hasLanguage")
	private String hasLanguage;

	@PropertyField(uri="vstoi:hasSIRMaintainerEmail")
	private String hasSIRMaintainerEmail;

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}
	
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

	public String getHasShortName() {
		return hasShortName;
	}

	public void setHasShortName(String hasShortName) {
		this.hasShortName = hasShortName;
	}

	public String getHasInstruction() {
		return hasInstruction;
	}

	public void setHasInstruction(String hasInstruction) {
		this.hasInstruction = hasInstruction;
	}

	public String getHasLanguage() {
		return hasLanguage;
	}

	public void setHasLanguage(String hasLanguage) {
		this.hasLanguage = hasLanguage;
	}

	public String getHasSIRMaintainerEmail() {
		return hasSIRMaintainerEmail;
	}

	public void setHasSIRMaintainerEmail(String hasSIRMaintainerEmail) {
		this.hasSIRMaintainerEmail = hasSIRMaintainerEmail;
	}

	public String getTypeLabel() {
    	InstrumentType insType = InstrumentType.find(getTypeUri());
    	if (insType == null || insType.getLabel() == null) {
    		return "";
    	}
    	return insType.getLabel();
    }

    public String getTypeURL() {
    	InstrumentType insType = InstrumentType.find(getTypeUri());
    	if (insType == null || insType.getLabel() == null) {
    		return "";
    	}
    	return insType.getURL();
    }

    public List<Detector> getAttachments() {
    	List<Detector> dets = new ArrayList<Detector>();
    	if (uri == null || uri.isEmpty()) {
    		return dets;
    	}
    	String iUri = uri;
    	if (uri.startsWith("http")) {
    		iUri = "<" + uri + ">";
    	}
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
			    "SELECT ?detUri WHERE { " +
			    "   ?detUri vstoi:isInstrumentAttachment " + iUri + " . " + 
			    "} ";
			
		ResultSetRewindable resultsrw = SPARQLUtils.select(
				CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
				
		while (resultsrw.hasNext()) {
			QuerySolution soln = resultsrw.next();
			Detector det = Detector.find(soln.getResource("detUri").getURI());
			dets.add(det);
		}			
    	return dets;
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

	public static List<Instrument> findByLanguage(String language) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
				" SELECT ?uri WHERE { " +
				" ?instModel rdfs:subClassOf+ vstoi:Instrument . " +
				" ?uri a ?instModel ." +
				" ?uri vstoi:hasLanguage ?language . " +
				"   FILTER (?language = \"" + language + "\") " +
				"} ";

		return findByQuery(queryString);
	}

	public static List<Instrument> findByKeyword(String keyword) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
				" SELECT ?uri WHERE { " +
				" ?instModel rdfs:subClassOf+ vstoi:Instrument . " +
				" ?uri a ?instModel ." +
				" ?uri rdfs:label ?label . " +
				"   FILTER regex(?label, \"" + keyword + "\", \"i\") " +
				"} ";

		return findByQuery(queryString);
	}

	public static List<Instrument> findByKeywordAndLanguage(String keyword, String language) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
				" SELECT ?uri WHERE { " +
				" ?instModel rdfs:subClassOf+ vstoi:Instrument . " +
				" ?uri a ?instModel ." +
				" ?uri vstoi:hasLanguage ?language . " +
				" ?uri rdfs:label ?label . " +
				"   FILTER (regex(?label, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) " +
				"} ";

		return findByQuery(queryString);
	}

	public static List<Instrument> findByMaintainerEmail(String maintainerEmail) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
				" SELECT ?uri WHERE { " +
				" ?instModel rdfs:subClassOf+ vstoi:Instrument . " +
				" ?uri a ?instModel ." +
				" ?uri vstoi:hasSIRMaintainerEmail ?maintainerEmail . " +
				"   FILTER (?maintainerEmail = \"" + maintainerEmail + "\") " +
				"} ";

		return findByQuery(queryString);
	}

	private static List<Instrument> findByQuery(String queryString) {
		List<Instrument> instruments = new ArrayList<Instrument>();
		ResultSetRewindable resultsrw = SPARQLUtils.select(
				CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultsrw.hasNext()) {
			return null;
		}

		while (resultsrw.hasNext()) {
			QuerySolution soln = resultsrw.next();
			Instrument instrument = find(soln.getResource("uri").getURI());
			instruments.add(instrument);
		}

		java.util.Collections.sort((List<Instrument>) instruments);
		return instruments;

	}

	public static List<Instrument> find() {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
		    " SELECT ?uri WHERE { " +
		    " ?instModel rdfs:subClassOf+ vstoi:Instrument . " + 
		    " ?uri a ?instModel ." + 
		    "} ";
		
		return findByQuery(queryString);
	}

	public static List<Instrument> findWithPages(int pageSize, int offset) {
		List<Instrument> instruments = new ArrayList<Instrument>();
		String queryString = "";
		queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
				"SELECT ?instUri ?instLabel ?subUri WHERE { " +
				"   ?instUri rdfs:subClassOf* vstoi:Instrument . " +
				"   ?instUri a ?subUri . " +
				"   ?instUri rdfs:label ?instLabel . " +
				" }" +
				" LIMIT " + pageSize +
				" OFFSET " + offset;

		ResultSetRewindable resultsrw = SPARQLUtils.select(
				CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

		Instrument instrument = null;
		while (resultsrw.hasNext()) {
			QuerySolution soln = resultsrw.next();
			if (soln != null && soln.getResource("instUri").getURI()!= null) {
				instrument = new Instrument();
				instrument.setUri(soln.get("instUri").toString());
				instrument.setLabel(soln.get("instLabel").toString());
				//System.out.println("Instrument URI: " + soln.get("instUri").toString());
			}
			instruments.add(instrument);
		}
		return instruments;
	}

	public static List<Instrument> findAvailable() {
		List<Instrument> instruments = new ArrayList<Instrument>();
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
		    " SELECT ?uri WHERE { " +
		    "   { ?instModel rdfs:subClassOf+ vstoi:Instrument . " + 
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
		    "   ?instModel rdfs:subClassOf+ vstoi:Instrument . " + 
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
	
	public static Instrument find(String uri) {
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
		    if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
		    	instrument.setLabel(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                instrument.setTypeUri(object.asResource().getURI());
			} else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
				instrument.setHascoTypeUri(object.asResource().getURI());
		    } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SERIAL_NUMBER)) {
		    	instrument.setSerialNumber(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_IMAGE)) {
                instrument.setImage(object.asLiteral().getString());
		    } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
		    	instrument.setComment(object.asLiteral().getString());
			} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SHORT_NAME)) {
				instrument.setHasShortName(object.asLiteral().getString());
			} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_INSTRUCTION)) {
				instrument.setHasInstruction(object.asLiteral().getString());
			} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
				instrument.setHasLanguage(object.asLiteral().getString());
			} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MAINTAINER_EMAIL)) {
				instrument.setHasSIRMaintainerEmail(object.asLiteral().getString());
		    }
		}
		
		instrument.setUri(uri);
		
		return instrument;
	}

	private static String centerText(String str, int width) {
		if (str == null) {
			str = "";
		}
		if (str.length() > width) {
			return str;
		}
		int left = (width - str.length()) / 2;
		StringBuffer newStr = new StringBuffer();
		for (int i=0; i < left; i++) {
			newStr.append(" ");
		}
		newStr.append(str);
		return newStr.toString();
	}

	private static List<String> breakString(String str, int width) {
		List<String> lines = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(str);
		String newLine = "";
		String nextWord = "";
		while (st.hasMoreTokens()) {
			nextWord = st.nextToken();
			if (nextWord.length() >= width) {
				if (newLine.equals("")) {
					newLine = nextWord;
				} else {
					newLine = newLine + " " + nextWord;
				}
				lines.add(newLine);
				newLine = "";
			} else if (newLine.length() + nextWord.length() > width) {
				lines.add(newLine);
				newLine = nextWord;
			} else {
				if (newLine.equals("")) {
					newLine = nextWord;
				} else {
					newLine = newLine + " " + nextWord;
				}
			}

		}
		if (newLine.length() > 0) {
			lines.add(newLine);
		}
		return lines;
	}

	public static String toString(String uri, int width) {
		Instrument instr = Instrument.find(uri);
		if (instr == null) {
			return "";
		}
		String str = "";

		str += centerText(instr.getHasShortName(), width) + "\n";
		str += "\n";

		for (String line : breakString("Instructions: " + instr.getHasInstruction(), width)) {
			str += line + "\n";
		}
		str += "\n";
		return str;
	}

	public static String toHTML(String uri, int width) {
		Instrument instr = Instrument.find(uri);
		if (instr == null) {
			return "";
		}
		String html = "";

		html += "<h2 style=\"text-align: center;\">" + instr.getHasShortName() + "</h2>";
		html += "<br>";

		html += "<b>Instructions</b>: " + instr.getHasInstruction() + "<br>";
		html += "<br>";
		return html;
	}

	public static int getNumberInstruments() {
		String query = "";
		query += NameSpaces.getInstance().printSparqlNameSpaceList();
		query += " select (count(?instrument) as ?tot) where { " +
				" ?instrumentType rdfs:subClassOf* vstoi:Instrument . " +
				" ?instrument a ?instrumentType . " +
				" }";

		//select ?obj ?collection ?objType where { ?obj hasco:isMemberOf ?collection . ?obj a ?objType . FILTER NOT EXISTS { ?objType rdfs:subClassOf* hasco:ObjectCollection . } }
		//System.out.println("Study query: " + query);

		try {
			ResultSetRewindable resultsrw = SPARQLUtils.select(
					CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

			if (resultsrw.hasNext()) {
				QuerySolution soln = resultsrw.next();
				return Integer.parseInt(soln.getLiteral("tot").getString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

    @Override
    public int compareTo(Instrument another) {
        return this.getLabel().compareTo(another.getLabel());
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

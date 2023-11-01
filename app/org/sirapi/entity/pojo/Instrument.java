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
import org.sirapi.utils.Utils;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.RDF;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sirapi.Constants.*;

@JsonFilter("instrumentFilter")
public class Instrument extends HADatAcThing implements SIRElement, Comparable<Instrument> {

	private static final Logger log = LoggerFactory.getLogger(Instrument.class);

	@PropertyField(uri="vstoi:hasStatus")
	private String hasStatus;

	@PropertyField(uri="vstoi:hasSerialNumber")
	private String serialNumber;

	@PropertyField(uri="vstoi:hasInformant")
	private String hasInformant;

	@PropertyField(uri="hasco:hasImage")
	private String image;

	@PropertyField(uri="vstoi:hasShortName")
	private String hasShortName;

	@PropertyField(uri="vstoi:hasInstruction")
	private String hasInstruction;

	@PropertyField(uri="vstoi:hasLanguage")
	private String hasLanguage;

	@PropertyField(uri="vstoi:hasVersion")
	private String hasVersion;

	@PropertyField(uri="vstoi:hasPageNumber")
	private String hasPageNumber;

	@PropertyField(uri="vstoi:hasDateField")
	private String hasDateField;

	@PropertyField(uri="vstoi:hasSubjectIDField")
	private String hasSubjectIDField;

	@PropertyField(uri="vstoi:hasSubjectRelationshipField")
	private String hasSubjectRelationshipField;

	@PropertyField(uri="vstoi:hasCopyrightNotice")
	private String hasCopyrightNotice;

	@PropertyField(uri="vstoi:hasSIRManagerEmail")
	private String hasSIRManagerEmail;

	public String getHasStatus() {
		return hasStatus;
	}

	public void setHasStatus(String hasStatus) {
		this.hasStatus = hasStatus;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public String getHasInformant() {
		return hasInformant;
	}

	public void setHasInformant(String hasInformant) {
		this.hasInformant = hasInformant;
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

	public String getHasVersion() {
		return hasVersion;
	}

	public void setHasVersion(String hasVersion) {
		this.hasVersion = hasVersion;
	}

	public String getHasPageNumber() {
		return hasPageNumber;
	}

	public void setHasPageNumber(String hasPageNumber) {
		this.hasPageNumber = hasPageNumber;
	}

	public String getHasDateField() {
		return hasDateField;
	}

	public void setHasDateField(String hasDateField) {
		this.hasDateField = hasDateField;
	}

	public String getHasSubjectIDField() {
		return hasSubjectIDField;
	}

	public void setHasSubjectIDField(String hasSubjectIDField) {
		this.hasSubjectIDField = hasSubjectIDField;
	}

	public String getHasSubjectRelationshipField() {
		return hasSubjectRelationshipField;
	}

	public void setHasSubjectRelationshipField(String hasSubjectRelationshipField) {
		this.hasSubjectRelationshipField = hasSubjectRelationshipField;
	}

	public String getHasCopyrightNotice() {
		return hasCopyrightNotice;
	}

	public void setHasCopyrightNotice(String hasCopyrightNotice) {
		this.hasCopyrightNotice = hasCopyrightNotice;
	}

	public String getHasSIRManagerEmail() {
		return hasSIRManagerEmail;
	}

	public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
		this.hasSIRManagerEmail = hasSIRManagerEmail;
	}

	public String gedetectorSlottTypeLabel() {
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

    public List<DetectorSlot> getDetectorSlots() {
    	List<DetectorSlot> atts = DetectorSlot.findByInstrument(uri);
    	return atts;
    }

    @JsonIgnore
	public List<Detector> getDetectors() {
		List<Detector> detectors = new ArrayList<Detector>();
    	List<DetectorSlot> atts = DetectorSlot.findByInstrument(uri);
		for (DetectorSlot att : atts) {
			Detector detector = att.getDetector();
			detectors.add(detector);
		} 
    	return detectors;
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
				" ?instModel rdfs:subClassOf* vstoi:Instrument . " +
				" ?uri a ?instModel ." +
				" ?uri vstoi:hasLanguage ?language . " +
				"   FILTER (?language = \"" + language + "\") " +
				"} ";

		return findByQuery(queryString);
	}

	public static List<Instrument> findByKeyword(String keyword) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
				" SELECT ?uri WHERE { " +
				" ?instModel rdfs:subClassOf* vstoi:Instrument . " +
				" ?uri a ?instModel ." +
				" ?uri rdfs:label ?label . " +
				"   FILTER regex(?label, \"" + keyword + "\", \"i\") " +
				"} ";

		return findByQuery(queryString);
	}

	public static List<Instrument> findByKeywordAndLanguageWithPages(String keyword, String language, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
				" ?instModel rdfs:subClassOf* vstoi:Instrument . " +
				" ?uri a ?instModel .";
		if (!language.isEmpty()) {
			queryString += " ?uri vstoi:hasLanguage ?language . ";
		}
		if (!keyword.isEmpty()) {
			queryString += " ?uri rdfs:label ?label . ";
		}
		if (!keyword.isEmpty() && !language.isEmpty()) {
			queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) ";
		} else if (!keyword.isEmpty()) {
			queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\")) ";
		} else if (!language.isEmpty()) {
			queryString += "   FILTER ((?language = \"" + language + "\")) ";
		}
		queryString += "} " +
				" LIMIT " + pageSize +
				" OFFSET " + offset;
		return findByQuery(queryString);
	}

	public static int findTotalByKeywordAndLanguage(String keyword, String language) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
				" ?instModel rdfs:subClassOf* vstoi:Instrument . " +
				" ?uri a ?instModel .";
		if (!language.isEmpty()) {
			queryString += " ?uri vstoi:hasLanguage ?language . ";
		}
		if (!keyword.isEmpty()) {
			queryString += " ?uri rdfs:label ?label . ";
		}
		if (!keyword.isEmpty() && !language.isEmpty()) {
			queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) ";
		} else if (!keyword.isEmpty()) {
			queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\")) ";
		} else if (!language.isEmpty()) {
			queryString += "   FILTER ((?language = \"" + language + "\")) ";
		}
		queryString += "}";

		//System.out.println(queryString);

		try {
			ResultSetRewindable resultsrw = SPARQLUtils.select(
					CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

			if (resultsrw.hasNext()) {
				QuerySolution soln = resultsrw.next();
				return Integer.parseInt(soln.getLiteral("tot").getString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	public static List<Instrument> findByManagerEmailWithPages(String managerEmail, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
				" ?instModel rdfs:subClassOf* vstoi:Instrument . " +
				" ?uri a ?instModel ." +
				" ?uri rdfs:label ?label . " +
				" ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
				"   FILTER (?managerEmail = \"" + managerEmail + "\") " +
				"}" +
				" ORDER BY ASC(?label) " +
				" LIMIT " + pageSize +
				" OFFSET " + offset;
		return findByQuery(queryString);
	}

	public static int findTotalByManagerEmail(String managerEmail) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
				" ?instModel rdfs:subClassOf* vstoi:Instrument . " +
				" ?uri a ?instModel ." +
				" ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
				"   FILTER (?managerEmail = \"" + managerEmail + "\") " +
				"}";

		try {
			ResultSetRewindable resultsrw = SPARQLUtils.select(
					CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

			if (resultsrw.hasNext()) {
				QuerySolution soln = resultsrw.next();
				return Integer.parseInt(soln.getLiteral("tot").getString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	public static List<Instrument> findByManagerEmail(String managerEmail) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
				" SELECT ?uri WHERE { " +
				" ?instModel rdfs:subClassOf* vstoi:Instrument . " +
				" ?uri a ?instModel ." +
				" ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
				"   FILTER (?managerEmail = \"" + managerEmail + "\") " +
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
		    " ?instModel rdfs:subClassOf* vstoi:Instrument . " +
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
	
	private static String objectToString(RDFNode node) {
 		if (node.isLiteral()) {
  			return node.asLiteral().getString();
 		} else if (node.isResource()) {
  			return node.asResource().getURI();
 		}
 		return null;
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
			String str = objectToString(object);
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
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_INSTRUCTION)) {
					instrument.setHasInstruction(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
					instrument.setHasLanguage(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
					instrument.setHasVersion(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_PAGE_NUMBER)) {
					instrument.setHasPageNumber(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_DATE_FIELD)) {
					instrument.setHasDateField(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SUBJECT_ID_FIELD)) {
					instrument.setHasSubjectIDField(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SUBJECT_RELATIONSHIP_FIELD)) {
					instrument.setHasSubjectRelationshipField(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_COPYRIGHT_NOTICE)) {
					instrument.setHasCopyrightNotice(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MAINTAINER_EMAIL)) {
					instrument.setHasSIRManagerEmail(str);
				}
			}
		}

		instrument.setUri(uri);
		
		return instrument;
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

	public boolean deleteDetectorSlots() {
		if (this.getDetectorSlots() == null || uri == null || uri.isEmpty()) {
			return true;
		}
		List<DetectorSlot> detectorSlots = DetectorSlot.findByInstrument(uri);
		if (detectorSlots == null) {
			return true;
		}
		for (DetectorSlot detectorSlot: detectorSlots) {
			detectorSlot.delete();
		}
		detectorSlots = DetectorSlot.findByInstrument(uri);
		return (detectorSlots == null);
	}

	public boolean createDetectorSlots(int totDetectorSlots) {
		if (totDetectorSlots <= 0) {
			return false;
		}
		if (this.getDetectorSlots() != null || uri == null || uri.isEmpty()) {
			return false;
		}
		for (int aux=1; aux <= totDetectorSlots; aux++) {
			String auxstr = Utils.adjustedPriority(String.valueOf(aux), totDetectorSlots);
			String newUri = uri + "/" + DETECTOR_SLOT_PREFIX + "/" + auxstr;
			DetectorSlot.createDetectorSlot(uri, newUri, auxstr,null);
		}
		List<DetectorSlot> detectorSlotList = DetectorSlot.findByInstrument(uri);
		if (detectorSlotList == null) {
			return false;
		}
		return (detectorSlotList.size() == totDetectorSlots);
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

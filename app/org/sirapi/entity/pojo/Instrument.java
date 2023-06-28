package org.sirapi.entity.pojo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

	@PropertyField(uri="vstoi:hasSIRMaintainerEmail")
	private String hasSIRMaintainerEmail;

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

	public String getHasSIRMaintainerEmail() {
		return hasSIRMaintainerEmail;
	}

	public void setHasSIRMaintainerEmail(String hasSIRMaintainerEmail) {
		this.hasSIRMaintainerEmail = hasSIRMaintainerEmail;
	}

	public String geattachmenttTypeLabel() {
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

    public List<Attachment> getAttachments() {
    	List<Attachment> atts = Attachment.findByInstrument(uri);
    	return atts;
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

	public static List<Instrument> findByKeywordAndLanguage(String keyword, String language) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
				" SELECT ?uri WHERE { " +
				" ?instModel rdfs:subClassOf* vstoi:Instrument . " +
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
				" ?instModel rdfs:subClassOf* vstoi:Instrument . " +
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
			} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
				instrument.setHasStatus(object.asLiteral().getString());
		    } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SERIAL_NUMBER)) {
		    	instrument.setSerialNumber(object.asLiteral().getString());
			} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_INFORMANT)) {
				instrument.setHasInformant(object.asResource().getURI());
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
			} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
				instrument.setHasVersion(object.asLiteral().getString());
			} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_PAGE_NUMBER)) {
				instrument.setHasPageNumber(object.asLiteral().getString());
			} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_DATE_FIELD)) {
				instrument.setHasDateField(object.asLiteral().getString());
			} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SUBJECT_ID_FIELD)) {
				instrument.setHasSubjectIDField(object.asLiteral().getString());
			} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SUBJECT_RELATIONSHIP_FIELD)) {
				instrument.setHasSubjectRelationshipField(object.asLiteral().getString());
			} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_COPYRIGHT_NOTICE)) {
				instrument.setHasCopyrightNotice(object.asLiteral().getString());
			} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MAINTAINER_EMAIL)) {
				instrument.setHasSIRMaintainerEmail(object.asLiteral().getString());
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

	public boolean deleteAttachments() {
		if (this.getAttachments() == null || uri == null || uri.isEmpty()) {
			return true;
		}
		List<Attachment> attachments = Attachment.findByInstrument(uri);
		if (attachments == null) {
			return true;
		}
		for (Attachment attachment: attachments) {
			attachment.delete();
		}
		attachments = Attachment.findByInstrument(uri);
		return (attachments == null);
	}

	private String adjustedPriority(String priority, int totAttachments) {
		int digits = 0;
		if (totAttachments < 10) {
			digits = 1;
		} else if (totAttachments < 100) {
			digits = 2;
		} else if (totAttachments < 1000) {
			digits = 3;
		} else {
			digits = 4;
		}
		String auxstr = String.valueOf(priority);
		for (int filler = auxstr.length(); filler < digits; filler++) {
			auxstr = "0" + auxstr;
		}
		return auxstr;
	}

	public boolean createAttachments(int totAttachments) {
		if (totAttachments <= 0) {
			return false;
		}
		if (this.getAttachments() != null || uri == null || uri.isEmpty()) {
			return false;
		}
		for (int aux=1; aux <= totAttachments; aux++) {
			String auxstr = adjustedPriority(String.valueOf(aux), totAttachments);
			String newUri = uri + "/ATT/" + auxstr;
			Attachment.createAttachment(uri, newUri, auxstr,null);
		}
		List<Attachment> attachmentList = Attachment.findByInstrument(uri);
		if (attachmentList == null) {
			return false;
		}
		return (attachmentList.size() == totAttachments);
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

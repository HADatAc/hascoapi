package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.vocabularies.*;

@JsonFilter("detectorStemFilter")
public class DetectorStem extends HADatAcClass implements SIRElement, Comparable<DetectorStem>  {

    @PropertyField(uri="vstoi:hasStatus")
    private String hasStatus;

    @PropertyField(uri="vstoi:hasContent")
    private String hasContent;

    @PropertyField(uri="vstoi:hasLanguage")
    private String hasLanguage;

    @PropertyField(uri="vstoi:hasVersion")
    private String hasVersion;

    @PropertyField(uri="prov:wasDerivedFrom")
    private String wasDerivedFrom;

    @PropertyField(uri="prov:wasGeneratedBy")
    private String wasGeneratedBy;

    @PropertyField(uri = "vstoi:hasReviewNote")
    String hasReviewNote;

    @PropertyField(uri = "vstoi:hasSIRManagerEmail")
    private String hasSIRManagerEmail;

    @PropertyField(uri = "vstoi:hasEditorEmail")
    private String hasEditorEmail;

    @PropertyField(uri="hasco:detects")
    private String detects;

    public String getHasStatus() {
        return hasStatus;
    }

    public void setHasStatus(String hasStatus) {
        this.hasStatus = hasStatus;
    }

    public String getHasContent() {
        return hasContent;
    }

    public void setHasContent(String hasContent) {
        this.hasContent = hasContent;
    }

    public String getDetects() {
        return detects;
    }

    public SemanticVariable getDetectsSemanticVariable() {
        if (detects == null) {
            return null;
        }
        return SemanticVariable.find(detects);
    }

    public void setDetects(String detects) {
        this.detects = detects;
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

    public void setWasDerivedFrom(String wasDerivedFrom) {
        this.wasDerivedFrom = wasDerivedFrom;
    }

    public String getWasDerivedFrom() {
        return wasDerivedFrom;
    }

    public void setWasGeneratedBy(String wasGeneratedBy) {
        this.wasGeneratedBy = wasGeneratedBy;
    }

    public String getWasGeneratedBy() {
        return wasGeneratedBy;
    }

    public void setHasVersion(String hasVersion) {
        this.hasVersion = hasVersion;
    }

    public String getHasReviewNote() {      
        return hasReviewNote;
    }

    public void setHasReviewNote(String hasReviewNote) {
        this.hasReviewNote = hasReviewNote;
    }

    public String getHasSIRManagerEmail() {
        return hasSIRManagerEmail;
    }

    public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail;
    }

    public String getHasEditorEmail() {
        return hasEditorEmail;
    }

    public void setHasEditorEmail(String hasEditorEmail) {
        this.hasEditorEmail = hasEditorEmail;
    }

    /* 
    public String getTypeLabel() {
        DetectorStemType detType = DetectorStemType.find(getTypeUri());
        if (detType == null || detType.getLabel() == null) {
            return "";
        }
        return detType.getLabel();
    }

    public String getTypeURL() {
        DetectorStemType detType = DetectorStemType.find(getTypeUri());
        if (detType == null || detType.getLabel() == null) {
            return "";
        }
        return detType.getURL();
    }
    */

    public DetectorStem () {
    }

    public DetectorStem (String className) {
		super(className);
    }

    public static List<DetectorStem> findByInstrument(String instrumentUri) {
        //System.out.println("findByInstrument: [" + instrumentUri + "]");
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:DetectorStem . " +
                " ?uri a ?detModel ." +
                " ?attUri vstoi:hasDetectorStem ?uri . " +
                " ?attUri vstoi:belongsTo <" + instrumentUri + ">. " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<DetectorStem> findAvailable() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                "   { ?detModel rdfs:subClassOf* vstoi:DetectorStem . " +
                "     ?uri a ?detModel ." +
                "   } MINUS { " +
                "     ?dep_uri a vstoi:Deployment . " +
                "     ?dep_uri hasco:hasDetectorStem ?uri .  " +
                "     FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " +
                "    } " +
                "} " +
                "ORDER BY DESC(?datetime) ";

        return findByQuery(queryString);
    }

    public static List<DetectorStem> findDeployed() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                "   ?detModel rdfs:subClassOf* vstoi:DetectorStem . " +
                "   ?uri a ?detModel ." +
                "   ?dep_uri a vstoi:Deployment . " +
                "   ?dep_uri hasco:hasDetectorStem ?uri .  " +
                "   FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " +
                "} " +
                "ORDER BY DESC(?datetime) ";

        return findByQuery(queryString);
    }

    private static List<DetectorStem> findByQuery(String queryString) {
        List<DetectorStem> detectorStems = new ArrayList<DetectorStem>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            DetectorStem detectorStem = find(soln.getResource("uri").getURI());
            detectorStems.add(detectorStem);
        }

        java.util.Collections.sort((List<DetectorStem>) detectorStems);
        return detectorStems;

    }

    public static DetectorStem find(String uri) {
		if (uri == null || uri.isEmpty()) {
			return null;
		}
		DetectorStem detectorStem = null;
		// Construct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
            detectorStem = new DetectorStem(VSTOI.DETECTOR_STEM);
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				detectorStem.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);

				if (predicate.equals(RDFS.LABEL)) {
					detectorStem.setLabel(object);
				} else if (predicate.equals(RDFS.SUBCLASS_OF)) {
					detectorStem.setSuperUri(object); 
				} else if (predicate.equals(HASCO.HASCO_TYPE)) {
					detectorStem.setHascoTypeUri(object);
				} else if (predicate.equals(HASCO.HAS_IMAGE)) {
					detectorStem.setHasImageUri(object);
				} else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
					detectorStem.setHasWebDocument(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    detectorStem.setComment(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    detectorStem.setHascoTypeUri(object);
                } else if (predicate.equals(VSTOI.HAS_STATUS)) {
                    detectorStem.setHasStatus(object);
                } else if (predicate.equals(VSTOI.HAS_CONTENT)) {
                    detectorStem.setHasContent(object);
                } else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
                    detectorStem.setHasLanguage(object);
                } else if (predicate.equals(VSTOI.HAS_VERSION)) {
                    detectorStem.setHasVersion(object);
                } else if (predicate.equals(HASCO.DETECTS)) {
                    try {
                        detectorStem.setDetects(object);
                    } catch (Exception e) {
                    }
                } else if (predicate.equals(PROV.WAS_DERIVED_FROM)) {
                    try {
                        detectorStem.setWasDerivedFrom(object);
                    } catch (Exception e) {
                    }
                } else if (predicate.equals(PROV.WAS_GENERATED_BY)) {
                    try {
                        detectorStem.setWasGeneratedBy(object);
                    } catch (Exception e) {
                    }
				} else if (predicate.equals(VSTOI.HAS_REVIEW_NOTE)) {
					detectorStem.setHasReviewNote(object);
				} else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					detectorStem.setHasSIRManagerEmail(object);
				} else if (predicate.equals(VSTOI.HAS_EDITOR_EMAIL)) {
					detectorStem.setHasEditorEmail(object);
                } 
            }
        }

        detectorStem.setUri(uri);

        return detectorStem;
    }

    public static List<DetectorStem> derivation(String detectorStemuri) {
        if (detectorStemuri == null || detectorStemuri.isEmpty()) {
            return null;
        }
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:DetectorStem . " +
                " ?uri a ?detModel ." +
                " ?uri prov:wasDerivedFrom <" + detectorStemuri + "> . " +
                " ?uri vstoi:hasContent ?content . " +
                "} " +
                "ORDER BY ASC(?content) ";

        //System.out.println("Query: " + queryString);

        return findByQuery(queryString);
    }

    @Override
    public int compareTo(DetectorStem another) {
        return this.getLabel().compareTo(another.getLabel());
    }

    @Override public void save() {
        saveToTripleStore();
    }

    @Override public void delete() {
        deleteFromTripleStore();
    }

}

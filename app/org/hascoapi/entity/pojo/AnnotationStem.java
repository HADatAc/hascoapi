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

@JsonFilter("annotationStemFilter")
public class AnnotationStem extends HADatAcThing implements SIRElement, Comparable<AnnotationStem>  {

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

    public static AnnotationStem find(String uri) {
        if (uri == null || uri.isEmpty()) {
			return null;
		}
		AnnotationStem annotationStem = null;
		// Construct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
            annotationStem = new AnnotationStem();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				annotationStem.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);            
                if (predicate.equals(RDFS.LABEL)) {
                    annotationStem.setLabel(object);
                } else if (predicate.equals(RDF.TYPE)) {
                    annotationStem.setTypeUri(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    annotationStem.setComment(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    annotationStem.setHascoTypeUri(object);
                } else if (predicate.equals(HASCO.HAS_IMAGE)) {
                    annotationStem.setHasImageUri(object);
                } else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
                    annotationStem.setHasWebDocument(object);
                } else if (predicate.equals(VSTOI.HAS_STATUS)) {
                    annotationStem.setHasStatus(object);
                } else if (predicate.equals(VSTOI.HAS_CONTENT)) {
                    annotationStem.setHasContent(object);
                } else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
                    annotationStem.setHasLanguage(object);
                } else if (predicate.equals(VSTOI.HAS_VERSION)) {
                    annotationStem.setHasVersion(object);
                } else if (predicate.equals(PROV.WAS_DERIVED_FROM)) {
                    annotationStem.setWasDerivedFrom(object);
                } else if (predicate.equals(PROV.WAS_GENERATED_BY)) {
                    annotationStem.setWasGeneratedBy(object);
                } else if (predicate.equals(VSTOI.HAS_REVIEW_NOTE)) {
                    annotationStem.setHasReviewNote(object);
                } else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                    annotationStem.setHasSIRManagerEmail(object);
                } else if (predicate.equals(VSTOI.HAS_EDITOR_EMAIL)) {
                    annotationStem.setHasEditorEmail(object);
                }
            } 
        }

        annotationStem.setUri(uri);

        return annotationStem;
    }

    public static List<AnnotationStem> derivation(String annotationStemUri) {
        if (annotationStemUri == null || annotationStemUri.isEmpty()) {
            return null;
        }
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* vstoi:AnnotationStem . " +
                " ?uri a ?type ." +
                " ?uri prov:wasDerivedFrom <" + annotationStemUri + "> . " +
                " ?uri vstoi:hasContent ?content . " +
                "} " +
                "ORDER BY ASC(?content) ";

        //System.out.println("Query: " + queryString);

        return findByQuery(queryString);
    }

    public static List<Annotation> usage(String annotationStemUri) {
        if (annotationStemUri == null || annotationStemUri.isEmpty()) {
            return null;
        }
        List<Annotation> annotations = new ArrayList<Annotation>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* vstoi:Annotation . " +
                " ?uri a ?type ." +
                " ?uri vstoi:hasAnnotationStem <" + annotationStemUri + "> . " +
                " ?uri vstoi:belongsTo ?instUri . " +
                " ?instUri rdfs:label ?instLabel . " +
                "} " +
                "ORDER BY ASC(?instLabel) ";

        //System.out.println("Query: " + queryString);

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            //System.out.println("inside Detector.usage(): found uri [" + soln.getResource("uri").getURI().toString() + "]");
            Annotation annotation = Annotation.find(soln.getResource("uri").getURI());
            annotations.add(annotation);
        }
        return annotations;
    }

    private static List<AnnotationStem> findByQuery(String queryString) {
        List<AnnotationStem> annotationStems = new ArrayList<AnnotationStem>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            AnnotationStem annotationStem = find(soln.getResource("uri").getURI());
            annotationStems.add(annotationStem);
        }

        java.util.Collections.sort((List<AnnotationStem>) annotationStems);
        return annotationStems;

    }

    @Override
    public int compareTo(AnnotationStem another) {
        return this.getLabel().compareTo(another.getLabel());
    }

    @Override public void save() {
        saveToTripleStore();
    }

    @Override public void delete() {
        deleteFromTripleStore();
    }

}

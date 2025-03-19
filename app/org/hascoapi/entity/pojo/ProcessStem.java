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

@JsonFilter("processStemFilter")
public class ProcessStem extends HADatAcClass implements SIRElement, Comparable<ProcessStem>  {

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

    public ProcessStem () {
    }

    public ProcessStem (String className) {
		super(className);
    }

    public static ProcessStem find(String uri) {
 		if (uri == null || uri.isEmpty()) {
			return null;
		}
		ProcessStem processStem = null;
		// Construct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
            processStem = new ProcessStem();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				processStem.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);

				if (predicate.equals(RDFS.LABEL)) {
					processStem.setLabel(object);
				} else if (predicate.equals(RDFS.SUBCLASS_OF)) {
					processStem.setSuperUri(object); 
				} else if (predicate.equals(HASCO.HASCO_TYPE)) {
					processStem.setHascoTypeUri(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    processStem.setComment(object);
				} else if (predicate.equals(HASCO.HAS_IMAGE)) {
					processStem.setHasImageUri(object);
				} else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
					processStem.setHasWebDocument(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    processStem.setHascoTypeUri(object);
                } else if (predicate.equals(VSTOI.HAS_STATUS)) {
                    processStem.setHasStatus(object);
                } else if (predicate.equals(VSTOI.HAS_CONTENT)) {
                    processStem.setHasContent(object);
                } else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
                    processStem.setHasLanguage(object);
                } else if (predicate.equals(VSTOI.HAS_VERSION)) {
                    processStem.setHasVersion(object);
                } else if (predicate.equals(PROV.WAS_DERIVED_FROM)) {
                    try {
                        processStem.setWasDerivedFrom(object);
                    } catch (Exception e) {
                    }
                } else if (predicate.equals(PROV.WAS_GENERATED_BY)) {
                    try {
                        processStem.setWasGeneratedBy(object);
                    } catch (Exception e) {
                    }
				} else if (predicate.equals(VSTOI.HAS_REVIEW_NOTE)) {
					processStem.setHasReviewNote(object);
				} else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					processStem.setHasSIRManagerEmail(object);
				} else if (predicate.equals(VSTOI.HAS_EDITOR_EMAIL)) {
					processStem.setHasEditorEmail(object);
                } 
            }
        }

        processStem.setUri(uri);

        return processStem;
    }

    @Override
    public int compareTo(ProcessStem another) {
        return this.getLabel().compareTo(another.getLabel());
    }

    @Override public void save() {
        saveToTripleStore();
    }

    @Override public void delete() {
        deleteFromTripleStore();
    }

}

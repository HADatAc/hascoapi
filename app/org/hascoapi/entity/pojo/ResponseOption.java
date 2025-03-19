package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.PROV;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@JsonFilter("responseOptionFilter")
public class ResponseOption extends HADatAcThing implements SIRElement /*, Comparable<ResponseOption>*/ {

    @PropertyField(uri = "vstoi:hasStatus")    
    private String hasStatus;

    @PropertyField(uri = "vstoi:hasSerialNumber")
    String serialNumber;

    @PropertyField(uri = "vstoi:hasContent")
    String hasContent;

    @PropertyField(uri = "vstoi:hasLanguage")
    private String hasLanguage;

    @PropertyField(uri = "vstoi:hasVersion")
    String hasVersion;

    @PropertyField(uri = "vstoi:hasReviewNote")
    String hasReviewNote;

    @PropertyField(uri="prov:wasDerivedFrom")
    private String wasDerivedFrom;

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

    public String getSerialNumber() {
        return serialNumber;
    }       

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
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

    public void setHasVersion(String hasVersion) {
        this.hasVersion = hasVersion;
    }

    public String getHasReviewNote() {      
        return hasReviewNote;
    }

    public void setHasReviewNote(String hasReviewNote) {
        this.hasReviewNote = hasReviewNote;
    }

    public void setWasDerivedFrom(String wasDerivedFrom) {
        this.wasDerivedFrom = wasDerivedFrom;
    }

    public String getWasDerivedFrom() {
        return wasDerivedFrom;
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

    public static ResponseOption find(String uri) {
 		if (uri == null || uri.isEmpty()) {
			return null;
		}
		ResponseOption responseOption = null;
		// Construct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
            responseOption = new ResponseOption();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				responseOption.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);

                if (predicate.equals(RDFS.LABEL)) {
                    responseOption.setLabel(object);
                } else if (predicate.equals(RDF.TYPE)) {
                    responseOption.setTypeUri(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    responseOption.setComment(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    responseOption.setHascoTypeUri(object);
                } else if (predicate.equals(HASCO.HAS_IMAGE)) {
                    responseOption.setHasImageUri(object);
                } else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
                    responseOption.setHasWebDocument(object);
                } else if (predicate.equals(VSTOI.HAS_STATUS)) {
                    responseOption.setHasStatus(object);
                } else if (predicate.equals(VSTOI.HAS_SERIAL_NUMBER)) {
                    responseOption.setSerialNumber(object);
                } else if (predicate.equals(VSTOI.HAS_CONTENT)) {
                    responseOption.setHasContent(object);
                } else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
                    responseOption.setHasLanguage(object);
                } else if (predicate.equals(VSTOI.HAS_VERSION)) {
                    responseOption.setHasVersion(object);
                } else if (predicate.equals(VSTOI.HAS_REVIEW_NOTE)) {
                    responseOption.setHasReviewNote(object);
                } else if (predicate.equals(PROV.WAS_DERIVED_FROM)) {
                    responseOption.setWasDerivedFrom(object);
                } else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                    responseOption.setHasSIRManagerEmail(object);
                } else if (predicate.equals(VSTOI.HAS_EDITOR_EMAIL)) {
                    responseOption.setHasEditorEmail(object);
                }
            }
        }

        responseOption.setUri(uri);

        return responseOption;
    }

    public static boolean attach(CodebookSlot codebookSlot, ResponseOption responseOption) {
        if (codebookSlot == null) {
            return false;
        }
        return codebookSlot.updateCodebookSlotResponseOption(responseOption);
    }

    public static boolean detach(CodebookSlot codebookSlot) {
        if (codebookSlot == null) {
            return false;
        }
        return codebookSlot.updateCodebookSlotResponseOption(null);
    }

    /*
    @Override
    public int compareTo(ResponseOption another) {
        return this.getLabel().compareTo(another.getLabel());
    }
    */

    @Override
    public void save() {
        saveToTripleStore();
    }

    @Override
    public void delete() {
        deleteFromTripleStore();
    }

}

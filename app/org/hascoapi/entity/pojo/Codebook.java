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
import org.hascoapi.utils.Utils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.PROV;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;

import java.util.ArrayList;
import java.util.List;

import static org.hascoapi.Constants.*;

@JsonFilter("codebookFilter")
public class Codebook extends HADatAcThing implements Comparable<Codebook> {

    @PropertyField(uri = "vstoi:hasStatus")
    private String hasStatus;

    @PropertyField(uri = "vstoi:hasSerialNumber")
    private String serialNumber;

    @PropertyField(uri = "vstoi:hasLanguage")
    private String hasLanguage;

    @PropertyField(uri = "vstoi:hasVersion")
    private String hasVersion;

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

    public List<CodebookSlot> getCodebookSlots() {
        List<CodebookSlot> slots = CodebookSlot.findByCodebook(uri);
        return slots;
    }

    public boolean createCodebookSlots(int totSlots) {
        if (totSlots <= 0) {
            return false;
        }
        if (this.getCodebookSlots() != null || uri == null || uri.isEmpty()) {
            return false;
        }
        for (int aux = 1; aux <= totSlots; aux++) {
            String auxstr = Utils.adjustedPriority(String.valueOf(aux), totSlots);
            String newUri = uri + "/" + CODEBOOK_SLOT_PREFIX + "/" + auxstr;
            CodebookSlot.createCodebookSlot(uri, newUri, auxstr, null);
        }
        List<CodebookSlot> slotList = CodebookSlot.findByCodebook(uri);
        if (slotList == null) {
            return false;
        }
        return (slotList.size() == totSlots);
    }

    public boolean deleteCodebookSlots() {
        if (this.getCodebookSlots() == null || uri == null || uri.isEmpty()) {
            return true;
        }
        List<CodebookSlot> slots = CodebookSlot.findByCodebook(uri);
        if (slots == null) {
            return true;
        }
        for (CodebookSlot slot : slots) {
            slot.delete();
        }
        slots = CodebookSlot.findByCodebook(uri);
        return (slots == null);
    }

    public static Codebook find(String uri) {
 		if (uri == null || uri.isEmpty()) {
			return null;
		}
		Codebook codebook = null;
		// Construct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
            codebook = new Codebook();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				codebook.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);

				if (predicate.equals(RDFS.LABEL)) {
					codebook.setLabel(object);
                } else if (predicate.equals(RDF.TYPE)) {
                    codebook.setTypeUri(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    codebook.setComment(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    codebook.setHascoTypeUri(object);
				} else if (predicate.equals(HASCO.HAS_IMAGE)) {
					codebook.setHasImageUri(object);
				} else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
					codebook.setHasWebDocument(object);
                } else if (predicate.equals(VSTOI.HAS_STATUS)) {
                    codebook.setHasStatus(object);
                } else if (predicate.equals(VSTOI.HAS_SERIAL_NUMBER)) {
                    codebook.setSerialNumber(object);
                } else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
                    codebook.setHasLanguage(object);
                } else if (predicate.equals(VSTOI.HAS_VERSION)) {
                    codebook.setHasVersion(object);
                } else if (predicate.equals(VSTOI.HAS_REVIEW_NOTE)) {
                    codebook.setHasReviewNote(object);
                } else if (predicate.equals(PROV.WAS_DERIVED_FROM)) {
                    codebook.setWasDerivedFrom(object);
                } else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                    codebook.setHasSIRManagerEmail(object);
                } else if (predicate.equals(VSTOI.HAS_EDITOR_EMAIL)) {
                    codebook.setHasEditorEmail(object);
                }
            }
        }

        codebook.setUri(uri);

        return codebook;
    }

    @Override
    public int compareTo(Codebook another) {
        return this.getLabel().compareTo(another.getLabel());
    }

    @Override
    public void save() {
        saveToTripleStore();
    }

    @Override
    public void delete() {
        deleteFromTripleStore();
    }

}

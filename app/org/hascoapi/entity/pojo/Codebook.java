package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
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
        Codebook codebook = null;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

        codebook = new Codebook();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
 			String str = URIUtils.objectRDFToString(object);
			if (uri != null && !uri.isEmpty()) {
				if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
					codebook.setLabel(str);
                } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                    codebook.setTypeUri(str);
                } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                    codebook.setComment(str);
                } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                    codebook.setHascoTypeUri(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
                    codebook.setHasStatus(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SERIAL_NUMBER)) {
                    codebook.setSerialNumber(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
                    codebook.setHasLanguage(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
                    codebook.setHasVersion(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_REVIEW_NOTE)) {
                    codebook.setHasReviewNote(str);
                } else if (statement.getPredicate().getURI().equals(PROV.WAS_DERIVED_FROM)) {
                    codebook.setWasDerivedFrom(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                    codebook.setHasSIRManagerEmail(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_EDITOR_EMAIL)) {
                    codebook.setHasEditorEmail(str);
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

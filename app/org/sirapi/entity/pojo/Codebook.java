package org.sirapi.entity.pojo;

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
import org.sirapi.utils.Utils;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.RDF;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.VSTOI;

import java.util.ArrayList;
import java.util.List;

import static org.sirapi.Constants.*;

@JsonFilter("codebookFilter")
public class Codebook extends HADatAcThing implements SIRElement, Comparable<Codebook> {

    @PropertyField(uri = "vstoi:hasStatus")
    private String hasStatus;

    @PropertyField(uri = "vstoi:hasSerialNumber")
    private String serialNumber;

    @PropertyField(uri = "vstoi:hasLanguage")
    private String hasLanguage;

    @PropertyField(uri = "vstoi:hasVersion")
    private String hasVersion;

    @PropertyField(uri = "vstoi:hasSIRManagerEmail")
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

    public String getHasSIRManagerEmail() {
        return hasSIRManagerEmail;
    }

    public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail;
    }

    public List<ResponseOptionSlot> getResponseOptionSlots() {
        List<ResponseOptionSlot> slots = ResponseOptionSlot.findByCodebook(uri);
        return slots;
    }

    public boolean createResponseOptionSlots(int totSlots) {
        if (totSlots <= 0) {
            return false;
        }
        if (this.getResponseOptionSlots() != null || uri == null || uri.isEmpty()) {
            return false;
        }
        for (int aux = 1; aux <= totSlots; aux++) {
            String auxstr = Utils.adjustedPriority(String.valueOf(aux), totSlots);
            String newUri = uri + "/" + RESPONSE_OPTION_SLOT_PREFIX + "/" + auxstr;
            ResponseOptionSlot.createResponseOptionSlot(uri, newUri, auxstr, null);
        }
        List<ResponseOptionSlot> slotList = ResponseOptionSlot.findByCodebook(uri);
        if (slotList == null) {
            return false;
        }
        return (slotList.size() == totSlots);
    }

    public boolean deleteResponseOptionSlots() {
        if (this.getResponseOptionSlots() == null || uri == null || uri.isEmpty()) {
            return true;
        }
        List<ResponseOptionSlot> slots = ResponseOptionSlot.findByCodebook(uri);
        if (slots == null) {
            return true;
        }
        for (ResponseOptionSlot slot : slots) {
            slot.delete();
        }
        slots = ResponseOptionSlot.findByCodebook(uri);
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
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                codebook.setLabel(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                codebook.setTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                codebook.setComment(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                codebook.setHascoTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
                codebook.setHasStatus(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SERIAL_NUMBER)) {
                codebook.setSerialNumber(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
                codebook.setHasLanguage(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
                codebook.setHasVersion(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                codebook.setHasSIRManagerEmail(object.asLiteral().getString());
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

    @Override
    public boolean saveToSolr() {
        return false;
    }

    @Override
    public int deleteFromSolr() {
        return 0;
    }

}

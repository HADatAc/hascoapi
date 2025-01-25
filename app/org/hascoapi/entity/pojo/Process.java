package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.annotations.PropertyValueType;
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

@JsonFilter("processFilter")
public class Process extends HADatAcThing implements Comparable<Process> {

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

    @PropertyField(uri="vstoi:usesInstrument", valueType=PropertyValueType.URI)
    private List<String> instrumentUris = new ArrayList<String>();

    @PropertyField(uri="vstoi:requiresDetector", valueType=PropertyValueType.URI)
    private List<String> detectorUris = new ArrayList<String>();

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

    public List<String> getInstrumentUris() {
        return instrumentUris;
    }

    public List<Instrument> getInstruments() {
        List<Instrument> resp = new ArrayList<Instrument>();
        if (instrumentUris == null || instrumentUris.size() <= 0) {
            return resp;
        }
        for (String instrumentUri : instrumentUris) {
            Instrument instrument = Instrument.find(instrumentUri);
            if (instrument != null) {
                resp.add(instrument);
            }
        }
        return resp;
    }

    public boolean addInstrumentUri(String instrumentUri) {
        Instrument instrument = Instrument.find(instrumentUri);
        if (instrument == null) {
            return false;
        }
        if (instrumentUris == null) {
            instrumentUris = new ArrayList<String>();
        }
        if (instrumentUris == null || instrumentUris.contains(instrumentUri)) {
            return false; 
        }
        instrumentUris.add(instrumentUri);
        return true;
    }

    public boolean removeInstrumentUri(String instrumentUri) {
        Instrument instrument = Instrument.find(instrumentUri);
        if (instrument == null) {
            return false;
        }
        if (instrumentUris == null || !instrumentUris.contains(instrumentUri)) {
            return false; 
        }
        instrumentUris.remove(instrumentUri);
        return true;
    }

    public List<String> getDetectorUris() {
        return detectorUris;
    }

    public List<Detector> getDetectors() {
        List<Detector> resp = new ArrayList<Detector>();
        if (detectorUris == null || detectorUris.size() <= 0) {
            return resp;
        }
        for (String detectorUri : detectorUris) {
            Detector detector = Detector.find(detectorUri);
            if (detector != null) {
                resp.add(detector);
            }
        }
        return resp;
    }

    public boolean addDetectorUri(String detectorUri) {
        Detector detector = Detector.find(detectorUri);
        if (detector == null) {
            return false;
        }
        if (detectorUris == null) {
            detectorUris = new ArrayList<String>();
        }
        if (detectorUris == null || detectorUris.contains(detector)) {
            return false; 
        }
        detectorUris.add(detectorUri);
        return true;
    }

    public boolean removeDetectorUri(String detectorUri) {
        Detector detector = Detector.find(detectorUri);
        if (detector == null) {
            return false;
        }
        if (detectorUri == null || !detectorUris.contains(detectorUri)) {
            return false; 
        }
        detectorUris.remove(detectorUri);
        return true;
    }

    public static Process find(String uri) {
        Process process = null;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

        process = new Process();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
 			String str = URIUtils.objectRDFToString(object);
			if (uri != null && !uri.isEmpty()) {
				if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
					process.setLabel(str);
                } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                    process.setTypeUri(str);
                } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                    process.setComment(str);
                } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                    process.setHascoTypeUri(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
                    process.setHasStatus(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SERIAL_NUMBER)) {
                    process.setSerialNumber(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
                    process.setHasLanguage(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
                    process.setHasVersion(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_REVIEW_NOTE)) {
                    process.setHasReviewNote(str);
                } else if (statement.getPredicate().getURI().equals(PROV.WAS_DERIVED_FROM)) {
                    process.setWasDerivedFrom(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                    process.setHasSIRManagerEmail(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_EDITOR_EMAIL)) {
                    process.setHasEditorEmail(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.USES_INSTRUMENT)) {
                    process.addInstrumentUri(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.REQUIRES_DETECTOR)) {
                    process.addDetectorUri(str);
                }
            }
        }

        process.setUri(uri);

        return process;
    }

    @Override
    public int compareTo(Process another) {
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

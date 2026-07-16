package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.annotations.PropertyValueType;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProcessBasedStudy - A specialization of Study that requires a Process/Workflow
 * 
 * This class represents studies that are defined by an executable process or workflow.
 * It extends Study and adds the constraint that at least one Process must be associated.
 * 
 * The corresponding ontology class is hasco:ProcessBasedStudy (defined in HASCO v1.5).
 * 
 * Study metadata can be embedded in WKF Excel files and auto-generated if missing.
 * 
 * @see Study
 * @see Process
 */
@JsonFilter("processBasedStudyFilter")
public class ProcessBasedStudy extends Study {

    private static final Logger log = LoggerFactory.getLogger(ProcessBasedStudy.class);

    private static String className = "hasco:ProcessBasedStudy";

    /**
     * URI of the Process/Workflow that defines this study
     * Property: hasco:hasProcess
     * Cardinality: exactly 1 (required)
     */
    @PropertyField(uri = "hasco:hasProcess", valueType = PropertyValueType.URI)
    private String processUri;

    /**
     * Study-specific metadata fields (optional, auto-generated if empty)
     * These correspond to the new WKF v1.1 study metadata columns
     */
    
    @PropertyField(uri = "hasco:hasStudyID")
    private String studyID;

    @PropertyField(uri = "hasco:hasSpecificAims")
    private String specificAims;

    @PropertyField(uri = "hasco:hasSignificance")
    private String significance;

    @PropertyField(uri = "hasco:hasContactEmail")
    private String contactEmail;

    @PropertyField(uri = "hasco:hasStartDate")
    private String startDate;

    @PropertyField(uri = "hasco:hasEndDate")
    private String endDate;

    /**
     * Default constructor
     */
    public ProcessBasedStudy() {
        super();
        this.processUri = "";
        this.studyID = "";
        this.specificAims = "";
        this.significance = "";
        this.contactEmail = "";
        this.startDate = "";
        this.endDate = "";
    }

    /**
     * Full constructor
     */
    public ProcessBasedStudy(String id,
                             String uri,
                             String studyType,
                             String label,
                             String title,
                             String project,
                             String comment,
                             String externalSource,
                             String institutionUri,
                             String piUri,
                             String startDateTime,
                             String endDateTime,
                             String processUri) {
        super(id, uri, studyType, label, title, project, comment, externalSource,
              institutionUri, piUri, startDateTime, endDateTime);
        this.processUri = processUri;
        this.studyID = id;  // Default to study ID
    }

    // Getters and Setters

    public String getProcessUri() {
        return processUri;
    }

    public void setProcessUri(String processUri) {
        this.processUri = processUri;
    }

    /**
     * Get the associated Process object
     * @return Process object or null if not found
     */
    public Process getProcess() {
        if (processUri == null || processUri.trim().isEmpty()) {
            return null;
        }
        return Process.find(processUri);
    }

    public String getStudyID() {
        return studyID;
    }

    public void setStudyID(String studyID) {
        this.studyID = studyID;
    }

    public String getSpecificAims() {
        return specificAims;
    }

    public void setSpecificAims(String specificAims) {
        this.specificAims = specificAims;
    }

    public String getSignificance() {
        return significance;
    }

    public void setSignificance(String significance) {
        this.significance = significance;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    /**
     * Validate ProcessBasedStudy
     * A ProcessBasedStudy MUST have a process URI
     * @return true if valid, false otherwise
     */
    public boolean validate() {
        if (processUri == null || processUri.trim().isEmpty()) {
            log.error("ProcessBasedStudy validation failed: processUri is required");
            return false;
        }
        if (!processUri.contains("/PROC/")) {
            log.error("ProcessBasedStudy validation failed: processUri must contain /PROC/");
            return false;
        }
        return true;
    }

    /**
     * Derive Study URI from Process URI
     * Pattern: pmsr:WKF-{id}/PROC/{proc_id} → pmsr:STD-{id}
     * @return derived study URI
     */
    public String deriveStudyUriFromProcess() {
        if (processUri == null || processUri.isEmpty()) {
            return null;
        }
        
        // Extract base from process URI and replace WKF- with STD-
        // Example: http://pmsr.net/ont/pmsr#/WKF_SECRETION_001/PROC/0001
        //       -> http://pmsr.net/ont/pmsr#/STD_SECRETION_001
        if (processUri.contains("/WKF_") || processUri.contains("/WKF-")) {
            String baseUri = processUri.substring(0, processUri.indexOf("/PROC/"));
            baseUri = baseUri.replace("/WKF_", "/STD_").replace("/WKF-", "/STD-");
            return baseUri;
        }
        
        log.warn("Could not derive study URI from process URI: " + processUri);
        return processUri.replace("/PROC/", "/STD/");
    }

    /**
     * Find ProcessBasedStudy by URI
     * @param uri The study URI
     * @return ProcessBasedStudy object or null if not found
     */
    public static ProcessBasedStudy find(String uri) {
        if (uri == null || uri.isEmpty()) {
            return null;
        }

        ProcessBasedStudy study = null;
        
        // Construct SPARQL query
        String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
        ResultSet resultSet = SPARQLUtils.select(
            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), 
            queryString
        );

        if (!resultSet.hasNext()) {
            return null;
        }

        study = new ProcessBasedStudy();

        // Iterate over results and populate fields
        while (resultSet.hasNext()) {
            QuerySolution qs = resultSet.next();
            
            if (qs.contains("graph")) {
                study.setNamedGraph(qs.get("graph").toString());
            }
            
            if (qs.contains("p") && qs.contains("o")) {
                String predicate = qs.get("p").toString();
                String object = qs.get("o").toString();

                // Handle Study base properties
                if (predicate.equals(RDFS.LABEL)) {
                    study.setLabel(object);
                } else if (predicate.equals(HASCO.HAS_ID)) {
                    study.setId(object);
                } else if (predicate.equals(HASCO.HAS_TITLE)) {
                    study.setTitle(object);
                } else if (predicate.equals(RDF.TYPE)) {
                    study.setTypeUri(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    study.setHascoTypeUri(object);
                } else if (predicate.equals(VSTOI.HAS_STATUS)) {
                    study.setHasStatus(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    study.setComment(object);
                } else if (predicate.equals(HASCO.HAS_PROJECT)) {
                    study.setProject(object);
                } else if (predicate.equals(HASCO.HAS_INSTITUTION)) {
                    study.setInstitutionUri(object);
                } else if (predicate.equals(HASCO.HAS_PI)) {
                    study.setPiUri(object);
                } else if (predicate.equals(HASCO.HAS_VERSION)) {
                    study.setHasVersion(object);
                
                // Handle ProcessBasedStudy-specific properties
                } else if (predicate.equals(HASCO.HAS_PROCESS)) {
                    study.setProcessUri(object);
                } else if (predicate.equals(HASCO.HAS_STUDY_ID)) {
                    study.setStudyID(object);
                } else if (predicate.equals(HASCO.HAS_SPECIFIC_AIMS)) {
                    study.setSpecificAims(object);
                } else if (predicate.equals(HASCO.HAS_SIGNIFICANCE)) {
                    study.setSignificance(object);
                } else if (predicate.equals(HASCO.HAS_CONTACT_EMAIL)) {
                    study.setContactEmail(object);
                } else if (predicate.equals(HASCO.HAS_START_DATE)) {
                    study.setStartDate(object);
                } else if (predicate.equals(HASCO.HAS_END_DATE)) {
                    study.setEndDate(object);
                }
            }
        }

        study.setUri(uri);
        return study;
    }

    /**
     * Delete ProcessBasedStudy and associated Process
     * Implements cascade delete as per migration plan
     */
    @Override
    public void delete() {
        // Delete associated Process (cascade delete)
        if (processUri != null && !processUri.isEmpty()) {
            Process process = Process.find(processUri);
            if (process != null) {
                log.info("Cascade deleting process: " + processUri);
                process.delete();
            }
        }

        // Call parent delete (handles SOCs, measurements, etc.)
        super.delete();
    }

    @Override
    public String toString() {
        return "ProcessBasedStudy{" +
                "uri='" + uri + '\'' +
                ", id='" + getId() + '\'' +
                ", title='" + getTitle() + '\'' +
                ", processUri='" + processUri + '\'' +
                ", studyID='" + studyID + '\'' +
                '}';
    }
}

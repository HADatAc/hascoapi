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

import java.util.ArrayList;
import java.util.List;

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

    @PropertyField(uri = "hasco:hasStudyTitle")
    private String studyTitle;

    @PropertyField(uri = "hasco:hasSpecificAims")
    private String specificAims;

    @PropertyField(uri = "hasco:hasSignificance")
    private String significance;

    @PropertyField(uri = "hasco:hasInstitutionName")
    private String institutionName;

    @PropertyField(uri = "hasco:hasPrincipalInvestigator")
    private String principalInvestigator;

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
        this.studyTitle = "";
        this.specificAims = "";
        this.significance = "";
        this.institutionName = "";
        this.principalInvestigator = "";
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

    public String getStudyTitle() {
        return studyTitle;
    }

    public void setStudyTitle(String studyTitle) {
        this.studyTitle = studyTitle;
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

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getPrincipalInvestigator() {
        return principalInvestigator;
    }

    public void setPrincipalInvestigator(String principalInvestigator) {
        this.principalInvestigator = principalInvestigator;
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
                } else if (predicate.equals(HASCO.HAS_STUDY_TITLE)) {
                    study.setStudyTitle(object);
                } else if (predicate.equals(HASCO.HAS_SPECIFIC_AIMS)) {
                    study.setSpecificAims(object);
                } else if (predicate.equals(HASCO.HAS_SIGNIFICANCE)) {
                    study.setSignificance(object);
                } else if (predicate.equals("http://hadatac.org/ont/hasco/hasInstitutionName")) {
                    study.setInstitutionName(object);
                } else if (predicate.equals(HASCO.HAS_PRINCIPAL_INVESTIGATOR)) {
                    study.setPrincipalInvestigator(object);
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
     * Find ProcessBasedStudy by its associated Process URI
     * @param processUri The URI of the Process/Workflow
     * @return ProcessBasedStudy object or null if not found
     */
    public static ProcessBasedStudy findByProcess(String processUri) {
        if (processUri == null || processUri.isEmpty()) {
            return null;
        }

        // Query for ProcessBasedStudy where hasco:hasProcess = processUri
        String queryString = 
            "PREFIX hasco: <http://hadatac.org/ont/hasco/> " +
            "SELECT DISTINCT ?study WHERE { " +
            "  ?study a hasco:ProcessBasedStudy . " +
            "  ?study hasco:hasProcess <" + processUri + "> . " +
            "}";

        ResultSet resultSet = SPARQLUtils.select(
            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), 
            queryString
        );

        if (!resultSet.hasNext()) {
            return null;
        }

        QuerySolution qs = resultSet.next();
        if (qs.contains("study")) {
            String studyUri = qs.getResource("study").getURI();
            return find(studyUri);
        }

        return null;
    }

    /**
     * Find all ProcessBasedStudy entities with pagination
     * @param pageSize Number of results per page
     * @param offset Starting offset
     * @return List of ProcessBasedStudy objects
     */
    public static List<ProcessBasedStudy> findWithPages(int pageSize, int offset) {
        List<ProcessBasedStudy> studies = new ArrayList<>();

        String queryString = 
            "PREFIX hasco: <http://hadatac.org/ont/hasco/> " +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
            "SELECT DISTINCT ?uri ?label WHERE { " +
            "  ?uri a hasco:ProcessBasedStudy . " +
            "  OPTIONAL { ?uri rdfs:label ?label } " +
            "} " +
            "ORDER BY ?label " +
            "LIMIT " + pageSize + " " +
            "OFFSET " + offset;

        ResultSet resultSet = SPARQLUtils.select(
            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), 
            queryString
        );

        while (resultSet.hasNext()) {
            QuerySolution qs = resultSet.next();
            if (qs.contains("uri")) {
                String studyUri = qs.getResource("uri").getURI();
                ProcessBasedStudy study = find(studyUri);
                if (study != null) {
                    studies.add(study);
                }
            }
        }

        return studies;
    }

    /**
     * Find total count of ProcessBasedStudy entities
     * @return Total count
     */
    public static int findTotal() {
        String queryString = 
            "PREFIX hasco: <http://hadatac.org/ont/hasco/> " +
            "SELECT (COUNT(DISTINCT ?uri) AS ?count) WHERE { " +
            "  ?uri a hasco:ProcessBasedStudy . " +
            "}";

        ResultSet resultSet = SPARQLUtils.select(
            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), 
            queryString
        );

        if (resultSet.hasNext()) {
            QuerySolution qs = resultSet.next();
            if (qs.contains("count")) {
                return qs.getLiteral("count").getInt();
            }
        }

        return 0;
    }

    /**
     * Search ProcessBasedStudy by keyword in title, aims, or significance
     * @param keyword Search keyword
     * @param pageSize Number of results per page
     * @param offset Starting offset
     * @return List of matching ProcessBasedStudy objects
     */
    public static List<ProcessBasedStudy> findByKeyword(String keyword, int pageSize, int offset) {
        List<ProcessBasedStudy> studies = new ArrayList<>();

        String queryString = 
            "PREFIX hasco: <http://hadatac.org/ont/hasco/> " +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
            "SELECT DISTINCT ?uri WHERE { " +
            "  ?uri a hasco:ProcessBasedStudy . " +
            "  { " +
            "    ?uri hasco:hasTitle ?title . " +
            "    FILTER(CONTAINS(LCASE(?title), LCASE(\"" + keyword + "\"))) " +
            "  } UNION { " +
            "    ?uri hasco:hasSpecificAims ?aims . " +
            "    FILTER(CONTAINS(LCASE(?aims), LCASE(\"" + keyword + "\"))) " +
            "  } UNION { " +
            "    ?uri hasco:hasSignificance ?significance . " +
            "    FILTER(CONTAINS(LCASE(?significance), LCASE(\"" + keyword + "\"))) " +
            "  } UNION { " +
            "    ?uri hasco:hasStudyID ?studyID . " +
            "    FILTER(CONTAINS(LCASE(?studyID), LCASE(\"" + keyword + "\"))) " +
            "  } " +
            "} " +
            "LIMIT " + pageSize + " " +
            "OFFSET " + offset;

        ResultSet resultSet = SPARQLUtils.select(
            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), 
            queryString
        );

        while (resultSet.hasNext()) {
            QuerySolution qs = resultSet.next();
            if (qs.contains("uri")) {
                String studyUri = qs.getResource("uri").getURI();
                ProcessBasedStudy study = find(studyUri);
                if (study != null) {
                    studies.add(study);
                }
            }
        }

        return studies;
    }

    /**
     * Validate ProcessBasedStudy entity before saving
     * @return true if valid, false otherwise
     */
    public boolean validate() {
        // ProcessBasedStudy MUST have a Process URI
        if (processUri == null || processUri.isEmpty()) {
            errorMessage = "ProcessBasedStudy requires a Process URI (hasco:hasProcess)";
            return false;
        }

        // Process URI must match expected pattern
        if (!processUri.contains("/PROC/")) {
            errorMessage = "Invalid Process URI format (must contain /PROC/): " + processUri;
            return false;
        }

        // Study ID validation (if present)
        if (studyID != null && !studyID.isEmpty() && !studyID.startsWith("STD-")) {
            errorMessage = "Study ID must start with 'STD-': " + studyID;
            return false;
        }

        // Email validation (if present)
        if (contactEmail != null && !contactEmail.isEmpty() && !contactEmail.contains("@")) {
            errorMessage = "Invalid email format: " + contactEmail;
            return false;
        }

        // Date format validation (basic check - ISO 8601)
        if (startDate != null && !startDate.isEmpty() && !startDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            errorMessage = "Invalid start date format (must be YYYY-MM-DD): " + startDate;
            return false;
        }

        if (endDate != null && !endDate.isEmpty() && !endDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            errorMessage = "Invalid end date format (must be YYYY-MM-DD): " + endDate;
            return false;
        }

        return true;
    }

    private String errorMessage = "";

    /**
     * Get validation error message
     * @return Error message from last validation
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Save ProcessBasedStudy to triplestore
     */
    @Override
    public void save() {
        saveToTripleStore();
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
        deleteFromTripleStore();
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

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
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    private static String normalizePredicate(String predicate) {
        if (predicate == null) {
            return "";
        }
        return predicate.replace("#/", "#");
    }

    private static boolean predicateEquals(String predicate, String expected) {
        if (expected == null) {
            return false;
        }
        String normalizedPredicate = normalizePredicate(predicate);
        String normalizedExpected = normalizePredicate(expected);
        return normalizedPredicate.equals(normalizedExpected);
    }

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

    @PropertyField(uri = "vstoi:hasLearningObjectives")
    private String hasLearningObjectives;

    @PropertyField(uri = "vstoi:hasCriticalActions")
    private String hasCriticalActions;

    @PropertyField(uri = "vstoi:hasDebriefingFocus")
    private String hasDebriefingFocus;

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
        this.hasLearningObjectives = "";
        this.hasCriticalActions = "";
        this.hasDebriefingFocus = "";
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

    public String getHasLearningObjectives() {
        return hasLearningObjectives;
    }

    public void setHasLearningObjectives(String hasLearningObjectives) {
        this.hasLearningObjectives = hasLearningObjectives;
    }

    public String getHasCriticalActions() {
        return hasCriticalActions;
    }

    public void setHasCriticalActions(String hasCriticalActions) {
        this.hasCriticalActions = hasCriticalActions;
    }

    public String getHasDebriefingFocus() {
        return hasDebriefingFocus;
    }

    public void setHasDebriefingFocus(String hasDebriefingFocus) {
        this.hasDebriefingFocus = hasDebriefingFocus;
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

        processUri = URIUtils.canonicalizePmsrUri(processUri);
        
        // Extract base from process URI and replace WKF- with STD-
        // Example: http://pmsr.net/ont/pmsr#/WKF_SECRETION_001/PROC/0001
        //       -> http://pmsr.net/ont/pmsr#/STD_SECRETION_001
        if (processUri.contains("/WKF_") || processUri.contains("/WKF-") || processUri.contains("/WFK_") || processUri.contains("/WFK-")) {
            String baseUri = processUri.substring(0, processUri.indexOf("/PROC/"));
            baseUri = baseUri
                    .replace("/WKF_", "/STD-")
                    .replace("/WKF-", "/STD-")
                    .replace("/WFK_", "/STD-")
                    .replace("/WFK-", "/STD-")
                    .replace("_", "-");
            return URIUtils.canonicalizePmsrUri(baseUri);
        }
        
        log.warn("Could not derive study URI from process URI: " + processUri);
        return URIUtils.canonicalizePmsrUri(processUri.replace("/PROC/", "/STD/"));
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

        // Resolve ProcessBasedStudy from named graphs only.
        // WKF-derived scenario content must be persisted in the DFL-associated graph.
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
                if (predicateEquals(predicate, RDFS.LABEL)) {
                    study.setLabel(object);
                } else if (predicateEquals(predicate, HASCO.HAS_ID)) {
                    study.setId(object);
                } else if (predicateEquals(predicate, HASCO.HAS_TITLE)) {
                    study.setTitle(object);
                } else if (predicateEquals(predicate, RDF.TYPE)) {
                    study.setTypeUri(object);
                } else if (predicateEquals(predicate, HASCO.HASCO_TYPE)) {
                    study.setHascoTypeUri(object);
                } else if (predicateEquals(predicate, VSTOI.HAS_STATUS)) {
                    study.setHasStatus(object);
                } else if (predicateEquals(predicate, VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                    study.setHasSIRManagerEmail(object);
                } else if (predicateEquals(predicate, RDFS.COMMENT)) {
                    study.setComment(object);
                } else if (predicateEquals(predicate, HASCO.HAS_PROJECT)) {
                    study.setProject(object);
                } else if (predicateEquals(predicate, HASCO.HAS_INSTITUTION)) {
                    study.setInstitutionUri(object);
                } else if (predicateEquals(predicate, HASCO.HAS_PI)) {
                    study.setPiUri(object);
                } else if (predicateEquals(predicate, HASCO.HAS_VERSION)) {
                    study.setHasVersion(object);
                
                // Handle ProcessBasedStudy-specific properties
                } else if (predicateEquals(predicate, HASCO.HAS_PROCESS)) {
                    study.setProcessUri(object);
                } else if (predicateEquals(predicate, HASCO.HAS_STUDY_ID)) {
                    study.setStudyID(object);
                } else if (predicateEquals(predicate, HASCO.HAS_STUDY_TITLE)) {
                    study.setStudyTitle(object);
                } else if (predicateEquals(predicate, HASCO.HAS_SPECIFIC_AIMS)) {
                    study.setSpecificAims(object);
                } else if (predicateEquals(predicate, HASCO.HAS_SIGNIFICANCE)) {
                    study.setSignificance(object);
                } else if (predicateEquals(predicate, "http://hadatac.org/ont/hasco/hasInstitutionName")) {
                    study.setInstitutionName(object);
                } else if (predicateEquals(predicate, HASCO.HAS_PRINCIPAL_INVESTIGATOR)) {
                    study.setPrincipalInvestigator(object);
                } else if (predicateEquals(predicate, HASCO.HAS_CONTACT_EMAIL)) {
                    study.setContactEmail(object);
                } else if (predicateEquals(predicate, HASCO.HAS_START_DATE)) {
                    study.setStartDate(object);
                } else if (predicateEquals(predicate, HASCO.HAS_END_DATE)) {
                    study.setEndDate(object);
                } else if (predicateEquals(predicate, VSTOI.HAS_LEARNING_OBJECTIVES)) {
                    study.setHasLearningObjectives(object);
                } else if (predicateEquals(predicate, VSTOI.HAS_CRITICAL_ACTIONS)) {
                    study.setHasCriticalActions(object);
                } else if (predicateEquals(predicate, VSTOI.HAS_DEBRIEFING_FOCUS)) {
                    study.setHasDebriefingFocus(object);
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
            "  { ?study a hasco:ProcessBasedStudy . } UNION { ?study hasco:hascoType hasco:ProcessBasedStudy . } " +
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
     * Count ProcessBasedStudy entities that reference the given process URI.
     */
    public static int countByProcess(String processUri) {
        if (processUri == null || processUri.isEmpty()) {
            return 0;
        }

        String queryString =
            "PREFIX hasco: <http://hadatac.org/ont/hasco/> " +
            "SELECT (COUNT(DISTINCT ?study) AS ?count) WHERE { " +
            "  { ?study a hasco:ProcessBasedStudy . } UNION { ?study hasco:hascoType hasco:ProcessBasedStudy . } " +
            "  ?study hasco:hasProcess <" + processUri + "> . " +
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
     * Find DataFile URIs directly attached to a given owner URI via hasco:hasDataFile.
     */
    private static Set<String> findDataFileUrisForOwner(String ownerUri) {
        Set<String> uris = new LinkedHashSet<>();
        if (ownerUri == null || ownerUri.trim().isEmpty()) {
            return uris;
        }

        String queryString =
            NameSpaces.getInstance().printSparqlNameSpaceList() +
            " SELECT DISTINCT ?df WHERE { " +
            "   <" + ownerUri + "> hasco:hasDataFile ?df . " +
            " }";

        ResultSet resultSet = SPARQLUtils.select(
            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
            queryString
        );

        while (resultSet.hasNext()) {
            QuerySolution qs = resultSet.next();
            if (qs.contains("df") && qs.get("df").isResource()) {
                String dfUri = qs.getResource("df").getURI();
                if (dfUri != null && !dfUri.trim().isEmpty()) {
                    uris.add(dfUri.trim());
                }
            }
        }

        return uris;
    }

    /**
     * Delete DataFile graphs for the provided DataFile URIs.
     */
    private static void deleteDataFiles(Set<String> dataFileUris) {
        if (dataFileUris == null || dataFileUris.isEmpty()) {
            return;
        }

        for (String dfUri : dataFileUris) {
            try {
                DataFile df = DataFile.find(dfUri);
                if (df != null) {
                    df.delete();
                }
            }
            catch (Exception e) {
                log.warn("Failed to delete DataFile {} during ProcessBasedStudy deletion: {}", dfUri, e.getMessage());
            }
        }
    }

    /**
     * Delete only the ProcessBasedStudy (study layer) and its directly linked DataFiles.
     */
    public void deleteScenarioOnly() {
        Set<String> dataFiles = findDataFileUrisForOwner(this.getUri());
        deleteDataFiles(dataFiles);
        super.delete();
    }

    /**
     * Delete ProcessBasedStudy, linked process hierarchy (process + tasks), and linked DataFiles.
     */
    public void deleteScenarioWithProcessHierarchy() {
        Set<String> dataFiles = new LinkedHashSet<>();
        dataFiles.addAll(findDataFileUrisForOwner(this.getUri()));
        if (processUri != null && !processUri.trim().isEmpty()) {
            dataFiles.addAll(findDataFileUrisForOwner(processUri.trim()));
        }

        deleteDataFiles(dataFiles);

        if (processUri != null && !processUri.trim().isEmpty()) {
            Process process = Process.find(processUri.trim());
            if (process != null) {
                process.deleteWithTasks();
            }
        }

        super.delete();
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
            "  { ?uri a hasco:ProcessBasedStudy . } UNION { ?uri hasco:hascoType hasco:ProcessBasedStudy . } " +
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
            "  { ?uri a hasco:ProcessBasedStudy . } UNION { ?uri hasco:hascoType hasco:ProcessBasedStudy . } " +
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

        String safeKeyword = (keyword == null) ? "" : keyword.trim();
        if (safeKeyword.isEmpty() || safeKeyword.equals("_")) {
            return findWithPages(pageSize, offset);
        }

        String queryString = 
            "PREFIX hasco: <http://hadatac.org/ont/hasco/> " +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
            "SELECT DISTINCT ?uri WHERE { " +
            "  { ?uri a hasco:ProcessBasedStudy . } UNION { ?uri hasco:hascoType hasco:ProcessBasedStudy . } " +
            "  { " +
            "    ?uri hasco:hasTitle ?title . " +
            "    FILTER(CONTAINS(LCASE(?title), LCASE(\"" + safeKeyword + "\"))) " +
            "  } UNION { " +
            "    ?uri hasco:hasSpecificAims ?aims . " +
            "    FILTER(CONTAINS(LCASE(?aims), LCASE(\"" + safeKeyword + "\"))) " +
            "  } UNION { " +
            "    ?uri hasco:hasSignificance ?significance . " +
            "    FILTER(CONTAINS(LCASE(?significance), LCASE(\"" + safeKeyword + "\"))) " +
            "  } UNION { " +
            "    ?uri hasco:hasStudyID ?studyID . " +
            "    FILTER(CONTAINS(LCASE(?studyID), LCASE(\"" + safeKeyword + "\"))) " +
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

        // Date range validation: end date must not be before start date
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            if (endDate.compareTo(startDate) < 0) {
                errorMessage = "End date cannot be before start date: " + endDate + " < " + startDate;
                return false;
            }
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
        // Keep legacy behavior for callers that still use delete() directly.
        deleteScenarioWithProcessHierarchy();
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

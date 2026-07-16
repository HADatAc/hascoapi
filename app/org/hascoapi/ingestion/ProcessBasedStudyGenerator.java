package org.hascoapi.ingestion;

import java.lang.String;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.Process;
import org.hascoapi.entity.pojo.ProcessBasedStudy;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProcessBasedStudyGenerator - Generates ProcessBasedStudy instances from Process/Workflow entities
 * 
 * This generator is invoked after WKF ingestion to create ProcessBasedStudy entities
 * from the study metadata embedded in Process records. It:
 * 
 * 1. Reads study metadata from Process properties (WKF v1.1 study columns)
 * 2. Auto-generates missing metadata with intelligent defaults
 * 3. Derives Study URI from Process URI (WKF-{id} → STD-{id})
 * 4. Creates ProcessBasedStudy entity with hasProcess relationship
 * 5. Inserts triples into the knowledge graph
 * 
 * Auto-generation rules:
 * - Study ID: Derived from Process URI (pmsr:WKF-X/PROC/0001 → STD-X)
 * - Study Title: From Process rdfs:label or "Study for [Process Label]"
 * - Specific Aims: From Process rdfs:comment (first sentence) or default
 * - Significance: "Clinical simulation study" (default)
 * - Institution: From workflow creator's institution or "Unknown"
 * - Principal Investigator: Workflow creator or "Unknown"
 * - Contact Email: Creator's email or empty
 * - Start Date: Workflow creation date or current date (ISO 8601)
 * - End Date: Empty (open-ended)
 * 
 * @see ProcessBasedStudy
 * @see Process
 * @see WKFGenerator
 */
public class ProcessBasedStudyGenerator extends BaseGenerator {

    private static final Logger log = LoggerFactory.getLogger(ProcessBasedStudyGenerator.class);

    private String processUri;
    private String creatorEmail;
    private String creationDate;

    public ProcessBasedStudyGenerator(DataFile dataFile, String processUri) {
        super(dataFile);
        this.processUri = processUri;
        this.creatorEmail = dataFile.getHasSIRManagerEmail();
        // Use current date if not specified
        this.creationDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public ProcessBasedStudyGenerator(DataFile dataFile, String processUri, String creatorEmail, String creationDate) {
        super(dataFile);
        this.processUri = processUri;
        this.creatorEmail = creatorEmail != null ? creatorEmail : dataFile.getHasSIRManagerEmail();
        this.creationDate = creationDate != null ? creationDate : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    public void initMapping() {
        // No column mapping needed - we read from Process entity, not Excel
    }

    /**
     * Derives Study ID from Process URI
     * Pattern: pmsr:WKF-{id}/PROC/0001 → STD-{id}
     */
    private String deriveStudyIdFromProcessUri(String procUri) {
        if (procUri == null || procUri.isEmpty()) {
            log.warn("Cannot derive Study ID from null/empty Process URI");
            return null;
        }

        // Extract WKF-{id} from URI like pmsr:WKF-SECRETION-001/PROC/0001
        try {
            String[] parts = procUri.split("/");
            if (parts.length > 0) {
                String wkfPart = parts[0];
                if (wkfPart.contains("WKF-")) {
                    // Extract WKF-{id} and convert to STD-{id}
                    String id = wkfPart.substring(wkfPart.indexOf("WKF-") + 4);
                    return "STD-" + id;
                }
            }
            log.warn("Process URI does not contain WKF- pattern: {}", procUri);
            return null;
        } catch (Exception e) {
            log.error("Error deriving Study ID from Process URI: {}", procUri, e);
            return null;
        }
    }

    /**
     * Derives Study URI from Study ID
     * Pattern: STD-{id} → pmsr:STD-{id}
     */
    private String deriveStudyUri(String studyId) {
        if (studyId == null || studyId.isEmpty()) {
            return null;
        }
        // Remove STD- prefix if present, then add it back with namespace
        String id = studyId.startsWith("STD-") ? studyId.substring(4) : studyId;
        return Constants.PREFIX_STUDY + "-" + id;
    }

    /**
     * Auto-generates missing study metadata from Process properties
     */
    private void autoGenerateMetadata(Process process, Map<String, String> metadata) {
        // Study ID - required, derive from Process URI
        if (metadata.get("studyID") == null || metadata.get("studyID").isEmpty()) {
            String derivedId = deriveStudyIdFromProcessUri(processUri);
            if (derivedId != null) {
                metadata.put("studyID", derivedId);
                log.info("Auto-generated Study ID: {}", derivedId);
            } else {
                log.error("Failed to auto-generate Study ID from Process URI: {}", processUri);
            }
        }

        // Study Title - from Process label or auto-generate
        if (metadata.get("studyTitle") == null || metadata.get("studyTitle").isEmpty()) {
            String title = process.getLabel();
            if (title == null || title.isEmpty()) {
                title = "Study for " + metadata.get("studyID");
            }
            metadata.put("studyTitle", title);
            log.info("Auto-generated Study Title: {}", title);
        }

        // Specific Aims - from Process comment (first sentence) or default
        if (metadata.get("specificAims") == null || metadata.get("specificAims").isEmpty()) {
            String comment = process.getComment();
            String aims = "To execute and evaluate the workflow outcomes";
            if (comment != null && !comment.isEmpty()) {
                // Extract first sentence
                int periodIdx = comment.indexOf('.');
                if (periodIdx > 0) {
                    aims = comment.substring(0, periodIdx + 1);
                } else {
                    aims = comment;
                }
            }
            metadata.put("specificAims", aims);
            log.info("Auto-generated Specific Aims: {}", aims);
        }

        // Significance - default value
        if (metadata.get("significance") == null || metadata.get("significance").isEmpty()) {
            String significance = "Clinical simulation study";
            metadata.put("significance", significance);
            log.info("Auto-generated Significance: {}", significance);
        }

        // Institution - from creator profile or default
        if (metadata.get("institution") == null || metadata.get("institution").isEmpty()) {
            // TODO: Look up creator's institution from user profile
            String institution = "Unknown";
            metadata.put("institution", institution);
            log.info("Auto-generated Institution: {}", institution);
        }

        // Principal Investigator - workflow creator
        if (metadata.get("principalInvestigator") == null || metadata.get("principalInvestigator").isEmpty()) {
            String pi = creatorEmail != null && !creatorEmail.isEmpty() ? creatorEmail : "Unknown";
            metadata.put("principalInvestigator", pi);
            log.info("Auto-generated Principal Investigator: {}", pi);
        }

        // Contact Email - creator's email
        if (metadata.get("contactEmail") == null || metadata.get("contactEmail").isEmpty()) {
            String email = creatorEmail != null ? creatorEmail : "";
            metadata.put("contactEmail", email);
            log.info("Auto-generated Contact Email: {}", email);
        }

        // Start Date - creation date or current date
        if (metadata.get("startDate") == null || metadata.get("startDate").isEmpty()) {
            String startDate = creationDate != null ? creationDate : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            metadata.put("startDate", startDate);
            log.info("Auto-generated Start Date: {}", startDate);
        }

        // End Date - empty (open-ended)
        if (metadata.get("endDate") == null || metadata.get("endDate").isEmpty()) {
            metadata.put("endDate", "");
            log.info("End Date left empty (open-ended study)");
        }
    }

    /**
     * Creates a single ProcessBasedStudy row from a Process entity
     */
    @Override
    public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
        // This generator doesn't use Record/Excel - it reads from Process entity
        // This method is called by GeneratorChain, but we override createRowFromProcess instead
        throw new UnsupportedOperationException("ProcessBasedStudyGenerator does not use Record-based processing. Use createRowFromProcess() instead.");
    }

    /**
     * Creates a ProcessBasedStudy row from a Process entity (not from Excel Record)
     */
    public Map<String, Object> createRowFromProcess() throws Exception {
        log.info("Creating ProcessBasedStudy from Process: {}", processUri);

        // 1. Find the Process entity
        Process process = Process.find(processUri);
        if (process == null) {
            logger.printExceptionByIdWithArgs("GBL_00040", processUri);
            log.error("Process not found: {}", processUri);
            return null;
        }

        // 2. Extract study metadata from Process properties (WKF v1.1)
        Map<String, String> metadata = new HashMap<>();
        metadata.put("studyID", process.getStudyID());
        metadata.put("studyTitle", process.getStudyTitle());
        metadata.put("specificAims", process.getSpecificAims());
        metadata.put("significance", process.getSignificance());
        metadata.put("institution", process.getInstitution());
        metadata.put("principalInvestigator", process.getPrincipalInvestigator());
        metadata.put("contactEmail", process.getContactEmail());
        metadata.put("startDate", process.getStartDate());
        metadata.put("endDate", process.getEndDate());

        log.info("Extracted metadata from Process - studyID: {}, title: {}", 
                 metadata.get("studyID"), metadata.get("studyTitle"));

        // 3. Auto-generate missing metadata
        autoGenerateMetadata(process, metadata);

        // 4. Derive Study URI
        String studyId = metadata.get("studyID");
        if (studyId == null || studyId.isEmpty()) {
            logger.printExceptionById("GBL_00041");
            log.error("Cannot create ProcessBasedStudy without Study ID");
            return null;
        }

        String studyUri = deriveStudyUri(studyId);
        if (studyUri == null || studyUri.isEmpty()) {
            logger.printExceptionByIdWithArgs("GBL_00042", studyId);
            log.error("Failed to derive Study URI from Study ID: {}", studyId);
            return null;
        }

        log.info("Derived Study URI: {} from Study ID: {}", studyUri, studyId);

        // 5. Build the row with all properties
        Map<String, Object> row = new HashMap<>();
        
        // Required fields
        row.put("hasURI", studyUri);
        row.put("a", "hasco:ProcessBasedStudy");
        row.put("hasco:hascoType", HASCO.PROCESS_BASED_STUDY);
        row.put("hasco:hasId", studyId);
        row.put("rdfs:label", studyId);
        row.put("hasco:hasProcess", processUri);  // CRITICAL: Link to Process
        
        // Study metadata
        row.put("hasco:hasTitle", metadata.get("studyTitle"));
        row.put("hasco:hasStudyID", studyId);
        row.put("hasco:hasSpecificAims", metadata.get("specificAims"));
        row.put("hasco:hasSignificance", metadata.get("significance"));
        row.put("hasco:hasInstitution", metadata.get("institution"));
        row.put("hasco:hasPrincipalInvestigator", metadata.get("principalInvestigator"));
        row.put("hasco:hasContactEmail", metadata.get("contactEmail"));
        row.put("hasco:hasStartDate", metadata.get("startDate"));
        if (metadata.get("endDate") != null && !metadata.get("endDate").isEmpty()) {
            row.put("hasco:hasEndDate", metadata.get("endDate"));
        }
        
        // Provenance
        row.put("hasco:hasDataFile", dataFile.getUri());
        row.put("vstoi:hasSIRManagerEmail", creatorEmail);
        row.put("rdfs:comment", "ProcessBasedStudy auto-generated from workflow " + processUri);

        log.info("Created ProcessBasedStudy row: URI={}, hasProcess={}", studyUri, processUri);
        return row;
    }

    @Override
    public String getTableName() {
        return "ProcessBasedStudy";
    }

    @Override
    public String getErrorMsg(Exception e) {
        return "Error in ProcessBasedStudyGenerator: " + e.getMessage();
    }

    /**
     * Validates that the Process exists and has required properties
     */
    public boolean validateProcess() {
        if (processUri == null || processUri.isEmpty()) {
            log.error("Process URI is null or empty");
            return false;
        }

        Process process = Process.find(processUri);
        if (process == null) {
            log.error("Process not found: {}", processUri);
            return false;
        }

        // Process must have label and type
        if (process.getLabel() == null || process.getLabel().isEmpty()) {
            log.warn("Process has no label: {}", processUri);
        }

        if (process.getTypeUri() == null || process.getTypeUri().isEmpty()) {
            log.warn("Process has no type: {}", processUri);
        }

        return true;
    }
}

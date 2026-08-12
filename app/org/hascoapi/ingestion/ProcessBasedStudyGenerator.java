package org.hascoapi.ingestion;

import java.lang.String;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.Organization;
import org.hascoapi.entity.pojo.Person;
import org.hascoapi.entity.pojo.Process;
import org.hascoapi.entity.pojo.ProcessBasedStudy;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
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
    private Map<String, String> stdMetadata;

    private static class UserContext {
        String personUri = "";
        String personDisplay = "";
        String organizationUri = "";
        String organizationDisplay = "";
    }

    /**
     * Normalize email values from ingestion context.
     */
    private String normalizeEmailValue(String raw) {
        if (raw == null) {
            return "";
        }

        String value = raw.trim().toLowerCase();
        if (value.isEmpty()) {
            return "";
        }

        if (value.startsWith("mailto:")) {
            value = value.substring("mailto:".length()).trim();
        }

        int managerIdx = value.indexOf("manageremail=");
        if (managerIdx >= 0) {
            int start = managerIdx + "manageremail=".length();
            int endAmp = value.indexOf('&', start);
            value = (endAmp > start) ? value.substring(start, endAmp).trim() : value.substring(start).trim();
        }

        int qPos = value.indexOf('?');
        if (qPos >= 0) {
            value = value.substring(0, qPos).trim();
        }

        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("[a-z0-9._%+\\-]+@[a-z0-9.\\-]+\\.[a-z]{2,}")
            .matcher(value);
        if (m.find()) {
            return m.group().trim();
        }

        return value;
    }

    public ProcessBasedStudyGenerator(DataFile dataFile, String processUri) {
        super(dataFile);
        this.processUri = URIUtils.canonicalizePmsrUri(processUri);
        this.creatorEmail = normalizeEmailValue(dataFile.getHasSIRManagerEmail());
        // Use current date if not specified
        this.creationDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        this.stdMetadata = new HashMap<>();
    }

    public ProcessBasedStudyGenerator(DataFile dataFile, String processUri, String creatorEmail, String creationDate) {
        this(dataFile, processUri, creatorEmail, creationDate, null);
    }

    public ProcessBasedStudyGenerator(DataFile dataFile, String processUri, String creatorEmail, String creationDate, Map<String, String> stdMetadata) {
        super(dataFile);
        this.processUri = URIUtils.canonicalizePmsrUri(processUri);
        this.creatorEmail = normalizeEmailValue(creatorEmail != null ? creatorEmail : dataFile.getHasSIRManagerEmail());
        this.creationDate = creationDate != null ? creationDate : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        this.stdMetadata = normalizeStdMetadata(stdMetadata);
    }

    private Map<String, String> normalizeStdMetadata(Map<String, String> source) {
        Map<String, String> normalized = new HashMap<>();
        if (source == null || source.isEmpty()) {
            return normalized;
        }

        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String key = entry.getKey().trim();
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (key.isEmpty() || value.isEmpty()) {
                continue;
            }
            normalized.put(key, value);
        }

        return normalized;
    }

    private void applyStdMetadata(Map<String, String> metadata) {
        if (stdMetadata == null || stdMetadata.isEmpty() || metadata == null) {
            return;
        }

        putIfPresent(metadata, "studyID", stdMetadata.get("Study ID"));
        putIfPresent(metadata, "studyTitle", stdMetadata.get("Title"));
        putIfPresent(metadata, "specificAims", stdMetadata.get("Specific Aims"));
        putIfPresent(metadata, "significance", stdMetadata.get("Significance"));
        putIfPresent(metadata, "institution", stdMetadata.get("Institution"));
        putIfPresent(metadata, "principalInvestigator", stdMetadata.get("Principal Investigator"));
        putIfPresent(metadata, "contactEmail", stdMetadata.get("Email"));
        putIfPresent(metadata, "startDate", stdMetadata.get("Start Date"));
        putIfPresent(metadata, "endDate", stdMetadata.get("End Date"));
        putIfPresent(metadata, "hasLearningObjectives", stdMetadata.get("vstoi:hasLearningObjectives"));
        putIfPresent(metadata, "hasCriticalActions", stdMetadata.get("vstoi:hasCriticalActions"));
        putIfPresent(metadata, "hasDebriefingFocus", stdMetadata.get("vstoi:hasDebriefingFocus"));
        putIfPresent(metadata, "hasStudyDescription", stdMetadata.get("hasStudyDescription"));

        if ((metadata.get("specificAims") == null || metadata.get("specificAims").isEmpty())
                && metadata.get("hasStudyDescription") != null
                && !metadata.get("hasStudyDescription").trim().isEmpty()) {
            metadata.put("specificAims", metadata.get("hasStudyDescription").trim());
        }
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (map == null || key == null || key.trim().isEmpty()) {
            return;
        }
        if (value != null && !value.trim().isEmpty()) {
            map.put(key, value.trim());
        }
    }

    private String normalizedUri(String raw) {
        if (raw == null) {
            return "";
        }

        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }

        return URIUtils.canonicalizePmsrUri(value);
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

        procUri = URIUtils.canonicalizePmsrUri(procUri);

        // Extract WKF token from URI like
        //   .../WKF-SECRETION-001/PROC/0001 or .../WKF_SECRETION_001/PROC/0001
        try {
            String marker = "/PROC/";
            int procIdx = procUri.indexOf(marker);
            String head = procIdx > 0 ? procUri.substring(0, procIdx) : procUri;

            int slashIdx = head.lastIndexOf('/');
            String wkfPart = slashIdx >= 0 ? head.substring(slashIdx + 1) : head;

            if (wkfPart.startsWith("WKF-")) {
                String id = wkfPart.substring(4);
                return "STD-" + id.replace('_', '-');
            }

            if (wkfPart.startsWith("WKF_")) {
                String id = wkfPart.substring(4);
                return "STD-" + id.replace('_', '-');
            }

            if (wkfPart.startsWith("WFK-") || wkfPart.startsWith("WFK_")) {
                String id = wkfPart.substring(4);
                return "STD-" + id.replace('_', '-');
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
        if (processUri != null && !processUri.isEmpty() && processUri.contains("/PROC/")) {
            String baseUri = URIUtils.canonicalizePmsrUri(processUri.substring(0, processUri.indexOf("/PROC/")));
            if (baseUri.contains("/WKF-")) {
                return URIUtils.canonicalizePmsrUri(baseUri.replace("/WKF-", "/STD-"));
            }
            if (baseUri.contains("/WKF_")) {
                return URIUtils.canonicalizePmsrUri(baseUri.replace("/WKF_", "/STD-"));
            }
            if (baseUri.contains("/WFK-") || baseUri.contains("/WFK_")) {
                return URIUtils.canonicalizePmsrUri(baseUri.replace("/WFK-", "/STD-").replace("/WFK_", "/STD-"));
            }
        }

        if (studyId == null || studyId.isEmpty()) {
            return null;
        }
        // Remove STD- prefix if present, then add it back with namespace
        String normalizedStudyId = studyId.replace('_', '-');
        String id = normalizedStudyId.startsWith("STD-") ? normalizedStudyId.substring(4) : normalizedStudyId;
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

        if (metadata.get("hasStudyDescription") == null || metadata.get("hasStudyDescription").isEmpty()) {
            String comment = process.getComment();
            if (comment != null && !comment.trim().isEmpty()) {
                metadata.put("hasStudyDescription", comment.trim());
            } else if (metadata.get("specificAims") != null && !metadata.get("specificAims").trim().isEmpty()) {
                metadata.put("hasStudyDescription", metadata.get("specificAims").trim());
            }
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

    private UserContext resolveUserContext() {
        UserContext context = new UserContext();

        String email = creatorEmail != null ? creatorEmail.trim() : "";
        if (email.isEmpty()) {
            return context;
        }

        try {
            Person person = Person.findByEmail(email);
            if (person != null) {
                if (person.getUri() != null) {
                    context.personUri = person.getUri().trim();
                }
                if (person.getName() != null && !person.getName().trim().isEmpty()) {
                    context.personDisplay = person.getName().trim();
                } else if (person.getUserName() != null && !person.getUserName().trim().isEmpty()) {
                    context.personDisplay = person.getUserName().trim();
                } else if (person.getLabel() != null && !person.getLabel().trim().isEmpty()) {
                    context.personDisplay = person.getLabel().trim();
                }

                String affiliationUri = person.getHasAffiliationUri();
                if (affiliationUri != null && !affiliationUri.trim().isEmpty()) {
                    context.organizationUri = affiliationUri.trim();
                    try {
                        Organization org = person.getHasAffiliation();
                        if (org != null) {
                            if (org.getName() != null && !org.getName().trim().isEmpty()) {
                                context.organizationDisplay = org.getName().trim();
                            } else if (org.getLabel() != null && !org.getLabel().trim().isEmpty()) {
                                context.organizationDisplay = org.getLabel().trim();
                            }
                        }
                    } catch (Exception ignored) {
                        // Keep URI even if organization object lookup fails.
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve Person by email {}", email, e);
        }

        if (!context.personUri.isEmpty() && !context.organizationUri.isEmpty()) {
            return context;
        }

        String escaped = email.replace("\\", "\\\\").replace("\"", "\\\"");
        String query = NameSpaces.getInstance().printSparqlNameSpaceList()
            + "SELECT DISTINCT ?person ?org ?pName ?pLabel ?oName ?oLabel WHERE { "
            + "  VALUES ?inputEmail { \"" + escaped + "\" } "
            + "  ?personType rdfs:subClassOf* schema:Person . "
            + "  ?person a ?personType . "
            + "  { ?person foaf:mbox ?emailRaw . } "
            + "  UNION { ?person hasco:userEmail ?emailRaw . } "
            + "  UNION { ?person vstoi:hasSIRManagerEmail ?emailRaw . } "
            + "  FILTER( LCASE(STR(?emailRaw)) = LCASE(?inputEmail) || LCASE(STR(?emailRaw)) = CONCAT(\"mailto:\", LCASE(?inputEmail)) ) "
            + "  OPTIONAL { ?person foaf:member ?org . } "
            + "  OPTIONAL { ?org foaf:member ?person . } "
            + "  OPTIONAL { ?person foaf:name ?pName . } "
            + "  OPTIONAL { ?person rdfs:label ?pLabel . } "
            + "  OPTIONAL { ?org foaf:name ?oName . } "
            + "  OPTIONAL { ?org rdfs:label ?oLabel . } "
            + "} LIMIT 1";

        try {
            ResultSetRewindable rs = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
            if (rs != null && rs.hasNext()) {
                QuerySolution sol = rs.next();

                if (context.personUri.isEmpty() && sol.get("person") != null && sol.get("person").isResource()) {
                    context.personUri = sol.getResource("person").getURI();
                }
                if (context.organizationUri.isEmpty() && sol.get("org") != null && sol.get("org").isResource()) {
                    context.organizationUri = sol.getResource("org").getURI();
                }
                if (context.personDisplay.isEmpty()) {
                    if (sol.get("pName") != null) {
                        context.personDisplay = sol.get("pName").toString();
                    } else if (sol.get("pLabel") != null) {
                        context.personDisplay = sol.get("pLabel").toString();
                    }
                }
                if (context.organizationDisplay.isEmpty()) {
                    if (sol.get("oName") != null) {
                        context.organizationDisplay = sol.get("oName").toString();
                    } else if (sol.get("oLabel") != null) {
                        context.organizationDisplay = sol.get("oLabel").toString();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve user context by SPARQL for {}", email, e);
        }

        return context;
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

        // 2. Extract study metadata from Process properties (legacy fallback)
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

        // 3. STD sheet is canonical in WKF v1.2.1; apply STD values before autogeneration.
        applyStdMetadata(metadata);

        // 4. Auto-generate any remaining missing metadata.
        autoGenerateMetadata(process, metadata);

        // 4.1 Resolve current user context for URI-based Study ownership fields
        UserContext userContext = resolveUserContext();

        String ingestionOrganizationOverride = normalizedUri(dataFile.getIngestionOrganizationUri());
        if (!ingestionOrganizationOverride.isEmpty()) {
            if (!userContext.organizationUri.isEmpty() && !userContext.organizationUri.equals(ingestionOrganizationOverride)) {
                log.warn("User affiliation URI ({}) overridden by ingestion request organization URI ({})", userContext.organizationUri, ingestionOrganizationOverride);
            }
            userContext.organizationUri = ingestionOrganizationOverride;
            try {
                Organization targetOrg = Organization.find(ingestionOrganizationOverride);
                if (targetOrg != null) {
                    if (targetOrg.getName() != null && !targetOrg.getName().trim().isEmpty()) {
                        userContext.organizationDisplay = targetOrg.getName().trim();
                    } else if (targetOrg.getLabel() != null && !targetOrg.getLabel().trim().isEmpty()) {
                        userContext.organizationDisplay = targetOrg.getLabel().trim();
                    }
                }
            } catch (Exception ignored) {
                // Keep URI override even if organization label lookup fails.
            }
        }

        String stdPiUri = stdMetadata == null ? "" : stdMetadata.get("Principal Investigator");
        String stdInstitutionUri = stdMetadata == null ? "" : stdMetadata.get("Institution");

        // User context has precedence over WKF STD content for ownership semantics.
        // If STD and user URIs disagree, keep user context and log the override.
        String normalizedStdPiUri = normalizedUri(stdPiUri);
        String normalizedUserPiUri = normalizedUri(userContext.personUri);
        if (!normalizedStdPiUri.isEmpty() && !normalizedUserPiUri.isEmpty()
                && !normalizedStdPiUri.equals(normalizedUserPiUri)) {
            log.warn("WKF STD PI URI ({}) overridden by ingestion user URI ({})", stdPiUri, userContext.personUri);
        }

        String normalizedStdInstitutionUri = normalizedUri(stdInstitutionUri);
        String normalizedUserInstitutionUri = normalizedUri(userContext.organizationUri);
        if (!normalizedStdInstitutionUri.isEmpty() && !normalizedUserInstitutionUri.isEmpty()
                && !normalizedStdInstitutionUri.equals(normalizedUserInstitutionUri)) {
            log.warn("WKF STD Institution URI ({}) overridden by ingestion user affiliation URI ({})", stdInstitutionUri, userContext.organizationUri);
        }

        if (!userContext.personDisplay.isEmpty()) {
            metadata.put("principalInvestigator", userContext.personDisplay);
        } else if (creatorEmail != null && !creatorEmail.trim().isEmpty()) {
            metadata.put("principalInvestigator", creatorEmail.trim());
        }

        if (!userContext.organizationDisplay.isEmpty()) {
            metadata.put("institution", userContext.organizationDisplay);
        }

        // Start Date remains a strict dependency for label composition,
        // but its value must be the WKF submission datetime.
        metadata.put("startDate", resolveStartDateFromSubmissionTimeStrict());

        // 5. Derive Study URI
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

        String studyLabel = buildStudyInstanceLabel(process, metadata);
        if (metadata.get("studyTitle") == null || metadata.get("studyTitle").trim().isEmpty()) {
            metadata.put("studyTitle", process.getLabel());
        }

        // 6. Build the row with all properties
        Map<String, Object> row = new HashMap<>();
        
        // Required fields
        row.put("hasURI", studyUri);
        row.put("a", "hasco:ProcessBasedStudy");
        row.put("hasco:hascoType", HASCO.PROCESS_BASED_STUDY);
        row.put("hasco:hasId", studyId);
        row.put("rdfs:label", studyLabel);
        row.put("hasco:hasProcess", processUri);  // CRITICAL: Link to Process
        
        // Study metadata
        row.put("hasco:hasTitle", metadata.get("studyTitle"));
        row.put("hasco:hasStudyID", studyId);
        row.put("hasco:hasSpecificAims", metadata.get("specificAims"));
        row.put("hasco:hasSignificance", metadata.get("significance"));

        if (userContext.organizationUri != null && !userContext.organizationUri.trim().isEmpty()) {
            row.put("hasco:hasInstitution", userContext.organizationUri.trim());
        } else if (stdInstitutionUri != null && !stdInstitutionUri.trim().isEmpty()) {
            row.put("hasco:hasInstitution", stdInstitutionUri.trim());
        }

        if (metadata.get("institution") != null && !metadata.get("institution").trim().isEmpty()) {
            row.put("hasco:hasInstitutionName", metadata.get("institution"));
        }

        if (userContext.personUri != null && !userContext.personUri.trim().isEmpty()) {
            row.put("hasco:hasPI", userContext.personUri.trim());
        } else if (stdPiUri != null && !stdPiUri.trim().isEmpty()) {
            row.put("hasco:hasPI", stdPiUri.trim());
        }

        if (metadata.get("principalInvestigator") != null && !metadata.get("principalInvestigator").trim().isEmpty()) {
            row.put("hasco:hasPrincipalInvestigator", metadata.get("principalInvestigator"));
        }

        row.put("hasco:hasContactEmail", metadata.get("contactEmail"));
        row.put("hasco:hasStartDate", metadata.get("startDate"));
        if (metadata.get("endDate") != null && !metadata.get("endDate").isEmpty()) {
            row.put("hasco:hasEndDate", metadata.get("endDate"));
        }

        if (metadata.get("hasLearningObjectives") != null && !metadata.get("hasLearningObjectives").trim().isEmpty()) {
            row.put("vstoi:hasLearningObjectives", metadata.get("hasLearningObjectives"));
        }
        if (metadata.get("hasCriticalActions") != null && !metadata.get("hasCriticalActions").trim().isEmpty()) {
            row.put("vstoi:hasCriticalActions", metadata.get("hasCriticalActions"));
        }
        if (metadata.get("hasDebriefingFocus") != null && !metadata.get("hasDebriefingFocus").trim().isEmpty()) {
            row.put("vstoi:hasDebriefingFocus", metadata.get("hasDebriefingFocus"));
        }
        
        // Provenance
        row.put("hasco:hasDataFile", dataFile.getUri());
        row.put("vstoi:hasSIRManagerEmail", creatorEmail);
        String studyDescription = metadata.get("hasStudyDescription") != null
            ? metadata.get("hasStudyDescription").trim()
            : "";
        if (!studyDescription.isEmpty()) {
            row.put("rdfs:comment", studyDescription);
        } else {
            row.put("rdfs:comment", "ProcessBasedStudy auto-generated from workflow " + processUri);
        }

        log.info("Created ProcessBasedStudy row: URI={}, hasProcess={}", studyUri, processUri);
        return row;
    }

    /**
     * Build study instance label as:
     * [Person Full Name]'s [Process Name] at [YYYY/MM/DD] [HH:MM]
     *
     * This method is strict by design and throws when any required label part
     * cannot be resolved exactly from source data.
     */
    private String buildStudyInstanceLabel(Process process, Map<String, String> metadata) throws Exception {
        String processName = resolveProcessNameStrict(process);
        String[] startDateTimeParts = resolveLabelStartDateTimePartsStrict(metadata);

        String userDisplayName = "User";
        String email = creatorEmail != null ? creatorEmail.trim() : "";
        if (!email.isEmpty()) {
            try {
                Person person = Person.findByEmail(email);
                if (person != null) {
                    if (person.getName() != null && !person.getName().trim().isEmpty()) {
                        userDisplayName = person.getName().trim();
                    } else if (person.getUserName() != null && !person.getUserName().trim().isEmpty()) {
                        userDisplayName = person.getUserName().trim();
                    } else if (person.getLabel() != null && !person.getLabel().trim().isEmpty()) {
                        userDisplayName = person.getLabel().trim();
                    }
                }
            } catch (Exception e) {
                // Keep fallback behavior on lookup failure.
            }
        }

        return userDisplayName + "'s " + processName + " at " + startDateTimeParts[0] + " " + startDateTimeParts[1];
    }

    private String resolveProcessNameStrict(Process process) throws Exception {
        if (process == null) {
            throw new Exception("Cannot compose ProcessBasedStudy rdfs:label: Process is null");
        }

        String processLabel = process.getLabel() == null ? "" : process.getLabel().trim();
        if (processLabel.isEmpty()) {
            throw new Exception("Cannot compose ProcessBasedStudy rdfs:label: Process rdfs:label is missing for " + process.getUri());
        }

        return processLabel;
    }

    /**
     * Resolve Start Date from DataFile submission timestamp.
     * Returned in ISO local datetime format to preserve strict Start Date
     * dependency downstream.
     */
    private String resolveStartDateFromSubmissionTimeStrict() throws Exception {
        String raw = dataFile != null && dataFile.getSubmissionTime() != null
            ? dataFile.getSubmissionTime().trim()
            : "";

        if (raw.isEmpty()) {
            throw new Exception("Cannot compose ProcessBasedStudy rdfs:label: DataFile submission timestamp is missing");
        }

        LocalDateTime dateTime = parseExactDateTime(raw);
        if (dateTime == null) {
            throw new Exception("Cannot compose ProcessBasedStudy rdfs:label: DataFile submission timestamp is not parseable as exact datetime: " + raw);
        }

        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    /**
     * Resolve label date/time from Start Date metadata.
     * This keeps Start Date as a strict dependency for label generation.
     */
    private String[] resolveLabelStartDateTimePartsStrict(Map<String, String> metadata) throws Exception {
        String raw = "";
        if (metadata != null) {
            raw = metadata.get("startDate") == null ? "" : metadata.get("startDate").trim();
        }

        if (raw.isEmpty()) {
            throw new Exception("Cannot compose ProcessBasedStudy rdfs:label: Start Date is missing");
        }

        LocalDateTime dateTime = parseExactDateTime(raw);
        if (dateTime == null) {
            throw new Exception("Cannot compose ProcessBasedStudy rdfs:label: Start Date is not parseable as exact datetime: " + raw);
        }

        return new String[] {
            dateTime.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
            dateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        };
    }

    private LocalDateTime parseExactDateTime(String raw) {
        LocalDateTime dateTime = null;

        // Primary format used by DataFile submission time: yyyy/MM/dd HH:mm:ss
        try {
            dateTime = LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
        }

        // Compatibility fallback: allow ISO date-time if persisted in that format.
        if (dateTime == null) {
            String normalized = raw.replace(' ', 'T');

            try {
                dateTime = OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
            }

            if (dateTime == null) {
                try {
                    dateTime = LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                } catch (DateTimeParseException ignored) {
                }
            }

            if (dateTime == null) {
                try {
                    dateTime = LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                } catch (DateTimeParseException ignored) {
                }
            }
        }

        return dateTime;
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

package org.hascoapi.ingestion;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.Organization;
import org.hascoapi.entity.pojo.Person;
import org.hascoapi.entity.pojo.ProcessStem;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.VSTOI;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;

import java.util.HashMap;
import java.util.Map;

public class WKFGenerator extends BaseGenerator {

    private static final String LEGACY_PMSR_PROCESS_STEM_URI = "https://pmsr.net/ont/MedicalSimulationProcessStem";
    private static final String CANONICAL_PMSR_PROCESS_STEM_URI = "https://pmsr.net/ont/MedicalSimulationProcessStem";
    private static final String LEGACY_PMSR_BASE = "https://pmsr.net/ont/";
    private static final String LEGACY_PMSR_BASE_HTTPS = "https://pmsr.net/ont/";
    private static final String CANONICAL_PMSR_BASE = "https://pmsr.net/ont/";
    private static final java.util.regex.Pattern EMAIL_PATTERN = java.util.regex.Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,}$");

    protected String wkfUri = "";
    protected String hasStatus = "";
    private Map<String, String> processStemLabelByUri = null;

    public String getWKFUri() {
        return wkfUri;
    }

    public void setWKFUri(String wkfUri) {
        this.wkfUri = wkfUri;
    }

    public String getHasStatus() {
        return hasStatus;
    }

    public void setHasStatus(String hasStatus) {
        this.hasStatus = hasStatus;
    }

    public WKFGenerator(String elementType, DataFile dataFile, String hasStatus) {
        super(dataFile);
        this.elementType = elementType;
        this.hasStatus = hasStatus;
    }

    @Override
    public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
        Map<String, Object> row = new HashMap<>();

        // Copy all data from the Excel record
        for (String header : file.getHeaders()) {
            if (!header.trim().isEmpty()) {
                String value = rec.getValueByColumnName(header);
                if (value != null && !value.isEmpty()) {
                    row.put(header, normalizeWkfValue(value));
                }
            }
        }

        String elementType = this.getElementType();

        // For Tasks: split concatenated multi-value properties into Lists
        if (elementType.equals("task")) {
            splitMultiValueProperty(row, "vstoi:usesComponentInstance");
            splitMultiValueProperty(row, "vstoi:hasSubtask");
        }

        // Add common metadata to all rows
        if (elementType.equals("processstem")) {
            row.put("hasco:hascoType", VSTOI.PROCESS_STEM);
        } else if (elementType.equals("process")) {
            row.put("hasco:hascoType", VSTOI.PROCESS);

            // Process type is represented by workflow stem reference.
            // If WKF omits it, enforce the workflow-stem entry-level class.
            String stemRefUri = extractStemReferenceUri(row);
            if (stemRefUri.isEmpty()) {
                row.put("prov:wasDerivedFrom", VSTOI.PROCESS_STEM);
            } else {
                row.put("prov:wasDerivedFrom", stemRefUri);
            }

            String computedLabel = buildProcessInstanceLabel(row);
            row.put("rdfs:label", computedLabel);
        } else if (elementType.equals("task")) {
            // WKF v1.2.1 semantics:
            // - hasco:hascoType MUST be vstoi:Task (archetype)
            // - rdf:type MAY be vstoi:Task or any task subclass
            String hascoTypeValue = row.containsKey("hasco:hascoType") && row.get("hasco:hascoType") != null
                    ? row.get("hasco:hascoType").toString().trim()
                    : "";
            String rdfTypeValue = row.containsKey("rdf:type") && row.get("rdf:type") != null
                    ? row.get("rdf:type").toString().trim()
                    : "";

            if (!hascoTypeValue.isEmpty() && !VSTOI.TASK.equals(hascoTypeValue)) {
                // Backward-compatible migration: if previous templates stored subclass in hasco:hascoType,
                // preserve it as rdf:type when rdf:type is missing or generic.
                if (rdfTypeValue.isEmpty() || VSTOI.TASK.equals(rdfTypeValue)) {
                    row.put("rdf:type", hascoTypeValue);
                }

                if (!isValidCTTTaskType(hascoTypeValue)) {
                    System.out.println("[WKFGenerator] WARNING: Task " + row.get("hasURI") + " has non-standard Task rdf:type candidate: " + hascoTypeValue);
                    this.dataFile.getLogger().printWarningByIdWithArgs("WKF_00009", row.get("hasURI").toString(), hascoTypeValue);
                }
            }

            if (rdfTypeValue.isEmpty()) {
                row.put("rdf:type", VSTOI.TASK);
            }

            row.put("hasco:hascoType", VSTOI.TASK);
        } else {
            this.dataFile.getLogger().printExceptionByIdWithArgs("GEN_00001", elementType);
            return null;
        }

        // Add status from the generator context (only if not null or "_")
        if (this.hasStatus != null && !this.hasStatus.equals("_")) {
            row.put("vstoi:hasStatus", this.hasStatus);
        }

        // Add data file reference
        row.put("hasco:hasDataFile", this.dataFile.getUri());
        row.put("vstoi:hasSIRManagerEmail", sanitizeManagerEmail(this.dataFile.getHasSIRManagerEmail()));

        // Only return row if it has a URI
        if (row.containsKey("hasURI") && !row.get("hasURI").toString().trim().isEmpty()) {
            return row;
        }

        System.out.println("[WKFGenerator] WARNING: Row #" + rowNumber + " missing hasURI for elementType=" + elementType + " - skipping");
        return null;
    }

    /**
     * Build process instance label from WKF metadata label (Add WKF Name).
     *
     * Primary rule:
     * - Use the WKF template rdfs:label linked to this DataFile.
     *
     * Fallback:
     * - Keep legacy WorkflowStem/organization labeling if WKF label cannot be resolved.
     */
    private String buildProcessInstanceLabel(Map<String, Object> row) throws Exception {
        String wkfLabel = resolveWkfTemplateLabel();
        if (!wkfLabel.isEmpty()) {
            return wkfLabel;
        }

        String stemLabel = "";
        String orgLabel = "";
        String processUri = row.get("hasURI") == null ? "" : row.get("hasURI").toString().trim();
        String stemRefUri = extractStemReferenceUri(row);

        if (!stemRefUri.isEmpty()) {
            try {
                ProcessStem stem = ProcessStem.find(stemRefUri);
                if (stem != null && stem.getLabel() != null && !stem.getLabel().trim().isEmpty()) {
                    stemLabel = stem.getLabel().trim();
                }
            } catch (Exception e) {
                // handled below via strict validation
            }

            // Generator order computes Process labels before ProcessStem commit.
            // Resolve from current WKF workbook as strict in-file source of truth.
            if (stemLabel.isEmpty()) {
                stemLabel = resolveProcessStemLabelFromWorkbook(stemRefUri);
            }
        }

        if (stemLabel.isEmpty()) {
            if (VSTOI.PROCESS_STEM.equals(stemRefUri)) {
                stemLabel = "Process Stem";
            }
        }

        if (stemLabel.isEmpty()) {
            String msg = "WKF strict labeling error: could not resolve WorkflowStem label (wasDerivedFrom/prov:wasDerivedFrom) for Process " + processUri;
            this.dataFile.getLogger().printException(msg);
            throw new Exception(msg);
        }

        String managerEmail = sanitizeManagerEmail(this.dataFile.getHasSIRManagerEmail());
        if (managerEmail != null && !managerEmail.trim().isEmpty()) {
            try {
                Person person = Person.findByEmail(managerEmail.trim());
                if (person != null) {
                    Organization org = person.getHasAffiliation();
                    if (org != null) {
                        orgLabel = resolveOrganizationShortName(org);
                    }
                }
            } catch (Exception e) {
                // handled below via strict validation
            }

            // Keep strict short-name semantics, but resolve directly from manager email
            // when Person affiliation mapping/predicate shape is incomplete.
            if (orgLabel.isEmpty()) {
                orgLabel = resolveOrganizationShortNameByManagerEmail(managerEmail.trim());
            }
        }

        if (managerEmail == null || managerEmail.trim().isEmpty()) {
            String msg = "WKF labeling fallback: DataFile has no manager email; using fallback organization label for Process " + processUri;
            this.dataFile.getLogger().printWarning(msg);
        }

        if (orgLabel.isEmpty()) {
            String msg = "WKF labeling fallback: could not resolve organization short-name; using fallback organization label for Process " + processUri;
            this.dataFile.getLogger().printWarning(msg);
            orgLabel = "Unknown Organization";
        }

        return stemLabel + " at " + orgLabel;
    }

    /**
     * Resolve WKF metadata template label associated with the current DataFile.
     */
    private String resolveWkfTemplateLabel() {
        if (this.dataFile == null || this.dataFile.getUri() == null || this.dataFile.getUri().trim().isEmpty()) {
            return "";
        }

        String dataFileUri = this.dataFile.getUri().trim();
        String query = NameSpaces.getInstance().printSparqlNameSpaceList()
            + "SELECT DISTINCT ?label WHERE { "
            + "  ?wkf hasco:hasDataFile <" + dataFileUri + "> . "
            + "  { ?wkf a hasco:WKF . } UNION { ?wkf hasco:hascoType hasco:WKF . } "
            + "  ?wkf rdfs:label ?label . "
            + "} LIMIT 1";

        ResultSetRewindable results = SPARQLUtils.select(
            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

        if (results != null && results.hasNext()) {
            QuerySolution sol = results.next();
            if (sol != null && sol.get("label") != null) {
                String label = sol.get("label").toString();
                if (label != null && !label.trim().isEmpty()) {
                    return label.trim();
                }
            }
        }

        return "";
    }

    private String resolveOrganizationShortName(Organization org) {
        if (org == null) {
            return "";
        }

        if (org.getHasShortName() != null && !org.getHasShortName().trim().isEmpty()) {
            return org.getHasShortName().trim();
        }

        String orgUri = org.getUri();
        if (orgUri == null || orgUri.trim().isEmpty()) {
            return "";
        }

        // Strict short-name lookup only (schema:alternateName), but support both
        // https://schema.org and legacy http://schema.org predicates.
        // If absent, use rdfs:label as strict short label source.
        String query = NameSpaces.getInstance().printSparqlNameSpaceList()
            + "SELECT ?short ?rank WHERE { "
            + "  { <" + orgUri + "> schema:alternateName ?short . BIND(1 AS ?rank) } "
            + "  UNION "
            + "  { <" + orgUri + "> <http://schema.org/alternateName> ?short . BIND(2 AS ?rank) } "
            + "  UNION "
            + "  { <" + orgUri + "> rdfs:label ?short . BIND(3 AS ?rank) } "
            + "} ORDER BY ?rank LIMIT 1";

        ResultSetRewindable results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
        if (results != null && results.hasNext()) {
            QuerySolution sol = results.next();
            if (sol != null && sol.get("short") != null) {
                String shortName = sol.get("short").toString();
                if (shortName != null && !shortName.trim().isEmpty()) {
                    return shortName.trim();
                }
            }
        }

        return "";
    }

    private String resolveOrganizationShortNameByManagerEmail(String managerEmail) {
        if (managerEmail == null || managerEmail.trim().isEmpty()) {
            return "";
        }

        String email = managerEmail.trim().replace("\\", "\\\\").replace("\"", "\\\"");
        String query = NameSpaces.getInstance().printSparqlNameSpaceList()
            + "SELECT DISTINCT ?short ?rank WHERE { "
                + "  VALUES ?inputEmail { \"" + email + "\" } "
                + "  { ?person foaf:mbox ?emailRaw . } "
                + "  UNION { ?person hasco:userEmail ?emailRaw . } "
                + "  UNION { ?person vstoi:hasSIRManagerEmail ?emailRaw . } "
                + "  FILTER( LCASE(STR(?emailRaw)) = LCASE(?inputEmail) || LCASE(STR(?emailRaw)) = CONCAT(\"mailto:\", LCASE(?inputEmail)) ) "
                + "  { ?person foaf:member ?org . } "
                + "  UNION { ?org foaf:member ?person . } "
            + "  { ?org schema:alternateName ?short . BIND(1 AS ?rank) } "
            + "  UNION { ?org <http://schema.org/alternateName> ?short . BIND(2 AS ?rank) } "
            + "  UNION { ?org rdfs:label ?short . BIND(3 AS ?rank) } "
            + "} ORDER BY ?rank LIMIT 1";

        ResultSetRewindable results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
        if (results != null && results.hasNext()) {
            QuerySolution sol = results.next();
            if (sol != null && sol.get("short") != null) {
                String shortName = sol.get("short").toString();
                if (shortName != null && !shortName.trim().isEmpty()) {
                    return shortName.trim();
                }
            }
        }

        return "";
    }

    private String extractStemReferenceUri(Map<String, Object> row) {
        String[] keys = new String[] {
            "prov:wasDerivedFrom",
            "wasDerivedFrom",
            "http://www.w3.org/ns/prov#wasDerivedFrom",
            "prov:wasderivedfrom"
        };

        for (String key : keys) {
            Object value = row.get(key);
            if (value == null) {
                continue;
            }

            String ref = normalizeUriValue(value.toString());
            if (!ref.isEmpty()) {
                return ref;
            }
        }

        return "";
    }

    private String normalizeUriValue(String raw) {
        if (raw == null) {
            return "";
        }

        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }

        try {
            value = java.net.URLDecoder.decode(value, "UTF-8").trim();
        } catch (Exception e) {
            // keep original value if decoding fails
        }

        value = URIUtils.stripAngleBrackets(value);
        value = URIUtils.replacePrefixEx(value);
        value = normalizeWkfValue(value);
        if (LEGACY_PMSR_PROCESS_STEM_URI.equals(value)) {
            value = CANONICAL_PMSR_PROCESS_STEM_URI;
        }
        return value == null ? "" : value.trim();
    }

    /**
     * Apply canonical WKF replacements globally to incoming cell values.
     */
    private String normalizeWkfValue(String raw) {
        if (raw == null) {
            return "";
        }

        String value = raw;
        value = value.replace(LEGACY_PMSR_BASE, CANONICAL_PMSR_BASE);
        value = value.replace(LEGACY_PMSR_BASE_HTTPS, CANONICAL_PMSR_BASE);
        value = value.replace("STD_", "STD-");
        value = value.replace("WKF_", "WKF-");
        value = URIUtils.canonicalizePmsrUri(value);
        return value;
    }

    private String sanitizeManagerEmail(String raw) {
        if (raw == null) {
            return "";
        }

        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }

        int lt = value.indexOf('<');
        int gt = value.indexOf('>');
        if (lt >= 0 && gt > lt) {
            value = value.substring(lt + 1, gt).trim();
        }

        String lower = value.toLowerCase();
        if (lower.startsWith("mailto:")) {
            value = value.substring(7).trim();
        }

        int qPos = value.indexOf('?');
        if (qPos >= 0) {
            value = value.substring(0, qPos).trim();
        }

        int ampPos = value.indexOf('&');
        if (ampPos >= 0) {
            value = value.substring(0, ampPos).trim();
        }

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            return "";
        }

        return value;
    }

    private String resolveProcessStemLabelFromWorkbook(String stemRefUri) {
        if (stemRefUri == null || stemRefUri.trim().isEmpty() || this.dataFile == null || this.dataFile.getFile() == null) {
            return "";
        }

        try {
            if (processStemLabelByUri == null) {
                processStemLabelByUri = new HashMap<>();
                RecordFile stemSheet = new SpreadsheetRecordFile(this.dataFile.getFile(), "ProcessStems");
                if (stemSheet == null || !stemSheet.isValid() || stemSheet.getRecords() == null) {
                    return "";
                }

                for (Record record : stemSheet.getRecords()) {
                    String uri = firstNonEmpty(record, "hasURI", "uri", "URI");
                    String label = firstNonEmpty(record, "rdfs:label", "label", "Label");
                    String normalizedUri = normalizeUriValue(uri);
                    if (!normalizedUri.isEmpty() && !label.isEmpty()) {
                        processStemLabelByUri.put(normalizedUri, label.trim());
                    }
                }
            }

            String normalizedStemRefUri = normalizeUriValue(stemRefUri);
            if (normalizedStemRefUri.isEmpty()) {
                return "";
            }

            String label = processStemLabelByUri.get(normalizedStemRefUri);
            return label == null ? "" : label;
        } catch (Exception e) {
            return "";
        }
    }

    private String firstNonEmpty(Record record, String... columnNames) {
        if (record == null || columnNames == null) {
            return "";
        }
        for (String columnName : columnNames) {
            try {
                String value = record.getValueByColumnName(columnName);
                if (value != null && !value.trim().isEmpty()) {
                    return value;
                }
            } catch (Exception e) {
                // ignore and continue
            }
        }
        return "";
    }

    /**
     * Split concatenated values (separated by ; or |) into a List for multi-value properties.
     * This ensures MetadataFactory creates multiple triples instead of one literal triple.
     */
    private void splitMultiValueProperty(Map<String, Object> row, String propertyKey) {
        Object value = row.get(propertyKey);
        if (value == null) {
            return;
        }

        String valueStr = value.toString().trim();
        if (valueStr.isEmpty()) {
            return;
        }

        // Check if value contains separators (semicolon or pipe)
        if (valueStr.contains(";") || valueStr.contains("|")) {
            java.util.List<String> uris = new java.util.ArrayList<>();
            
            // Split by semicolon or pipe, handling both separators
            String[] parts;
            if (valueStr.contains(";")) {
                parts = valueStr.split("\\s*;\\s*");
            } else {
                parts = valueStr.split("\\s*\\|\\s*");
            }

            for (String part : parts) {
                String cleanUri = part.trim();
                // Decode URL encoding (e.g., %20 -> space, then trim again)
                try {
                    cleanUri = java.net.URLDecoder.decode(cleanUri, "UTF-8").trim();
                } catch (Exception e) {
                    // If decoding fails, use the original value
                }

                cleanUri = normalizeWkfValue(cleanUri);
                
                if (!cleanUri.isEmpty()) {
                    uris.add(cleanUri);
                }
            }

            if (!uris.isEmpty()) {
                row.put(propertyKey, uris);
                System.out.println("[WKFGenerator] Split " + propertyKey + " into " + uris.size() + " values: " + uris);
            }
        }
    }

    /**
     * Validate if the provided task type is a valid CTT (ConcurTaskTree) task type.
     * According to WKF-SPEC-V1, valid CTT task types are:
     * - vstoi:UserTask - Cognitive/motor tasks performed by user
     * - vstoi:ApplicationTask / vstoi:SystemTask - Automated system tasks
     * - vstoi:InteractiveTask - User-system collaboration tasks
     * - vstoi:AbstractTask - High-level decomposable tasks
     * - Domain-specific extensions (vstoi:*, pmsr:*, etc.)
     */
    private boolean isValidCTTTaskType(String taskType) {
        if (taskType == null || taskType.trim().isEmpty()) {
            return false;
        }
        
        // Check against standard CTT task types
        if (taskType.equals(VSTOI.USER_TASK) ||
            taskType.equals(VSTOI.APPLICATION_TASK) ||
            taskType.equals(VSTOI.INTERACTIVE_TASK) ||
            taskType.equals(VSTOI.ABSTRACT_TASK) ||
            taskType.equals(VSTOI.TASK)) {  // Generic task is also valid
            return true;
        }
        
        // Check if it's a domain-specific extension (vstoi:*, pmsr:*, etc.)
        // These are valid as long as they follow the namespace pattern
        if (taskType.startsWith("vstoi:") || taskType.startsWith("pmsr:") || 
            taskType.startsWith("hasco:") || taskType.contains("Task")) {
            return true;
        }
        
        // Check if it's a full URI (starts with http://)
        if (taskType.startsWith("http://") && taskType.contains("Task")) {
            return true;
        }
        
        return false;
    }

    @Override
    public String getTableName() {
        return "WKF-" + this.getElementType();
    }

    @Override
    public String getErrorMsg(Exception e) {
        return "Error in WKFGenerator for element type [" + this.getElementType() + "]: " + e.getMessage();
    }

    @Override
    public void preprocessuris(Map<String, String> uris) {
        // Process URIs if needed - currently no preprocessing required for WKF
    }
}

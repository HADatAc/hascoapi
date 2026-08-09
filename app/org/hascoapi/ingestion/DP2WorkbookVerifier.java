package org.hascoapi.ingestion;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Java implementation of DP2 workbook verification rules used to gate DP2 ingestion.
 *
 * This verifier ports the core behavior of scripts/dp2_verify.py and runs entirely
 * within hascoapi before generator-chain execution.
 */
public class DP2WorkbookVerifier {

    private static final List<String> REQUIRED_SHEETS = Arrays.asList(
            "Deployments",
            "ComponentDeployments",
            "Platforms",
            "PlatformInstances",
            "FieldsOfView",
            "InstrumentInstances",
            "ComponentInstances",
            "SensingPerspective"
    );

    private static final List<String> EXPECTED_INFOSHEET_KEYS = Arrays.asList(
            "hasDependencies",
            "Deployments",
            "ComponentDeployments",
            "Platforms",
            "PlatformInstances",
            "FieldsOfView",
            "InstrumentInstances",
            "ComponentInstances",
            "SensingPerspective"
    );

    private static final Pattern URI_SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:.*$");
    private static final Pattern INS_MODEL_PATTERN = Pattern.compile("(^|[:/#])INS[-_/].*", Pattern.CASE_INSENSITIVE);

    private final DataFile dataFile;
    private final Map<String, String> mapCatalog;
    private final String sparqlService;

    public DP2WorkbookVerifier(DataFile dataFile, Map<String, String> mapCatalog) {
        this.dataFile = dataFile;
        this.mapCatalog = mapCatalog == null ? new HashMap<>() : mapCatalog;
        this.sparqlService = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);
    }

    public boolean verify() {
        boolean ok = true;

        // 1) Structural checks
        if (!validateInfoSheetKeys()) {
            ok = false;
        }
        if (!validateRequiredSheetsExist()) {
            ok = false;
        }

        Map<String, RecordFile> sheets = loadRequiredSheets();
        if (sheets.size() != REQUIRED_SHEETS.size()) {
            // Missing required sheets already logged by validateRequiredSheetsExist.
            return false;
        }

        if (!validateRequiredHeaders(sheets)) {
            ok = false;
        }

        if (!validateFixedHascoTypeValues(sheets)) {
            ok = false;
        }

        // 2) Build URI sets
        Set<String> deploymentUris = uriSet(sheets.get("Deployments"));
        Set<String> platformInstanceUris = uriSet(sheets.get("PlatformInstances"));
        Set<String> instrumentInstanceUris = uriSet(sheets.get("InstrumentInstances"));
        Set<String> componentInstanceUris = uriSet(sheets.get("ComponentInstances"));

        // 2.1) InstrumentInstances model policy checks.
        if (!validateInstrumentInstanceModels(sheets.get("InstrumentInstances"))) {
            ok = false;
        }

        // 3) hasURI checks on core sheets
        if (!validateHasUriPresence(sheets)) {
            ok = false;
        }

        // 4) Deployments references and malformed component-link values
        if (!validateDeployments(sheets.get("Deployments"), platformInstanceUris, instrumentInstanceUris, componentInstanceUris)) {
            ok = false;
        }

        // 5) ComponentDeployments semantic checks
        if (!validateComponentDeployments(sheets.get("ComponentDeployments"), deploymentUris, componentInstanceUris)) {
            ok = false;
        }

        return ok;
    }

    private boolean validateInstrumentInstanceModels(RecordFile instrumentInstances) {
        if (instrumentInstances == null) {
            return false;
        }

        boolean ok = true;
        int rowNum = 1;
        for (Record rec : instrumentInstances.getRecords()) {
            rowNum++;
            String iiUri = normUri(getValue(rec, "hasURI", "uri"));
            if (isBlank(iiUri)) {
                continue;
            }

            String modelUri = normUri(getValue(rec, "a"));
            if (isBlank(modelUri)) {
                error("II_MODEL_MISSING", "InstrumentInstances row " + rowNum + " is missing model URI in column 'a'");
                ok = false;
                continue;
            }

            if (!looksLikeUri(modelUri)) {
                error("II_MODEL_INVALID_URI", "InstrumentInstances row " + rowNum + " has non-URI model value in column 'a': " + modelUri);
                ok = false;
                continue;
            }

            if (!hasInsLikeUri(modelUri)) {
                error("II_MODEL_POLICY_VIOLATION", "InstrumentInstances row " + rowNum + " model URI is not INS-like: " + modelUri);
                ok = false;
            }

            if (!uriExists(modelUri)) {
                error("II_MODEL_NOT_FOUND", "InstrumentInstances row " + rowNum + " model URI does not resolve in repository: " + modelUri);
                ok = false;
            }
        }

        return ok;
    }

    private boolean validateInfoSheetKeys() {
        boolean ok = true;
        for (String key : EXPECTED_INFOSHEET_KEYS) {
            if (!mapCatalog.containsKey(key)) {
                error("INFOSHEET_KEY_MISSING", "InfoSheet missing expected key: " + key);
                ok = false;
            }
        }
        return ok;
    }

    private boolean validateRequiredSheetsExist() {
        boolean ok = true;
        for (String key : REQUIRED_SHEETS) {
            String sheetName = resolveSheetName(key);
            RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), sheetName);
            if (!sheet.isValid()) {
                error("SHEET_MISSING", "Required sheet missing or invalid: " + key + " (mapped to '" + sheetName + "')");
                ok = false;
            }
        }
        return ok;
    }

    private Map<String, RecordFile> loadRequiredSheets() {
        Map<String, RecordFile> sheets = new HashMap<>();
        for (String key : REQUIRED_SHEETS) {
            String sheetName = resolveSheetName(key);
            RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), sheetName);
            if (sheet.isValid()) {
                sheets.put(key, sheet);
            }
        }
        return sheets;
    }

    private boolean validateRequiredHeaders(Map<String, RecordFile> sheets) {
        Map<String, List<String>> requiredHeaders = new LinkedHashMap<>();
        requiredHeaders.put("Deployments", Arrays.asList(
                "hasURI",
                "a",
                "rdfs:label",
                "vstoi:hasPlatformInstance",
                "vstoi:hasInstrumentInstance",
                "vstoi:designedAtTime",
            "prov:startedAtTime",
            "prov:endedAtTime"
        ));
        requiredHeaders.put("PlatformInstances", Arrays.asList(
            "hasURI", "a", "hasco:hascoType", "rdfs:label", "vstoi:hasSerialNumber",
            "hasco:hasFirstCoordinate", "hasco:hasFirstCoordinateUnit", "hasco:hasFirstCoordinateCharacteristic",
            "hasco:hasSecondCoordinate", "hasco:hasSecondCoordinateUnit", "hasco:hasSecondCoordinateCharacteristic",
            "hasco:hasThirdCoordinate", "hasco:hasThirdCoordinateUnit", "hasco:hasThirdCoordinateCharacteristic",
            "hasco:partOf"
        ));
        requiredHeaders.put("InstrumentInstances", Arrays.asList(
            "hasURI", "a", "hasco:hascoType", "rdfs:label", "vstoi:hasSerialNumber", "skos:definition", "owl:sameAs", "vstoi:hasOwner"
        ));
        requiredHeaders.put("ComponentInstances", Arrays.asList(
            "hasURI", "a", "hasco:hascoType", "rdfs:label", "vstoi:hasSerialNumber", "vstoi:isInstrumentAttachment"
        ));
        requiredHeaders.put("ComponentDeployments", Arrays.asList(
            "hasURI", "rdf:type", "hasco:hascoDeployment", "hasco:hasInstrumentSlot", "hasco:hasComponentInstance"
        ));

        boolean ok = true;
        for (Map.Entry<String, List<String>> e : requiredHeaders.entrySet()) {
            RecordFile sheet = sheets.get(e.getKey());
            if (sheet == null) {
                continue;
            }
            Set<String> normalizedHeaders = normalizedHeaderSet(sheet.getHeaders());
            for (String required : e.getValue()) {
                if (!normalizedHeaders.contains(normalizeHeader(required))) {
                    error("HEADER_MISSING", "Sheet " + e.getKey() + " is missing required header: " + required);
                    ok = false;
                }
            }
        }
        return ok;
    }

    private boolean validateFixedHascoTypeValues(Map<String, RecordFile> sheets) {
        boolean ok = true;
        if (!validateHascoTypeForSheet(sheets.get("PlatformInstances"), "PlatformInstances", "hasco:PlatformInstance")) {
            ok = false;
        }
        if (!validateHascoTypeForSheet(sheets.get("InstrumentInstances"), "InstrumentInstances", "hasco:InstrumentInstance")) {
            ok = false;
        }
        if (!validateHascoTypeForSheet(sheets.get("ComponentInstances"), "ComponentInstances", "hasco:ComponentInstance")) {
            ok = false;
        }
        return ok;
    }

    private boolean validateHascoTypeForSheet(RecordFile sheet, String sheetName, String expectedType) {
        if (sheet == null) {
            return false;
        }

        boolean ok = true;
        int rowNum = 1;
        for (Record rec : sheet.getRecords()) {
            rowNum++;
            if (!hasAnyValue(rec, sheet.getHeaders())) {
                continue;
            }

            String hascoType = normUri(getValue(rec, "hasco:hascoType"));
            if (isBlank(hascoType)) {
                warn("HASCO_TYPE_MISSING", "Sheet " + sheetName + " row " + rowNum + " is populated but hasco:hascoType is empty");
                continue;
            }

            if (!normUri(expectedType).equals(hascoType)) {
                error("HASCO_TYPE_INVALID",
                        "Sheet " + sheetName + " row " + rowNum + " has invalid hasco:hascoType=" + hascoType
                                + " (expected " + expectedType + ")");
                ok = false;
            }
        }

        return ok;
    }

    private boolean validateHasUriPresence(Map<String, RecordFile> sheets) {
        boolean ok = true;
        List<String> hasUriSheets = Arrays.asList(
                "Deployments",
            "ComponentDeployments",
                "Platforms",
                "PlatformInstances",
                "FieldsOfView",
                "InstrumentInstances",
                "ComponentInstances",
                "SensingPerspective"
        );

        for (String sheetName : hasUriSheets) {
            RecordFile sheet = sheets.get(sheetName);
            if (sheet == null) {
                continue;
            }
            int rowNum = 1;
            for (Record rec : sheet.getRecords()) {
                rowNum++;
                if (!hasAnyValue(rec, sheet.getHeaders())) {
                    continue;
                }
                String hasUri = normUri(getValue(rec, "hasURI", "uri"));
                if (isBlank(hasUri)) {
                    error("ROW_HASURI_MISSING", "Sheet " + sheetName + " row " + rowNum + " is populated but hasURI is empty");
                    ok = false;
                }
            }
        }
        return ok;
    }

    private boolean validateDeployments(RecordFile deployments,
                                        Set<String> platformInstanceUris,
                                        Set<String> instrumentInstanceUris,
                                        Set<String> componentInstanceUris) {
        if (deployments == null) {
            return false;
        }

        boolean ok = true;

        int rowNum = 1;
        for (Record rec : deployments.getRecords()) {
            rowNum++;
            String dUri = normUri(getValue(rec, "hasURI", "uri"));
            if (isBlank(dUri)) {
                continue;
            }

            String platform = normUri(getValue(rec, "vstoi:hasPlatformInstance"));
            if (isBlank(platform) || !platformInstanceUris.contains(platform)) {
                error("DEPLOY_PLATFORM_INVALID", "Deployments row " + rowNum + " has invalid platform instance reference: " + platform);
                ok = false;
            }

            String instrument = normUri(getValue(rec, "vstoi:hasInstrumentInstance"));
            if (isBlank(instrument) || !instrumentInstanceUris.contains(instrument)) {
                error("DEPLOY_INSTRUMENT_INVALID", "Deployments row " + rowNum + " has invalid instrument instance reference: " + instrument);
                ok = false;
            }
        }

        return ok;
    }

    private boolean validateComponentDeployments(RecordFile componentDeployments,
                                                 Set<String> deploymentUris,
                                                 Set<String> componentInstanceUris) {
        if (componentDeployments == null) {
            return false;
        }

        boolean ok = true;
        int rowNum = 1;
        for (Record rec : componentDeployments.getRecords()) {
            rowNum++;

            String cdUri = normUri(getValue(rec, "hasURI", "uri", "ComponentDeployment URI", "componentDeploymentUri"));
            String cdType = normUri(getValue(rec, "rdf:type", "a"));

            String depUri = normUri(getValue(rec,
                    "hasco:hascoDeployment", "Deployment URI", "deployment URI", "deploymentUri", "DeploymentUri",
                    "hasco:hasDeployment", "hasDeployment"));

            String slotUri = normUri(getValue(rec,
                    "hasco:hasInstrumentSlot", "Instrument Slot URI", "Instrument slot URI", "instrumentSlotUri", "InstrumentSlotUri",
                    "vstoi:hasContainerSlot", "hasContainerSlot", "Slot URI", "slot URI"));

            String compUri = normUri(getValue(rec,
                    "hasco:hasComponentInstance", "Component Instance URI", "Component instance URI", "componentInstanceUri", "ComponentInstanceUri",
                    "vstoi:hasComponentInstance", "hasComponentInstance"));

            if (isBlank(cdUri) && isBlank(depUri) && isBlank(slotUri) && isBlank(compUri)) {
                continue;
            }

            if (isBlank(cdUri)) {
                error("COMP_DEPLOYMENT_URI_MISSING", "ComponentDeployments row " + rowNum + " is missing hasURI");
                ok = false;
                continue;
            }

            if (isBlank(cdType) || !normUri("vstoi:ComponentDeployment").equals(cdType)) {
                error("COMP_DEPLOYMENT_TYPE_INVALID", "ComponentDeployments row " + rowNum + " must have rdf:type = vstoi:ComponentDeployment");
                ok = false;
            }

            if (isBlank(depUri) || isBlank(slotUri) || isBlank(compUri)) {
                error("COMP_DEPLOYMENT_REQUIRED_MISSING", "ComponentDeployments row " + rowNum + " is missing one or more required values");
                ok = false;
                continue;
            }

            if (!deploymentUris.contains(depUri)) {
                error("COMP_DEPLOYMENT_DEPLOYMENT_INVALID", "ComponentDeployments row " + rowNum + " references unknown deployment URI: " + depUri);
                ok = false;
            }

            if (!componentInstanceUris.contains(compUri)) {
                error("COMP_DEPLOYMENT_COMPONENT_INVALID", "ComponentDeployments row " + rowNum + " references unknown component instance URI: " + compUri);
                ok = false;
            }

            if (!uriExists(slotUri)) {
                error("COMP_DEPLOYMENT_SLOT_INVALID", "ComponentDeployments row " + rowNum + " references unknown slot URI: " + slotUri);
                ok = false;
            }

            if (!slotBelongsToDeploymentInstrument(depUri, slotUri)) {
                error("COMP_DEPLOYMENT_SLOT_INSTRUMENT_MISMATCH", "ComponentDeployments row " + rowNum + " slot owner does not match deployment instrument context");
                ok = false;
            }

            // Step 2 enforcement: component model must be compatible with slot model.
            // Slot model: <slot> vstoi:hasComponent ?slotComponentModel
            // Instance model: <componentInstance> vstoi:hasComponent ?componentModel
            CompatibilityCheckResult modelCompatibility = checkSlotComponentModelCompatibility(slotUri, compUri);
            if (modelCompatibility.status == CompatibilityStatus.MISMATCH) {
                error("COMP_DEPLOYMENT_COMPONENT_MODEL_MISMATCH",
                        "ComponentDeployments row " + rowNum + " component model does not match slot model. slotModel="
                                + modelCompatibility.slotModel + ", componentModel=" + modelCompatibility.componentModel);
                ok = false;
            } else if (modelCompatibility.status == CompatibilityStatus.UNKNOWN) {
                error("COMP_DEPLOYMENT_COMPONENT_MODEL_UNKNOWN",
                        "ComponentDeployments row " + rowNum + " model compatibility could not be fully verified (missing slot/component model link)");
                ok = false;
            }
        }

        return ok;
    }

    private CompatibilityCheckResult checkSlotComponentModelCompatibility(String slotUri, String componentInstanceUri) {
        String query = NameSpaces.getInstance().printSparqlNameSpaceList()
                + "SELECT ?slotComponentModel ?componentModel WHERE { "
                + "  OPTIONAL { <" + slotUri + "> vstoi:hasComponent ?slotComponentModel . } "
                + "  OPTIONAL { <" + componentInstanceUri + "> vstoi:hasComponent ?componentModel . } "
                + "} LIMIT 1";

        try {
            ResultSetRewindable rs = SPARQLUtils.select(sparqlService, query);
            if (rs == null || !rs.hasNext()) {
                return CompatibilityCheckResult.unknown();
            }

            QuerySolution qs = rs.next();
            String slotModel = qs.contains("slotComponentModel") ? qs.get("slotComponentModel").toString() : "";
            String componentModel = qs.contains("componentModel") ? qs.get("componentModel").toString() : "";

            if (isBlank(slotModel) || isBlank(componentModel)) {
                return CompatibilityCheckResult.unknown(slotModel, componentModel);
            }

            if (slotModel.equals(componentModel)) {
                return CompatibilityCheckResult.ok(slotModel, componentModel);
            }

            return CompatibilityCheckResult.mismatch(slotModel, componentModel);
        } catch (Exception e) {
            return CompatibilityCheckResult.unknown();
        }
    }

    private boolean uriExists(String uri) {
        if (isBlank(uri)) {
            return false;
        }

        String query = "SELECT ?s WHERE { <" + uri + "> ?p ?o . BIND(<" + uri + "> AS ?s) } LIMIT 1";
        try {
            ResultSetRewindable results = SPARQLUtils.select(sparqlService, query);
            return results != null && results.hasNext();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean slotBelongsToDeploymentInstrument(String deploymentUri, String slotUri) {
        String query = NameSpaces.getInstance().printSparqlNameSpaceList()
                + "SELECT ?depInstrument ?slotInstrument WHERE { "
                + "  OPTIONAL { <" + deploymentUri + "> vstoi:hasInstrumentInstance ?depInstrument . } "
                + "  OPTIONAL { <" + slotUri + "> vstoi:belongsTo ?slotInstrument . } "
                + "}";

        try {
            ResultSetRewindable results = SPARQLUtils.select(sparqlService, query);
            if (results == null || !results.hasNext()) {
                return false;
            }
            QuerySolution qs = results.next();
            if (!qs.contains("depInstrument") || !qs.contains("slotInstrument")) {
                return false;
            }
            String depInstrument = qs.get("depInstrument").toString();
            String slotInstrument = qs.get("slotInstrument").toString();
            return depInstrument.equals(slotInstrument);
        } catch (Exception e) {
            return false;
        }
    }

    private String resolveSheetName(String key) {
        String raw = mapCatalog.get(key);
        if (raw == null || raw.trim().isEmpty()) {
            return key;
        }
        return raw.replace("#", "").trim();
    }

    private Set<String> uriSet(RecordFile sheet) {
        Set<String> out = new HashSet<>();
        if (sheet == null || sheet.getRecords() == null) {
            return out;
        }
        for (Record rec : sheet.getRecords()) {
            String uri = normUri(getValue(rec, "hasURI", "uri"));
            if (!isBlank(uri)) {
                out.add(uri);
            }
        }
        return out;
    }

    private String getValue(Record rec, String... keys) {
        for (String key : keys) {
            String value = rec.getValueByColumnName(key);
            if (!isBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private boolean hasAnyValue(Record rec, List<String> headers) {
        for (String h : headers) {
            String v = safe(rec.getValueByColumnName(h));
            if (!v.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Set<String> normalizedHeaderSet(List<String> headers) {
        Set<String> out = new HashSet<>();
        if (headers == null) {
            return out;
        }
        for (String h : headers) {
            out.add(normalizeHeader(h));
        }
        return out;
    }

    private String normalizeHeader(String header) {
        return safe(header)
                .replace("\u00A0", " ")
                .replace("\u2007", " ")
                .replace("\u202F", " ")
                .replace("\u200B", "")
                .replace("\uFEFF", "")
                .trim()
                .toLowerCase();
    }

    private String normUri(String value) {
        String v = safe(value).trim();
        if (v.isEmpty()) {
            return "";
        }

        if (v.startsWith("<") && v.endsWith(">") && v.length() > 2) {
            v = v.substring(1, v.length() - 1).trim();
        }

        try {
            return URIUtils.replaceNameSpaceEx(v);
        } catch (Exception e) {
            return v;
        }
    }

    private boolean looksLikeUri(String value) {
        String v = normUri(value);
        return !v.isEmpty() && URI_SCHEME.matcher(v).matches();
    }

    private boolean hasInsLikeUri(String value) {
        String v = normUri(value);
        if (v.isEmpty()) {
            return false;
        }
        return INS_MODEL_PATTERN.matcher(v).matches();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void warn(String code, String message) {
        dataFile.getLogger().printWarning("[DP2-VERIFY] " + code + ": " + message);
    }

    private void error(String code, String message) {
        dataFile.getLogger().printException("[DP2-VERIFY] " + code + ": " + message);
    }

    private enum CompatibilityStatus {
        OK,
        MISMATCH,
        UNKNOWN
    }

    private static class CompatibilityCheckResult {
        final CompatibilityStatus status;
        final String slotModel;
        final String componentModel;

        private CompatibilityCheckResult(CompatibilityStatus status, String slotModel, String componentModel) {
            this.status = status;
            this.slotModel = slotModel;
            this.componentModel = componentModel;
        }

        static CompatibilityCheckResult ok(String slotModel, String componentModel) {
            return new CompatibilityCheckResult(CompatibilityStatus.OK, slotModel, componentModel);
        }

        static CompatibilityCheckResult mismatch(String slotModel, String componentModel) {
            return new CompatibilityCheckResult(CompatibilityStatus.MISMATCH, slotModel, componentModel);
        }

        static CompatibilityCheckResult unknown() {
            return new CompatibilityCheckResult(CompatibilityStatus.UNKNOWN, "", "");
        }

        static CompatibilityCheckResult unknown(String slotModel, String componentModel) {
            return new CompatibilityCheckResult(CompatibilityStatus.UNKNOWN, slotModel, componentModel);
        }
    }
}
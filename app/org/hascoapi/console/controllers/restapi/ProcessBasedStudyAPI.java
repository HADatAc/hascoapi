package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.GenericFind;
import org.hascoapi.entity.pojo.Process;
import org.hascoapi.entity.pojo.ProcessBasedStudy;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProcessBasedStudyAPI - REST API controller for ProcessBasedStudy entities
 * 
 * Provides CRUD operations and specialized queries for ProcessBasedStudy.
 * ProcessBasedStudy is a specialization of Study that requires a Process/Workflow.
 * 
 * API Endpoints (defined in conf/routes):
 * - GET /api/processbasedstudy/:uri - Get single ProcessBasedStudy by URI
 * - GET /api/processbasedstudy/byprocess/:processuri - Get study by Process URI
 * - POST /api/processbasedstudy/create - Create new ProcessBasedStudy
 * - POST /api/processbasedstudy/update/:uri - Update existing ProcessBasedStudy
 * - POST /api/processbasedstudy/delete/:uri - Delete ProcessBasedStudy
 * - GET /api/processbasedstudy/elements/:pageSize/:offset - Get paginated list
 * - GET /api/processbasedstudy/elements/total - Get total count
 * 
 * @see ProcessBasedStudy
 * @see Study
 * @see Process
 */
public class ProcessBasedStudyAPI extends Controller {

    /**
     * Format list of ProcessBasedStudy entities as JSON response
     */
    public static Result getProcessBasedStudies(List<ProcessBasedStudy> results) {
        if (results == null || results.isEmpty()) {
            return ok(ApiUtil.createResponse("No ProcessBasedStudy has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL, HASCO.PROCESS_BASED_STUDY);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    /**
     * Format single ProcessBasedStudy entity as JSON response
     */
    public static Result getProcessBasedStudy(ProcessBasedStudy result) {
        if (result == null) {
            return ok(ApiUtil.createResponse("ProcessBasedStudy not found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL, HASCO.PROCESS_BASED_STUDY);
            JsonNode jsonObject = mapper.convertValue(result, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    /**
     * GET /api/processbasedstudy/:uri
     * Retrieve a single ProcessBasedStudy by URI
     * 
     * @param uri The URI of the ProcessBasedStudy (can be prefixed or full URI)
     * @return JSON response with ProcessBasedStudy entity
     */
    public Result getProcessBasedStudyByUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }

        try {
            // Expand URI if it's a prefixed form (e.g., pmsr:STD-X)
            String expandedUri = URIUtils.replacePrefixEx(uri);
            
            ProcessBasedStudy study = ProcessBasedStudy.find(expandedUri);
            return getProcessBasedStudy(study);
            
        } catch (Exception e) {
            return ok(ApiUtil.createResponse("Error retrieving ProcessBasedStudy: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/processbasedstudy/byprocess/:processuri
     * Retrieve a ProcessBasedStudy by its associated Process URI
     * 
     * @param processuri The URI of the Process/Workflow
     * @return JSON response with ProcessBasedStudy entity
     */
    public Result getProcessBasedStudyByProcess(String processuri) {
        if (processuri == null || processuri.isEmpty()) {
            return ok(ApiUtil.createResponse("No Process URI has been provided", false));
        }

        try {
            // Expand URI if it's a prefixed form
            String expandedUri = URIUtils.replacePrefixEx(processuri);
            
            // Query for ProcessBasedStudy where hasco:hasProcess = processuri
            ProcessBasedStudy study = ProcessBasedStudy.findByProcess(expandedUri);
            return getProcessBasedStudy(study);
            
        } catch (Exception e) {
            return ok(ApiUtil.createResponse("Error retrieving ProcessBasedStudy by Process: " + e.getMessage(), false));
        }
    }

    /**
     * POST /api/processbasedstudy/create
     * Create a new ProcessBasedStudy from JSON payload
     * 
     * Request Body (JSON):
     * {
     *   "processUri": "pmsr:WKF-X/PROC/0001",
     *   "studyID": "STD-X",  // Optional - will be derived if missing
     *   "title": "Study Title",
     *   "specificAims": "...",
     *   "significance": "...",
     *   "institution": "...",
     *   "principalInvestigator": "...",
     *   "contactEmail": "...",
     *   "startDate": "2026-07-17",
     *   "endDate": "2026-12-31"
     * }
     * 
     * @param json JSON string with ProcessBasedStudy data
     * @return JSON response with created study URI
     */
    public Result createProcessBasedStudy(String json) {
        if (json == null || json.isEmpty()) {
            return ok(ApiUtil.createResponse("No JSON payload has been provided", false));
        }

        try {
            // Standard pattern: Deserialize JSON with ObjectMapper
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(json);
            ProcessBasedStudy study = mapper.readValue(json, ProcessBasedStudy.class);

            if (jsonNode.has("institution") && (study.getInstitutionName() == null || study.getInstitutionName().isEmpty())) {
                study.setInstitutionName(jsonNode.get("institution").asText());
            }
            if (jsonNode.has("hasLearningObjectives")) {
                study.setHasLearningObjectives(jsonNode.get("hasLearningObjectives").asText());
            }
            if (jsonNode.has("hasCriticalActions")) {
                study.setHasCriticalActions(jsonNode.get("hasCriticalActions").asText());
            }
            if (jsonNode.has("hasDebriefingFocus")) {
                study.setHasDebriefingFocus(jsonNode.get("hasDebriefingFocus").asText());
            }
            
            // Expand URI prefixes (if needed)
            if (study.getUri() != null && !study.getUri().isEmpty()) {
                study.setUri(URIUtils.replacePrefixEx(study.getUri()));
            }
            if (study.getProcessUri() != null && !study.getProcessUri().isEmpty()) {
                study.setProcessUri(URIUtils.replacePrefixEx(study.getProcessUri()));
            }
            
            // Set RDF type and HASCO type (required for proper RDF generation)
            study.setTypeUri(HASCO.PROCESS_BASED_STUDY);  // Specific subclass for rdf:type
            study.setHascoTypeUri(HASCO.STUDY);           // Fundamental concept for hasco:hascoType
            
            // Validate before saving
            if (!study.validate()) {
                return ok(ApiUtil.createResponse("ProcessBasedStudy validation failed: " + study.getErrorMessage(), false));
            }

            // Save to triplestore
            study.save();
            Map<String, String> responseData = new HashMap<>();
            responseData.put("uri", study.getUri());
            responseData.put("studyID", study.getStudyID());
            responseData.put("processUri", study.getProcessUri());
            
            JsonNode responseJson = mapper.convertValue(responseData, JsonNode.class);
            return ok(ApiUtil.createResponse(responseJson, true));

        } catch (Exception e) {
            e.printStackTrace();
            return ok(ApiUtil.createResponse("Error creating ProcessBasedStudy: " + e.getMessage(), false));
        }
    }

    /**
     * POST /api/processbasedstudy/update/:uri
     * Update an existing ProcessBasedStudy
     * 
     * @param uri The URI of the ProcessBasedStudy to update
     * @param json JSON string with updated fields
     * @return JSON response confirming update
     */
    public Result updateProcessBasedStudy(String uri, String json) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        if (json == null || json.isEmpty()) {
            return ok(ApiUtil.createResponse("No JSON payload has been provided", false));
        }

        try {
            // Find existing study
            String expandedUri = URIUtils.replacePrefixEx(uri);
            ProcessBasedStudy study = ProcessBasedStudy.find(expandedUri);
            
            if (study == null) {
                return ok(ApiUtil.createResponse("ProcessBasedStudy not found: " + uri, false));
            }

            // Parse JSON and update fields
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(json);

            // Update mutable fields (processUri is immutable)
            if (jsonNode.has("studyID")) {
                study.setStudyID(jsonNode.get("studyID").asText());
            }
            if (jsonNode.has("studyTitle")) {
                study.setStudyTitle(jsonNode.get("studyTitle").asText());
            }
            if (jsonNode.has("specificAims")) {
                study.setSpecificAims(jsonNode.get("specificAims").asText());
            }
            if (jsonNode.has("significance")) {
                study.setSignificance(jsonNode.get("significance").asText());
            }
            if (jsonNode.has("institutionName")) {
                study.setInstitutionName(jsonNode.get("institutionName").asText());
            } else if (jsonNode.has("institution")) {
                study.setInstitutionName(jsonNode.get("institution").asText());
            }
            if (jsonNode.has("principalInvestigator")) {
                study.setPrincipalInvestigator(jsonNode.get("principalInvestigator").asText());
            }
            if (jsonNode.has("contactEmail")) {
                study.setContactEmail(jsonNode.get("contactEmail").asText());
            }
            if (jsonNode.has("startDate")) {
                study.setStartDate(jsonNode.get("startDate").asText());
            }
            if (jsonNode.has("endDate")) {
                study.setEndDate(jsonNode.get("endDate").asText());
            }
            if (jsonNode.has("hasLearningObjectives")) {
                study.setHasLearningObjectives(jsonNode.get("hasLearningObjectives").asText());
            }
            if (jsonNode.has("hasCriticalActions")) {
                study.setHasCriticalActions(jsonNode.get("hasCriticalActions").asText());
            }
            if (jsonNode.has("hasDebriefingFocus")) {
                study.setHasDebriefingFocus(jsonNode.get("hasDebriefingFocus").asText());
            }

            // Save changes
            study.save();
            return ok(ApiUtil.createResponse("ProcessBasedStudy updated successfully", true));

        } catch (Exception e) {
            e.printStackTrace();
            return ok(ApiUtil.createResponse("Error updating ProcessBasedStudy: " + e.getMessage(), false));
        }
    }

    /**
     * POST /api/processbasedstudy/delete/:uri
        * Delete a ProcessBasedStudy with conditional process/task cascade.
        *
        * Rules:
        * - If associated process is shared by other ProcessBasedStudy entities,
        *   delete only the current scenario and its direct DataFiles.
        * - If not shared, also delete process + tasks and process-linked DataFiles.
     * 
     * @param uri The URI of the ProcessBasedStudy to delete
     * @return JSON response confirming deletion
     */
    public Result deleteProcessBasedStudy(String uri) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }

        try {
            String expandedUri = URIUtils.replacePrefixEx(uri);
            ProcessBasedStudy study = ProcessBasedStudy.find(expandedUri);
            
            if (study == null) {
                return ok(ApiUtil.createResponse("ProcessBasedStudy not found: " + uri, false));
            }

            String processUri = study.getProcessUri();
            int references = ProcessBasedStudy.countByProcess(processUri);
            boolean processShared = references > 1;

            if (processShared) {
                study.deleteScenarioOnly();
                return ok(ApiUtil.createResponse("ProcessBasedStudy deleted successfully (shared process was preserved)", true));
            }

            study.deleteScenarioWithProcessHierarchy();
            return ok(ApiUtil.createResponse("ProcessBasedStudy deleted successfully (process, tasks, and related DataFiles also deleted)", true));

        } catch (Exception e) {
            e.printStackTrace();
            return ok(ApiUtil.createResponse("Error deleting ProcessBasedStudy: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/processbasedstudy/elements/:pageSize/:offset
     * Get paginated list of all ProcessBasedStudy entities
     * 
     * @param pageSize Number of results per page
     * @param offset Starting offset for pagination
     * @return JSON response with list of ProcessBasedStudy entities
     */
    public Result getProcessBasedStudiesWithPage(int pageSize, int offset) {
        try {
            // Prefer the keyword-backed path because it has proven resilient across
            // mixed repository data shapes and still honors paging.
            List<ProcessBasedStudy> results = ProcessBasedStudy.findByKeyword("_", pageSize, offset);
            return getProcessBasedStudies(results);
        } catch (Exception e) {
            return ok(ApiUtil.createResponse("Error retrieving ProcessBasedStudy list: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/processbasedstudy/elements/total
     * Get total count of ProcessBasedStudy entities
     * 
     * @return JSON response with total count
     */
    public Result getTotalProcessBasedStudies() {
        try {
            int total = ProcessBasedStudy.findTotal();
            String totalJSON = "{\"total\":" + total + "}";
            return ok(ApiUtil.createResponse(totalJSON, true));
        } catch (Exception e) {
            return ok(ApiUtil.createResponse("Error retrieving ProcessBasedStudy total: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/processbasedstudy/search/:keyword/:pageSize/:offset
     * Search ProcessBasedStudy by keyword in title, aims, or significance
     * 
     * @param keyword Search keyword
     * @param pageSize Number of results per page
     * @param offset Starting offset for pagination
     * @return JSON response with matching ProcessBasedStudy entities
     */
    public Result searchProcessBasedStudies(String keyword, int pageSize, int offset) {
        if (keyword == null || keyword.isEmpty()) {
            return ok(ApiUtil.createResponse("No search keyword provided", false));
        }

        try {
            List<ProcessBasedStudy> results = ProcessBasedStudy.findByKeyword(keyword, pageSize, offset);
            return getProcessBasedStudies(results);
        } catch (Exception e) {
            return ok(ApiUtil.createResponse("Error searching ProcessBasedStudy: " + e.getMessage(), false));
        }
    }

    /**
     * Generate DSG Excel file from ProcessBasedStudy
     * GET /api/processbasedstudy/dsg/:uri
     * 
     * @param uri ProcessBasedStudy URI
     * @return Excel file download or error response
     */
    public Result generateDSG(String uri) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("No URI provided", false));
        }

        try {
            ProcessBasedStudy study = ProcessBasedStudy.find(uri);
            if (study == null) {
                return ok(ApiUtil.createResponse("ProcessBasedStudy not found: " + uri, false));
            }

            // Generate filename
            String studyId = study.getStudyID() != null ? study.getStudyID() : "STUDY";
            String filename = studyId + "_DSG_" + System.currentTimeMillis() + ".xlsx";
            
            // Get temp directory
            String tempDir = System.getProperty("java.io.tmpdir");
            
            // Generate DSG using ProcessBasedStudyDSGGen
            String outputPath = org.hascoapi.transform.mt.dsg.ProcessBasedStudyDSGGen.generateFromProcessBasedStudy(
                study, filename, tempDir
            );
            
            if (outputPath == null) {
                return ok(ApiUtil.createResponse("Failed to generate DSG", false));
            }
            
            // Return file for download
            java.io.File file = new java.io.File(outputPath);
            if (!file.exists()) {
                return ok(ApiUtil.createResponse("Generated file not found", false));
            }
            
            return ok(file)
                .as("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .withHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            
        } catch (Exception e) {
            return ok(ApiUtil.createResponse("Error generating DSG: " + e.getMessage(), false));
        }
    }
}

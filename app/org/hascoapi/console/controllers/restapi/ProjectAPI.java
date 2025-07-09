package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.Project;
import org.hascoapi.entity.pojo.RequiredInstrument;
import org.hascoapi.entity.pojo.Task;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.SCHEMA;
import play.mvc.Http;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectAPI extends Controller {

    public static Result getProjects(List<Project> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No Project has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,SCHEMA.PROJECT);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result setMembers(Http.Request request) {
        System.out.println("ProjectAPI: setMembers() has been invoked");
        // Get the JSON body from the request
        JsonNode json = request.body().asJson();

        if (json == null) {
            return badRequest("Expecting JSON data");
        }

        // Extract the "taskuri" from the JSON body
        String projecturi = json.path("projecturi").asText();

        if (projecturi.isEmpty()) {
            return badRequest("Missing parameter: projecturi");
        }

        Project project = Project.find(projecturi);

        if (project == null) {
            return ok(ApiUtil.createResponse("Project with URI <" + projecturi + "> could not be found.", false));
        } 

        // Extract the "members" array from the JSON body
        JsonNode membersNode = json.path("members");

        if (!membersNode.isArray()) {
            return badRequest("Missing or invalid parameter: members");
        }

        List<String> memberUris = new ArrayList<>();

        for (JsonNode node : membersNode) {
            String memberUri = node.path("memberUri").asText();
            
            if (memberUri.isEmpty()) {
                return badRequest("Each members entry must have a memberUri");
            }

            System.out.println("   member uri: <" + memberUri + ">");
            memberUris.add(memberUri);

        }

        System.out.println("Total members: <" + memberUris.size() + ">");

        if (memberUris.isEmpty()) {
            return badRequest("List of members is empty");
        }

        project.setContributorUris(memberUris);

        System.out.println("ProjectAPI: will save process");
        try {
            project.save();
        } catch (Exception e) {
            e.printStackTrace();
            return internalServerError("Error saving task: " + e.getMessage());
        }
        System.out.println("ProjectAPI: saved task");

        return ok("Received projecturi: " + projecturi + ", memberuris: " + memberUris);
    }

}

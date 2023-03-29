package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.entity.pojo.Experience;
import org.sirapi.utils.ApiUtil;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;

import static org.sirapi.Constants.*;


public class ExperienceAPI extends Controller {

    private Result createExperienceResult(Experience experience) {
        experience.save();
        return ok(ApiUtil.createResponse("Experience <" + experience.getUri() + "> has been CREATED.", true));
    }

    public Result createExperienceForTesting() {
        Experience testExperience = Experience.find(TEST_EXPERIENCE_URI);
        if (testExperience != null) {
            return ok(ApiUtil.createResponse("Test Experience already exists.", false));
        } else {
            testExperience = new Experience();
            testExperience.setUri(TEST_EXPERIENCE_URI);
            testExperience.setLabel("Test Experience");
            testExperience.setTypeUri(VSTOI.EXPERIENCE);
            testExperience.setHascoTypeUri(VSTOI.EXPERIENCE);
            testExperience.save();
            return ok(ApiUtil.createResponse("Test Experience been CREATED.", true));
        }
    }

    public Result createExperience(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        Experience newExperience;
        try {
            //convert json string to Instrument instance
            newExperience  = objectMapper.readValue(json, Experience.class);
        } catch (Exception e) {
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createExperienceResult(newExperience);
    }

    private Result deleteExperienceResult(Experience experience) {
        String uri = experience.getUri();
        experience.delete();
        return ok(ApiUtil.createResponse("Experience <" + uri + "> has been DELETED.", true));
    }

    public Result deleteExperienceForTesting(){
        Experience experience = Experience.find(TEST_EXPERIENCE_URI);
        if (experience == null) {
            return ok(ApiUtil.createResponse("There is no Test Experience to be deleted.", false));
        } else {
            experience.delete();
            return ok(ApiUtil.createResponse("Test Experience has been DELETED.", true));
        }
    }

    public Result deleteExperience(String uri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No experience URI has been provided.", false));
        }
        Experience experience = Experience.find(uri);
        if (experience == null) {
            return ok(ApiUtil.createResponse("There is no experience with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteExperienceResult(experience);
        }
    }

    public Result getAllExperiences(){
        ObjectMapper mapper = new ObjectMapper();

        List<Experience> results = Experience.find();
        if (results == null) {
            return notFound(ApiUtil.createResponse("No experience has been found", false));
        } else {
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("experienceFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri", "hascoTypeLabel", "comment"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}

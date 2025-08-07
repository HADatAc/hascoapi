package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.ResultSetRewindable;
import org.hascoapi.RepositoryInstance;
import org.hascoapi.console.controllers.ontologies.LoadOnt;
import org.hascoapi.entity.pojo.NameSpace;
import org.hascoapi.entity.pojo.Repository;
import org.hascoapi.entity.pojo.Table;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import play.mvc.Controller;
import play.mvc.Result;
import com.typesafe.config.ConfigFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RepoPage extends Controller {

    public Result getRepository() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // get the list of variables in that study
            // serialize the Study object first as ObjectNode
            //   as JsonNode is immutable and meant to be read-only
            ObjectNode obj = mapper.convertValue(RepositoryInstance.getInstance(), ObjectNode.class);
            JsonNode jsonObject = mapper.convertValue(obj, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error parsing class " + Repository.className, false));
        }
    }

    public Result updateLabel(String label){
        if (label == null || label.equals("")) {
            return ok(ApiUtil.createResponse("No (name) has been provided.", false));
        }
        RepositoryInstance.getInstance().setLabel(label);
        RepositoryInstance.getInstance().save();
        return ok(ApiUtil.createResponse("Repository's (name) has been UPDATED.", true));
    }

    public Result updateTitle(String title){
        if (title == null || title.equals("")) {
            return ok(ApiUtil.createResponse("No (title) has been provided.", false));
        }
        RepositoryInstance.getInstance().setTitle(title);
        RepositoryInstance.getInstance().save();
        return ok(ApiUtil.createResponse("Repository's (title) has been UPDATED.", true));
    }

    public Result updateURL(String url){
        if (url == null || url.equals("")) {
            return ok(ApiUtil.createResponse("No (domainURL) has been provided.", false));
        }
        RepositoryInstance.getInstance().setHasDomainURL(url);
        RepositoryInstance.getInstance().save();
        return ok(ApiUtil.createResponse("Repository's (domainURL) has been UPDATED.", true));
    }

    public Result updateDescription(String description){
        if (description == null || description.equals("")) {
            return ok(ApiUtil.createResponse("No (description) has been provided.", false));
        }
        RepositoryInstance.getInstance().setComment(description);
        RepositoryInstance.getInstance().save();
        return ok(ApiUtil.createResponse("Repository's (description) has been UPDATED.", true));
    }

    public Result updateDefaultNamespace(String prefix, String url, String sourceMime, String source) {
        if (sourceMime.equals("_")) {
            sourceMime = "";
        }
        if (source.equals("_")) {
            source = "";
        }
        //System.out.println("updateDefaultNamespace:");
        //System.out.println("    - Namespace prefix: [" + prefix + "]");
        //System.out.println("    - Namespace url: [" + url + "]");
        //System.out.println("    - Namespace mime: [" + sourceMime + "]");
        //System.out.println("    - Namespace source: [" + source + "]");
        if (prefix == null || prefix.equals("")) {
            return ok(ApiUtil.createResponse("No (prefix) has been provided.", false));
        }
        if (url == null || url.equals("")) {
            return ok(ApiUtil.createResponse("No (url) has been provided.", false));
        }
        if (sourceMime == null) {
            sourceMime = "";
        }
        if (source == null) {
            source = "";
        }
        RepositoryInstance.getInstance().setHasDefaultNamespacePrefix(prefix);
        System.out.println("RepoPage: default namespace prefix is [" + prefix + "]");
        RepositoryInstance.getInstance().setHasDefaultNamespaceURL(url);
        RepositoryInstance.getInstance().setHasDefaultNamespaceSourceMime(sourceMime);
        RepositoryInstance.getInstance().setHasDefaultNamespaceSource(source);
        RepositoryInstance.getInstance().save();
        NameSpaces.getInstance().updateLocalNamespace();
        return ok(ApiUtil.createResponse("Repository's local namespace has been UPDATED.", true));
    }

    public Result updateNamespace(String abbreviation, String url){
        if (abbreviation == null || abbreviation.equals("")) {
            return ok(ApiUtil.createResponse("No (abbreviation) has been provided.", false));
        }
        if (url == null || url.equals("")) {
            return ok(ApiUtil.createResponse("No (url) has been provided.", false));
        }
        RepositoryInstance.getInstance().setHasNamespaceAbbreviation(abbreviation);
        RepositoryInstance.getInstance().setHasNamespaceURL(url);
        RepositoryInstance.getInstance().save();
        NameSpaces.getInstance().resetNameSpaces();;
        return ok(ApiUtil.createResponse("Repository's local namespace has been UPDATED.", true));
    }

    public Result createNamespace(String json){
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No JSON has been provided.", false));
        }
        if (RepositoryInstance.getInstance().newNamespace(json)) {
            RepositoryInstance.getInstance().save();
            //NameSpaces.getInstance().resetNameSpaces();
            return ok(ApiUtil.createResponse("New namespace has been added to the repository.", true));
        } else {
            return ok(ApiUtil.createResponse("Failed to add new namespace into the repository.", false));
        }
    }

    public Result resetNamespaces(){
        if (RepositoryInstance.getInstance().resetNamespaces()) {
            NameSpaces.getInstance().resetNameSpaces();;
            return ok(ApiUtil.createResponse("Namespaces have been reset.", true));
        } else {
            return ok(ApiUtil.createResponse("Failed to reset namespaces.", false));
        }
    }

    public Result deleteSelectedNamespace(String abbreviation){
        if (abbreviation == null || abbreviation.equals("")) {
            return ok(ApiUtil.createResponse("No Namespace's ABBREVIATION has been provided.", false));
        }
        String response = NameSpace.deleteNamespace(abbreviation);
        NameSpaces.getInstance().resetNameSpaces();;
        if (response.isEmpty()) {
            return ok(ApiUtil.createResponse("Namespace [" + abbreviation + "] has been DELETED.", true));
        } else {
            return ok(ApiUtil.createResponse("Namespace [" + abbreviation + "] has NOT been DELETED. Reason: " + response, false));
        }
    }

    public Result deleteNamespace(){
        String prefix = RepositoryInstance.getInstance().getHasDefaultNamespacePrefix();
        String url = RepositoryInstance.getInstance().getHasDefaultNamespaceURL();
        String mime = RepositoryInstance.getInstance().getHasDefaultNamespaceSourceMime();
        String source = RepositoryInstance.getInstance().getHasDefaultNamespaceSource();
        if (prefix == null || prefix.equals("") ||
            url == null    || url.equals("") ||
            mime == null   || mime.equals("") ||
            source == null || source.equals("")) {
            return ok(ApiUtil.createResponse("There is no default namespace to be deleted.", false));
        }
        RepositoryInstance.getInstance().setHasDefaultNamespacePrefix("");
        RepositoryInstance.getInstance().setHasDefaultNamespaceURL("");
        RepositoryInstance.getInstance().setHasDefaultNamespaceSourceMime("");
        RepositoryInstance.getInstance().setHasDefaultNamespaceSource("");
        RepositoryInstance.getInstance().save();
        NameSpaces.getInstance().deleteLocalNamespace();
        return ok(ApiUtil.createResponse("Repository's local namespace has been DELETED.", true));
    }

    private Long manageTriples(String oper, String kb) {
        LoadOnt.playLoadOntologiesAsync(oper, kb);
        return 0L;
    }

    public Result loadOntologies(){
        String kb = ConfigFactory.load().getString("hascoapi.repository.triplestore");
        CompletableFuture<Long> completableFuture = CompletableFuture.supplyAsync(() -> manageTriples("load", kb));
        //while (!completableFuture.isDone()) {
        //    System.out.println("CompletableFuture is not finished yet...");
        //}
        //long result = completableFuture.get();
        return ok(ApiUtil.createResponse("Repository's ontologies has been requested to be LOADED.", true));
    }

    public Result deleteOntologies(){
        String kb = ConfigFactory.load().getString("hascoapi.repository.triplestore");
        CompletableFuture<Long> completableFuture = CompletableFuture.supplyAsync(() -> manageTriples("delete", kb));
        //while (!completableFuture.isDone()) {
        //    System.out.println("CompletableFuture is not finished yet...");
        //}
        //long result = completableFuture.get();
        return ok(ApiUtil.createResponse("Repository's ontologies have been requested to be DELETED.", true));
    }

    public Result getLanguages() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // get the list of variables in that study
            // serialize the Study object first as ObjectNode
            //   as JsonNode is immutable and meant to be read-only
            //List<Table> table = Table.findLanguage();
            //for (Table entry: table) {
            //    System.out.println(entry.getCode());
            // }
            //System.out.println("inside getLanguages");
            ArrayNode array = mapper.convertValue(Table.findLanguage(), ArrayNode.class);
            JsonNode jsonObject = mapper.convertValue(array, JsonNode.class);
            //System.out.println("inside getLanguages [" + jsonObject + "]");
            return ok(ApiUtil.createResponse(jsonObject, true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error retrieving languages", false));
        }
    }

    public Result getGenerationActivities() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // get the list of variables in that study
            // serialize the Study object first as ObjectNode
            //   as JsonNode is immutable and meant to be read-only
            //List<Table> table = Table.find();
            //for (Table entry: table) {
            //    System.out.println(entry.getCode());
            //}
            ArrayNode array = mapper.convertValue(Table.findGenerationActivity(), ArrayNode.class);
            JsonNode jsonObject = mapper.convertValue(array, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error retrieving generation activities", false));
        }
    }

    public Result getInformants() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // get the list of variables in that study
            // serialize the Study object first as ObjectNode
            //   as JsonNode is immutable and meant to be read-only
            //List<Table> table = Table.find();
            //for (Table entry: table) {
            //    System.out.println(entry.getCode());
            //}
            ArrayNode array = mapper.convertValue(Table.findInformant(), ArrayNode.class);
            JsonNode jsonObject = mapper.convertValue(array, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error retrieving informants", false));
        }
    }

    public Result getNamespaces() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            ArrayNode array = mapper.convertValue(NameSpaces.getInstance().getOrderedNamespacesAsList(), ArrayNode.class);
            JsonNode jsonObject = mapper.convertValue(array, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error retrieving namespaces", false));
        }
    }

    public Result getInstrumentPositions() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            ArrayNode array = mapper.convertValue(Table.findInstrumentPosition(), ArrayNode.class);
            JsonNode jsonObject = mapper.convertValue(array, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error retrieving Instrument Positions", false));
        }
    }

    public Result getSubcontainerPositions() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            ArrayNode array = mapper.convertValue(Table.findSubcontainerPosition(), ArrayNode.class);
            JsonNode jsonObject = mapper.convertValue(array, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error retrieving Subcontainer Positions", false));
        }
    }

    public static Result queryTest() {
        String sparqlEndpoint = "http://localhost:3030/ds/sparql";
        String sparqlQuery = "SELECT * WHERE { ?s ?p ?o } LIMIT 1";

        try {
            // Executa a query via SPARQLUtils
            ResultSetRewindable results = SPARQLUtils.select(sparqlEndpoint, sparqlQuery);

            // Converte o ResultSet para JSON (formato SPARQL Results JSON)
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(outputStream, results);
            String json = outputStream.toString();

            return ok(json).as("application/json");

        } catch (Exception e) {
            return internalServerError("SPARQL query failed: " + e.getMessage());
        }
    }

}

package org.sirapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hl7.fhir.r4.model.Coding;
import org.sirapi.annotations.PropertyField;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.NameSpaces;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.RDF;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.VSTOI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@JsonFilter("responseOptionFilter")
public class ResponseOption extends HADatAcThing implements SIRElement, Comparable<ResponseOption>  {

    @PropertyField(uri="vstoi:hasStatus")
    private String hasStatus;

    @PropertyField(uri="vstoi:hasSerialNumber")
    String serialNumber;

    @PropertyField(uri="hasco:hasImage")
    String image;

    @PropertyField(uri="vstoi:hasContent")
    String hasContent;

    @PropertyField(uri="vstoi:hasLanguage")
    private String hasLanguage;

    @PropertyField(uri="vstoi:hasVersion")
    String hasVersion;

    @PropertyField(uri="vstoi:hasSIRMaintainerEmail")
    private String hasSIRMaintainerEmail;

    public String getHasStatus() {
        return hasStatus;
    }

    public void setHasStatus(String hasStatus) {
        this.hasStatus = hasStatus;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getHasContent() {
        return hasContent;
    }

    public void setHasContent(String hasContent) {
        this.hasContent = hasContent;
    }

    public String getHasLanguage() {
        return hasLanguage;
    }

    public void setHasLanguage(String hasLanguage) {
        this.hasLanguage = hasLanguage;
    }

    public String getHasVersion() {
        return hasVersion;
    }

    public void setHasVersion(String hasVersion) {
        this.hasVersion = hasVersion;
    }

    public String getHasSIRMaintainerEmail() {
        return hasSIRMaintainerEmail;
    }

    public void setHasSIRMaintainerEmail(String hasSIRMaintainerEmail) {
        this.hasSIRMaintainerEmail = hasSIRMaintainerEmail;
    }

    public static List<ResponseOption> find() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?respOption rdfs:subClassOf* vstoi:ResponseOption . " +
                " ?uri a ?respOption ." +
                "} ";

        return findByQuery(queryString);
    }

    public static List<ResponseOption> findByLanguage(String language) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?respOption rdfs:subClassOf* vstoi:ResponseOption . " +
                " ?uri a ?respOption ." +
                " ?uri vstoi:hasLanguage ?language . " +
                "   FILTER (?language = \"" + language + "\") " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<ResponseOption> findByKeyword(String keyword) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?respModel rdfs:subClassOf* vstoi:ResponseOption . " +
                " ?uri a ?respModel ." +
                " ?uri vstoi:hasContent ?content . " +
                "   FILTER regex(?content, \"" + keyword + "\", \"i\") " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<ResponseOption> findByKeywordAndLanguageWithPages(String keyword, String language, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT ?uri WHERE { " +
                " ?respModel rdfs:subClassOf* vstoi:ResponseOption . " +
                " ?uri a ?respModel .";
        if (!language.isEmpty()) {
            queryString += " ?uri vstoi:hasLanguage ?language . ";
        }
        queryString += " ?uri vstoi:hasContent ?content . ";
        if (!keyword.isEmpty() && !language.isEmpty()) {
            queryString += "   FILTER (regex(?content, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) ";
        } else if (!keyword.isEmpty()) {
            queryString += "   FILTER (regex(?content, \"" + keyword + "\", \"i\")) ";
        } else if (!language.isEmpty()) {
            queryString += "   FILTER ((?language = \"" + language + "\")) ";
        }
        queryString += "} " +
                " ORDER BY ASC(?content) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return findByQuery(queryString);
    }

    public static int findTotalByKeywordAndLanguage(String keyword, String language) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
                " ?respModel rdfs:subClassOf* vstoi:ResponseOption . " +
                " ?uri a ?respModel .";
        if (!language.isEmpty()) {
            queryString += " ?uri vstoi:hasLanguage ?language . ";
        }
        if (!keyword.isEmpty()) {
            queryString += " ?uri vstoi:hasContent ?content . ";
        }
        if (!keyword.isEmpty() && !language.isEmpty()) {
            queryString += "   FILTER (regex(?content, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) ";
        } else if (!keyword.isEmpty()) {
            queryString += "   FILTER (regex(?content, \"" + keyword + "\", \"i\")) ";
        } else if (!language.isEmpty()) {
            queryString += "   FILTER ((?language = \"" + language + "\")) ";
        }
        queryString += "}";

        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

            if (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                return Integer.parseInt(soln.getLiteral("tot").getString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<ResponseOption> findByMaintainerEmailWithPages(String maintainerEmail, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT ?uri WHERE { " +
                " ?respModel rdfs:subClassOf* vstoi:ResponseOption . " +
                " ?uri a ?respModel ." +
                " ?uri vstoi:hasContent ?content . " +
                " ?uri vstoi:hasSIRMaintainerEmail ?maintainerEmail . " +
                "   FILTER (?maintainerEmail = \"" + maintainerEmail + "\") " +
                "}";
        queryString += " ORDER BY ASC(?content) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return findByQuery(queryString);
    }

    public static int findTotalByMaintainerEmail(String maintainerEmail) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
                " ?respModel rdfs:subClassOf* vstoi:ResponseOption . " +
                " ?uri a ?respModel ." +
                " ?uri vstoi:hasSIRMaintainerEmail ?maintainerEmail . " +
                "   FILTER (?maintainerEmail = \"" + maintainerEmail + "\") " +
                "}";
        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
            if (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                return Integer.parseInt(soln.getLiteral("tot").getString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<ResponseOption> findByMaintainerEmail(String maintainerEmail) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?respOption rdfs:subClassOf+ vstoi:ResponseOption . " +
                " ?uri a ?respOption ." +
                " ?uri vstoi:hasSIRMaintainerEmail ?maintainerEmail . " +
                "   FILTER (?maintainerEmail = \"" + maintainerEmail + "\") " +
                "} ";

        return findByQuery(queryString);
    }

    private static List<ResponseOption> findByQuery(String queryString) {
        List<ResponseOption> options = new ArrayList<ResponseOption>();

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            //System.out.println("inside ResponseOption.find(): found uri [" + soln.getResource("uri").getURI().toString() + "]");
            if (soln != null && soln.getResource("uri") != null) {
                ResponseOption responseOption = find(soln.getResource("uri").getURI());
                options.add(responseOption);
            }
        }

        java.util.Collections.sort((List<ResponseOption>) options);
        return options;
    }

    public static int getNumberResponseOptions() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?respOption rdfs:subClassOf* vstoi:ResponseOption . " +
                " ?uri a ?respOption ." +
                "}";

        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

            if (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                return Integer.parseInt(soln.getLiteral("tot").getString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<ResponseOption> findWithPages(int pageSize, int offset) {
        List<ResponseOption> options = new ArrayList<ResponseOption>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?option rdfs:subClassOf* vstoi:ResponseOption . " +
                " ?uri a ?option . } " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI() != null) {
                ResponseOption responseOption = ResponseOption.find(soln.getResource("uri").getURI());
                options.add(responseOption);
            }
        }
        return options;
    }

    public static ResponseOption find(String uri) {
        ResponseOption responseOption = null;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

        responseOption = new ResponseOption();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                responseOption.setLabel(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                responseOption.setTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                responseOption.setComment(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                responseOption.setHascoTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
                responseOption.setHasStatus(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SERIAL_NUMBER)) {
                responseOption.setSerialNumber(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_IMAGE)) {
                responseOption.setImage(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_CONTENT)) {
                responseOption.setHasContent(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
                responseOption.setHasLanguage(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
                responseOption.setHasVersion(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MAINTAINER_EMAIL)) {
                responseOption.setHasSIRMaintainerEmail(object.asLiteral().getString());
            }
        }

        responseOption.setUri(uri);

        return responseOption;
    }

    public static boolean attach(String codebookSlotUri, String responseOptionUri) {
        if (codebookSlotUri == null || codebookSlotUri.isEmpty()) {
            return false;
        }
        CodebookSlot codebookSlot = CodebookSlot.find(codebookSlotUri);
        if (codebookSlot == null) {
            System.out.println("CodebookSlot.find returned nothing");
        }
        if (codebookSlot == null) {
            return false;
        }
        return codebookSlot.updateCodebookSlotResponseOption(responseOptionUri);
    }

    public static boolean detach(String codebookSlotUri) {
        if (codebookSlotUri == null || codebookSlotUri.isEmpty()) {
            return false;
        }
        CodebookSlot codebookSlot = CodebookSlot.find(codebookSlotUri);
        if (codebookSlot == null) {
            return false;
        }
        return codebookSlot.updateCodebookSlotResponseOption(null);
    }



    @Override
    public int compareTo(ResponseOption another) {
        return this.getLabel().compareTo(another.getLabel());
    }

    @Override public void save() {
        saveToTripleStore();
    }

    @Override public void delete() {
        deleteFromTripleStore();
    }

    @Override
    public boolean saveToSolr() {
        return false;
    }

    @Override
    public int deleteFromSolr() {
        return 0;
    }

    /*
    public Coding getFHIRObject() {
        Coding coding = new Coding();
        coding.setCode(getUri());
        coding.setDisplay(getHasContent());
        return coding;
    }
     */
}

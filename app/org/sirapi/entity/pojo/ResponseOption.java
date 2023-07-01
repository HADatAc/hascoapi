package org.sirapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.sirapi.annotations.PropertyField;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.NameSpaces;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.RDF;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.VSTOI;

import java.util.ArrayList;
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

    @PropertyField(uri="vstoi:ofExperience")
    String ofExperience;

    @PropertyField(uri="vstoi:hasContent")
    String hasContent;

    @PropertyField(uri="vstoi:hasPriority")
    String hasPriority;

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

    public String getOfExperience() {
        return ofExperience;
    }

    public void setOfExperience(String ofExperience) {
        this.ofExperience = ofExperience;
    }

    public String getHasContent() {
        return hasContent;
    }

    public void setHasContent(String hasContent) {
        this.hasContent = hasContent;
    }

    public String getHasPriority() {
        return hasPriority;
    }

    public void setHasPriority(String hasPriority) {
        this.hasPriority = hasPriority;
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

    public static List<ResponseOption> findByExperience(String experienceUri) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?respOption rdfs:subClassOf* vstoi:ResponseOption . " +
                " ?uri a ?respOption ." +
                " ?uri vstoi:ofExperience <" + experienceUri + "> . " +
                "} ";

        return findByQueryOrderedByPriority(queryString);

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
                " ?respOption rdfs:subClassOf+ vstoi:ResponseOption . " +
                " ?uri a ?respOption ." +
                " ?uri vstoi:hasLanguage ?language . " +
                "   FILTER (?language = \"" + language + "\") " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<ResponseOption> findByKeyword(String keyword) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?respOption rdfs:subClassOf+ vstoi:ResponseOption . " +
                " ?uri a ?respOption ." +
                " ?uri rdfs:label ?label . " +
                "   FILTER regex(?label, \"" + keyword + "\", \"i\") " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<ResponseOption> findByKeywordAndLanguage(String keyword, String language) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?respOption rdfs:subClassOf+ vstoi:ResponseOption . " +
                " ?uri a ?respOption ." +
                " ?uri vstoi:hasLanguage ?language . " +
                " ?uri rdfs:label ?label . " +
                "   FILTER (regex(?label, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) " +
                "} ";

        return findByQuery(queryString);
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
        //System.out.println("Query: " + queryString);

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            //System.out.println("inside ResponseOption.find(): found uri [" + soln.getResource("uri").getURI().toString() + "]");
            ResponseOption responseOption = find(soln.getResource("uri").getURI());
            options.add(responseOption);
        }

        java.util.Collections.sort((List<ResponseOption>) options);
        return options;
    }

    private static List<ResponseOption> findByQueryOrderedByPriority(String queryString) {
        List<ResponseOption> options = new ArrayList<ResponseOption>();
        //System.out.println("Query: " + queryString);

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            //System.out.println("inside ResponseOption.find(): found uri [" + soln.getResource("uri").getURI().toString() + "]");
            ResponseOption responseOption = find(soln.getResource("uri").getURI());
            options.add(responseOption);
        }

        java.util.Collections.sort(options, new Comparator<ResponseOption>() {
            @Override
            public int compare(ResponseOption lhs, ResponseOption rhs) {
                int left = Integer.parseInt(lhs.getHasPriority());
                int right = Integer.parseInt(rhs.getHasPriority());
                return left < right ? -1 : (left > right) ? 1 : 0;
            }
        });

        //System.out.println(Arrays.toString(options.toArray()));

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
            } else if (statement.getPredicate().getURI().equals(VSTOI.OF_EXPERIENCE)) {
                responseOption.setOfExperience(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_CONTENT)) {
                responseOption.setHasContent(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_PRIORITY)) {
                responseOption.setHasPriority(object.asLiteral().getString());
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
}

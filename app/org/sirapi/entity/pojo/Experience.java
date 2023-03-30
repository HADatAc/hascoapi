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
import java.util.List;

@JsonFilter("experienceFilter")
public class Experience extends HADatAcThing implements Comparable<Experience>  {

    @PropertyField(uri="vstoi:hasSerialNumber")
    private String serialNumber;

    @PropertyField(uri="vstoi:hasLanguage")
    private String hasLanguage;

    @PropertyField(uri="vstoi:hasSIRMaintainerEmail")
    private String hasSIRMaintainerEmail;

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getHasLanguage() {
        return hasLanguage;
    }

    public void setHasLanguage(String hasLanguage) {
        this.hasLanguage = hasLanguage;
    }

    public String getHasSIRMaintainerEmail() {
        return hasSIRMaintainerEmail;
    }

    public void setHasSIRMaintainerEmail(String hasSIRMaintainerEmail) {
        this.hasSIRMaintainerEmail = hasSIRMaintainerEmail;
    }

    public List<ResponseOption> getResponseOptions() {
        return ResponseOption.findByExperience(getUri());
    }

    public static List<Experience> findByLanguage(String language) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?experience rdfs:subClassOf* vstoi:Experience . " +
                " ?uri a ?experience ." +
                " ?uri vstoi:hasLanguage ?language . " +
                "   FILTER (?language = \"" + language + "\") " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<Experience> findByKeyword(String keyword) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?experienceType rdfs:subClassOf* vstoi:Experience . " +
                " ?uri a ?experienceType ." +
                " ?uri rdfs:label ?label . " +
                "   FILTER regex(?label, \"" + keyword + "\", \"i\") " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<Experience> findByKeywordAndLanguage(String keyword, String language) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?experienceType rdfs:subClassOf* vstoi:Experience . " +
                " ?uri a ?experienceType ." +
                " ?uri vstoi:hasLanguage ?language . " +
                " ?uri rdfs:label ?label . " +
                "   FILTER (regex(?label, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<Experience> findByMaintainerEmail(String maintainerEmail) {
        System.out.println("Owner emmail: [" + maintainerEmail + "]");
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?experienceType rdfs:subClassOf* vstoi:Experience . " +
                " ?uri a ?experienceType ." +
                " ?uri vstoi:hasSIRMaintainerEmail ?maintainerEmail . " +
                "   FILTER (?maintainerEmail = \"" + maintainerEmail + "\") " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<Experience> find() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?experience rdfs:subClassOf* vstoi:Experience . " +
                " ?uri a ?experience ." +
                "} ";

        return findByQuery(queryString);
    }

    private static List<Experience> findByQuery(String queryString) {
        List<Experience> experiences = new ArrayList<Experience>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            Experience experience = find(soln.getResource("uri").getURI());
            System.out.println("Found [" + experience.getUri() + "]");
            experiences.add(experience);
        }

        java.util.Collections.sort((List<Experience>) experiences);
        return experiences;
    }

    public static int getNumberExperiences() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?experience rdfs:subClassOf* vstoi:Experience . " +
                " ?uri a ?experience ." +
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

    public static List<Experience> findWithPages(int pageSize, int offset) {
        List<Experience> options = new ArrayList<Experience>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?experience rdfs:subClassOf* vstoi:Experience . " +
                " ?uri a ?option . } " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI() != null) {
                Experience responseOption = Experience.find(soln.getResource("uri").getURI());
                options.add(responseOption);
            }
        }
        return options;
    }

    public static Experience find(String uri) {
        Experience experience = null;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

        experience = new Experience();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                experience.setLabel(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                experience.setTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                experience.setHascoTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SERIAL_NUMBER)) {
                experience.setSerialNumber(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
                experience.setHasLanguage(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MAINTAINER_EMAIL)) {
                experience.setHasSIRMaintainerEmail(object.asLiteral().getString());
            }
        }

        experience.setUri(uri);

        return experience;
    }

    @Override
    public int compareTo(Experience another) {
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

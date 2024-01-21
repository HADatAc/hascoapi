package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.vocabularies.FOAF;

@JsonFilter("personFilter")
public class Person extends Agent {

    public Organization getAffiliation() {
        if (this.getMember() == null) {
            return null;
        }
        return Organization.find(this.getMember());
    }

    public static List<Person> find() {
        String query =
            " SELECT ?uri WHERE { " +
            " ?uri a foaf:Person ." +
            "} ";
        return findByQuery(query);
    }

    private static List<Person> findByQuery(String requestedQuery) {
        List<Person> people = new ArrayList<Person>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + requestedQuery;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            String uri = soln.getResource("uri").getURI();
            Person person = Person.find(uri);
            people.add(person);
        }

        java.util.Collections.sort((List<Person>) people);
        return people;
    }

    public static Person find(String uri) {
        Person person = null;
        Statement statement;
        RDFNode object;
        String queryString;

        if (uri.startsWith("<")) {
            queryString = "DESCRIBE " + uri + " ";
        } else {
            queryString = "DESCRIBE <" + uri + ">";
        }

        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        person = new Person();
        StmtIterator stmtIterator = model.listStatements();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            String str = URIUtils.objectRDFToString(object);
             if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                person.setLabel(str);
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                person.setTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                person.setComment(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                person.setHascoTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(FOAF.NAME)) {
                person.setName(str);
            } else if (statement.getPredicate().getURI().equals(FOAF.FAMILY_NAME)) {
                person.setFamilyName(str);
            } else if (statement.getPredicate().getURI().equals(FOAF.GIVEN_NAME)) {
                person.setGivenName(str);
            } else if (statement.getPredicate().getURI().equals(FOAF.MBOX)) {
                person.setMbox(str);
            } else if (statement.getPredicate().getURI().equals(FOAF.MEMBER)) {
                person.setMember(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                person.setHasSIRManagerEmail(str);
            }
        }

        person.setUri(uri);

        return person;
    }

    @Override
    public void save() {
        saveToTripleStore();
    }

    @Override
    public void delete() {
        deleteFromTripleStore();
    }

}

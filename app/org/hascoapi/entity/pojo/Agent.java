package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.vocabularies.FOAF;

public class Agent extends HADatAcThing implements Comparable<Agent> {

    @PropertyField(uri="foaf:name")
    protected String name;

    @PropertyField(uri="foaf:familyName")
    protected String familyName;

    @PropertyField(uri="foaf:givenName")
    protected String givenName;

    @PropertyField(uri="foaf:mbox")
    protected String mbox;

    @PropertyField(uri="foaf:member")
    protected String member;

    @PropertyField(uri="vstoi:hasSIRManagerEmail")
    protected String hasSIRManagerEmail;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getFamilyName() {
        return familyName;
    }
    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getGivenName() {
        return givenName;
    }
    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getMbox() {
        return mbox;
    }
    public void setMbox(String mbox) {
        this.mbox = mbox;
    }

    public String getMember() {
        return member;
    }
    public void setMember(String member) {
        this.member = member;
    }

    public String getHasSIRManagerEmail() {
        return hasSIRManagerEmail;
    }
    public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail;
    }

    /** 
    public static List<Agent> findOrganizations() {
        String query =
                " SELECT ?uri WHERE { " +
                        " ?uri a foaf:Group ." +
                        "} ";
        return findByQuery(query);
    }

    public static List<Agent> findPersons() {
        String query =
                " SELECT ?uri WHERE { " +
                        " ?uri a foaf:Person ." +
                        "} ";
        return findByQuery(query);
    }
    */

    /** 
    private static List<Agent> findByQuery(String requestedQuery) {
        List<Agent> agents = new ArrayList<Agent>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + requestedQuery;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            String resp_uri = soln.getResource("uri").getURI();
            Agent agent = Agent.find(resp_uri);
            agents.add(agent);
        }

        java.util.Collections.sort((List<Agent>) agents);
        return agents;
    }
    */

    /** 
    public static Agent find(String agent_uri) {
        Agent agent = null;
        Statement statement;
        RDFNode object;
        String queryString;

        if (agent_uri.startsWith("<")) {
            queryString = "DESCRIBE " + agent_uri + " ";
        } else {
            queryString = "DESCRIBE <" + agent_uri + ">";
        }

        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        agent = new Agent();
        StmtIterator stmtIterator = model.listStatements();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            String str = URIUtils.objectRDFToString(object);
             if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                agent.setLabel(str);
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                agent.setTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                agent.setComment(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                agent.setHascoTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(FOAF.NAME)) {
                agent.setName(str);
            } else if (statement.getPredicate().getURI().equals(FOAF.FAMILY_NAME)) {
                agent.setFamilyName(str);
            } else if (statement.getPredicate().getURI().equals(FOAF.GIVEN_NAME)) {
                agent.setGivenName(str);
            } else if (statement.getPredicate().getURI().equals(FOAF.MBOX)) {
                agent.setMbox(str);
            } else if (statement.getPredicate().getURI().equals(FOAF.MEMBER)) {
                agent.setMember(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                agent.setHasSIRManagerEmail(str);
            }
        }

        agent.setUri(agent_uri);

        return agent;
    }
    */

    @Override
    public int compareTo(Agent another) {
        if (this.getName() == null || another == null || another.getName() == null) {
            return 0;
        }
        return this.getName().compareTo(another.getName());
    }

}

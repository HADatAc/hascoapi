package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.SCHEMA;
import org.hascoapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@JsonFilter("projectFilter")
public class Project extends HADatAcThing implements Comparable<Project> {

    private static final Logger log = LoggerFactory.getLogger(Project.class);

    @PropertyField(uri="schema:startDate")
    private String startDate;

    @PropertyField(uri="schema:endDate")
    private String endDate;

    @PropertyField(uri="schema:funding")
    private String fundingUri;

    @PropertyField(uri="schema:contributor")
    private List<String> contributorUris = new ArrayList<>();

    @PropertyField(uri="vstoi:hasSIRManagerEmail")
    private String hasSIRManagerEmail;

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getFundingUri() {
        return fundingUri;
    }

    public void setFundingUri(String fundingUri) {
        this.fundingUri = fundingUri;
    }

    public List<String> getContributorUris() {
        return contributorUris;
    }

    public void setContributorUris(List<String> contributorUris) {
        this.contributorUris = contributorUris;
    }

    public void addContributorUri(String contributorUri) {
        this.contributorUris.add(contributorUri);
    }

    public String getHasSIRManagerEmail() {
        return hasSIRManagerEmail;
    }
    public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail;
    }

    public static Project findByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String query =
                "SELECT ?uri WHERE { " +
                        "?uri a schema:Project . " +
                        "?uri rdfs:label ?label . " +
                        "FILTER (?label=\"" + name + "\"^^xsd:string) . " +
                        "}";
        return findOneByQuery(query);
    }

    private static Project findOneByQuery(String requestedQuery) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + requestedQuery;
        
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        String uri = null;
        Project project = null;
        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.get("uri") != null) {
                uri = soln.get("uri").toString();
                project = Project.find(uri);
            }
        }

        return project;
    }

    public static Project find(String uri) {
        Project project = new Project();
        project.setUri(uri);

        String queryString = "DESCRIBE <" + uri + ">";
        org.apache.jena.rdf.model.Model model = SPARQLUtils.describe(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        model.listStatements().forEachRemaining(statement -> {
            String predicateUri = statement.getPredicate().getURI();
            String objectValue = URIUtils.objectRDFToString(statement.getObject());

            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                project.setLabel(objectValue);
            } else if (predicateUri.equals(RDF.TYPE)) {
                project.setTypeUri(objectValue);
            } else if (predicateUri.equals(RDFS.COMMENT)) {
                project.setComment(objectValue);
            } else if (predicateUri.equals(HASCO.HASCO_TYPE)) {
                project.setHascoTypeUri(objectValue);
            } else if (predicateUri.equals(HASCO.HAS_IMAGE)) {
                project.setHasImageUri(objectValue);
            } else if (predicateUri.equals(HASCO.HAS_WEB_DOCUMENT)) {
                project.setHasWebDocument(objectValue);
            } else if (predicateUri.equals(SCHEMA.FUNDING)) {
                project.setFundingUri(objectValue);
            } else if (predicateUri.equals(SCHEMA.START_DATE)) {
                project.setStartDate(objectValue);
            } else if (predicateUri.equals(SCHEMA.END_DATE)) {
                project.setEndDate(objectValue);
            } else if (predicateUri.equals(SCHEMA.CONTRIBUTOR)) {
                project.addContributorUri(objectValue);
            } else if (predicateUri.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                project.setHasSIRManagerEmail(objectValue);
            }
        });

        return project;
    }

    @Override
    public int compareTo(Project another) {
        return this.getLabel().compareTo(another.getLabel());
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


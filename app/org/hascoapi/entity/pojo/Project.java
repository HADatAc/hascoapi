package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.*;
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

	@PropertyField(uri="vstoi:hasStatus")
	private String hasStatus;

    @PropertyField(uri = "vstoi:hasVersion")
    private String hasVersion;

    @PropertyField(uri = "vstoi:hasReviewNote")
    String hasReviewNote;

    @PropertyField(uri = "vstoi:hasEditorEmail")
    private String hasEditorEmail;

    @PropertyField(uri="schema:alternateName")
    private String hasShortName;

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

    public String getHasStatus() {
        return hasStatus;
    }

    public void setHasStatus(String hasStatus) {
        this.hasStatus = hasStatus;
    }

    public String getHasVersion() {
        return hasVersion;
    }

    public void setHasVersion(String hasVersion) {
        this.hasVersion = hasVersion;
    }

    public String getHasReviewNote() {      
        return hasReviewNote;
    }

    public void setHasReviewNote(String hasReviewNote) {
        this.hasReviewNote = hasReviewNote;
    }

    public String getHasEditorEmail() {
        return hasEditorEmail;
    }

    public void setHasEditorEmail(String hasEditorEmail) {
        this.hasEditorEmail = hasEditorEmail;
    }

    public String getHasShortName() {
        return hasShortName;
    }

    public void setHasShortName(String hasShortName) {
        this.hasShortName = hasShortName;
    }

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

    public FundingScheme getFunding() {
        if (fundingUri == null || fundingUri.isEmpty()) {
            return null;
        }
        return FundingScheme.find(fundingUri);
    }

    public void setFundingUri(String fundingUri) {
        this.fundingUri = fundingUri;
    }

    public List<String> getContributorUris() {
        return contributorUris;
    }

    public List<Organization> getContributors() {
        List<Organization> contributors = new ArrayList<Organization>();
        if (contributorUris != null && contributorUris.size() > 0) {
            for (String contributorUri : contributorUris) {
                if (contributorUri != null && !contributorUri.isEmpty()) {
                    Organization contributor = Organization.find(contributorUri);
                    if (contributor != null) {
                        contributors.add(contributor);
                    }
                }
            }
        }
        return contributors;
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
        Project project;

		// SELECT query used to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
			project = new Project();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				project.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object); 

                if (predicate.equals(RDFS.LABEL)) {
                    project.setLabel(object);
                } else if (predicate.equals(RDF.TYPE)) {
                    project.setTypeUri(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    project.setComment(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    project.setHascoTypeUri(object);
                } else if (predicate.equals(HASCO.HAS_IMAGE)) {
                    project.setHasImageUri(object);
                } else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
                    project.setHasWebDocument(object);
                } else if (predicate.equals(VSTOI.HAS_STATUS)) {
                    project.setHasStatus(object);
                } else if (predicate.equals(VSTOI.HAS_VERSION)) {
                    project.setHasVersion(object);
                } else if (predicate.equals(VSTOI.HAS_REVIEW_NOTE)) {
                    project.setHasReviewNote(object);
                } else if (predicate.equals(VSTOI.HAS_EDITOR_EMAIL)) {
                    project.setHasEditorEmail(object);
                } else if (predicate.equals(SCHEMA.ALTERNATE_NAME)) {
                    project.setHasShortName(object);
                } else if (predicate.equals(SCHEMA.FUNDING)) {
                    project.setFundingUri(object);
                } else if (predicate.equals(SCHEMA.START_DATE)) {
                    project.setStartDate(object);
                } else if (predicate.equals(SCHEMA.END_DATE)) {
                    project.setEndDate(object);
                } else if (predicate.equals(SCHEMA.CONTRIBUTOR)) {
                    project.addContributorUri(object);
                } else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                    project.setHasSIRManagerEmail(object);
                }
            }
        }

		if (project.getHascoTypeUri() == null || project.getHascoTypeUri().isEmpty()) { 
			System.out.println("[ERROR] Place.java: URI [" + uri + "] has no HASCO TYPE.");
			return null;
		} else if (!project.getHascoTypeUri().equals(SCHEMA.PROJECT)) {
			System.out.println("[ERROR] Place.java: URI [" + uri + "] HASCO TYPE is not " + SCHEMA.PROJECT);
			return null;
		}

		project.setUri(uri);
		
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


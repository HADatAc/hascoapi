package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.*;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.Utils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.SCHEMA;
import org.hascoapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


@JsonFilter("fundingSchemeFilter")
public class FundingScheme extends HADatAcThing implements Comparable<FundingScheme> {

    private static final Logger log = LoggerFactory.getLogger(FundingScheme.class);

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

    @PropertyField(uri="schema:funder")
    private String funderUri;

    @PropertyField(uri="schema:sponsor")
    private String sponsorUri;

    @PropertyField(uri="schema:startDate")
    private String startDate;

    @PropertyField(uri="schema:endDate")
    private String endDate;

    @PropertyField(uri="schema:amount")
    private String amount;

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

    public String getFunderUri() {
        return funderUri;
    }

    public FundingScheme getFunder() {
        if (funderUri == null || funderUri.isEmpty()) {
            return null;
        }
        return FundingScheme.find(funderUri);
    }

    public void setFunderUri(String funderUri) {
        this.funderUri = funderUri;
    }

    public String getSponsorUri() {
        return sponsorUri;
    }

    public Organization getSponsor() {
        if (sponsorUri == null || sponsorUri.isEmpty()) {
            return null;
        }
        return Organization.find(sponsorUri);
    }

    public void setSponsorUri(String sponsorUri) {
        this.sponsorUri = sponsorUri;
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

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getHasSIRManagerEmail() {
        return hasSIRManagerEmail;
    }
    public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail;
    }

    public static FundingScheme findByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String query =
                "SELECT ?uri WHERE { " +
                        "?uri a schema:FundingScheme . " +
                        "?uri rdfs:label ?label . " +
                        "FILTER (?label=\"" + name + "\"^^xsd:string) . " +
                        "}";
        return findOneByQuery(query);
    }

    private static FundingScheme findOneByQuery(String requestedQuery) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + requestedQuery;
        
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        String uri = null;
        FundingScheme fundingScheme = null;
        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.get("uri") != null) {
                uri = soln.get("uri").toString();
                fundingScheme = FundingScheme.find(uri);
            }
        }

        return fundingScheme;
    }



    public static FundingScheme find(String uri) {
        FundingScheme scheme = new FundingScheme();

        // SELECT query used to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
			scheme = new FundingScheme();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				scheme.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object); 

                if (predicate.equals(RDFS.LABEL)) {
                    scheme.setLabel(object);
                } else if (predicate.equals(RDF.TYPE)) {
                    scheme.setTypeUri(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    scheme.setComment(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    scheme.setHascoTypeUri(object);
                } else if (predicate.equals(HASCO.HAS_IMAGE)) {
                    scheme.setHasImageUri(object);
                } else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
                    scheme.setHasWebDocument(object);
                } else if (predicate.equals(VSTOI.HAS_STATUS)) {
                    scheme.setHasStatus(object);
                } else if (predicate.equals(VSTOI.HAS_VERSION)) {
                    scheme.setHasVersion(object);
                } else if (predicate.equals(VSTOI.HAS_REVIEW_NOTE)) {
                    scheme.setHasReviewNote(object);
                } else if (predicate.equals(VSTOI.HAS_EDITOR_EMAIL)) {
                    scheme.setHasEditorEmail(object);
                } else if (predicate.equals(SCHEMA.ALTERNATE_NAME)) {
                    scheme.setHasShortName(object);
                } else if (predicate.equals(SCHEMA.FUNDER)) {
                    scheme.setFunderUri(object);
                } else if (predicate.equals(SCHEMA.SPONSOR)) {
                    scheme.setSponsorUri(object);
                } else if (predicate.equals(SCHEMA.START_DATE)) {
                    scheme.setStartDate(object);
                } else if (predicate.equals(SCHEMA.END_DATE)) {
                    scheme.setEndDate(object);
                } else if (predicate.equals(SCHEMA.AMOUNT)) {
                    scheme.setAmount(object);
                } else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                    scheme.setHasSIRManagerEmail(object);
                }
            }
        }

		if (scheme.getHascoTypeUri() == null || scheme.getHascoTypeUri().isEmpty()) { 
			System.out.println("[ERROR] Place.java: URI [" + uri + "] has no HASCO TYPE.");
			return null;
		} else if (!scheme.getHascoTypeUri().equals(SCHEMA.FUNDING_SCHEME)) {
			System.out.println("[ERROR] Place.java: URI [" + uri + "] HASCO TYPE is not " + SCHEMA.FUNDING_SCHEME);
			return null;
		}

		scheme.setUri(uri);
		
        return scheme;
    }

    @Override
    public int compareTo(FundingScheme another) {
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

package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
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

    public void setFunderUri(String funderUri) {
        this.funderUri = funderUri;
    }

    public String getSponsorUri() {
        return sponsorUri;
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
        scheme.setUri(uri);

        String queryString = "DESCRIBE <" + uri + ">";
        org.apache.jena.rdf.model.Model model = SPARQLUtils.describe(
                org.hascoapi.utils.CollectionUtil.getCollectionPath(
                        org.hascoapi.utils.CollectionUtil.Collection.SPARQL_QUERY), queryString);

        model.listStatements().forEachRemaining(statement -> {
            String predicateUri = statement.getPredicate().getURI();
            String objectValue = URIUtils.objectRDFToString(statement.getObject());

            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                scheme.setLabel(objectValue);
            } else if (predicateUri.equals(RDF.TYPE)) {
                scheme.setTypeUri(objectValue);
            } else if (predicateUri.equals(RDFS.COMMENT)) {
                scheme.setComment(objectValue);
            } else if (predicateUri.equals(HASCO.HASCO_TYPE)) {
                scheme.setHascoTypeUri(objectValue);
            } else if (predicateUri.equals(HASCO.HAS_IMAGE)) {
                scheme.setHasImageUri(objectValue);
            } else if (predicateUri.equals(HASCO.HAS_WEB_DOCUMENT)) {
                scheme.setHasWebDocument(objectValue);
            } else if (predicateUri.equals(VSTOI.HAS_STATUS)) {
                scheme.setHasStatus(objectValue);
            } else if (predicateUri.equals(VSTOI.HAS_VERSION)) {
                scheme.setHasVersion(objectValue);
            } else if (predicateUri.equals(VSTOI.HAS_REVIEW_NOTE)) {
                scheme.setHasReviewNote(objectValue);
            } else if (predicateUri.equals(VSTOI.HAS_EDITOR_EMAIL)) {
                scheme.setHasEditorEmail(objectValue);
            } else if (predicateUri.equals(SCHEMA.ALTERNATE_NAME)) {
                scheme.setHasShortName(objectValue);
            } else if (predicateUri.equals(SCHEMA.FUNDER)) {
                scheme.setFunderUri(objectValue);
            } else if (predicateUri.equals(SCHEMA.SPONSOR)) {
                scheme.setSponsorUri(objectValue);
            } else if (predicateUri.equals(SCHEMA.START_DATE)) {
                scheme.setStartDate(objectValue);
            } else if (predicateUri.equals(SCHEMA.END_DATE)) {
                scheme.setEndDate(objectValue);
            } else if (predicateUri.equals(SCHEMA.AMOUNT)) {
                scheme.setAmount(objectValue);
            } else if (predicateUri.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                scheme.setHasSIRManagerEmail(objectValue);
            }
        });

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

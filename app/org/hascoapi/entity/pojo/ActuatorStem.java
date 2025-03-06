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
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.vocabularies.*;

@JsonFilter("actuatorStemFilter")
public class ActuatorStem extends HADatAcClass implements SIRElement, Comparable<ActuatorStem>  {

    @PropertyField(uri="vstoi:hasStatus")
    private String hasStatus;

    @PropertyField(uri="vstoi:hasContent")
    private String hasContent;

    @PropertyField(uri="vstoi:hasLanguage")
    private String hasLanguage;

    @PropertyField(uri="vstoi:hasVersion")
    private String hasVersion;

    @PropertyField(uri="prov:wasDerivedFrom")
    private String wasDerivedFrom;

    @PropertyField(uri="prov:wasGeneratedBy")
    private String wasGeneratedBy;

    @PropertyField(uri = "vstoi:hasReviewNote")
    String hasReviewNote;

    @PropertyField(uri = "vstoi:hasSIRManagerEmail")
    private String hasSIRManagerEmail;

    @PropertyField(uri = "vstoi:hasEditorEmail")
    private String hasEditorEmail;

    @PropertyField(uri="hasco:activates")
    private String activates;

    public String getHasStatus() {
        return hasStatus;
    }

    public void setHasStatus(String hasStatus) {
        this.hasStatus = hasStatus;
    }

    public String getHasContent() {
        return hasContent;
    }

    public void setHasContent(String hasContent) {
        this.hasContent = hasContent;
    }

    public String getActivates() {
        return activates;
    }

    public SemanticVariable getActivatesSemanticVariable() {
        if (activates == null) {
            return null;
        }
        return SemanticVariable.find(activates);
    }

    public void setActivates(String activates) {
        this.activates = activates;
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

    public void setWasDerivedFrom(String wasDerivedFrom) {
        this.wasDerivedFrom = wasDerivedFrom;
    }

    public String getWasDerivedFrom() {
        return wasDerivedFrom;
    }

    public void setWasGeneratedBy(String wasGeneratedBy) {
        this.wasGeneratedBy = wasGeneratedBy;
    }

    public String getWasGeneratedBy() {
        return wasGeneratedBy;
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

    public String getHasSIRManagerEmail() {
        return hasSIRManagerEmail;
    }

    public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail;
    }

    public String getHasEditorEmail() {
        return hasEditorEmail;
    }

    public void setHasEditorEmail(String hasEditorEmail) {
        this.hasEditorEmail = hasEditorEmail;
    }

    public ActuatorStem () {
    }

    public ActuatorStem (String className) {
		super(className);
    }

    public static List<ActuatorStem> findByInstrument(String instrumentUri) {
        //System.out.println("findByInstrument: [" + instrumentUri + "]");
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:ActuatorStem . " +
                " ?uri a ?model ." +
                " ?attUri vstoi:hasActuatorStem ?uri . " +
                " ?attUri vstoi:belongsTo <" + instrumentUri + ">. " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<ActuatorStem> findAvailable() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                "   { ?model rdfs:subClassOf* vstoi:ActuatorStem . " +
                "     ?uri a ?model ." +
                "   } MINUS { " +
                "     ?dep_uri a vstoi:Deployment . " +
                "     ?dep_uri hasco:hasActuatorStem ?uri .  " +
                "     FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " +
                "    } " +
                "} " +
                "ORDER BY DESC(?datetime) ";

        return findByQuery(queryString);
    }

    public static List<ActuatorStem> findDeployed() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                "   ?model rdfs:subClassOf* vstoi:ActuatorStem . " +
                "   ?uri a ?model ." +
                "   ?dep_uri a vstoi:Deployment . " +
                "   ?dep_uri hasco:hasActuatorStem ?uri .  " +
                "   FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " +
                "} " +
                "ORDER BY DESC(?datetime) ";

        return findByQuery(queryString);
    }

    private static List<ActuatorStem> findByQuery(String queryString) {
        List<ActuatorStem> actuatorStems = new ArrayList<ActuatorStem>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            ActuatorStem actuatorStem = find(soln.getResource("uri").getURI());
            actuatorStems.add(actuatorStem);
        }

        java.util.Collections.sort((List<ActuatorStem>) actuatorStems);
        return actuatorStems;

    }

    public static ActuatorStem find(String uri) {
        ActuatorStem actuatorStem = null;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

        actuatorStem = new ActuatorStem(VSTOI.DETECTOR_STEM);

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
 			String str = URIUtils.objectRDFToString(object);
			if (uri != null && !uri.isEmpty()) {
				if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
					actuatorStem.setLabel(str);
				} else if (statement.getPredicate().getURI().equals(RDFS.SUBCLASS_OF)) {
					actuatorStem.setSuperUri(str); 
				} else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
					actuatorStem.setHascoTypeUri(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_IMAGE)) {
					actuatorStem.setHasImageUri(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_WEB_DOCUMENT)) {
					actuatorStem.setHasWebDocument(str);
                } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                    actuatorStem.setComment(str);
                } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                    actuatorStem.setHascoTypeUri(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
                    actuatorStem.setHasStatus(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_CONTENT)) {
                    actuatorStem.setHasContent(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
                    actuatorStem.setHasLanguage(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
                    actuatorStem.setHasVersion(str);
                } else if (statement.getPredicate().getURI().equals(HASCO.ACTIVATES)) {
                    try {
                        actuatorStem.setActivates(str);
                    } catch (Exception e) {
                    }
                } else if (statement.getPredicate().getURI().equals(PROV.WAS_DERIVED_FROM)) {
                    try {
                        actuatorStem.setWasDerivedFrom(str);
                    } catch (Exception e) {
                    }
                } else if (statement.getPredicate().getURI().equals(PROV.WAS_GENERATED_BY)) {
                    try {
                        actuatorStem.setWasGeneratedBy(str);
                    } catch (Exception e) {
                    }
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_REVIEW_NOTE)) {
					actuatorStem.setHasReviewNote(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					actuatorStem.setHasSIRManagerEmail(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_EDITOR_EMAIL)) {
					actuatorStem.setHasEditorEmail(str);
                } 
            }
        }

        actuatorStem.setUri(uri);

        return actuatorStem;
    }

    public static List<ActuatorStem> derivation(String actuatorStemuri) {
        if (actuatorStemuri == null || actuatorStemuri.isEmpty()) {
            return null;
        }
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:ActuatorStem . " +
                " ?uri a ?model ." +
                " ?uri prov:wasDerivedFrom <" + actuatorStemuri + "> . " +
                " ?uri vstoi:hasContent ?content . " +
                "} " +
                "ORDER BY ASC(?content) ";

        //System.out.println("Query: " + queryString);

        return findByQuery(queryString);
    }

    @Override
    public int compareTo(ActuatorStem another) {
        return this.getLabel().compareTo(another.getLabel());
    }

    @Override public void save() {
        saveToTripleStore();
    }

    @Override public void delete() {
        deleteFromTripleStore();
    }

}

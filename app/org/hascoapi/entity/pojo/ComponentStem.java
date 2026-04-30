package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.*;
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

@JsonFilter("componentStemFilter")
public class ComponentStem extends HADatAcClass implements SIRElement, Comparable<ComponentStem>  {

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

    @PropertyField(uri="hasco:isAssociatedWith")
    private String isAssociatedWith;

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

    public String getIsAssociatedWith() {
        return isAssociatedWith;
    }

    public SemanticVariable getIsAssociatedWithSemanticVariable() {
        if (isAssociatedWith == null) {
            return null;
        }
        return SemanticVariable.find(isAssociatedWith);
    }

    public void setIsAssociatedWith(String isAssociatedWith) {
        this.isAssociatedWith = isAssociatedWith;
    }

    public ComponentStem () {
    }

    public ComponentStem (String className) {
		super(className);
    }

    public static List<ComponentStem> findByInstrument(String instrumentUri) {
        //System.out.println("findByInstrument: [" + instrumentUri + "]");
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:ComponentStem . " +
                " ?uri a ?model ." +
                " ?attUri vstoi:hasComponentStem ?uri . " +
                " ?attUri vstoi:belongsTo <" + instrumentUri + ">. " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<ComponentStem> findAvailable() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                "   { ?model rdfs:subClassOf* vstoi:ComponentStem . " +
                "     ?uri a ?model ." +
                "   } MINUS { " +
                "     ?dep_uri a vstoi:Deployment . " +
                "     ?dep_uri hasco:hasComponentStem ?uri .  " +
                "     FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " +
                "    } " +
                "} " +
                "ORDER BY DESC(?datetime) ";

        return findByQuery(queryString);
    }

    public static List<ComponentStem> findDeployed() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                "   ?model rdfs:subClassOf* vstoi:ComponentStem . " +
                "   ?uri a ?model ." +
                "   ?dep_uri a vstoi:Deployment . " +
                "   ?dep_uri hasco:hasComponentStem ?uri .  " +
                "   FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " +
                "} " +
                "ORDER BY DESC(?datetime) ";

        return findByQuery(queryString);
    }

    private static List<ComponentStem> findByQuery(String queryString) {
        List<ComponentStem> ComponentStems = new ArrayList<ComponentStem>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            ComponentStem ComponentStem = find(soln.getResource("uri").getURI());
            ComponentStems.add(ComponentStem);
        }

        java.util.Collections.sort((List<ComponentStem>) ComponentStems);
        return ComponentStems;

    }

    public static ComponentStem find(String uri) {
        System.out.println("[DEBUG-CSTEM-FIND] ComponentStem.find() called with URI: " + uri);
		if (uri == null || uri.isEmpty()) {
            System.out.println("[DEBUG-CSTEM-FIND] URI is null or empty, returning null");
			return null;
		}
		ComponentStem ComponentStem = null;
		// Construct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
        System.out.println("[DEBUG-CSTEM-FIND] Executing SPARQL query: " + queryString);
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
            System.out.println("[DEBUG-CSTEM-FIND] No results found for URI: " + uri);
			return null;
		} else {
            System.out.println("[DEBUG-CSTEM-FIND] Results found! Creating new ComponentStem instance");
            ComponentStem = new ComponentStem(VSTOI.COMPONENT_STEM);
		}

		// Iterate over results
        int propertyCount = 0;
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				ComponentStem.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);
                propertyCount++;

				if (predicate.equals(RDFS.LABEL)) {
					ComponentStem.setLabel(object);
				} else if (predicate.equals(RDFS.SUBCLASS_OF)) {
					ComponentStem.setSuperUri(object); 
				} else if (predicate.equals(HASCO.HASCO_TYPE)) {
					ComponentStem.setHascoTypeUri(object);
				} else if (predicate.equals(HASCO.HAS_IMAGE)) {
					ComponentStem.setHasImageUri(object);
				} else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
					ComponentStem.setHasWebDocument(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    ComponentStem.setComment(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    ComponentStem.setHascoTypeUri(object);
                } else if (predicate.equals(VSTOI.HAS_STATUS)) {
                    ComponentStem.setHasStatus(object);
                } else if (predicate.equals(VSTOI.HAS_CONTENT)) {
                    ComponentStem.setHasContent(object);
                } else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
                    ComponentStem.setHasLanguage(object);
                } else if (predicate.equals(VSTOI.HAS_VERSION)) {
                    ComponentStem.setHasVersion(object);
                } else if (predicate.equals(HASCO.IS_ASSOCIATED_WITH)) {
                    try {
                        ComponentStem.setIsAssociatedWith(object);
                    } catch (Exception e) {
                    }
                } else if (predicate.equals(PROV.WAS_DERIVED_FROM)) {
                    try {
                        ComponentStem.setWasDerivedFrom(object);
                    } catch (Exception e) {
                    }
                } else if (predicate.equals(PROV.WAS_GENERATED_BY)) {
                    try {
                        ComponentStem.setWasGeneratedBy(object);
                    } catch (Exception e) {
                    }
				} else if (predicate.equals(VSTOI.HAS_REVIEW_NOTE)) {
					ComponentStem.setHasReviewNote(object);
				} else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					ComponentStem.setHasSIRManagerEmail(object);
				} else if (predicate.equals(VSTOI.HAS_EDITOR_EMAIL)) {
					ComponentStem.setHasEditorEmail(object);
                } 
            }
        }

        System.out.println("[DEBUG-CSTEM-FIND] Loaded " + propertyCount + " properties for ComponentStem");
        ComponentStem.setUri(uri);
        System.out.println("[DEBUG-CSTEM-FIND] Returning ComponentStem with URI: " + ComponentStem.getUri() + ", Label: " + ComponentStem.getLabel());

        return ComponentStem;
    }

    public static List<ComponentStem> derivation(String ComponentStemuri) {
        if (ComponentStemuri == null || ComponentStemuri.isEmpty()) {
            return null;
        }
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:ComponentStem . " +
                " ?uri a ?model ." +
                " ?uri prov:wasDerivedFrom <" + ComponentStemuri + "> . " +
                " ?uri vstoi:hasContent ?content . " +
                "} " +
                "ORDER BY ASC(?content) ";

        //System.out.println("Query: " + queryString);

        return findByQuery(queryString);
    }

    @Override
    public int compareTo(ComponentStem another) {
        return this.getLabel().compareTo(another.getLabel());
    }

    @Override public void save() {
        saveToTripleStore();
    }

    @Override public void delete() {
        deleteFromTripleStore();
    }

}

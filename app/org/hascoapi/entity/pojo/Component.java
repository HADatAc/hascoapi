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
import org.hascoapi.console.controllers.restapi.URIPage;
import org.hascoapi.entity.pojo.Component;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.vocabularies.*;

@JsonFilter("componentFilter")
public class Component extends HADatAcThing implements SIRElement  {

    @PropertyField(uri="vstoi:hasCodebook")
    private String hasCodebook;

    @PropertyField(uri="vstoi:isAttributeOf")
    private String isAttributeOf;

    @PropertyField(uri = "vstoi:hasStatus")    
    private String hasStatus;

    @PropertyField(uri = "vstoi:hasContent")
    String hasContent;

    @PropertyField(uri = "vstoi:hasComponentStem")
    String hasComponentStem;

    @PropertyField(uri = "vstoi:hasLanguage")
    private String hasLanguage;

    @PropertyField(uri = "vstoi:hasVersion")
    String hasVersion;

    @PropertyField(uri = "vstoi:hasReviewNote")
    String hasReviewNote;

    @PropertyField(uri="prov:wasDerivedFrom")
    private String wasDerivedFrom;

    @PropertyField(uri="prov:wasGeneratedBy")
    private String wasGeneratedBy;

    @PropertyField(uri = "vstoi:hasMaker")
    private String hasMakerUri;

    @PropertyField(uri = "vstoi:hasSIRManagerEmail")
    private String hasSIRManagerEmail;

    @PropertyField(uri = "vstoi:hasEditorEmail")
    private String hasEditorEmail;

    public String getHasCodebook() {
        return hasCodebook;
    }

    public void setHasCodebook(String hasCodebook) {
        this.hasCodebook = hasCodebook;
    }

    public Codebook getCodebook() {
        if (hasCodebook == null || hasCodebook.equals("")) {
            return null;
        }
        return Codebook.find(hasCodebook);
    }

    public void setIsAttributeOf(String isAttributeOf) {
        this.isAttributeOf = isAttributeOf;
    }

    public String getIsAttributeOf() {
        return isAttributeOf;
    }

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

    public String getHasComponentStem() {
        return hasComponentStem;
    }

    public ComponentStem getComponentStem() {
        if (hasComponentStem == null || hasComponentStem.isEmpty()) {
            return null;
        }
        return ComponentStem.find(hasComponentStem);
    }

    public void setHasComponentStem(String hasComponentStem) {
        this.hasComponentStem = hasComponentStem;
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

    public String getHasReviewNote() {      
        return hasReviewNote;
    }

    public void setHasReviewNote(String hasReviewNote) {
        this.hasReviewNote = hasReviewNote;
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

    public String getHasMakerUri() {
        return hasMakerUri;
    }

    public void setHasMakerUri(String hasMakerUri) {
        this.hasMakerUri = hasMakerUri;
    }

    public Organization getHasMaker() {
        if (hasMakerUri == null || hasMakerUri.isEmpty()) {
			return null;
		}
		return Organization.find(hasMakerUri);
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

    public static boolean attach(ContainerSlot containerSlot, Component component) {
        //System.out.println("called Component.attach()");
        if (containerSlot == null) {
            System.out.println("A valid container slot is required to attach a component");
            return false;
        }
        return containerSlot.updateContainerSlotComponent(component);
    }

    public static boolean detach(ContainerSlot containerSlot) {
        //System.out.println("called Component.detach()");
        if (containerSlot == null) {
            return false;
        }
        return containerSlot.updateContainerSlotComponent(null);
    }

    public static List<Component> findComponents() {
        List<Component> components = new ArrayList<Component>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:Component . " +
                " ?uri a ?model ." +
                " ?uri vstoi:hasComponentStem ?stem . " +
                " ?stem vstoi:hasContent ?content . " +
                "} " +
                " ORDER BY ASC(?content) ";

        //System.out.println("Query: " + queryString);

        return findComponentsByQuery(queryString);
    }

    /*
    public static int getNumberComponents() {
        String queryString = "";
        queryString += NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " select (count(?uri) as ?tot) where { " +
                " ?detModel rdfs:subClassOf* vstoi:Component . " +
                " ?uri a ?detModel ." +
                "}";

        return findTotalComponentsByQuery(queryString);
    }*/

    public static List<Component> findComponentsWithPages(int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:Component . " +
                " ?uri a ?model . } " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        return findComponentsByQuery(queryString);
    }

    public static List<Component> findComponentsByLanguage(String language) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:Component . " +
                " ?uri a ?model ." +
                " ?uri vstoi:hasComponentStem ?stem . " +
                " ?stem vstoi:hasLanguage ?language . " +
                "   FILTER (?language = \"" + language + "\") " +
                "} ";

        return findComponentsByQuery(queryString);
    }

    public static List<Component> findComponentsByKeyword(String keyword) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:Component . " +
                " ?uri a ?model ." +
                " ?uri vstoi:hasComponentStem ?stem . " +
                " ?stem vstoi:hasContent ?content . " +
                "   FILTER regex(?content, \"" + keyword + "\", \"i\") " +
                "} ";

        return findComponentsByQuery(queryString);
    }

    public static List<Component> findComponentsByKeywordAndLanguageWithPages(String keyword, String language, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:Component . " +
                " ?uri a ?model ." + 
                " ?uri vstoi:hasComponentStem ?stem . ";
        if (!language.isEmpty()) {
            queryString += " ?stem vstoi:hasLanguage ?language . ";
        }
        queryString += " ?stem vstoi:hasContent ?content . ";
        if (!keyword.isEmpty() && !language.isEmpty()) {
            queryString += "   FILTER (regex(?content, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) ";
        } else if (!keyword.isEmpty()) {
            queryString += "   FILTER (regex(?content, \"" + keyword + "\", \"i\")) ";
        } else if (!language.isEmpty()) {
            queryString += "   FILTER ((?language = \"" + language + "\")) ";
        }
        queryString += "} " +
                " ORDER BY ASC(?content) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        return findComponentsByQuery(queryString);
    }

    public static List<Component> findComponentsByManagerEmailWithPages(String managerEmail, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Component . " +
                " ?uri a ?detModel ." +
                " ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                " ?uri vstoi:hasComponentStem ?stem . " +
                " ?stem vstoi:hasContent ?content . " +
                "   FILTER (?managerEmail = \"" + managerEmail + "\") " +
                "} " +
                " ORDER BY ASC(?content) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        return findComponentsByQuery(queryString);
    }

    public static List<Component> findComponentsByManagerEmail(String managerEmail) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Component . " +
                " ?uri a ?detModel ." +
                " ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                " ?uri vstoi:hasComponentStem ?stem . " +
                " ?stem vstoi:hasContent ?content . " +
                "   FILTER (?managerEmail = \"" + managerEmail + "\") " +
                "} " +
                " ORDER BY ASC(?content) ";

        return findComponentsByQuery(queryString);
    }

    public static List<Component> findComponentsByContainer(String containerUri) {
        //System.out.println("findByContainer: [" + containerUri + "]");
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:Component . " +
                " ?uri a ?model ." +
                " ?detSlotUri vstoi:hasComponent ?uri . " +
                " ?detSlotUri vstoi:belongsTo <" + containerUri + ">. " +
                "} ";

        return findComponentsByQuery(queryString);
    }

    public static List<Component> findComponentsDeployed() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                "   ?model rdfs:subClassOf* vstoi:Component . " +
                "   ?uri a ?model ." +
                "   ?dep_uri a vstoi:Deployment . " +
                "   ?dep_uri hasco:hasComponent ?uri .  " +
                "   FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " +
                "} " +
                "ORDER BY DESC(?datetime) ";

        return findComponentsByQuery(queryString);
    }

    private static List<Component> findComponentsByQuery(String queryString) {
        List<Component> components = new ArrayList<Component>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            Component component = find(soln.getResource("uri").getURI());
            components.add(component);
        }

        //java.util.Collections.sort((List<Component>) components);
        return components;

    }

    public static Component find(String uri) {
 		if (uri == null || uri.isEmpty()) {
			return null;
		}
		Component component = null;
		// Construct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
            component = new Component();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				component.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);

                if (predicate.equals(RDFS.LABEL)) {
                    component.setLabel(object);
                } else if (predicate.equals(RDF.TYPE)) {
                    component.setTypeUri(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    component.setComment(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    component.setHascoTypeUri(object);
				} else if (predicate.equals(HASCO.HAS_IMAGE)) {
					component.setHasImageUri(object);
				} else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
					component.setHasWebDocument(object);
                } else if (predicate.equals(VSTOI.HAS_STATUS)) {
                    component.setHasStatus(object);
                } else if (predicate.equals(VSTOI.HAS_CONTENT)) {
                    component.setHasContent(object);
                } else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
                    component.setHasLanguage(object);
                } else if (predicate.equals(VSTOI.HAS_VERSION)) {
                    component.setHasVersion(object);
                } else if (predicate.equals(PROV.WAS_DERIVED_FROM)) {
                    try {
                        component.setWasDerivedFrom(object);
                    } catch (Exception e) {
                    }
                } else if (predicate.equals(PROV.WAS_GENERATED_BY)) {
                    try {
                        component.setWasGeneratedBy(object);
                    } catch (Exception e) {
                    }
				} else if (predicate.equals(VSTOI.HAS_REVIEW_NOTE)) {
					component.setHasReviewNote(object);
				} else if (predicate.equals(VSTOI.HAS_MAKER)) {
					component.setHasMakerUri(object);
				} else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					component.setHasSIRManagerEmail(object);
				} else if (predicate.equals(VSTOI.HAS_EDITOR_EMAIL)) {
				    component.setHasEditorEmail(object);
                } else if (predicate.equals(VSTOI.HAS_COMPONENT_STEM)) {
                    try {
                        component.setHasComponentStem(object);
                    } catch (Exception e) {
                        component.setHasComponentStem(null);
                    }
                } else if (predicate.equals(VSTOI.HAS_CODEBOOK)) {
                    try {
                        component.setHasCodebook(object);
                    } catch (Exception e) {
                        component.setHasCodebook(null);
                    }
                } else if (predicate.equals(VSTOI.IS_ATTRIBUTE_OF)) {
                    try {
                        component.setIsAttributeOf(object);
                    } catch (Exception e) {
                        component.setIsAttributeOf(null);
                    }
                }
            }
        }

        component.setUri(uri);

        return component;
    }

    public static List<ContainerSlot> usage(String componenturi) {
        if (componenturi == null || componenturi.isEmpty()) {
            return null;
        }
        List<ContainerSlot> containerSlots = new ArrayList<ContainerSlot>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?detSlotUri WHERE { " +
                " ?detSlotModel rdfs:subClassOf* vstoi:ContainerSlot . " +
                " ?detSlotUri a ?detSlotModel ." +
                " ?detSlotUri vstoi:hasComponent <" + componenturi + "> . " +
                " ?detSlotUri vstoi:belongsTo ?instUri . " +
                " ?instUri rdfs:label ?instLabel . " +
                "} " +
                "ORDER BY ASC(?instLabel) ";

        //System.out.println("Query: " + queryString);

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            //System.out.println("inside Component.usage(): found uri [" + soln.getResource("uri").getURI().toString() + "]");
            ContainerSlot containerSlot = ContainerSlot.find(soln.getResource("detSlotUri").getURI());
            containerSlots.add(containerSlot);
        }
        return containerSlots;
    }

    public static List<Component> derivationComponent(String componenturi) {
        if (componenturi == null || componenturi.isEmpty()) {
            return null;
        }
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Component . " +
                " ?uri a ?detModel ." +
                " ?uri prov:wasDerivedFrom <" + componenturi + "> . " +
                " ?uri vstoi:hasContent ?content . " +
                "} " +
                "ORDER BY ASC(?content) ";

        //System.out.println("Query: " + queryString);

        return findComponentsByQuery(queryString);
    }

    @Override public void save() {
        saveToTripleStore();
    }

    @Override public void delete() {
        deleteFromTripleStore();
    }

}

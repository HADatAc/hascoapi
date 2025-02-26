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

@JsonFilter("actuatorFilter")
public class Actuator extends Component {

    @PropertyField(uri="vstoi:hasActuatorStem")
    private String hasActuatorStem;

    public String getHasActuatorStem() {
        return hasActuatorStem;
    }

    public ActuatorStem getActuatorStem() {
        if (hasActuatorStem == null || hasActuatorStem.equals("")) {
            return null;
        }
        ActuatorStem actuatorStem = ActuatorStem.find(hasActuatorStem);
        return actuatorStem;
    }

    public void setHasActuatorStem(String hasActuatorStem) {
        this.hasActuatorStem = hasActuatorStem;
    }

    public static List<Actuator> findActuators() {
        List<Actuator> actuators = new ArrayList<Actuator>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:Actuator . " +
                " ?uri a ?model ." +
                " ?uri vstoi:hasActuatorStem ?stem . " +
                " ?stem vstoi:hasContent ?content . " +
                "} " +
                " ORDER BY ASC(?content) ";

        //System.out.println("Query: " + queryString);

        return findActuatorsByQuery(queryString);
    }

    public static List<Actuator> findActuatorsWithPages(int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:Actuator . " +
                " ?uri a ?model . } " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        return findActuatorsByQuery(queryString);
    }

    public static List<Actuator> findActuatorsByLanguage(String language) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:Actuator . " +
                " ?uri a ?model ." +
                " ?uri vstoi:hasActuatorStem ?stem . " +
                " ?stem vstoi:hasLanguage ?language . " +
                "   FILTER (?language = \"" + language + "\") " +
                "} ";

        return findActuatorsByQuery(queryString);
    }

    public static List<Actuator> findActuatorsByKeyword(String keyword) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:Actuator . " +
                " ?uri a ?model ." +
                " ?uri vstoi:hasActuatorStem ?stem . " +
                " ?stem vstoi:hasContent ?content . " +
                "   FILTER regex(?content, \"" + keyword + "\", \"i\") " +
                "} ";

        return findActuatorsByQuery(queryString);
    }

    public static List<Actuator> findActuatorsByKeywordAndLanguageWithPages(String keyword, String language, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:Actuator . " +
                " ?uri a ?model ." + 
                " ?uri vstoi:hasActuatorStem ?stem . ";
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

        return findActuatorsByQuery(queryString);
    }

    public static List<Actuator> findActuatorsByManagerEmailWithPages(String managerEmail, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:Actuator . " +
                " ?uri a ?model ." +
                " ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                " ?uri vstoi:hasActuatorStem ?stem . " +
                " ?stem vstoi:hasContent ?content . " +
                "   FILTER (?managerEmail = \"" + managerEmail + "\") " +
                "} " +
                " ORDER BY ASC(?content) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        return findActuatorsByQuery(queryString);
    }

    public static List<Actuator> findActuatorsByManagerEmail(String managerEmail) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:Actuator . " +
                " ?uri a ?model ." +
                " ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                " ?uri vstoi:hasActuatorStem ?stem . " +
                " ?stem vstoi:hasContent ?content . " +
                "   FILTER (?managerEmail = \"" + managerEmail + "\") " +
                "} " +
                " ORDER BY ASC(?content) ";

        return findActuatorsByQuery(queryString);
    }

    public static List<Actuator> findActuatorsByContainer(String containerUri) {
        //System.out.println("findByContainer: [" + containerUri + "]");
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:Actuator . " +
                " ?uri a ?model ." +
                " ?detSlotUri vstoi:hasActuator ?uri . " +
                " ?detSlotUri vstoi:belongsTo <" + containerUri + ">. " +
                "} ";

        return findActuatorsByQuery(queryString);
    }

    public static List<Actuator> findActuatorsDeployed() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                "   ?model rdfs:subClassOf* vstoi:Actuator . " +
                "   ?uri a ?model ." +
                "   ?dep_uri a vstoi:Deployment . " +
                "   ?dep_uri hasco:hasActuator ?uri .  " +
                "   FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " +
                "} " +
                "ORDER BY DESC(?datetime) ";

        return findActuatorsByQuery(queryString);
    }

    private static List<Actuator> findActuatorsByQuery(String queryString) {
        List<Actuator> actuators = new ArrayList<Actuator>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            Actuator actuator = find(soln.getResource("uri").getURI());
            actuators.add(actuator);
        }

        //java.util.Collections.sort((List<Actuator>) actuators);
        return actuators;

    }

    public static Actuator find(String uri) {
        Actuator actuator = null;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

        //actuator = new Actuator(VSTOI.ACTUATOR);
        actuator = new Actuator();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
 			String str = URIUtils.objectRDFToString(object);
			if (uri != null && !uri.isEmpty()) {
                if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                    actuator.setLabel(str);
                } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                    actuator.setTypeUri(str);
                } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                    actuator.setComment(str);
                } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                    actuator.setHascoTypeUri(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_IMAGE)) {
					actuator.setHasImageUri(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_WEB_DOCUMENT)) {
					actuator.setHasWebDocument(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
                    actuator.setHasStatus(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_CONTENT)) {
                    actuator.setHasContent(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
                    actuator.setHasLanguage(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
                    actuator.setHasVersion(str);
                } else if (statement.getPredicate().getURI().equals(PROV.WAS_DERIVED_FROM)) {
                    try {
                        actuator.setWasDerivedFrom(str);
                    } catch (Exception e) {
                    }
                } else if (statement.getPredicate().getURI().equals(PROV.WAS_GENERATED_BY)) {
                    try {
                        actuator.setWasGeneratedBy(str);
                    } catch (Exception e) {
                    }
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_REVIEW_NOTE)) {
					actuator.setHasReviewNote(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					actuator.setHasSIRManagerEmail(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_EDITOR_EMAIL)) {
				    actuator.setHasEditorEmail(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_ACTUATOR_STEM)) {
                    try {
                        actuator.setHasActuatorStem(str);
                    } catch (Exception e) {
                        actuator.setHasActuatorStem(null);
                    }
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_CODEBOOK)) {
                    try {
                        actuator.setHasCodebook(str);
                    } catch (Exception e) {
                        actuator.setHasCodebook(null);
                    }
                } else if (statement.getPredicate().getURI().equals(VSTOI.IS_ATTRIBUTE_OF)) {
                    try {
                        actuator.setIsAttributeOf(str);
                    } catch (Exception e) {
                        actuator.setIsAttributeOf(null);
                    }
                }
            }
        }

        actuator.setUri(uri);

        return actuator;
    }

    public static List<ContainerSlot> usage(String actuatoruri) {
        if (actuatoruri == null || actuatoruri.isEmpty()) {
            return null;
        }
        List<ContainerSlot> containerSlots = new ArrayList<ContainerSlot>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?detSlotUri WHERE { " +
                " ?detSlotModel rdfs:subClassOf* vstoi:ContainerSlot . " +
                " ?detSlotUri a ?detSlotModel ." +
                " ?detSlotUri vstoi:hasActuator <" + actuatoruri + "> . " +
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
            //System.out.println("inside Actuator.usage(): found uri [" + soln.getResource("uri").getURI().toString() + "]");
            ContainerSlot containerSlot = ContainerSlot.find(soln.getResource("detSlotUri").getURI());
            containerSlots.add(containerSlot);
        }
        return containerSlots;
    }

    public static List<Actuator> derivationActuator(String actuatoruri) {
        if (actuatoruri == null || actuatoruri.isEmpty()) {
            return null;
        }
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* vstoi:Actuator . " +
                " ?uri a ?model ." +
                " ?uri prov:wasDerivedFrom <" + actuatoruri + "> . " +
                " ?uri vstoi:hasContent ?content . " +
                "} " +
                "ORDER BY ASC(?content) ";

        //System.out.println("Query: " + queryString);

        return findActuatorsByQuery(queryString);
    }

    @Override public void save() {
        saveToTripleStore();
    }

    @Override public void delete() {
        deleteFromTripleStore();
    }

}

package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
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
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;

import java.util.ArrayList;
import java.util.List;

@JsonFilter("containerSlotFilter")
public class ContainerSlot extends HADatAcThing implements SlotElement, Comparable<ContainerSlot>  {

    @PropertyField(uri="vstoi:belongsTo")
    private String belongsTo;

    @PropertyField(uri="vstoi:hasDetector")
    private String hasDetector;

    @PropertyField(uri="vstoi:hasNext")
    private String hasNext;

    @PropertyField(uri="vstoi:hasPrevious")
    private String hasPrevious;

    @PropertyField(uri="vstoi:hasPriority")
    private String hasPriority;

    public String getBelongsTo() {
        return belongsTo;
    }

    public void setBelongsTo(String belongsTo) {
        this.belongsTo = belongsTo;
    }

    public String getHasDetector() {
        return hasDetector;
    }

    public void setHasDetector(String hasDetector) {
        this.hasDetector = hasDetector;
    }

    public String getHasNext() {
        return hasNext;
    }

    public void setHasNext(String hasNext) {
        this.hasNext = hasNext;
    }

    public String getHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(String hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public String getHasPriority() {
        return hasPriority;
    }

    public void setHasPriority(String hasPriority) {
        this.hasPriority = hasPriority;
    }

    public Detector getDetector() {
        if (hasDetector == null || hasDetector.isEmpty()) {
            return null;
        }
        return Detector.findDetector(hasDetector);
    }

    public static int getNumberContainerSlots() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?attModel rdfs:subClassOf* vstoi:ContainerSlot . " +
                " ?uri a ?attModel ." +
                "}";

        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

            if (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                return Integer.parseInt(soln.getLiteral("tot").getString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int getNumberContainerSlotsWithDetectors() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?attModel rdfs:subClassOf* vstoi:ContainerSlot . " +
                " ?uri a ?attModel ." +
                " ?uri vstoi:hasDetector ?detector . " +
                "}";

        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

            if (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                return Integer.parseInt(soln.getLiteral("tot").getString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int getNumberContainerSlotsByContainer(String containerUri) {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?type rdfs:subClassOf* vstoi:ContainerSlot . " +
                " ?uri a ?type ." +
                " ?uri vstoi:belongsTo <" + containerUri + ">. " +
                "}";

        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

            if (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                return Integer.parseInt(soln.getLiteral("tot").getString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<ContainerSlot> findByContainerWithPages(String containerUri, int pageSize, int offset) {
        List<ContainerSlot> containerSlots = new ArrayList<ContainerSlot>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* vstoi:ContainerSlot . " +
                " ?uri a ?type . } " +
                " ?uri vstoi:belongsTo <" + containerUri + ">. " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI() != null) {
                ContainerSlot containerSlot = ContainerSlot.find(soln.getResource("uri").getURI());
                containerSlots.add(containerSlot);
            }
        }
        return containerSlots;
    }

    public static List<ContainerSlot> find() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* vstoi:ContainerSlot . " +
                " ?uri a ?type ." +
                "} ";

        return findContainerSlotByQuery(queryString);
    }

    private static List<ContainerSlot> findContainerSlotByQuery(String queryString) {
        List<ContainerSlot> containerSlots = new ArrayList<ContainerSlot>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            ContainerSlot containerSlot = ContainerSlot.find(soln.getResource("uri").getURI());
            containerSlots.add(containerSlot);
        }

        //java.util.Collections.sort((List<ContainerSlot>) containerSlots);
        return containerSlots;

    }

    public static ContainerSlot find(String uri) {
        ContainerSlot containerSlot = null;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

        containerSlot = new ContainerSlot();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            String str = URIUtils.objectRDFToString(object);
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                containerSlot.setLabel(str);
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                containerSlot.setTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                containerSlot.setComment(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_NEXT)) {
                containerSlot.setHasNext(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_PREVIOUS)) {
                containerSlot.setHasPrevious(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                containerSlot.setHascoTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.BELONGS_TO)) {
                containerSlot.setBelongsTo(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_DETECTOR)) {
                containerSlot.setHasDetector(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_PRIORITY)){
                containerSlot.setHasPriority(str);
            }
        }

        containerSlot.setUri(uri);

        if (containerSlot.getHascoTypeUri().equals(VSTOI.CONTAINER_SLOT)) {
            return containerSlot;
        } 
        return null;
    }

    static public boolean createContainerSlot(String containerUri, String containerSlotUri, 
                            String containerSlotUriNext, String containerSlotUriPrevious,  
                            String priority, String hasDetector) {
        if (containerUri == null || containerUri.isEmpty()) {
            return false;
        }
        if (priority == null || priority.isEmpty()) {
            return false;
        }
        ContainerSlot containerSlot = new ContainerSlot();
        containerSlot.setUri(containerSlotUri);
        containerSlot.setLabel("ContainerSlot " + priority);
        containerSlot.setTypeUri(VSTOI.CONTAINER_SLOT);
        containerSlot.setHascoTypeUri(VSTOI.CONTAINER_SLOT);
        containerSlot.setComment("ContainerSlot " + priority + " of container with URI " + containerUri);
        containerSlot.setBelongsTo(containerUri);
        containerSlot.setHasPriority(priority);
        if (containerSlotUriNext != null && !containerSlotUriNext.isEmpty()) {
            containerSlot.setHasNext(containerSlotUriNext);
        }
        if (containerSlotUriPrevious != null && !containerSlotUriPrevious.isEmpty()) {
            containerSlot.setHasPrevious(containerSlotUriPrevious);
        }
        if (hasDetector != null) {
            containerSlot.setHasDetector(hasDetector);
        }
        containerSlot.save();
        //System.out.println("ContainerSlot.createContainerSlot: creating containerSlot with URI [" + containerSlotUri + "]" );
        return true;
    }

    public boolean updateContainerSlotDetector(Detector detector) {
        ContainerSlot newContainerSlot = new ContainerSlot();
        newContainerSlot.setUri(this.uri);
        newContainerSlot.setLabel(this.getLabel());
        newContainerSlot.setTypeUri(this.getTypeUri());
        newContainerSlot.setComment(this.getComment());
        newContainerSlot.setHascoTypeUri(this.getHascoTypeUri());
        newContainerSlot.setBelongsTo(this.getBelongsTo());
        newContainerSlot.setHasPriority(this.getHasPriority());
        newContainerSlot.setHasNext(this.getHasNext());
        newContainerSlot.setHasPrevious(this.getHasPrevious());
        if (detector != null && detector.getUri() != null && !detector.getUri().isEmpty()) {
            newContainerSlot.setHasDetector(detector.getUri());
        } else {
            newContainerSlot.setHasDetector(null);
        }
        this.delete();
        newContainerSlot.save();
        return true;
    }

    @Override
    public int compareTo(ContainerSlot another) {
        return this.getHasPriority().compareTo(another.getHasPriority());
    }

    @Override public void save() {
        saveToTripleStore();
    }

    @Override public void delete() {
        deleteFromTripleStore();
    }

    @Override
    public boolean saveToSolr() {
        return false;
    }

    @Override
    public int deleteFromSolr() {
        return 0;
    }

}

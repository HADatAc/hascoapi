package org.sirapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.sirapi.annotations.PropertyField;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.NameSpaces;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.RDF;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.VSTOI;

import java.util.ArrayList;
import java.util.List;

@JsonFilter("detectorSlotFilter")
public class DetectorSlot extends HADatAcThing implements Comparable<DetectorSlot>  {

    @PropertyField(uri="vstoi:belongsTo")
    private String belongsTo;

    @PropertyField(uri="vstoi:hasDetector")
    private String hasDetector;

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

    public static int getNumberDetectorSlots() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?attModel rdfs:subClassOf* vstoi:DetectorSlot . " +
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

    public static int getNumberDetectorSlotsWithDetectors() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?attModel rdfs:subClassOf* vstoi:DetectorSlot . " +
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

    public static int getNumberDetectorSlotsByContainer(String containerUri) {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?attModel rdfs:subClassOf* vstoi:DetectorSlot . " +
                " ?uri a ?attModel ." +
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

    public static List<DetectorSlot> findByContainerWithPages(String containerUri, int pageSize, int offset) {
        List<DetectorSlot> detectorSlots = new ArrayList<DetectorSlot>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?attModel rdfs:subClassOf* vstoi:DetectorSlot . " +
                " ?uri a ?attModel . } " +
                " ?uri vstoi:belongsTo <" + containerUri + ">. " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI() != null) {
                DetectorSlot detectorSlot = DetectorSlot.find(soln.getResource("uri").getURI());
                detectorSlots.add(detectorSlot);
            }
        }
        return detectorSlots;
    }

    public static List<DetectorSlot> find() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?attModel rdfs:subClassOf* vstoi:DetectorSlot . " +
                " ?uri a ?attModel ." +
                "} ";

        return findByQuery(queryString);
    }

    public static List<DetectorSlot> findByContainer(String containerUri) {
        //System.out.println("findByContainer: [" + containerUri + "]");
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?attModel rdfs:subClassOf* vstoi:DetectorSlot . " +
                " ?uri a ?attModel ." +
                " ?uri vstoi:belongsTo <" + containerUri + ">. " +
                "} ";

        return findByQuery(queryString);
    }

    private static List<DetectorSlot> findByQuery(String queryString) {
        List<DetectorSlot> detectorSlots = new ArrayList<DetectorSlot>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            DetectorSlot detectorSlot = find(soln.getResource("uri").getURI());
            detectorSlots.add(detectorSlot);
        }

        java.util.Collections.sort((List<DetectorSlot>) detectorSlots);
        return detectorSlots;

    }

    public static DetectorSlot find(String uri) {
        DetectorSlot detectorSlot = null;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

        detectorSlot = new DetectorSlot();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                detectorSlot.setLabel(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                detectorSlot.setTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                detectorSlot.setComment(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                detectorSlot.setHascoTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.BELONGS_TO)) {
                detectorSlot.setBelongsTo(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_DETECTOR)) {
                detectorSlot.setHasDetector(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_PRIORITY)){
                detectorSlot.setHasPriority(object.asLiteral().getString());
            }
        }

        detectorSlot.setUri(uri);

        return detectorSlot;
    }

    static public boolean createDetectorSlot(String containerUri, String detectorSlotUri, String priority, String hasDetector) {
        if (containerUri == null || containerUri.isEmpty()) {
            return false;
        }
        if (priority == null || priority.isEmpty()) {
            return false;
        }
        DetectorSlot att = new DetectorSlot();
        att.setUri(detectorSlotUri);
        att.setLabel("DetectorSlot " + priority);
        att.setTypeUri(VSTOI.DETECTOR_SLOT);
        att.setHascoTypeUri(VSTOI.DETECTOR_SLOT);
        att.setComment("DetectorSlot " + priority + " of container with URI " + containerUri);
        att.setBelongsTo(containerUri);
        att.setHasPriority(priority);
        if (hasDetector != null) {
            att.setHasDetector(hasDetector);
        }
        att.save();
        //System.out.println("DetectorSlot.createDetectorSlot: creating detectorSlot with URI [" + detectorSlotUri + "]" );
        return true;
    }

    public boolean updateDetectorSlotDetector(Detector detector) {
        DetectorSlot newDetectorSlot = new DetectorSlot();
        newDetectorSlot.setUri(this.uri);
        newDetectorSlot.setLabel(this.getLabel());
        newDetectorSlot.setTypeUri(this.getTypeUri());
        newDetectorSlot.setComment(this.getComment());
        newDetectorSlot.setHascoTypeUri(this.getHascoTypeUri());
        newDetectorSlot.setBelongsTo(this.getBelongsTo());
        newDetectorSlot.setHasPriority(this.getHasPriority());
        if (detector != null && detector.getUri() != null && !detector.getUri().isEmpty()) {
            newDetectorSlot.setHasDetector(detector.getUri());
        } else {
            newDetectorSlot.setHasDetector(null);
        }
        this.delete();
        newDetectorSlot.save();
        return true;
    }

    @Override
    public int compareTo(DetectorSlot another) {
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

package org.sirapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.sirapi.annotations.PropertyField;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.NameSpaces;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.RDF;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.VSTOI;

@JsonFilter("detectorFilter")
public class Detector extends HADatAcThing implements Comparable<Detector>  {

    @PropertyField(uri="vstoi:hasSerialNumber")
    String serialNumber;

    @PropertyField(uri="hasco:hasImage")
    String image;

    @PropertyField(uri="vstoi:isInstrumentAttachment")
    String isInstrumentAttachment;

    @PropertyField(uri="vstoi:hasContent")
    String hasContent;

    @PropertyField(uri="vstoi:hasPriority")
    String hasPriority;

    @PropertyField(uri="vstoi:hasExperience")
    String hasExperience;

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getIsInstrumentAttachment() {
        return isInstrumentAttachment;
    }

    public void setIsInstrumentAttachment(String isInstrumentAttachment) {
        this.isInstrumentAttachment = isInstrumentAttachment;
    }

    public String getHasContent() {
        return hasContent;
    }

    public void setHasContent(String hasContent) {
        this.hasContent = hasContent;
    }

    public String getHasPriority() {
        return hasPriority;
    }

    public void setHasPriority(String hasPriority) {
        this.hasPriority = hasPriority;
    }

    public String getHasExperience() {
        return hasExperience;
    }

    public void setHasExperience(String hasExperience) {
        this.hasExperience = hasExperience;
    }

    public Experience getExperience() {
        if (hasExperience == null || hasExperience.equals("")) {
            return null;
        }
        Experience experience = Experience.find(hasExperience);
        return experience;
    }

    public String getTypeLabel() {
        DetectorType detType = DetectorType.find(getTypeUri());
        if (detType == null || detType.getLabel() == null) {
            return "";
        }
        return detType.getLabel();
    }

    public String getTypeURL() {
        DetectorType detType = DetectorType.find(getTypeUri());
        if (detType == null || detType.getLabel() == null) {
            return "";
        }
        return detType.getURL();
    }

    public static List<Detector> find() {
        List<Detector> detectors = new ArrayList<Detector>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                "} ";

        //System.out.println("Query: " + queryString);

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            //System.out.println("inside Detector.find(): found uri [" + soln.getResource("uri").getURI().toString() + "]");
            Detector detector = find(soln.getResource("uri").getURI());
            detectors.add(detector);
        }

        java.util.Collections.sort((List<Detector>) detectors);
        return detectors;
    }

    public static int getNumberDetectors() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
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

    public static List<Detector> findWithPages(int pageSize, int offset) {
        List<Detector> detectors = new ArrayList<Detector>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel . } " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI() != null) {
                Detector detector = Detector.find(soln.getResource("uri").getURI());
                detectors.add(detector);
            }
        }
        return detectors;
    }

    public static Detector find(String uri) {
        Detector detector = null;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

        detector = new Detector();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                detector.setLabel(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                detector.setTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                detector.setHascoTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SERIAL_NUMBER)) {
                detector.setSerialNumber(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_IMAGE)) {
                detector.setImage(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.IS_INSTRUMENT_ATTACHMENT)) {
                detector.setIsInstrumentAttachment(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_CONTENT)) {
                detector.setHasContent(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_PRIORITY)) {
                detector.setHasPriority(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_EXPERIENCE)) {
                detector.setHasExperience(object.asResource().getURI());
            }
        }

        detector.setUri(uri);

        return detector;
    }

    @Override
    public int compareTo(Detector another) {
        return this.getLabel().compareTo(another.getLabel());
    }

    public static List<Detector> findAvailable() {
        List<Detector> detectors = new ArrayList<Detector>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                "   { ?detModel rdfs:subClassOf+ vstoi:Detector . " +
                "     ?uri a ?detModel ." +
                "   } MINUS { " +
                "     ?dep_uri a vstoi:Deployment . " +
                "     ?dep_uri hasco:hasDetector ?uri .  " +
                "     FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " +
                "    } " +
                "} " +
                "ORDER BY DESC(?datetime) ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            Detector detector = find(soln.getResource("uri").getURI());
            detectors.add(detector);
        }

        java.util.Collections.sort((List<Detector>) detectors);
        return detectors;
    }

    public static List<Detector> findDeployed() {
        List<Detector> detectors = new ArrayList<Detector>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                "   ?detModel rdfs:subClassOf+ vstoi:Detector . " +
                "   ?uri a ?detModel ." +
                "   ?dep_uri a vstoi:Deployment . " +
                "   ?dep_uri hasco:hasDetector ?uri .  " +
                "   FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " +
                "} " +
                "ORDER BY DESC(?datetime) ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            Detector detector = find(soln.getResource("uri").getURI());
            detectors.add(detector);
        }

        java.util.Collections.sort((List<Detector>) detectors);
        return detectors;
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

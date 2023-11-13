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
import org.sirapi.vocabularies.*;

@JsonFilter("detectorFilter")
public class Detector extends DetectorStem {

    @PropertyField(uri="vstoi:hasSerialNumber")
    private String hasSerialNumber;
    
    @PropertyField(uri="vstoi:hasDetectorStem")
    private String hasDetectorStem;

    @PropertyField(uri="vstoi:hasCodebook")
    private String hasCodebook;

    public String getHasSerialNumber() {
        return hasSerialNumber;
    }

    public void setHasSerialNumber(String hasSerialNumber) {
        this.hasSerialNumber = hasSerialNumber;
    }

    public String getHasDetectorStem() {
        return hasDetectorStem;
    }

    public DetectorStem getDetectorStem() {
        if (hasDetectorStem == null || hasDetectorStem.equals("")) {
            return null;
        }
        DetectorStem detectorStem = DetectorStem.find(hasDetectorStem);
        return detectorStem;
    }

    public void setHasDetectorStem(String hasDetectorStem) {
        this.hasDetectorStem = hasDetectorStem;
    }

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
        Codebook codebook = Codebook.find(hasCodebook);
        return codebook;
    }

    public String getTypeLabel() {
        DetectorStemType detStemType = DetectorStemType.find(getTypeUri());
        if (detStemType == null || detStemType.getLabel() == null) {
            return "";
        }
        return detStemType.getLabel();
    }

    public String getTypeURL() {
        DetectorStemType detStemType = DetectorStemType.find(getTypeUri());
        if (detStemType == null || detStemType.getLabel() == null) {
            return "";
        }
        return detStemType.getURL();
    }

    public static List<Detector> findDetectors() {
        List<Detector> detectors = new ArrayList<Detector>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?uri vstoi:hasDetectorStem ?stem . " +
                " ?stem vstoi:hasContent ?content . " +
                "} " +
                " ORDER BY ASC(?content) ";

        //System.out.println("Query: " + queryString);

        return findDetectorsByQuery(queryString);
    }

    /** 
    public static int getNumberDetectors() {
        String queryString = "";
        queryString += NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " select (count(?uri) as ?tot) where { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                "}";

        return findTotalDetectorsByQuery(queryString);
    }

    public static List<Detector> findDetectorsWithPages(int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel . } " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        return findDetectorsByQuery(queryString);
    }

    public static List<Detector> findDetectorsByLanguage(String language) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?uri vstoi:hasDetectorStem ?stem . " +
                " ?stem vstoi:hasLanguage ?language . " +
                "   FILTER (?language = \"" + language + "\") " +
                "} ";

        return findDetectorsByQuery(queryString);
    }

    public static List<Detector> findDetectorsByKeyword(String keyword) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?uri vstoi:hasDetectorStem ?stem . " +
                " ?stem vstoi:hasContent ?content . " +
                "   FILTER regex(?content, \"" + keyword + "\", \"i\") " +
                "} ";

        return findDetectorsByQuery(queryString);
    }

    public static List<Detector> findDetectorsByKeywordAndLanguageWithPages(String keyword, String language, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." + 
                " ?uri vstoi:hasDetectorStem ?stem . ";
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

        return findDetectorsByQuery(queryString);
    }

    public static int findTotalDetectorsByKeywordAndLanguage(String keyword, String language) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel . " +
                " ?uri vstoi:hasDetectorStem ?stem . ";
        if (!language.isEmpty()) {
            queryString += " ?stem vstoi:hasLanguage ?language . ";
        }
        if (!keyword.isEmpty()) {
            queryString += " ?stem vstoi:hasContent ?content . ";
        }
        if (!keyword.isEmpty() && !language.isEmpty()) {
            queryString += "   FILTER (regex(?content, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) ";
        } else if (!keyword.isEmpty()) {
            queryString += "   FILTER (regex(?content, \"" + keyword + "\", \"i\")) ";
        } else if (!language.isEmpty()) {
            queryString += "   FILTER ((?language = \"" + language + "\")) ";
        }
        queryString += "}";

        return findTotalDetectorsByQuery(queryString);
    }

    public static List<Detector> findDetectorsByManagerEmailWithPages(String managerEmail, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                " ?uri vstoi:hasDetectorStem ?stem . " +
                " ?stem vstoi:hasContent ?content . " +
                "   FILTER (?managerEmail = \"" + managerEmail + "\") " +
                "} " +
                " ORDER BY ASC(?content) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        return findDetectorsByQuery(queryString);
    }

    public static int findTotalDetectorsByManagerEmail(String managerEmail) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                "   FILTER (?managerEmail = \"" + managerEmail + "\") " +
                "}";

        return findTotalDetectorsByQuery(queryString);
    }

    public static List<Detector> findDetectorsByManagerEmail(String managerEmail) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                " ?uri vstoi:hasDetectorStem ?stem . " +
                " ?stem vstoi:hasContent ?content . " +
                "   FILTER (?managerEmail = \"" + managerEmail + "\") " +
                "} " +
                " ORDER BY ASC(?content) ";

        return findDetectorsByQuery(queryString);
    }
    **/

    public static List<Detector> findDetectorsByInstrument(String instrumentUri) {
        //System.out.println("findByInstrument: [" + instrumentUri + "]");
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?detSlotUri vstoi:hasDetector ?uri . " +
                " ?detSlotUri vstoi:belongsTo <" + instrumentUri + ">. " +
                "} ";

        return findDetectorsByQuery(queryString);
    }

    public static List<Detector> findDetectorsDeployed() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                "   ?detModel rdfs:subClassOf* vstoi:Detector . " +
                "   ?uri a ?detModel ." +
                "   ?dep_uri a vstoi:Deployment . " +
                "   ?dep_uri hasco:hasDetector ?uri .  " +
                "   FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " +
                "} " +
                "ORDER BY DESC(?datetime) ";

        return findDetectorsByQuery(queryString);
    }

    private static List<Detector> findDetectorsByQuery(String queryString) {
        List<Detector> detectors = new ArrayList<Detector>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            Detector detector = findDetector(soln.getResource("uri").getURI());
            detectors.add(detector);
        }

        java.util.Collections.sort((List<Detector>) detectors);
        return detectors;

    }

    private static int findTotalDetectorsByQuery(String queryString) {
        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

            if (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                return Integer.parseInt(soln.getLiteral("tot").getString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static Detector findDetector(String uri) {
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
//            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
//                detector.setTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                detector.setComment(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                detector.setHascoTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
                detector.setHasStatus(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SERIAL_NUMBER)) {
                detector.setHasSerialNumber(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_IMAGE)) {
                detector.setImage(object.asLiteral().getString());
//            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_CONTENT)) {
//                detector.setHasContent(object.asLiteral().getString());
//            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
//                detector.setHasLanguage(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
                detector.setHasVersion(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(PROV.WAS_DERIVED_FROM)) {
                try {
                    detector.setWasDerivedFrom(object.asResource().getURI());
                } catch (Exception e) {
                }
            } else if (statement.getPredicate().getURI().equals(PROV.WAS_GENERATED_BY)) {
                try {
                    detector.setWasGeneratedBy(object.asResource().getURI());
                } catch (Exception e) {
                }
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MAINTAINER_EMAIL)) {
                detector.setHasSIRManagerEmail(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_DETECTOR_STEM)) {
                try {
                    detector.setHasDetectorStem(object.asResource().getURI());
                } catch (Exception e) {
                    detector.setHasDetectorStem(null);
                }
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_CODEBOOK)) {
                try {
                    detector.setHasCodebook(object.asResource().getURI());
                } catch (Exception e) {
                    detector.setHasCodebook(null);
                }
            }
        }

        detector.setUri(uri);

        return detector;
    }

    public static List<DetectorSlot> usage(String detectoruri) {
        if (detectoruri == null || detectoruri.isEmpty()) {
            return null;
        }
        List<DetectorSlot> detectorSlots = new ArrayList<DetectorSlot>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?detSlotUri WHERE { " +
                " ?detSlotModel rdfs:subClassOf* vstoi:DetectorSlot . " +
                " ?detSlotUri a ?detSlotModel ." +
                " ?detSlotUri vstoi:hasDetector <" + detectoruri + "> . " +
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
            //System.out.println("inside Detector.usage(): found uri [" + soln.getResource("uri").getURI().toString() + "]");
            DetectorSlot detectorSlot = DetectorSlot.find(soln.getResource("attUri").getURI());
            detectorSlots.add(detectorSlot);
        }
        return detectorSlots;
    }

    public static List<Detector> derivationDetector(String detectoruri) {
        if (detectoruri == null || detectoruri.isEmpty()) {
            return null;
        }
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?uri prov:wasDerivedFrom <" + detectoruri + "> . " +
                " ?uri vstoi:hasContent ?content . " +
                "} " +
                "ORDER BY ASC(?content) ";

        //System.out.println("Query: " + queryString);

        return findDetectorsByQuery(queryString);
    }

    public static boolean attach(String detectorSlotUri, String detectorUri) {
        if (detectorSlotUri == null || detectorSlotUri.isEmpty()) {
            return false;
        }
        DetectorSlot detectorSlot = DetectorSlot.find(detectorSlotUri);
        if (detectorSlot == null) {
            System.out.println("DetectorSlot.find returned nothing for URI [" + detectorSlotUri + "]");
            return false;
        }
        return detectorSlot.updateDetectorSlotDetector(detectorUri);
    }

    public static boolean detach(String detectorSlotUri) {
        if (detectorSlotUri == null || detectorSlotUri.isEmpty()) {
            return false;
        }
        DetectorSlot detectorSlot = DetectorSlot.find(detectorSlotUri);
        if (detectorSlot == null) {
            return false;
        }
        return detectorSlot.updateDetectorSlotDetector(null);
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

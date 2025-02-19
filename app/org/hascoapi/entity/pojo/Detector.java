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

@JsonFilter("detectorFilter")
public class Detector extends HADatAcThing implements SIRElement  {

    @PropertyField(uri="vstoi:hasDetectorStem")
    private String hasDetectorStem;

    @PropertyField(uri="vstoi:hasCodebook")
    private String hasCodebook;

    @PropertyField(uri="vstoi:isAttributeOf")
    private String isAttributeOf;

    @PropertyField(uri = "vstoi:hasStatus")    
    private String hasStatus;

    @PropertyField(uri = "vstoi:hasSerialNumber")
    String serialNumber;

    @PropertyField(uri = "vstoi:hasContent")
    String hasContent;

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

    @PropertyField(uri = "vstoi:hasSIRManagerEmail")
    private String hasSIRManagerEmail;

    @PropertyField(uri = "vstoi:hasEditorEmail")
    private String hasEditorEmail;

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

    public String getSerialNumber() {
        return serialNumber;
    }       

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
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

    //public Detector() {
    //}

    //public Detector(String className) {
	//	super(className);
    //}

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

    /*
    public static int getNumberDetectors() {
        String queryString = "";
        queryString += NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " select (count(?uri) as ?tot) where { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                "}";

        return findTotalDetectorsByQuery(queryString);
    }*/

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

    /*
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
    }*/

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

    /*
    public static int findTotalDetectorsByManagerEmail(String managerEmail) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                "   FILTER (?managerEmail = \"" + managerEmail + "\") " +
                "}";

        return findTotalDetectorsByQuery(queryString);
    }*/

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

    public static List<Detector> findDetectorsByContainer(String containerUri) {
        //System.out.println("findByContainer: [" + containerUri + "]");
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?detSlotUri vstoi:hasDetector ?uri . " +
                " ?detSlotUri vstoi:belongsTo <" + containerUri + ">. " +
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
            Detector detector = find(soln.getResource("uri").getURI());
            detectors.add(detector);
        }

        //java.util.Collections.sort((List<Detector>) detectors);
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

        //detector = new Detector(VSTOI.DETECTOR);
        detector = new Detector();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
 			String str = URIUtils.objectRDFToString(object);
			if (uri != null && !uri.isEmpty()) {
                if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                    detector.setLabel(str);
                } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                    detector.setTypeUri(str);
                } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                    detector.setComment(str);
                } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                    detector.setHascoTypeUri(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_IMAGE)) {
					detector.setHasImageUri(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_WEB_DOCUMENT)) {
					detector.setHasWebDocument(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
                    detector.setHasStatus(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_CONTENT)) {
                    detector.setHasContent(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
                    detector.setHasLanguage(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
                    detector.setHasVersion(str);
                } else if (statement.getPredicate().getURI().equals(PROV.WAS_DERIVED_FROM)) {
                    try {
                        detector.setWasDerivedFrom(str);
                    } catch (Exception e) {
                    }
                } else if (statement.getPredicate().getURI().equals(PROV.WAS_GENERATED_BY)) {
                    try {
                        detector.setWasGeneratedBy(str);
                    } catch (Exception e) {
                    }
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_REVIEW_NOTE)) {
					detector.setHasReviewNote(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					detector.setHasSIRManagerEmail(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_EDITOR_EMAIL)) {
				    detector.setHasEditorEmail(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_DETECTOR_STEM)) {
                    try {
                        detector.setHasDetectorStem(str);
                    } catch (Exception e) {
                        detector.setHasDetectorStem(null);
                    }
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_CODEBOOK)) {
                    try {
                        detector.setHasCodebook(str);
                    } catch (Exception e) {
                        detector.setHasCodebook(null);
                    }
                } else if (statement.getPredicate().getURI().equals(VSTOI.IS_ATTRIBUTE_OF)) {
                    try {
                        detector.setIsAttributeOf(str);
                    } catch (Exception e) {
                        detector.setIsAttributeOf(null);
                    }
                }
            }
        }

        detector.setUri(uri);

        return detector;
    }

    public static List<ContainerSlot> usage(String detectoruri) {
        if (detectoruri == null || detectoruri.isEmpty()) {
            return null;
        }
        List<ContainerSlot> containerSlots = new ArrayList<ContainerSlot>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?detSlotUri WHERE { " +
                " ?detSlotModel rdfs:subClassOf* vstoi:ContainerSlot . " +
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
            ContainerSlot containerSlot = ContainerSlot.find(soln.getResource("detSlotUri").getURI());
            containerSlots.add(containerSlot);
        }
        return containerSlots;
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

    public static boolean attach(ContainerSlot containerSlot, Detector detector) {
        //System.out.println("called Detector.attach()");
        if (containerSlot == null) {
            System.out.println("A valid container slot is required to attach a detector");
            return false;
        }
        return containerSlot.updateContainerSlotDetector(detector);
    }

    public static boolean detach(ContainerSlot containerSlot) {
        //System.out.println("called Detector.detach()");
        if (containerSlot == null) {
            return false;
        }
        return containerSlot.updateContainerSlotDetector(null);
    }

    @Override public void save() {
        saveToTripleStore();
    }

    @Override public void delete() {
        deleteFromTripleStore();
    }

}

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
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemAnswerOptionComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.sirapi.annotations.PropertyField;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.NameSpaces;
import org.sirapi.vocabularies.*;

@JsonFilter("detectorFilter")
public class Detector extends HADatAcThing implements SIRElement, Comparable<Detector>  {

    @PropertyField(uri="vstoi:hasStatus")
    private String hasStatus;

    @PropertyField(uri="vstoi:hasSerialNumber")
    private String serialNumber;

    @PropertyField(uri="hasco:hasImage")
    private String image;

    //@PropertyField(uri="vstoi:isInstrumentAttachment")
    //private String isInstrumentAttachment;

    @PropertyField(uri="vstoi:hasContent")
    private String hasContent;

    //@PropertyField(uri="vstoi:hasPriority")
    //private String hasPriority;

    @PropertyField(uri="vstoi:hasLanguage")
    private String hasLanguage;

    @PropertyField(uri="vstoi:hasVersion")
    private String hasVersion;

    @PropertyField(uri="prov:wasDerivedFrom")
    private String wasDerivedFrom;

    @PropertyField(uri="prov:wasGeneratedBy")
    private String wasGeneratedBy;

    @PropertyField(uri="vstoi:hasSIRMaintainerEmail")
    private String hasSIRMaintainerEmail;

    @PropertyField(uri="vstoi:hasExperience")
    private String hasExperience;

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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    //public String getIsInstrumentAttachment() {
    //    return isInstrumentAttachment;
    //}

    //public void setIsInstrumentAttachment(String isInstrumentAttachment) {
    //    this.isInstrumentAttachment = isInstrumentAttachment;
    //}

    public String getHasContent() {
        return hasContent;
    }

    public void setHasContent(String hasContent) {
        this.hasContent = hasContent;
    }

    //public String getHasPriority() {
    //    return hasPriority;
    //}

    //public void setHasPriority(String hasPriority) {
    //    this.hasPriority = hasPriority;
    //}

    public String getHasExperience() {
        return hasExperience;
    }

    public void setHasExperience(String hasExperience) {
        this.hasExperience = hasExperience;
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

    public String getHasSIRMaintainerEmail() {
        return hasSIRMaintainerEmail;
    }

    public void setHasSIRMaintainerEmail(String hasSIRMaintainerEmail) {
        this.hasSIRMaintainerEmail = hasSIRMaintainerEmail;
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
                " ?uri vstoi:hasContent ?content . " +
                "} " +
                " ORDER BY ASC(?content) ";

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
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel . } " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        return findByQuery(queryString);
    }

    public static List<Detector> findByLanguage(String language) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?uri vstoi:hasLanguage ?language . " +
                "   FILTER (?language = \"" + language + "\") " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<Detector> findByKeyword(String keyword) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?uri vstoi:hasContent ?content . " +
                "   FILTER regex(?content, \"" + keyword + "\", \"i\") " +
                "} ";

        return findByQuery(queryString);
    }


    public static List<Detector> findByKeywordAndLanguageWithPages(String keyword, String language, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel .";
        if (!language.isEmpty()) {
            queryString += " ?uri vstoi:hasLanguage ?language . ";
        }
        queryString += " ?uri vstoi:hasContent ?content . ";
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
        return findByQuery(queryString);
    }

    public static int findTotalByKeywordAndLanguage(String keyword, String language) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel .";
        if (!language.isEmpty()) {
            queryString += " ?uri vstoi:hasLanguage ?language . ";
        }
        if (!keyword.isEmpty()) {
            queryString += " ?uri vstoi:hasContent ?content . ";
        }
        if (!keyword.isEmpty() && !language.isEmpty()) {
            queryString += "   FILTER (regex(?content, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) ";
        } else if (!keyword.isEmpty()) {
            queryString += "   FILTER (regex(?content, \"" + keyword + "\", \"i\")) ";
        } else if (!language.isEmpty()) {
            queryString += "   FILTER ((?language = \"" + language + "\")) ";
        }
        queryString += "}";

        //System.out.println(queryString);

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

    public static List<Detector> findByMaintainerEmail(String maintainerEmail) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?uri vstoi:hasSIRMaintainerEmail ?maintainerEmail . " +
                " ?uri vstoi:hasContent ?content . " +
                "   FILTER (?maintainerEmail = \"" + maintainerEmail + "\") " +
                "} " +
                " ORDER BY ASC(?content) ";

        return findByQuery(queryString);
    }

    public static List<Detector> findByMaintainerEmailWithPages(String maintainerEmail, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?uri vstoi:hasSIRMaintainerEmail ?maintainerEmail . " +
                " ?uri vstoi:hasContent ?content . " +
                "   FILTER (?maintainerEmail = \"" + maintainerEmail + "\") " +
                "} " +
                " ORDER BY ASC(?content) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        return findByQuery(queryString);
    }

    public static List<Detector> findByInstrument(String instrumentUri) {
        //System.out.println("findByInstrument: [" + instrumentUri + "]");
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?attUri vstoi:hasDetector ?uri . " +
                " ?attUri vstoi:belongsTo <" + instrumentUri + ">. " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<Detector> findAvailable() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                "   { ?detModel rdfs:subClassOf* vstoi:Detector . " +
                "     ?uri a ?detModel ." +
                "   } MINUS { " +
                "     ?dep_uri a vstoi:Deployment . " +
                "     ?dep_uri hasco:hasDetector ?uri .  " +
                "     FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " +
                "    } " +
                "} " +
                "ORDER BY DESC(?datetime) ";

        return findByQuery(queryString);
    }

    public static List<Detector> findDeployed() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                "   ?detModel rdfs:subClassOf* vstoi:Detector . " +
                "   ?uri a ?detModel ." +
                "   ?dep_uri a vstoi:Deployment . " +
                "   ?dep_uri hasco:hasDetector ?uri .  " +
                "   FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " +
                "} " +
                "ORDER BY DESC(?datetime) ";

        return findByQuery(queryString);
    }

    private static List<Detector> findByQuery(String queryString) {
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

        java.util.Collections.sort((List<Detector>) detectors);
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
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                detector.setComment(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                detector.setHascoTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
                detector.setHasStatus(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SERIAL_NUMBER)) {
                detector.setSerialNumber(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_IMAGE)) {
                detector.setImage(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_CONTENT)) {
                detector.setHasContent(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
                detector.setHasLanguage(object.asLiteral().getString());
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
                detector.setHasSIRMaintainerEmail(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_EXPERIENCE)) {
                try {
                    detector.setHasExperience(object.asResource().getURI());
                } catch (Exception e) {
                    detector.setHasExperience(null);
                }

            }
        }

        detector.setUri(uri);

        return detector;
    }

    public static boolean attach(String attachmentUri, String detectorUri) {
        if (attachmentUri == null || attachmentUri.isEmpty()) {
            return false;
        }
        Attachment attachment = Attachment.find(attachmentUri);
        if (attachment == null) {
            System.out.println("Attachment.find returned nothing");
        }
        if (attachment == null) {
            return false;
        }
        return attachment.updateAttachmentDetector(detectorUri);
    }

    public static boolean detach(String attachmentUri) {
        if (attachmentUri == null || attachmentUri.isEmpty()) {
            return false;
        }
        Attachment attachment = Attachment.find(attachmentUri);
        if (attachment == null) {
            return false;
        }
        return attachment.updateAttachmentDetector(null);
    }

    @Override
    public int compareTo(Detector another) {
        return this.getLabel().compareTo(another.getLabel());
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

    /*
    public QuestionnaireItemComponent getFHIRObject() {
        QuestionnaireItemComponent item = new QuestionnaireItemComponent();
        Experience experience = getExperience();
        if (experience != null) {
            List<ResponseOption> responseOptions = experience.getResponseOptions();
            for (ResponseOption responseOption : responseOptions) {
                QuestionnaireItemAnswerOptionComponent answerOption = new QuestionnaireItemAnswerOptionComponent();
                Coding coding = responseOption.getFHIRObject();
                answerOption.setValue(coding);
                item.addAnswerOption(answerOption);
            }
        }

        item.setDefinition(getUri());
        item.setText(getHasContent());
        item.setType(QuestionnaireItemType.CHOICE);

        return item;
    }
     */
}

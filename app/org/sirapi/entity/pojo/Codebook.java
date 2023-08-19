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
import org.sirapi.utils.Utils;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.RDF;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.VSTOI;

import java.util.ArrayList;
import java.util.List;

@JsonFilter("codebookFilter")
public class Codebook extends HADatAcThing implements SIRElement, Comparable<Codebook>  {

    @PropertyField(uri="vstoi:hasStatus")
    private String hasStatus;

    @PropertyField(uri="vstoi:hasSerialNumber")
    private String serialNumber;

    @PropertyField(uri="vstoi:hasLanguage")
    private String hasLanguage;

    @PropertyField(uri="vstoi:hasVersion")
    private String hasVersion;

    @PropertyField(uri="vstoi:hasSIRMaintainerEmail")
    private String hasSIRMaintainerEmail;

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

    public String getHasSIRMaintainerEmail() {
        return hasSIRMaintainerEmail;
    }

    public void setHasSIRMaintainerEmail(String hasSIRMaintainerEmail) {
        this.hasSIRMaintainerEmail = hasSIRMaintainerEmail;
    }

    public List<CodebookSlot> getCodebookSlots() {
        List<CodebookSlot> slots = CodebookSlot.findByCodebook(uri);
        return slots;
    }

    /*
    public List<ResponseOption> getResponseOptions() {
        return ResponseOption.findByCodebook(getUri());
    }
     */

    public boolean createCodebookSlots(int totSlots) {
        if (totSlots <= 0) {
            return false;
        }
        if (this.getCodebookSlots() != null || uri == null || uri.isEmpty()) {
            return false;
        }
        for (int aux=1; aux <= totSlots; aux++) {
            String auxstr = Utils.adjustedPriority(String.valueOf(aux), totSlots);
            String newUri = uri + "/CBS/" + auxstr;
            CodebookSlot.createCodebookSlot(uri, newUri, auxstr,null);
        }
        List<CodebookSlot> slotList = CodebookSlot.findByCodebook(uri);
        if (slotList == null) {
            return false;
        }
        return (slotList.size() == totSlots);
    }

    public boolean deleteCodebookSlots() {
        if (this.getCodebookSlots() == null || uri == null || uri.isEmpty()) {
            return true;
        }
        List<CodebookSlot> slots = CodebookSlot.findByCodebook(uri);
        if (slots == null) {
            return true;
        }
        for (CodebookSlot slot: slots) {
            slot.delete();
        }
        slots = CodebookSlot.findByCodebook(uri);
        return (slots == null);
    }


    public static List<Codebook> findByLanguage(String language) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?codebook rdfs:subClassOf* vstoi:Codebook . " +
                " ?uri a ?codebook ." +
                " ?uri vstoi:hasLanguage ?language . " +
                "   FILTER (?language = \"" + language + "\") " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<Codebook> findByKeyword(String keyword) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?codebookType rdfs:subClassOf* vstoi:Codebook . " +
                " ?uri a ?codebookType ." +
                " ?uri rdfs:label ?label . " +
                "   FILTER regex(?label, \"" + keyword + "\", \"i\") " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<Codebook> findByKeywordAndLanguageWithPages(String keyword, String language, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT ?uri WHERE { " +
                " ?expModel rdfs:subClassOf* vstoi:Codebook . " +
                " ?uri a ?expModel .";
        if (!language.isEmpty()) {
            queryString += " ?uri vstoi:hasLanguage ?language . ";
        }
        if (!keyword.isEmpty()) {
            queryString += " ?uri rdfs:label ?label . ";
        }
        if (!keyword.isEmpty() && !language.isEmpty()) {
            queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) ";
        } else if (!keyword.isEmpty()) {
            queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\")) ";
        } else if (!language.isEmpty()) {
            queryString += "   FILTER ((?language = \"" + language + "\")) ";
        }
        queryString += "} " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return findByQuery(queryString);
    }

    public static int findTotalByKeywordAndLanguage(String keyword, String language) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
                " ?expModel rdfs:subClassOf* vstoi:Codebook . " +
                " ?uri a ?expModel .";
        if (!language.isEmpty()) {
            queryString += " ?uri vstoi:hasLanguage ?language . ";
        }
        if (!keyword.isEmpty()) {
            queryString += " ?uri rdfs:label ?label . ";
        }
        if (!keyword.isEmpty() && !language.isEmpty()) {
            queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) ";
        } else if (!keyword.isEmpty()) {
            queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\")) ";
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

    public static List<Codebook> findByMaintainerEmailWithPages(String maintainerEmail, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT ?uri WHERE { " +
                " ?expModel rdfs:subClassOf* vstoi:Codebook . " +
                " ?uri a ?expModel ." +
                " ?uri vstoi:hasSIRMaintainerEmail ?maintainerEmail . " +
                "   FILTER (?maintainerEmail = \"" + maintainerEmail + "\") " +
                "} " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return findByQuery(queryString);
    }

    public static int findTotalByMaintainerEmail(String maintainerEmail) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
                " ?expModel rdfs:subClassOf* vstoi:Codebook . " +
                " ?uri a ?expModel ." +
                " ?uri vstoi:hasSIRMaintainerEmail ?maintainerEmail . " +
                "   FILTER (?maintainerEmail = \"" + maintainerEmail + "\") " +
                "} ";
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

    public static List<Codebook> findByMaintainerEmail(String maintainerEmail) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?codebookType rdfs:subClassOf* vstoi:Codebook . " +
                " ?uri a ?codebookType ." +
                " ?uri vstoi:hasSIRMaintainerEmail ?maintainerEmail . " +
                "   FILTER (?maintainerEmail = \"" + maintainerEmail + "\") " +
                "} ";

        return findByQuery(queryString);
    }

    public static List<Codebook> find() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?codebook rdfs:subClassOf* vstoi:Codebook . " +
                " ?uri a ?codebook ." +
                "} ";

        return findByQuery(queryString);
    }

    private static List<Codebook> findByQuery(String queryString) {
        List<Codebook> codebooks = new ArrayList<Codebook>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            Codebook codebook = find(soln.getResource("uri").getURI());
            codebooks.add(codebook);
        }

        java.util.Collections.sort((List<Codebook>) codebooks);
        return codebooks;
    }

    public static int getNumberCodebooks() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?codebook rdfs:subClassOf* vstoi:Codebook . " +
                " ?uri a ?codebook ." +
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

    public static List<Codebook> findWithPages(int pageSize, int offset) {
        List<Codebook> options = new ArrayList<Codebook>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?codebook rdfs:subClassOf* vstoi:Codebook . " +
                " ?uri a ?option . } " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI() != null) {
                Codebook responseOption = Codebook.find(soln.getResource("uri").getURI());
                options.add(responseOption);
            }
        }
        return options;
    }

    public static Codebook find(String uri) {
        Codebook codebook = null;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

        codebook = new Codebook();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                codebook.setLabel(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                codebook.setTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                codebook.setComment(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                codebook.setHascoTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
                codebook.setHasStatus(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SERIAL_NUMBER)) {
                codebook.setSerialNumber(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
                codebook.setHasLanguage(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
                codebook.setHasVersion(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MAINTAINER_EMAIL)) {
                codebook.setHasSIRMaintainerEmail(object.asLiteral().getString());
            }
        }

        codebook.setUri(uri);

        return codebook;
    }

    @Override
    public int compareTo(Codebook another) {
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

}

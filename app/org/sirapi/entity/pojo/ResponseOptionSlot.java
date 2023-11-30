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

@JsonFilter("ResponseOptionSlotFilter")
public class ResponseOptionSlot extends HADatAcThing implements Comparable<ResponseOptionSlot> {

    @PropertyField(uri = "vstoi:belongsTo")
    private String belongsTo;

    @PropertyField(uri = "vstoi:hasResponseOption")
    private String hasResponseOption;

    @PropertyField(uri = "vstoi:hasPriority")
    private String hasPriority;

    public String getBelongsTo() {
        return belongsTo;
    }

    public void setBelongsTo(String belongsTo) {
        this.belongsTo = belongsTo;
    }

    public String getHasResponseOption() {
        return hasResponseOption;
    }

    public void setHasResponseOption(String hasResponseOption) {
        this.hasResponseOption = hasResponseOption;
    }

    public String getHasPriority() {
        return hasPriority;
    }

    public void setHasPriority(String hasPriority) {
        this.hasPriority = hasPriority;
    }

    public ResponseOption getResponseOption() {
        if (hasResponseOption == null || hasResponseOption.isEmpty()) {
            return null;
        }
        return ResponseOption.find(hasResponseOption);
    }

    public static int getNumberResponseOptionSlotsByInstrument(String codebookUri) {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?slotModel rdfs:subClassOf* vstoi:ResponseOptionSlot . " +
                " ?uri a ?slotModel ." +
                " ?uri vstoi:belongsTo <" + codebookUri + ">. " +
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

    public static List<ResponseOptionSlot> findByCodebookWithPages(String codebookUri, int pageSize, int offset) {
        List<ResponseOptionSlot> slots = new ArrayList<ResponseOptionSlot>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?slotModel rdfs:subClassOf* vstoi:ResponseOptionSlot . " +
                " ?uri a ?slotModel . } " +
                " ?uri vstoi:belongsTo <" + codebookUri + ">. " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI() != null) {
                ResponseOptionSlot slot = ResponseOptionSlot.find(soln.getResource("uri").getURI());
                slots.add(slot);
            }
        }
        return slots;
    }

    public static List<ResponseOptionSlot> find() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?slotModel rdfs:subClassOf* vstoi:ResponseOptionSlot . " +
                " ?uri a ?slotModel ." +
                "} ";

        return findByQuery(queryString);
    }

    public static List<ResponseOptionSlot> findByCodebook(String codebookUri) {
        // System.out.println("findByCodebook: [" + codebookUri + "]");
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?slotModel rdfs:subClassOf* vstoi:ResponseOptionSlot . " +
                " ?uri a ?slotModel ." +
                " ?uri vstoi:belongsTo <" + codebookUri + ">. " +
                "} ";

        return findByQuery(queryString);
    }

    private static List<ResponseOptionSlot> findByQuery(String queryString) {
        List<ResponseOptionSlot> slots = new ArrayList<ResponseOptionSlot>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            ResponseOptionSlot slot = find(soln.getResource("uri").getURI());
            slots.add(slot);
        }

        java.util.Collections.sort((List<ResponseOptionSlot>) slots);
        return slots;

    }

    public static ResponseOptionSlot find(String uri) {
        ResponseOptionSlot slot = null;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

        slot = new ResponseOptionSlot();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                slot.setLabel(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                slot.setTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                slot.setComment(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                slot.setHascoTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.BELONGS_TO)) {
                slot.setBelongsTo(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_RESPONSE_OPTION)) {
                slot.setHasResponseOption(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_PRIORITY)) {
                slot.setHasPriority(object.asLiteral().getString());
            }
        }

        slot.setUri(uri);

        return slot;
    }

    public static int getNumberResponseOptionSlots() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?rosModel rdfs:subClassOf* vstoi:ResponseOptionSlot . " +
                " ?uri a ?rosModel ." +
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

    public static int getNumberResponseOptionSlotsWithResponseOptions() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?rosModel rdfs:subClassOf* vstoi:ResponseOptionSlot . " +
                " ?uri a ?rosModel ." +
                " ?uri vstoi:hasResponseOption ?ros . " +
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



    static public boolean createResponseOptionSlot(String codebookUri, String slotUri, String priority,
            String hasResponseOption) {
        if (codebookUri == null || codebookUri.isEmpty()) {
            return false;
        }
        if (priority == null || priority.isEmpty()) {
            return false;
        }
        ResponseOptionSlot ros = new ResponseOptionSlot();
        ros.setUri(slotUri);
        ros.setLabel("ResponseOptionSlot " + priority);
        ros.setTypeUri(VSTOI.RESPONSE_OPTION_SLOT);
        ros.setHascoTypeUri(VSTOI.RESPONSE_OPTION_SLOT);
        ros.setComment("ResponseOptionSlot " + priority + " of codebook with URI " + codebookUri);
        ros.setBelongsTo(codebookUri);
        ros.setHasPriority(priority);
        if (hasResponseOption != null) {
            ros.setHasResponseOption(hasResponseOption);
        }
        ros.save();
        //System.out.println("ResponseOptionSlot.createResponseOptionSlot: creating slot " + priority + " with URI ["
        //        + slotUri + "]");
        return true;
    }

    public boolean updateResponseOptionSlotResponseOption(ResponseOption responseOption) {
        ResponseOptionSlot newResponseOptionSlot = new ResponseOptionSlot();
        newResponseOptionSlot.setUri(this.uri);
        newResponseOptionSlot.setLabel(this.getLabel());
        newResponseOptionSlot.setTypeUri(this.getTypeUri());
        newResponseOptionSlot.setComment(this.getComment());
        newResponseOptionSlot.setHascoTypeUri(this.getHascoTypeUri());
        newResponseOptionSlot.setBelongsTo(this.getBelongsTo());
        newResponseOptionSlot.setHasPriority(this.getHasPriority());
        if (responseOption != null && responseOption.getUri() != null && !responseOption.getUri().isEmpty()) {
            newResponseOptionSlot.setHasResponseOption(responseOption.getUri());
        } else {
            newResponseOptionSlot.setHasResponseOption(null);
        }
        this.delete();
        newResponseOptionSlot.save();
        //System.out.println("In ResponseOption.updateResponseOptionSlotResponseOption(): value of hasResponseOption["
        //        + newResponseOptionSlot.getHasResponseOption() + "]");
        return true;
    }

    @Override
    public int compareTo(ResponseOptionSlot another) {
        return this.getHasPriority().compareTo(another.getHasPriority());
    }

    @Override
    public void save() {
        saveToTripleStore();
    }

    @Override
    public void delete() {
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

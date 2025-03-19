package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;

import java.util.ArrayList;
import java.util.List;

@JsonFilter("CodebookSlotFilter")
public class CodebookSlot extends HADatAcThing implements Comparable<CodebookSlot> {

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

    public static int getNumberCodebookSlotsByInstrument(String codebookUri) {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?slotModel rdfs:subClassOf* vstoi:CodebookSlot . " +
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

    public static List<CodebookSlot> findByCodebookWithPages(String codebookUri, int pageSize, int offset) {
        List<CodebookSlot> slots = new ArrayList<CodebookSlot>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?slotModel rdfs:subClassOf* vstoi:CodebookSlot . " +
                " ?uri a ?slotModel . } " +
                " ?uri vstoi:belongsTo <" + codebookUri + ">. " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI() != null) {
                CodebookSlot slot = CodebookSlot.find(soln.getResource("uri").getURI());
                slots.add(slot);
            }
        }
        return slots;
    }

    public static List<CodebookSlot> find() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?slotModel rdfs:subClassOf* vstoi:CodebookSlot . " +
                " ?uri a ?slotModel ." +
                "} ";

        return findByQuery(queryString);
    }

    public static List<CodebookSlot> findByCodebook(String codebookUri) {
        // System.out.println("findByCodebook: [" + codebookUri + "]");
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?slotModel rdfs:subClassOf* vstoi:CodebookSlot . " +
                " ?uri a ?slotModel ." +
                " ?uri vstoi:belongsTo <" + codebookUri + ">. " +
                "} ";

        return findByQuery(queryString);
    }

    private static List<CodebookSlot> findByQuery(String queryString) {
        List<CodebookSlot> slots = new ArrayList<CodebookSlot>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            CodebookSlot slot = find(soln.getResource("uri").getURI());
            slots.add(slot);
        }

        java.util.Collections.sort((List<CodebookSlot>) slots);
        return slots;

    }

    public static CodebookSlot find(String uri) {
 		if (uri == null || uri.isEmpty()) {
			return null;
		}
		CodebookSlot slot = null;
		// Construct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
            slot = new CodebookSlot();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				slot.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);

                if (predicate.equals(RDFS.LABEL)) {
                    slot.setLabel(object);
                } else if (predicate.equals(RDF.TYPE)) {
                    slot.setTypeUri(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    slot.setComment(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    slot.setHascoTypeUri(object);
                } else if (predicate.equals(VSTOI.BELONGS_TO)) {
                    slot.setBelongsTo(object);
                } else if (predicate.equals(VSTOI.HAS_RESPONSE_OPTION)) {
                    slot.setHasResponseOption(object);
                } else if (predicate.equals(VSTOI.HAS_PRIORITY)) {
                    slot.setHasPriority(object);
                }
            }
        }

        slot.setUri(uri);

        return slot;
    }

    public static int getNumberCodebookSlots() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?rosModel rdfs:subClassOf* vstoi:CodebookSlot . " +
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

    public static int getNumberCodebookSlotsWithResponseOptions() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?rosModel rdfs:subClassOf* vstoi:CodebookSlot . " +
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

    static public boolean createCodebookSlot(String codebookUri, String slotUri, String priority,
            String hasResponseOption) {
        if (codebookUri == null || codebookUri.isEmpty()) {
            return false;
        }
        if (priority == null || priority.isEmpty()) {
            return false;
        }
        CodebookSlot ros = new CodebookSlot();
        ros.setUri(slotUri);
        ros.setLabel("CodebookSlot " + priority);
        ros.setTypeUri(VSTOI.CODEBOOK_SLOT);
        ros.setHascoTypeUri(VSTOI.CODEBOOK_SLOT);
        ros.setComment("CodebookSlot " + priority + " of codebook with URI " + codebookUri);
        ros.setBelongsTo(codebookUri);
        ros.setHasPriority(priority);
        if (hasResponseOption != null) {
            ros.setHasResponseOption(hasResponseOption);
        }
        ros.save();
        //System.out.println("CodebookSlot.createCodebookSlot: creating slot " + priority + " with URI ["
        //        + slotUri + "]");
        return true;
    }

    public boolean updateCodebookSlotResponseOption(ResponseOption responseOption) {
        CodebookSlot newCodebookSlot = new CodebookSlot();
        newCodebookSlot.setUri(this.uri);
        newCodebookSlot.setLabel(this.getLabel());
        newCodebookSlot.setTypeUri(this.getTypeUri());
        newCodebookSlot.setComment(this.getComment());
        newCodebookSlot.setHascoTypeUri(this.getHascoTypeUri());
        newCodebookSlot.setBelongsTo(this.getBelongsTo());
        newCodebookSlot.setHasPriority(this.getHasPriority());
        if (responseOption != null && responseOption.getUri() != null && !responseOption.getUri().isEmpty()) {
            newCodebookSlot.setHasResponseOption(responseOption.getUri());
        } else {
            newCodebookSlot.setHasResponseOption(null);
        }
        this.delete();
        newCodebookSlot.save();
        //System.out.println("In ResponseOption.updateCodebookSlotResponseOption(): value of hasResponseOption["
        //        + newCodebookSlot.getHasResponseOption() + "]");
        return true;
    }

    @Override
    public int compareTo(CodebookSlot another) {
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

}

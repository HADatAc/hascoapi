package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.RDFNode;

import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.Utils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.PROV;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.annotations.PropertyField;

import com.fasterxml.jackson.annotation.JsonFilter;

@JsonFilter("semanticStreamTopicFilter")
public class SemanticStreamTopic extends HADatAcThing implements Comparable<SemanticStreamTopic> {

    private static final String className = "hasco:SemanticStreamTopic";

    /*
     * SEMANTIC STREAM TOPIC PROPERTIES
     */
    @PropertyField(uri = "hasco:hasSemanticStream")
    private String hasSemanticStream;

    @PropertyField(uri="prov:startedAtTime")
    private String startedAtTime;

    public SemanticStreamTopic() {
    }

    // Getters and Setters
    public String getHasSemanticStream() {
        return hasSemanticStream;
    }

    public void setHasSemanticStream(String hasSemanticStream) {
        this.hasSemanticStream = hasSemanticStream;
    }

    public String getstartedAtTime() {
        return startedAtTime;
    }

    public void setstartedAtTime(String startedAtTime) {
        this.startedAtTime = startedAtTime;
    }

    @Override
    public int compareTo(SemanticStreamTopic another) {
        if (this.getLabel() != null && another.getLabel() != null) {
            return this.getLabel().compareTo(another.getLabel());
        }
        return this.getUri().compareTo(another.getUri());
    }

    @Override
    public boolean equals(Object o) {
        if ((o instanceof SemanticStreamTopic) && (((SemanticStreamTopic) o).getUri().equals(this.getUri()))) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * SPARQL Retrieval
     */
    public static SemanticStreamTopic find(String uri) {
        SemanticStreamTopic topic;
        String hascoTypeUri = Utils.retrieveHASCOTypeUri(uri);

        if (hascoTypeUri.equals(HASCO.SEMANTIC_STREAM_TOPIC)) {
            topic = new SemanticStreamTopic();
        } else {
            return null;
        }

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

        while (stmtIterator.hasNext()) {
            Statement statement = stmtIterator.next();
            RDFNode object = statement.getObject();
            String string = URIUtils.objectRDFToString(object);

            if (uri != null && !uri.isEmpty()) {
                if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                    topic.setLabel(string);
                } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                    topic.setTypeUri(string);
                } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                    topic.setHascoTypeUri(string);
                } else if (statement.getPredicate().getURI().equals(HASCO.HAS_SEMANTIC_STREAM)) {
                    topic.setHasSemanticStream(string);
                } else if (statement.getPredicate().getURI().equals(PROV.STARTED_AT_TIME)) {
                    topic.setstartedAtTime(string);
                }
            }
        }

        topic.setUri(uri);
        return topic;
    }

    /*
     * Query utilities
     */
    public static List<SemanticStreamTopic> findAll() {
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                "?uri a hasco:SemanticStreamTopic . " +
                "}";
        return findManyByQuery(query);
    }

    public static List<SemanticStreamTopic> findManyByQuery(String query) {
        List<SemanticStreamTopic> topics = new ArrayList<>();
        var resultsrw = SPARQLUtils.select(CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

        while (resultsrw.hasNext()) {
            var soln = resultsrw.next();
            String uri = soln.getResource("uri").getURI();
            SemanticStreamTopic topic = SemanticStreamTopic.find(uri);
            topics.add(topic);
        }

        return topics;
    }

    @Override
    public void save() {
        try {
            saveToTripleStore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete() {
        deleteFromTripleStore();
    }
}

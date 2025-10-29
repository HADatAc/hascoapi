package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

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
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.annotations.PropertyField;

import com.fasterxml.jackson.annotation.JsonFilter;

@JsonFilter("semanticMessageBrokerFilter")
public class SemanticMessageBroker extends HADatAcThing implements Comparable<SemanticMessageBroker> {

    private static final String className = "hasco:SemanticMessageBroker";

    /*
     * SEMANTIC MESSAGE BROKER PROPERTIES
     */
    @PropertyField(uri = "hasco:hasMessageIp")
    private String ip;

    public SemanticMessageBroker() {
    }

    // Getters and Setters
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public int compareTo(SemanticMessageBroker another) {
        if (this.getLabel() != null && another.getLabel() != null) {
            return this.getLabel().compareTo(another.getLabel());
        }
        return this.getUri().compareTo(another.getUri());
    }

    @Override
    public boolean equals(Object o) {
        if ((o instanceof SemanticMessageBroker) && (((SemanticMessageBroker) o).getUri().equals(this.getUri()))) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * SPARQL Retrieval
     */
    public static SemanticMessageBroker find(String uri) {
        SemanticMessageBroker broker;
        String hascoTypeUri = Utils.retrieveHASCOTypeUri(uri);

        if (hascoTypeUri.equals(HASCO.SEMANTIC_MESSAGE_BROKER)) {
            broker = new SemanticMessageBroker();
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
                    broker.setLabel(string);
                } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                    broker.setTypeUri(string);
                } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                    broker.setHascoTypeUri(string);
                } else if (statement.getPredicate().getURI().equals(HASCO.HAS_MESSAGE_IP)) {
                    broker.setIp(string);
                }
            }
        }

        broker.setUri(uri);
        return broker;
    }

    /*
     * Query utilities
     */
    public static List<SemanticMessageBroker> findAll() {
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                "?uri a hasco:SemanticMessageBroker . " +
                "}";
        return findManyByQuery(query);
    }

    public static List<SemanticMessageBroker> findManyByQuery(String query) {
        List<SemanticMessageBroker> brokers = new ArrayList<>();
        var resultsrw = SPARQLUtils.select(CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

        while (resultsrw.hasNext()) {
            var soln = resultsrw.next();
            String uri = soln.getResource("uri").getURI();
            SemanticMessageBroker broker = SemanticMessageBroker.find(uri);
            brokers.add(broker);
        }

        return brokers;
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

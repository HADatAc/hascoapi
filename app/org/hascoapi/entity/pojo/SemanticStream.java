package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
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

@JsonFilter("semanticStreamFilter")
public class SemanticStream extends HADatAcThing implements Comparable<SemanticStream> {

    private static final String className = "hasco:SemanticStream";

    /*
     * SEMANTIC STREAM PROPERTIES
     */
    @PropertyField(uri = "hasco:hasMessageProtocol")
    private String hasMessageProtocol;

    @PropertyField(uri = "hasco:port")
    private String port;

    @PropertyField(uri = "hasco:hasStream")
    private String hasStream;

    @PropertyField(uri = "hasco:hasSemanticMessageBroker")
    private String hasSemanticMessageBroker;

    public SemanticStream() {
    }

    // Getters and Setters
    public String getHasMessageProtocol() {
        return hasMessageProtocol;
    }

    public void setHasMessageProtocol(String hasMessageProtocol) {
        this.hasMessageProtocol = hasMessageProtocol;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getHasStream() {
        return hasStream;
    }

    public void setHasStream(String hasStream) {
        this.hasStream = hasStream;
    }

    public String getHasSemanticMessageBroker() {
        return hasSemanticMessageBroker;
    }

    public void setHasSemanticMessageBroker(String hasSemanticMessageBroker) {
        this.hasSemanticMessageBroker = hasSemanticMessageBroker;
    }

    // Comparison by label or URI
    @Override
    public int compareTo(SemanticStream another) {
        if (this.getLabel() != null && another.getLabel() != null) {
            return this.getLabel().compareTo(another.getLabel());
        }
        return this.getUri().compareTo(another.getUri());
    }

    @Override
    public boolean equals(Object o) {
        if ((o instanceof SemanticStream) && (((SemanticStream) o).getUri().equals(this.getUri()))) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * SPARQL Retrieval
     */
    public static SemanticStream find(String uri) {
        SemanticStream semanticStream;
        String hascoTypeUri = Utils.retrieveHASCOTypeUri(uri);

        if (hascoTypeUri.equals(HASCO.SEMANTIC_STREAM)) {
            semanticStream = new SemanticStream();
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
                    semanticStream.setLabel(string);
                } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                    semanticStream.setTypeUri(string);
                } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                    semanticStream.setHascoTypeUri(string);
                } else if (statement.getPredicate().getURI().equals(HASCO.HAS_MESSAGE_PROTOCOL)) {
                    semanticStream.setHasMessageProtocol(string);
                } else if (statement.getPredicate().getURI().equals(HASCO.HAS_MESSAGE_PORT)) {
                    semanticStream.setPort(string);
                } else if (statement.getPredicate().getURI().equals(HASCO.HAS_STREAM)) {
                    semanticStream.setHasStream(string);
                } else if (statement.getPredicate().getURI().equals(HASCO.HAS_SEMANTIC_MESSAGE_BROKER)) {
                    semanticStream.setHasSemanticMessageBroker(string);
                }
            }
        }

        semanticStream.setUri(uri);
        return semanticStream;
    }

    /*
     * SPARQL query to find all Semantic Streams
     */
    public static List<SemanticStream> findAll() {
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                "?uri a hasco:SemanticStream . " +
                "}";
        return findManyByQuery(query);
    }

    public static List<SemanticStream> findManyByQuery(String query) {
        List<SemanticStream> streams = new ArrayList<SemanticStream>();
        var resultsrw = SPARQLUtils.select(CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

        while (resultsrw.hasNext()) {
            var soln = resultsrw.next();
            String uri = soln.getResource("uri").getURI();
            SemanticStream stream = SemanticStream.find(uri);
            streams.add(stream);
        }

        return streams;
    }

    public static SemanticStream findByStream(String streamUri) {
        if (streamUri == null || streamUri.isEmpty()) {
            return null;
        }
    
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() +
            "SELECT ?uri WHERE { " +
            "?uri a hasco:SemanticStream . " +
            "?uri hasco:hasStream <" + streamUri + "> . " +
            "} LIMIT 1";
    
        var results = SPARQLUtils.select(CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
    
        if (results.hasNext()) {
            var soln = results.next();
            String uri = soln.getResource("uri").getURI();
            return SemanticStream.find(uri);
        }
    
        return null;
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

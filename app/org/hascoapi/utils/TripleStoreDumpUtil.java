package org.hascoapi.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;

/**
 * Utility methods to dump and preview triples from the triplestore.
 *
 * Contract:
 * - Dumps a named graph to Turtle (.ttl) using SPARQL CONSTRUCT.
 * - Prints a small preview (first N statements) to the logs.
 */
public class TripleStoreDumpUtil {

    private static final String DEFAULT_PREFIXES = NameSpaces.getInstance().printSparqlNameSpaceList();

    private TripleStoreDumpUtil() {
    }

    public static Model constructNamedGraph(String namedGraphUri) {
        String sparqlService = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);
        String queryString = DEFAULT_PREFIXES
                + " CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <" + namedGraphUri + "> { ?s ?p ?o } }";

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlService, queryString)) {
            return qexec.execConstruct();
        }
    }

    public static long countTriplesInNamedGraph(String namedGraphUri) {
        String sparqlService = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);
        String queryString = "SELECT (COUNT(*) AS ?tot) WHERE { GRAPH <" + namedGraphUri + "> { ?s ?p ?o } }";
        try {
            return SPARQLCountUtil.count(sparqlService, queryString);
        } catch (Exception e) {
            System.out.println("[WARN] countTriplesInNamedGraph failed for graph=" + namedGraphUri + ": " + e.getMessage());
            return -1;
        }
    }

    public static void writeNamedGraphAsTurtle(String namedGraphUri, File outFile) {
        Model model = constructNamedGraph(namedGraphUri);
        if (model == null) {
            System.out.println("[WARN] writeNamedGraphAsTurtle: model is null for graph=" + namedGraphUri);
            return;
        }

        try {
            File parent = outFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
        } catch (Exception ignored) {
        }

        try (OutputStream out = new FileOutputStream(outFile)) {
            model.write(out, "TURTLE");
        } catch (Exception e) {
            System.out.println("[WARN] Failed to write TTL to " + outFile.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    public static void logNamedGraphTriplesPreview(String namedGraphUri, int maxStatements) {
        Model model = constructNamedGraph(namedGraphUri);
        if (model == null) {
            System.out.println("[WARN] logNamedGraphTriplesPreview: model is null for graph=" + namedGraphUri);
            return;
        }

        long size = model.size();
        System.out.println("[TTL PREVIEW] graph=" + namedGraphUri + " triples=" + size);

        int i = 0;
        for (Statement st : model.listStatements().toList()) {
            if (i >= maxStatements) {
                break;
            }
            // keep it readable and single-line
            String line = st.getSubject().toString() + " " + st.getPredicate().toString() + " " + st.getObject().toString();
            System.out.println("[TTL PREVIEW] " + line);
            i++;
        }

        if (size > maxStatements) {
            System.out.println("[TTL PREVIEW] ... (" + (size - maxStatements) + " more triples)");
        }
    }

    /**
     * Small helper to write the preview itself in Turtle too (useful for debugging without full dump).
     */
    public static void writePreviewAsText(File outFile, String content) {
        try {
            File parent = outFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (OutputStream out = new FileOutputStream(outFile)) {
                out.write(content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            System.out.println("[WARN] Failed to write preview text to " + outFile.getAbsolutePath() + ": " + e.getMessage());
        }
    }
}


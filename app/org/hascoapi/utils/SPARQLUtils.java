package org.hascoapi.utils;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hascoapi.utils.URIUtils;

public class SPARQLUtils {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 120L;
    private static final boolean LOG_TRANSIENT_SPARQL_FAILURES =
            Boolean.parseBoolean(System.getenv().getOrDefault("HASCOAPI_LOG_TRANSIENT_SPARQL_FAILURES", "false"));

    public static ResultSetRewindable select(String sparqlService, String queryString) {
        //System.out.println("queryString: " + queryString + "\n");

        try {
            Query query = QueryFactory.create(queryString);
            RuntimeException lastRuntime = null;

            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                QueryExecution qexec = null;
                try {
                    qexec = QueryExecutionFactory.sparqlService(sparqlService, query);
                    ResultSet results = qexec.execSelect();
                    return ResultSetFactory.copyResults(results);
                } catch (RuntimeException e) {
                    lastRuntime = e;
                    if (!isTransientTransportFailure(e) || attempt == MAX_RETRIES) {
                        throw e;
                    }
                    sleepBeforeRetry(attempt);
                } finally {
                    if (qexec != null) {
                        qexec.close();
                    }
                }
            }

            throw lastRuntime == null ? new RuntimeException("SPARQL select failed") : lastRuntime;
        } catch (QueryParseException e) {
            System.out.println("[ERROR] sparqlService: " + sparqlService + "\n");
            System.out.println("[ERROR] queryString: " + queryString);
            throw e;
        } catch (RuntimeException e) {
            if (isTransientTransportFailure(e)) {
                // Degrade gracefully for transient triplestore/network interruptions.
                logTransientFailure("select", sparqlService, e);
                return emptyResultSet();
            }
            throw e;
        }
    }

    public static Model describe(String sparqlService, String queryString) {
        //System.out.println("\nqueryString: " + queryString + "\n");

        try {
            Query query = QueryFactory.create(queryString);
            RuntimeException lastRuntime = null;

            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                QueryExecution qexec = null;
                try {
                    qexec = QueryExecutionFactory.sparqlService(sparqlService, query);
                    return qexec.execDescribe();
                } catch (RuntimeException e) {
                    lastRuntime = e;
                    if (!isTransientTransportFailure(e) || attempt == MAX_RETRIES) {
                        throw e;
                    }
                    sleepBeforeRetry(attempt);
                } finally {
                    if (qexec != null) {
                        qexec.close();
                    }
                }
            }

            throw lastRuntime == null ? new RuntimeException("SPARQL describe failed") : lastRuntime;
        } catch (QueryParseException e) {
            System.out.println("[ERROR] queryString: " + queryString);
            throw e;
        } catch (Exception e) {
            if (isTransientTransportFailure(e)) {
                // Degrade gracefully for transient triplestore/network interruptions.
                logTransientFailure("describe", sparqlService, e);
                return ModelFactory.createDefaultModel();
            }
            System.err.println("[SPARQLUtils] describe() failed with non-transient exception:");
            System.err.println("[SPARQLUtils] Service URL: " + sparqlService);
            System.err.println("[SPARQLUtils] Query: " + queryString);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Execute a describe query returning the model as a SELECT result set
     *
     * @param sparqlService String sparql service URL
     * @param queryString String query string
     * @return ResultSetRewindable
     */
    public static ResultSetRewindable describeAsRs(String sparqlService, String queryString) {
        final String selectAllQuery = "SELECT ?subject ?predicate ?object WHERE { ?subject ?predicate ?object . }";
        Model model = describe(sparqlService, queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(selectAllQuery, model)) {
            ResultSet results = qexec.execSelect();
            ResultSetRewindable resultsrw = ResultSetFactory.copyResults(results);
            return resultsrw;
        }
    }

    public static String describe(String uri) {
        // Robustify: avoid unresolved prefixed names by expanding to absolute URI
        String target = uri;
        if (target != null) {
            target = target.trim();
            try {
                // If it's a prefixed name (e.g., ahead:DPL-WS-001), expand it.
                if (!target.startsWith("<") && !URIUtils.isValidURI(target)) {
                    target = URIUtils.replacePrefixEx(target);
                }
            } catch (Exception e) {
                // keep original; we'll wrap later if needed
            }
            if (!target.startsWith("<") && URIUtils.isValidURI(target)) {
                target = "<" + target + ">";
            }
        }

        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "DESCRIBE " + target;

        return select("http://localhost:8890/sparql", queryString).toString();
    }

    private static boolean isTransientTransportFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String type = current.getClass().getSimpleName().toLowerCase();
            String message = String.valueOf(current.getMessage()).toLowerCase();

            if (type.contains("eofexception")
                    || type.contains("riotexception")
                    || type.contains("connectexception")
                    || type.contains("socketexception")
                    || type.contains("sockettimeoutexception")
                    || type.contains("httptimeoutexception")
                    || type.contains("queryexceptionhttp")
                    || message.contains("eof reached while reading")
                    || message.contains("bad input stream")
                    || message.contains("java.io.ioexception: closed")
                    || message.contains("input stream")
                    || message.contains("stream closed")
                    || message.contains("connection reset")
                    || message.contains("broken pipe")
                    || message.contains("timed out")
                    || message.contains("timeout")
                    || message.contains("connection refused")
                    || message.contains("temporarily unavailable")) {
                return true;
            }

            current = current.getCause();
        }
        return false;
    }

    private static void sleepBeforeRetry(int attempt) {
        long delay = BASE_BACKOFF_MS * attempt;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static ResultSetRewindable emptyResultSet() {
        Model emptyModel = ModelFactory.createDefaultModel();
        try (QueryExecution qexec = QueryExecutionFactory.create("SELECT * WHERE { FILTER(false) }", emptyModel)) {
            return ResultSetFactory.copyResults(qexec.execSelect());
        }
    }

    private static void logTransientFailure(String operation, String sparqlService, Throwable error) {
        if (!LOG_TRANSIENT_SPARQL_FAILURES) {
            return;
        }

        String details = (error == null || error.getMessage() == null) ? "" : (": " + error.getMessage());
        System.err.println("[SPARQLUtils] " + operation + "() transient failure; returning degraded empty result" + details);
        System.err.println("[SPARQLUtils] Service URL: " + sparqlService);
        System.err.println("[SPARQLUtils] Hint: set HASCOAPI_LOG_TRANSIENT_SPARQL_FAILURES=false to keep this silent.");
    }

}
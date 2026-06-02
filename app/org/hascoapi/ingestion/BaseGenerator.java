package org.hascoapi.ingestion;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.hascoapi.utils.Templates;
import org.hascoapi.utils.IngestionLogger;
import org.hascoapi.Constants;
import org.hascoapi.RepositoryInstance;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.HADatAcThing;
import org.hascoapi.utils.MetadataFactory;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.SPARQLUtils;

import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;

public abstract class BaseGenerator {

    protected int QUERY_LIMIT = 2000;

    protected List<Record> records = null;
    protected RecordFile file;
    protected DataFile dataFile;

    protected List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
    protected List<HADatAcThing> objects = new ArrayList<HADatAcThing>();

    protected Map<String, Cache> caches = new HashMap<String, Cache>();

    protected HashMap<String, String> mapCol = new HashMap<String, String>();
    protected String fileName = "";
    protected String relativePath = "";

    protected String studyUri = "";
    protected String namedGraphUri = "";
    protected Templates templates = null;
    protected String elementType = "";

    protected IngestionLogger logger = null;

    public BaseGenerator(DataFile dataFile) {
        this(dataFile, null, null);
    }

    public BaseGenerator(DataFile dataFile, String studyUri) {
        this(dataFile, studyUri, null);
    }

    public BaseGenerator(DataFile dataFile, String studyUri, String templateFile) {
        if (studyUri != null && !studyUri.equals("")) {
            this.studyUri = studyUri;
        }
        if (templateFile != null) {
            templates = new Templates(templateFile);
        }

        //System.out.println("BaseGenerator: (Constructor) process dataFile");
        if (dataFile != null) {
            this.dataFile = dataFile;
            //System.out.println("BaseGenerator: (Constructor) process dataFile: file");
            file = dataFile.getRecordFile();
            //System.out.println("BaseGenerator: (Constructor) process dataFile: records");
            records = file.getRecords();
            //System.out.println("BaseGenerator: Number of records is [" + records.size() + "]");
            fileName = dataFile.getFilename();
            //System.out.println("BaseGenerator: (Constructor) process dataFile: logger");
            logger = dataFile.getLogger();
        }

        //System.out.println("BaseGenerator: (Constructor) process initMapping");
        initMapping();
    }

    public void initMapping() {
    }

    public void dispose() {
        if (rows != null) {
            rows.clear();
        }
        if (objects != null) {
            objects.clear();
        }
    }

    public void addCache(Cache cache) {
        if (!caches.containsKey(cache.getName())) {
            caches.put(cache.getName(), cache);
        }
    }

    public void clearCacheByName(String name) {
        if (caches.containsKey(name)) {
            caches.get(name).clear();
        }
    }

    public void clearAllCaches() {
        for (String name : caches.keySet()) {
            caches.get(name).clear();
        }
    }

    public IngestionLogger getLogger() {
        return logger;
    }

    public String getTableName() {
        return null;
    }

    public String getErrorMsg(Exception e) {
        e.printStackTrace();
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        return "Errors in " + getClass().getSimpleName() + ": " + e.getMessage() + " " + errors.toString();
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public String getFileName() {
        return fileName;
    }

    public RecordFile getRecordFile() {
        return file;
    }

    public String getStudyUri() {
        return studyUri;
    }

    public void setStudyUri(String studyUri) {
        this.studyUri = studyUri;
    }

    public String getNamedGraphUri() {
        return namedGraphUri;
    }

    public void setNamedGraphUri(String namedGraphUri) {
        this.namedGraphUri = namedGraphUri;
    }

    public String getElementType() {
        return this.elementType;
    }

    public void setElementType(String elementType) {
        this.elementType = elementType;
    }


    public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
        return null;
    }

    public HADatAcThing createObject(Record rec, int rowNumber, String selector) throws Exception {
        return null;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public List<HADatAcThing> getObjects() {
        return objects;
    }

    public void addRow(Map<String, Object> row) {
        rows.add(row);
    }

    public void addObject(HADatAcThing object) {
        objects.add(object);
    }

    public void preprocess() throws Exception {
    }

    public void preprocessuris(Map<String, String> uris) throws Exception {
    }

    public void postprocess() throws Exception {
    }

    public Map<String, String> postprocessuris() throws Exception {
        return new HashMap<String, String>();
    }

    public void createRows() throws Exception {
        if (records == null) {
            System.out.println("[WARNING] BaseGenerator.createRows(): records is NULL");
            return;
        }
        System.out.println("[" + this.getClass().getSimpleName() + "] Processing " + records.size() + " records for elementType=" + this.getElementType());

        int rowNumber = 0;
        int skippedRows = 0;
        int validRows = 0;
        Record lastRecord = null;
        for (Record record : records) {
            if (lastRecord != null && record.equals(lastRecord)) {
                skippedRows++;
            } else {
                Map<String, Object> tempRow = createRow(record, ++rowNumber);
                if (tempRow != null) {
                    rows.add(tempRow);
                    validRows++;
                    lastRecord = record;
                }
            }
        }
        System.out.println("[" + this.getClass().getSimpleName() + "] Completed: " + validRows + " valid rows, " + skippedRows + " duplicates skipped");
    }

    public void createObjects() throws Exception {
        System.out.println("[CREATE-OBJECTS DEBUG] createObjects() called for generator: " + this.getClass().getSimpleName());
        if (records == null) {
            System.out.println("[CREATE-OBJECTS DEBUG] records is null, returning");
            return;
        }

        int rowNumber = 0;
        for (Record record : records) {
            if (record.size() <= 0) {
                break;
            }
            HADatAcThing obj = createObject(record, ++rowNumber, null);
            if (obj != null) {
                objects.add(obj);
                System.out.println("[CREATE-OBJECTS DEBUG] Added object class: " + obj.getClass().getSimpleName() + ", URI: " + obj.getUri());
            }
        }
        
        System.out.println("[CREATE-OBJECTS DEBUG] Total objects created: " + objects.size());

        if (rowNumber == 0) {
            dataFile.getLogger().printWarningById("GBL_00040");
            // System.out.println("[WARNING] BaseGenerator: no record of size greater than zero has been provided for ingestion.");
            return;
        }

        Map<String, Integer> mapStats = new HashMap<String, Integer>();
        for (HADatAcThing obj : objects) {
            String clsName = obj.getClass().getSimpleName();
            if (mapStats.containsKey(clsName)) {
                mapStats.put(clsName, mapStats.get(clsName) + 1);
            } else {
                mapStats.put(clsName, 1);
            }
        }
        String results = String.join(" and ", mapStats.entrySet().stream()
                .map(e -> e.getValue() + " " + e.getKey() + "(s)")
                .collect(Collectors.toList()));
        if (!results.isEmpty()) {
            logger.println(results + " have been created. ");
        }
    }

    @Override
    public String toString() {
        if (rows.isEmpty()) {
            return "";
        }
        String result = "";
        result = String.join(",", rows.get(0).keySet());
        for (Map<String, Object> row : rows) {
            List<String> values = new ArrayList<String>();
            for (String colName : rows.get(0).keySet()) {
                if (row.containsKey(colName)) {
                    values.add((String) row.get(colName));
                } else {
                    values.add("");
                }
            }
            result += "\n";
            result += String.join(",", values);
        }

        return result;
    }

    private void checkRows(List<Map<String, Object>> rows, String primaryKey) throws Exception {
        int i = 1;
        Set<String> values = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String val = (String) row.get(primaryKey);
            if (null == val) {
                throw new Exception(String.format("Found Row %d without URI specified!", i));
            }
            if (values.contains(val)) {
                throw new Exception(String.format("Duplicate Concepts in Inputfile row %d :" + val + " would be duplicate URIs!", i));
            } else {
                values.add(val);
            }

            i++;
        }
    }

    public boolean commitRowsToTripleStore(List<Map<String, Object>> rows) {
        System.out.println("\n========== BaseGenerator.commitRowsToTripleStore() START ==========");
        System.out.println("Generator type: " + this.getClass().getSimpleName());
        System.out.println("Element type: " + this.getElementType());
        System.out.println("Number of rows to commit: " + rows.size());
        System.out.println("Named graph URI: " + getNamedGraphUri());

        //System.out.println("BaseGenerator: commitRowsToTripleStore(): received values");
        //for (Map<String, Object> row : rows) {
        //    for (Map.Entry<String, Object> entry : row.entrySet()) {
        //        System.out.println("Row: " + entry.getKey() + ": " + entry.getValue());
        //    }
        //}
        //System.out.println("BaseGenerator: commitRowsToTripleStore(): getNamedGraphUri() is " + getNamedGraphUri());

        System.out.println("Creating RDF model from rows...");
        Model model = MetadataFactory.createModel(rows, getNamedGraphUri());

        System.out.println("Committing model to triple store...");
        int numCommitted = MetadataFactory.commitModelToTripleStore(
                model, CollectionUtil.getCollectionPath(
                        CollectionUtil.Collection.SPARQL_GRAPH));

        if (numCommitted > 0) {
            logger.println(String.format("%d triple(s) have been committed to triple store", model.size()));
            System.out.println("✓ Successfully committed " + model.size() + " triples");
        } else {
            System.out.println("✗ No triples committed (model size: " + (model != null ? model.size() : "null") + ")");
        }

        System.out.println("========== BaseGenerator.commitRowsToTripleStore() END ==========\n");

        return true;
    }

    public boolean commitObjectsToTripleStore(List<HADatAcThing> objects) {
        int count = 0;
        boolean withValidation = true;

        // Create a empty model
        ModelFactory modelFactory = new LinkedHashModelFactory();
        Model model = modelFactory.createEmptyModel();

        // Create a empty query (To StudyObjectCollections)
        List<String> query = new ArrayList<>();

        // Delete all objects triples from the named graph
        deleteObjectsFromTripleStore(objects);

        for (HADatAcThing obj : objects) {
            obj.setNamedGraph(getNamedGraphUri());
            System.out.println("[COMMIT DEBUG] Object class: " + obj.getClass().getSimpleName() + ", URI: " + obj.getUri());

            // FIX: Use .equals() instead of == for string comparison
            // The == operator compares object references, not string content
            if ("StudyObjectCollection".equals(obj.getClass().getSimpleName())) {
                System.out.println("[COMMIT DEBUG]   -> StudyObjectCollection path (no model)");
                obj.saveToTripleStore(false, false, null, query);
                count++; // Increment count for SOCs too!
            } else {
                System.out.println("[COMMIT DEBUG]   -> Regular path (with shared model)");
                boolean saved = obj.saveToTripleStore(withValidation, false, model);
                System.out.println("[COMMIT DEBUG]   -> saveToTripleStore returned: " + saved);
                if (saved) {
                    count++;
                }
            }

        }

        for (String name : caches.keySet()) {
            if (caches.get(name).getNeedCommit()) {
                //System.out.println("cache " + name + " size: Initial " + caches.get(name).getInitialCache().values().size());
                //System.out.println("cache " + name + " size: New " + caches.get(name).getNewCache().values().size());
                //System.out.println("cache " + name + " size: Total " + caches.get(name).getMapCache().values().size());
                for (Object obj : caches.get(name).getNewCache().values()) {
                    if (obj instanceof HADatAcThing) {
                        //System.out.println("BaseGenerator.commitObjectsToTriplestore() [2]");
                        ((HADatAcThing) obj).setNamedGraph(getNamedGraphUri());
                        ((HADatAcThing) obj).saveToTripleStore(withValidation, false, model);
                        count++;

                    }
                }
            }
        }

        int numCommitted = 0;
        if (count > 0) {
            logger.println(String.format("%d object(s) have been committed to triple store", count));

            if (model.size() > 0) {
                numCommitted += MetadataFactory.commitModelToTripleStore(
                        model, CollectionUtil.getCollectionPath(
                                CollectionUtil.Collection.SPARQL_GRAPH));
            }

            // Commit StudyObjectCollection updates (they queue SPARQL UPDATE strings)
            if (query != null && !query.isEmpty()) {
                for (String q : query) {
                    if (q == null) {
                        continue;
                    }
                    String qTrim = q.trim();
                    if (qTrim.isEmpty()) {
                        continue;
                    }
                    // Some generators may already include the closing braces.
                    if (!qTrim.endsWith("}")) {
                        qTrim = qTrim + "}";
                    }
                    updateTripleStore(qTrim);
                }
            }

            // Post-commit verification: make sure objects are actually present in the triple store.
            boolean allFound = true;
            for (HADatAcThing obj : objects) {
                if (obj == null || obj.getUri() == null || obj.getUri().trim().isEmpty()) {
                    continue;
                }
                String verificationGraph = getVerificationGraphFor(obj);
                if (!existsInTripleStore(obj.getUri(), verificationGraph)) {
                    allFound = false;
                    if (dataFile != null && dataFile.getLogger() != null) {
                        dataFile.getLogger().println("[ERROR] Post-commit verification failed: object not found in triplestore: <" + obj.getUri() + "> (namedGraph=<" + verificationGraph + ">) ");
                    } else if (logger != null) {
                        logger.println("[ERROR] Post-commit verification failed: object not found in triplestore: <" + obj.getUri() + "> (namedGraph=<" + verificationGraph + ">) ");
                    }
                }
            }
            if (!allFound) {
                return false;
            }
        }

        return true;
    }

    private String getVerificationGraphFor(HADatAcThing obj) {
        if (obj == null) {
            return getNamedGraphUri();
        }
        // Some objects (notably StudyObjectCollection / SOC* helpers) are stored in DEFAULT_REPOSITORY
        // by their own implementation. When we verify, we must check the same graph.
        String cls = obj.getClass().getSimpleName();
        if ("StudyObjectCollection".equals(cls) || "SOCGroup".equals(cls) || "StudyRole".equals(cls)) {
            return Constants.DEFAULT_REPOSITORY;
        }
        return getNamedGraphUri();
    }

    private boolean existsInTripleStore(String uri, String preferredGraph) {
        try {
            String ns = NameSpaces.getInstance().printSparqlNameSpaceList();

            String service = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);

            // Prefer checking inside the graph where the object should be stored.
            String namedGraph = preferredGraph;
            if (namedGraph != null && !namedGraph.trim().isEmpty()) {
                String askInGraph = ns + "ASK { GRAPH <" + namedGraph + "> { <" + uri + "> ?p ?o } }";
                Boolean bGraph = execAsk(service, askInGraph);
                if (bGraph != null) {
                    return bGraph;
                }
            }

            // Fallback: check across all graphs (keeps backward compatibility).
            String askAny = ns + "ASK { <" + uri + "> ?p ?o }";
            Boolean bAny = execAsk(service, askAny);
            if (bAny != null) {
                return bAny;
            }

            // Last fallback: minimal SELECT (any graph).
            String q2 = ns + "SELECT ?p WHERE { <" + uri + "> ?p ?o } LIMIT 1";
            ResultSetRewindable rs2 = SPARQLUtils.select(service, q2);
            return rs2 != null && rs2.hasNext();
        } catch (Exception e) {
            // If verification fails (endpoint down), be conservative and report failure.
            return false;
        }
    }

    private Boolean execAsk(String sparqlService, String askQueryString) {
        try {
            org.apache.jena.query.Query q = org.apache.jena.query.QueryFactory.create(askQueryString);
            try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlService, q)) {
                return qexec.execAsk();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public void deleteRowsFromTripleStore(List<Map<String, Object>> rows) {
        Model model = MetadataFactory.createModel(rows, "");

        Repository repo = new SPARQLRepository(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE));
        repo.init();

        RepositoryConnection con = repo.getConnection();
        con.remove(model);

        String namedGraph = !this.getNamedGraphUri().isEmpty() ? this.getNamedGraphUri() : this.getStudyUri();
        dropGraph(namedGraph);
    }

    public void deleteObjectsFromTripleStore(List<HADatAcThing> objects) {
        int totalUri = 0;
        String query = "";
        String queryHeader;
        String queryGraphHeader;
        List<String> queries;

        // Group objects uri by named graph
        Map<String, List<String>> objectsUriByNamedGraph = new HashMap<>();
        for (HADatAcThing obj : objects) {
            String namedGraph = obj.getNamedGraph();
            String uri = obj.getUri();

            // Adjust the uri to be a valid URI
            if (uri == null || uri.equals("")) {
                continue;
            } else if (uri.startsWith("http")) {
                uri = "<" + uri + ">";
            }

            // Set the namedGraph if it is not set
            if (namedGraph == null || namedGraph.equals("")) {
                if (RepositoryInstance.getInstance() != null &&
                        RepositoryInstance.getInstance().getHasDefaultNamespaceURL() != null) {
                    namedGraph = RepositoryInstance.getInstance().getHasDefaultNamespaceURL();
                } else {
                    namedGraph = Constants.DEFAULT_REPOSITORY;
                }
            }

            // Add namedGraph as key
            objectsUriByNamedGraph.putIfAbsent(namedGraph, new ArrayList<>());

            // Get the namedGraph value list
            List<String> uris = objectsUriByNamedGraph.get(namedGraph);

            // Append uri to the respective list
            uris.add(uri);

        }

        // Start a query list
        queries = new ArrayList<String>();

        // Init the query with namespaces
        queryHeader = NameSpaces.getInstance().printSparqlNameSpaceList();

        // Set the query action
        queryHeader += "DELETE WHERE { \n";

        for (Map.Entry<String, List<String>> entry : objectsUriByNamedGraph.entrySet()) {
            // Get the namedGraph and the uri list
            String namedGraph = entry.getKey();
            List<String> uris = entry.getValue();

            // Concatenate query header to GRAPH header
            query = queryGraphHeader = "\n    GRAPH <" + namedGraph + "> { \n";

            // Init a controller counter
            int controllerCounter = 0;

            for (String uri : uris) {

                // Restart query
                if (controllerCounter == QUERY_LIMIT) {
                    // Close current query and add to query list
                    queries.add(
                            queryHeader + query + "\n\n    } \n\n}  "
                    );

                    // Restart current query
                    query = queryGraphHeader;

                    //System.out.println("[WARNING] Query limit reached (" + QUERY_LIMIT + ")");

                    totalUri += controllerCounter;

                    // Restart controllerCounter
                    controllerCounter = 0;
                }

                // Add row aassociated to respective URI
                query += "\n        " + uri + " ?p ?o . ";

                // Update controller counter
                controllerCounter++;
            }

            // Update the total URI counter
            totalUri += controllerCounter;

            // Close the current GRAPH query
            query += "\n\n    } \n";
        }
        // Finalize the query
        query += "\n}  ";

        // Add the header and append to query list
        queries.add(queryHeader + query);

        // Looping through the query list
        for (String query_i : queries) {
            // Update respective query into the triple store
            updateTripleStore(query_i);
        }

        // Show the total URI
        //System.out.println("[INFO] BaseGenerator: (deleteObjectsFromTripleStore) Total URI deleted from triplestore: " + totalUri);
    }

    private void dropGraph(String namedGraphUri) {
        String dropGraph = "DROP GRAPH <" + namedGraphUri + ">";
        updateTripleStore(dropGraph);
    }

    public void updateTripleStore(String query) {
        //System.out.println("Query: " + query);
        // Create a request
        UpdateRequest request = UpdateFactory.create(query);

        // System.out.println("\n\n(updateTripleStore) Query: " + query + "\n\n ");

        // Create a processor to execute the request
        UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                request, CollectionUtil.getCollectionPath(
                        CollectionUtil.Collection.SPARQL_UPDATE
                )
        );

        try {
            // Apply query to update triplestore
            processor.execute();
        } catch (QueryParseException e) {
            System.out.println(
                    "[WARNING] QueryParseException due to update query: " + query
            );
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package org.hascoapi.ingestion;

import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hascoapi.Constants;
import org.hascoapi.RepositoryInstance;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.HADatAcThing;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.MetadataFactory;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.IngestionLogger;
import org.hascoapi.vocabularies.VSTOI;

import org.eclipse.rdf4j.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QueryParseException;


public class INSCleanerGenerator extends BaseGenerator {

    protected IngestionLogger logger = null;

    protected String instrumentUri = "";
    protected String hasStatus = "";

    public String getInstrumentUri() {
        return this.instrumentUri;
    }

    public void setInstrumentUri(String instrumentUri) {
        this.instrumentUri = instrumentUri;
    }

    public String getHasStatus() {
        return this.hasStatus;
    }

    public void setHasStatus(String hasStatus) {
        this.hasStatus = hasStatus;
    }

    public INSCleanerGenerator(String elementType, DataFile dataFile, String hasStatus) {
        super(dataFile);
        this.setElementType(elementType);
        this.setHasStatus(hasStatus);
        logger = dataFile.getLogger();
    }

    @Override
    public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
        Map<String, Object> row = new HashMap<String, Object>();

        for (String header : file.getHeaders()) {
            if (!header.trim().isEmpty()) {
                String value = rec.getValueByColumnName(header);
                if (value != null && !value.isEmpty()) {
                    //System.out.println("Header: [" + header + "] Value: [" + value + "]");
                    row.put(header, value);
                }
            }
        }

        if (this.getElementType().equals("cleaner")) {
            //row.put("hasco:hascoType", VSTOI.INSTRUMENT);
            if (this.getHasStatus() != null && !this.getHasStatus().equals("_")) {
                row.put("vstoi:hasStatus", this.getHasStatus());
            }
            row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
        }

        if (row.containsKey("hasURI") && !row.get("hasURI").toString().trim().isEmpty()) {
            return row;
        }

        return null;
    }

    @Override
    public String getTableName() {
        return "INS";
    }

    @Override
    public String getErrorMsg(Exception e) {
        return "Error in INSGenerator: " + e.getMessage();
    }

    @Override
    public void preprocessuris(Map<String,String> uris) throws Exception {
        if (this.getElementType().equals("instrument")) {
            this.setInstrumentUri(uris.get("instrumentUri"));
        }
    }

    @Override
    public boolean commitRowsToTripleStore(List<Map<String, Object>> rows) {
        System.out.println("INSCleanerGenerator: commitRowsToTripleStore(): deleteRowsFromTripleStore()");

        // Delete all the same rows from the named graph
        deleteURIandDependencies(rows);

        return true;
    }

    public void deleteURIandDependencies(List<Map<String, Object>> rows) {
        int totalUri = 0;
        List<String> queries;

        // Group objects uri by named graph
        Map<String, List<String>> uriByNamedGraph = new HashMap<>();
        for (Map<String, Object> row : rows) {
            // String namedGraph = row.getNamedGraph();
            String uri = row.get("hasURI").toString();
            //String hascoType = row.get("hasco:hascoType").toString();

            // Adjust the uri to be a valid URI
            if (uri == null || uri.equals("")) {
                continue;
            } else if (uri.startsWith("http")) {
				//System.out.println("\n\nuri: " + uri + "\n\n ");
                uri = "<" + uri + ">";
				//System.out.println("\n\nuri: " + uri + "\n\n ");
            } else if (uri.contains(":")) {
                uri = URIUtils.replacePrefix(uri);
            }

			// Get the named graphs that contains uri
			List<String> uriNamedGraphs = findNamedGraphUri(uri);

            for (String namedGraph : uriNamedGraphs) {
                // Add namedGraph as key
                uriByNamedGraph.putIfAbsent(namedGraph, new ArrayList<>());

                // Append uri if not already in the list
                if (! uriByNamedGraph.get(namedGraph).contains(uri)) {
                    // Add uri to the namedGraph
                    uriByNamedGraph.get(namedGraph).add(uri);

                    // // Check who depends of these uri
                    // List<String> dependUris = findDepends(namedGraph, uri);

                    // if (dependUris.size() > 0) {
                    //     // Add depend uri to the namedGraph
                    //     for (String dependUri : dependUris) {
                    //         if (! uriByNamedGraph.get(namedGraph).contains(dependUri)) {
                    //             uriByNamedGraph.get(namedGraph).add(dependUri);
                    //         }
                    //     }
                    // }
                }
            }

            // // Verify if there is dependUri in other namedgraphs
            // for (String dependUri : dependUris) {
            //     // Get the named graphs that contains depend
            //     uriNamedGraphs = findNamedGraphUri(dependUri);

            //     for (String namedGraph : uriNamedGraphs) {
            //         // Add namedGraph as key
            //         uriByNamedGraph.putIfAbsent(namedGraph, new ArrayList<>());

            //         // Check if the depend is in the list
            //         if (! uriByNamedGraph.get(namedGraph).contains(dependUri)) {
            //             // Add depend to the namedGraph
            //             uriByNamedGraph.get(namedGraph).add(dependUri);
            //         }
            //     }
            // }
        }

        // Generate the DELETE queries
        queries = generateQueries("DELETE", uriByNamedGraph, QUERY_LIMIT);

        // Turn total uri from string to integer
        totalUri = Integer.parseInt(queries.get(queries.size() - 1));

        if (totalUri > 0) {
            // Looping through the query list
            for (String query_i : queries.subList(0, queries.size() - 1)) {
                System.out.println(query_i);
                // Update respective query into the triple store
                updateTripleStore(query_i);
            }
        }

        // Show the total URI
        String message = "[INFO] INSCleanerGenerator: (deleteURIandDependencies) Total URI deleted from triplestore: " + totalUri;
        System.out.println(message);
        logger.println(totalUri + " URI deleted from triplestore");
    }

    public List<String> generateQueries(String action, Map<String, List<String>> uriByNamedGraph, int queryLimit) {
        int totalUri = 0;
        List<String> queries = new ArrayList<>();
        String queryHeader;
        String queryGraphHeader;
        String query = "";

        // Start a query list
        queries = new ArrayList<String>();

        // Init the query with namespaces
        queryHeader = NameSpaces.getInstance().printSparqlNameSpaceList();

        if (action.toUpperCase() != "DELETE") {
            return null;
        }

        // Set the query action
        queryHeader += action.toUpperCase() + " WHERE { \n";

        for (Map.Entry<String, List<String>> entry : uriByNamedGraph.entrySet()) {
            // Get the namedGraph and the uri list
            String namedGraph = entry.getKey();
            List<String> uris = entry.getValue();

            // Concatenate query header to GRAPH header
            query = queryGraphHeader = "\n    GRAPH " + namedGraph + " { \n";

            // Init a controller counter
            int queryCounter = 0;

            for (String uri : uris) {

                // Restart query
                if (queryCounter == queryLimit){
                    // Close current query and add to query list
                    queries.add(
                        queryHeader + query + "\n\n    } \n\n}  "
                    );

                    // Restart current query
                    query = queryGraphHeader;

                    //System.out.println("[WARNING] Query limit reached (" + QUERY_LIMIT + ")");

                    totalUri += queryCounter;

                    // Restart controllerCounter
                    queryCounter = 0;
                }

                // Add row aassociated to respective URI
                query += "\n        " + uri + " ?p ?o . ";

                // Update controller counter
                queryCounter++;
            }

            // Update the total URI counter
            totalUri += queryCounter;

            // Close the current GRAPH query
            query += "\n\n    } \n";
        }
        // Finalize the DELETE query
        query += "\n}  ";

        // Add the header and append to query list
        queries.add(queryHeader + query);

        // Add total URI as string
        queries.add(totalUri + "");

        return queries;
    }

    public List<String> findNamedGraphUri(String uri) {
        String namedGraphUri = "";
		int counter = 0;
		List<String> namedGraphs = new ArrayList<>();

		String query = "SELECT ?g WHERE { GRAPH ?g { " + uri + " ?p ?o } }";

		ResultSetRewindable resultsrw = SPARQLUtils.select(
			CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

		while (resultsrw.hasNext()) {
			try {
				QuerySolution solution = resultsrw.next();
				RDFNode graphNode = solution.get("g");
				namedGraphUri = graphNode.asResource().getURI();

                if ( !namedGraphUri.startsWith("<") ){
                    namedGraphUri = "<" + namedGraphUri + ">";
                }

				// Check if the list already contains the value
				if (!namedGraphs.contains(namedGraphUri)) {
					namedGraphs.add(namedGraphUri);
                    System.out.println("INSCleanerGenerator: findNamedGraphUri(): " + namedGraphs + "[" + namedGraphs.contains(namedGraphUri.trim()) + "]");
					// Count named graph found
					counter++;
				}

			} catch (QueryParseException e) {
				System.out.println(
					"[WARNING] QueryParseException due to get next result"
				);
				throw e;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//System.out.println("INSCleanerGenerator: findNamedGraphUri(): Number of named graphs found: " + counter);

        return namedGraphs;
    }

    public List<String> findDepends(String namedGraph, String uri){
		List<String> uris = new ArrayList<>();
        String nodeUri = "";
        int counter = 0;

		String query = "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX  vstoi: <http://hadatac.org/ont/vstoi#>\n";

        query += String.format(
            "SELECT ?s WHERE {\n GRAPH %s {\n "
                + "{ ?s rdfs:subClassOf %s } " 
                + "UNION { ?s vstoi:belongsTo %s } "
                + "\n}\n}",
            namedGraph, uri, uri
        );

		ResultSetRewindable resultsrw = SPARQLUtils.select(
			CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query
        );

		while (resultsrw.hasNext()) {
			try {
				QuerySolution solution = resultsrw.next();
				RDFNode graphNode = solution.get("s");
				nodeUri = graphNode.asResource().getURI();

				// Check if the list already contains the value
				if (!uris.contains(nodeUri) && !nodeUri.equals(uri)) {
					uris.add(
                        "<" + nodeUri + ">"
                    );
					// Count named graph found
					counter++;
				}

			} catch (QueryParseException e) {
				System.out.println(
					"[WARNING] QueryParseException due to get next result"
				);
				throw e;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

        // Make a copy of the uris list
        List<String> _uris = new ArrayList<>();
        _uris.addAll(uris);

        for (String _uri : _uris) {
            // Check subsequent dependencies
            uris.addAll(findDepends(namedGraph, _uri));
        }

        return uris;
    }
}

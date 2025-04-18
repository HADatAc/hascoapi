package org.hascoapi.entity.pojo;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URL;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.*;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.hascoapi.utils.GSPClient;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.Constants;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.utils.NameSpaces;

public class NameSpace extends HADatAcThing implements Comparable<NameSpace> {

    static String className = HASCO.MANAGED_ONTOLOGY;

    @PropertyField(uri="hasco:hasSource")
    private String nsSource = "";

    @PropertyField(uri="hasco:hasSourceMime")
    private String nsSourceMime = "";

    @PropertyField(uri="vstoi:hasVersion")
    private String version = "";

    @PropertyField(uri="vstoi:hasPriority")
    private String priority = "-1";

    private int numberOfLoadedTriples = 0;

    private boolean permanent = false;

    public NameSpace () {
    }

    public NameSpace (String abbrev, String name, String sourceMime, String source, String comment, String version, int priority) {
        this.label = abbrev;
        this.uri = name;
        this.nsSource = source;
        this.nsSourceMime = sourceMime;
        this.comment = comment;
        this.version = version;
        this.priority = Integer.toString(priority);
    }

    // setURI MUST OVERRIDE the method in HADatAcThing since the method in HADatAcThing uses the list of existing namespaces
    @Override
    public void setUri(String uri) {
        if (uri == null || uri.equals("")) {
            this.uri = "";
            return;
        }
        this.uri = uri;
    }

    public String getSourceMime() {
        return nsSourceMime;
    }
    public void setSourceMime(String sourceMime) {
        nsSourceMime = sourceMime;
    }

    public String getVersion() {
        return this.version;
    }
    public void setVersion(String version) {
        this.version = version;
    }

    public void setPriority(int priority) {
        this.priority = Integer.toString(priority);
    }
    public void setPriority(String priorityStr) {
        this.priority = priorityStr;
    }
    public int getPriority() {
        return Integer.parseInt(priority);
    }

    public String getSource() {
        return this.nsSource;
    }
    public void setSource(String source) {
        this.nsSource = source;
    }

    public int getNumberOfLoadedTriples() {
        return this.numberOfLoadedTriples;
    }
    public void setNumberOfLoadedTriples() {
        try {
            String queryString = "SELECT (COUNT(*) as ?tot) \n"
                    + "FROM <" + getUri() + "> \n"
                    + "WHERE { ?s ?p ?o . } \n";

            ResultSetRewindable resultsrw = SPARQLUtils.select(CollectionUtil.getCollectionPath(
                    CollectionUtil.Collection.SPARQL_QUERY), queryString);
            QuerySolution soln = resultsrw.next();

            this.numberOfLoadedTriples = Integer.valueOf(soln.getLiteral("tot").getValue().toString()).intValue();
        } catch (Exception e) {
            System.out.println("NameSpace.updateLoadedTripleSize()");
            System.out.println("  - Value of CollectionUtil.Collection.SPARQL_QUERY=[" + CollectionUtil.Collection.SPARQL_QUERY + "]");
            System.out.println("  - Value of CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY)=[" + CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY) + "]");
            e.printStackTrace();
        }
    }

    public boolean getPermanent() {
        return this.permanent;
    }
    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }

    public String toString() {
        if (label == null) {
            return "null";
        }
        String showType = "null";
        if (label != null)
            showType = nsSourceMime;
        if (nsSource == null)
            return "<" + label + ":> " + uri + " (" + showType + ", NO URL)";
        else
            return "<" + label + ":> " + uri + " (" + showType + ", " + nsSource + ")";
    }

    public List<String> getOntologyURIs() {
        List<String> uris = new ArrayList<String>();
        for(NameSpace ns : NameSpaces.getInstance().getOrderedNamespacesAsList())
            uris.add(ns.getUri());
        return uris;
    }

    public static List<NameSpace> findWithPages(int pageSize, int offset) {
        List<NameSpace> listOntologies = NameSpaces.getInstance().getOrderedNamespacesAsList();
        if (listOntologies == null || pageSize < 1 || offset < 0) {
            return new ArrayList<NameSpace>();
        }
        return listOntologies.subList(offset, offset + pageSize - 1);
    }

    public static int getNumberOntologies() {
        return NameSpaces.getInstance().getOrderedNamespacesAsList().size();
    }

    public static List<NameSpace> findInMemory() {
        return NameSpaces.getInstance().getOrderedNamespacesAsList();
    }

    public static HADatAcThing findInMemory(String uri) {
        for(NameSpace ns : NameSpaces.getInstance().getOrderedNamespacesAsList())
            if (ns.getSource().equals(uri)) {
                return (HADatAcThing) ns;
            }
        return null;
    }

    public static NameSpace findInMemoryByAbbreviation(String abbreviation) {
        for(NameSpace ns : NameSpaces.getInstance().getOrderedNamespacesAsList())
            if (ns.getLabel().equals(abbreviation)) {
                return ns;
            }
        return null;
    }

	public static NameSpace find(String uri) {
 		if (uri == null || uri.isEmpty()) {
			return null;
		}
		NameSpace ns = null;
		// Construct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
            ns = new NameSpace();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				ns.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);

				if (predicate.equals(RDFS.LABEL)) {
					ns.setLabel(object);
				} else if (predicate.equals(RDF.TYPE)) {
					ns.setTypeUri(object); 
				} else if (predicate.equals(HASCO.HASCO_TYPE)) {
					ns.setHascoTypeUri(object);
				} else if (predicate.equals(RDFS.COMMENT)) {
					ns.setComment(object);
				} else if (predicate.equals(HASCO.HAS_IMAGE)) {
					ns.setHasImageUri(object);
				} else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
					ns.setHasWebDocument(object);
				} else if (predicate.equals(HASCO.HAS_ABBREVIATION)) {
					ns.setLabel(object);
				} else if (predicate.equals(HASCO.HAS_SOURCE)) {
					ns.setSource(object);
				} else if (predicate.equals(HASCO.HAS_SOURCE_MIME)) {
					ns.setSourceMime(object);
				} else if (predicate.equals(VSTOI.HAS_PRIORITY)) {
					ns.setPriority(object);
				} else if (predicate.equals(VSTOI.HAS_VERSION)) {
         			ns.setVersion(object);
				}
			}
		}

        ns.setNumberOfLoadedTriples();

        ns.setUri(uri);
		
		return ns;
	}

    public static List<NameSpace> find() {
        String query =
            " SELECT ?uri ?graph WHERE { " +
            "    GRAPH ?graph {  " +
            "       ?uri  <" + HASCO.HASCO_TYPE + ">  <" + HASCO.ONTOLOGY + "> . " +
            "    } " +
            "} ";
        return findManyByQuery(query);
    }

    public static List<NameSpace> findManyByQuery(String query) {
        List<NameSpace> nss = new ArrayList<NameSpace>();

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            String uri = soln.getResource("uri").getURI();
            String graph = soln.getResource("graph").getURI();
            NameSpace ns = NameSpace.find(uri);
            ns.setNamedGraph(graph);
            nss.add(ns);
        }

        java.util.Collections.sort((List<NameSpace>) nss);
        return nss;
    }

    /* 
    public void updateFromTripleStore() {
        OntologyTripleStore ont = OntologyTripleStore.find(this.getUri());
        this.setComment(ont.getComment());
        this.setVersion(ont.getVersion());
        this.save();
    }
    */

    /* 
    public void updateNumberOfLoadedTriples() {
        try {
            String queryString = "SELECT (COUNT(*) as ?tot) \n"
                    + "FROM <" + getUri() + "> \n"
                    + "WHERE { ?s ?p ?o . } \n";

            ResultSetRewindable resultsrw = SPARQLUtils.select(CollectionUtil.getCollectionPath(
                    CollectionUtil.Collection.SPARQL_QUERY), queryString);
            QuerySolution soln = resultsrw.next();

            this.setNumberOfLoadedTriples(Integer.valueOf(soln.getLiteral("tot").getValue().toString()).intValue());
            this.save();
        } catch (Exception e) {
            System.out.println("NameSpace.updateLoadedTripleSize()");
            System.out.println("  - Value of CollectionUtil.Collection.SPARQL_QUERY=[" + CollectionUtil.Collection.SPARQL_QUERY + "]");
            System.out.println("  - Value of CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY)=[" + CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY) + "]");
            e.printStackTrace();
        }
    }
    */

    public void loadTriples(String address, boolean fromRemote) {
        Optional<File> tempFileOpt = Optional.empty();
        RDFFormat format = getRioFormat(getSourceMime());
        try {
            System.out.println("Namespace: Loading triples from " + address);
            File tripleFile;
            if (fromRemote) {
                tempFileOpt = Optional.of(File.createTempFile("remoteTriples", "." + format.getDefaultFileExtension()));

                tripleFile = tempFileOpt.get();
                FileUtils.copyURLToFile(new URL(address), tripleFile);            }
            else {
                tripleFile = new File(address);
            }
            String endpointUrl = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_GRAPH);
            GSPClient gspClient = new GSPClient(endpointUrl);
            if (address.equals("http://hadatac.org/ont/uberon/uberonpmsr.ttl")) {
                NameSpace.printFirst30Lines(tripleFile);
            }
            gspClient.postFile(tripleFile, format.getDefaultMIMEType(), getUri());
            System.out.println("Loaded triples from " + address + " \n");
            //System.out.println("Loaded triples from " + address + " \n");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            tempFileOpt.ifPresent(FileUtils::deleteQuietly);
        }
    }

    public static void printFirst30Lines(File file) {
        BufferedReader reader = null;

        try {
            // Create BufferedReader using the File object
            reader = new BufferedReader(new FileReader(file));
            String line;
            int lineCount = 0;

            // Read and print the first 30 lines
            while ((line = reader.readLine()) != null && lineCount < 30) {
                System.out.println(line);
                lineCount++;
            }

            // If the file contains fewer than 30 lines
            if (lineCount < 30) {
                System.out.println("The file contains fewer than 30 lines.");
            }
        } catch (Exception e) {
            System.out.println("Error reading the file: " + e.getMessage());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                System.out.println("Error closing the file: " + e.getMessage());
            }
        }
    }

    public void deleteTriples() {
        deleteTriplesByNamedGraph(getUri());
    }

    public static void deleteTriplesByNamedGraph(String namedGraphUri) {
        if (!namedGraphUri.isEmpty()) {
            String queryString = "";
            queryString += NameSpaces.getInstance().printSparqlNameSpaceList();
            queryString += "WITH <" + namedGraphUri + "> ";
            queryString += "DELETE { ?s ?p ?o } WHERE { ?s ?p ?o . } ";

            UpdateRequest req = UpdateFactory.create(queryString);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(req,
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE));
            try {
                processor.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
    public void saveWithoutURIValidation() {
        // permanent name spaces are not saved into the triple store
        if (!this.permanent) {
            System.out.println("Save: namespace without URI validation: " + this.namedGraph);
            System.out.println("   URI = [" + this.getUri() + "]");
            saveToTripleStore(false);
        }
     }
     */

    @Override
    public void save() {
        // permanent name spaces are not saved into the triple store
        if (!this.permanent) {
            // namespaces are always stored into the named graph called DEFAULT_REPOSITORY 
            this.setNamedGraph(Constants.DEFAULT_REPOSITORY);
            System.out.println("   URI = [" + this.getUri() + "]");
            saveToTripleStore(false);
        }
     }

    @Override
    public void delete() {
        // permanent name spaces cannot be deleted from triple store because they are not store into the triple store
        if (!this.permanent) {
            // namespaces are always stored into the named graph called DEFAULT_REPOSITORY 
            this.setNamedGraph(Constants.DEFAULT_REPOSITORY);
            System.out.println("   URI = [" + this.getUri() + "]");
            deleteFromTripleStore();
        }
    }

    public static String deleteNamespace(String abbreviation) {

        // RETRIEVE FROM MEMORY
        NameSpace ns = NameSpace.findInMemoryByAbbreviation(abbreviation);
        if (ns == null) {
            return "Could not find namespace with abbreviation [" + abbreviation + "] to be deleted.";
        }

        // DELETE FROM MEMORY
        //boolean response = NameSpaces.getInstance().deleteNamespace(abbreviation);
        //if (response == false) {
        //    return "When deleting from memory cache, could not find namespace with abbreviation [" + abbreviation + "] to be deleted.";
        //}
        //System.out.println(abbreviation + " deleted from memory");

        // DELETE FROM TRIPLE STORE
        // NameSpace.find() retrieves the namespace from triple store
        NameSpace forDeletion = NameSpace.find(ns.getUri());
        if (forDeletion != null) {
            forDeletion.delete();
            System.out.println(abbreviation + " deleted from triple store");
        }

        NameSpaces.resetNameSpaces();

        return "";
    }

    public static int deleteAll() {  return 0; }

    public static RDFFormat getRioFormat(String contentType) {
        if (contentType.contains("turtle")) {
            return RDFFormat.TURTLE;
        } else if (contentType.contains("rdf+xml")) {
            return RDFFormat.RDFXML;
        } else {
            return RDFFormat.NTRIPLES;
        }
    }

    @Override
    public int compareTo(NameSpace another) {
        return this.getLabel().compareTo(another.getLabel());
    }

}

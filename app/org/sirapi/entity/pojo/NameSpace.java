package org.sirapi.entity.pojo;

import java.io.File;
import java.net.URL;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.sirapi.utils.GSPClient;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.utils.NameSpaces;

public class NameSpace extends HADatAcThing {

    static String className = HASCO.MANAGED_ONTOLOGY;

    private String nsAbbrev = "";

    private String nsName = "";

    private String nsType = "";

    private String nsURL = "";

    private String nsComment = "";

    private String version = "";

    private int numberOfLoadedTriples = 0;

    private int priority = -1;

    public NameSpace () {
    }

    public NameSpace (String abbrev, String name, String type, String url, String comment, String version, int priority) {
        this.nsAbbrev = abbrev;
        this.nsName = name;
        this.nsType = type;
        this.nsURL = url;
        this.nsComment = comment;
        this.version = version;
        this.priority = priority;
    }

    public String getAbbreviation() {
        return nsAbbrev;
    }

    public void setAbbreviation(String abbrev) {
        nsAbbrev = abbrev;
    }

    @Override
    public String getLabel() {
        return nsAbbrev;
    }

    @Override
    public void setLabel(String abbrev) {
        nsAbbrev = abbrev;
    }

    public String getName() {
        return nsName;
    }

    public void setName(String name) {
        nsName = name;
        uri = name;
    }

    @Override
    public String getUri() {
        return nsName;
    }

    @Override
    public void setUri(String name) {
        nsName = name;
    }

    public String getMimeType() {
        return nsType;
    }

    public void setMimeType(String type) {
        nsType = type;
    }

    @Override
    public String getComment() {
        return nsComment;
    }

    @Override
    public void setComment(String comment) {
        nsComment = comment;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public String getURL() {
        return nsURL;
    }

    public void setURL(String url) {
        nsURL = url;
    }

    public int getNumberOfLoadedTriples() {
        return numberOfLoadedTriples;
    }

    public void setNumberOfLoadedTriples(int numberOfLoadedTriples) {
        this.numberOfLoadedTriples = numberOfLoadedTriples;
    }

    public String toString() {
        if (nsAbbrev == null) {
            return "null";
        }
        String showType = "null";
        if (nsType != null)
            showType = nsType;
        if (nsURL == null)
            return "<" + nsAbbrev + ":> " + nsName + " (" + showType + ", NO URL)";
        else
            return "<" + nsAbbrev + ":> " + nsName + " (" + showType + ", " + nsURL + ")";
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

    public static HADatAcThing find(String uri) {
        for(NameSpace ns : NameSpaces.getInstance().getOrderedNamespacesAsList())
            if (ns.getURL().equals(uri)) {
                return (HADatAcThing) ns;
            }
        return null;
    }

    public static NameSpace findByAbbreviation(String abbreviation) {
        for(NameSpace ns : NameSpaces.getInstance().getOrderedNamespacesAsList())
            if (ns.getAbbreviation().equals(abbreviation)) {
                return ns;
            }
        return null;
    }

    public static List<NameSpace> findAll() {
        return NameSpaces.getInstance().getOrderedNamespacesAsList();
    }

    public void updateFromTripleStore() {
        OntologyTripleStore ont = OntologyTripleStore.find(this.getUri());
        this.setComment(ont.getComment());
        this.setVersion(ont.getVersion());
        this.save();
    }

    public void updateNumberOfLoadedTriples() {
        try {
            String queryString = "SELECT (COUNT(*) as ?tot) \n"
                    + "FROM <" + getName() + "> \n"
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

    public void loadTriples(String address, boolean fromRemote) {
        Optional<File> tempFileOpt = Optional.empty();
        RDFFormat format = getRioFormat(getMimeType());
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
            gspClient.postFile(tripleFile, format.getDefaultMIMEType(), getName());
            System.out.println("Loaded triples from " + address + " \n");
            //System.out.println("Loaded triples from " + address + " \n");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            tempFileOpt.ifPresent(FileUtils::deleteQuietly);
        }
    }

    public void deleteTriples() {
        deleteTriplesByNamedGraph(getName());
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

    @Override
    public void save() { }

    @Override
    public boolean saveToSolr() { return true; }

    @Override
    public void delete() { }

    @Override
    public int deleteFromSolr() { return 0; }

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
}

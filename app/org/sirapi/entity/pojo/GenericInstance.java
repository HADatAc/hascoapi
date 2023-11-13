package org.sirapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.NameSpaces;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.vocabularies.RDF;
import org.sirapi.vocabularies.RDFS;

public class GenericInstance extends HADatAcThing implements Comparable<GenericInstance> {

    public GenericInstance(String uri,
                           String typeUri,
                           String hascoTypeUri,
                           String label,
                           String comment) {
        this.uri = uri;
        this.typeUri = typeUri;
        this.hascoTypeUri = typeUri;
        this.label = label;
        this.comment = comment;
    }

    public GenericInstance() {
        this.uri = "";
        this.typeUri = "";
        this.hascoTypeUri = "";
        this.label = "";
        this.comment = "";
    }

    public String getTypeLabel() {
        PlatformType pltType = PlatformType.find(getTypeUri());
        if (pltType == null || pltType.getLabel() == null) {
            return "";
        }
        return pltType.getLabel();
    }

    public static GenericInstance find(String uri) {
        GenericInstance instance = null;
        Model model;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
        model = qexec.execDescribe();
        //System.out.println("GenericInstance.find() [1]");

        StmtIterator stmtIterator = model.listStatements();
        if (!stmtIterator.hasNext()) {
            return instance;
        }

        instance = new GenericInstance();
        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                instance.setLabel(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                instance.setTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                instance.setHascoTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                instance.setComment(object.asLiteral().getString());
            }
        }

        instance.setUri(uri);

        return instance;
    }

    public static int getNumberGenericInstances(String requiredClass) {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?model rdfs:subClassOf* " + requiredClass + " . " +
                " ?uri a ?model ." +
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

    public static String jsonInstanceStatisticsByType(String requiredClass) {
        String result = "[['Model', 'Quantity']";
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select ?modelName (count(?uri) as ?tot) where { " +
                " ?model rdfs:subClassOf* " + requiredClass + " . " +
                " ?model rdfs:label ?modelName . " +
                " ?uri a ?model ." +
                " } " +
                " GROUP BY ?modelName ";
        //System.out.println(query);
        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

            int i = 0;
            String n = "";
            while (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                i = Integer.parseInt(soln.getLiteral("tot").getString());
                n = soln.getLiteral("modelName").getString();
                if (n != null && !n.isEmpty()) {
                    result = result + ", ['" + n + "'," + i + "]";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        result = result + "]";
        return result;
    }

    public static List<GenericInstance> findGenericWithPages(String requiredClass, int pageSize, int offset) {
        List<GenericInstance> instances = new ArrayList<GenericInstance>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?model rdfs:subClassOf* " + requiredClass + " . " +
                " ?uri a ?model . } " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI() != null) {
                GenericInstance instance = GenericInstance.find(soln.getResource("uri").getURI());
                instances.add(instance);
            }
        }
        return instances;
    }

    @Override
    public int compareTo(GenericInstance another) {
        return this.getLabel().compareTo(another.getLabel());
    }

    @Override
    public boolean saveToSolr() {
        return false;
    }

    @Override
    public int deleteFromSolr() {
        return 0;
    }

}

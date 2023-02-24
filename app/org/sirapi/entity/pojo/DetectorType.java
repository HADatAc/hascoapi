package org.sirapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonFilter;
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
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.NameSpaces;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.VSTOI;

@JsonFilter("detectorTypeFilter")
public class DetectorType extends HADatAcClass implements Comparable<DetectorType> {

    static String className = "vstoi:Detector";

    private String url;

    public DetectorType () {
        super(className);
    }

    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;
    }

    public String getSuperLabel() {
        DetectorType superInsType = DetectorType.find(getSuperUri());
        if (superInsType == null || superInsType.getLabel() == null) {
            return "";
        }
        return superInsType.getLabel();
    }


    public static List<DetectorType> find() {
        List<DetectorType> detectorTypes = new ArrayList<DetectorType>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?uri rdfs:subClassOf* " + className + " . " +
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            DetectorType detectorType = find(soln.getResource("uri").getURI());
            detectorTypes.add(detectorType);
        }

        java.util.Collections.sort((List<DetectorType>) detectorTypes);
        return detectorTypes;

    }

    public static Map<String,String> getMap() {
        List<DetectorType> list = find();
        Map<String,String> map = new HashMap<String,String>();
        for (DetectorType typ: list)
            map.put(typ.getUri(),typ.getLabel());
        return map;
    }

    public static DetectorType find(String uri) {
        DetectorType detectorType = null;
        Model model;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
        model = qexec.execDescribe();

        detectorType = new DetectorType();
        StmtIterator stmtIterator = model.listStatements();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                detectorType.setLabel(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_WEB_DOCUMENTATION)) {
                detectorType.setURL(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(RDFS.SUBCLASS_OF)) {
                detectorType.setSuperUri(object.asResource().getURI());
            }
        }

        detectorType.setUri(uri);
        detectorType.setLocalName(uri.substring(uri.indexOf('#') + 1));

        return detectorType;
    }

    public static int getNumberDetectorTypes() {
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT (count(?detectorType) as ?tot) WHERE { " +
                " ?detectorType rdfs:subClassOf* " + className + " . " +
                "} ";

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

    @Override
    public int compareTo(DetectorType another) {
        if (this.getLabel() != null && another.getLabel() != null) {
            return this.getLabel().compareTo(another.getLabel());
        }
        return this.getLocalName().compareTo(another.getLocalName());
    }

}

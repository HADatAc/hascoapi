package org.hascoapi.entity.pojo;

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
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.vocabularies.OWL;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;

public class OntologyTripleStore {

    static String className = "owl:Ontology";

    private String uri;

    private String typeUri;

    private String label;

    private String version;

    private String comment;

    public OntologyTripleStore(String uri) {
        this.uri = uri;
        this.typeUri = uri;
        this.label = "";
        this.version = "";
        this.comment = "";
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTypeUri() {
        return typeUri;
    }

    public void setTypeUri(String typeUri) {
        this.typeUri = typeUri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public static OntologyTripleStore find(String uri) {
        OntologyTripleStore ontology = null;
        Model model;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        //System.out.println(queryString);
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
        model = qexec.execDescribe();

        ontology = new OntologyTripleStore(uri);
        StmtIterator stmtIterator = model.listStatements();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                ontology.setLabel(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                ontology.setTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                ontology.setComment(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(OWL.VERSION_IRI)) {
                ontology.setVersion(object.asResource().getURI());
            }
        }

        ontology.setUri(uri);

        return ontology;
    }

    public static String getVersionFromAbbreviation(String abbr) {
        NameSpaces nss = NameSpaces.getInstance();
        String uri = nss.getNameByAbbreviation(abbr).replace("#", "");
        //System.out.println("OntVersion: " + uri);
        if (uri == null || uri.isEmpty()) {
            return "";
        }
        OntologyTripleStore ont = OntologyTripleStore.find(uri);
        if (ont == null || ont.getVersion() == null) {
            return "";
        }
        return ont.getVersion();
    }

    public static List<OntologyTripleStore> find() {
        List<OntologyTripleStore> ontologies = new ArrayList<OntologyTripleStore>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?ont rdfs:subClassOf* owl:Ontology . " +
                " ?uri a ?ont ." +
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            OntologyTripleStore ontology = find(soln.getResource("uri").getURI());
            ontologies.add(ontology);
        }

        return ontologies;
    }

}

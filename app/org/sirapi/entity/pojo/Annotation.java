package org.sirapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.sirapi.annotations.PropertyField;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.NameSpaces;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.RDF;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.VSTOI;

import java.util.ArrayList;
import java.util.List;

@JsonFilter("annotationFilter")
public class Annotation extends HADatAcThing implements Comparable<Annotation>  {

    @PropertyField(uri="vstoi:belongsTo")
    private String belongsTo;

    @PropertyField(uri="vstoi:hasAnnotationStem")
    private String hasAnnotationStem;

    @PropertyField(uri="vstoi:hasPosition")
    private String hasPosition;

    @PropertyField(uri="vstoi:hasStyle")
    private String hasStyle;

    public String getBelongsTo() {
        return belongsTo;
    }

    public void setBelongsTo(String belongsTo) {
        this.belongsTo = belongsTo;
    }

    public String getHasAnnotationStem() {
        return hasAnnotationStem;
    }

    public void setHasAnnotationStem(String hasAnnotationStem) {
        this.hasAnnotationStem = hasAnnotationStem;
    }

    public AnnotationStem getAnnotationStem() {
        if (hasAnnotationStem == null || hasAnnotationStem.equals("")) {
            return null;
        }
        AnnotationStem annotationStem = AnnotationStem.find(hasAnnotationStem);
        return annotationStem;
    }

    public String getHasPosition() {
        return hasPosition;
    }

    public void setHasPosition(String hasPosition) {
        this.hasPosition = hasPosition;
    }

    public String getHasStyle() {
        return hasStyle;
    }

    public void setHasStyle(String hasStyle) {
        this.hasStyle = hasStyle;
    }

    public static int getNumberAnnotationsByInstrument(String instrumentUri) {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?type rdfs:subClassOf* vstoi:Annotation . " +
                " ?uri a ?type ." +
                " ?uri vstoi:belongsTo <" + instrumentUri + ">. " +
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

    public static List<Annotation> findByInstrumentWithPages(String instrumentUri, int pageSize, int offset) {
        List<Annotation> annotations = new ArrayList<Annotation>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* vstoi:Annotation . " +
                " ?uri a ?type . } " +
                " ?uri vstoi:belongsTo <" + instrumentUri + ">. " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI() != null) {
                Annotation annotation = Annotation.find(soln.getResource("uri").getURI());
                annotations.add(annotation);
            }
        }
        return annotations;
    }

    public static List<Annotation> findByInstrument(String instrumentUri) {
        //System.out.println("findByInstrument: [" + instrumentUri + "]");
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* vstoi:Annotation . " +
                " ?uri a ?type ." +
                " ?uri vstoi:belongsTo <" + instrumentUri + ">. " +
                "} ";

        return findByQuery(queryString);
    }

    private static List<Annotation> findByQuery(String queryString) {
        List<Annotation> annotations = new ArrayList<Annotation>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            Annotation Annotation = find(soln.getResource("uri").getURI());
            annotations.add(Annotation);
        }

        java.util.Collections.sort((List<Annotation>) annotations);
        return annotations;

    }

    public static Annotation find(String uri) {
        Annotation Annotation = null;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

        Annotation = new Annotation();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                Annotation.setLabel(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                Annotation.setTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                Annotation.setComment(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                Annotation.setHascoTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.BELONGS_TO)) {
                Annotation.setBelongsTo(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_ANNOTATION_STEM)) {
                Annotation.setHasAnnotationStem(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_POSITION)){
                Annotation.setHasPosition(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STYLE)){
                Annotation.setHasStyle(object.asLiteral().getString());
            }
        }

        Annotation.setUri(uri);

        return Annotation;
    }

    /*
    static public boolean createAnnotation(String instrumentUri, String annotationUri, String position, String style, String hasAnnotationStem) {
        if (instrumentUri == null || instrumentUri.isEmpty()) {
            return false;
        }
        if (position == null || position.isEmpty()) {
            return false;
        }
        Annotation annotation = new Annotation();
        annotation.setUri(annotationUri);
        annotation.setLabel("Annotation " + position);
        annotation.setTypeUri(VSTOI.ANNOTATION);
        annotation.setHascoTypeUri(VSTOI.ANNOTATION);
        annotation.setComment("Annotation " + position + " of instrument with URI " + instrumentUri);
        annotation.setHasAnnotationStem(hasAnnotationStem);
        annotation.setBelongsTo(instrumentUri);
        annotation.setHasPosition(position);
        annotation.setHasStyle(style);
        if (hasAnnotationStem != null) {
            annotation.setHasAnnotationStem(hasAnnotationStem);
        }
        annotation.save();
        //System.out.println("Annotation.createAnnotationStemSlot: creating Annotation with URI [" + annotationUri + "]" );
        return true;
    }
    */

   /*
    public boolean updateAnnotationAnnotationStem(String hasAnnotationStem) {
        Annotation newAnnotation = new Annotation();
        newAnnotation.setUri(this.uri);
        newAnnotation.setLabel(this.getLabel());
        newAnnotation.setTypeUri(this.getTypeUri());
        newAnnotation.setComment(this.getComment());
        newAnnotation.setHascoTypeUri(this.getHascoTypeUri());
        newAnnotation.setBelongsTo(this.getBelongsTo());
        newAnnotation.setHasPosition(this.getHasPosition());
        newAnnotation.setHasStyle(this.getHasStyle());
        if (hasAnnotationStem != null && !hasAnnotationStem.isEmpty()) {
            newAnnotation.setHasAnnotationStem(hasAnnotationStem);
        }
        this.delete();
        newAnnotation.save();
        return true;
    }
    */


    @Override
    public int compareTo(Annotation another) {
        return this.getHasPosition().compareTo(another.getHasPosition());
    }

    @Override public void save() {
        saveToTripleStore();
    }

    @Override public void delete() {
        deleteFromTripleStore();
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

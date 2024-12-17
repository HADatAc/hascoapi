package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;

import java.util.ArrayList;
import java.util.List;

@JsonFilter("publicationFilter")
public class Publication extends DataFile {

    public static Publication find(String uri) {
        Publication publication = null;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

        publication = new Publication();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            String str = URIUtils.objectRDFToString(object);
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                publication.setLabel(str);
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                publication.setTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                publication.setComment(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                publication.setHascoTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                publication.setHasSIRManagerEmail(str);
            }
        }

        publication.setUri(uri);

        return publication;
    }

    @Override
    public void save() {
        saveToTripleStore();
    }

    @Override
    public void delete() {
        deleteFromTripleStore();
    }

}

package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;

@JsonFilter("wkfFilter")
public class WKF extends MetadataTemplate {

    public String className = "hasco:WKF";

    @Override
    public void save() {
        super.save();
    }

    public static WKF find(String uri) {
        if (uri == null || uri.isEmpty()) {
            return null;
        }

        WKF wkf;
        Statement statement;
        RDFNode object;

        String cleanUri = URIUtils.stripAngleBrackets(uri);

        String queryString = "DESCRIBE <" + cleanUri + ">";

        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        } else {
            wkf = new WKF();
        }

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            String str = URIUtils.objectRDFToString(object);

            if (statement.getPredicate().getURI().equals(RDFS.label.getURI())) {
                wkf.setLabel(str);
            } else if (statement.getPredicate().getURI().equals(RDF.type.getURI())) {
                wkf.setTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                wkf.setHascoTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
                wkf.setHasStatus(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
                wkf.setHasVersion(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_DATAFILE)) {
                wkf.setHasDataFileUri(str);
            } else if (statement.getPredicate().getURI().equals(RDFS.comment.getURI())) {
                wkf.setComment(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                wkf.setHasSIRManagerEmail(str);
            }
        }

        wkf.setUri(uri);

        // Set default status if not present
        if (wkf.getHasStatus() == null || wkf.getHasStatus().isEmpty()) {
            wkf.setHasStatus("DRAFT");
        }


        return wkf;
    }
}

package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.Constants;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.ArrayList;
import java.util.List;

@JsonFilter("wkfNamespaceFilter")
public class WKFNamespace extends HADatAcThing implements SIRElement, Comparable<WKFNamespace> {

    public static String className = HASCO.WKF_NAMESPACE;

    @PropertyField(uri="hasco:hasAbbreviation")
    private String hasAbbreviation = "";

    @PropertyField(uri="hasco:WKFNamespaceUri")
    private String wkfNamespaceUri = "";

    @PropertyField(uri="hasco:hasSource")
    private String source = "";

    @PropertyField(uri="hasco:hasSourceMime")
    private String sourceMime = "";

    @PropertyField(uri="vstoi:hasStatus")
    private String hasStatus = "";

    @PropertyField(uri="vstoi:hasVersion")
    private String hasVersion = "";

    @PropertyField(uri="vstoi:hasLanguage")
    private String hasLanguage = "";

    @PropertyField(uri="vstoi:hasSIRManagerEmail")
    private String hasSIRManagerEmail = "";

    public String getHasAbbreviation() {
        return hasAbbreviation;
    }

    public void setHasAbbreviation(String hasAbbreviation) {
        this.hasAbbreviation = hasAbbreviation == null ? "" : hasAbbreviation.trim();
    }

    public String getWkfNamespaceUri() {
        return wkfNamespaceUri;
    }

    public void setWkfNamespaceUri(String wkfNamespaceUri) {
        if (wkfNamespaceUri == null || wkfNamespaceUri.trim().isEmpty()) {
            this.wkfNamespaceUri = "";
            return;
        }
        this.wkfNamespaceUri = URIUtils.normalizeNamespaceBase(wkfNamespaceUri.trim());
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source == null ? "" : source.trim();
    }

    public String getSourceMime() {
        return sourceMime;
    }

    public void setSourceMime(String sourceMime) {
        this.sourceMime = sourceMime == null ? "" : sourceMime.trim();
    }

    @Override
    public String getHasStatus() {
        return hasStatus;
    }

    public void setHasStatus(String hasStatus) {
        this.hasStatus = hasStatus == null ? "" : hasStatus.trim();
    }

    @Override
    public String getHasVersion() {
        return hasVersion;
    }

    public void setHasVersion(String hasVersion) {
        this.hasVersion = hasVersion == null ? "" : hasVersion.trim();
    }

    @Override
    public String getHasLanguage() {
        return hasLanguage;
    }

    public void setHasLanguage(String hasLanguage) {
        this.hasLanguage = hasLanguage == null ? "" : hasLanguage.trim();
    }

    @Override
    public String getHasSIRManagerEmail() {
        return hasSIRManagerEmail;
    }

    public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail == null ? "" : hasSIRManagerEmail.trim();
    }

    @Override
    public void save() {
        if (getTypeUri() == null || getTypeUri().isEmpty()) {
            setTypeUri(HASCO.WKF_NAMESPACE);
        }
        if (getHascoTypeUri() == null || getHascoTypeUri().isEmpty()) {
            setHascoTypeUri(HASCO.WKF_NAMESPACE);
        }
        if ((getLabel() == null || getLabel().isEmpty()) && hasAbbreviation != null && !hasAbbreviation.isEmpty()) {
            setLabel(hasAbbreviation);
        }
        if (getNamedGraph() == null || getNamedGraph().isEmpty()) {
            setNamedGraph(Constants.DEFAULT_REPOSITORY);
        }
        super.save();
    }

    public static WKFNamespace find(String uri) {
        if (uri == null || uri.isEmpty()) {
            return null;
        }

        WKFNamespace wkfNamespace;
        Statement statement;
        RDFNode object;

        String cleanUri = URIUtils.stripAngleBrackets(uri);
        String queryString = "DESCRIBE <" + cleanUri + ">";

        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();
        if (!stmtIterator.hasNext()) {
            return null;
        }

        wkfNamespace = new WKFNamespace();
        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            String str = URIUtils.objectRDFToString(object);

            if (statement.getPredicate().getURI().equals(RDFS.label.getURI())) {
                wkfNamespace.setLabel(str);
            } else if (statement.getPredicate().getURI().equals(RDF.type.getURI())) {
                wkfNamespace.setTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                wkfNamespace.setHascoTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_ABBREVIATION)) {
                wkfNamespace.setHasAbbreviation(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.WKF_NAMESPACE_URI)) {
                wkfNamespace.setWkfNamespaceUri(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_SOURCE)) {
                wkfNamespace.setSource(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_SOURCE_MIME)) {
                wkfNamespace.setSourceMime(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
                wkfNamespace.setHasStatus(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
                wkfNamespace.setHasVersion(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
                wkfNamespace.setHasLanguage(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                wkfNamespace.setHasSIRManagerEmail(str);
            } else if (statement.getPredicate().getURI().equals(RDFS.comment.getURI())) {
                wkfNamespace.setComment(str);
            }
        }

        wkfNamespace.setUri(cleanUri);
        if ((wkfNamespace.getLabel() == null || wkfNamespace.getLabel().isEmpty()) &&
            wkfNamespace.getHasAbbreviation() != null && !wkfNamespace.getHasAbbreviation().isEmpty()) {
            wkfNamespace.setLabel(wkfNamespace.getHasAbbreviation());
        }

        return wkfNamespace;
    }

    public static List<WKFNamespace> find() {
        String query =
            " SELECT ?uri WHERE { " +
            "    GRAPH <" + Constants.DEFAULT_REPOSITORY + "> {  " +
            "       ?uri  <" + HASCO.HASCO_TYPE + ">  <" + HASCO.WKF_NAMESPACE + "> . " +
            "    } " +
            "} ";
        return findManyByQuery(query);
    }

    public static WKFNamespace findByAbbreviation(String abbreviation) {
        if (abbreviation == null || abbreviation.trim().isEmpty()) {
            return null;
        }
        String normalized = abbreviation.trim();
        List<WKFNamespace> namespaces = find();
        if (namespaces == null) {
            return null;
        }
        for (WKFNamespace ns : namespaces) {
            if (ns == null) {
                continue;
            }
            if (normalized.equals(ns.getHasAbbreviation()) || normalized.equals(ns.getLabel())) {
                return ns;
            }
        }
        return null;
    }

    public static List<WKFNamespace> findManyByQuery(String query) {
        List<WKFNamespace> namespaces = new ArrayList<WKFNamespace>();

        ResultSetRewindable resultsrw = SPARQLUtils.select(
            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            String uri = soln.getResource("uri").getURI();
            WKFNamespace ns = WKFNamespace.find(uri);
            if (ns != null) {
                namespaces.add(ns);
            }
        }

        java.util.Collections.sort(namespaces);
        return namespaces;
    }

    @Override
    public int compareTo(WKFNamespace another) {
        String left = this.getHasAbbreviation() == null ? "" : this.getHasAbbreviation();
        String right = (another == null || another.getHasAbbreviation() == null) ? "" : another.getHasAbbreviation();
        return left.compareTo(right);
    }
}
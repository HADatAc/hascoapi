package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

import java.util.ArrayList;
import java.util.List;

@JsonFilter("analyticalToolFilter")
public class AnalyticalTool extends HADatAcThing implements SIRElement, Comparable<AnalyticalTool> {

    @PropertyField(uri = "vstoi:hasStatus")
    private String hasStatus;

    @PropertyField(uri = "vstoi:hasLanguage")
    private String hasLanguage;

    @PropertyField(uri = "vstoi:hasVersion")
    private String hasVersion;

    @PropertyField(uri = "vstoi:hasReviewNote")
    private String hasReviewNote;

    @PropertyField(uri = "vstoi:hasSIRManagerEmail")
    private String hasSIRManagerEmail;

    @PropertyField(uri = "vstoi:hasEditorEmail")
    private String hasEditorEmail;

    @PropertyField(uri = "hasco:hasProcess")
    private String hasProcessUri;

    public String getHasStatus() {
        return hasStatus;
    }

    public void setHasStatus(String hasStatus) {
        this.hasStatus = hasStatus;
    }

    public String getHasLanguage() {
        return hasLanguage;
    }

    public void setHasLanguage(String hasLanguage) {
        this.hasLanguage = hasLanguage;
    }

    public String getHasVersion() {
        return hasVersion;
    }

    public void setHasVersion(String hasVersion) {
        this.hasVersion = hasVersion;
    }

    public String getHasReviewNote() {
        return hasReviewNote;
    }

    public void setHasReviewNote(String hasReviewNote) {
        this.hasReviewNote = hasReviewNote;
    }

    public String getHasSIRManagerEmail() {
        return hasSIRManagerEmail;
    }

    public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail;
    }

    public String getHasEditorEmail() {
        return hasEditorEmail;
    }

    public void setHasEditorEmail(String hasEditorEmail) {
        this.hasEditorEmail = hasEditorEmail;
    }

    public String getHasProcessUri() {
        return hasProcessUri;
    }

    public void setHasProcessUri(String hasProcessUri) {
        this.hasProcessUri = hasProcessUri;
    }

    public static AnalyticalTool find(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            return null;
        }

        String normalizedUri = uri.trim();
        AnalyticalTool analyticalTool = null;

        String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + normalizedUri + "> ?p ?o } }";
        ResultSet resultSet = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                queryString
        );

        if (!resultSet.hasNext()) {
            return null;
        }

        analyticalTool = new AnalyticalTool();

        while (resultSet.hasNext()) {
            QuerySolution qs = resultSet.next();

            if (qs.contains("graph")) {
                analyticalTool.setNamedGraph(qs.get("graph").toString());
            }

            if (!qs.contains("p") || !qs.contains("o")) {
                continue;
            }

            String predicate = qs.get("p").toString();
            String object = qs.get("o").toString();

            if (predicate.equals(RDFS.LABEL)) {
                analyticalTool.setLabel(object);
            } else if (predicate.equals(RDF.TYPE)) {
                analyticalTool.setTypeUri(object);
            } else if (predicate.equals(RDFS.COMMENT)) {
                analyticalTool.setComment(object);
            } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                analyticalTool.setHascoTypeUri(object);
            } else if (predicate.equals(HASCO.HAS_IMAGE)) {
                analyticalTool.setHasImageUri(object);
            } else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
                analyticalTool.setHasWebDocument(object);
            } else if (predicate.equals(VSTOI.HAS_STATUS)) {
                analyticalTool.setHasStatus(object);
            } else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
                analyticalTool.setHasLanguage(object);
            } else if (predicate.equals(VSTOI.HAS_VERSION)) {
                analyticalTool.setHasVersion(object);
            } else if (predicate.equals(VSTOI.HAS_REVIEW_NOTE)) {
                analyticalTool.setHasReviewNote(object);
            } else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                analyticalTool.setHasSIRManagerEmail(object);
            } else if (predicate.equals(VSTOI.HAS_EDITOR_EMAIL)) {
                analyticalTool.setHasEditorEmail(object);
            } else if (predicate.equals(HASCO.HAS_PROCESS)) {
                analyticalTool.setHasProcessUri(object);
            }
        }

        analyticalTool.setUri(normalizedUri);
        return analyticalTool;
    }

    public static List<AnalyticalTool> findByProcessUri(String processUri) {
        List<AnalyticalTool> results = new ArrayList<AnalyticalTool>();

        String normalizedProcessUri = processUri == null ? "" : processUri.trim();
        if (normalizedProcessUri.isEmpty()) {
            return results;
        }

        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT DISTINCT ?uri WHERE { "
                + "   ?uri hasco:hascoType hasco:AnalyticalTool . "
                + "   OPTIONAL { ?uri hasco:hasProcess ?processRef . } "
                + "   OPTIONAL { <" + normalizedProcessUri + "> hasco:hasAnalyticalTool ?linkedTool . } "
                + "   OPTIONAL { <" + HASCO.ANY_PROCESS + "> hasco:hasAnalyticalTool ?globalLinkedTool . } "
                + "   FILTER ("
                + "      (?uri = ?linkedTool) "
                + "      || (?uri = ?globalLinkedTool) "
                + "      || (BOUND(?processRef) && ?processRef = \"*\") "
                + "      || (BOUND(?processRef) && ?processRef = <" + HASCO.ANY_PROCESS + ">) "
                + "      || (BOUND(?processRef) && ?processRef = <" + normalizedProcessUri + ">) "
                + "   ) "
                + " } ORDER BY ?uri ";

        ResultSet resultSet = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                queryString
        );

        while (resultSet.hasNext()) {
            QuerySolution qs = resultSet.next();
            if (!qs.contains("uri") || qs.getResource("uri") == null) {
                continue;
            }

            String toolUri = qs.getResource("uri").getURI();
            AnalyticalTool tool = find(toolUri);
            if (tool != null) {
                results.add(tool);
            }
        }

        return results;
    }

    @Override
    public int compareTo(AnalyticalTool another) {
        return this.getLabel().compareTo(another.getLabel());
    }

    @Override
    public void save() {
        saveToTripleStore();
    }

    @Override
    public void delete() {
        unlinkAllProcessesFromTool(getUri());
        deleteFromTripleStore();
    }

    public static boolean linkProcessToTool(String processUri, String toolUri) {
        if (!isHttpUri(processUri) || !isHttpUri(toolUri)) {
            return false;
        }

        String query = NameSpaces.getInstance().printSparqlNameSpaceList()
                + "INSERT DATA { <" + processUri + "> hasco:hasAnalyticalTool <" + toolUri + "> . }";
        return executeUpdate(query);
    }

    public static boolean unlinkProcessFromTool(String processUri, String toolUri) {
        if (!isHttpUri(processUri) || !isHttpUri(toolUri)) {
            return false;
        }

        String query = NameSpaces.getInstance().printSparqlNameSpaceList()
                + "DELETE WHERE { <" + processUri + "> hasco:hasAnalyticalTool <" + toolUri + "> . }";
        return executeUpdate(query);
    }

    public static boolean unlinkAllProcessesFromTool(String toolUri) {
        if (!isHttpUri(toolUri)) {
            return false;
        }

        String query = NameSpaces.getInstance().printSparqlNameSpaceList()
                + "DELETE WHERE { ?process hasco:hasAnalyticalTool <" + toolUri + "> . }";
        return executeUpdate(query);
    }

    private static boolean executeUpdate(String query) {
        try {
            UpdateRequest request = UpdateFactory.create(query);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                    request,
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE)
            );
            processor.execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isHttpUri(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }
}

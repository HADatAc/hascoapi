package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.annotations.PropertyValueType;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.Utils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.PROV;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;


import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.hascoapi.Constants.*;

@JsonFilter("taskFilter")
public class Task extends HADatAcThing implements Comparable<Task> {

    private static final String UBERON_ANATOMICAL_ENTITY_ROOT = "http://purl.obolibrary.org/obo/UBERON_0000465";

    @PropertyField(uri = "vstoi:hasStatus")
    private String hasStatus;

	@PropertyField(uri="vstoi:hasLanguage")
	private String hasLanguage;

    @PropertyField(uri = "vstoi:hasVersion")
    private String hasVersion;

    @PropertyField(uri = "vstoi:hasReviewNote")
    String hasReviewNote;

    @PropertyField(uri="prov:wasDerivedFrom")
    private String wasDerivedFrom;

    @PropertyField(uri = "vstoi:hasSIRManagerEmail")
    private String hasSIRManagerEmail;

    @PropertyField(uri = "vstoi:hasEditorEmail")
    private String hasEditorEmail;

    @PropertyField(uri = "vstoi:hasSupertask")
    private String hasSupertaskUri;

    @PropertyField(uri = "vstoi:hasTemporalDependency")
    private String hasTemporalDependency;

    @PropertyField(uri="vstoi:hasRequiredInstrument", valueType=PropertyValueType.URI)
    private List<String> hasRequiredInstrumentUris = new ArrayList<String>();

    @PropertyField(uri="vstoi:hasSubtask", valueType=PropertyValueType.URI)
    private List<String> hasSubtaskUris = new ArrayList<String>();

    @PropertyField(uri="vstoi:associatedAnatomy", valueType=PropertyValueType.URI)
    private List<String> hasAssociatedAnatomyUris = new ArrayList<String>();

    @PropertyField(uri="vstoi:hasIterationConstraint")
    private String hasIterationConstraint;

    @PropertyField(uri="vstoi:supportsObjective")
    private String supportsObjective;

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

    public void setWasDerivedFrom(String wasDerivedFrom) {
        this.wasDerivedFrom = wasDerivedFrom;
    }

    public String getWasDerivedFrom() {
        return wasDerivedFrom;
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

    public String getHasSupertaskUri() {
        return hasSupertaskUri;
    }

    public String getHasSupertask() {
        return hasSupertaskUri;
    }

    public void setHasSupertaskUri(String hasSupertaskUri) {
        this.hasSupertaskUri = hasSupertaskUri;
    }

    public String getHasTemporalDependency() {
        return hasTemporalDependency;
    }

    public String getTemporalDependencyLabel() {
        if (hasTemporalDependency == null || hasTemporalDependency.isEmpty())
            return "";
        return VSTOI.temporalDependencyLabel(hasTemporalDependency);
    }

    public void setHasTemporalDependency(String hasTemporalDependency) {
        this.hasTemporalDependency = hasTemporalDependency;
    }

    public List<String> getHasRequiredInstrumentUris() {
        return hasRequiredInstrumentUris;
    }

    public void setHasRequiredInstrumentUris(List<String> hasRequiredInstrumentUris) {
        if (hasRequiredInstrumentUris == null) {
            this.hasRequiredInstrumentUris = new ArrayList<String>();
        } else {
            this.hasRequiredInstrumentUris = hasRequiredInstrumentUris;
        }
    }

    public void addHasRequiredInstrumentUri(String hasRequiredInstrumentUri) {
        String cleanUri = normalizeRelatedUri(hasRequiredInstrumentUri);
        if (cleanUri.isEmpty()) {
            return;
        }
        if (!this.hasRequiredInstrumentUris.contains(cleanUri)) {
            this.hasRequiredInstrumentUris.add(cleanUri);
        }
    }

    private static String normalizeRelatedUri(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            return "";
        }

        String normalized = URIUtils.canonicalizePmsrUri(uri.trim());

        // Canonicalize legacy WKF fragment path variants.
        normalized = normalized.replaceAll("(?i)/ont/WKF#/", "/ont/");
        normalized = normalized.replaceAll("(?i)/ont/WKF#$", "/ont/");

        return normalized.trim();
    }

    private static List<String> splitAndNormalizeUriValues(String value) {
        LinkedHashSet<String> uris = new LinkedHashSet<String>();
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<String>();
        }

        String[] parts;
        if (value.contains("|")) {
            parts = value.split("\\s*\\|\\s*");
        } else if (value.contains(";")) {
            parts = value.split("\\s*;\\s*");
        } else {
            parts = new String[] { value };
        }

        for (String part : parts) {
            String normalized = normalizeRelatedUri(part);
            if (!normalized.isEmpty()) {
                uris.add(normalized);
            }
        }

        return new ArrayList<String>(uris);
    }

    public List<RequiredInstrument> getRequiredInstrument() {
        List<RequiredInstrument> resp = new ArrayList<RequiredInstrument>();
        if (hasRequiredInstrumentUris == null || hasRequiredInstrumentUris.size() <= 0) {
            return resp;
        }

        // Avoid repeated lookups by expanding separators + deduplicating first.
        LinkedHashSet<String> uniqueUris = new LinkedHashSet<String>();
        for (String hasRequiredInstrumentUri : hasRequiredInstrumentUris) {
            List<String> expanded = splitAndNormalizeUriValues(hasRequiredInstrumentUri);
            for (String uri : expanded) {
                uniqueUris.add(uri);
            }
        }

        for (String uri : uniqueUris) {
            RequiredInstrument requiredInstrument = RequiredInstrument.find(uri);
            if (requiredInstrument != null) {
                resp.add(requiredInstrument);
            }
        }

        return resp;
    }

    public void setHasSubtaskUris(List<String> hasSubtaskUris) {
        if (hasSubtaskUris == null) {
            this.hasSubtaskUris = new ArrayList<String>();
        } else {
            this.hasSubtaskUris = hasSubtaskUris;
        }
    }

    public void addHasSubtaskUri(String hasSubtaskUri) {
        String cleanUri = normalizeRelatedUri(hasSubtaskUri);
        if (cleanUri.isEmpty()) {
            return;
        }
        if (!this.hasSubtaskUris.contains(cleanUri)) {
            this.hasSubtaskUris.add(cleanUri);
        }
    }

    public void removeHasSubtaskUri(String hasSubtaskUri) {
        if (hasSubtaskUri.contains(hasSubtaskUri)) {
            this.hasSubtaskUris.remove(hasSubtaskUri);
        }
    }

    public List<String> getHasSubtaskUris() {
        return this.hasSubtaskUris;
    }

    public String getHasIterationConstraint() {
        return hasIterationConstraint;
    }

    public void setHasIterationConstraint(String hasIterationConstraint) {
        this.hasIterationConstraint = hasIterationConstraint;
    }

    public String getSupportsObjective() {
        return supportsObjective;
    }

    public void setSupportsObjective(String supportsObjective) {
        this.supportsObjective = supportsObjective;
    }

    public List<String> getHasAssociatedAnatomyUris() {
        return hasAssociatedAnatomyUris;
    }

    // Backward-compatible alias used by some API payloads.
    public List<String> getAssociatedAnatomyUris() {
        return getHasAssociatedAnatomyUris();
    }

    public void setHasAssociatedAnatomyUris(List<String> hasAssociatedAnatomyUris) {
        this.hasAssociatedAnatomyUris = new ArrayList<String>();
        if (hasAssociatedAnatomyUris == null) {
            return;
        }
        for (String anatomyUri : hasAssociatedAnatomyUris) {
            addHasAssociatedAnatomyUri(anatomyUri);
        }
    }

    // Backward-compatible alias used by some API payloads.
    public void setAssociatedAnatomyUris(List<String> associatedAnatomyUris) {
        setHasAssociatedAnatomyUris(associatedAnatomyUris);
    }

    public void addHasAssociatedAnatomyUri(String anatomyUri) {
        String cleanUri = normalizeRelatedUri(anatomyUri);
        if (cleanUri.isEmpty()) {
            return;
        }
        if (!isValidAssociatedAnatomyUri(cleanUri)) {
            System.out.println("[Task] Ignoring associated anatomy URI outside UBERON anatomical entity hierarchy: " + cleanUri);
            return;
        }
        if (!this.hasAssociatedAnatomyUris.contains(cleanUri)) {
            this.hasAssociatedAnatomyUris.add(cleanUri);
        }
    }

    private boolean isValidAssociatedAnatomyUri(String anatomyUri) {
        if (anatomyUri.equals(UBERON_ANATOMICAL_ENTITY_ROOT)) {
            return true;
        }

        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT ?uri WHERE { \n"
                + "  <" + anatomyUri + "> rdfs:subClassOf* <" + UBERON_ANATOMICAL_ENTITY_ROOT + "> . \n"
                + "  BIND(<" + anatomyUri + "> as ?uri) \n"
                + " } LIMIT 1 \n";

        ResultSet resultSet = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        return resultSet != null && resultSet.hasNext();
    }

    // Backward-compatible alias used by some clients (e.g., workflow editor)
    public List<Task> getSubtask() {
        List<Task> resp = new ArrayList<Task>();
        if (hasSubtaskUris == null || hasSubtaskUris.size() <= 0) {
            return resp;
        }
        for (String hasSubtaskUri : hasSubtaskUris) {
            Task subtask = Task.find(hasSubtaskUri);
            if (subtask != null) {
                resp.add(subtask);
            }
        }
        return resp;
    }

    // Backward-compatible alias used by some clients (e.g., workflow editor)
    public List<RequiredInstrument> getRequiredInstrumentation() {
        return getRequiredInstrument();
    }

    /* 
    public List<Task> getSubtasks() {
        List<Task> resp = new ArrayList<Task>();
        if (hasSubtaskUris == null || hasSubtaskUris.size() <= 0) {
            return resp;
        }
        for (String hasSubtaskUri : hasSubtaskUris) {
            Task subtask = Task.find(hasSubtaskUri);
            if (subtask != null) {
                resp.add(subtask);
            }
        }
        return resp;
    }
    */

    public static Task find(String uri) {
 		if (uri == null || uri.isEmpty()) {
			return null;
		}
		Task task = null;

        // Single query retrieves both data and one candidate graph.
        String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
        ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (resultSet == null || !resultSet.hasNext()) {
			return null;
		} else {
            task = new Task();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();

            if ((task.getNamedGraph() == null || task.getNamedGraph().isEmpty()) && qs.contains("graph")) {
                task.setNamedGraph(qs.get("graph").toString());
            }
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);

                if (predicate.equals(RDFS.LABEL)) {
                    task.setLabel(object);
                } else if (predicate.equals(RDF.TYPE)) {
                    task.setTypeUri(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    task.setComment(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    task.setHascoTypeUri(object);
                } else if (predicate.equals(HASCO.HAS_IMAGE)) {
                    task.setHasImageUri(object);
                } else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
                    task.setHasWebDocument(object);
                } else if (predicate.equals(VSTOI.HAS_STATUS)) {
                    task.setHasStatus(object);
                } else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
                    task.setHasLanguage(object);
                } else if (predicate.equals(VSTOI.HAS_VERSION)) {
                    task.setHasVersion(object);
                } else if (predicate.equals(VSTOI.HAS_REVIEW_NOTE)) {
                    task.setHasReviewNote(object);
                } else if (predicate.equals(PROV.WAS_DERIVED_FROM)) {
                    task.setWasDerivedFrom(object);
                } else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                    task.setHasSIRManagerEmail(object);
                } else if (predicate.equals(VSTOI.HAS_EDITOR_EMAIL)) {
                    task.setHasEditorEmail(object);
                } else if (predicate.equals(VSTOI.HAS_SUPERTASK)) {
                    task.setHasSupertaskUri(object);
                } else if (predicate.equals(VSTOI.HAS_TEMPORAL_DEPENDENCY)) {
                    task.setHasTemporalDependency(object);
                } else if (predicate.equals(VSTOI.HAS_REQUIRED_INSTRUMENT)) {
                    List<String> uris = splitAndNormalizeUriValues(object);
                    for (String instrumentUri : uris) {
                        task.addHasRequiredInstrumentUri(instrumentUri);
                    }
                } else if (predicate.equals(VSTOI.HAS_SUBTASK)) {
                    List<String> uris = splitAndNormalizeUriValues(object);
                    for (String subtaskUri : uris) {
                        task.addHasSubtaskUri(subtaskUri);
                    }
                } else if (predicate.equals(VSTOI.HAS_ITERATION_CONSTRAINT)) {
                    task.setHasIterationConstraint(object);
                } else if (predicate.equals(VSTOI.SUPPORTS_OBJECTIVE)) {
                    task.setSupportsObjective(object);
                } else if (predicate.equals(VSTOI.ASSOCIATED_ANATOMY)) {
                    List<String> uris = splitAndNormalizeUriValues(object);
                    for (String anatomyUri : uris) {
                        task.addHasAssociatedAnatomyUri(anatomyUri);
                    }
                }
            }
        }
                                                                                                                                                                                                              
        task.setUri(uri);

        return task;
    }

    @Override
    public int compareTo(Task another) {
        return this.getLabel().compareTo(another.getLabel());
    }

    @Override
    public void save() {
        saveToTripleStore();
        if (this.hasSupertaskUri != null && !this.hasSupertaskUri.isEmpty()) {
            Task superTask = Task.find(this.hasSupertaskUri);
            if (superTask != null && !superTask.getHasSubtaskUris().contains(this.uri)) {
                superTask.addHasSubtaskUri(this.uri);
                superTask.save();
            }
        }
    }

    @Override
    public void delete() {
        if (this.hasSupertaskUri != null && !this.hasSupertaskUri.isEmpty()) {
            Task superTask = Task.find(this.hasSupertaskUri);
            if (superTask != null && superTask.getHasSubtaskUris().contains(this.uri)) {
                superTask.removeHasSubtaskUri(this.uri);
                superTask.save();
            }
        }
        deleteFromTripleStore();
    }

    public static void deleteWithSubtasks(Task task) {
        if (task.getHasSubtaskUris() != null && task.getHasSubtaskUris().size() > 0) {
            for (String subtaskUri : task.getHasSubtaskUris()) {
                Task subtask = Task.find(subtaskUri);
                if (subtask != null) {
                    Task.deleteWithSubtasks(subtask);
                }
            }
        }
        task.delete();
    }

}

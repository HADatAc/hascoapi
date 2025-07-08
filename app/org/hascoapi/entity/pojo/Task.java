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
import java.util.List;

import static org.hascoapi.Constants.*;

@JsonFilter("taskFilter")
public class Task extends HADatAcThing implements Comparable<Task> {

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

    public void setHasSupertaskUri(String hasSupertaskUri) {
        this.hasSupertaskUri = hasSupertaskUri;
    }

    public String getHasTemporalDependency() {
        return hasTemporalDependency;
    }

    public void setHasTemporalDependency(String hasTemporalDependency) {
        this.hasTemporalDependency = hasTemporalDependency;
    }

    public List<String> getHasRequiredInstrumentUris() {
        return hasRequiredInstrumentUris;
    }

    public void setHasRequiredInstrumentUris(List<String> hasRequiredInstrumentUris) {
        this.hasRequiredInstrumentUris = hasRequiredInstrumentUris;
    }

    public void addHasRequiredInstrumentUri(String hasRequiredInstrumentUri) {
        this.hasRequiredInstrumentUris.add(hasRequiredInstrumentUri);
    }

    public List<RequiredInstrument> getRequiredInstrument() {
        List<RequiredInstrument> resp = new ArrayList<RequiredInstrument>();
        if (hasRequiredInstrumentUris == null || hasRequiredInstrumentUris.size() <= 0) {
            return resp;
        }
        for (String hasRequiredInstrumentUri : hasRequiredInstrumentUris) {
            RequiredInstrument requiredInstrument = RequiredInstrument.find(hasRequiredInstrumentUri);
            if (requiredInstrument != null) {
                resp.add(requiredInstrument);
            }
        }
        return resp;
    }

    public void setHasSubtaskUris(List<String> hasSubtaskUris) {
        this.hasSubtaskUris = hasSubtaskUris;
    }

    public void addHasSubtaskUri(String hasSubtaskUri) {
        this.hasSubtaskUris.add(hasSubtaskUri);
    }

    public List<String> getHasSubtaskUris() {
        return this.hasSubtaskUris;
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
		// Construct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
            task = new Task();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				task.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
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
                    task.addHasRequiredInstrumentUri(object);
                } else if (predicate.equals(VSTOI.HAS_SUBTASK)) {
                    task.addHasSubtaskUri(object);
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

    private void deleteSubtasks(Task task) {
        if (task.getHasSubtaskUris() != null && task.getHasSubtaskUris().size() > 0) {
            for (String subtaskUri : task.getHasSubtaskUris()) {
                Task subtask = Task.find(subtaskUri);
                if (subtask != null) {
                    this.deleteSubtasks(subtask);
                }
            }
        }
        task.delete();
    }

    @Override
    public void delete() {
        deleteFromTripleStore();
    }

    public void deleteWithSubtasks() {
        if (this.getHasSubtaskUris() != null && this.getHasSubtaskUris().size() > 0) {
            for (String subtaskUri : this.getHasSubtaskUris()) {
                Task subtask = Task.find(subtaskUri);
                if (subtask != null) {
                    this.deleteSubtasks(subtask);
                }
            }
        }
        deleteFromTripleStore();
    }

}

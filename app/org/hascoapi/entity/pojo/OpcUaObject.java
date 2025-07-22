package org.hascoapi.entity.pojo;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.WordUtils;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.hascoapi.utils.IngestionLogger;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.Utils;
import org.hascoapi.Constants;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.PROV;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.SCHEMA;
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.ingestion.mqtt.MqttMessageWorker;
import org.hascoapi.utils.CollectionUtil;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonFilter("opcUaObjectFilter")
public class OpcUaObject extends HADatAcThing implements Comparable<OpcUaObject> {
    private static final String className = "hasco:OpcUaObject";
    @PropertyField(uri="hasco:hasStream")
    private String streamUri;

    @PropertyField(uri="hasco:hasSDD")
    private String semanticDataDictionaryUri;

    @PropertyField(uri="hasco:hasDeployment")
    private String deploymentUri;

    @PropertyField(uri="hasco:hasNodeId")
    private String nodeId;

    @PropertyField(uri="hasco:hasBrowseName")
    private String browseName;

    @PropertyField(uri="hasco:hasNamespaceIndex")
    private int namespaceIndex;

    @PropertyField(uri="hasco:hasObjectType")
    private String objectType;

    @PropertyField(uri="hasco:hasStatus")
    private String status;

    @PropertyField(uri="hasco:canView")
    private List<String> canView;

    @PropertyField(uri="hasco:canUpdate")
    private List<String> canUpdate;

    @PropertyField(uri="vstoi:hasSIRManagerEmail")
    private String hasSIRManagerEmail;

    @PropertyField(uri="hasco:hasVariables")
    private String messageVariables;

    private List<String> headers;


    public OpcUaObject() {
        canView = new ArrayList<>();
        canUpdate = new ArrayList<>();
    }

    public String getStreamUri() {
        return streamUri;
    }
    
    public void setStreamUri(String streamUri) {
        this.streamUri = streamUri;
    }
    
    public String getSemanticDataDictionaryUri() {
        return this.semanticDataDictionaryUri;
    }
    
    public void setSemanticDataDictionaryUri(String semanticDataDictionaryUri) {
        this.semanticDataDictionaryUri = semanticDataDictionaryUri;
    }
    public SemanticDataDictionary getSemanticDataDictionary() {
        if (this.semanticDataDictionaryUri == null || this.semanticDataDictionaryUri.equals("")) {
            return null;
        }
        SemanticDataDictionary semanticDataDictionary = SemanticDataDictionary.find(semanticDataDictionaryUri);
        headers = new ArrayList<String>();
        if (semanticDataDictionary != null && semanticDataDictionary.getAttributes() != null) {
            for (SDDAttribute attr : semanticDataDictionary.getAttributes()) {
                headers.add(attr.getLabel());
            }
        }
        setHeaders(headers.toString());
        return semanticDataDictionary;
    }

    public void setHeaders(String headersStr) {
        this.messageVariables = headersStr;
        getHeaders();
    }
    public List<String> getHeaders() {
        if (this.headers != null) {
            return this.headers;
        }
        this.headers = new ArrayList<String>();
        if (messageVariables == null || messageVariables.isEmpty()) {
            return this.headers;
        }
        String auxstr = messageVariables.replace("[","").replace("]","");
        StringTokenizer str = new StringTokenizer(auxstr,",");
        while (str.hasMoreTokens()) {
            this.headers.add(str.nextToken().trim());
        }
        return this.headers;
    }
    
    public String getDeploymentUri() {
        return deploymentUri;
    }
    
    public void setDeploymentUri(String deploymentUri) {
        this.deploymentUri = deploymentUri;
    }

    public Deployment getDeployment() {
        if (deploymentUri == null || deploymentUri.equals("")) {
            return null;
        }
        return Deployment.find(deploymentUri);
    }
    
    public String getNodeId() {
        return this.nodeId;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    
    public String getBrowseName() {
        return this.browseName;
    }
    
    public void setBrowseName(String browseName) {
        this.browseName = browseName;
    }
    
    public int getNamespaceIndex() {
        return this.namespaceIndex;
    }
    
    public void setNamespaceIndex(int namespaceIndex) {
        this.namespaceIndex = namespaceIndex;
    }
    
    public String getObjectType() {
        return this.objectType;
    }
    
    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }
    
    public String getStatus() {
        return this.status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<String> getCanView() {
        return canView;
    }
    public void setCanView(List<String> canView) {
        this.canView = canView;
    }
    public void addCanView(String canViewEmail) {
        if (canView != null) {
            if (!canView.contains(canViewEmail)) {
                this.canView.add(canViewEmail);
            }
        }
    }
    

    public List<String> getCanUpdate() {
        return canUpdate;
    }
    public void setCanUpdate(List<String> canUpdate) {
        this.canUpdate = canUpdate;
    }
    public void addCanUpdate(String canUpdateEmail) {
        if (canUpdate != null) {
            if (!canUpdate.contains(canUpdateEmail)) {
                this.canUpdate.add(canUpdateEmail);
            }
        }
    }
    
    public String getHasSIRManagerEmail() {
        return this.hasSIRManagerEmail;
    }
    
    public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail;
    }
    

    @Override
    public boolean equals(Object o) {
        if ((o instanceof OpcUaObject) && (((OpcUaObject) o).getUri().equals(this.getUri()))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(OpcUaObject o) {
        if (this.getLabel() != null && o.getLabel() != null) {
            return this.getLabel().compareTo(o.getLabel());
        }
        return this.getUri().compareTo(o.getUri());
    }

    @Override
    public void save() {
        try {
            saveToTripleStore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete() {
        deleteFromTripleStore();
    }

    public static OpcUaObject find(String uri) {

		OpcUaObject obj;
		String hascoTypeUri = Utils.retrieveHASCOTypeUri(uri);
		if (hascoTypeUri.equals(HASCO.OPCUA_OBJECT)) {
			obj = new OpcUaObject();
		} else {
			return null;
		}

	    Statement statement;
	    RDFNode object;

	    String queryString = "DESCRIBE <" + uri + ">";
	    Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

		StmtIterator stmtIterator = model.listStatements();

		if (!stmtIterator.hasNext()) {
			return null;
		}

		while (stmtIterator.hasNext()) {
		    statement = stmtIterator.next();
		    object = statement.getObject();
			String string = URIUtils.objectRDFToString(object);

			if (uri != null && !uri.isEmpty()) {
				if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
					obj.setLabel(string);
				} else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
					obj.setTypeUri(string);
				} else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
					obj.setHascoTypeUri(string);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_STREAM)) {
					obj.setStreamUri(string);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_DEPLOYMENT)) {
					obj.setDeploymentUri(string);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_SDD)) {
					obj.setSemanticDataDictionaryUri(string);
				} else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
					obj.setComment(string);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_OPCUA_OBJECT_STATUS)) {
					obj.setStatus(string);
                } else if (statement.getPredicate().getURI().equals(HASCO.CAN_UPDATE)) {
                    obj.addCanUpdate(string);
                } else if (statement.getPredicate().getURI().equals(HASCO.CAN_VIEW)) {
                    obj.addCanView(string);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					obj.setHasSIRManagerEmail(string);
				}
			}
		}

		obj.setUri(uri);

		return obj;
	}

    public static void initiateStreamObjects() {
        List<OpcUaObject> openTopics = OpcUaObject.findOpenStreamObjects();
        if (openTopics != null && openTopics.size() > 0) {
            System.out.println("StreamTopic.initiateStreamTopics() called to fix " + openTopics.size() + " topics.");
            for (StreamTopic topic : openTopics) {
                topic.setHasTopicStatus(HASCO.INACTIVE);
                topic.save();
            }
        }
    }
    public static List<OpcUaObject> findOpenStreamObjects() {
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() +
            "SELECT ?uri WHERE { " +
            "   ?uri a hasco:OpcUaObject . " +
            "   ?uri hasco:hasStatus ?status . " +
            "   FILTER (?status != hasco:Inactive) " +
            "} ";
        return findManyByQuery(query);
    }

    public static List<OpcUaObject> findManyByQuery(String query) {
        List<OpcUaObject> objects = new ArrayList<OpcUaObject>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            String uri = soln.getResource("uri").getURI();
            OpcUaObject object = OpcUaObject.find(uri);
            objects.add(object);
        }
        return objects;
    }
    
    public static List<OpcUaObject> findByStream(String streamUri) {
        if (streamUri == null || streamUri.isEmpty()) {
            return new ArrayList<OpcUaObject>();
        }
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
            "SELECT ?uri WHERE { \n" +
            "   ?uri hasco:hasStream <" + streamUri + "> . \n" +
            "   ?uri hasco:hascoType hasco:OpcUaObject . \n" + 
            " }";
        return findManyByQuery(queryString);
    }     
}

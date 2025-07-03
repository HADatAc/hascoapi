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
import org.hascoapi.utils.IngestionLogger;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonFilter("streamTopicFilter")
public class StreamTopic extends HADatAcThing implements Comparable<StreamTopic> {
    private static final String className = "hasco:StreamTopic";

    /*
     *   STREAM TOPIC PROPERTIES
     */
    @PropertyField(uri="hasco:hasStream")
    private String streamUri;
    @PropertyField(uri="hasco:hasSDD")
    private String semanticDataDictionaryUri;
    @PropertyField(uri="hasco:hasDeployment")
    private String deploymentUri;
    @PropertyField(uri="hasco:hasCellScopeUri")
    private List<String> cellScopeUri;
    @PropertyField(uri="hasco:hasCellScopeName")
    private List<String> cellScopeName;
    @PropertyField(uri="hasco:hasTotalReceivedMessages")
    private String totalReceivedMessages;
    @PropertyField(uri="hasco:hasMessageHeader")
    private String messageHeaders;

    /*
     * A stream topic has a hasTopicStatus is to be used for MESSAGE STREAMS. 
     * Possible values for topic status:
     *    INACTIVE: The message stream is either DRAFT or CLOSED.
     *    RECORDING:  The message stream is active and the topic's content is being recorded into a datafile.
     *    INGESTING:  The message stream is active, the topic's content is being recorded into a datafile, and, at the same time, the topic'scontent is 
     *       being ingested into a knowledge graph.  
     *    SUSPENDED: The message stream is active but no topic's content is being recorded or ingested.
     * 
     * The hasTopicStatus is a property of the stream topic itself, and it is not a property of a datafile that  
     * the stream topic may be recording. This means that if the topic's content is being saved into a datafile now to be ingested later, 
     * the topic will not be in INGESTION mode when the recorded datafile is ingested later.
     * 
     */
    @PropertyField(uri="hasco:hasTopicStatus")
    private String hasTopicStatus;

    @PropertyField(uri = "hasco:canView")
    private List<String> canView;

    @PropertyField(uri = "hasco:canUpdate")
    private List<String> canUpdate;

	@PropertyField(uri="vstoi:hasSIRManagerEmail")
	private String hasSIRManagerEmail;

    private List<String> headers;
    
    private DataFile archive = null;
    private String log;
    private IngestionLogger logger = null;

    public StreamTopic() {
        totalReceivedMessages = "0";
        cellScopeUri = new ArrayList<String>();
        cellScopeName = new ArrayList<String>();
        canView = new ArrayList<String>();
        canUpdate = new ArrayList<String>();
        logger = new IngestionLogger(this);
    }

    @Override
    public boolean equals(Object o) {
        if ((o instanceof StreamTopic) && (((StreamTopic) o).getUri().equals(this.getUri()))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(StreamTopic another) {
        if (this.getLabel() != null && another.getLabel() != null) {
            return this.getLabel().compareTo(another.getLabel());
        }
        return this.getUri().compareTo(another.getUri());
    }

    public String getStreamUri() {
        return streamUri;
    }
    public void setStreamUri(String stream_uri) {
        this.streamUri = stream_uri;
    }

    public String getDeploymentUri() {
        return deploymentUri;
    }
    public Deployment getDeployment() {
        if (deploymentUri == null || deploymentUri.equals("")) {
            return null;
        }
        return Deployment.find(deploymentUri);
    }

    public void setDeploymentUri(String deploymentUri) {
        this.deploymentUri = deploymentUri;
    }

    public String getSemanticDataDictionaryUri() {
        return this.semanticDataDictionaryUri;
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
    public void setSemanticDataDictionaryUri(String semanticDataDictionaryUri) {
        this.semanticDataDictionaryUri = semanticDataDictionaryUri;
    }

    public List<String> getHeaders() {
        if (headers != null) {
            return headers;
        }
        List<String> headers = new ArrayList<String>();
        if (messageHeaders == null || messageHeaders.isEmpty()) {
            return headers;
        }
        String auxstr = messageHeaders.replace("[","").replace("]","");
        StringTokenizer str = new StringTokenizer(auxstr,",");
        while (str.hasMoreTokens()) {
            headers.add(str.nextToken().trim());
        }
        return headers;
    }

    public void setHeaders(String headersStr) {
        this.messageHeaders = headersStr;
        getHeaders();
    }

    public String getHasTotalReceivedMessages() {
        return totalReceivedMessages;
    }
    public void setHasTotalReceivedMessages(String totalReceivedMessages) {
        this.totalReceivedMessages = totalReceivedMessages;
    }

    public String getHasTopicStatus() {
        return hasTopicStatus;
    }

    public void setHasTopicStatus(String hasTopicStatus) {
        this.hasTopicStatus = hasTopicStatus;
    }

    @JsonIgnore
    public String getLog() {
        return getMessageLogger().getLog();
    }
    public void setLog(String log) {
        getMessageLogger().setLog(log);
        this.log = log;
    }

    public IngestionLogger getMessageLogger() {
        return logger;
    }
    public void setMessageLogger(IngestionLogger logger) {
        this.logger = logger;
    }

    public boolean hasScope() {
        return (hasCellScope());
    }

    public boolean hasCellScope() {
        if (cellScopeUri != null && cellScopeUri.size() > 0) {
            for (String tmpUri : cellScopeUri) {
                if (tmpUri != null && !tmpUri.equals("")) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> getCellScopeUri() {
        return cellScopeUri;
    }

    public void setCellScopeUri(List<String> cellScopeUri) {
        this.cellScopeUri = cellScopeUri;
        if (cellScopeUri == null || cellScopeUri.size() == 0) {
            return;
        }
        cellScopeName = new ArrayList<String>();
        for (String objUri : cellScopeUri) {
            StudyObject obj = StudyObject.find(objUri);
            if (obj != null && obj.getUri().equals(objUri)) {
                cellScopeName.add(obj.getLabel());
            } else {
                cellScopeName.add("");
            }
        }
    }

    public void addCellScopeUri(String cellScopeUri) {
        this.cellScopeUri.add(cellScopeUri);
    }

    public List<String> getCellScopeName() {
        return cellScopeName;
    }

    public void setCellScopeName(List<String> cellScopeName) {
        this.cellScopeName = cellScopeName;
    }

    public void addCellScopeName(String cellScopeName) {
        this.cellScopeName.add(cellScopeName);
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

    public String getHasSIRManagerEmail() {
        return this.hasSIRManagerEmail;
    }
    public void setHASSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail;
    }

    public static StreamTopic find(String uri) {
        //System.out.println("inside StreamTopic.find(uri): " + uri);
		StreamTopic topic;
		String hascoTypeUri = Utils.retrieveHASCOTypeUri(uri);
		if (hascoTypeUri.equals(HASCO.STREAM_TOPIC)) {
			topic = new StreamTopic();
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
            //System.out.println("Predicate: [" + statement.getPredicate().getURI() + "]   Predicate value: [" + string + "]");
			if (uri != null && !uri.isEmpty()) {
				if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
					topic.setLabel(string);
				} else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
					topic.setTypeUri(string);
				} else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
					topic.setHascoTypeUri(string);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_STREAM)) {
					topic.setStreamUri(string);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_DEPLOYMENT)) {
					topic.setDeploymentUri(string);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_SDD)) {
					topic.setSemanticDataDictionaryUri(string);
				} else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
					topic.setComment(string);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_TOPIC_STATUS)) {
					topic.setHasTopicStatus(string);
				} else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
					topic.setComment(string);
                } else if (statement.getPredicate().getURI().equals(HASCO.HAS_TOTAL_RECEIVED_MESSAGES)) {
                    //System.out.println("StreamTopic: showing received message [" + string + "]");  
                    topic.setHasTotalReceivedMessages(string);
                } else if (statement.getPredicate().getURI().equals(HASCO.CAN_UPDATE)) {
                    topic.addCanUpdate(string);
                } else if (statement.getPredicate().getURI().equals(HASCO.CAN_VIEW)) {
                    topic.addCanView(string);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					topic.setHASSIRManagerEmail(string);
				}
			}
		}

		topic.setUri(uri);

		return topic;
	}

    /* 
     *   This method needs to be called every time a hascoapi instance is initiated.
     *   Non-inactive topics at hascoapi instance initialization means that 
     *   something wrong happened with the instance. To recover from this situation,
     *   topics that are not inactive are set to be inactive,
     */
    public static void initiateStreamTopics() {
        List<StreamTopic> openTopics = StreamTopic.findOpenStreamTopics();
        if (openTopics != null && openTopics.size() > 0) {
            System.out.println("StreamTopic.initiateStreamTopics() called to fix " + openTopics.size() + " topics.");
            for (StreamTopic topic : openTopics) {
                topic.setHasTopicStatus(HASCO.INACTIVE);
                topic.save();
            }
        }
    }

    /* Open streams are those with ended_at_date =  9999-12-31T23:59:59.999Z */
    public static List<StreamTopic> findOpenStreamTopics() {
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() +
            "SELECT ?uri WHERE { " +
            "   ?uri a hasco:StreamTopic . " +
            "   ?uri hasco:hasTopicStatus ?status . " +
            "   FILTER (?status != hasco:Inactive) " +
            "} ";
        return findManyByQuery(query);
    }

    public static List<StreamTopic> findByStream(String streamUri) {
        if (streamUri == null || streamUri.isEmpty()) {
            return new ArrayList<StreamTopic>();
        }
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
            "SELECT ?uri WHERE { \n" +
            "   ?uri hasco:hasStream <" + streamUri + "> . \n" +
            "   ?uri hasco:hascoType hasco:StreamTopic . \n" + 
            " }";
        return findManyByQuery(queryString);
    } 

    public static List<StreamTopic> findManyByQuery(String query) {
        List<StreamTopic> topics = new ArrayList<StreamTopic>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            String uri = soln.getResource("uri").getURI();
            StreamTopic topic = StreamTopic.find(uri);
            topics.add(topic);
        }
        return topics;
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

}

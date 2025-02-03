package org.hascoapi.entity.pojo;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFilter;
import module.DatabaseExecutionContext;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.annotations.PropertyValueType;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.NameSpaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonFilter("valueFilter")
public class Value extends HADatAcThing {

    private static final Logger log = LoggerFactory.getLogger(Value.class);

    /************************************* 
     *
     *            PROPERTIES
     * 
     *************************************/

    // CORE

    //Field("uri")                                  // HADatAcThing
    //private String uri;
    //Field("type_uri_str")                         // HADatAcThing
    //private String typeUri;
    //Field("hasco_type_uri_str")                   // HADatAcThing
    //private String hascoTypeUri;                    


    // PROVENANCE

    //Field("manager_uri_str")                        // streamUri
    @PropertyField(uri="hasco:hasManager")
    private String hasManagerUri;                       
    //Field("stream_uri_str")
    @PropertyField(uri="hasco:hasStream")
    private String streamUri;
    //Field("study_uri_str")
    //Field("dataset_uri_str")
    //private String datasetUri;                      // streamUri
    //Field("original_id_str")
    @PropertyField(uri="hasco:hasOriginalID")
    private String originalId;
    //Field("timestamp_date")
    @PropertyField(uri="hasco:hasSubmissionTime")
    private Date timestamp;


    // POPULATION

    @PropertyField(uri="hasco:hasStudy")
    private String studyUri;
    //Field("object_collection_type_str")
    @PropertyField(uri="hasco:hasSOCType")
    private String socType;
    //Field("object_uri_str")
    @PropertyField(uri="hasco:hasObject")
    private String objectUri;
    //Field("study_object_uri_str")
    @PropertyField(uri="hasco:hasStudyObject")
    private String studyObjectUri;
    //Field("entry_object_uri_str")
    //private String entryObjectUri;                // ???
    //Field("study_object_type_uri_str")
    @PropertyField(uri="hasco:hasStudyObjectType")
    private String studyObjectTypeUri;


    // ACTUAL VALUE

    //Field("value_str")
    @PropertyField(uri="hasco:hasValue")
    private String value;
    @PropertyField(uri="hasco:hasValueClass")
    private String valueClass;
    //Field("original_value_str")
    @PropertyField(uri="hasco:hasOriginalValue")
    private String originalValue;
    //Field("lod_str")
    @PropertyField(uri="hasco:hasLevelOfDetection")
    private String levelOfDetection;

    // SEMVAR

    @PropertyField(uri="hasco:hasSemanticVariable")
    private String semanticVariableUddi;
    //Field("named_time_str")                        // SEMVAR: Event
    //private String abstractTime;
    //Field("time_value_double")                     // SEMVAR: Event (2)
    //private String timeValue;
    //Field("time_value_unit_uri_str")
    //private String timeValueUnitUri;
    //Field("pid_str")                               // originalId
    //private String pid;                              
    //Field("sid_str")                               // originalId
    //private String sid;                              
    //Field("role_str")                              // SEMVAR: Role
    //private String role;
    //Field("unit_uri_str")                          // SEMVAR: Unit
    //private String unitUri;
    //Field("daso_uri_str")
    //private String dasoUri;
    //Field("dasa_uri_str")
    //private String dasaUri;
    //Field("in_relation_to_uri_str")                // SEMVAR: InRelationTo
    //private String inRelationToUri;
    //Field("entity_uri_str")                        // SEMVAR: Entity
    //private String entityUri;
    //Field("characteristic_uri_str_multi")          // SEMVAR: Attribute
    //private List<String> characteristicUris;
    //Field("categorical_class_uri_str")
    //private String categoricalClassUri;
    //Field("location_latlong")
    //private String location;
    //Field("elevation_double")
    //private double elevation;


    /************************************* 
     *
     *            CONSTRUCT
     * 
     *************************************/

    public Value() {
        typeUri = HASCO.VALUE;
        hascoTypeUri = HASCO.VALUE;
    }

    /************************************* 
     *
     *         GETTERS/SETTERS
     * 
     *************************************/

    public String getHasManagerUri() {
        return hasManagerUri;
    }

    public void setHasManagerUri(String hasManagerUri) {
        this.hasManagerUri = hasManagerUri;
    }

    public String getStreamUri() {
        return streamUri;
    }

    public void setStreamUri(String streamUri) {
        this.streamUri = streamUri;
    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getStudyUri() {
        return studyUri;
    }

    public void setStudyUri(String studyUri) {
        this.studyUri = studyUri;
    }

    public String getSOCType() {
        return socType;
    }

    public void setSOCType(String socType) {
        this.socType = socType;
    }

    public String getObjectUri() {
        return objectUri;
    }

    public void setObjectUri(String objectUri) {
        this.objectUri = objectUri;
    }

    public String getStudyObjectUri() {
        return studyObjectUri;
    }

    public void setStudyObjectUri(String studyObjectUri) {
        this.studyObjectUri = studyObjectUri;
    }

    //public String getEntryObjectUri() {
    //    return entryObjectUri;
    //}

    //public void setEntryObjectUri(String entryObjectUri) {
    //    this.entryObjectUri = entryObjectUri;
    //}

    public String getStudyObjectTypeUri() {
        return studyObjectTypeUri;
    }

    public void setStudyObjectTypeUri(String studyObjectTypeUri) {
        this.studyObjectTypeUri = studyObjectTypeUri;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValueClass() {
        return valueClass;
    }

    public void setValueClass(String valueClass) {
        this.valueClass = valueClass;
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(String originalValue) {
        this.originalValue = originalValue;
    }

    public String getLevelOfDetection() {
        return levelOfDetection;
    }

    public void setLevelOfDetection(String levelOfDetection) {
        this.levelOfDetection = levelOfDetection;
    }

    public String getSemanticVariableUddi() {
        return semanticVariableUddi;
    }

    public void setSemanticVariableUddi(String semanticVariableUddi) {
        this.semanticVariableUddi = semanticVariableUddi;
    }

    public SemanticVariable getSemanticVariable() {
        if (this.semanticVariableUddi == null) {
            return null;
        }
        return SemanticVariable.find(this.semanticVariableUddi);
    }

    /************************************* 
     *
     *         GENERIC METHODS
     * 
     *************************************/

    public static Value find(String uri) {
        if (uri == null || uri.isEmpty()) {
            System.out.println("[ERROR] No valid URI provided to retrieve Study object: " + uri);
            return null;
        }

		//System.out.println("Study.java : in find(): uri = [" + uri + "]");
	    Value value = null;
	    Statement statement;
	    RDFNode object;
	    
	    String queryString = "DESCRIBE <" + uri + ">";
	    Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);
		
		StmtIterator stmtIterator = model.listStatements();

		if (!stmtIterator.hasNext()) {
			return null;
		} else {
			value = new Value();
		}
		
		while (stmtIterator.hasNext()) {
		    statement = stmtIterator.next();
		    object = statement.getObject();
			String str = URIUtils.objectRDFToString(object);
			if (uri != null && !uri.isEmpty()) {
				if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
					value.setLabel(str);
				} else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
				    value.setTypeUri(str); 
				} else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
					value.setHascoTypeUri(str);
				} else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
					value.setComment(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.ORIGINAL_ID)) {
					value.setOriginalId(str);
				//} else if (statement.getPredicate().getURI().equals(HASCO.HAS_SUBMISSION_TIME)) {
				//	value.setTimestamp(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_VALUE)) {
					value.setValue(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_VALUE_CLASS)) {
					value.setValueClass(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_ORIGINAL_VALUE)) {
					value.setOriginalValue(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_LEVEL_OF_DETECTION)) {
					value.setLevelOfDetection(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_STREAM)) {
					value.setStreamUri(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_STUDY)) {
					value.setStudyUri(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_SOC_TYPE)) {
					value.setSOCType(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_OBJECT)) {
					value.setObjectUri(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_STUDY_OBJECT)) {
					value.setStudyObjectUri(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_STUDY_OBJECT_TYPE)) {
					value.setStudyObjectTypeUri(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_SEMANTIC_VARIABLE)) {
					value.setSemanticVariableUddi(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_MANAGER)) {
					value.setHasManagerUri(str);
				}
			}
		}

        value.setUri(uri);

        return value;
    }

    /*
    public static String buildQuery(String user_uri, String study_uri, String subject_uri, String char_uri) {
        String stream_query = "";
        String facet_query = "";
        String q = "";

        List<String> listURI = STR.findAllAccessibleDataStream(user_uri);
        Iterator<String> iter_uri = listURI.iterator();
        while (iter_uri.hasNext()) {
            String uri = iter_uri.next();
            stream_query += "stream_uri_str" + ":\"" + uri + "\"";
            if (iter_uri.hasNext()) {
                stream_query += " OR ";
            }
        }

        if (stream_query.equals("")) {
            return "";
        }

        if (!study_uri.equals("")) {
            facet_query += "study_uri_str" + ":\"" + study_uri + "\"";
        }
        if (!subject_uri.equals("")) {
            if (!study_uri.equals("")) {
                facet_query += " AND ";
            }
            facet_query += "object_uri_str" + ":\"" + subject_uri + "\"";
        }

        if (!char_uri.equals("")) {
            if (!study_uri.equals("") || !subject_uri.equals("")) {
                facet_query += " AND ";
            }
            facet_query += "characteristic_uri_str_multi" + ":\"" + char_uri + "\"";
        }

        if (facet_query.trim().equals("")) {
            q = stream_query;
        } else {
            q = "(" + stream_query + ") AND (" + facet_query + ")";
        }

        return q;
    }
    */

    /*
    public static String buildQuery(List<String> ownedDataStreams, FacetHandler handler) {
        String stream_query = String.join(" OR ", ownedDataStreams.stream()
                .map(p -> "stream_uri_str:\"" + p + "\"")
                .collect(Collectors.toList()));

        if (stream_query.equals("")) {
            return "";
        }

        String facet_query = "";
        String q = "";
        if (handler != null) {
            facet_query = handler.toSolrQuery();
        }

        if (facet_query.trim().equals("") || facet_query.trim().equals("*:*")) {
            q = stream_query;
        } else {
            q = "(" + stream_query + ") AND (" + facet_query + ")";
        }

        return q;
    }
    */

    /*
    public static StreamQueryResult findForViews(String user_uri, String study_uri, 
            String subject_uri, String char_uri, boolean bNumberOfResultsOnly) {
        StreamQueryResult result = new StreamQueryResult();

        String q = buildQuery(user_uri, study_uri, subject_uri, char_uri);
        
        // an empty query happens when current user is not allowed to see any
        // data stream
         
        if (q.equals("")) {
            return result;
        }

        SolrQuery query = new SolrQuery();
        query.setQuery(q);
        if (bNumberOfResultsOnly) {
            query.setRows(0);
        }
        else {
            query.setRows(10000000);
        }
        query.setFacet(false);

        try {
            SolrClient solr = new HttpSolrClient.Builder(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.DATA_ACQUISITION)).build();
            QueryResponse queryResponse = solr.query(query, SolrRequest.METHOD.POST);
            solr.close();
            SolrDocumentList results = queryResponse.getResults();
            if (bNumberOfResultsOnly) {
                result.setDocumentSize(results.getNumFound());
            } else {
                Iterator<SolrDocument> m = results.iterator();
                while (m.hasNext()) {
                    result.documents.add(convertFromSolr(m.next(), null, new HashMap<>()));
                }
            }
        } catch (SolrServerException e) {
            System.out.println("[ERROR] Measurement.findForViews() - SolrServerException message: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("[ERROR] Measurement.findForViews() - IOException message: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] Measurement.findForViews() - Exception message: " + e.getMessage());
        }

        return result;
    }
    */

    /*
    public static StreamQueryResult find(String user_uri, int page, int qtd, String facets) {
        StreamQueryResult result = new StreamQueryResult();

        long startTime = System.currentTimeMillis();
        List<String> ownedDAs = STR.findAllAccessibleDataStream(user_uri);
        log.debug("STR.findAllAccessibleDataStream(user_uri) takes " + (System.currentTimeMillis()-startTime) + "sms to finish");
        if (ownedDAs.isEmpty()) {
            
            // an empty query happens when current user is not allowed to see any
            // data stream
             
            System.out.println("User with this URL: " + user_uri + ": Not allowed to access any Data Stream!");
            return result;
        }

        startTime = System.currentTimeMillis();
        FacetHandler facetHandler = new FacetHandler();
        facetHandler.loadFacetsFromString(facets);
        log.debug("facetHandler.loadFacetsFromString(facets) takes " + (System.currentTimeMillis()-startTime) + "sms to finish");

        startTime = System.currentTimeMillis();
        FacetHandler retFacetHandler = new FacetHandler();
        retFacetHandler.loadFacetsFromString(facets);
        log.debug("retFacetHandler.loadFacetsFromString(facets) takes " + (System.currentTimeMillis()-startTime) + "sms to finish");

        // System.out.println("\nfacetHandler before: " + facetHandler.toSolrQuery());
        // System.out.println("\nfacetHandler before: " + facetHandler.toJSON());

        // Run one time
        // getAllFacetStats(facetHandler, retFacetHandler, result, false);

        // Get facet statistics
        // getAllFacetStats(retFacetHandler, retFacetHandler, result, true);
        startTime = System.currentTimeMillis();
        getAllFacetStats(facetHandler, retFacetHandler, result, true);
        log.debug("getAllFacetStats() takes " + (System.currentTimeMillis()-startTime) + "sms to finish");

        //System.out.println("\n\n\nfacetHandler after: " + retFacetHandler.bottommostFacetsToSolrQuery());
        //System.out.println("\n\n\nfacetHandler after: " + retFacetHandler.toJSON());

        // Get documents
        long docSize = 0;

        //String q = buildQuery(ownedDAs, retFacetHandler);
        String q = buildQuery(ownedDAs, facetHandler);

        //System.out.println("measurement solr query: " + q);

        SolrQuery query = new SolrQuery();
        query.setQuery(q);
        if (page != -1) {
            query.setStart((page - 1) * qtd);
            query.setRows(qtd);
        } else {
            query.setRows(99999999);
        }
        query.setFacet(true);
        query.setFacetLimit(-1);

        try {
            startTime = System.currentTimeMillis();
            SolrClient solr = new HttpSolrClient.Builder(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.DATA_ACQUISITION)).build();
            QueryResponse queryResponse = solr.query(query, SolrRequest.METHOD.POST);
            solr.close();
            log.debug("solr.query takes " + (System.currentTimeMillis()-startTime) + "sms to finish");

            SolrDocumentList docs = queryResponse.getResults();
            docSize = docs.getNumFound();
            System.out.println("Num of results: " + docSize);

            startTime = System.currentTimeMillis();
            Set<String> uri_set = new HashSet<String>();
            Map<String, STR> cachedDA = new HashMap<String, STR>();
            Map<String, String> mapClassLabel = generateCodeClassLabel();
            log.debug("generateCodeClassLabel() takes " + (System.currentTimeMillis()-startTime) + "sms to finish");

            Iterator<SolrDocument> iterDoc = docs.iterator();
            while (iterDoc.hasNext()) {

                startTime = System.currentTimeMillis();
                Measurement measurement = convertFromSolr(iterDoc.next(), cachedDA, mapClassLabel);
                log.debug("convertFromSolr() takes " + (System.currentTimeMillis()-startTime) + "sms to finish");

                result.addDocument(measurement);
                uri_set.add(measurement.getEntityUri());
                uri_set.addAll(measurement.getCharacteristicUris());
                uri_set.add(measurement.getUnitUri());
            }

            // Assign labels of entity, characteristic, and units collectively
            startTime = System.currentTimeMillis();
            Map<String, String> cachedLabels = Measurement.generateCachedLabel(new ArrayList<String>(uri_set));
            for (Measurement measurement : result.getDocuments()) {
                measurement.setLabels(cachedLabels);
            }
            log.debug("generateCachedLabel() takes " + (System.currentTimeMillis()-startTime) + "sms to finish");

        } catch (SolrServerException e) {
            System.out.println("[ERROR] Measurement.find() - SolrServerException message: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("[ERROR] Measurement.find() - IOException message: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] Measurement.find() - Exception message: " + e.getMessage());
            e.printStackTrace();
        }

        result.setDocumentSize(docSize);

        return result;
    }
    */

    /*
    public static StreamQueryResult findAsync(String user_uri, int page, int qtd, String facets, DatabaseExecutionContext databaseExecutionContext)  {

        StreamQueryResult resultAsync = new StreamQueryResult();

        CompletableFuture<List<String>> promiseOfOwnedDAs = CompletableFuture.supplyAsync((
                () -> { return STR.findAllAccessibleDataStream(user_uri); }
        ), databaseExecutionContext);

        CompletableFuture<StreamQueryResult> promiseOfFacetStats = CompletableFuture.supplyAsync((
                () -> { return getAllFacetStatsWrapper(resultAsync, facets, databaseExecutionContext); }
        ), databaseExecutionContext);

        List<String> ownedDAs = null;
        try {
            ownedDAs = promiseOfOwnedDAs.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        StreamQueryResult result = null;
        try {
            result = promiseOfFacetStats.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        // CompletableFuture<StreamQueryResult> ans = promiseOfOwnedDAs.thenCombine(promiseOfFacetStats, (ownedDAs, facetResult) -> {

        long startTime = System.currentTimeMillis();
        if (ownedDAs.isEmpty()) {
            System.out.println("User with this URL: " + user_uri + ": Not allowed to access any Data Stream!");
            return result;
        }

        long docSize = 0;

        FacetHandler facetHandler = new FacetHandler();
        facetHandler.loadFacetsFromString(facets);
        String q = buildQuery(ownedDAs, facetHandler);

        SolrQuery query = new SolrQuery();
        query.setQuery(q);
        if (page != -1) {
            query.setStart((page - 1) * qtd);
            query.setRows(qtd);
        } else {
            query.setRows(99999999);
        }
        query.setFacet(true);
        query.setFacetLimit(-1);

        try {
            SolrClient solr = new HttpSolrClient.Builder(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.DATA_ACQUISITION)).build();
            QueryResponse queryResponse = solr.query(query, SolrRequest.METHOD.POST);
            solr.close();

            SolrDocumentList docs = queryResponse.getResults();
            docSize = docs.getNumFound();
            System.out.println("Num of results: " + docSize);

            Set<String> uri_set = new HashSet<String>();
            Map<String, STR> cachedDA = new HashMap<String, STR>();
            Map<String, String> mapClassLabel = generateCodeClassLabelFacetSearch();

            Iterator<SolrDocument> iterDoc = docs.iterator();
            while (iterDoc.hasNext()) {
                Measurement measurement = convertFromSolr(iterDoc.next(), cachedDA, mapClassLabel);
                result.addDocument(measurement);
                uri_set.add(measurement.getEntityUri());
                uri_set.addAll(measurement.getCharacteristicUris());
                uri_set.add(measurement.getUnitUri());
            }

            // Assign labels of entity, characteristic, and units collectively
            Map<String, String> cachedLabels = Measurement.generateCachedLabelFacetSearch(new ArrayList<String>(uri_set));
            for (Measurement measurement : result.getDocuments()) {
                measurement.setLabels(cachedLabels);
            }

        } catch (SolrServerException e) {
            System.out.println("[ERROR] Measurement.find() - SolrServerException message: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("[ERROR] Measurement.find() - IOException message: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] Measurement.find() - Exception message: " + e.getMessage());
            e.printStackTrace();
        }

        result.setDocumentSize(docSize);

        log.debug("findAsync takes: " + (System.currentTimeMillis()-startTime));

        return result;
        //});

        //return result;

    }
    */

    /*
    public static StreamQueryResult getAllFacetStatsWrapper(StreamQueryResult result, String facets, DatabaseExecutionContext databaseExecutionContext) {

        long startTime = System.currentTimeMillis();

        FacetHandler facetHandler = new FacetHandler();
        facetHandler.loadFacetsFromString(facets);

        FacetHandler retFacetHandler = new FacetHandler();
        retFacetHandler.loadFacetsFromString(facets);

        log.debug("findAsync -> getAllFacetStatsWrapper-1: " + (System.currentTimeMillis()-startTime));

        startTime = System.currentTimeMillis();
        getAllFacetStatsAsync(facetHandler, retFacetHandler, result, true, databaseExecutionContext);
        log.debug("findAsync -> getAllFacetStatsWrapper-2: " + (System.currentTimeMillis()-startTime));

        return result;

    }
    */

    /*
    public static void getAllFacetStats(
            FacetHandler facetHandler, 
            FacetHandler retFacetHandler,
            StreamQueryResult result,
            boolean bAddToResults) {

        FacetTree fTreeS = new FacetTree();
        fTreeS.setTargetFacet(STR.class);
        fTreeS.addUpperFacet(Study.class);

        long startTime = System.currentTimeMillis();
        Pivot pivotS = getFacetStats(fTreeS, 
                retFacetHandler.getFacetByName(FacetHandler.STUDY_FACET), 
                facetHandler);
        log.debug("getFacetStats(fTreeS = " + (System.currentTimeMillis()-startTime) + " sms to finish");

        FacetTree fTreeOC = new FacetTree();
        fTreeOC.setTargetFacet(StudyObjectType.class);
        //fTreeOC.addUpperFacet(ObjectCollectionType.class);
        fTreeOC.addUpperFacet(StudyObjectRole.class);
        startTime = System.currentTimeMillis();
        Pivot pivotOC = getFacetStats(fTreeOC, 
                retFacetHandler.getFacetByName(FacetHandler.OBJECT_COLLECTION_FACET), 
                facetHandler);
        log.debug("getFacetStats(fTreeOC = " + (System.currentTimeMillis()-startTime) + " sms to finish");

        //
        //  The facet tree EC computes the entity-attribute indicators for indicators based on property's main attribute 
        //
        FacetTree fTreeEC = new FacetTree();
        fTreeEC.setTargetFacet(AttributeInstance.class);
        fTreeEC.addUpperFacet(Indicator.class);
        fTreeEC.addUpperFacet(EntityRole.class);
        //fTreeEC.addUpperFacet(Category.class);
        fTreeEC.addUpperFacet(InRelationToInstance.class);
        fTreeEC.addUpperFacet(EntityInstance.class);
        startTime = System.currentTimeMillis();
        Pivot pivotEC = getFacetStats(fTreeEC, 
                retFacetHandler.getFacetByName(FacetHandler.ENTITY_CHARACTERISTIC_FACET), 
                facetHandler);
        log.debug("getFacetStats(fTreeEC = " + (System.currentTimeMillis()-startTime) + " sms to finish");


        //
        //  The facet tree EC computes the entity-attribute indicators for indicators based on property's in-relation-to attribute 
        //
        FacetTree fTreeEC2 = new FacetTree();
        fTreeEC2.setTargetFacet(AttributeInstance.class);
        fTreeEC2.addUpperFacet(Indicator.class);
        fTreeEC2.addUpperFacet(EntityRole.class);
        //fTreeEC2.addUpperFacet(Category.class);
        fTreeEC2.addUpperFacet(InRelationToInstance.class);
        fTreeEC2.addUpperFacet(EntityInstance.class);
        startTime = System.currentTimeMillis();
        Pivot pivotEC2 = getFacetStats(fTreeEC2, 
                retFacetHandler.getFacetByName(FacetHandler.ENTITY_CHARACTERISTIC_FACET2), 
                facetHandler);
        log.debug("getFacetStats(fTreeEC2 = " + (System.currentTimeMillis()-startTime) + " sms to finish");

        //
        //  Merging the computation result of pivotEC2 into pivotEC
        //
        pivotEC.addChildrenFromPivot(pivotEC2);
        pivotEC.normalizeCategoricalVariableLabels(retFacetHandler.getFacetByName(FacetHandler.ENTITY_CHARACTERISTIC_FACET), facetHandler);
            	
        FacetTree fTreeU = new FacetTree();
        fTreeU.setTargetFacet(UnitInstance.class);
        startTime = System.currentTimeMillis();
        Pivot pivotU = getFacetStats(fTreeU, 
                retFacetHandler.getFacetByName(FacetHandler.UNIT_FACET),
                facetHandler);
        log.debug("getFacetStats(fTreeU = " + (System.currentTimeMillis()-startTime) + " sms to finish");

        FacetTree fTreeT = new FacetTree();
        fTreeT.setTargetFacet(TimeInstance.class);
        //fTreeT.addUpperFacet(DASEType.class);
        startTime = System.currentTimeMillis();
        Pivot pivotT = getFacetStats(fTreeT, 
                retFacetHandler.getFacetByName(FacetHandler.TIME_FACET),
                facetHandler);
        log.debug("getFacetStats(fTreeT = " + (System.currentTimeMillis()-startTime) + " sms to finish");

        FacetTree fTreePI = new FacetTree();
        fTreePI.setTargetFacet(STR.class);
        fTreePI.addUpperFacet(Platform.class);
        fTreePI.addUpperFacet(Instrument.class);
        startTime = System.currentTimeMillis();
        Pivot pivotPI = getFacetStats(fTreePI, 
                retFacetHandler.getFacetByName(FacetHandler.PLATFORM_INSTRUMENT_FACET),
                facetHandler);
        log.debug("getFacetStats(fTreePI = " + (System.currentTimeMillis()-startTime) + " sms to finish");

        if (bAddToResults) {
            result.extra_facets.put(FacetHandler.STUDY_FACET, pivotS);
            result.extra_facets.put(FacetHandler.OBJECT_COLLECTION_FACET, pivotOC);
            result.extra_facets.put(FacetHandler.ENTITY_CHARACTERISTIC_FACET, pivotEC);
            result.extra_facets.put(FacetHandler.UNIT_FACET, pivotU);
            result.extra_facets.put(FacetHandler.TIME_FACET, pivotT);
            result.extra_facets.put(FacetHandler.PLATFORM_INSTRUMENT_FACET, pivotPI);
        }
               
    }
    */

    /*
    private static void getAllFacetStatsAsync(
            FacetHandler facetHandler,
            FacetHandler retFacetHandler,
            StreamQueryResult result,
            boolean bAddToResults, DatabaseExecutionContext databaseExecutionContext) {

        AtomicReference<Pivot> pEC = new AtomicReference<>();
        AtomicReference<Pivot> pEC2 = new AtomicReference<>();

        CompletableFuture<FacetTree> promiseOfTreeS = CompletableFuture.supplyAsync((
                () -> {
                    long startTime = System.currentTimeMillis();
                    FacetTree fTreeS = new FacetTree();
                    fTreeS.setTargetFacet(STR.class);
                    fTreeS.addUpperFacet(Study.class);
                    Pivot pivotS = getFacetStats(fTreeS,
                            retFacetHandler.getFacetByName(FacetHandler.STUDY_FACET),
                            facetHandler);
                    if (bAddToResults) {
                        result.extra_facets.put(FacetHandler.STUDY_FACET, pivotS);
                    }
                    log.debug("getAllFacetStatsAsync - getFacetStats(fTreeS = " + (System.currentTimeMillis() - startTime) + " sms to finish");
                    return fTreeS;
                }
        ), databaseExecutionContext);

        CompletableFuture<FacetTree> promiseOfTreeOC = CompletableFuture.supplyAsync((
                () -> {
                    long startTime = System.currentTimeMillis();
                    FacetTree fTreeOC = new FacetTree();
                    fTreeOC.setTargetFacet(StudyObjectType.class);
                    //fTreeOC.addUpperFacet(ObjectCollectionType.class);
                    fTreeOC.addUpperFacet(StudyObjectRole.class);
                    Pivot pivotOC = getFacetStats(fTreeOC,
                            retFacetHandler.getFacetByName(FacetHandler.OBJECT_COLLECTION_FACET),
                            facetHandler);
                    if (bAddToResults) {
                        result.extra_facets.put(FacetHandler.OBJECT_COLLECTION_FACET, pivotOC);
                    }
                    log.debug("getAllFacetStatsAsync - getFacetStats(fTreeOC = " + (System.currentTimeMillis() - startTime) + " sms to finish");
                    return fTreeOC;
                }
        ), databaseExecutionContext);

        CompletableFuture<FacetTree> promiseOfTreeEC = CompletableFuture.supplyAsync((
                () -> {
                    long startTime = System.currentTimeMillis();
                    FacetTree fTreeEC = new FacetTree();
                    fTreeEC.setTargetFacet(AttributeInstance.class);
                    fTreeEC.addUpperFacet(Indicator.class);
                    fTreeEC.addUpperFacet(EntityRole.class);
                    //fTreeEC.addUpperFacet(Category.class);
                    fTreeEC.addUpperFacet(InRelationToInstance.class);
                    fTreeEC.addUpperFacet(EntityInstance.class);
                    Pivot pivotEC = getFacetStats(fTreeEC,
                            retFacetHandler.getFacetByName(FacetHandler.ENTITY_CHARACTERISTIC_FACET),
                            facetHandler);
                    if (bAddToResults) {
                        result.extra_facets.put(FacetHandler.ENTITY_CHARACTERISTIC_FACET, pivotEC);
                    }
                    pEC.set(pivotEC);
                    log.debug("getAllFacetStatsAsync - getFacetStats(fTreeEC = " + (System.currentTimeMillis() - startTime) + " sms to finish");
                    return fTreeEC;
                }
        ), databaseExecutionContext);

        CompletableFuture<FacetTree> promiseOfTreeEC2 = CompletableFuture.supplyAsync((
                () -> {
                    long startTime = System.currentTimeMillis();
                    FacetTree fTreeEC2 = new FacetTree();
                    fTreeEC2.setTargetFacet(AttributeInstance.class);
                    fTreeEC2.addUpperFacet(Indicator.class);
                    fTreeEC2.addUpperFacet(EntityRole.class);
                    //fTreeEC2.addUpperFacet(Category.class);
                    fTreeEC2.addUpperFacet(InRelationToInstance.class);
                    fTreeEC2.addUpperFacet(EntityInstance.class);
                    Pivot pivotEC2 = getFacetStats(fTreeEC2,
                            retFacetHandler.getFacetByName(FacetHandler.ENTITY_CHARACTERISTIC_FACET2),
                            facetHandler);
                    pEC2.set(pivotEC2);
                    log.debug("getAllFacetStatsAsync - getFacetStats(fTreeEC2 = " + (System.currentTimeMillis() - startTime) + " sms to finish");
                    return fTreeEC2;
                }
        ), databaseExecutionContext);

        //
        //  Merging the computation result of pivotEC2 into pivotEC
        //

        CompletableFuture<FacetTree> promiseOfTreeU = CompletableFuture.supplyAsync((
                () -> {
                    long startTime = System.currentTimeMillis();
                    FacetTree fTreeU = new FacetTree();
                    fTreeU.setTargetFacet(UnitInstance.class);
                    Pivot pivotU = getFacetStats(fTreeU,
                            retFacetHandler.getFacetByName(FacetHandler.UNIT_FACET),
                            facetHandler);
                    if (bAddToResults) {
                        result.extra_facets.put(FacetHandler.UNIT_FACET, pivotU);
                    }
                    log.debug("getAllFacetStatsAsync - getFacetStats(fTreeU = " + (System.currentTimeMillis() - startTime) + " sms to finish");
                    return fTreeU;
                }
        ), databaseExecutionContext);

        CompletableFuture<FacetTree> promiseOfTreeT = CompletableFuture.supplyAsync((
                () -> {
                    long startTime = System.currentTimeMillis();
                    FacetTree fTreeT = new FacetTree();
                    fTreeT.setTargetFacet(TimeInstance.class);
                    //fTreeT.addUpperFacet(DASEType.class);
                    Pivot pivotT = getFacetStats(fTreeT,
                            retFacetHandler.getFacetByName(FacetHandler.TIME_FACET),
                            facetHandler);
                    if (bAddToResults) {
                        result.extra_facets.put(FacetHandler.TIME_FACET, pivotT);
                    }
                    log.debug("getAllFacetStatsAsync - getFacetStats(fTreeT = " + (System.currentTimeMillis() - startTime) + " sms to finish");
                    return fTreeT;
                }
        ), databaseExecutionContext);

        CompletableFuture<FacetTree> promiseOfTreePI = CompletableFuture.supplyAsync((
                () -> {
                    long startTime = System.currentTimeMillis();
                    FacetTree fTreePI = new FacetTree();
                    fTreePI.setTargetFacet(STR.class);
                    fTreePI.addUpperFacet(Platform.class);
                    fTreePI.addUpperFacet(Instrument.class);
                    Pivot pivotPI = getFacetStats(fTreePI,
                            retFacetHandler.getFacetByName(FacetHandler.PLATFORM_INSTRUMENT_FACET),
                            facetHandler);
                    if (bAddToResults) {
                        result.extra_facets.put(FacetHandler.PLATFORM_INSTRUMENT_FACET, pivotPI);
                    }
                    log.debug("getAllFacetStatsAsync - getFacetStats(fTreePI = " + (System.currentTimeMillis() - startTime) + " sms to finish");
                    return fTreePI;
                }
        ),databaseExecutionContext);

        try {

            long currentTime = System.currentTimeMillis();
            FacetTree fTreeS = promiseOfTreeS.get();
            FacetTree fTreeOC = promiseOfTreeOC.get();
            FacetTree fTreeEC = promiseOfTreeEC.get();
            FacetTree fTreeEC2 = promiseOfTreeEC2.get();
            FacetTree fTreeU = promiseOfTreeU.get();
            FacetTree fTreeT = promiseOfTreeT.get();
            FacetTree fTreePI = promiseOfTreePI.get();

            pEC.get().addChildrenFromPivot(pEC2.get());
            pEC.get().normalizeCategoricalVariableLabelsFacetSearch(retFacetHandler.getFacetByName(FacetHandler.ENTITY_CHARACTERISTIC_FACET), facetHandler);
            //pivotEC.addChildrenFromPivot(pivotEC2);
            //pivotEC.normalizeCategoricalVariableLabels(retFacetHandler.getFacetByName(FacetHandler.ENTITY_CHARACTERISTIC_FACET), facetHandler);

            log.debug("getAllFacetStatsAsync - final stage: " + (System.currentTimeMillis()-currentTime));

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }
    */

    /*
    private static Pivot getFacetStats(
            FacetTree fTree, 
            Facet facet,
            FacetHandler facetHandler) {
        long startTime = System.currentTimeMillis();
        Pivot pivot = new Pivot();
        fTree.retrieveFacetData(0, facet, facetHandler, pivot);
        pivot.recomputeStats();
        log.debug("***** bottom getFacetStats:" + (System.currentTimeMillis()-startTime));
        return pivot;
    }
    */

    /*
    public static long getNumByDataStream(STR dataStream) {
        SolrClient solr = new HttpSolrClient.Builder(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.DATA_ACQUISITION)).build();
        SolrQuery query = new SolrQuery();
        query.set("q", "stream_uri_str:\"" + dataStream.getUri() + "\"");
        query.set("rows", "10000000");

        try {
            QueryResponse response = solr.query(query);
            solr.close();
            SolrDocumentList results = response.getResults();
            // Update the data set URI list
            dataStream.deleteAllDatasetURIs();
            Iterator<SolrDocument> iter = results.iterator();
            while (iter.hasNext()) {
                SolrDocument doc = iter.next();
                dataStream.addDatasetUri(SolrUtils.getFieldValue(doc, "dataset_uri_str"));
            }
            return results.getNumFound();
        } catch (Exception e) {
            System.out.println("[ERROR] Measurement.findByDataStreamUri(stream_uri) - Exception message: "
                    + e.getMessage());
        }

        return 0;
    }
    */

    /* Possible concepts:
         - HASCO.DATA_ACQUISITION
         - HASCO.STUDY_OBJECT
         - HASCO.DA_SCHEMA_ATTRIBUTE
         - HASCO.DA_SCHEMA_OBJECT
         - HASCO.DATA_FILE
     */
    /*
    public static List<Measurement> findByConceptAndUri(String concept_uri, String uri) {
        List<Measurement> listMeasurement = new ArrayList<Measurement>();

        SolrClient solr = new HttpSolrClient.Builder(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.DATA_ACQUISITION)).build();
        SolrQuery query = new SolrQuery();
        if (concept_uri.equals(HASCO.DATA_ACQUISITION)) {
            query.set("q", "stream_uri_str:\"" + uri + "\"");
        } else if (concept_uri.equals(HASCO.STUDY_OBJECT)) {
            query.set("q", "object_uri_str:\"" + uri + "\"");
        } else if (concept_uri.equals(HASCO.DA_SCHEMA_ATTRIBUTE)) {
            query.set("q", "dasa_uri_str:\"" + uri + "\"");
        } else if (concept_uri.equals(HASCO.DA_SCHEMA_OBJECT)) {
            query.set("q", "daso_uri_str:\"" + uri + "\"");
        } else if (concept_uri.equals(HASCO.DATA_FILE)) {
            query.set("q", "dataset_uri_str:\"" + uri + "\"");
        }
        query.set("rows", "10000000");

        try {
            QueryResponse response = solr.query(query);
            solr.close();
            SolrDocumentList results = response.getResults();
            Iterator<SolrDocument> i = results.iterator();
            while (i.hasNext()) {
                Measurement measurement = convertFromSolr(i.next(), null, new HashMap<>());
                listMeasurement.add(measurement);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Measurement.findByDataStreamUri(stream_uri) - Exception message: "
                    + e.getMessage());
        }

        return listMeasurement;
    }
    */

    /*
    public static Map<String, String> generateCachedLabel(List<String> uris) {
        Map<String, String> results = new HashMap<String, String>();

        List<String> validURIs = new ArrayList<String>();
        // Set default label as local name
        for (String uri : uris) {
            if (URIUtils.isValidURI(uri)) {
                results.put(uri, URIUtils.getBaseName(uri));
                validURIs.add(uri);
            } else {
                results.put(uri, uri);
            }
        }

        String valueConstraint = "";
        if (uris.isEmpty()) {
            return results;
        } else {
            valueConstraint = " VALUES ?uri { " + HADatAcThing.stringify(validURIs) + " } ";
        }

        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += "SELECT ?uri ?label WHERE { \n"
                + valueConstraint + " \n"
                + " ?uri rdfs:label ?label . \n"
                + "}";

        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.METADATA_SPARQL), query);

            while (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                if (soln.get("label") != null && !soln.get("label").toString().isEmpty()) {
                    results.put(soln.get("uri").toString(), soln.get("label").toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
    */

    /*
    public static Map<String, String> generateCachedLabelFacetSearch(List<String> uris) {
        Map<String, String> results = new HashMap<String, String>();

        List<String> validURIs = new ArrayList<String>();
        // Set default label as local name
        for (String uri : uris) {
            if (URIUtils.isValidURI(uri)) {
                results.put(uri, URIUtils.getBaseName(uri));
                validURIs.add(uri);
            } else {
                results.put(uri, uri);
            }
        }

        String valueConstraint = "";
        if (uris.isEmpty()) {
            return results;
        } else {
            valueConstraint = " VALUES ?uri { " + HADatAcThing.stringify(validURIs) + " } ";
        }

        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += "SELECT ?uri ?label WHERE { \n"
                + valueConstraint + " \n"
                + " ?uri rdfs:label ?label . \n"
                + "}";

        try {
            ResultSetRewindable resultsrw = SPARQLUtilsFacetSearch.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.METADATA_SPARQL), query);

            while (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                if (soln.get("label") != null && !soln.get("label").toString().isEmpty()) {
                    results.put(soln.get("uri").toString(), soln.get("label").toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
    */

    /*
    public static Map<String, String> generateCodeClassLabel() {
        Map<String, String> results = new HashMap<String, String>();

        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += "SELECT ?possibleValue ?class ?codeLabel ?label WHERE { \n"
                + "?possibleValue a hasco:PossibleValue . \n"
                + "?possibleValue hasco:hasClass ?class . \n"
                + "OPTIONAL { ?possibleValue hasco:hasCodeLabel ?codeLabel } . \n"
                + "OPTIONAL { ?class rdfs:label ?label } . \n"
                + "}";

        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.METADATA_SPARQL), query);

            while (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                if (soln.get("label") != null && !soln.get("label").toString().isEmpty()) {
                    results.put(soln.get("class").toString(), soln.get("label").toString());
                } else if (soln.get("codeLabel") != null && !soln.get("codeLabel").toString().isEmpty()) {
                    results.put(soln.get("class").toString(), soln.get("codeLabel").toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
    */

    /*
    public static Map<String, String> generateCodeClassLabelFacetSearch() {
        Map<String, String> results = new HashMap<String, String>();

        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += "SELECT ?possibleValue ?class ?codeLabel ?label WHERE { \n"
                + "?possibleValue a hasco:PossibleValue . \n"
                + "?possibleValue hasco:hasClass ?class . \n"
                + "OPTIONAL { ?possibleValue hasco:hasCodeLabel ?codeLabel } . \n"
                + "OPTIONAL { ?class rdfs:label ?label } . \n"
                + "}";

        try {
            ResultSetRewindable resultsrw = SPARQLUtilsFacetSearch.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.METADATA_SPARQL), query);

            while (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                if (soln.get("label") != null && !soln.get("label").toString().isEmpty()) {
                    results.put(soln.get("class").toString(), soln.get("label").toString());
                } else if (soln.get("codeLabel") != null && !soln.get("codeLabel").toString().isEmpty()) {
                    results.put(soln.get("class").toString(), soln.get("codeLabel").toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
    */

    /*
    public void setLabels(Map<String, String> cache) {
        if (cache.containsKey(getEntityUri())) {
            setEntity(cache.get(getEntityUri()));
        }

        List<String> attributes = new ArrayList<String>();
        for (String attributeUri : getCharacteristicUris()) {
            if (cache.containsKey(attributeUri)) {
                attributes.add(cache.get(attributeUri));
            } else {
                attributes.add(attributeUri);
            }
        }
        //if (attributes.size() > 0) {
        //    setCharacteristic(String.join("; ", attributes));
        //}

        if (cache.containsKey(getUnitUri())) {
            setUnit(cache.get(getUnitUri()));
        }
    }
    */

    /*
    public static List<String> getFieldNames() {
        List<String> results = new ArrayList<String>();
        java.lang.reflect.Field[] fields = Measurement.class.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            // Not include dates
            if (field.getType() != Date.class) {
                results.add(field.getName());
            }
        }

        return results;
    }
    */

    /*
    // helper function to check if a give string has all digital and ","
    public boolean isAllNumerical(String strValue) {
        if ( strValue == null || strValue.length() == 0 ) return false;
        for ( char c : strValue.toCharArray() ) {
            if ( !Character.isDigit(c) && c != ',' && c != '.' ) return false;
        }
        return true;
    }
    */

    /*
    public String toCSVRow(List<String> fieldNames) {
        List<String> values = new ArrayList<String>();
        for (String name : fieldNames) {
            Object obj = null;
            try {
                obj = Measurement.class.getDeclaredField(name).get(this);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            if (obj != null) {
                values.add(obj.toString());
            } else {
                values.add("");
            }
        }

        return String.join(",", values);
    }
    */

}

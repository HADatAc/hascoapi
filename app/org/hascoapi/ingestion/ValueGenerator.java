package org.hascoapi.ingestion;

import java.time.Instant;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.hascoapi.entity.pojo.Stream;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.entity.pojo.PossibleValue;
import org.hascoapi.entity.pojo.SOCGroup;
import org.hascoapi.entity.pojo.StudyObject;
import org.hascoapi.entity.pojo.StudyObjectMatching;
import org.hascoapi.entity.pojo.SemanticDataDictionary;
import org.hascoapi.entity.pojo.SDDAttribute;
import org.hascoapi.entity.pojo.SDDObject;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.HADatAcThing;
import org.hascoapi.entity.pojo.Value;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.Feedback;

import org.hascoapi.vocabularies.HASCO;

public class ValueGenerator extends BaseGenerator {

	public static final int FILEMODE = 0;
	public static final int MSGMODE = 1;

    private int mode;
    private Stream stream;
    private DataFile dataFile;

    private SemanticDataDictionary schema = null;
    private Map<String, SDDObject> mapSchemaObjects = new HashMap<String, SDDObject>();
    private Map<String, SDDObject> mapSchemaEvents = new HashMap<String, SDDObject>();

    // ASSIGN positions for MetaDASAs
    private int posTimestamp = -1;
    private int posTimeInstant = -1;
    private int posNamedTime = -1;
    private int posId = -1;
    private int posOriginalId = -1;
    private int posEntity = -1;
    private int posUnit = -1;
    private int posInRelation = -1;
    private int posLOD = -1;
    private int posGroup = -1;
    private int posMatching = -1;

    private long totalCount = 0;

    private Map<String, Map<String, String>> possibleValues = null;
    private Map<String, String> urisByLabels = null;
    //private Map<String, Map<String, String>> mapIDStudyObjects = null;
    private Map<String,SOCGroup> groupBySocAndId = null;
    private StudyObject cellScopeObject = null;
    private StudyObjectCollection cellScopeSOC = null;
    private Map<String,StudyObjectCollection> matchingSOCs = null;

    private String dasoUnitUri = "";

    private int rowErrors = 0;
    private int rowErrorsLimit = 20;

    //private List<DASVirtualObject> templateList = new ArrayList<DASVirtualObject>();
    private DASOInstanceGenerator dasoiGen = null;

    public ValueGenerator(int mode, DataFile dataFile, Stream stream, SemanticDataDictionary schema, DASOInstanceGenerator dasoiGen) {
        super(dataFile);
        this.mode = mode;
        if (mode == MSGMODE) {
        	this.logger = stream.getMessageLogger();
        	//this.totalCount = stream.getIngestedMessages();
        }
        if (mode == FILEMODE) {
            this.dataFile = dataFile;
        }
        this.stream = stream;
        this.schema = schema;
    	this.dasoiGen = dasoiGen;

    	boolean cont = true;
        if (stream.hasCellScope()) {
        	//System.out.println("Measurement Generator: hasCellScope is TRUE");
        	cellScopeObject = StudyObject.find(URIUtils.replacePrefixEx(stream.getCellScopeUri().get(0).trim()));
        	//System.out.println("StudyObject's URI: [" + URIUtils.replacePrefixEx(da.getCellScopeUri().get(0).trim()) + "]");
        	if (cellScopeObject == null) {
        		System.out.println("No scope object");
        	} else {
        		cellScopeSOC = null;//StudyObjectCollection.find(cellScopeObject.getIsMemberOf());
        	}
        } else {
        	//System.out.println("Measurement Generator: hasCellScope is FALSE");
        	//if (!dasoiGen.initiateCache(stream.getStudyUri())) {
        	//	logger.printExceptionById("DA_00001");
        	//	cont = false;
        	//}
        	matchingSOCs = dasoiGen.getMatchingSOCs();
        }
        if (cont) {
        	//System.out.println("Measurement Generator: setting STUDY URI");
    		// Store necessary information before hand to avoid frequent SPARQL queries
    		setStudyUri(stream.getStudyUri());
    		urisByLabels = SemanticDataDictionary.findAllUrisByLabel(schema.getUri());
    		//possibleValues = PossibleValue.findPossibleValues(stream.getSchemaUri());
    		//dasoUnitUri = urisByLabels.get(schema.getUnitLabel());
    		groupBySocAndId = new HashMap<String,SOCGroup>();
        }
        rowErrors = 0;

    }

    @Override
    public void preprocess() throws Exception {
        //System.out.println("[Parser] indexMeasurements()...");

        // ASSIGN values for tempPositionInt
        List<String> unknownHeaders;
        if (mode == FILEMODE) {
        	unknownHeaders = schema.defineTemporaryPositions(file.getHeaders());
        } else {
        	//unknownHeaders = schema.defineTemporaryPositions(stream.getHeaders());
        }

        //System.out.println("DASA after defineTemporaryPositions]");
    	for (SDDAttribute dasa : schema.getAttributes()) {
            //System.out.println("DASA URI: [" + dasa.getUri() + "]   Position: [" + dasa.getTempPositionInt() + "]");
    	}

        //if (!unknownHeaders.isEmpty()) {
        //    logger.addLine(Feedback.println(Feedback.WEB,
        //            "[WARNING] Failed to match the following "
        //                    + unknownHeaders.size() + " headers: " + unknownHeaders));
        //}

        // if (!schema.getTimestampLabel().equals("")) {
        //     posTimestamp = schema.tempPositionOfLabel(schema.getTimestampLabel());
        //     //System.out.println("posTimestamp: " + posTimestamp);
        // }
        // if (!schema.getTimeInstantLabel().equals("")) {
        //     posTimeInstant = schema.tempPositionOfLabel(schema.getTimeInstantLabel());
        //     //System.out.println("posTimeInstant: " + posTimeInstant);
        // }
        // if (!schema.getNamedTimeLabel().equals("")) {
        //     posNamedTime = schema.tempPositionOfLabel(schema.getNamedTimeLabel());
        //     //System.out.println("posNamedTime: " + posNamedTime);
        // }
        // if (!schema.getIdLabel().equals("")) {
        //     posId = schema.tempPositionOfLabel(schema.getIdLabel());
        //     //System.out.println("posId: " + posId);
        // }
        // if (!schema.getOriginalIdLabel().equals("")) {
        //     posOriginalId = schema.tempPositionOfLabel(schema.getOriginalIdLabel());
        //     //System.out.println("posOriginalId: " + posOriginalId);
        // }
        // if (!schema.getEntityLabel().equals("")) {
        //     posEntity = schema.tempPositionOfLabel(schema.getEntityLabel());
        //     //System.out.println("posEntity: " + posEntity);
        // }
        // if (!schema.getUnitLabel().equals("")) {
        //     posUnit = schema.tempPositionOfLabel(schema.getUnitLabel());
        //     //System.out.println("posUnit: " + posUnit);
        // }
        // if (!schema.getInRelationToLabel().equals("")) {
        //     posInRelation = schema.tempPositionOfLabel(schema.getInRelationToLabel());
        //     //System.out.println("posInRelation: " + posInRelation);
        // }
        // if (!schema.getLODLabel().equals("")) {
        //     posLOD = schema.tempPositionOfLabel(schema.getLODLabel());
        //     //System.out.println("posLOD: " + posLOD);
        // }
        // if (!schema.getGroupLabel().equals("")) {
        //     posGroup = schema.tempPositionOfLabel(schema.getGroupLabel());
        //     //System.out.println("posGroup: " + posGroup);
        // }
        // if (!schema.getMatchingLabel().equals("")) {
        //     posMatching = schema.tempPositionOfLabel(schema.getMatchingLabel());
        //     //System.out.println("posMatching: " + posMatching);
        // }

        //System.out.println("possibleValues: " + possibleValues);

        // Comment out row instance generation
        // Map<String, DASOInstance> rowInstances = new HashMap<String, DASOInstance>();
    }

    @Override
    public HADatAcThing createObject(Record record, int rowNumber, String selector) throws Exception {
      	//System.out.println("rowNumber: " + rowNumber);

    	//System.out.println("Position 0 : [" + record.getValueByColumnIndex(0) + "]");
    	//System.out.println("Position 1 : [" + record.getValueByColumnIndex(1) + "]");

    	//for (SDDAttribute dasa : schema.getAttributes()) {
        //    System.out.println("DASA URI: [" + dasa.getUri() + "]   Position: [" + dasa.getTempPositionInt() + "]");
    	//}

    	Map<String, Map<String,String>> objList = null;
        Map<String,String> groundObj = null;
        String socUri = "";
        String objUri = "";
        boolean doMatching = false;
        boolean doGroup = false;
        StudyObject cellObject = null;
        StudyObjectCollection cellSoc = null;
        if (stream.hasCellScope()) {
        	if (selector == null) {
        		return null;
        	}
        	//cellObject = stream.getTopicsMap().get(selector).getStudyObject();
        	//objUri = cellObject.getUri();
        	//if (objUri == null) {
        	//	return null;
        	//}
        	//cellSoc = stream.getTopicsMap().get(selector).getSOC();
        	//socUri = cellSoc.getUri();
        	//if (socUri == null) {
        	//	return null;
        	//}
            //socUri = cellScopeSOC.getUri();
            //objUri = cellScopeObject.getUri();
        } else {
            // Objects defined by Row Scope
            String id = "";
            if (!schema.getOriginalIdLabel().equals("")) {
                id = record.getValueByColumnIndex(posOriginalId);
            } else if (!schema.getIdLabel().equals("")) {
                id = record.getValueByColumnIndex(posId);
            }
            objList = dasoiGen.generateRowInstances(id);
            groundObj = dasoiGen.retrieveGroundObject(id);

            if (groundObj == null) {
                if (rowErrors < rowErrorsLimit) {
                    logger.addLine(Feedback.println(Feedback.WEB, String.format(
                    	"[ERROR] ValueGenerator: Could not retrieve Study Object for ID=[%s]",id)));
                	rowErrors++;
                	if (rowErrors == rowErrorsLimit) {
                        logger.addLine(Feedback.println(Feedback.WEB, String.format(
                        	"[ERROR] ValueGenerator: The reporting of ingestion issues has been halted. The limit of %s faulty rows has been exceeded.",rowErrorsLimit)));

                	}
                }
            }

            // socUri and objUri for row scope is defined later under value processing
        }

        Iterator<SDDAttribute> iterAttributes = schema.getAttributes().iterator();
        while (iterAttributes.hasNext()) {
            SDDAttribute dasa = iterAttributes.next();

            // if (!dasa.getPartOfSchema().equals(schema.getUri())){
            //     continue;
            // }
            // if (dasa.getLabel().equals(schema.getTimestampLabel())) {
            //     continue;
            // }
            // if (dasa.getLabel().equals(schema.getTimeInstantLabel())) {
            //     continue;
            // }
            // if (dasa.getLabel().equals(schema.getNamedTimeLabel())) {
            //     continue;
            // }
            // if (dasa.getLabel().equals(schema.getIdLabel())) {
            //     continue;
            // }
            // if (dasa.getLabel().equals(schema.getOriginalIdLabel())) {
            //     continue;
            // }
            // if (dasa.getLabel().equals(schema.getEntityLabel())) {
            //     continue;
            // }
            // if (dasa.getLabel().equals(schema.getUnitLabel())) {
            //     continue;
            // }
            // if (dasa.getLabel().equals(schema.getInRelationToLabel())) {
            //     continue;
            // }
            // if (dasa.getLabel().equals(schema.getLODLabel())) {
            //     continue;
            // }
            // if (dasa.getLabel().equals(schema.getGroupLabel())) {
            // 	doGroup = true;
            // }
            // if (dasa.getLabel().equals(schema.getMatchingLabel())) {
            // 	doMatching = true;
            // }

            Value value = new Value();

            /*==================================*
             *                                  *
             *   SET VALUE  AND ORIGINAL ID     *
             *                                  *
             *==================================*/

            String codeClass = "";
            String originalValue = "";
            if (dasa.getTempPositionInt() < 0 || dasa.getTempPositionInt() >= record.size()) {
                continue;
            } else if (record.getValueByColumnIndex(dasa.getTempPositionInt()).isEmpty()) {
                continue;
            } else {
                originalValue = record.getValueByColumnIndex(dasa.getTempPositionInt());
                String dasa_uri_temp = dasa.getUri();
                //System.out.println("DASA URI: [" + dasa_uri_temp + "]   Position: [" + dasa.getTempPositionInt() + "]");
                value.setOriginalValue(originalValue);
                if (possibleValues.containsKey(dasa_uri_temp)) {
                    if (possibleValues.get(dasa_uri_temp).containsKey(originalValue.toLowerCase())) {
                        value.setValue(possibleValues.get(dasa_uri_temp).get(originalValue.toLowerCase()));
                        if (value.getValue().startsWith("http")) {
                        	codeClass = value.getValue();
                        }
                    } else {
                        value.setValue(originalValue);
                    }
                } else {
                    value.setValue(originalValue);
                }
            }

            /*===========================*
             *                           *
             * SET LEVEL OF DETECTION    *
             *                           *
             *===========================*/
            value.setLevelOfDetection("");
            // if (!schema.getLODLabel().equals("") && posLOD >= 0) {
            //     value.setLevelOfDetection(record.getValueByColumnIndex(posLOD));
            // }

            /*============================*
             *                            *
             *   SET TIME(STAMP)          *
             *                            *
             *============================*/

            /*
              - TimestampLabel is used for machine generated timestamp
              - TimeInstantLabel is used for timestamps told to system to be timestamp, but that are not further processed
              - Abstract times are encoded as DASA's events, and are supposed to be strings
             */
            value.setTimestamp(new Date(0));
            //value.setAbstractTime("");

            // if(dasa.getLabel() == schema.getTimestampLabel()) {
            //     // full-row regular (Epoch) timemestamp
            //     String sTime = record.getValueByColumnIndex(posTimestamp);
            //     //System.out.println("Timestamp received: " + sTime);
            //     int timeStamp = new BigDecimal(sTime).intValue();
            //     //System.out.println("Tmestamp recorded: " + Instant.ofEpochSecond(timeStamp).toString());
            //     value.setTimestamp(Instant.ofEpochSecond(timeStamp).toString());
            // } else if (!schema.getTimeInstantLabel().equals("")) {
            //     // full-row regular (XSD) time interval
            //     String timeValue = record.getValueByColumnIndex(posTimeInstant);
            //     //timeValue = timeValue.replace("-05:00","-0500");
            //     SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            //     //System.out.println("Timestamp received: [" + timeValue + "]");
            //     if (timeValue != null) {
            //     	if (timeValue.length() > 24) {
            //          	timeValue = timeValue.substring(0, 23) + timeValue.substring(timeValue.length() - 6);
            //             //System.out.println("Timestamp adjusted: " + timeValue);
            //         }
            //         try {
            //             Date date = formatter.parse(timeValue);
            //             //System.out.println(date);
            //             //System.out.println(formatter.format(date));
            //             //value.setTimestamp(timeValue);
            //             value.setTimestamp(date);
            //         } catch (Exception e) {
            //         	//System.out.println("Setting current time!");
            //             value.setTimestamp(new Date(0).toInstant().toString());
            //         }
            //     }
            // } else if (!schema.getNamedTimeLabel().equals("")) {
            // 	// full-row named time
            //     String timeValue = record.getValueByColumnIndex(posNamedTime);
            //     if (timeValue != null) {
            //         value.setAbstractTime(timeValue);
            //     } else {
            //         value.setAbstractTime("");
            //     }
            // } else if (dasa.getEventUri() != null && !dasa.getEventUri().equals("")) {
            //     //SDDEvent dase = null;
            //     SDDObject dase = null;
            //     String daseUri = dasa.getEventUri();
            //     if (mapSchemaEvents.containsKey(daseUri)) {
            //         dase = mapSchemaEvents.get(daseUri);
            //     } else {
            //         dase = schema.getEvent(daseUri);
            //         if (dase != null) {
            //             mapSchemaEvents.put(daseUri, dase);
            //         }
            //     }
            //     if (dase != null) {
            //         if (!dase.getEntity().equals("")) {
            //             value.setAbstractTime(dase.getEntity());
            //         } else {
            //             value.setAbstractTime(dase.getUri());
            //         }
            //     }
            // }

            /*===================================*
             *                                   *
             *   SET STUDY                       *
             *                                   *
             *===================================*/
            value.setStudyUri(stream.getStudyUri());

            /*===================================*
             *                                   *
             *   SET OBJECT ID, PID, SID, ROLE   *
             *                                   *
             *===================================*/
            //value.setObjectCollectionType("");
            value.setStudyObjectUri("");
            value.setStudyObjectTypeUri("");
            value.setObjectUri("");
            //value.setPID("");
            //value.setSID("");
            // value.setRole("");
            // value.setEntityUri("");

            String reference = null;
            if (stream.hasCellScope()) {

                // Objects defined by Cell Scope
            	if (objUri != null && socUri != null) {
                    value.setStudyObjectUri(objUri);
                    value.setStudyObjectTypeUri(cellObject.getTypeUri());
                    value.setObjectUri(objUri);
                    //value.setObjectCollectionType(cellSoc.getTypeUri());
                    // value.setRole(cellSoc.getRoleLabel());
            	} else if (stream.getCellScopeName().get(0).equals("*")) {
                    value.setStudyObjectUri(cellScopeObject.getUri());
                    value.setStudyObjectTypeUri(cellScopeObject.getTypeUri());
                    value.setObjectUri(cellScopeObject.getUri());
                    //value.setObjectCollectionType(cellScopeSOC.getTypeUri());
                    // value.setRole(cellScopeSOC.getRoleLabel());
                    if (cellScopeObject.getOriginalId() != null) {
                    	// value.setPID(cellScopeObject.getOriginalId());
                    }
                    //System.out.println("Measurement: ObjectURI (before replace): <" + da.getCellScopeUri().get(0).trim() + ">");
                    //System.out.println("Measurement: ObjectURI (after replace): <" + URIUtils.replacePrefixEx(da.getCellScopeUri().get(0).trim()) + ">");
                } else {
                    // TO DO: implement rest of cell scope
                }
            } else {
                // Objects defined by Row Scope
                String id = "";
                if (!schema.getOriginalIdLabel().equals("")) {
                    id = record.getValueByColumnIndex(posOriginalId);
                } else if (!schema.getIdLabel().equals("")) {
                    id = record.getValueByColumnIndex(posId);
                }

                if (!id.equals("")) {

                	value.setOriginalId(id);
                    reference = dasa.getObjectViewLabel();
                    // value.setEntryObjectUri(this.getEntryObjectUri(id,objList));
                    // objUri = this.getObjectUri(id, reference, objList);

                    if (reference != null && !reference.equals("")) {
                        if (objList.get(reference) == null) {
                            System.out.println("ValueGenerator: [ERROR] Processing objList for reference [" + reference + "]");
                        } else {
                            // from object list
                            objUri = objList.get(reference).get(StudyObject.STUDY_OBJECT_URI);
                            value.setObjectUri(objUri);
                            //value.setObjectCollectionType(objList.get(reference).get(StudyObject.SOC_TYPE));
                            // value.setRole(objList.get(reference).get(StudyObject.SOC_LABEL));
                            if (objList.get(reference).get(StudyObject.SOC_TYPE).equals(HASCO.SAMPLE_COLLECTION)) {
                                // value.setSID(objList.get(reference).get(StudyObject.OBJECT_ORIGINAL_ID));
                            }
                            if (objList.get(reference).get(StudyObject.OBJECT_TIME) != null && !objList.get(reference).get(StudyObject.OBJECT_TIME).equals("")) {
                                // value.setAbstractTime(objList.get(reference).get(StudyObject.OBJECT_TIME));
                            }
                            if (objList.get(reference).get(StudyObject.SOC_URI) != null && !objList.get(reference).get(StudyObject.SOC_URI).equals("")) {
                                socUri = objList.get(reference).get(StudyObject.SOC_URI);
                            }
                            if (objList.get(reference).get(StudyObject.STUDY_OBJECT_TYPE) != null && !objList.get(reference).get(StudyObject.STUDY_OBJECT_TYPE).equals("")) {
                                String entityUri = objList.get(reference).get(StudyObject.STUDY_OBJECT_TYPE);
                                // value.setEntityUri(entityUri);
                                if (entityUri.equals(URIUtils.replacePrefix(StudyObjectMatching.className)) && !doMatching) {
                                    String scopeObjectUri = objList.get(reference).get(StudyObject.SCOPE_OBJECT_URI);
                                	StudyObjectMatching matching = StudyObjectMatching.findByMemberUri(scopeObjectUri);
                                	if (matching != null) {
                                		value.setObjectUri(matching.getUri());
                                	} else {
                                		value.setObjectUri(objUri);
                                	}
                                	//value.setObjectCollectionType();
                                	//value.setRole();
                                	value.setStudyObjectUri(objList.get(reference).get(StudyObject.SCOPE_OBJECT_URI));
                                	value.setStudyObjectTypeUri(groundObj.get(StudyObject.STUDY_OBJECT_TYPE));
                                	//value.setPID();
                                }
                            }

                            // from ground object
                            if (groundObj == null || groundObj.get(StudyObject.STUDY_OBJECT_URI) == null || groundObj.get(StudyObject.STUDY_OBJECT_URI).equals("")) {
                                System.out.println("ValueGenerator: [ERROR] Could not retrieve Ground Object for reference [" + reference + "]");
                            } else {
                                value.setStudyObjectUri(groundObj.get(StudyObject.STUDY_OBJECT_URI));
                                value.setStudyObjectTypeUri(groundObj.get(StudyObject.STUDY_OBJECT_TYPE));
                                // value.setPID(groundObj.get(StudyObject.SUBJECT_ID));
                            }
                        }
                        //System.out.println("[ValueGenerator] For Id=[" + id + "] and reference=[" + reference + "] it was assigned Obj URI=[" + value.getObjectUri() + "]");
                    } else {
                        System.out.println("ValueGenerator: [ERROR]: could not find DASA reference for ID=[" + id + "]");
                    }

                }
            }

            /*=====================================*
             *                                     *
             *   SET GROUP                         *
             *                                     *
             *=====================================*/

            // if (doGroup && !schema.getGroupLabel().equals("") && posGroup >= 0 && !socUri.equals("") && !objUri.equals("")) {
            //     // group value exists
            //     String groupValue = record.getValueByColumnIndex(posGroup);
            //     if (groupValue != null) {

            //     	// verify if SOC has a group with id=groupValue. If not, create new group inside SOC
            //     	SOCGroup grp = null;
            //     	String key = socUri + "=" + groupValue;
            //     	if (groupBySocAndId.containsKey(key)) {
            //     		grp = groupBySocAndId.get(key);
            //     	} else {
            //     		grp = SOCGroup.findBySOCUriAndId(socUri, groupValue);
            //     		if (grp == null) {
            //     			grp = new SOCGroup(socUri, groupValue);
            //     			grp.saveToTripleStore();
            //     			groupBySocAndId.put(key, grp);
            //     		}
            //     	}
            //     	// add object URI to group
            //     	grp.addMemberUri(objUri);
            //     	grp.saveMemberToTripleStore(objUri);

            //     }
            //     doGroup = false;
            // }

        	/*=====================================*
             *                                     *
             *   SET MATCHING                      *
             *                                     *
             *=====================================*/

            // if (doMatching && !schema.getMatchingLabel().equals("") && posMatching >= 0) {
            //     String scopeObjectUri = objList.get(reference).get(StudyObject.SCOPE_OBJECT_URI);
            //     String scopeObjectSOCUri = objList.get(reference).get(StudyObject.SCOPE_OBJECT_SOC_URI);
            // 	String matchingValue = record.getValueByColumnIndex(posMatching);
            //     if (matchingSOCs.containsKey(scopeObjectSOCUri) && matchingValue != null && !matchingValue.isEmpty()) {
            //     	StudyObjectCollection matchingSOC = matchingSOCs.get(scopeObjectSOCUri);
            //     	StudyObjectMatching matching = StudyObjectMatching.find(matchingSOC.getUri(), matchingValue);
            //     	if (matching == null) {
            //     		matching = new StudyObjectMatching(matchingSOC.getUri(), matchingValue);
            //     		matching.addMemberUri(scopeObjectUri);
            //     		matching.saveToTripleStore();
            //     	} else {
            //     		matching.addMemberUri(scopeObjectUri);
            //     		matching.saveLastMemberTripleStore();
            //     	}
            //         value.setObjectUri(matching.getUri());
            //         //value.setObjectCollectionType(matchingSOC.getTypeUri());
            //     }
            //     doMatching = false;
            // }

            /*=====================================*
             *                                     *
             *   SET URI, OWNER AND DA URI         *
             *                                     *
             *=====================================*/

            if (mode == FILEMODE) {
            	value.setUri(URIUtils.replacePrefixEx(value.getStudyUri()) + "/" +
            			URIUtils.replaceNameSpaceEx(stream.getUri()).split(":")[1] + "/" +
            			dasa.getLabel() + "/" );//+
            			//dataFile.getFileName() + "-" + totalCount++);
            } else {
                value.setUri(URIUtils.replacePrefixEx(value.getStudyUri()) + "/" +
                        URIUtils.replaceNameSpaceEx(stream.getUri()).split(":")[1] + "/" +
                        dasa.getLabel() + "/" +
                        stream.getLabel() + "-" + totalCount++);
            }
            // value.setOwnerUri(stream.getOwnerUri());
            // value.setAcquisitionUri(stream.getUri());

            /*======================================*
             *                                      *
             *   SET ENTITY AND CHARACTERISTIC URI  *              *
             *                                      *
             *======================================*/
            // value.setDasoUri(dasa.getObjectUri());
            // value.setDasaUri(dasa.getUri());

            SDDObject daso = null;
            String dasoUri = dasa.getObjectUri();
            if (mapSchemaObjects.containsKey(dasoUri)) {
                daso = mapSchemaObjects.get(dasoUri);
            } else {
                daso = schema.getObject(dasoUri);
                mapSchemaObjects.put(dasoUri, daso);
            }

            // if (value.getEntityUri().equals("")) {
            //     if (null != daso) {
            //         if (daso.getTempPositionInt() > 0) {
            //             // values of daso exist in the columns
            //             String dasoValue = record.getValueByColumnIndex(daso.getTempPositionInt());
            //             if (possibleValues.containsKey(dasa.getObjectUri())) {
            //                 if (possibleValues.get(dasa.getObjectUri()).containsKey(dasoValue.toLowerCase())) {
            //                     value.setEntityUri(possibleValues.get(dasa.getObjectUri()).get(dasoValue.toLowerCase()));
            //                 } else {
            //                     value.setEntityUri(dasoValue);
            //                 }
            //             } else {
            //                 value.setEntityUri(dasoValue);
            //             }
            //         } else {
            //             value.setEntityUri(daso.getEntity());
            //         }
            //     } else {
            //         value.setEntityUri(dasa.getObjectUri());
            //     }
            // }

            // if (!codeClass.isEmpty()) {
            // 	// value.setCharacteristicUris(Arrays.asList(codeClass));
            // 	//System.out.println(">>> POSSIBLE CLASS VALUE: Obj: [" + dasa.getObjectUri() + "]  code: [" + originalValue + "]   class: [" + codeClass + "]");
            // 	//System.out.println(">>> POSSIBLE CLASS VALUE: Current Attr: [" + dasa.getReversedAttributeString() + "]");
            // 	value.setCategoricalClassUri(dasa.getAttributes().get(0));
            // } else {
            // 	value.setCharacteristicUris(Arrays.asList(dasa.getReversedAttributeString()));
            // }

            /*======================================*
             *                                      *
             *   SET IN RELATION TO URI             *
             *                                      *
             *======================================*/
            // value.setInRelationToUri("");

            SDDObject inRelationToDaso = null;
            String inRelationToUri = dasa.getInRelationToUri(URIUtils.replacePrefixEx("sio:SIO_000668"));
            if (mapSchemaObjects.containsKey(inRelationToUri)) {
                inRelationToDaso = mapSchemaObjects.get(inRelationToUri);
            } else {
                inRelationToDaso = schema.getObject(inRelationToUri);
                mapSchemaObjects.put(inRelationToUri, inRelationToDaso);
            }

            codeClass = "";
            if (null != inRelationToDaso) {
                if (inRelationToDaso.getTempPositionInt() > 0) {
                    String inRelationToDasoValue = record.getValueByColumnIndex(inRelationToDaso.getTempPositionInt());
                    if (possibleValues.containsKey(inRelationToUri)) {
                        if (possibleValues.get(inRelationToUri).containsKey(inRelationToDasoValue.toLowerCase())) {
                        	// System.out.println("in possible values");
                            // value.setInRelationToUri(possibleValues.get(inRelationToUri).get(inRelationToDasoValue.toLowerCase()));
                        }
                    }
                } else {
                    // Assign the org.hadatac.entity of inRelationToDaso to inRelationToUri
                    // value.setInRelationToUri(inRelationToDaso.getEntity());
                }
            }

            /*=============================*
             *                             *
             *   SET UNIT                  *
             *                             *
             *=============================*/

            // if (!schema.getUnitLabel().equals("") && posUnit >= 0) {
            //     // unit exists in the columns
            //     String unitValue = record.getValueByColumnIndex(posUnit);
            //     if (unitValue != null) {
            //         if (possibleValues.containsKey(dasoUnitUri)) {
            //             if (possibleValues.get(dasoUnitUri).containsKey(unitValue.toLowerCase())) {
            //                 value.setUnitUri(possibleValues.get(dasoUnitUri).get(unitValue.toLowerCase()));
            //             } else {
            //                 value.setUnitUri(unitValue);
            //             }
            //         } else {
            //             value.setUnitUri(unitValue);
            //         }
            //     }
            // } else {
            //     value.setUnitUri("");
            // }

            // if (value.getUnitUri().equals("") && !dasa.getUnit().equals("")) {
            //     // Assign units from the Unit column of SDD
            //     value.setUnitUri(dasa.getUnit());
            // }

            // /*=================================*
            //  *                                 *
            //  *   SET DATASET                   *
            //  *                                 *
            //  *=================================*/
            // if (mode == FILEMODE) {
            // 	value.setDatasetUri(dataFile.getDatasetUri());
            // } else {
            // 	value.setDatasetUri(stream.getUri());
            // }

            objects.add(value);
        }

        return null;
    }

    // @Override
    // public boolean commitObjectsToSolr(List<HADatAcThing> objects) throws Exception {
    //     SolrClient solr = new HttpSolrClient.Builder(
    //             CollectionUtil.getCollectionPath(CollectionUtil.Collection.DATA_ACQUISITION)).build();

    //     int count = 0;
    //     int batchSize = 10000;

    //     for (HADatAcThing value : objects) {
    //         try {
    //             solr.addBean(value);
    //         } catch (IOException | SolrServerException e) {
    //             System.out.println("[ERROR] SolrClient.addBean - e.Message: " + e.getMessage());
    //         }

    //         // INTERMEDIARY COMMIT
    //         if((++count) % batchSize == 0) {
    //             commitToSolr(solr, batchSize);
    //         }
    //     }

    //     // FINAL COMMIT
    //     commitToSolr(solr, count % batchSize);

    //     stream.addNumberDataPoints(totalCount);
    //     stream.saveToSolr();

    //     logger.addLine(Feedback.println(Feedback.WEB, String.format(
    //             "[OK] %d object(s) have been committed to solr", count)));

    //     return true;
    // }

    // public boolean commitObjectToSolr(HADatAcThing object) throws Exception {
    //     SolrClient solr = new HttpSolrClient.Builder(
    //             CollectionUtil.getCollectionPath(CollectionUtil.Collection.DATA_ACQUISITION)).build();

    //     try {
    //     	solr.addBean(object);
    //     } catch (IOException | SolrServerException e) {
    //     	System.out.println("[ERROR] SolrClient.addBean - e.Message: " + e.getMessage());
    //     }
    //     commitToSolr(solr, -1);
    //     return true;
    // }

    // private void commitToSolr(SolrClient solr, int batch_size) throws Exception {
    //     try {
    //     	/*
    //     	if (batch_size != -1) {
    //     		System.out.println("solr.commit()...");
    //     	}
    //     	*/
    //         solr.commit();
    //     	if (batch_size != -1) {
    //     		//System.out.println(String.format("[OK] Committed %s values!", batch_size));
    //     		logger.addLine(Feedback.println(Feedback.WEB, String.format("[OK] Committed %s values!", batch_size)));
    //     	}
    //     } catch (IOException | SolrServerException e) {
    //         System.out.println("[ERROR] SolrClient.commit - e.Message: " + e.getMessage());
    //         try {
    //             solr.close();
    //         } catch (IOException e1) {
    //             System.out.println("[ERROR] SolrClient.close - e.Message: " + e1.getMessage());
    //         }

    //         throw new Exception("Fail to commit to solr");
    //     }
    // }

    // helper method to get the full URI of the VC object for a given originalId
    private String getEntryObjectUri(String id, Map<String, Map<String,String>> objList ) {

        if ( id == null || id.length() == 0 ) return null;
        if ( objList == null || objList.size() == 0 ) return null;

        for (Map.Entry<String, Map<String,String>> entrySet : objList.entrySet() ) {
            Map map = entrySet.getValue();
            if ( map.containsKey(StudyObject.SUBJECT_ID) ) {
                if (id.equals(map.get(StudyObject.SUBJECT_ID))) {
                    return (String) map.get(StudyObject.STUDY_OBJECT_URI);
                }
            }
        }

        return null;

    }

    @Override
    public String getTableName() {
        return "";
    }
}


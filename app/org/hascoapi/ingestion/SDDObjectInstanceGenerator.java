package org.hascoapi.ingestion;

import com.typesafe.config.ConfigFactory;
import org.hascoapi.entity.pojo.SDD;
import org.hascoapi.entity.pojo.SDDAttribute;
import org.hascoapi.entity.pojo.SDDObject;
import org.hascoapi.entity.pojo.DataFile;
//import org.hascoapi.entity.pojo.ObjectCollection;
//import org.hascoapi.entity.pojo.STR;
import org.hascoapi.entity.pojo.VirtualColumn;
//import org.hascoapi.entity.pojo.StudyObject;
//import org.hascoapi.entity.pojo.StudyObjectMatching;
//import org.hascoapi.entity.pojo.Study;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.ConfigProp;

import java.lang.String;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;


public class SDDObjectInstanceGenerator extends BaseGenerator {

/** 
	private final boolean DEBUG_MODE = false;
	private final int ID_LENGTH = 5;
	
	private final String SIO_OBJECT = "sio:SIO_000776";
    private final String SIO_SAMPLE = "sio:SIO_001050";;
    private final String kbPrefix = ConfigProp.getKbPrefix();
    private STR str;
    private SDD sdd;
    private String mainLabel;
    private SDDObject mainSDDo;
    private String mainSDDoUri;
    private ObjectCollection mainSoc;
    private ObjectCollection groundingSoc;
    private String mainSocUri;
    private String fileName; 
    private Map<String, DataAcquisitionSchemaObject> dasos = new ConcurrentHashMap<String, DataAcquisitionSchemaObject>();  
    private Map<String, ObjectCollection> requiredSocs = new ConcurrentHashMap<String, ObjectCollection>();  
    private List<ObjectCollection> socsList = null;  
    private List<ObjectCollection> groundingPath = new ArrayList<ObjectCollection>();  
    private List<ObjectCollection> reverseGroundingPath;
    private List<String> groundingPathUris = new ArrayList<String>();
    private Map<String, List<ObjectCollection>> socPaths = new HashMap<String, List<ObjectCollection>>(); 
    private Map<String, String> socLabels = new ConcurrentHashMap<String, String>();
    private Map<String, ObjectCollection> socMatchingSOCs = new ConcurrentHashMap<String, ObjectCollection>();
//    private Map<String, String > groundingIds = new ArrayList<String>();
    
    public SDDObjectInstanceGenerator(DataFile dataFile, STR str, String fileName) {
        super(dataFile, str.getStudyUri());

        this.str = str;
        this.sdd = str.getSchema();
        logger.println("Initiating cache for study " + str.getStudyUri());        
        if (!initiateCache(str.getStudyUri())) {
            logger.printExceptionById("DA_00001");
        	return;
        }
        socPaths.clear();
        socLabels.clear();

        // ***************************************************************************************
        //                                                                                       *
        //                MAPPING OF IDENTIFIER AND ASSOCIATED OBJECTS                           *
        //                                                                                       *
        // ***************************************************************************************

        mainLabel = "";
        String origId = sdd.getOriginalIdLabel(); 
        String id = sdd.getIdLabel(); 
        if (origId != null && !origId.equals("")) {
            mainLabel = origId;
        } else if (id != null && !id.equals("")) {
            mainLabel = id;
        }

        if (fileName == null || fileName.equals("")) {
            logger.printException("SDDObjectInstanceGenerator: [ERROR] NO RECORD FILE PROVIDED");
            return;
        } 
        this.fileName = fileName;

        if (mainLabel.equals("")) {
            logger.printException("SDDObjectInstanceGenerator: NO IDENTIFIER");
            return;
        } else {
            logger.println("SDDObjectInstanceGenerator: Study URI: " + studyUri);
            logger.println("SDDObjectInstanceGenerator: Label of main SDDO: " + mainLabel);
        }

        ////////////////////////////////////////////
        // Strictly follow the given order of steps
        if (!retrieveAvailableSOCs()) {
            return;
        }
        if (!identifyMainSDDO()) {
            return;
        }
        if (!identifyGroundingPathForMainSOC()) {
            return;
        }
        if (!identifyTargetSDDoURIs()) {
            return;
        }
        if (!identitySOCsForSDDOs()) {
            return;
        }
        if (!retrieveAdditionalSOCs()) {
            return;
        }
        if (!printRequiredSOCs()) {
            return;
        }
        if (!computePathsForTargetSOCs()) {
            return;
        }
        if (!computeLabelsForTargetSOCs()) {
            return;
        }
        if (!mapSOCsAndMatchings()) {
            return;
        }
        ////////////////////////////////////////////
    }

    private boolean retrieveAvailableSOCs() {
        // 
        //  (1/10) INITIALLY AVAILABLE SOCs
        //

        logger.println("SDDObjectInstanceGenerator: (1/10) ======== INITIALLY AVAILABLE SOCs ========");
        socsList = ObjectCollection.findByStudyUri(studyUri);
        if (socsList == null) {
            logger.println("SDDObjectInstanceGenerator: no SOC is available");
            socsList = new ArrayList<ObjectCollection>();  
        } else {
            for (ObjectCollection soc : socsList) {
                logger.println("SDDObjectInstanceGenerator: SOC: " + soc.getUri() + "   Reference : " + soc.getSOCReference());
            }
        }
        
        return true;
    }

    private boolean identifyMainSDDO() {
        // 
        //  (2/10) IDENTIFY MAIN SDDO and SDDOS REQUIRED FROM SDDAs. THESE SDDOS ARE LISTED IN STEP (4)
        //

        mainSDDoUri = "";
        Iterator<DataAcquisitionSchemaAttribute> iterAttributes = sdd.getAttributes().iterator();
        while (iterAttributes.hasNext()) {
            DataAcquisitionSchemaAttribute dasa = iterAttributes.next();
            String dasoUri = dasa.getObjectUri(); 
            DataAcquisitionSchemaObject tmpSDDo = DataAcquisitionSchemaObject.find(dasoUri);
            if (dasa.getLabel().equals(mainLabel)) {
                mainSDDoUri = dasoUri;
                mainSDDo = tmpSDDo;
                if (mainSDDo == null) {
                    logger.printException("SDDObjectInstanceGenerator: [ERROR] FAILED TO LOAD MAIN SDDO");
                    return false;
                }
            } 
            if (dasoUri != null && !dasoUri.equals("") && !dasos.containsKey(dasoUri)) {
                if (tmpSDDo != null) {
                    dasos.put(dasoUri, tmpSDDo);
                }
            }
        }

        mainSoc = socFromSDDo(mainSDDo, socsList);
        if (mainSoc == null) {
            logger.printException("SDDObjectInstanceGenerator: FAILED TO LOAD MAIN SOC. The virtual column for the file identifier (the row with attribute hasco:originalID) is not one of the virtual columns in the SSD for this study.");
            return false;
        }
        mainSocUri = mainSoc.getUri();
        logger.println("SDDObjectInstanceGenerator: (2/10) ============= MAIN SDDO ================");
        logger.println("SDDObjectInstanceGenerator: Main SDDO: " + mainSDDoUri);
        logger.println("SDDObjectInstanceGenerator: Main SOC: " + mainSocUri);
        groundingPathUris.clear();
        
        return true;
    }

    private boolean identifyGroundingPathForMainSOC() {
        // 
        //  (3/10) IDENTIFY GROUNDING PATH FOR MAIN SOC
        //

        logger.println("SDDObjectInstanceGenerator: (3/10) =========== GROUNDING PATH FOR  MAIN SOC ============");
        if (mainSoc.getHasScopeUri() == null || mainSoc.getHasScopeUri().equals("")) {
            logger.println("SDDObjectInstanceGenerator: Main SOC is already grounded. No grouding path required");
            groundingSoc = mainSoc;
        } else {
            logger.println("SDDObjectInstanceGenerator: Main SOC is not grounded. Computing grouding path");
            ObjectCollection currentSoc = mainSoc;
            groundingPath.add(mainSoc);
            while (currentSoc.getHasScopeUri() != null && !currentSoc.getHasScopeUri().equals("") && !containsUri(currentSoc.getHasScopeUri(), groundingPath)) {

                ObjectCollection nextSoc = ObjectCollection.find(currentSoc.getHasScopeUri());
                if (nextSoc == null) {
                    logger.printException("SDDObjectInstanceGenerator: Could not find SOC with following URI : " + currentSoc.getHasScopeUri());
                    return false;
                } else {
                    if (!containsUri(nextSoc.getUri(), groundingPath)) {
                        groundingPath.add(nextSoc);
                    }
                    currentSoc = nextSoc;
                }
            }
            int i = 0;
            for (ObjectCollection soc : groundingPath) {
                logger.println("SDDObjectInstanceGenerator: SOC in grouding path: " + soc.getUri() + " at index " + i++);
                groundingPathUris.add(soc.getUri());
            }
            i = 0;
            for (String socUri : groundingPathUris) {
                logger.println("SDDObjectInstanceGenerator: SOCURI in grouding path: " + socUri + " at index " + i++);
            }
        }
        
        return true;
    }

    private boolean identifyTargetSDDoURIs() {
        // 
        //  (4/10) IDENTIFY URIs of TARGET SDDOs
        //

        logger.println("SDDObjectInstanceGenerator: (4/10) =============== TRAVERSE SDDOS ================");

        for (Map.Entry<String, DataAcquisitionSchemaObject> entry : dasos.entrySet()) {
            String key = entry.getKey();
            DataAcquisitionSchemaObject daso = entry.getValue();
            processTargetSDDo(daso);
        }
        
        return true;
    }

    private boolean identitySOCsForSDDOs() {
        // 
        //  (5/10) IDENTIFY SOCs ASSOCIATED WITH IDENTIFIED SDDOs
        //

        logger.println("SDDObjectInstanceGenerator: (5/10) ===== IDENTIFY SOCs ASSOCIATED WITH IDENTIFIED SDDOs ======");

        this.requiredSocs.clear();
        for (Map.Entry<String, DataAcquisitionSchemaObject> entry : dasos.entrySet()) {
            String key = entry.getKey();
            DataAcquisitionSchemaObject daso = entry.getValue();
            if (!findCreateAssociatedSOC(daso)) {
                logger.printWarning("SDDObjectInstanceGenerator: Cannot create SOC for the following daso: " + daso.getUri());
            }
        }
        
        return true;
    }

    private boolean retrieveAdditionalSOCs() {
        // 
        //  (6/10) RETRIEVING ADDITIONAL SOCs required for traversing existing SOCs
        //

        logger.println("SDDObjectInstanceGenerator: (6/10) ======== RETRIEVING ADDITINAL  SOCs ========");
        for (Map.Entry<String, ObjectCollection> entry : requiredSocs.entrySet()) {
            String key = entry.getKey();
            ObjectCollection soc = entry.getValue();
            ObjectCollection currentSoc = soc;
            
            while (currentSoc.getHasScopeUri() != null && !currentSoc.getHasScopeUri().equals("") && 
                    !requiredSocs.containsKey(currentSoc.getHasScopeUri()) && !containsUri(currentSoc.getHasScopeUri(),groundingPath)) {

                // lookup in socsList for next SOC, i.e., currentSoc.getHasScopeUri()
                ObjectCollection nextSoc = null;
                for (ObjectCollection tmpSoc : socsList) {
                    if (tmpSoc.getUri().equals(currentSoc.getHasScopeUri())) {
                        nextSoc = tmpSoc;
                        break;
                    }
                }
                if (nextSoc == null) {
                    logger.printException("SDDObjectInstanceGenerator: Could not find SOC with following URI : " + currentSoc.getHasScopeUri());
                    return false;
                } else {
                    if (!requiredSocs.containsKey(nextSoc.getUri())) {
                        requiredSocs.put(nextSoc.getUri(), nextSoc);
                        logger.println("SDDObjectInstanceGenerator: Loading SOC: " + nextSoc.getUri() + " to required SOCs");
                    }
                    currentSoc = nextSoc;
                }
            }   
        }
        
        return true;
    }

    private boolean printRequiredSOCs() {
        // 
        //  (7/10) LIST OF REQUIRED SOCs
        //

        logger.println("SDDObjectInstanceGenerator: (7/10) ======== REQUIRED SOCs ========");
        for (Map.Entry<String, ObjectCollection> entry : requiredSocs.entrySet()) {
            String key = entry.getKey();
            ObjectCollection soc = entry.getValue();
            logger.println("SDDObjectInstanceGenerator: SOC: " + soc.getUri() + "   Reference : " + soc.getSOCReference() + 
                    "    with hasScope: " + soc.getHasScopeUri());
        }
        
        return true;
    }

    private boolean computePathsForTargetSOCs() {
        // 
        //  (8/10) COMPUTE PATH for each TARGET SOC
        //

        logger.println("SDDObjectInstanceGenerator: (8/10) ======== BUILD SOC PATHS ========");
        for (Map.Entry<String, ObjectCollection> entry : requiredSocs.entrySet()) {
            String key = entry.getKey();
            ObjectCollection soc = entry.getValue();
            List<ObjectCollection> socs = new ArrayList<ObjectCollection>();
            logger.println("SDDObjectInstanceGenerator: START: " + soc.getUri());
            logger.println("SDDObjectInstanceGenerator: PATH ---->> ");
            if (groundingPathUris.contains(soc.getUri())) {
                //logger.println("SDDObjectInstanceGenerator:       " + soc.getUri() + " (in grounding path)");
            	String socAux = null;
            	boolean start = false;
    			logger.println("SDDObjectInstanceGenerator:       DBG: grounding path size = " + groundingPathUris.size());
            	for (int index = groundingPathUris.size() - 1; index >= 0; index--) {
            		socAux = groundingPathUris.get(index);
            		if (socAux.equals(soc.getUri())) {
            			start = true;
            		}
            		if (start) {
            			logger.println("SDDObjectInstanceGenerator:       " + socAux + " (in grounding path)");
            			if (groundingPath.get(index) == null || !groundingPath.get(index).getUri().equals(socAux)) {
                            logger.println("SDDObjectInstanceGenerator:       ERROR: Could not find SOC for " + socAux);
            			} else {
            				socs.add(groundingPath.get(index));
            			}
            		}
        			logger.println("SDDObjectInstanceGenerator:       DBG: " + socAux + " (" + index + ")  start (" + start + ")");
            	}
            } else if (soc.getHasScope() == null) {
                logger.println("SDDObjectInstanceGenerator:       " + soc.getUri());
                socs.add(soc);
            } else {
                String toUri = soc.getHasScope().getUri();
                ObjectCollection nextTarget = soc;
                logger.println("SDDObjectInstanceGenerator:       " + nextTarget.getUri());
                socs.add(nextTarget);
                while (nextTarget != null && !nextTarget.getUri().equals(mainSocUri)) {
                    String nextTargetUri = nextTarget.getHasScopeUri();
                    nextTarget =  requiredSocs.get(nextTargetUri);
                    if (nextTarget != null) {
                    	logger.println("SDDObjectInstanceGenerator:       " + nextTarget.getUri());
                    	socs.add(nextTarget);
                    }
                }
                if (nextTarget == null) {
                	reverseGroundingPath = new ArrayList<ObjectCollection>(); 
                	//reverseGroundingPath.add(mainSoc);
                	boolean validPath = false;
                	for (ObjectCollection socTmp : groundingPath) {
                		if (socTmp.getUri().equals(toUri)) {
                			validPath = true;
                			break;
                		}
                		reverseGroundingPath.add(socTmp);
                	}
                	if (!validPath) {
                    	logger.printException("SDDObjectInstanceGenerator: Could not complete path for " + toUri);
                    	return false;
                	}
                	ObjectCollection socTmp2 = null;
                	if (reverseGroundingPath.size() > 0) {
                		for (int index = reverseGroundingPath.size() - 1; index >= 0; index--) {
                			socTmp2 = reverseGroundingPath.get(index);
                        	logger.println("SDDObjectInstanceGenerator:       " + socTmp2.getUri());
                        	socs.add(socTmp2);
                		}
                	}
                }
            } 
            socPaths.put(key,socs);
        }
        for (Map.Entry<String, List<ObjectCollection>> entry : socPaths.entrySet()) {
            String key = entry.getKey();
            List<ObjectCollection> path = entry.getValue();
            List<ObjectCollection> socs = new ArrayList<ObjectCollection>();
            logger.println("SDDObjectInstanceGenerator: DEBUG START: " + key);
            logger.println("SDDObjectInstanceGenerator:    DEBUG PATH ---->> ");
            for (ObjectCollection socTmp : path) {
                logger.println("SDDObjectInstanceGenerator:    DEBUG ELEMENT:  " + socTmp.getUri());
            } 
        }
        
        return true;
    }

    private boolean computeLabelsForTargetSOCs() {
        // 
        //  (9/10) COMPUTE LABEL for each TARGET SOC
        //

        logger.println("SDDObjectInstanceGenerator: (9/10) ======== COMPUTE SOC LABELS ========");
        for (Map.Entry<String, ObjectCollection> entry : requiredSocs.entrySet()) {
            String key = entry.getKey();
            ObjectCollection soc = entry.getValue();
            String fullLabel = "";
            boolean process = true;

            logger.println("SDDObjectInstanceGenerator: START: " + soc.getUri());
            String label = soc.getGroundingLabel();
            if (label == null) {
                label = "";
            }
            if (soc.getHasScope() == null || !label.equals("")) {
                fullLabel = label;
                logger.println("SDDObjectInstanceGenerator: Computed label [" + fullLabel + "]");
                socLabels.put(soc.getSOCReference(), fullLabel);
                if (soc.getRoleLabel() == null || soc.getRoleLabel().equals("")) {
                    soc.saveRoleLabel(fullLabel);
                }
                process = false;
            } else {
                fullLabel = getPrettyLabel(soc.getLabel());
            }

            if (process) {
                String toUri = soc.getHasScope().getUri();

                ObjectCollection nextTarget = soc;
                label = nextTarget.getGroundingLabel();
                if (label == null) {
                    label = "";
                }
                if (nextTarget.getUri().equals(mainSocUri)) {
                    String nextTargetUri = nextTarget.getHasScopeUri();
                    nextTarget = null;
                    for (ObjectCollection gsoc : groundingPath) {
                    	if (gsoc.getUri().equals(nextTargetUri)) {
                    		nextTarget = gsoc;
                    	}
                    }
                    if (nextTarget == null) {
                        logger.printException("SDDObjectInstanceGenerator: Could not complete path for " + toUri);
                        return false;
                    }
                    label = nextTarget.getGroundingLabel();
                    if (label == null) {
                        label = "";
                    }
                    if (label.equals("")) {
                        fullLabel = getPrettyLabel(nextTarget.getLabel()) + " " + fullLabel;
                    } else {
                        fullLabel = label + " " + fullLabel;
                   }                	
                } else {
	                while (!nextTarget.getUri().equals(mainSocUri) && label.equals("")) {
	                    String nextTargetUri = nextTarget.getHasScopeUri();
	                    nextTarget =  requiredSocs.get(nextTargetUri);
	                    if (nextTarget == null) {
	                        logger.printException("SDDObjectInstanceGenerator: Could not complete path for " + toUri);
	                        return false;
	                    }
	                    label = nextTarget.getGroundingLabel();
	                    if (label == null) {
	                        label = "";
	                    }
	                    if (label.equals("")) {
	                        fullLabel = getPrettyLabel(nextTarget.getLabel()) + " " + fullLabel;
	                    } else {
	                        fullLabel = label + " " + fullLabel;
	                   }
	                }
                }
                logger.println("SDDObjectInstanceGenerator: Computed label [" + fullLabel + "]");       
                if (soc.getRoleLabel() == null || soc.getRoleLabel().equals("")) {
                    soc.saveRoleLabel(fullLabel);
                }
                socLabels.put(soc.getSOCReference(), fullLabel);
            }
        }
        
        return true;
    }

    private boolean mapSOCsAndMatchings() {
        // 
        //  (10/10) Map SOCs and Matchings 
        //

        logger.println("SDDObjectInstanceGenerator: (10/10) ======== MAP SOCs and MATCHINGS ========");
        for (Map.Entry<String, ObjectCollection> entry : requiredSocs.entrySet()) {
            String key = entry.getKey();
            ObjectCollection soc = entry.getValue();
            if (!socMatchingSOCs.containsKey(soc.getUri())) {
            	List<ObjectCollection> matchingSOCs = ObjectCollection.findMatchingScopeCollections(soc.getUri());
            	if (matchingSOCs.size() > 1) {
            		logger.printWarning("SDDObjectInstanceGenerator: SOC: " + soc.getUri() + "   has more than one matching SOC");
            	}
            	if (matchingSOCs.size() >- 0) {
            		socMatchingSOCs.put(soc.getUri(), matchingSOCs.get(0));
            		logger.println("SDDObjectInstanceGenerator: SOC: " + soc.getUri() + "   Has matching SOC: " + matchingSOCs.get(0).getUri());
            	}
            }
        }
        
        return true;
    }

    // **************************************************************************************
    //                                                                                      *
    //                            SUPPORTING METHODS                                        *
    //                                                                                      *
    // **************************************************************************************

    private boolean containsUri(String uri, List<ObjectCollection> list) {
        if (uri == null || uri.equals("") || list == null || list.size() == 0) {
            return false;
        }
        for (ObjectCollection soc : list) {
            if (soc.getUri() != null && !soc.getUri().equals("")) {
                if (soc.getUri().equals(uri)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean processTargetSDDo(DataAcquisitionSchemaObject daso) { 
        String toUri = targetUri(daso);
        logger.println("SDDObjectInstanceGenerator: SDDO: " + daso.getUri() + "   From : " + daso.getLabel() + "  To: " + toUri);

        //  LOAD each TARGET SDDO into SDDOs, if TARGET SDDO is not loaded yet
        if (toUri != null && !toUri.equals("") && !dasos.containsKey(toUri)) {
            logger.println("SDDObjectInstanceGenerator: Loading " + toUri);
            DataAcquisitionSchemaObject newSDDo = DataAcquisitionSchemaObject.find(toUri);
            if (newSDDo == null) {
                logger.println("SDDObjectInstanceGenerator: [ERROR] Could not find SDDO with following URI : " + toUri);
                return false;
            }
            dasos.put(toUri, newSDDo);
            return processTargetSDDo(newSDDo);
        }
        return true;
    }

    private String targetUri(DataAcquisitionSchemaObject daso) {
        if (!daso.getWasDerivedFrom().equals("")) {
            String toLabel = daso.getWasDerivedFrom();
            DataAcquisitionSchemaObject tmpSDDo = DataAcquisitionSchemaObject.findByLabelInSchema(sdd.getUri(), toLabel);
            if (tmpSDDo == null) {
                return "";
            } else {

                return tmpSDDo.getUri();
            }
        } else if (!daso.getInRelationTo().equals("")) {
            return daso.getInRelationTo();
        }
        return "";
    }

    private boolean isSample(DataAcquisitionSchemaObject daso) {
        if (!daso.getWasDerivedFrom().equals("")) {
            String toLabel = daso.getWasDerivedFrom();
            DataAcquisitionSchemaObject tmpSDDo = DataAcquisitionSchemaObject.findByLabelInSchema(sdd.getUri(), toLabel);
            if (tmpSDDo == null) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    private ObjectCollection socFromTargetSDDo(DataAcquisitionSchemaObject daso, List<ObjectCollection> list) {
        String targetObjUri = targetUri(daso);
        if (targetObjUri.equals("")) {
            return null;
        }
        DataAcquisitionSchemaObject targetObj = DataAcquisitionSchemaObject.find(targetObjUri);
        if (targetObj == null || targetObj.getLabel() == null || targetObj.getLabel().equals("")) {
            return null;
        }
        for (ObjectCollection soc : list) {
        	//logger.println("socFromTargetSDDo : " + targetObj.getLabel() + "    soc's getSOCReference " + soc.getSOCReference()); 
            if (soc.getSOCReference().equals(targetObj.getLabel())) {
                return soc;
            }
        }
        return null;
    }

    private ObjectCollection socFromSDDo(DataAcquisitionSchemaObject daso, List<ObjectCollection> list) {
        if (daso == null || daso.getLabel() == null || daso.getLabel().equals("")) {
            return null;
        }
        for (ObjectCollection soc : list) {
            if (soc.getSOCReference().equals(daso.getLabel())) {
                return soc;
            }
        }
        return null;
    }

    private DataAcquisitionSchemaObject dasoFromSoc(ObjectCollection soc, Map<String, DataAcquisitionSchemaObject> mapSDDos) {
        if (soc == null || soc.getSOCReference() == null || soc.getSOCReference().equals("")) {
            return null;
        }
        for (Map.Entry<String, DataAcquisitionSchemaObject> entry : mapSDDos.entrySet()) {
            DataAcquisitionSchemaObject daso = entry.getValue();
            if (soc.getSOCReference().equals(daso.getLabel())) {
                return daso;
            }
        }
        return null;
    }

    private boolean findCreateAssociatedSOC(DataAcquisitionSchemaObject daso) {
        ObjectCollection associatedSOC = null;

        //  Try to find existing SOC
        for (ObjectCollection soc : socsList) {
            if (soc.getSOCReference().equals(daso.getLabel())) {
                associatedSOC = ObjectCollection.find(soc.getUri());
                if (associatedSOC != null) {
                    logger.println("SDDObjectInstanceGenerator: Reference: " + daso.getLabel() + "  Associated SOC : " + associatedSOC + "    with hasScope: " + associatedSOC.getHasScopeUri());
                    if (!requiredSocs.containsKey(associatedSOC.getUri())) {
                        requiredSocs.put(associatedSOC.getUri(), associatedSOC);
                    }
                    break;
                }
            }
        }
         namedGraphUri = (!(studyUri==null) && studyUri.contains("STD")) ? studyUri.replace("STD","SSD") : "";

        //  Create a SOC when existing SOCs can be associated
        if (associatedSOC == null) {
            String newSOCUri = studyUri.contains("STD") ? studyUri.replace("STD","SOC") + "-" + daso.getLabel().replace("??","") : studyUri.replace("SSD","SOC") + "-" + daso.getLabel().replace("??","");;
            String scopeUri = "";
            if (daso != null) {
                ObjectCollection scopeObj = socFromTargetSDDo(daso, socsList);
                if (scopeObj != null && scopeObj.getUri() != null) {
                    scopeUri = scopeObj.getUri();
                } else {
                	String tmpUri = targetUri(daso);
                	if (tmpUri == null || tmpUri.isEmpty()) {
                		logger.println("SDDObjectInstanceGenerator:       [WARNING] SOC association ignored for " + daso.getUri());
                		return false;
                	}
                    DataAcquisitionSchemaObject newSDDo = DataAcquisitionSchemaObject.find(tmpUri);
                    if (newSDDo == null) {
                        logger.println("SDDObjectInstanceGenerator: [ERROR] Could not find SDDO with following URI : " + tmpUri);
                        return false;
                    }
                    scopeUri = studyUri.contains("STD") ? studyUri.replace("STD","SOC") + "-" + newSDDo.getLabel().replace("??","") : studyUri.replace("SSD","SOC") + "-" + newSDDo.getLabel().replace("??","");;
                	//scopeUri = tmpUri.replace("SDDO", "SOC");
                }
            }
            String newLabel = daso.getLabel().replace("??","");
            String collectionType = null;
            if (daso.getEntity().equals(URIUtils.replacePrefixEx("hasco:StudyObjectMatching"))) {
            	collectionType = ObjectCollection.MATCHING_COLLECTION;
            } else if (isSample(daso)) {
                collectionType = ObjectCollection.SAMPLE_COLLECTION;
            } else {
                // collectionType = ObjectCollection.SUBJECT_COLLECTION;
                collectionType = ObjectCollection.OBJECT_COLLECTION;
            }

            VirtualColumn newVc = VirtualColumn.find(studyUri, daso.getLabel());
            if (newVc == null) {
                newVc = new VirtualColumn(studyUri, "", daso.getLabel());
                newVc.setNamedGraph(namedGraphUri);
                newVc.saveToTripleStore();
                // addObject(newVc);
            }
            ObjectCollection newSoc = new ObjectCollection(newSOCUri, collectionType, newLabel, newLabel, studyUri, 
            		newVc.getUri(), "", scopeUri, null, null, null, "0");
            newSoc.setNamedGraph(namedGraphUri);
            newSoc.saveToTripleStore();
            // addObject(newSoc);

            if (!requiredSocs.containsKey(newSoc.getUri())) {
                requiredSocs.put(newSoc.getUri(), newSoc);
                socsList.add(newSoc);
            }
            logger.println("SDDObjectInstanceGenerator: Reference: " + daso.getLabel() + "   Created SOC : " + newSOCUri + "    with hasScope: " + scopeUri);
        }
        if (associatedSOC!=null) {associatedSOC.setNamedGraph(namedGraphUri);}
        return true;
    }

    private String getPrettyLabel(String label) {
        String prettyLabel = label;
        if (!prettyLabel.equals("")) {
            String c0 = prettyLabel.substring(0,1).toUpperCase();
            if (prettyLabel.length() == 1) {
                prettyLabel = c0;
            } else {
                prettyLabel = c0 + prettyLabel.substring(1);
            }
        }
        return prettyLabel;
    }

    // **************************************************************************************
    //                                                                                      *
    //                GENERATE INSTANCES FOR A GIVEN ROW's IDENTIFIER                       *
    //                                                                                      *
    // **************************************************************************************

    public Map<String, Map<String, String>> generateRowInstances(String id) {
        // Returns : First String : SDDO's Label
        //           Object URI   : The actual URI of the object that was retrieved/created for the identifier in CSV Record
        //

        if (id == null || id.equals("")) {
            System.out.println("SDDObjectInstanceGenerator::generateRowInstances: [ERROR] no identifier provided. See if your SDD contains an identifier," +
                    " and if the corresponding label in ths file is a valid identifier.");
            return null;
        }
        
        if (DEBUG_MODE) { 
        	System.out.println("SDDObjectInstanceGenerator: generate row instances for : " + id);
        }

        Map<String,Map<String,String>> objMapList = new HashMap<String,Map<String,String>>();

        //
        //   TRAVERSE list of objects for current record
        //

        for (Map.Entry<String, List<ObjectCollection>> entry : socPaths.entrySet()) {
            String key = entry.getKey();
            List<ObjectCollection> path = entry.getValue();

            //
            //   TRAVERSE SOC's PATH
            //

            ListIterator<ObjectCollection> iter = path.listIterator(path.size());

            if (DEBUG_MODE) { 
            	System.out.println("SDDObjectInstanceGenerator:     PATH >>> ");
            }

        	// Lookup first study object
        	ObjectCollection currentSoc = iter.previous();
        	String currentObjUri = getCachedObjectBySocAndOriginalId(currentSoc.getUri(), id); 
        	if (DEBUG_MODE) { 
        		System.out.println("SDDObjectInstanceGenerator:          Obj Original ID=[" + id + "]   SOC=[" + currentSoc.getUri() + "] =>  Obj URI=[" + currentObjUri + "]");
        	}

        	// 
            //   Test if there is Grounding Path. If so, replace ID of a main SOC's object by the ID of 
            //   the corresponding grounding scope object of the main SOC's object
            //
        	boolean hasGrounding = groundingPath.size() > 0;
        	
            ObjectCollection previousSoc = null;
            String previousObjUri = null;
            while (currentObjUri != null && !currentObjUri.equals("") && iter.hasPrevious()) {
                ObjectCollection nextSoc = iter.previous();
                if (DEBUG_MODE) { 
                	System.out.println("           Next SOC : [" + nextSoc.getUri() + "]    Current Obj URI : [" + currentObjUri + "]");
                }

                //
                //   RETRIEVE/CREATE next object in the path
                //

                String nextObjUri = null;
                if (hasGrounding) {
                	nextObjUri = getCachedScopeBySocAndObjectUri(nextSoc.getUri(), currentObjUri); 
                } else {
                	nextObjUri = getCachedSocAndScopeUri(nextSoc.getUri(), currentObjUri); 
                }
                
                if (nextObjUri == null || nextObjUri.equals("")) {
                    nextObjUri = createStudyObject(nextSoc, currentObjUri);
                }

                if (nextObjUri == null || nextObjUri.equals("")) {
                    if (DEBUG_MODE) { 
                    	System.out.println("SDDObjectInstanceGenerator:          [ERROR] Path generation stopped. Error ocurred retrieving/creating objects in path. See log above.");
                    }
                    currentSoc = nextSoc;
                    currentObjUri = nextObjUri;
                    break;
                }

                if (DEBUG_MODE) { 
                	System.out.println("SDDObjectInstanceGenerator:          Scope Obj URI=[" + currentObjUri + "]  SOC=[" + nextSoc.getUri() + 
                			"]  =>  Obj Uri=[" + nextObjUri + "]");
                }

                previousSoc = currentSoc;
                previousObjUri = currentObjUri;
                currentSoc = nextSoc;
                currentObjUri = nextObjUri;
            }

            if (currentObjUri == null || currentObjUri.equals("")) {
            	System.out.println("SDDObjectInstanceGenerator:     Response >>> failed to load object");
            } else {
            	StudyObject obj = getCachedObject(currentObjUri);
            	if (obj != null) { 
            		// TO-DO we will need to have a mechanism to decide whether to use instances or classes to represent abstract time
            		List<String> objTypeTimes = StudyObject.retrieveTimeScopeTypeUris(currentObjUri);
            		Map<String,String> referenceEntry = new HashMap<String,String>();
            		referenceEntry.put(StudyObject.STUDY_OBJECT_URI, currentObjUri);
            		referenceEntry.put(StudyObject.STUDY_OBJECT_TYPE, obj.getTypeUri());
            		referenceEntry.put(StudyObject.SOC_TYPE, currentSoc.getTypeUri());
            		referenceEntry.put(StudyObject.SOC_LABEL, socLabels.get(currentSoc.getSOCReference()));
            		referenceEntry.put(StudyObject.SUBJECT_ID, id);
            		if (previousObjUri != null && !previousObjUri.isEmpty()) {
            			referenceEntry.put(StudyObject.SCOPE_OBJECT_URI, previousObjUri);
            		}
            		if (previousSoc != null && previousSoc.getUri() != null && !previousSoc.getUri().isEmpty()) {
            			referenceEntry.put(StudyObject.SCOPE_OBJECT_SOC_URI, previousSoc.getUri());
            		}
            		referenceEntry.put(StudyObject.OBJECT_ORIGINAL_ID, obj.getOriginalId());
            		referenceEntry.put(StudyObject.SOC_URI, currentSoc.getUri());
            		if (objTypeTimes != null && objTypeTimes.size() > 0) {
            			referenceEntry.put(StudyObject.OBJECT_TIME, objTypeTimes.get(0));
            		}

            		objMapList.put(currentSoc.getSOCReference(),referenceEntry);

            	}	
            }

        }

        if (DEBUG_MODE) { 
        	System.out.println("SDDObjectInstanceGenerator:     Response >>> ");
        	for (Map.Entry<String, Map<String,String>> entry : objMapList.entrySet()) {
        		String label = entry.getKey();
        		Map<String,String> objMapEntry = entry.getValue();
        		System.out.println("SDDObjectInstanceGenerator:          Label=[" + label + "]    Obj Uri=[" + objMapEntry.get(StudyObject.STUDY_OBJECT_URI) + "]");
        	}
        }

        return objMapList;
    }// /generateRowInstances

    //
    //   CREATE next object in the path if it does not exist
    //

    private String createStudyObject(ObjectCollection nextSoc, String currentObjUri) {
        String newOriginalId = String.valueOf(nextSoc.getNextCounter());
        newOriginalId = addLeftZeros(newOriginalId);
        String newUri = createObjectUri(newOriginalId, nextSoc.getUri(), nextSoc.getTypeUri());
        String newLabel = createObjectLabel(newOriginalId, nextSoc);
        String newTypeUri = "";
        DataAcquisitionSchemaObject daso = dasoFromSoc(nextSoc, dasos);
        if (daso == null || daso.getEntity() == null || daso.getEntity().equals("")) {
            if (nextSoc.getTypeUri().equals(ObjectCollection.MATCHING_COLLECTION)) {
                newTypeUri = URIUtils.replacePrefixEx(StudyObjectMatching.className);
            } else if (nextSoc.getTypeUri().equals(ObjectCollection.SUBJECT_COLLECTION)) {
                newTypeUri = URIUtils.replacePrefixEx(SIO_OBJECT);
            } else {
                newTypeUri = URIUtils.replacePrefixEx(SIO_SAMPLE);
            }
        } else {
            newTypeUri = daso.getEntity();
        }
        List<String> newScopeUris = new ArrayList<String>();
        List<String> newTimeScopeUris = new ArrayList<String>();
        List<String> newSpaceScopeUris = new ArrayList<String>();
        newScopeUris.add(currentObjUri);

        StudyObject newObj = new StudyObject(newUri, newTypeUri, newOriginalId, newLabel, nextSoc.getUri(), "Automatically generated",
                newScopeUris, newTimeScopeUris, newSpaceScopeUris);
        newObj.setNamedGraph(str.getUri());
        newObj.setDeletable(false);
        addObjectToCache(newObj, currentObjUri);

        if (DEBUG_MODE) { 
        	System.out.println("SDDObjectInstanceGenerator:          Created Obj with URI=[" + newUri + "]   Type=[" + newTypeUri + "]");
        }

        return newObj.getUri();
    }

    private String addLeftZeros(String str) {
    	str = str.trim();
    	String zeros = "";
    	if (str.length() < ID_LENGTH) {
    		while (zeros.length() <= ID_LENGTH - str.length()) {
    			zeros = "0" + zeros;
    		}
    	}
    	return zeros + str;
    }
    
    private String createObjectUri(String originalID, String socUri, String socTypeUri) {
        String labelPrefix = "";
        if (socTypeUri.equals(ObjectCollection.SUBJECT_COLLECTION)) {
            labelPrefix = "SBJ-";
        } else {
            labelPrefix = "SPL-";
        }
        String uri = kbPrefix + labelPrefix + originalID + "-" + socIdFromUri(socUri);
        uri = URIUtils.replacePrefixEx(uri);

        return uri;
    }

    private String createObjectLabel(String originalID, ObjectCollection soc) {
        if (soc.getRoleLabel() != null && !soc.getRoleLabel().equals("")) {
        	return soc.getRoleLabel() + " " + originalID;
        } 
        String labelPrefix = "";
        if (soc.getTypeUri().equals(ObjectCollection.SUBJECT_COLLECTION)) {
            labelPrefix = "SBJ ";
        } else {
            labelPrefix = "SPL ";
        }
        if ( labelPrefix.contains("SBJ") && "ON".equalsIgnoreCase(ConfigFactory.load().getString("hadatac.graph.uniqueIdentifiers")) ) {
            return labelPrefix + originalID;
        }
        return labelPrefix + originalID + " - " + socIdFromUri(soc.getUri());

    }

    private String socIdFromUri(String socUri) {
        String SOC_PREFIX = "SOC-";
        if (socUri == null || socUri.equals("")) {
            return "";
        }
        int index = socUri.indexOf(SOC_PREFIX) + SOC_PREFIX.length();
        if (index == -1) {
            return "";
        }
        String resp = socUri.substring(index);
        if (resp == null) {
            return "";
        }
        return resp;
    }
    

    //
    //   METHODS RELATED TO INTERNAL CACHE
    //

    public boolean initiateCache(String study_uri) {
    	if (study_uri == null || study_uri.equals("")) {
    		return false;
    	}
    	Study study = Study.find(study_uri);
    	//if (mainSoc != null) {
    		System.out.println("INITIATE CACHE BEING CALLED!");
    		addCache(new Cache<String, StudyObject>("cacheObject", true, study.getObjectsMapInBatch()));
    		addCache(new Cache<String, String>("cacheObjectBySocAndScopeUri", false, StudyObject.buildCachedObjectBySocAndScopeUri()));
    		addCache(new Cache<String, String>("cacheObjectBySocAndOriginalId", false, StudyObject.buildCachedObjectBySocAndOriginalId()));
    		addCache(new Cache<String, String>("cacheScopeBySocAndObjectUri", false, StudyObject.buildCachedScopeBySocAndObjectUri()));
    	    
    		return true;
    	//}
    	//return false;
    }
    
    @SuppressWarnings("unchecked")
    private void addObjectToCache(StudyObject newObj, String scopeObjUri) {
    	if (newObj == null || caches.get("cacheObject").containsKey(newObj.getUri())) {
    		return;
    	}
    	
    	if (!caches.get("cacheObject").containsKey(newObj.getUri())) {
    	    caches.get("cacheObject").put(newObj.getUri(), newObj);
    	}
    	
    	String keySocAndOriginalId = newObj.getIsMemberOf() + ":" + newObj.getOriginalId();
    	if (!caches.get("cacheObjectBySocAndOriginalId").containsKey(keySocAndOriginalId)) {
    	    caches.get("cacheObjectBySocAndOriginalId").put(keySocAndOriginalId, newObj.getUri());
    	}
    	
    	String keySocAndScopeUri =  newObj.getIsMemberOf() + ":" + scopeObjUri;
    	if (!caches.get("cacheObjectBySocAndScopeUri").containsKey(keySocAndScopeUri)) {
    		caches.get("cacheObjectBySocAndScopeUri").put(keySocAndScopeUri, newObj.getUri());
    	}
    }

    @SuppressWarnings("unchecked")
    private StudyObject getCachedObject(String key) {
    	if (caches.get("cacheObject").containsKey(key)) {
    		return (StudyObject)caches.get("cacheObject").get(key); 
    	} else {
    		return null;
    	}
    }
    
    @SuppressWarnings("unchecked")
    private String getCachedObjectBySocAndOriginalId(String soc_uri, String id) {
    	String key = soc_uri + ":" + id;
    	if (caches.get("cacheObjectBySocAndOriginalId").containsKey(key)) {
    		return (String)caches.get("cacheObjectBySocAndOriginalId").get(key); 
    	} else {
    		return null;
    	}
    }
    
    @SuppressWarnings("unchecked")
    private String getCachedSocAndScopeUri(String soc_uri, String scope_uri) {
    	String key = soc_uri + ":" + scope_uri;
    	if (caches.get("cacheObjectBySocAndScopeUri").containsKey(key)) {
    		return (String)caches.get("cacheObjectBySocAndScopeUri").get(key); 
    	} else {
    		return null;
    	}
    }
    
    @SuppressWarnings("unchecked")
    private String getCachedScopeBySocAndObjectUri(String soc_uri, String obj_uri) {
    	String key = soc_uri + ":" + obj_uri;
    	if (caches.get("cacheScopeBySocAndObjectUri").containsKey(key)) {
    		return (String)caches.get("cacheScopeBySocAndObjectUri").get(key); 
    	} else {
    		return null;
    	}
    }
 
    public Map<String,ObjectCollection> getMatchingSOCs() {
    	return socMatchingSOCs;
    }
    
    // **************************************************************************************
    //                                                                                      *
    //  RETRIEVE URI, ORIGINAL ID,  AND TYPE OF GROUNDING OBJECT FROM CURRENT OBJECT URI    *
    //                                                                                      *
    // **************************************************************************************

    public Map<String, String> retrieveGroundObject(String id) {
        // Returns : First String : SDDO's Label
        //           Object URI   : The actual URI of the object that was retrieved/created for the identifier in CSV Record
        //

        if (id == null || id.equals("")) {
            System.out.println("SDDObjectInstanceGenerator::retrieveGroundObject: [ERROR] no identifier provided. See if your SDD contains an identifier," +
                    " and if the corresponding label in ths file is a valid identifier.");
            return null;
        }
        if (DEBUG_MODE) { 
        	System.out.println("SDDObjectInstanceGenerator: retrieve ground object for : " + id);
        	System.out.println("SDDObjectInstanceGenerator: groundingPath : " + groundingPath);
        }

        ObjectCollection currentSoc = mainSoc;
        StudyObject obj = null;
        Map<String,String> groundObj = new HashMap<String,String>();

        if (DEBUG_MODE) { 
        	System.out.println("SDDObjectInstanceGenerator:     PATH >>> ");
        }

        // Lookup first study object
        if (DEBUG_MODE) { 
        	System.out.println("SDDObjectInstanceGenerator: CachedObjectBySocAndOriginalId: soc: [" +currentSoc.getUri() + "]   Id: [" + id + "]");
        }
        String currentObjUri = getCachedObjectBySocAndOriginalId(currentSoc.getUri(), id); 
        if (DEBUG_MODE) { 
        	System.out.println("SDDObjectInstanceGenerator: currentObjUri: [" +currentObjUri + "]");
        }

        if (groundingPath == null || groundingPath.size() <= 0) {
        	obj = getCachedObject(currentObjUri);
        	if (obj == null || obj.getUri() == null || obj.getUri().equals("")) {
                System.out.println("SDDObjectInstanceGenerator: [ERROR] Could not retrieve first Study Object for URI=[" + currentObjUri + "]");
                return null;
            }
            groundObj.put(StudyObject.STUDY_OBJECT_URI, obj.getUri());
            groundObj.put(StudyObject.STUDY_OBJECT_TYPE, obj.getTypeUri());
            groundObj.put(StudyObject.SUBJECT_ID, obj.getOriginalId());
            return groundObj;
        } 

        if (DEBUG_MODE) { 
        	System.out.println("SDDObjectInstanceGenerator:          Obj Original ID=[" + id + "]   SOC=[" + currentSoc.getUri() + "] =>  Obj URI=[" + currentObjUri + "]");
        }
        
        for (int index=1; index < groundingPath.size(); index++) {
        	ObjectCollection nextSoc = groundingPath.get(index);
            if (DEBUG_MODE) { 
            	System.out.println("SDDObjectInstanceGenerator:      nextSOC=[" + nextSoc.getUri() + "] Obj URI=[" + currentObjUri + "]");
            }
            String nextObjUri = getCachedScopeBySocAndObjectUri(nextSoc.getUri(), currentObjUri); 
            if (nextObjUri == null || nextObjUri.equals("")) {
                System.out.println("SDDObjectInstanceGenerator:          [ERROR] Path generation stopped. Error ocurred retrieving/creating objects in path. See log above.");
                currentSoc = nextSoc;
                currentObjUri = nextObjUri;
                break;
            }

            if (DEBUG_MODE) { 
            	System.out.println("SDDObjectInstanceGenerator:          Scope Obj URI=[" + currentObjUri + "]  nextSOC=[" + nextSoc.getUri() + 
            			"]  =>  Obj Uri=[" + nextObjUri + "]");
            }

            currentSoc = nextSoc;
            currentObjUri = nextObjUri;
        }

    	obj = getCachedObject(currentObjUri);
        if (obj == null) {
            //System.out.println("SDDObjectInstanceGenerator: [ERROR] Could not retrieve Study Object for ID=[" + id + "]");
            return null;
        }
        groundObj.put(StudyObject.STUDY_OBJECT_URI, obj.getUri());
        groundObj.put(StudyObject.STUDY_OBJECT_TYPE, obj.getTypeUri());
        groundObj.put(StudyObject.SUBJECT_ID, obj.getOriginalId());
        return groundObj;

    }// /retrieveGroundObject
*/

}// /class

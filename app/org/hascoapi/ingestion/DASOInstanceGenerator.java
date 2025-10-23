package org.hascoapi.ingestion;

import com.typesafe.config.ConfigFactory;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.SemanticDataDictionary;
import org.hascoapi.entity.pojo.Stream;
import org.hascoapi.entity.pojo.VirtualColumn;
import org.hascoapi.entity.pojo.StudyObject;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.utils.ConfigProp;

import org.hascoapi.entity.pojo.SDDAttribute;
import org.hascoapi.entity.pojo.SDDObject;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.entity.pojo.StudyObjectMatching;
import org.hascoapi.vocabularies.HASCO;

import java.lang.String;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;


public class DASOInstanceGenerator extends BaseGenerator {

	private final boolean DEBUG_MODE = false;
	private final int ID_LENGTH = 5;
	
	private final String SIO_OBJECT = "sio:SIO_000776";
    private final String SIO_SAMPLE = "sio:SIO_001050";;
    private final String kbPrefix = ConfigProp.getKbPrefix();
    private Stream str;
    private SemanticDataDictionary sdd;
    private String mainLabel;
    private SDDObject mainDaso;
    private String mainDasoUri;
    private StudyObjectCollection mainSoc;
    private StudyObjectCollection groundingSoc;
    private String mainSocUri;
    private String fileName; 
    private Map<String, SDDObject> dasos = new ConcurrentHashMap<String, SDDObject>();  
    private Map<String, StudyObjectCollection> requiredSocs = new ConcurrentHashMap<String, StudyObjectCollection>();  
    private List<StudyObjectCollection> socsList = null;  
    private List<StudyObjectCollection> groundingPath = new ArrayList<StudyObjectCollection>();  
    private List<StudyObjectCollection> reverseGroundingPath;
    private List<String> groundingPathUris = new ArrayList<String>();
    private Map<String, List<StudyObjectCollection>> socPaths = new HashMap<String, List<StudyObjectCollection>>(); 
    private Map<String, String> socLabels = new ConcurrentHashMap<String, String>();
    private Map<String, StudyObjectCollection> socMatchingSOCs = new ConcurrentHashMap<String, StudyObjectCollection>();
//    private Map<String, String > groundingIds = new ArrayList<String>();
    
    public DASOInstanceGenerator(DataFile dataFile, Stream str, String fileName) {
        super(dataFile, str.getStudyUri());

        this.str = str;
        this.sdd = str.getSemanticDataDictionary();
        logger.println("Initiating cache for study " + str.getStudyUri());        
        if (!initiateCache(str.getStudyUri())) {
            logger.printExceptionById("DA_00001");
        	return;
        }
        socPaths.clear();
        socLabels.clear();

        /* ***************************************************************************************
         *                                                                                       *
         *                MAPPING OF IDENTIFIER AND ASSOCIATED OBJECTS                           *
         *                                                                                       *
         ****************************************************************************************/

        mainLabel = "";
        String origId = sdd.getOriginalIdLabel(); 
        String id = sdd.getIdLabel(); 
        if (origId != null && !origId.equals("")) {
            mainLabel = origId;
        } else if (id != null && !id.equals("")) {
            mainLabel = id;
        }

        if (fileName == null || fileName.equals("")) {
            logger.printException("DASOInstanceGenerator: [ERROR] NO RECORD FILE PROVIDED");
            return;
        } 
        this.fileName = fileName;

        if (mainLabel.equals("")) {
            logger.printException("DASOInstanceGenerator: NO IDENTIFIER");
            return;
        } else {
            logger.println("DASOInstanceGenerator: Study URI: " + studyUri);
            logger.println("DASOInstanceGenerator: Label of main DASO: " + mainLabel);
        }

        ////////////////////////////////////////////
        // Strictly follow the given order of steps
        // if (!retrieveAvailableSOCs()) {
        //     return;
        // }
        if (!identifyMainDASO()) {
            return;
        }
        if (!identifyGroundingPathForMainSOC()) {
            return;
        }
        if (!identifyTargetDasoURIs()) {
            return;
        }
        if (!identitySOCsForDASOs()) {
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

    // private boolean retrieveAvailableSOCs() {
    //     /* 
    //      *  (1/10) INITIALLY AVAILABLE SOCs
    //      */

    //     logger.println("DASOInstanceGenerator: (1/10) ======== INITIALLY AVAILABLE SOCs ========");
    //     socsList = StudyObjectCollection.findByStudyUriJSON(studyUri);
    //     if (socsList == null) {
    //         logger.println("DASOInstanceGenerator: no SOC is available");
    //         socsList = new ArrayList<StudyObjectCollection>();  
    //     } else {
    //         for (StudyObjectCollection soc : socsList) {
    //             logger.println("DASOInstanceGenerator: SOC: " + soc.getUri() + "   Reference : " + soc.getSOCReference());
    //         }
    //     }
        
    //     return true;
    // }

    private boolean identifyMainDASO() {
        /* 
         *  (2/10) IDENTIFY MAIN DASO and DASOS REQUIRED FROM DASAs. THESE DASOS ARE LISTED IN STEP (4)
         */

        mainDasoUri = "";
        Iterator<SDDAttribute> iterAttributes = sdd.getAttributes().iterator();
        while (iterAttributes.hasNext()) {
            SDDAttribute dasa = iterAttributes.next();
            String dasoUri = dasa.getObjectUri(); 
            SDDObject tmpDaso = SDDObject.find(dasoUri);
            if (dasa.getLabel().equals(mainLabel)) {
                mainDasoUri = dasoUri;
                mainDaso = tmpDaso;
                if (mainDaso == null) {
                    logger.printException("DASOInstanceGenerator: [ERROR] FAILED TO LOAD MAIN DASO");
                    return false;
                }
            } 
            if (dasoUri != null && !dasoUri.equals("") && !dasos.containsKey(dasoUri)) {
                if (tmpDaso != null) {
                    dasos.put(dasoUri, tmpDaso);
                }
            }
        }

        mainSoc = socFromDaso(mainDaso, socsList);
        if (mainSoc == null) {
            logger.printException("DASOInstanceGenerator: FAILED TO LOAD MAIN SOC. The virtual column for the file identifier (the row with attribute hasco:originalID) is not one of the virtual columns in the SSD for this study.");
            return false;
        }
        mainSocUri = mainSoc.getUri();
        logger.println("DASOInstanceGenerator: (2/10) ============= MAIN DASO ================");
        logger.println("DASOInstanceGenerator: Main DASO: " + mainDasoUri);
        logger.println("DASOInstanceGenerator: Main SOC: " + mainSocUri);
        groundingPathUris.clear();
        
        return true;
    }

    private boolean identifyGroundingPathForMainSOC() {
        /* 
         *  (3/10) IDENTIFY GROUNDING PATH FOR MAIN SOC
         */

        logger.println("DASOInstanceGenerator: (3/10) =========== GROUNDING PATH FOR  MAIN SOC ============");
        if (mainSoc.getHasScopeUri() == null || mainSoc.getHasScopeUri().equals("")) {
            logger.println("DASOInstanceGenerator: Main SOC is already grounded. No grouding path required");
            groundingSoc = mainSoc;
        } else {
            logger.println("DASOInstanceGenerator: Main SOC is not grounded. Computing grouding path");
            StudyObjectCollection currentSoc = mainSoc;
            groundingPath.add(mainSoc);
            while (currentSoc.getHasScopeUri() != null && !currentSoc.getHasScopeUri().equals("") && !containsUri(currentSoc.getHasScopeUri(), groundingPath)) {

                StudyObjectCollection nextSoc = StudyObjectCollection.find(currentSoc.getHasScopeUri());
                if (nextSoc == null) {
                    logger.printException("DASOInstanceGenerator: Could not find SOC with following URI : " + currentSoc.getHasScopeUri());
                    return false;
                } else {
                    if (!containsUri(nextSoc.getUri(), groundingPath)) {
                        groundingPath.add(nextSoc);
                    }
                    currentSoc = nextSoc;
                }
            }
            int i = 0;
            for (StudyObjectCollection soc : groundingPath) {
                logger.println("DASOInstanceGenerator: SOC in grouding path: " + soc.getUri() + " at index " + i++);
                groundingPathUris.add(soc.getUri());
            }
            i = 0;
            for (String socUri : groundingPathUris) {
                logger.println("DASOInstanceGenerator: SOCURI in grouding path: " + socUri + " at index " + i++);
            }
        }
        
        return true;
    }

    private boolean identifyTargetDasoURIs() {
        /* 
         *  (4/10) IDENTIFY URIs of TARGET DASOs
         */

        logger.println("DASOInstanceGenerator: (4/10) =============== TRAVERSE DASOS ================");

        for (Map.Entry<String, SDDObject> entry : dasos.entrySet()) {
            String key = entry.getKey();
            SDDObject daso = entry.getValue();
            processTargetDaso(daso);
        }
        
        return true;
    }

    private boolean identitySOCsForDASOs() {
        /* 
         *  (5/10) IDENTIFY SOCs ASSOCIATED WITH IDENTIFIED DASOs
         */

        logger.println("DASOInstanceGenerator: (5/10) ===== IDENTIFY SOCs ASSOCIATED WITH IDENTIFIED DASOs ======");

        this.requiredSocs.clear();
        for (Map.Entry<String, SDDObject> entry : dasos.entrySet()) {
            String key = entry.getKey();
            SDDObject daso = entry.getValue();
            if (!findCreateAssociatedSOC(daso)) {
                logger.printWarning("DASOInstanceGenerator: Cannot create SOC for the following daso: " + daso.getUri());
            }
        }
        
        return true;
    }

    private boolean retrieveAdditionalSOCs() {
        /* 
         *  (6/10) RETRIEVING ADDITIONAL SOCs required for traversing existing SOCs
         */

        logger.println("DASOInstanceGenerator: (6/10) ======== RETRIEVING ADDITINAL  SOCs ========");
        for (Map.Entry<String, StudyObjectCollection> entry : requiredSocs.entrySet()) {
            String key = entry.getKey();
            StudyObjectCollection soc = entry.getValue();
            StudyObjectCollection currentSoc = soc;
            
            while (currentSoc.getHasScopeUri() != null && !currentSoc.getHasScopeUri().equals("") && 
                    !requiredSocs.containsKey(currentSoc.getHasScopeUri()) && !containsUri(currentSoc.getHasScopeUri(),groundingPath)) {

                // lookup in socsList for next SOC, i.e., currentSoc.getHasScopeUri()
                StudyObjectCollection nextSoc = null;
                for (StudyObjectCollection tmpSoc : socsList) {
                    if (tmpSoc.getUri().equals(currentSoc.getHasScopeUri())) {
                        nextSoc = tmpSoc;
                        break;
                    }
                }
                if (nextSoc == null) {
                    logger.printException("DASOInstanceGenerator: Could not find SOC with following URI : " + currentSoc.getHasScopeUri());
                    return false;
                } else {
                    if (!requiredSocs.containsKey(nextSoc.getUri())) {
                        requiredSocs.put(nextSoc.getUri(), nextSoc);
                        logger.println("DASOInstanceGenerator: Loading SOC: " + nextSoc.getUri() + " to required SOCs");
                    }
                    currentSoc = nextSoc;
                }
            }   
        }
        
        return true;
    }

    private boolean printRequiredSOCs() {
        /* 
         *  (7/10) LIST OF REQUIRED SOCs
         */

        logger.println("DASOInstanceGenerator: (7/10) ======== REQUIRED SOCs ========");
        for (Map.Entry<String, StudyObjectCollection> entry : requiredSocs.entrySet()) {
            String key = entry.getKey();
            StudyObjectCollection soc = entry.getValue();
            logger.println("DASOInstanceGenerator: SOC: " + soc.getUri() + "   Reference : " + soc.getSOCReference() + 
                    "    with hasScope: " + soc.getHasScopeUri());
        }
        
        return true;
    }

    private boolean computePathsForTargetSOCs() {
        /* 
         *  (8/10) COMPUTE PATH for each TARGET SOC
         */

        logger.println("DASOInstanceGenerator: (8/10) ======== BUILD SOC PATHS ========");
        for (Map.Entry<String, StudyObjectCollection> entry : requiredSocs.entrySet()) {
            String key = entry.getKey();
            StudyObjectCollection soc = entry.getValue();
            List<StudyObjectCollection> socs = new ArrayList<StudyObjectCollection>();
            logger.println("DASOInstanceGenerator: START: " + soc.getUri());
            logger.println("DASOInstanceGenerator: PATH ---->> ");
            if (groundingPathUris.contains(soc.getUri())) {
                //logger.println("DASOInstanceGenerator:       " + soc.getUri() + " (in grounding path)");
            	String socAux = null;
            	boolean start = false;
    			logger.println("DASOInstanceGenerator:       DBG: grounding path size = " + groundingPathUris.size());
            	for (int index = groundingPathUris.size() - 1; index >= 0; index--) {
            		socAux = groundingPathUris.get(index);
            		if (socAux.equals(soc.getUri())) {
            			start = true;
            		}
            		if (start) {
            			logger.println("DASOInstanceGenerator:       " + socAux + " (in grounding path)");
            			if (groundingPath.get(index) == null || !groundingPath.get(index).getUri().equals(socAux)) {
                            logger.println("DASOInstanceGenerator:       ERROR: Could not find SOC for " + socAux);
            			} else {
            				socs.add(groundingPath.get(index));
            			}
            		}
        			logger.println("DASOInstanceGenerator:       DBG: " + socAux + " (" + index + ")  start (" + start + ")");
            	}
            } else if (soc.getHasScope() == null) {
                logger.println("DASOInstanceGenerator:       " + soc.getUri());
                socs.add(soc);
            } else {
                String toUri = soc.getHasScope().getUri();
                StudyObjectCollection nextTarget = soc;
                logger.println("DASOInstanceGenerator:       " + nextTarget.getUri());
                socs.add(nextTarget);
                while (nextTarget != null && !nextTarget.getUri().equals(mainSocUri)) {
                    String nextTargetUri = nextTarget.getHasScopeUri();
                    nextTarget =  requiredSocs.get(nextTargetUri);
                    if (nextTarget != null) {
                    	logger.println("DASOInstanceGenerator:       " + nextTarget.getUri());
                    	socs.add(nextTarget);
                    }
                }
                if (nextTarget == null) {
                	reverseGroundingPath = new ArrayList<StudyObjectCollection>(); 
                	//reverseGroundingPath.add(mainSoc);
                	boolean validPath = false;
                	for (StudyObjectCollection socTmp : groundingPath) {
                		if (socTmp.getUri().equals(toUri)) {
                			validPath = true;
                			break;
                		}
                		reverseGroundingPath.add(socTmp);
                	}
                	if (!validPath) {
                    	logger.printException("DASOInstanceGenerator: Could not complete path for " + toUri);
                    	return false;
                	}
                	StudyObjectCollection socTmp2 = null;
                	if (reverseGroundingPath.size() > 0) {
                		for (int index = reverseGroundingPath.size() - 1; index >= 0; index--) {
                			socTmp2 = reverseGroundingPath.get(index);
                        	logger.println("DASOInstanceGenerator:       " + socTmp2.getUri());
                        	socs.add(socTmp2);
                		}
                	}
                }
            } 
            socPaths.put(key,socs);
        }
        for (Map.Entry<String, List<StudyObjectCollection>> entry : socPaths.entrySet()) {
            String key = entry.getKey();
            List<StudyObjectCollection> path = entry.getValue();
            List<StudyObjectCollection> socs = new ArrayList<StudyObjectCollection>();
            logger.println("DASOInstanceGenerator: DEBUG START: " + key);
            logger.println("DASOInstanceGenerator:    DEBUG PATH ---->> ");
            for (StudyObjectCollection socTmp : path) {
                logger.println("DASOInstanceGenerator:    DEBUG ELEMENT:  " + socTmp.getUri());
            } 
        }
        
        return true;
    }

    private boolean computeLabelsForTargetSOCs() {
        /* 
         *  (9/10) COMPUTE LABEL for each TARGET SOC
         */

        logger.println("DASOInstanceGenerator: (9/10) ======== COMPUTE SOC LABELS ========");
        for (Map.Entry<String, StudyObjectCollection> entry : requiredSocs.entrySet()) {
            String key = entry.getKey();
            StudyObjectCollection soc = entry.getValue();
            String fullLabel = "";
            boolean process = true;

            logger.println("DASOInstanceGenerator: START: " + soc.getUri());
            String label = soc.getGroundingLabel();
            if (label == null) {
                label = "";
            }
            if (soc.getHasScope() == null || !label.equals("")) {
                fullLabel = label;
                logger.println("DASOInstanceGenerator: Computed label [" + fullLabel + "]");
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

                StudyObjectCollection nextTarget = soc;
                label = nextTarget.getGroundingLabel();
                if (label == null) {
                    label = "";
                }
                if (nextTarget.getUri().equals(mainSocUri)) {
                    String nextTargetUri = nextTarget.getHasScopeUri();
                    nextTarget = null;
                    for (StudyObjectCollection gsoc : groundingPath) {
                    	if (gsoc.getUri().equals(nextTargetUri)) {
                    		nextTarget = gsoc;
                    	}
                    }
                    if (nextTarget == null) {
                        logger.printException("DASOInstanceGenerator: Could not complete path for " + toUri);
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
	                        logger.printException("DASOInstanceGenerator: Could not complete path for " + toUri);
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
                logger.println("DASOInstanceGenerator: Computed label [" + fullLabel + "]");       
                if (soc.getRoleLabel() == null || soc.getRoleLabel().equals("")) {
                    soc.saveRoleLabel(fullLabel);
                }
                socLabels.put(soc.getSOCReference(), fullLabel);
            }
        }
        
        return true;
    }

    private boolean mapSOCsAndMatchings() {
        /* 
         *  (10/10) Map SOCs and Matchings 
         */

        logger.println("DASOInstanceGenerator: (10/10) ======== MAP SOCs and MATCHINGS ========");
        for (Map.Entry<String, StudyObjectCollection> entry : requiredSocs.entrySet()) {
            String key = entry.getKey();
            StudyObjectCollection soc = entry.getValue();
            if (!socMatchingSOCs.containsKey(soc.getUri())) {
            	List<StudyObjectCollection> matchingSOCs = StudyObjectCollection.findMatchingScopeCollections(soc.getUri());
            	if (matchingSOCs.size() > 1) {
            		logger.printWarning("DASOInstanceGenerator: SOC: " + soc.getUri() + "   has more than one matching SOC");
            	}
            	if (matchingSOCs.size() >- 0) {
            		socMatchingSOCs.put(soc.getUri(), matchingSOCs.get(0));
            		logger.println("DASOInstanceGenerator: SOC: " + soc.getUri() + "   Has matching SOC: " + matchingSOCs.get(0).getUri());
            	}
            }
        }
        
        return true;
    }

    /* **************************************************************************************
     *                                                                                      *
     *                            SUPPORTING METHODS                                        *
     *                                                                                      *
     ****************************************************************************************/

    private boolean containsUri(String uri, List<StudyObjectCollection> list) {
        if (uri == null || uri.equals("") || list == null || list.size() == 0) {
            return false;
        }
        for (StudyObjectCollection soc : list) {
            if (soc.getUri() != null && !soc.getUri().equals("")) {
                if (soc.getUri().equals(uri)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean processTargetDaso(SDDObject daso) { 
        String toUri = targetUri(daso);
        logger.println("DASOInstanceGenerator: DASO: " + daso.getUri() + "   From : " + daso.getLabel() + "  To: " + toUri);

        //  LOAD each TARGET DASO into DASOs, if TARGET DASO is not loaded yet
        if (toUri != null && !toUri.equals("") && !dasos.containsKey(toUri)) {
            logger.println("DASOInstanceGenerator: Loading " + toUri);
            SDDObject newDaso = SDDObject.find(toUri);
            if (newDaso == null) {
                logger.println("DASOInstanceGenerator: [ERROR] Could not find DASO with following URI : " + toUri);
                return false;
            }
            dasos.put(toUri, newDaso);
            return processTargetDaso(newDaso);
        }
        return true;
    }

    private String targetUri(SDDObject daso) {
        if (!daso.getWasDerivedFrom().equals("")) {
            String toLabel = daso.getWasDerivedFrom();
            SDDObject tmpDaso = SDDObject.findByLabelInSchema(sdd.getUri(), toLabel);
            if (tmpDaso == null) {
                return "";
            } else {

                return tmpDaso.getUri();
            }
        } else if (!daso.getInRelationTo().equals("")) {
            return daso.getInRelationTo();
        }
        return "";
    }

    private boolean isSample(SDDObject daso) {
        if (!daso.getWasDerivedFrom().equals("")) {
            String toLabel = daso.getWasDerivedFrom();
            SDDObject tmpDaso = SDDObject.findByLabelInSchema(sdd.getUri(), toLabel);
            if (tmpDaso == null) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    private StudyObjectCollection socFromTargetDaso(SDDObject daso, List<StudyObjectCollection> list) {
        String targetObjUri = targetUri(daso);
        if (targetObjUri.equals("")) {
            return null;
        }
        SDDObject targetObj = SDDObject.find(targetObjUri);
        if (targetObj == null || targetObj.getLabel() == null || targetObj.getLabel().equals("")) {
            return null;
        }
        for (StudyObjectCollection soc : list) {
        	//logger.println("socFromTargetDaso : " + targetObj.getLabel() + "    soc's getSOCReference " + soc.getSOCReference()); 
            if (soc.getSOCReference().equals(targetObj.getLabel())) {
                return soc;
            }
        }
        return null;
    }

    private StudyObjectCollection socFromDaso(SDDObject daso, List<StudyObjectCollection> list) {
        if (daso == null || daso.getLabel() == null || daso.getLabel().equals("")) {
            return null;
        }
        for (StudyObjectCollection soc : list) {
            if (soc.getSOCReference().equals(daso.getLabel())) {
                return soc;
            }
        }
        return null;
    }

    private SDDObject dasoFromSoc(StudyObjectCollection soc, Map<String, SDDObject> mapDasos) {
        if (soc == null || soc.getSOCReference() == null || soc.getSOCReference().equals("")) {
            return null;
        }
        for (Map.Entry<String, SDDObject> entry : mapDasos.entrySet()) {
            SDDObject daso = entry.getValue();
            if (soc.getSOCReference().equals(daso.getLabel())) {
                return daso;
            }
        }
        return null;
    }

    private boolean findCreateAssociatedSOC(SDDObject daso) {
        StudyObjectCollection associatedSOC = null;

        //  Try to find existing SOC
        for (StudyObjectCollection soc : socsList) {
            if (soc.getSOCReference().equals(daso.getLabel())) {
                associatedSOC = StudyObjectCollection.find(soc.getUri());
                if (associatedSOC != null) {
                    logger.println("DASOInstanceGenerator: Reference: " + daso.getLabel() + "  Associated SOC : " + associatedSOC + "    with hasScope: " + associatedSOC.getHasScopeUri());
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
                StudyObjectCollection scopeObj = socFromTargetDaso(daso, socsList);
                if (scopeObj != null && scopeObj.getUri() != null) {
                    scopeUri = scopeObj.getUri();
                } else {
                	String tmpUri = targetUri(daso);
                	if (tmpUri == null || tmpUri.isEmpty()) {
                		logger.println("DASOInstanceGenerator:       [WARNING] SOC association ignored for " + daso.getUri());
                		return false;
                	}
                    SDDObject newDaso = SDDObject.find(tmpUri);
                    if (newDaso == null) {
                        logger.println("DASOInstanceGenerator: [ERROR] Could not find DASO with following URI : " + tmpUri);
                        return false;
                    }
                    scopeUri = studyUri.contains("STD") ? studyUri.replace("STD","SOC") + "-" + newDaso.getLabel().replace("??","") : studyUri.replace("SSD","SOC") + "-" + newDaso.getLabel().replace("??","");;
                	//scopeUri = tmpUri.replace("DASO", "SOC");
                }
            }
            String newLabel = daso.getLabel().replace("??","");
            String collectionType = null;
            if (daso.getEntity().equals(URIUtils.replacePrefixEx("hasco:StudyObjectMatching"))) {
            	collectionType = HASCO.MATCHING_COLLECTION;
            } else if (isSample(daso)) {
                collectionType = HASCO.SAMPLE_COLLECTION;
            } else {
                // collectionType = StudyObjectCollection.SUBJECT_COLLECTION;
                collectionType = HASCO.OBJECT_COLLECTION;
            }

            VirtualColumn newVc = VirtualColumn.find(studyUri, daso.getLabel());
            if (newVc == null) {
                newVc = new VirtualColumn(studyUri, "", daso.getLabel(), scopeUri);
                newVc.setNamedGraph(namedGraphUri);
                newVc.saveToTripleStore();
                // addObject(newVc);
            }
            StudyObjectCollection newSoc = new StudyObjectCollection();
            
            newSoc.setUri(newSOCUri);
            newSoc.setTypeUri(URIUtils.replacePrefixEx(collectionType));
            newSoc.setHascoTypeUri(daso.getTypeUri());
            newSoc.setLabel(newLabel);
            newSoc.setComment(newLabel);
            newSoc.setIsMemberOfUri(studyUri);
            newSoc.setVirtualColumnUri(newVc.getUri());
            newSoc.setRoleUri(daso.getRole());
            newSoc.setHasSIRManagerEmail(daso.getHasSIRManagerEmail());
            newSoc.setHasScopeUri(scopeUri);
            newSoc.setTimeScopeUris(null);
            newSoc.setSpaceScopeUris(null);
            newSoc.setGroupUris(null);
            newSoc.setLastCounter("0");

            newSoc.setNamedGraph(namedGraphUri);
            newSoc.saveToTripleStore();
            // addObject(newSoc);

            if (!requiredSocs.containsKey(newSoc.getUri())) {
                requiredSocs.put(newSoc.getUri(), newSoc);
                socsList.add(newSoc);
            }
            logger.println("DASOInstanceGenerator: Reference: " + daso.getLabel() + "   Created SOC : " + newSOCUri + "    with hasScope: " + scopeUri);
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

    /* **************************************************************************************
     *                                                                                      *
     *                GENERATE INSTANCES FOR A GIVEN ROW's IDENTIFIER                       *
     *                                                                                      *
     ****************************************************************************************/

    public Map<String, Map<String, String>> generateRowInstances(String id) {
        /* Returns : First String : DASO's Label
         *           Object URI   : The actual URI of the object that was retrieved/created for the identifier in CSV Record
         */

        if (id == null || id.equals("")) {
            System.out.println("DASOInstanceGenerator::generateRowInstances: [ERROR] no identifier provided. See if your SDD contains an identifier," +
                    " and if the corresponding label in ths file is a valid identifier.");
            return null;
        }
        
        if (DEBUG_MODE) { 
        	System.out.println("DASOInstanceGenerator: generate row instances for : " + id);
        }

        Map<String,Map<String,String>> objMapList = new HashMap<String,Map<String,String>>();

        /*
         *   TRAVERSE list of objects for current record
         */

        for (Map.Entry<String, List<StudyObjectCollection>> entry : socPaths.entrySet()) {
            String key = entry.getKey();
            List<StudyObjectCollection> path = entry.getValue();

            /*
             *   TRAVERSE SOC's PATH
             */

            ListIterator<StudyObjectCollection> iter = path.listIterator(path.size());

            if (DEBUG_MODE) { 
            	System.out.println("DASOInstanceGenerator:     PATH >>> ");
            }

        	// Lookup first study object
        	StudyObjectCollection currentSoc = iter.previous();
        	String currentObjUri = getCachedObjectBySocAndOriginalId(currentSoc.getUri(), id); 
        	if (DEBUG_MODE) { 
        		System.out.println("DASOInstanceGenerator:          Obj Original ID=[" + id + "]   SOC=[" + currentSoc.getUri() + "] =>  Obj URI=[" + currentObjUri + "]");
        	}

        	/* 
             *   Test if there is Grounding Path. If so, replace ID of a main SOC's object by the ID of 
             *   the corresponding grounding scope object of the main SOC's object
             */
        	boolean hasGrounding = groundingPath.size() > 0;
        	
            StudyObjectCollection previousSoc = null;
            String previousObjUri = null;
            while (currentObjUri != null && !currentObjUri.equals("") && iter.hasPrevious()) {
                StudyObjectCollection nextSoc = iter.previous();
                if (DEBUG_MODE) { 
                	System.out.println("           Next SOC : [" + nextSoc.getUri() + "]    Current Obj URI : [" + currentObjUri + "]");
                }

                /*
                 *   RETRIEVE/CREATE next object in the path
                 */

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
                    	System.out.println("DASOInstanceGenerator:          [ERROR] Path generation stopped. Error ocurred retrieving/creating objects in path. See log above.");
                    }
                    currentSoc = nextSoc;
                    currentObjUri = nextObjUri;
                    break;
                }

                if (DEBUG_MODE) { 
                	System.out.println("DASOInstanceGenerator:          Scope Obj URI=[" + currentObjUri + "]  SOC=[" + nextSoc.getUri() + 
                			"]  =>  Obj Uri=[" + nextObjUri + "]");
                }

                previousSoc = currentSoc;
                previousObjUri = currentObjUri;
                currentSoc = nextSoc;
                currentObjUri = nextObjUri;
            }

            if (currentObjUri == null || currentObjUri.equals("")) {
            	System.out.println("DASOInstanceGenerator:     Response >>> failed to load object");
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
        	System.out.println("DASOInstanceGenerator:     Response >>> ");
        	for (Map.Entry<String, Map<String,String>> entry : objMapList.entrySet()) {
        		String label = entry.getKey();
        		Map<String,String> objMapEntry = entry.getValue();
        		System.out.println("DASOInstanceGenerator:          Label=[" + label + "]    Obj Uri=[" + objMapEntry.get(StudyObject.STUDY_OBJECT_URI) + "]");
        	}
        }

        return objMapList;
    }// /generateRowInstances

    /*
     *   CREATE next object in the path if it does not exist
     */

    private String createStudyObject(StudyObjectCollection nextSoc, String currentObjUri) {
        String newOriginalId = String.valueOf(nextSoc.getNextCounter());
        newOriginalId = addLeftZeros(newOriginalId);
        String newUri = createObjectUri(newOriginalId, nextSoc.getUri(), nextSoc.getTypeUri());
        String newLabel = createObjectLabel(newOriginalId, nextSoc);
        String newTypeUri = "";
        SDDObject daso = dasoFromSoc(nextSoc, dasos);
        if (daso == null || daso.getEntity() == null || daso.getEntity().equals("")) {
            if (nextSoc.getTypeUri().equals(HASCO.MATCHING_COLLECTION)) {
                newTypeUri = null;//URIUtils.replacePrefixEx(StudyObjectMatching.className);
            } else if (nextSoc.getTypeUri().equals(HASCO.SUBJECT_COLLECTION)) {
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

        StudyObject newObj = new StudyObject(newUri, newTypeUri, "", newOriginalId, newLabel, nextSoc.getUri(), "Automatically generated",
                newScopeUris, newTimeScopeUris, newSpaceScopeUris, "");
        newObj.setNamedGraph(str.getUri());
        newObj.setDeletable(false);
        addObjectToCache(newObj, currentObjUri);

        if (DEBUG_MODE) { 
        	System.out.println("DASOInstanceGenerator:          Created Obj with URI=[" + newUri + "]   Type=[" + newTypeUri + "]");
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
        if (socTypeUri.equals(HASCO.SUBJECT_COLLECTION)) {
            labelPrefix = "SBJ-";
        } else {
            labelPrefix = "SPL-";
        }
        String uri = kbPrefix + labelPrefix + originalID + "-" + socIdFromUri(socUri);
        uri = URIUtils.replacePrefixEx(uri);

        return uri;
    }

    private String createObjectLabel(String originalID, StudyObjectCollection soc) {
        if (soc.getRoleLabel() != null && !soc.getRoleLabel().equals("")) {
        	return soc.getRoleLabel() + " " + originalID;
        } 
        String labelPrefix = "";
        if (soc.getTypeUri().equals(HASCO.SUBJECT_COLLECTION)) {
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
    

    /*
     *   METHODS RELATED TO INTERNAL CACHE
     */

    public boolean initiateCache(String study_uri) {
    	if (study_uri == null || study_uri.equals("")) {
    		return false;
    	}
    	Study study = Study.find(study_uri);
    	//if (mainSoc != null) {
    		System.out.println("INITIATE CACHE BEING CALLED!");
    		//addCache(new Cache<String, StudyObject>("cacheObject", true, study.getObjectsMapInBatch()));
    		addCache(new Cache<String, String>("cacheObjectBySocAndScopeUri", false, StudyObject.buildCachedObjectBySocAndScopeUri()));
    		//addCache(new Cache<String, String>("cacheObjectBySocAndOriginalId", false, StudyObject.buildCachedObjectBySocAndOriginalId()));
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
 
    public Map<String,StudyObjectCollection> getMatchingSOCs() {
    	return socMatchingSOCs;
    }
    
    /* **************************************************************************************
     *                                                                                      *
     *  RETRIEVE URI, ORIGINAL ID,  AND TYPE OF GROUNDING OBJECT FROM CURRENT OBJECT URI    *
     *                                                                                      *
     ****************************************************************************************/

    public Map<String, String> retrieveGroundObject(String id) {
        /* Returns : First String : DASO's Label
         *           Object URI   : The actual URI of the object that was retrieved/created for the identifier in CSV Record
         */

        if (id == null || id.equals("")) {
            System.out.println("DASOInstanceGenerator::retrieveGroundObject: [ERROR] no identifier provided. See if your SDD contains an identifier," +
                    " and if the corresponding label in ths file is a valid identifier.");
            return null;
        }
        if (DEBUG_MODE) { 
        	System.out.println("DASOInstanceGenerator: retrieve ground object for : " + id);
        	System.out.println("DASOInstanceGenerator: groundingPath : " + groundingPath);
        }

        StudyObjectCollection currentSoc = mainSoc;
        StudyObject obj = null;
        Map<String,String> groundObj = new HashMap<String,String>();

        if (DEBUG_MODE) { 
        	System.out.println("DASOInstanceGenerator:     PATH >>> ");
        }

        // Lookup first study object
        if (DEBUG_MODE) { 
        	System.out.println("DASOInstanceGenerator: CachedObjectBySocAndOriginalId: soc: [" +currentSoc.getUri() + "]   Id: [" + id + "]");
        }
        String currentObjUri = getCachedObjectBySocAndOriginalId(currentSoc.getUri(), id); 
        if (DEBUG_MODE) { 
        	System.out.println("DASOInstanceGenerator: currentObjUri: [" +currentObjUri + "]");
        }

        if (groundingPath == null || groundingPath.size() <= 0) {
        	obj = getCachedObject(currentObjUri);
        	if (obj == null || obj.getUri() == null || obj.getUri().equals("")) {
                System.out.println("DASOInstanceGenerator: [ERROR] Could not retrieve first Study Object for URI=[" + currentObjUri + "]");
                return null;
            }
            groundObj.put(StudyObject.STUDY_OBJECT_URI, obj.getUri());
            groundObj.put(StudyObject.STUDY_OBJECT_TYPE, obj.getTypeUri());
            groundObj.put(StudyObject.SUBJECT_ID, obj.getOriginalId());
            return groundObj;
        } 

        if (DEBUG_MODE) { 
        	System.out.println("DASOInstanceGenerator:          Obj Original ID=[" + id + "]   SOC=[" + currentSoc.getUri() + "] =>  Obj URI=[" + currentObjUri + "]");
        }
        
        for (int index=1; index < groundingPath.size(); index++) {
        	StudyObjectCollection nextSoc = groundingPath.get(index);
            if (DEBUG_MODE) { 
            	System.out.println("DASOInstanceGenerator:      nextSOC=[" + nextSoc.getUri() + "] Obj URI=[" + currentObjUri + "]");
            }
            String nextObjUri = getCachedScopeBySocAndObjectUri(nextSoc.getUri(), currentObjUri); 
            if (nextObjUri == null || nextObjUri.equals("")) {
                System.out.println("DASOInstanceGenerator:          [ERROR] Path generation stopped. Error ocurred retrieving/creating objects in path. See log above.");
                currentSoc = nextSoc;
                currentObjUri = nextObjUri;
                break;
            }

            if (DEBUG_MODE) { 
            	System.out.println("DASOInstanceGenerator:          Scope Obj URI=[" + currentObjUri + "]  nextSOC=[" + nextSoc.getUri() + 
            			"]  =>  Obj Uri=[" + nextObjUri + "]");
            }

            currentSoc = nextSoc;
            currentObjUri = nextObjUri;
        }

    	obj = getCachedObject(currentObjUri);
        if (obj == null) {
            //System.out.println("DASOInstanceGenerator: [ERROR] Could not retrieve Study Object for ID=[" + id + "]");
            return null;
        }
        groundObj.put(StudyObject.STUDY_OBJECT_URI, obj.getUri());
        groundObj.put(StudyObject.STUDY_OBJECT_TYPE, obj.getTypeUri());
        groundObj.put(StudyObject.SUBJECT_ID, obj.getOriginalId());
        return groundObj;

    }// /retrieveGroundObject

}// /class

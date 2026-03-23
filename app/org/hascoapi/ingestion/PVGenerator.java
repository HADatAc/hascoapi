package org.hascoapi.ingestion;

import org.hascoapi.entity.pojo.Attribute;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.NameSpace;
import org.hascoapi.entity.pojo.PossibleValue;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.IngestionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.String;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Base64;
import java.math.BigInteger;  
import java.nio.charset.StandardCharsets; 
import java.security.MessageDigest;  
import java.security.NoSuchAlgorithmException;  

public class PVGenerator extends BaseGenerator {

	private static final Logger log = LoggerFactory.getLogger(PVGenerator.class);

	final String kbPrefix = ConfigProp.getKbPrefix();
	String startTime = "";
	String sddName = "";
	String sddUri = "";
	String managerEmail = "";
	Map<String, String> codeMap;
	Map<String, Map<String, String>> pvMap = new HashMap<String, Map<String, String>>();
	Map<String, String> mapAttrObj;
	Map<String, String> codeMappings;
    protected IngestionLogger logger = null;

	public PVGenerator(DataFile dataFile, String sddUri, String sddName,  
			Map<String, String> mapAttrObj, Map<String, String> codeMappings) {
		super(dataFile);
		this.sddUri = sddUri;
		this.sddName = sddName;
		this.mapAttrObj = mapAttrObj;
		this.codeMappings = codeMappings;
		this.logger = dataFile.getLogger();
		this.managerEmail = dataFile.getHasSIRManagerEmail();
	}
	
	//Column	Code	Label	Class	Resource
	@Override
	public void initMapping() {
		mapCol.clear();
		mapCol.put("Label", "Column");
		mapCol.put("Code", "Code");
		mapCol.put("CodeLabel", "Label");
		mapCol.put("Class", "Class");
		mapCol.put("Resource", "Resource");
		mapCol.put("OtherFor", "Other For");
	}

    private String getLabel(Record rec) {
		return rec.getValueByColumnName(mapCol.get("Label"));
	}

	private String getCode(Record rec) {
		String ss = Normalizer.normalize(rec.getValueByColumnName(mapCol.get("Code")), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "").trim();
		/*
		int iend = ss.indexOf(".");
		if (iend != -1){
			ss = ss.substring(0 , iend);
		}
		*/
		return ss.trim();
	}

	private String getCodeLabel(Record rec) {
		return rec.getValueByColumnName(mapCol.get("CodeLabel"));
	}

	private String getClass(Record rec) {
		String cls = rec.getValueByColumnName(mapCol.get("Class"));
		if (cls.length() > 0) {
			if (URIUtils.isValidURI(cls)) {
				return cls;
			}
		} else {
			if (codeMappings.containsKey(getCode(rec))) {
				return codeMappings.get(getCode(rec));
			}
		}

		return "";
	}

	private String getResource(Record rec) {
		return rec.getValueByColumnName(mapCol.get("Resource"));
	}

	private Boolean checkVirtual(Record rec) {
		if (getLabel(rec).contains("??")){
			return true;
		} else {
			return false;
		}
	}

	private String getOtherFor(Record rec) {
		return rec.getValueByColumnName(mapCol.get("OtherFor"));
	}

	/**
	 * Build the SDDAttribute URI for a given column name.
	 * The PossibleValue must link to the actual SDDAttribute URI, not just the column name string.
	 *
	 * CRITICAL FIX: During the generator chain execution, SDDAttributes are created BEFORE PossibleValues,
	 * but they are NOT YET COMMITTED to the triple store when PVGenerator runs.
	 * Therefore, we CANNOT query the triple store for SDDAttributes.
	 *
	 * Instead, we use the mapAttrObj provided by the SDDAttributeGenerator, which contains
	 * the mapping from column labels to their corresponding SDDAttribute URIs.
	 *
	 * The URI pattern is: https://hadatac.org/ont/hadatac#/SDDATT{timestamp}/{index}
	 */
	private String getPVvalue(Record rec) {
		String columnName = getLabel(rec);
		if (columnName == null || columnName.isEmpty()) {
			return "";
		}

		// Clean the column name
		columnName = columnName.trim();

		// First, try to find the URI in the mapAttrObj provided by SDDAttributeGenerator
		if (mapAttrObj != null && mapAttrObj.containsKey(columnName)) {
			String attrUri = mapAttrObj.get(columnName);
			logger.println("[PVGenerator] Linked PossibleValue for column '" + columnName +
				"' to SDDAttribute (via mapAttrObj): " + attrUri);
			return attrUri;
		}

		// If not found in map, log warning and fall back to column name
		// (This should not happen if the Codebook references valid columns from Dictionary Mapping)
		logger.printWarningByIdWithArgs("PV_00001",
			"Column '" + columnName + "' not found in SDDAttribute map. Codebook may reference non-existent column.");
		return columnName.replace(" ", "");
	}
	
	public List<String> createUris() throws Exception {
		int rowNumber = 0;
		List<String> result = new ArrayList<String>();
		for (Record record : records) {
			result.add((kbPrefix + "PV-" + getLabel(record).replace("_","-").replace("??", "") + ("-" + sddName + "-" + getCode(record)).replaceAll("--", "-")).replace(" ","") + "-" + rowNumber);
			++rowNumber;
		}
		return result;
	}

	@Override
	public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {	
		Map<String, Object> row = new HashMap<String, Object>();
		String sddPVUri = sddUri.replace("SDDICT","PSV") + "/" + String.valueOf(rowNumber);
		row.put("hasURI", sddPVUri);
		row.put("a", "hasco:PossibleValue");
		row.put("hasco:hascoType", "hasco:PossibleValue");
		row.put("hasco:partOfSchema", sddUri);
		row.put("hasco:listPosition", String.valueOf(rowNumber));
		row.put("hasco:hasVariable", getLabel(rec).replaceAll("[^a-zA-Z0-9:-]", "-"));
		row.put("hasco:hasCode", getCode(rec));
		row.put("hasco:hasCodeLabel", getCodeLabel(rec));
		row.put("hasco:hasClass", getClass(rec));
		row.put("hasco:isPossibleValueOf", getPVvalue(rec));
		row.put("hasco:otherFor", getOtherFor(rec));
		row.put("vstoi:hasSIRManagerEmail", managerEmail);
		
		return row;
	}

	@Override
	public String getTableName() {
		return "PossibleValue";
	}

	@Override
	public String getErrorMsg(Exception e) {
		return "Error in PVGenerator: " + e.getMessage();
	}
 	 
    public static String hashWith256(String textToHash) {
    	String encoded = null;
    	try {
    		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
    		byte hashBytes[] = messageDigest.digest(textToHash.getBytes(StandardCharsets.UTF_8));
    		BigInteger noHash = new BigInteger(1, hashBytes);
    		encoded = noHash.toString(16);
    	} catch (Exception e) {
    	}
        return encoded;
    }
    
    private static String generateDCTerms(String variable, String code) {  
    	return variable + "||||" + code;
    }
    
    private static void generateOtherOther(String superUri, PossibleValue pv, String graphName) {
    	if (pv.getHasClass() == null || pv.getHasClass().isEmpty()) {
    		return;
    	}
    	Attribute attr = Attribute.find(pv.getHasClass());
    	if (attr != null) {
    		attr.setHasDCTerms(generateDCTerms(pv.getHasVariable(), pv.getHasCode()));
    		attr.setSuperUri(superUri);
			attr.setNamedGraph(graphName);
    		attr.save();
    	}
    }

    private static void generateOther(String uri, String harmonizedCode, PossibleValue pv, String graphName) {
    	if (pv.getHasOtherFor() == null || pv.getHasOtherFor().isEmpty()) {
    		return;
    	}
    	Attribute attr = new Attribute();
    	attr.setUri(uri);
    	attr.setLabel(pv.getHasCodeLabel());
    	attr.setSuperUri(pv.getHasOtherFor());
    	attr.setHasDCTerms(generateDCTerms(pv.getHasVariable(), pv.getHasCode()));
    	attr.setHasSkosNotation(harmonizedCode);
    	attr.setNamedGraph(graphName);
    	attr.save();
    	
    }
    
    public static void generateOthers(DataFile dataFile, String sddUri, String kbPrefix) {
        // Defensive: PVGenerator is only meaningful for SDD codebooks.
        // For other ingest flows, chain.getPV() should be false; but if it's true
        // (or codebookFile/logger is missing) we must not crash finalization.
        if (dataFile == null) {
            System.out.println("[WARN] PVPostGenerator: dataFile is null; skipping PV post-processing.");
            return;
        }

        IngestionLogger logger = dataFile.getLogger();
        if (logger == null) {
            System.out.println("[WARN] PVPostGenerator: dataFile logger is null; skipping PV post-processing. dataFileUri=" + dataFile.getUri());
            return;
        }

        if (sddUri == null || sddUri.trim().isEmpty()) {
            logger.println("[WARN] PVPostGenerator: sddUri is empty; skipping PV post-processing.");
            return;
        }

        logger.println("PVPostGenerator: Processing additional knowledge for <" + sddUri + ">");

        List<PossibleValue> codes = PossibleValue.findBySchema(sddUri);
        if (codes == null) {
            logger.println("[WARN] PVPostGenerator: PossibleValue.findBySchema returned null for schema=" + sddUri + "; skipping.");
            return;
        }

        List<String> subs = new ArrayList<String>();
        logger.println("PVPostGenerator: Retrieved codes [" + codes.size() + "]");
        for (PossibleValue code : codes) {
            if (code == null) {
                continue;
            }
            if (code.getHasOtherFor() != null && !code.getHasOtherFor().isEmpty()) {
                subs.clear();
                // Defensive check: ensure code.getIsPossibleValueOf() is not null
                if (code.getIsPossibleValueOf() == null || code.getIsPossibleValueOf().isEmpty()) {
                    logger.println("[WARN] PVPostGenerator: code.getIsPossibleValueOf() is null or empty for code=" + code.getUri() + "; skipping.");
                    continue;
                }
                List<PossibleValue> variableCodes = PossibleValue.findByVariable(code.getIsPossibleValueOf());
                if (variableCodes == null) {
                    continue;
                }
                for (PossibleValue vc : variableCodes) {
                    if (vc == null) {
                        continue;
                    }
                    if (vc.getHasClass() != null && !vc.getHasClass().isEmpty() && (vc.getHasOtherFor() == null || vc.getHasOtherFor().isEmpty())) {
                        if (!subs.contains(vc.getHasClass())) {
                            subs.add(vc.getHasClass());
                        }

                        // update the class inside vc as a subclass of super
                        generateOtherOther(code.getHasOtherFor(), vc, sddUri);
                        logger.println("        - added " + vc.getHasClass() + " as a subclass of " + code.getHasOtherFor());
                    }
                }
                try {
                    Collections.sort(subs);
                    String shaString = "Super=" + code.getHasOtherFor() + "|Sub=";
                    for (String sub : subs) {
                        shaString = shaString + sub;
                    }
                    String shaHash = hashWith256(shaString);
                    if (shaHash == null || shaHash.length() < 5) {
                        logger.println("[WARN] PVPostGenerator: sha-256 returned invalid hash; skipping 'other' class generation for super=" + code.getHasOtherFor());
                        continue;
                    }
                    String harmonizedCodeHex = shaHash.substring(0, 5);
                    String harmonizedCode = String.valueOf(Integer.parseInt(harmonizedCodeHex, 16));
                    String newUri = URIUtils.replacePrefixEx(kbPrefix + shaHash);

                    // generate the 'other' class
                    generateOther(newUri, harmonizedCode, code, sddUri);
                    logger.println("        - created 'other' class " + newUri + " as a subclass of " + code.getHasOtherFor());

                    // associate the new 'other' class to the codebook element for the class
                    code.setHasClass(newUri);
                    code.setNamedGraph(sddUri);
                    code.save();
                }
                // For specifying wrong message digest algorithms
                catch (Exception e) {
                    logger.println("[ERROR] PVPostGenerator: error while generating sha-256 based 'other' class: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}

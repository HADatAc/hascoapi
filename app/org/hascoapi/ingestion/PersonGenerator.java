package org.hascoapi.ingestion;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.HADatAcThing;
import org.hascoapi.entity.pojo.Organization;
import org.hascoapi.entity.pojo.PostalAddress;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.NameSpace;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.SCHEMA;
import org.hascoapi.vocabularies.VSTOI;
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
import java.util.Random;
import java.util.Date;
import java.util.Collections;
import java.util.Base64;
import java.security.MessageDigest;  
import java.security.NoSuchAlgorithmException;  

public class PersonGenerator extends BaseGenerator {

	private static final Logger log = LoggerFactory.getLogger(PVGenerator.class);
    private long timestamp;
	private String managerEmail;
	private String status;
	final String kbPrefix = ConfigProp.getKbPrefix();
	String startTime = "";
    protected IngestionLogger logger = null;

	public PersonGenerator(DataFile dataFile, String status, String templateFile, String managerEmail) {
		super(dataFile, null, templateFile);
		this.status = status;
		this.logger = dataFile.getLogger();
		this.managerEmail = managerEmail;
	}

	@Override
	public void initMapping() {

		System.out.println("Inside PersonGenerator");

		// Get the current timestamp (in milliseconds)
        timestamp = System.currentTimeMillis();

		// Create mapping
		mapCol.clear();
		mapCol.put("OriginalID", templates.getAgentOriginalID());
		mapCol.put("Type", templates.getAgentType());
		mapCol.put("GivenName", templates.getAgentGivenName());
		mapCol.put("FamilyName", templates.getAgentFamilyName());
		mapCol.put("Email", templates.getAgentEmail());
		mapCol.put("Telephone", templates.getAgentTelephone());
		mapCol.put("Url", templates.getAgentUrl());
		mapCol.put("JobTitle", templates.getAgentJobTitle());
		mapCol.put("Affiliation", templates.getAgentHasAffiliationUri());
		mapCol.put("Address", templates.getAgentAddress());
		mapCol.put("UserName", templates.getAgentUserName());
		mapCol.put("UserEmail", templates.getAgentUserEmail());
		mapCol.put("UserID", templates.getAgentUserID());
	}

    private String getOriginalID(Record rec) {
		return rec.getValueByColumnName(mapCol.get("OriginalID"));
	}

	private String getType(Record rec) {
		String cls = rec.getValueByColumnName(mapCol.get("Type"));
		if (cls.length() > 0) {
			if (URIUtils.isValidURI(cls)) {
				return cls;
			} else {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00049",cls);
				//System.out.println("[WARNING] The following URI is considered invalid: " + cls);
			}
		} 
		return "";
	}

	private String getGivenName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("GivenName"));
	}

	private String getFamilyName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("FamilyName"));
	}

	private String getEmail(Record rec) {
		return rec.getValueByColumnName(mapCol.get("Email"));
	}

	private String getTelephone(Record rec) {
		return rec.getValueByColumnName(mapCol.get("Telephone"));
	}

	private String getUrl(Record rec) {
		return rec.getValueByColumnName(mapCol.get("Url"));
	}

	private String getJobTitle(Record rec) {
		return rec.getValueByColumnName(mapCol.get("JobTitle"));
	}

	private String getHasAffiliationUri(Record rec) {
		String orgName = rec.getValueByColumnName(mapCol.get("Affiliation"));
		Organization organization = Organization.findByName(orgName);
		if (organization != null && organization.getUri() != null) {
			return organization.getUri();
		}
		return "";
	}

	private String getAddress(Record rec) {
		String addressKey = rec.getValueByColumnName(mapCol.get("Address"));
		if (addressKey != null && !addressKey.isEmpty()) {
			String[] parts = addressKey.split("\\|", 2);
			PostalAddress postalAddress = PostalAddress.findByAddress(parts[0], parts[1]);
			if (postalAddress != null && postalAddress.getUri() != null) {
				return postalAddress.getUri();
			}
		}
		return "";
	}

	private String getUserName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("UserName"));
	}

	private String getUserEmail(Record rec) {
		return rec.getValueByColumnName(mapCol.get("UserEmail"));
	}

	private String getUserID(Record rec) {
		return rec.getValueByColumnName(mapCol.get("UserID"));
	}

	public String createPersonUri() throws Exception {

        // Generate a random integer between 10000 and 99999
        Random random = new Random();
        int randomNumber = random.nextInt(99999 - 10000 + 1) + 10000;

		return kbPrefix + "/" + Constants.PREFIX_PERSON + timestamp + randomNumber;
	}

	@Override
	public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {	
		Map<String, Object> row = new HashMap<String, Object>();
		row.put("hasURI", createPersonUri());
		row.put("hasco:hasOriginalID", URIUtils.replaceNameSpaceEx(getOriginalID(rec)));
		row.put("hasco:hascoType", SCHEMA.PERSON);
		row.put("a", URIUtils.replaceNameSpaceEx(getType(rec)));		
		row.put("rdfs:label", getGivenName(rec) + " " + getFamilyName(rec));
		row.put("foaf:name", getGivenName(rec) + " " + getFamilyName(rec));
		row.put("foaf:givenName", getGivenName(rec));
		row.put("foaf:familyName", getFamilyName(rec));
		row.put("foaf:mbox", getEmail(rec));
		row.put("schema:telephone", getTelephone(rec));
		row.put("schema:url", getUrl(rec));
		row.put("schema:jobTitle", getJobTitle(rec));
		row.put("foaf:member", getHasAffiliationUri(rec));
		row.put("schema:address", getAddress(rec));
		row.put("hasco:userName", getUserName(rec));
		row.put("hasco:userEmail", getUserEmail(rec));
		row.put("hasco:userID", getUserID(rec));
		row.put("vstoi:hasStatus", URIUtils.replaceNameSpaceEx(status));
		row.put("vstoi:hasSIRManagerEmail", managerEmail);
		row.put("vstoi:hasStatus", status);
		return row;
	}

	@Override
	public String getTableName() {
		return "Person";
	}

	@Override
	public String getErrorMsg(Exception e) {
		return "Error in PersonGenerator: " + e.getMessage();
	}

	/**
	 * Count the number of valid Person records in the KGR metadata template.
	 * Excludes header rows and any invalid/duplicate rows.
	 */
	private int countValidInputRecords() {
		if (records == null || records.isEmpty()) {
			return 0;
		}

		int validCount = 0;
		Record lastRecord = null;
		
		for (Record record : records) {
			// Skip empty records
			if (record.size() <= 0) {
				continue;
			}
			
			// Skip duplicate records
			if (lastRecord != null && record.equals(lastRecord)) {
				continue;
			}
			
			// Check if this is a valid Person record (has required fields)
			String givenName = getGivenName(record);
			String familyName = getFamilyName(record);
			
			// A valid Person must have at least a name
			if ((givenName != null && !givenName.trim().isEmpty()) || 
			    (familyName != null && !familyName.trim().isEmpty())) {
				validCount++;
				lastRecord = record;
			}
		}
		
		return validCount;
	}

	/**
	 * Verify that all Person records from the KGR metadata template were successfully ingested.
	 * Logs a warning if there's a mismatch between input records and committed objects.
	 */
	private void verifyIngestionCompleteness() {
		int inputRecordCount = countValidInputRecords();
		int committedObjectCount = 0;
		
		// Count successfully committed Person objects
		for (HADatAcThing obj : objects) {
			if (obj != null && "Person".equals(obj.getClass().getSimpleName())) {
				committedObjectCount++;
			}
		}
		
		logger.println(String.format("[PersonGenerator] Ingestion verification: %d Person record(s) in KGR template, %d Person(s) committed to knowledge graph", 
			inputRecordCount, committedObjectCount));
		
		if (committedObjectCount < inputRecordCount) {
			int missingCount = inputRecordCount - committedObjectCount;
			logger.println(String.format("[WARNING] PersonGenerator: Not all Person records were successfully ingested! %d Person record(s) from the KGR metadata template are missing in the knowledge graph.", 
				missingCount));
			logger.println("[WARNING] PersonGenerator: Please review the ingestion logs for errors. Some Person records may have been skipped due to validation failures or duplicate emails.");
		} else if (committedObjectCount > inputRecordCount) {
			logger.println("[INFO] PersonGenerator: More Person objects were committed than input records. This may occur if records were deduplicated or processed differently.");
		} else {
			logger.println(String.format("[SUCCESS] PersonGenerator: All %d Person record(s) from the KGR metadata template were successfully verified and ingested into the knowledge graph.", 
				committedObjectCount));
		}
	}

	@Override
	public boolean commitObjectsToTripleStore(List<HADatAcThing> objects) {
		// Call parent implementation to actually commit the objects
		boolean success = super.commitObjectsToTripleStore(objects);
		
		// Perform ingestion completeness verification
		if (success) {
			verifyIngestionCompleteness();
		} else {
			logger.println("[ERROR] PersonGenerator: Ingestion failed during commit to triple store. Verification skipped.");
		}
		
		return success;
	}
 	 
}

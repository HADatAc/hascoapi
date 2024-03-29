package org.hascoapi.ingestion;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.Templates;

public class AgentGenerator extends BaseGenerator {

	final String kbPrefix = ConfigProp.getKbPrefix();
	private int counter = 1; //starting index number
	private Study study;

	public AgentGenerator(Study study, DataFile dataFile, String templateFile) {
		super(dataFile, null, templateFile);
		this.study = study;
	}
	
	@Override
	public void initMapping() {
		mapCol.clear();
        mapCol.put("studyID", templates.getSTUDYID());
		mapCol.put("studyTitle", templates.getSTUDYTITLE());
		mapCol.put("studyAims", templates.getSTUDYAIMS());
		mapCol.put("studySignificance", templates.getSTUDYSIGNIFICANCE());
		mapCol.put("numSubjects", templates.getNUMSUBJECTS());
		mapCol.put("numSamples", templates.getNUMSAMPLES());
		mapCol.put("institution", templates.getINSTITUTION());
		mapCol.put("PI", templates.getPI());
		mapCol.put("PIAddress", templates.getPIADDRESS());
		mapCol.put("PICity", templates.getPICITY());
		mapCol.put("PIState", templates.getPISTATE());
		mapCol.put("PIZipCode", templates.getPIZIPCODE());
		mapCol.put("PIEmail", templates.getPIEMAIL());
		mapCol.put("PIPhone", templates.getPIPHONE());
		mapCol.put("CPI1FName", templates.getCPI1FNAME());
		mapCol.put("CPI1LName", templates.getCPI1LNAME());
		mapCol.put("CPI1Email", templates.getCPI1EMAIL());
		mapCol.put("CPI2FName", templates.getCPI2FNAME());
		mapCol.put("CPI2LName", templates.getCPI2LNAME());
		mapCol.put("CPI2Email", templates.getCPI2EMAIL());
		mapCol.put("contactFName", templates.getCONTACTFNAME());
		mapCol.put("contactLName", templates.getCONTACTLNAME());
		mapCol.put("contactEmail", templates.getCONTACTEMAIL());
		mapCol.put("createdDate", templates.getCREATEDDATE());
		mapCol.put("updatedDate", templates.getUPDATEDDATE());
		mapCol.put("DCAccessBool", templates.getDCACCESSBOOL());
	}
	
	private String getInstitutionUri(Record rec) {
		return kbPrefix + "ORG-" + rec.getValueByColumnName(mapCol.get("institution")).replaceAll(" ", "-").replaceAll(",", "").replaceAll("'", "");
	}
	
	private String getInstitutionName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("institution"));
	}
	
	private String getPIUri(Record rec) {
		return kbPrefix + "PER-" + rec.getValueByColumnName(mapCol.get("PI")).replaceAll(" ", "-");
	}
	
	private String getPIFullName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("PI"));
	}
	
	private String getPIGivenName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("PI")).substring(0, getPIFullName(rec).indexOf(' '));
	}
	
	private String getPIFamilyName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("PI")).substring(getPIFullName(rec).indexOf(' ') + 1);
	}
	
	private String getPIMBox(Record rec) {
		return rec.getValueByColumnName(mapCol.get("PIEmail"));
	}
	
	private String getCPI1Uri(Record rec) {
		return kbPrefix + "PER-" + getCPI1FullName(rec).replaceAll(" ", "-");
	}
	
	private String getCPI1FullName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("CPI1FName")) + " " + rec.getValueByColumnName(mapCol.get("CPI1LName"));
	}
	
	private String getCPI1GivenName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("CPI1FName"));
	}
	
	private String getCPI1FamilyName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("CPI1LName"));
	}
	
	private String getCPI1MBox(Record rec) {
		return rec.getValueByColumnName(mapCol.get("CPI1Email"));
	}
    
	private String getCPI2Uri(Record rec) {
		return kbPrefix + "PER-" + getCPI2FullName(rec).replaceAll(" ", "-");
	}
	
	private String getCPI2FullName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("CPI2FName")) + " " + rec.getValueByColumnName(mapCol.get("CPI2LName"));
	}
	
	private String getCPI2GivenName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("CPI2FName"));
	}
	
	private String getCPI2FamilyName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("CPI2LName"));
	}
	
	private String getCPI2MBox(Record rec) {
		return rec.getValueByColumnName(mapCol.get("CPI2Email"));
	}
	
	private String getContactUri(Record rec) {
		return kbPrefix + "PER-" + getContactFullName(rec).replaceAll(" ", "-");
	}
	
	private String getContactFullName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("contactFName")) + " " + rec.getValueByColumnName(mapCol.get("contactLName"));
	}
	
	private String getContactGivenName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("contactFName"));
	}
	
	private String getContactFamilyName(Record rec) {
		return rec.getValueByColumnName(mapCol.get("contactLName"));
	}
	
	private String getContactMBox(Record rec) {
		return rec.getValueByColumnName(mapCol.get("contactEmail"));
	}
	
    public Map<String, Object> createPIRow(Record rec) {
    	Map<String, Object> row = new HashMap<String, Object>();
    	row.put("hasURI", getPIUri(rec));
    	row.put("a", "prov:Person");
		row.put("hasco:hascoType", "prov:Person");
    	row.put("foaf:name", getPIFullName(rec));
    	row.put("rdfs:comment", "PI from " + getInstitutionName(rec));
    	row.put("foaf:familyName", getPIFamilyName(rec));
    	row.put("foaf:givenName", getPIGivenName(rec));
    	row.put("foaf:mbox", getPIMBox(rec));
    	row.put("foaf:member", getInstitutionUri(rec));
        row.put("vstoi:hasSIRManagerEmail", study.getHasSIRManagerEmail());
    	counter++;
    	
    	return row;
    }
    
    public Map<String, Object> createCPI1Row(Record rec) {
    	Map<String, Object> row = new HashMap<String, Object>();
    	row.put("hasURI", getCPI1Uri(rec));
    	row.put("a", "prov:Person");
		row.put("hasco:hascoType", "prov:Person");
    	row.put("foaf:name", getCPI1FullName(rec));
    	row.put("rdfs:comment", "Co-PI from " + getInstitutionName(rec));
    	row.put("foaf:familyName", getCPI1FamilyName(rec));
    	row.put("foaf:givenName", getCPI1GivenName(rec));
    	row.put("foaf:mbox", getCPI1MBox(rec));
    	row.put("foaf:member", getInstitutionUri(rec));
        row.put("vstoi:hasSIRManagerEmail", study.getHasSIRManagerEmail());
    	counter++;
    	
    	return row;
    }

    public Map<String, Object> createCPI2Row(Record rec) {
    	Map<String, Object> row = new HashMap<String, Object>();
    	row.put("hasURI", getCPI2Uri(rec));
    	row.put("a", "prov:Person");
		row.put("hasco:hascoType", "prov:Person");
    	row.put("foaf:name", getCPI2FullName(rec));
    	row.put("rdfs:comment", "Co-PI from " + getInstitutionName(rec));
    	row.put("foaf:familyName", getCPI2FamilyName(rec));
    	row.put("foaf:givenName", getCPI2GivenName(rec));
    	row.put("foaf:mbox", getCPI2MBox(rec));
    	row.put("foaf:member", getInstitutionUri(rec));
        row.put("vstoi:hasSIRManagerEmail", study.getHasSIRManagerEmail());
    	counter++;
    	
    	return row;
    }

    public Map<String, Object> createContactRow(Record rec) {
    	Map<String, Object> row = new HashMap<String, Object>();
    	row.put("hasURI", getContactUri(rec));
    	row.put("a", "prov:Person");
		row.put("hasco:hascoType", "prov:Person");
    	row.put("foaf:name", getContactFullName(rec));
    	row.put("rdfs:comment", "Co-PI from " + getInstitutionName(rec));
    	row.put("foaf:familyName", getContactFamilyName(rec));
    	row.put("foaf:givenName", getContactGivenName(rec));
    	row.put("foaf:mbox", getContactMBox(rec));
    	row.put("foaf:member", getInstitutionUri(rec));
        row.put("vstoi:hasSIRManagerEmail", study.getHasSIRManagerEmail());
    	counter++;
    	
    	return row;
    }

    public Map<String, Object> createInstitutionRow(Record rec) {
    	Map<String, Object> row = new HashMap<String, Object>();
    	row.put("hasURI", getInstitutionUri(rec));
    	row.put("a", "prov:Organization");
		row.put("hasco:hascoType", "prov:Organization");
    	row.put("foaf:name", getInstitutionName(rec));
    	row.put("rdfs:comment", getInstitutionName(rec) + " Institution");
        row.put("vstoi:hasSIRManagerEmail", study.getHasSIRManagerEmail());
    	counter++;
    	
    	return row;
    }
    
	@Override    
    public void createRows() throws Exception {
		boolean duplicate=false;
    	rows.clear();
    	// Currently using an inefficient way to check if row already exists in the list of rows; This should be addressed in the future
    	for (Record record : records) {
    		if(getPIFullName(record) != null && getPIFullName(record).length() > 0) {
    			System.out.println("Creating PI Row:" + getPIFullName(record) + ":");
    			duplicate=false;
    			for (Map<String, Object> row : rows) {
    				if(row.get("hasURI").equals(getPIUri(record))) {
    					System.out.println("Found Duplicate: " + getPIUri(record));
    					duplicate=true;
    					break;
    				}
    			}
    			if(!duplicate){
        			System.out.println("Didn't Find Duplicate, adding PI " + getPIUri(record) + " row to list of rows.");
        			rows.add(createPIRow(record));
    			}
    		}
    		if(getCPI1GivenName(record) != null && getCPI1GivenName(record).length() > 0) {
    			System.out.println("Creating CPI1 Row:" + getCPI1GivenName(record) + ":");
    			duplicate=false;
    			for (Map<String, Object> row : rows){
        			//System.out.println("Comparing: " + getCPI1Uri(record));
        			//System.out.println("With: " + row.get("hasURI"));
    				if(row.get("hasURI").equals(getCPI1Uri(record))){
    					System.out.println("Found Duplicate: " + getCPI1Uri(record));
    					duplicate=true;
    					break;
    				}
    			}
    			if(!duplicate){
        			System.out.println("Didn't Find Duplicate, adding CPI1 " + getCPI1Uri(record) + " row to list of rows.");
        			rows.add(createCPI1Row(record));
    			}
    		}
    		if(getCPI2GivenName(record) != null && getCPI2GivenName(record).length() > 0) {
    			System.out.println("Creating CPI2 Row:" + getCPI2GivenName(record) + ":");
    			duplicate=false;
    			for (Map<String, Object> row : rows){
        			//System.out.println("Comparing: " + getCPI2Uri(record));
        			//System.out.println("With: " + row.get("hasURI"));
    				if(row.get("hasURI").equals(getCPI2Uri(record))){
    					System.out.println("Found Duplicate: " + getCPI2Uri(record));
    					duplicate=true;
    					break;
    				}
    			}
    			if(!duplicate){
    				System.out.println("Didn't Find Duplicate, adding CPI2 " + getCPI2Uri(record) + " row to list of rows.");
    				rows.add(createCPI2Row(record));
    			}
    		}
    		if(getContactGivenName(record) != null && getContactGivenName(record).length() > 0) {
    			System.out.println("Creating Contact Row:" + getContactGivenName(record) + ":");
    			duplicate=false;
    			for (Map<String, Object> row : rows){
        			//System.out.println("Comparing: " + getContactUri(record));
        			//System.out.println("With: " + row.get("hasURI"));
    				if(row.get("hasURI").equals(getContactUri(record))){
    					System.out.println("Found Duplicate: " + getContactUri(record));
    					duplicate=true;
    					break;
    				}
    			}
    			if(!duplicate){
    				System.out.println("Didn't Find Duplicate, adding Contact " + getContactUri(record) + " row to list of rows.");
    				rows.add(createContactRow(record));
    			}
    		}
    		if(getInstitutionName(record) != null && getInstitutionName(record).length() > 0) {
    			System.out.println("Creating Institution Row:" + getInstitutionName(record) + ":");
    			duplicate=false;
    			for (Map<String, Object> row : rows){
        			//System.out.println("Comparing: " + getInstitutionUri(record));
        			//System.out.println("With: " + row.get("hasURI"));
    				if(row.get("hasURI").equals(getInstitutionUri(record))){
    					System.out.println("Found Duplicate: " + getInstitutionUri(record));
    					duplicate=true;
    					break;
    				}
    			}
    			if(!duplicate){
    				System.out.println("Didn't Find Duplicate, adding Contact " + getInstitutionUri(record) + " row to list of rows.");
    				rows.add(createInstitutionRow(record));
    			}
    		}
    	}
    }

	@Override
	public String getTableName() {
		return "Agent";
	}
	
	@Override
    public String getErrorMsg(Exception e) {
        return "Error in AgentGenerator: " + e.getMessage();
    }
}
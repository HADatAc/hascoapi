package org.hascoapi.ingestion;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.vocabularies.SCHEMA;


public class KGRGenerator extends BaseGenerator {
    
	protected String hasStatus = "";

	public String getHasStatus() {
		return this.hasStatus;
	}
    
	public void setHasStatus(String hasStatus) {
		this.hasStatus = hasStatus;
	}

	public KGRGenerator(String elementType, String hasStatus, DataFile dataFile) {
		super(dataFile);
		this.setElementType(elementType);
		this.setHasStatus(hasStatus);
	}

	@Override
	public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
		Map<String, Object> row = new HashMap<String, Object>();
		
		for (String header : file.getHeaders()) {
		    if (!header.trim().isEmpty()) {
		        String value = rec.getValueByColumnName(header);
		        if (value != null && !value.isEmpty()) {
					//System.out.println("Header: [" + header + "] Value: [" + value + "]");
		            row.put(header, value);
		        }
		    }
		}
		if (this.getElementType().equals("fundingscheme")) {
			row.put("hasco:hascoType", SCHEMA.FUNDING_SCHEME);
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
			if (this.getHasStatus() != null && !this.getHasStatus().equals("_")) {
			    row.put("vstoi:hasStatus", this.getHasStatus());
			}
		} else if (this.getElementType().equals("project")) {
			row.put("hasco:hascoType",SCHEMA.PROJECT);
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
			if (this.getHasStatus() != null && !this.getHasStatus().equals("_")) {
			    row.put("vstoi:hasStatus", this.getHasStatus());
			}
		}

		if (row.containsKey("hasURI") && !row.get("hasURI").toString().trim().isEmpty()) {
		    return row;
		}
		
		return null;
	}

	@Override
	public String getTableName() {
		return "KGR";
	}

	@Override
	public String getErrorMsg(Exception e) {
		return "Error in KGRGenerator: " + e.getMessage();
	}

}

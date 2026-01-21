package org.hascoapi.ingestion;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.vocabularies.VSTOI;


public class DP2Generator extends BaseGenerator {
    
	public DP2Generator(String elementType, DataFile dataFile) {
		super(dataFile);
		this.setElementType(elementType);
	}

	@Override
	public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
		Map<String, Object> row = new HashMap<String, Object>();
		
		for (String header : file.getHeaders()) {
		    if (!header.trim().isEmpty()) {
		        String value = rec.getValueByColumnName(header);
		        if (value != null && !value.isEmpty()) {
		            row.put(header, value);
					if (this.getElementType().equals("deployment")) {
						System.out.println("Inside DP2Generator(deployment): header [" + header + "] Value [" + value + "]");
					}
				}
		    }
		}

		// Accept common alias: some spreadsheets use 'uri' instead of 'hasURI'
		if (!row.containsKey("hasURI")) {
			Object alt = row.get("uri");
			if (alt != null && !alt.toString().trim().isEmpty()) {
				row.put("hasURI", alt.toString().trim());
			}
		}

		// Deployments
		// Platforms
		// PlatformsInstances
		// FieldsOfView
		// InstrumentInstances
	    // ComponentInstances
		// SensingPerspective
		// MessageStream
		// MessageTopic

		if (this.getElementType().equals("deployment")) {
			row.put("hasco:hascoType", VSTOI.DEPLOYMENT);
			row.put("hasco:canUpdate", this.dataFile.getHasSIRManagerEmail());
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
		} else if (this.getElementType().equals("platform")) {
			row.put("hasco:hascoType", VSTOI.PLATFORM);
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
		} else if (this.getElementType().equals("platforminstance")) {
			row.put("hasco:hascoType", VSTOI.PLATFORM_INSTANCE);
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
		} else if (this.getElementType().equals("fieldofview")) {
			row.put("hasco:hascoType", VSTOI.FIELD_OF_VIEW);
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
		} else if (this.getElementType().equals("instrumentinstance")) {
			row.put("hasco:hascoType", VSTOI.INSTRUMENT_INSTANCE);
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
		} else if (this.getElementType().equals("componentinstance")) {
			row.put("hasco:hascoType", VSTOI.COMPONENT_INSTANCE);
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
		}

		if (row.containsKey("hasURI") && row.get("hasURI") != null && !row.get("hasURI").toString().trim().isEmpty()) {
		    return row;
		}

		// Make the failure mode visible in logs to prevent "0 rows" surprises.
		//System.out.println("[WARNING] DP2Generator(" + this.getElementType() + "): skipping row " + rowNumber + " because hasURI is missing/empty");

		return null;
	}

	@Override
	public String getTableName() {
		return "DP2";
	}

	@Override
	public String getErrorMsg(Exception e) {
		return "Error in DP2Generator: " + e.getMessage();
	}
}

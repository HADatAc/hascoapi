package org.hascoapi.ingestion;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.VSTOI;


public class DP2Generator extends BaseGenerator {

    private final String status;

	public DP2Generator(String elementType, DataFile dataFile) {
		this(elementType, dataFile, null);
	}

	public DP2Generator(String elementType, DataFile dataFile, String status) {
		super(dataFile);
		this.setElementType(elementType);
		this.status = status;
	}

	@Override
	public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
		Map<String, Object> row = new HashMap<String, Object>();

		for (String header : file.getHeaders()) {
		    if (!header.trim().isEmpty()) {
		        String value = rec.getValueByColumnName(header);
		        if (value != null && !value.isEmpty()) {
		            row.put(header, value);
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

		// Add universal scoping fields to every DP2 element row
		row.put("hasco:hasDataFile", this.dataFile.getUri());

		// - persist status: FIRST check if the Excel column has vstoi:hasStatus
		//   ONLY use parameter status as fallback if column is empty
		String statusFromColumn = rec.getValueByColumnName("vstoi:hasStatus");
		if (statusFromColumn != null && !statusFromColumn.trim().isEmpty()) {
			row.put("vstoi:hasStatus", URIUtils.replaceNameSpaceEx(statusFromColumn.trim()));
		} else if (this.status != null && !this.status.trim().isEmpty()) {
			row.put("vstoi:hasStatus", URIUtils.replaceNameSpaceEx(this.status.trim()));
		}

		// Ensure rdf:type is present for downstream retrieval (many SPARQL queries use rdf:type)
		if (!row.containsKey("a") || row.get("a") == null || row.get("a").toString().trim().isEmpty()) {
			if (this.getElementType().equals("deployment")) {
				row.put("a", VSTOI.DEPLOYMENT);
			} else if (this.getElementType().equals("platform")) {
				row.put("a", VSTOI.PLATFORM);
			} else if (this.getElementType().equals("platforminstance")) {
				row.put("a", VSTOI.PLATFORM_INSTANCE);
			} else if (this.getElementType().equals("fieldofview")) {
				row.put("a", VSTOI.FIELD_OF_VIEW);
			} else if (this.getElementType().equals("instrumentinstance")) {
				row.put("a", VSTOI.INSTRUMENT_INSTANCE);
			} else if (this.getElementType().equals("componentinstance")) {
				row.put("a", VSTOI.COMPONENT_INSTANCE);
			}
		}

		// Set element-specific metadata
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


		// Validate hasURI
		if (row.containsKey("hasURI") && row.get("hasURI") != null && !row.get("hasURI").toString().trim().isEmpty()) {
		    return row;
		}

		System.out.println("[DP2Generator] WARNING: Row #" + rowNumber + " missing hasURI - skipping");
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

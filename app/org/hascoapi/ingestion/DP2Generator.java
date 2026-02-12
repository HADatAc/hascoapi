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
		
		System.out.println("\n========== DP2Generator.createRow() START ==========");
		System.out.println("ElementType: [" + this.getElementType() + "]");
		System.out.println("RowNumber: " + rowNumber);
		System.out.println("Record size: " + rec.size());
		System.out.println("File headers: " + file.getHeaders());

		for (String header : file.getHeaders()) {
		    if (!header.trim().isEmpty()) {
		        String value = rec.getValueByColumnName(header);
		        System.out.println("  Processing header [" + header + "] -> value [" + value + "]");
		        if (value != null && !value.isEmpty()) {
		            row.put(header, value);
					System.out.println("    ✓ Added to row: [" + header + "] = [" + value + "]");
				} else {
					System.out.println("    ✗ Skipped (null or empty)");
				}
		    }
		}

		System.out.println("Row keys after extraction: " + row.keySet());
		System.out.println("Row hasURI before alias check: " + row.get("hasURI"));

		System.out.println("Row keys after extraction: " + row.keySet());
		System.out.println("Row hasURI before alias check: " + row.get("hasURI"));

		// Accept common alias: some spreadsheets use 'uri' instead of 'hasURI'
		if (!row.containsKey("hasURI")) {
			Object alt = row.get("uri");
			System.out.println("  hasURI not found, checking 'uri' alias: " + alt);
			if (alt != null && !alt.toString().trim().isEmpty()) {
				row.put("hasURI", alt.toString().trim());
				System.out.println("  ✓ Using 'uri' as hasURI: " + alt.toString().trim());
			}
		}

		System.out.println("Row hasURI after alias check: " + row.get("hasURI"));

		// Add universal scoping fields to every DP2 element row
		// - link elements back to their DataFile so DP2Gen can retrieve by hasco:hasDataFile
		row.put("hasco:hasDataFile", this.dataFile.getUri());
		System.out.println("Added hasco:hasDataFile: " + this.dataFile.getUri());

		// - persist status: FIRST check if the Excel column has vstoi:hasStatus
		//   ONLY use parameter status as fallback if column is empty
		String statusFromColumn = rec.getValueByColumnName("vstoi:hasStatus");
		if (statusFromColumn != null && !statusFromColumn.trim().isEmpty()) {
			row.put("vstoi:hasStatus", URIUtils.replaceNameSpaceEx(statusFromColumn.trim()));
			System.out.println("Added vstoi:hasStatus from Excel column: " + URIUtils.replaceNameSpaceEx(statusFromColumn.trim()));
		} else if (this.status != null && !this.status.trim().isEmpty()) {
			row.put("vstoi:hasStatus", URIUtils.replaceNameSpaceEx(this.status.trim()));
			System.out.println("Added vstoi:hasStatus from parameter (fallback): " + URIUtils.replaceNameSpaceEx(this.status.trim()));
		}

		// Ensure rdf:type is present for downstream retrieval (many SPARQL queries use rdf:type)
		// Prefer the spreadsheet-provided 'a' column; if missing, set a default per elementType.
		System.out.println("Checking rdf:type ('a' column): current value = " + row.get("a"));
		System.out.println("Checking rdf:type ('a' column): current value = " + row.get("a"));
		if (!row.containsKey("a") || row.get("a") == null || row.get("a").toString().trim().isEmpty()) {
			System.out.println("  'a' column is missing/empty, setting default based on elementType");
			if (this.getElementType().equals("deployment")) {
				row.put("a", VSTOI.DEPLOYMENT);
				System.out.println("  ✓ Set 'a' to VSTOI.DEPLOYMENT: " + VSTOI.DEPLOYMENT);
			} else if (this.getElementType().equals("platform")) {
				row.put("a", VSTOI.PLATFORM);
				System.out.println("  ✓ Set 'a' to VSTOI.PLATFORM: " + VSTOI.PLATFORM);
			} else if (this.getElementType().equals("platforminstance")) {
				row.put("a", VSTOI.PLATFORM_INSTANCE);
				System.out.println("  ✓ Set 'a' to VSTOI.PLATFORM_INSTANCE: " + VSTOI.PLATFORM_INSTANCE);
			} else if (this.getElementType().equals("fieldofview")) {
				row.put("a", VSTOI.FIELD_OF_VIEW);
				System.out.println("  ✓ Set 'a' to VSTOI.FIELD_OF_VIEW: " + VSTOI.FIELD_OF_VIEW);
			} else if (this.getElementType().equals("instrumentinstance")) {
				row.put("a", VSTOI.INSTRUMENT_INSTANCE);
				System.out.println("  ✓ Set 'a' to VSTOI.INSTRUMENT_INSTANCE: " + VSTOI.INSTRUMENT_INSTANCE);
			} else if (this.getElementType().equals("componentinstance")) {
				row.put("a", VSTOI.COMPONENT_INSTANCE);
				System.out.println("  ✓ Set 'a' to VSTOI.COMPONENT_INSTANCE: " + VSTOI.COMPONENT_INSTANCE);
			} else if (this.getElementType().equals("sensingperspective")) {
				System.out.println("  ℹ sensingperspective: no dedicated VSTOI constant");
				// no dedicated constant available in VSTOI; rely on spreadsheet value when present
			}
		} else {
			System.out.println("  'a' column already present: " + row.get("a"));
		}

		System.out.println("Setting element-specific metadata for elementType: " + this.getElementType());
		if (this.getElementType().equals("deployment")) {
			row.put("hasco:hascoType", VSTOI.DEPLOYMENT);
			row.put("hasco:canUpdate", this.dataFile.getHasSIRManagerEmail());
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
			System.out.println("  Deployment metadata added");
		} else if (this.getElementType().equals("platform")) {
			row.put("hasco:hascoType", VSTOI.PLATFORM);
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
			System.out.println("  Platform metadata added");
		} else if (this.getElementType().equals("platforminstance")) {
			row.put("hasco:hascoType", VSTOI.PLATFORM_INSTANCE);
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
			System.out.println("  PlatformInstance metadata added");
		} else if (this.getElementType().equals("fieldofview")) {
			row.put("hasco:hascoType", VSTOI.FIELD_OF_VIEW);
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
			System.out.println("  FieldOfView metadata added");
		} else if (this.getElementType().equals("instrumentinstance")) {
			row.put("hasco:hascoType", VSTOI.INSTRUMENT_INSTANCE);
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
			System.out.println("  InstrumentInstance metadata added");
		} else if (this.getElementType().equals("componentinstance")) {
			row.put("hasco:hascoType", VSTOI.COMPONENT_INSTANCE);
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
			System.out.println("  ComponentInstance metadata added");
		}

		System.out.println("Final row validation - checking hasURI:");
		System.out.println("  hasURI present: " + row.containsKey("hasURI"));
		System.out.println("  hasURI value: " + row.get("hasURI"));

		if (row.containsKey("hasURI") && row.get("hasURI") != null && !row.get("hasURI").toString().trim().isEmpty()) {
			System.out.println("✓ Row VALID - returning row with " + row.size() + " properties");
			System.out.println("  Final row keys: " + row.keySet());
			System.out.println("========== DP2Generator.createRow() END (VALID) ==========\n");
		    return row;
		}

		System.out.println("✗ Row INVALID - returning null (no hasURI)");
		System.out.println("========== DP2Generator.createRow() END (INVALID) ==========\n");
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

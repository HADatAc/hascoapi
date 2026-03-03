package org.hascoapi.ingestion;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.vocabularies.VSTOI;

import java.util.HashMap;
import java.util.Map;

public class WKFGenerator extends BaseGenerator {

    protected String wkfUri = "";
    protected String hasStatus = "";

    public String getWKFUri() {
        return wkfUri;
    }

    public void setWKFUri(String wkfUri) {
        this.wkfUri = wkfUri;
    }

    public String getHasStatus() {
        return hasStatus;
    }

    public void setHasStatus(String hasStatus) {
        this.hasStatus = hasStatus;
    }

    public WKFGenerator(String elementType, DataFile dataFile, String hasStatus) {
        super(dataFile);
        this.elementType = elementType;
        this.hasStatus = hasStatus;
    }

    @Override
    public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
        Map<String, Object> row = new HashMap<>();

        // Debug: Log what we're processing
        System.out.println("[WKFGenerator.createRow] Row #" + rowNumber + ", elementType=" + this.getElementType());

        // First, copy all data from the Excel record (like INSGenerator does)
        for (String header : file.getHeaders()) {
            if (!header.trim().isEmpty()) {
                String value = rec.getValueByColumnName(header);
                if (value != null && !value.isEmpty()) {
                    row.put(header, value);
                    if ("hasURI".equals(header)) {
                        System.out.println("[WKFGenerator.createRow] hasURI from Excel: " + value);
                    }
                }
            }
        }

        String elementType = this.getElementType();

        // Add common metadata to all rows
        if (elementType.equals("processstem")) {
            row.put("hasco:hascoType", VSTOI.PROCESS_STEM);
        } else if (elementType.equals("process")) {
            row.put("hasco:hascoType", VSTOI.PROCESS);
        } else if (elementType.equals("task")) {
            row.put("hasco:hascoType", VSTOI.TASK);
        } else if (elementType.equals("requiredinstrument")) {
            row.put("hasco:hascoType", VSTOI.REQUIRED_INSTRUMENT);
        } else {
            this.dataFile.getLogger().printExceptionByIdWithArgs("GEN_00001", elementType);
            return null;
        }

        // Add status from the generator context (only if not null or "_")
        if (this.hasStatus != null && !this.hasStatus.equals("_")) {
            row.put("vstoi:hasStatus", this.hasStatus);
        }

        // Add data file reference
        row.put("hasco:hasDataFile", this.dataFile.getUri());
        row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());

        // Debug: Show what will be committed
        if (row.containsKey("hasURI")) {
            System.out.println("[WKFGenerator.createRow] Final hasURI: " + row.get("hasURI"));
            System.out.println("[WKFGenerator.createRow] DataFile URI: " + this.dataFile.getUri());
            System.out.println("[WKFGenerator.createRow] Element type: " + elementType);
        }

        // CRITICAL: Only return row if it has a URI (like INSGenerator does)
        if (row.containsKey("hasURI") && !row.get("hasURI").toString().trim().isEmpty()) {
            return row;
        }

        return null;
    }

    @Override
    public String getTableName() {
        return "WKF-" + this.getElementType();
    }

    @Override
    public String getErrorMsg(Exception e) {
        return "Error in WKFGenerator for element type [" + this.getElementType() + "]: " + e.getMessage();
    }

    @Override
    public void preprocessuris(Map<String, String> uris) {
        // Process URIs if needed - currently no preprocessing required for WKF
    }
}

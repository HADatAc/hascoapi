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

        // Copy all data from the Excel record
        for (String header : file.getHeaders()) {
            if (!header.trim().isEmpty()) {
                String value = rec.getValueByColumnName(header);
                if (value != null && !value.isEmpty()) {
                    row.put(header, value);
                }
            }
        }

        String elementType = this.getElementType();

        // For Tasks: split concatenated multi-value properties into Lists
        if (elementType.equals("task")) {
            splitMultiValueProperty(row, "vstoi:hasRequiredInstrument");
            splitMultiValueProperty(row, "vstoi:hasSubtask");
        }

        // Add common metadata to all rows
        if (elementType.equals("processstem")) {
            row.put("hasco:hascoType", VSTOI.PROCESS_STEM);
        } else if (elementType.equals("process")) {
            row.put("hasco:hascoType", VSTOI.PROCESS);
        } else if (elementType.equals("task")) {
            row.put("hasco:hascoType", VSTOI.TASK);
        } else if (elementType.equals("requiredinstrument")) {
            row.put("hasco:hascoType", VSTOI.REQUIRED_INSTRUMENT);
            // For RequiredInstruments: split concatenated components
            splitMultiValueProperty(row, "vstoi:hasRequiredComponent");
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

        // Only return row if it has a URI
        if (row.containsKey("hasURI") && !row.get("hasURI").toString().trim().isEmpty()) {
            return row;
        }

        System.out.println("[WKFGenerator] WARNING: Row #" + rowNumber + " missing hasURI for elementType=" + elementType + " - skipping");
        return null;
    }

    /**
     * Split concatenated values (separated by ; or |) into a List for multi-value properties.
     * This ensures MetadataFactory creates multiple triples instead of one literal triple.
     */
    private void splitMultiValueProperty(Map<String, Object> row, String propertyKey) {
        Object value = row.get(propertyKey);
        if (value == null) {
            return;
        }

        String valueStr = value.toString().trim();
        if (valueStr.isEmpty()) {
            return;
        }

        // Check if value contains separators (semicolon or pipe)
        if (valueStr.contains(";") || valueStr.contains("|")) {
            java.util.List<String> uris = new java.util.ArrayList<>();
            
            // Split by semicolon or pipe, handling both separators
            String[] parts;
            if (valueStr.contains(";")) {
                parts = valueStr.split("\\s*;\\s*");
            } else {
                parts = valueStr.split("\\s*\\|\\s*");
            }

            for (String part : parts) {
                String cleanUri = part.trim();
                // Decode URL encoding (e.g., %20 -> space, then trim again)
                try {
                    cleanUri = java.net.URLDecoder.decode(cleanUri, "UTF-8").trim();
                } catch (Exception e) {
                    // If decoding fails, use the original value
                }
                
                if (!cleanUri.isEmpty()) {
                    uris.add(cleanUri);
                }
            }

            if (!uris.isEmpty()) {
                row.put(propertyKey, uris);
                System.out.println("[WKFGenerator] Split " + propertyKey + " into " + uris.size() + " values: " + uris);
            }
        }
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

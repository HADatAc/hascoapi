package org.hascoapi.ingestion;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.utils.ConfigProp;

import java.util.HashMap;
import java.util.Map;

/**
 * Generator for Timeline sheet in SDD files.
 *
 * Creates SDDObject entities with hasco:TimeRole for explicit timeline definitions.
 * These are different from virtual time objects (??instant, ??observation) which
 * are handled in Dictionary Mapping.
 *
 * Timeline columns: Name, Label, Type, Start, End, Unit
 */
public class TimelineGenerator extends BaseGenerator {

    final String kbPrefix = ConfigProp.getKbPrefix();
    String sddUri = "";
    String sddName = "";
    String managerEmail = "";

    public TimelineGenerator(DataFile dataFile, String sddUri, String sddName) {
        super(dataFile);
        this.sddUri = sddUri;
        this.sddName = sddName;
        this.managerEmail = dataFile.getHasSIRManagerEmail();
    }

    @Override
    public void initMapping() {
        mapCol.clear();
        mapCol.put("Name", "Name");
        mapCol.put("Label", "Label");
        mapCol.put("Type", "Type");
        mapCol.put("Start", "Start");
        mapCol.put("End", "End");
        mapCol.put("Unit", "Unit");
    }

    private String getName(Record rec) {
        return rec.getValueByColumnName(mapCol.get("Name"));
    }

    private String getLabel(Record rec) {
        String label = rec.getValueByColumnName(mapCol.get("Label"));
        return (label != null && !label.isEmpty()) ? label : getName(rec);
    }

    private String getType(Record rec) {
        return rec.getValueByColumnName(mapCol.get("Type"));
    }

    private String getStart(Record rec) {
        return rec.getValueByColumnName(mapCol.get("Start"));
    }

    private String getEnd(Record rec) {
        return rec.getValueByColumnName(mapCol.get("End"));
    }

    private String getUnit(Record rec) {
        return rec.getValueByColumnName(mapCol.get("Unit"));
    }

    @Override
    public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
        String name = getName(rec);

        // Skip if no name provided
        if (name == null || name.isEmpty()) {
            return null;
        }

        Map<String, Object> row = new HashMap<String, Object>();

        // Create URI for this timeline object
        String timelineObjUri = sddUri.replace("SDDICT", "SDDOBJ") + "/timeline/" + String.valueOf(rowNumber);

        row.put("hasURI", timelineObjUri);
        row.put("a", "hasco:SDDObject");
        row.put("hasco:hascoType", "hasco:SDDObject");
        row.put("rdfs:label", name);  // Name column
        row.put("rdfs:comment", getLabel(rec));  // Label column (human-readable description)
        row.put("hasco:partOfSchema", sddUri);
        row.put("hasco:listPosition", String.valueOf(rowNumber));

        // Set Entity from Type column
        String type = getType(rec);
        if (type != null && !type.isEmpty()) {
            row.put("hasco:hasEntity", type);
        }

        // CRITICAL: Mark this as a Timeline object with TimeRole
        row.put("hasco:hasRole", "hasco:TimeRole");

        // Store temporal properties (Start, End, Unit) as additional metadata
        String start = getStart(rec);
        if (start != null && !start.isEmpty()) {
            row.put("hasco:hasStart", start);
            logger.println("[TimelineGenerator]   hasStart: " + start);
        }

        String end = getEnd(rec);
        if (end != null && !end.isEmpty()) {
            row.put("hasco:hasEnd", end);
            logger.println("[TimelineGenerator]   hasEnd: " + end);
        }

        String unit = getUnit(rec);
        if (unit != null && !unit.isEmpty()) {
            row.put("hasco:hasUnit", unit);
            logger.println("[TimelineGenerator]   hasUnit: " + unit);
        }

        row.put("vstoi:hasSIRManagerEmail", managerEmail);

        logger.println("[TimelineGenerator] Created Timeline object: " + name + " with TimeRole");

        return row;
    }

    @Override
    public String getTableName() {
        return "TimelineObject";
    }

    @Override
    public String getErrorMsg(Exception e) {
        return "Error in TimelineGenerator: " + e.getMessage();
    }
}


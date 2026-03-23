package org.hascoapi.ingestion;

import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.utils.*;
import org.hascoapi.utils.MetadataFactory;
import org.hascoapi.utils.CollectionUtil;
// import org.hascoapi.utils.Templates; // No longer needed - inherited from BaseGenerator

public class SDDAttributeGenerator extends BaseGenerator {

    final String kbPrefix = ConfigProp.getKbPrefix();
    String startTime = "";
    String sddName = "";
    String sddUri = "";
    String filename = "";
    String managerEmail = "";
    // Templates templates; // REMOVED: This was shadowing BaseGenerator's templates field, causing null in initMapping()
    Map<String, String> codeMap;
    Map<String, List<String>> hasEntityMap = new HashMap<String, List<String>>();
    Map<String, List<String>> mergedEA = new HashMap<String, List<String>>();
    Map<String, List<String>> mergedAA = new HashMap<String, List<String>>();
    List<String> AttrList = new ArrayList<String>();
    Map<String, String> currentHasEntity = new HashMap<String, String>();

    // Map of column label -> SDDAttribute URI (created during createRows)
    // This will be used by PVGenerator to link PossibleValues to SDDAttributes
    private Map<String, String> labelToUriMap = new HashMap<String, String>();

    public SDDAttributeGenerator(DataFile dataFile, String sddUri, String sddName, Map<String, String> codeMap, List<Map<String, List<String>>> merging, String templateFile) {
        // Ensure BaseGenerator creates Templates with the same templateFile so initMapping() is safe.
        super(dataFile, null, templateFile);
        //System.out.println("SDDAttributeGenerator (START)");
        System.out.println("SDDAttributeGenerator: SDDUri = [" + sddUri + "]");
        this.codeMap = codeMap;
        this.sddName = sddName;
        this.sddUri = sddUri;
        this.managerEmail = dataFile.getHasSIRManagerEmail();
        this.mergedEA = merging.get(0);
        this.mergedAA = merging.get(1);
        this.fileName = dataFile.getFilename();
        // this.templates = new Templates(templateFile); // REMOVED: BaseGenerator already initialized templates
        logger.println("[Merged Attributes] : " + mergedEA.keySet());
        logger.println("[Derived Attributes] : " + mergedAA.keySet());

        // BaseGenerator already called initMapping() in its constructor; calling it again is redundant and
        // can hide ordering bugs if templates are not ready.

        for (Record rec : file.getRecords()) {
            List<String> tmp = new ArrayList<String>();
            tmp.add(rec.getValueByColumnName(mapCol.get("VariableOf")));
            tmp.add(rec.getValueByColumnName(mapCol.get("Entity")));
            tmp.add(rec.getValueByColumnName(mapCol.get("InRelationTo")));
            hasEntityMap.put(rec.getValueByColumnName(mapCol.get("Label")), tmp);
            if (rec.getValueByColumnName(mapCol.get("AttributeType")) != null && rec.getValueByColumnName(mapCol.get("AttributeType")).length() > 0) {
                AttrList.add(rec.getValueByColumnName(mapCol.get("Label")));
            }
        }
    }

    //Column	Attribute	attributeOf	Unit	Time	Entity	Role	Relation	inRelationTo	wasDerivedFrom	wasGeneratedBy	hasPosition	
    @Override
    public void initMapping() {
        try {
            // BaseGenerator's constructor already initialized templates from templateFile before calling initMapping()
            // So we should use that templates field directly
            if (templates == null) {
                System.out.println("[ERROR] SDDAttributeGenerator.initMapping(): Templates is null after BaseGenerator initialization");
                return;
            }

            mapCol.clear();
            mapCol.put("Label", templates.getLABEL());
            mapCol.put("AttributeType", templates.getATTRIBUTETYPE());
            mapCol.put("VariableOf", templates.getATTTRIBUTEOF());
            mapCol.put("Unit", templates.getUNIT());
            mapCol.put("Time", templates.getTIME());
            mapCol.put("Entity", templates.getENTITY());
            mapCol.put("Role", templates.getROLE());
            mapCol.put("Relation", templates.getRELATION());
            mapCol.put("InRelationTo", templates.getINRELATIONTO());
            mapCol.put("WasDerivedFrom", templates.getWASDERIVEDFROM());       
            mapCol.put("WasGeneratedBy", templates.getWASGENERATEDBY());

            // Guard against null template values to prevent NPEs later when calling .trim()
            for (Map.Entry<String, String> entry : new HashMap<>(mapCol).entrySet()) {
                if (entry.getValue() == null) {
                    mapCol.put(entry.getKey(), "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getLabel(Record rec) {
        String value = rec.getValueByColumnName(mapCol.get("Label"));
        return (value == null) ? "" : value.trim();
    }

    private String getAttribute(Record rec) {
        String value = rec.getValueByColumnName(mapCol.get("AttributeType"));
        return (value == null) ? "" : value.trim();
    }

    private String getVariableOf(Record rec) {
        return rec.getValueByColumnName(mapCol.get("VariableOf").trim());
    }
 
    private String getVariableOf(String str) {
        if (str == null || str.equals("")) {
            return "";
        }
        return str.trim();
    }

    private String getUnit(Record rec) {
        String original = rec.getValueByColumnName(mapCol.get("Unit"));
        if (original == null || original.isEmpty()) {
            return "";
        }
        // Trim whitespace including newlines to prevent malformed URIs
        original = original.trim();

        // First check if it's already a valid URI
        if (URIUtils.isValidURI(original)) {
            // Convert abbreviated URI to full URI
            String fullUri = URIUtils.convertToWholeURI(original);
            // Validate the full URI is well-formed
            if (URIUtils.isWellFormedURI(fullUri)) {
                return original;
            } else {
                System.out.println("[WARNING] SDDAttributeGenerator.getUnit(): Malformed URI after conversion: " + fullUri);
                System.out.println("[WARNING]   Original value: '" + original + "'");
                return "";
            }
        } else if (codeMap.containsKey(original)) {
            // It's a code that needs to be looked up
            String mappedUri = codeMap.get(original);
            // Validate the mapped URI is well-formed
            String fullUri = URIUtils.convertToWholeURI(mappedUri);
            if (URIUtils.isWellFormedURI(fullUri)) {
                return codeMap.get(original);
            } else {
                System.out.println("[WARNING] SDDAttributeGenerator.getUnit(): Malformed URI in codeMap: " + fullUri);
                System.out.println("[WARNING]   Original value: '" + original + "', mapped to: '" + mappedUri + "'");
                return "";
            }
        }

        return "";
    }

    private String getTime(Record rec) {
        String value = rec.getValueByColumnName(mapCol.get("Time"));
        return (value == null) ? "" : value.trim();
    }

    private String getEntity(Record rec) {
        String daso = rec.getValueByColumnName(mapCol.get("VariableOf"));
        if (daso.equals("")) {
            currentHasEntity.put(getLabel(rec), "hasco:unknownEntity");
            return "hasco:unknownEntity";
        } else {
            if (codeMap.containsKey(hasEntityMap.get(daso))) {
                currentHasEntity.put(getLabel(rec), codeMap.get(hasEntityMap.get(daso)));
                return codeMap.get(hasEntityMap.get(daso));
            } else {
                if (hasEntityMap.containsKey(daso)) {
                    if(codeMap.containsKey(hasEntityMap.get(daso).get(1))) {
                        currentHasEntity.put(getLabel(rec), codeMap.get(hasEntityMap.get(daso).get(1)));
                        return codeMap.get(hasEntityMap.get(daso).get(1));
                    }
                    currentHasEntity.put(getLabel(rec), hasEntityMap.get(daso).get(1));
                    return hasEntityMap.get(daso).get(1);
                }
                currentHasEntity.put(getLabel(rec), "hasco:unknownEntity");
                return "hasco:unknownEntity";
            }
        }
    }

    private String getRelation(Record rec) {
        String value = rec.getValueByColumnName(mapCol.get("Relation"));
        return (value == null) ? "" : value.trim();
    }

    private String getInRelationTo(Record rec) {
        String value = rec.getValueByColumnName(mapCol.get("InRelationTo"));
        return (value == null) ? "" : value.trim();
        /*
        if (inRelationTo.length() == 0) {
            return "";
        } else {
            List<String> items = new ArrayList<String>();
            for (String item : Arrays.asList(inRelationTo.split("\\s*,\\s*"))) {
                items.add(kbPrefix + "SDDO-" + sddName + "-" + item.replace(" ", "").replace("_","-").replace("??", ""));
            }
            return items.get(0);
        }
        */
    }

    private String getWasDerivedFrom(Record rec) {
        String value = rec.getValueByColumnName(mapCol.get("WasDerivedFrom"));
        return (value == null) ? "" : value.trim();
    }

    private List<String> getListWasDerivedFrom(Record rec) {
        String derivedFrom = rec.getValueByColumnName(mapCol.get("WasDerivedFrom"));
        if (derivedFrom == null) {
            return new ArrayList<String>();
        }
        derivedFrom = derivedFrom.trim();
        List<String> list = new ArrayList<String>();
        if (derivedFrom.length() == 0) {
            return list;
        } else {
            List<String> items = Arrays.asList(derivedFrom.split("\\s*,\\s*"));
            for (String item : items) {
                if (AttrList.contains(item)) {
                    list.add(item);
                    //list.add(kbPrefix + "SDDA-" + sddName + "-" + item.replace(" ", "").replace("_","-").replace("??", ""));
                }
            }
            return list;
        }
    }

    public boolean checkCellUriRegistered(String str) {
        String prefixString = NameSpaces.getInstance().printSparqlNameSpaceList();
        //        System.out.println(prefixString);
        if (str.contains(":")){
            String[] split = str.split(":");
            String prefixname = split[0];
            if (!prefixString.contains(prefixname)){
                return false;
            }
            return true;
        } else {
            return true;
        }
    }

    public boolean checkCellUriResolvable(String str) {

        if (str.contains(":")){
            if (URIUtils.isValidURI(str)){
                try {
                    URIUtils.convertToWholeURI(str);
                } catch (Exception e) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private String getWasGeneratedBy(Record rec) {

        String str = rec.getValueByColumnName(mapCol.get("WasGeneratedBy"));
        if (str == null || str.trim().length() == 0) {
            return "";
        }
        str = str.trim();
        if (checkCellUriRegistered(str)) {
            if (checkCellUriResolvable(str)) {
                return str;
            }
        } else if (AttrList.contains(str)) {
            return kbPrefix + "SDDA-" + sddName + "-" + str.replace(" ", "").replace("_","-").replace("??", "");
        } else {
            return "";
        }

        return "";
    }

    private Boolean checkVirtual(Record rec) {
        if (getLabel(rec).contains("??")){
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void createRows() throws Exception {

        Map<String, List<String>> mergedEA = this.mergedEA;
        rows.clear();
        List<String> column_name = new ArrayList<String>();
        int rowNumber = 0;

        System.out.println("[SDDAttributeGenerator] createRows() START - total records: " + records.size());
        System.out.println("[SDDAttributeGenerator] Template mapping for AttributeType: '" + mapCol.get("AttributeType") + "'");

        for (Record record : records) {

            String attr = getAttribute(record);
            String label = getLabel(record);

            System.out.println("[SDDAttributeGenerator] Processing record #" + (rowNumber + 1) + ":");
            System.out.println("[SDDAttributeGenerator]   label='" + label + "'");
            System.out.println("[SDDAttributeGenerator]   attribute (from column '" + mapCol.get("AttributeType") + "')='" + attr + "'");
            System.out.println("[SDDAttributeGenerator]   attribute is null: " + (attr == null));
            System.out.println("[SDDAttributeGenerator]   attribute is empty: " + (attr != null && attr.equals("")));

            if ( attr  == null || attr.equals("")){
                System.out.println("[SDDAttributeGenerator]   -> SKIPPED (empty attribute)");
                if (column_name.contains(getLabel(record))){
                    System.out.println("[SDDAttributeGenerator]   -> Creating relation row instead");
                    rows.add(createRelationRow(record, ++rowNumber));
                }
                continue;
            } else {
                System.out.println("[SDDAttributeGenerator]   -> Creating SDDAttribute row");
                Map<String, Object> newRow = createRow(record, ++rowNumber);
                System.out.println("[SDDAttributeGenerator]   -> Row created with URI: " + newRow.get("hasURI"));
                rows.add(newRow);
                System.out.println("[SDDAttributeGenerator]   -> Rows list now has " + rows.size() + " rows");

                // Store the mapping of column label -> SDDAttribute URI for PVGenerator
                String columnLabel = getLabel(record);
                String sddAttUri = (String) newRow.get("hasURI");
                if (columnLabel != null && !columnLabel.isEmpty() && sddAttUri != null) {
                    labelToUriMap.put(columnLabel, sddAttUri);
                    System.out.println("[SDDAttributeGenerator]   -> Stored label->URI mapping: '" + columnLabel + "' -> '" + sddAttUri + "'");
                }

                //for (String item : getWasDerivedFrom(record)) {
                //    rows.add(createDerivedFromRow(item, record));
                //}
                column_name.add(getLabel(record));
            }
        }

        System.out.println("[SDDAttributeGenerator] createRows() END - total rows created: " + rows.size());

        /*if (mergedEA != null && mergedEA.keySet().size() > 0) {
            for (String attr : mergedEA.keySet()) {
                if (attr.length() > 0) {
                    rows.add(createMergeEAAttrRow(attr, mergedEA));
                }
            }
        }
        if (mergedAA != null && mergedAA.keySet().size() > 0) {
            for (String attr : mergedAA.keySet()) {
                if (attr.length() > 0) {
                    rows.add(createMergeAAAttrRow(attr, mergedAA));
                }
            }
        }*/
    }

    /* 
    public List<String> createUris() throws Exception {
        List<String> result = new ArrayList<String>();
        for (Record record : records) {
            if (getAttribute(record)  == null || getAttribute(record).equals("")){
                continue;
            } else {
                result.add(kbPrefix + "SDDA-" + sddName + "-" + getLabel(record).trim().replace(" ", "").replace("_","-").replace("??", ""));
            }
        }
        return result;
    }
    */

    //Column	Attribute	attributeOf	Unit	Time	Entity	Role	Relation	inRelationTo	wasDerivedFrom	wasGeneratedBy	hasPosition   
    @Override
    public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {

        //System.out.println("SDDAttributeGenerator: createRow with label " + getLabel(rec));

        Map<String, Object> row = new HashMap<String, Object>();
        List<String> tmp = new ArrayList<String>();
        String sddAttUri = sddUri.replace("SDDICT","SDDATT") + "/" + String.valueOf(rowNumber);

        /* this makes sure columns that are related to mergedEA will not be repeatedly processed.
           for example:
           MSLvrTscore	Visual Reception: T score	ncit:C120401	??visual_reception1
           ??visual_reception1		                cogat:03171 	??child
           MSLvrTscore is processed since ??visual_reception1 is in mergedEA, i.e., processing
           ??visual_reception1 is the same as processing MSLvrTscore. So when we scan the original file
           and reach MSLvrTscore, we should skip it. This tmp map is to make sure we can skip things like
           these correctly
         */
        for (String i : mergedEA.keySet()) {
            tmp.add(mergedEA.get(i).get(0));
        }

        String lineLabel = getLabel(rec);
        if (mergedEA.containsKey(getLabel(rec))) {
            logger.println("[Merged Attribute] : " + getLabel(rec) + " ---> " + mergedEA.get(getLabel(rec)));
            row.put("hasURI", sddAttUri);
            row.put("a", "hasco:SDDAttribute");
            row.put("hasco:hascoType", "hasco:SDDAttribute");
            row.put("rdfs:label", mergedEA.get(getLabel(rec)).get(0));
            row.put("rdfs:comment", mergedEA.get(getLabel(rec)).get(1));
            row.put("hasco:partOfSchema", sddUri);
            row.put("hasco:listPosition", String.valueOf(rowNumber));
            if (!currentHasEntity.containsKey(getLabel(rec))){
                row.put("hasco:hasEntity", getEntity(rec));
            }
            if (getRelation(rec).length() > 0) {
                row.put(getRelation(rec), getInRelationTo(rec));
            } else {
                row.put("sio:SIO_000668", getInRelationTo(rec));
            }
            if (getInRelationTo(rec).length() > 0) {
                if (getRelation(rec).length() > 0) {
                    row.put("hasco:Relation", getRelation(rec));
                } else {
                    row.put("hasco:Relation", "sio:SIO_000668");
                }
            }
            row.put("hasco:hasAttribute", retrieveAttributes(rec, lineLabel));
            row.put("hasco:hasUnit", mergedEA.get(getLabel(rec)).get(2));
            if (mergedEA.get(getLabel(rec)).get(3).length()>0){
                //row.put("hasco:hasEvent", kbPrefix + "SDDE-" + SDDName + "-" + mergedEA.get(getLabel(rec)).get(3).trim().replace(" ","").replace("_","-").replace("??", "").replace(":", "-"));
                row.put("hasco:hasEvent", kbPrefix + "SDDO-" + sddName + "-" + mergedEA.get(getLabel(rec)).get(3).trim().replace(" ","").replace("_","-").replace("??", "").replace(":", "-"));
            }
            row.put("hasco:hasSource", "");
            row.put("hasco:isVariableOf", getVariableOf(rec));
            row.put("hasco:isVirtual", checkVirtual(rec).toString());
            row.put("hasco:isPIConfirmed", "false");
            row.put("prov:wasDerivedFrom", getWasDerivedFrom(rec));	
            if (getWasGeneratedBy(rec).length() > 0) {
                row.put("prov:wasGeneratedBy", getWasGeneratedBy(rec));	
            }
            row.put("vstoi:hasSIRManagerEmail", managerEmail);
        } else if (!tmp.contains(getLabel(rec))) {
            if (mergedAA.containsKey(getLabel(rec))) {
                logger.println("[Derived Attribute] : " + getLabel(rec) + " ---> " + mergedAA.get(getLabel(rec)));
                List<String> attributes = mergedAA.get(getLabel((rec)));
                row.put("hasco:isVariableOf", getVariableOf(attributes.get(attributes.size()-1)));
                row.put("hasco:hasAttribute", attributes);
                //row.put("hasco:hasAttribute", URIUtils.replacePrefixEx(mergedAA.get(getLabel(rec)).get(1)));
            } else {
                row.put("hasco:isVariableOf", getVariableOf(rec));
                row.put("hasco:hasAttribute", getAttribute(rec));
            }
            row.put("hasURI", sddAttUri);
            row.put("a", "hasco:SDDAttribute");
            row.put("hasco:hascoType", "hasco:SDDAttribute");
            row.put("rdfs:label", getLabel(rec));
            row.put("rdfs:comment", getLabel(rec));
            row.put("hasco:partOfSchema", sddUri);
            row.put("hasco:listPosition", String.valueOf(rowNumber));
            if (!currentHasEntity.containsKey(getLabel(rec))){
                row.put("hasco:hasEntity", getEntity(rec));
            }
            if (getRelation(rec).length() > 0) {
                row.put(getRelation(rec), getInRelationTo(rec));
            } else {
                row.put("sio:SIO_000668", getInRelationTo(rec));
            }
            if (getInRelationTo(rec).length() > 0) {
                if (getRelation(rec).length() > 0) {
                    row.put("hasco:Relation", getRelation(rec));
                } else {
                    row.put("hasco:Relation", "sio:SIO_000668");
                }
            }
            row.put("hasco:hasUnit", getUnit(rec));
            row.put("hasco:hasEvent", getTime(rec));
            row.put("hasco:hasSource", "");
            row.put("hasco:isVirtual", checkVirtual(rec).toString());
            row.put("hasco:isPIConfirmed", "false");
            row.put("prov:wasDerivedFrom", getWasDerivedFrom(rec));
            if (getWasGeneratedBy(rec).length() > 0) {
                row.put("prov:wasGeneratedBy", getWasGeneratedBy(rec));	
            }
            row.put("vstoi:hasSIRManagerEmail", managerEmail);
        } else {
            row.put("hasURI", sddAttUri);
        }

        return row;
    }

    private List<String> retrieveAttributes(Record record, String columnLabel) {

        List<String> attributes = new ArrayList<>();
        if ( !mergedEA.containsKey(columnLabel) ) return attributes;

        // note starting from [4] is the list of attributes
        // attributes.add(getAttribute(record));
        List<String> items = mergedEA.get(columnLabel);
        for ( int i = items.size()-1; i >= 4; i-- ) {
            attributes.add(items.get(i));
        }
        attributes.add("end"); // a dummy one to be consistent with mergeAA

        return attributes;
    }

    Map<String, Object> createRelationRow(Record rec, int rowNumber) throws Exception {
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("hasURI", kbPrefix + "SDDA-" + sddName + "-" + getLabel(rec).trim().replace(" ", "").replace("_","-").replace("??", ""));
        if (getRelation(rec).length() > 0) {
            row.put(getRelation(rec), getInRelationTo(rec));
        } else {
            row.put("sio:SIO_000668", getInRelationTo(rec));
        }
        if (getInRelationTo(rec).length() > 0) {
            if (getRelation(rec).length() > 0) {
                row.put("hasco:Relation", getRelation(rec));
            } else {
                row.put("hasco:Relation", "sio:SIO_000668");
            }
        }

        return row;
    }

    Map<String, Object> createDerivedFromRow(String item, Record rec) throws Exception {
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("hasURI", kbPrefix + "SDDA-" + sddName + "-" + getLabel(rec).trim().replace(" ", "").replace("_","-").replace("??", ""));
        row.put("prov:wasDerivedFrom", item);

        return row;
    }

    Map<String, Object> createMergeEAAttrRow(String attr, Map<String, List<String>> mergedEA) throws Exception {
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("hasURI", kbPrefix + "SDDA-" + sddName + "-" + mergedEA.get(attr).get(0).trim().replace(" ", "").replace("_","-").replace("??", ""));
        row.put("hasco:hasAttribute", mergedEA.get(attr).get(4));
        return row;
    }

    Map<String, Object> createMergeAAAttrRow(String attr, Map<String, List<String>> mergedAA) throws Exception {
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("hasURI", kbPrefix + "SDDA-" + sddName + "-" + attr.trim().replace(" ", "").replace("_","-").replace("??", ""));
        row.put("hasco:hasAttribute", mergedAA.get(attr).get(1));
        return row;
    }

    @Override
    public String getTableName() {
        return "SDDAttribute";
    }

    @Override
    public String getErrorMsg(Exception e) {
        return "Error in SDDAttributeGenerator: " + e.getMessage();
    }

    /**
     * Get the mapping of column labels to their SDDAttribute URIs.
     * This is used by PVGenerator to properly link PossibleValues to SDDAttributes.
     *
     * @return Map of column label -> SDDAttribute URI
     */
    public Map<String, String> getLabelToUriMap() {
        return labelToUriMap;
    }

    @Override
    public void deleteRowsFromTripleStore(List<Map<String, Object>> rows) {
        // doing nothing here because the SDD itself has been deleted already
        return;
    }

}

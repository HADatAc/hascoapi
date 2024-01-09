package org.hascoapi.ingestion;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.Templates;
import org.hascoapi.vocabularies.HASCO;

public class StudyGenerator extends BaseGenerator {

    final String kbPrefix = ConfigProp.getKbPrefix();
    String fileName;
    Study study;

    public StudyGenerator(Study study, DataFile dataFile, String templateFile) {
        super(dataFile,null,templateFile);
        this.fileName = dataFile.getFilename();
        this.study = study;
    }

    @Override
    public void initMapping() {
        System.out.println("start initMapping");
        System.out.println("STUDYID: " + templates.getSTUDYID());
        try {
        mapCol.clear();
        System.out.println("initMapping (1) ");
        mapCol.put("studyID", templates.getSTUDYID());
        System.out.println("initMapping (2)");
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
        System.out.println("initMapping (3)");
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
        mapCol.put("externalSource", templates.getEXTSRC());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("end initMapping");
    }

    private String getId(Record rec) {
        return rec.getValueByColumnName(mapCol.get("studyID"));
    }

    private String getUri(Record rec) {
        //System.out.println("Study Generator: template for STUDYID: [" +  templates.getSTUDYID + "]");
        String str = rec.getValueByColumnName(mapCol.get("studyID"));
        return kbPrefix + "STD-" + str;
    }

    private String getType() {
        return "hasco:Study";
    }

    private String getTitle(Record rec) {
        return rec.getValueByColumnName(mapCol.get("studyTitle"));
    }

    private String getAims(Record rec) {
        return rec.getValueByColumnName(mapCol.get("studyAims"));
    }

    private String getSignificance(Record rec) {
        return rec.getValueByColumnName(mapCol.get("studySignificance"));
    }

    private String getInstitutionUri(Record rec) {
        return kbPrefix + "ORG-" + rec.getValueByColumnName(mapCol.get("institution")).replaceAll(" ", "-").replaceAll(",", "").replaceAll("'", ""); 
    }

    private String getAgentUri(Record rec) {
        return kbPrefix + "PER-" + rec.getValueByColumnName(mapCol.get("PI")).replaceAll(" ", "-"); 
    }

    private String getExtSource(Record rec) {
        return rec.getValueByColumnName(mapCol.get("externalSource")); 
    }

    @Override
    public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
        Map<String, Object> row = new HashMap<String, Object>();
        if (getUri(rec).length() > 0) {
            row.put("hasco:hasId", getId(rec));
            row.put("hasURI", getUri(rec));
            row.put("a", getType());
            row.put("hasco:hascoType", HASCO.STUDY);
            row.put("rdfs:label", getTitle(rec));
            row.put("skos:definition", getAims(rec));
            row.put("rdfs:comment", getSignificance(rec));
            row.put("vstoi:hasSIRManagerEmail", study.getHasSIRManagerEmail());
            if(mapCol.get("PI") != null && rec.getValueByColumnName(mapCol.get("PI")) != null && 
                    rec.getValueByColumnName(mapCol.get("PI")).length() > 0) {
                row.put("hasco:hasAgent", getAgentUri(rec));
            }
            if(mapCol.get("institution") != null && rec.getValueByColumnName(mapCol.get("institution")) != null && 
                    rec.getValueByColumnName(mapCol.get("institution")).length() > 0) {
                row.put("hasco:hasInstitution", getInstitutionUri(rec));
            }
            if(mapCol.get("externalSource") != null && rec.getValueByColumnName(mapCol.get("externalSource")) != null && 
                    rec.getValueByColumnName(mapCol.get("externalSource")).length() > 0) {
                row.put("hasco:hasExternalSource", getExtSource(rec));
            }
            setStudyUri(URIUtils.replacePrefixEx(getUri(rec)));
        }

        return row;
    }

    @Override
    public String getTableName() {
        return "Study";
    }

    @Override
    public String getErrorMsg(Exception e) {
        return "Error in StudyGenerator: " + e.getMessage();
    }
}


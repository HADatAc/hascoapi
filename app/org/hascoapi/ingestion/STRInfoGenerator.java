package org.hascoapi.ingestion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hascoapi.ingestion.Record;
import org.hascoapi.ingestion.RecordFile;
import org.hascoapi.entity.pojo.SDD;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.HADatAcThing;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.ConfigProp;

import io.jsonwebtoken.lang.Objects;

public class STRInfoGenerator extends BaseGenerator{

	final String kbPrefix = ConfigProp.getKbPrefix();
	public static final String FILESTREAM = "FileStream";
    public static final String MESSAGESTREAM = "MessageStream";
    public static final String MESSAGETOPIC = "MessageTopic";

    private Map<String, String> mapCatalog = new HashMap<String, String>();
    private Map<String, Map<String, String>> fileStreamSpec = new HashMap<String, Map<String, String>>();
    private Map<String, Map<String, String>> messageStreamSpec = new HashMap<String, Map<String, String>>();
    // private String studyId;
    private String studyUri;
    //private String version;
    private Study study;

    public STRInfoGenerator(DataFile dataFile) {
    	super(dataFile);
    	//System.out.println("...inside STRInfoGenerator.");
    	readCatalog(dataFile.getRecordFile());
    	study = null;
    }

    public Map<String, String> getCatalog() {
        return mapCatalog;
    }

    public Map<String, Map<String, String>> getFileStreamSpec() {
        return fileStreamSpec;
    }

    public Map<String, Map<String, String>> getMessageStreamSpec() {
        return messageStreamSpec;
    }

    private void readCatalog(RecordFile file) {
        if (!file.isValid()) {
            return;
        }
        // This is on the infosheet
        for (Record record : file.getRecords()) {
            mapCatalog.put(record.getValueByColumnIndex(0), record.getValueByColumnIndex(1));
            System.out.println("STR's mapCatalog: [" + record.getValueByColumnIndex(0) + "]  [" + record.getValueByColumnIndex(1) + "]");
        }
    }

    // public String getStudyId() {
    //     studyId = mapCatalog.get("Study_ID");
    // 	//System.out.println("studyID in getStudyId: [" + studyId + "]");
    //     if (studyId == null || studyId.isEmpty()) {
    //     	study = null;
    //     	return "";
    //     }
    //     study = Study.findById(studyId);
    //     return studyId;
    // }

    public String getStudyUri() {
        studyUri = mapCatalog.get("Study_ID");
    	//System.out.println("studyID in getStudyUri: [" + studyUri + "]");
        if (studyUri == null || studyUri.isEmpty()) {
        	return "";
        }
        return studyUri;
    }

    public Study getStudy() {
    	// this will load the study, if available
    	//getStudyId();
    	//return study;
        studyUri = mapCatalog.get("Study_ID");
        // studyUri = mapCatalog.get("Study_Uri");

        studyUri = URIUtils.replacePrefixEx(studyUri);

        if (studyUri == null || studyUri.isEmpty()) {
        	study = null;
        	return null;
        }

        study = Study.find(studyUri);

        return study;
    }

    public String getVersion() {
        return mapCatalog.get("Version");
    }

    //public void setVersion(String version) {
    //    this.version = version;
    //}

}

package org.hascoapi.ingestion;

import java.lang.String;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.Study;

public class AnnotateSTR {

     public static GeneratorChain exec(DataFile dataFile, String templateFile) {
        // System.out.println("Processing STR file ...");

        // // verifies if data file is an Excel spreadsheet
        // String fileName = dataFile.getFilename();
        // if (!fileName.endsWith(".xlsx")) {
        //     dataFile.getLogger().printExceptionById("STR_00004");
        //     return null;
        // }

        // verifies if data file contains an InfoSheet sheet
        RecordFile recordFile = new SpreadsheetRecordFile(dataFile.getFile(), "InfoSheet");
        if (!recordFile.isValid()) {
            dataFile.getLogger().printExceptionById("STR_00001");
            return null;
        } else {
            dataFile.setRecordFile(recordFile);
        }

        STRInfoGenerator strInfo = new STRInfoGenerator(dataFile);
        Study strStudy = strInfo.getStudy();
        String strVersion = strInfo.getVersion();

        // verifies if study is specified
        if (strStudy == null) {
            dataFile.getLogger().printExceptionByIdWithArgs("STR_00002", strInfo.getStudyUri());
            return null;
        }
        // verifies if version is specified
        if (strVersion == "") {
            dataFile.getLogger().printExceptionById("STR_00003");
            return null;
        }
        Map<String, String> mapCatalog = strInfo.getCatalog();

        RecordFile fileStreamRecordFile = null;
        RecordFile messageStreamRecordFile = null;
        RecordFile messageTopicRecordFile = null;

        // verifies if filestream sheet is available, even if no file stream is specified
        if (mapCatalog.get(STRInfoGenerator.FILESTREAM) == null) {
        	dataFile.getLogger().printExceptionById("STR_00005");
        	return null;
        }
        fileStreamRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get(STRInfoGenerator.FILESTREAM).replace("#", ""));

        // verifies if messagestream sheet is available, even if no message stream is specified
        if (mapCatalog.get(STRInfoGenerator.MESSAGESTREAM) == null) {
    		dataFile.getLogger().printExceptionById("STR_00006");
    		return null;
        }
        messageStreamRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get(STRInfoGenerator.MESSAGESTREAM).replace("#", ""));

        // verifies if messagetopic sheet is available, even if no message topic is specified
        if (mapCatalog.get(STRInfoGenerator.MESSAGETOPIC) == null) {
    		dataFile.getLogger().printExceptionById("STR_00016");
    		return null;
        }
        messageTopicRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get(STRInfoGenerator.MESSAGETOPIC).replace("#", ""));

        // verifies if not both fileStream sheet and messageStream sheet are empty
        if (fileStreamRecordFile.getNumberOfRows() <= 0 && messageStreamRecordFile.getNumberOfRows() <= 0) {
    		dataFile.getLogger().printExceptionById("STR_00007");
    		return null;
        }
        // verifies that there is info in messageTopics in case messageStream is not empty
        if ((messageStreamRecordFile.getNumberOfRows() <= 0 && messageTopicRecordFile.getNumberOfRows() > 0) ||
            (messageStreamRecordFile.getNumberOfRows() > 0 && messageTopicRecordFile.getNumberOfRows() <= 0)) {
    		dataFile.getLogger().printExceptionById("STR_00010");
    		return null;
        }

        GeneratorChain chain = new GeneratorChain();
        chain.setStudyUri(strStudy.getUri());

        DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String startTime = isoFormat.format(new Date());
        if (fileStreamRecordFile.getNumberOfRows() > 1 && fileStreamRecordFile.getRecords().size() > 0) {
        	// TODO
            chain.addGenerator(new STRFileGenerator(dataFile, strStudy, fileStreamRecordFile, startTime, templateFile));
        }
        if (messageStreamRecordFile.getNumberOfRows() > 1 && messageStreamRecordFile.getRecords().size() > 0) {
        	STRMessageGenerator messageGen = new STRMessageGenerator(dataFile, strStudy, messageStreamRecordFile, startTime);
        	if (!messageGen.isValid()) {
        		dataFile.getLogger().printExceptionByIdWithArgs(messageGen.getErrorMessage(),messageGen.getErrorArgument());
            	return null;
        	}
        	chain.addGenerator(messageGen);
        }
        if (messageTopicRecordFile.getNumberOfRows() > 1 && messageTopicRecordFile.getRecords().size() > 0) {
        	STRTopicGenerator topicGen = new STRTopicGenerator(dataFile, messageTopicRecordFile, startTime);
        	if (!topicGen.isValid()) {
        		dataFile.getLogger().printExceptionByIdWithArgs(topicGen.getErrorMessage(),topicGen.getErrorArgument());
            	return null;
        	}
        	chain.addGenerator(topicGen);
        }
        return chain;
     }
}

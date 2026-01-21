package org.hascoapi.ingestion;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.Study;

public class AnnotateSTR extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String templateFile) {
        dataFile.getLogger().println("Processing STR meta-template ...");

        // Load InfoSheet catalog using BaseAnnotator
        Map<String, String> mapCatalog = loadCatalog(dataFile, Constants.MT_STR);
        if (mapCatalog == null) {
            dataFile.getLogger().printExceptionById("STR_00001");
            return null;
        }

        // Extract STR info using STRInfoGenerator
        STRInfoGenerator strInfo = new STRInfoGenerator(dataFile);
        Study strStudy = strInfo.getStudy();
        String strVersion = strInfo.getVersion();

        // Validate required fields
        if (strStudy == null) {
            dataFile.getLogger().printExceptionByIdWithArgs("STR_00002", strInfo.getStudyUri());
            return null;
        }
        if (strVersion == null || strVersion.isEmpty()) {
            dataFile.getLogger().printExceptionById("STR_00003");
            return null;
        }

        // Verify required sheets exist, else log error and return null
        if (!mapCatalog.containsKey(STRInfoGenerator.FILESTREAM)) {
            dataFile.getLogger().printExceptionById("STR_00004"); // FileStream sheet missing
            return null;
        }
        if (!mapCatalog.containsKey(STRInfoGenerator.MESSAGESTREAM)) {
            dataFile.getLogger().printExceptionById("STR_00005"); // MessageStream sheet missing
            return null;
        }
        if (!mapCatalog.containsKey(STRInfoGenerator.MESSAGETOPIC)) {
            dataFile.getLogger().printExceptionById("STR_00015"); // MessageTopic sheet missing
            return null;
        }

        // Load each sheet as RecordFile
        RecordFile fileStreamRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get(STRInfoGenerator.FILESTREAM).replace("#", "").trim());
        RecordFile messageStreamRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get(STRInfoGenerator.MESSAGESTREAM).replace("#", "").trim());
        RecordFile messageTopicRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get(STRInfoGenerator.MESSAGETOPIC).replace("#", "").trim());

        // Validate rows presence across streams and topics
        if (fileStreamRecordFile.getNumberOfRows() <= 0 && messageStreamRecordFile.getNumberOfRows() <= 0) {
            dataFile.getLogger().printExceptionById("STR_00006");
            return null;
        }
        if ((messageStreamRecordFile.getNumberOfRows() <= 0 && messageTopicRecordFile.getNumberOfRows() > 0) ||
            (messageStreamRecordFile.getNumberOfRows() > 0 && messageTopicRecordFile.getNumberOfRows() <= 0)) {
            dataFile.getLogger().printExceptionById("STR_00009");
            return null;
        }

        GeneratorChain chain = new GeneratorChain();
        chain.setDataFile(dataFile);
        chain.setStudyUri(strStudy.getUri());

        DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String startTime = isoFormat.format(new Date());

        // Add STRFileGenerator if fileStream sheet is valid (requires at least one non-empty record)
        if (fileStreamRecordFile.getRecords() != null && !fileStreamRecordFile.getRecords().isEmpty()) {
            STRFileGenerator fileGen = new STRFileGenerator(dataFile, strStudy, fileStreamRecordFile, startTime, strVersion, templateFile);
            fileGen.setNamedGraphUri(dataFile.getUri());
            chain.addGenerator(fileGen);
        }

        // Add STRMessageGenerator if messageStream sheet is valid (requires at least one non-empty record)
        if (messageStreamRecordFile.getRecords() != null && !messageStreamRecordFile.getRecords().isEmpty()) {
            STRMessageGenerator messageGen = new STRMessageGenerator(dataFile, strStudy, messageStreamRecordFile, startTime);
            if (!messageGen.isValid()) {
                dataFile.getLogger().printExceptionByIdWithArgs(messageGen.getErrorMessage(), messageGen.getErrorArgument());
                return null;
            }
            chain.addGenerator(messageGen);
        }

        // Add STRTopicGenerator if messageTopic sheet is valid (requires at least one non-empty record)
        if (messageTopicRecordFile.getRecords() != null && !messageTopicRecordFile.getRecords().isEmpty()) {
            STRTopicGenerator topicGen = new STRTopicGenerator(dataFile, messageTopicRecordFile, startTime);
            if (!topicGen.isValid()) {
                dataFile.getLogger().printExceptionByIdWithArgs(topicGen.getErrorMessage(), topicGen.getErrorArgument());
                return null;
            }
            chain.addGenerator(topicGen);
        }

        return chain;
    }
}

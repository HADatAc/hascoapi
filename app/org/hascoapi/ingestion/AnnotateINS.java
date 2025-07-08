package org.hascoapi.ingestion;

import java.lang.String;
import java.util.*;

import org.hascoapi.entity.pojo.DataFile;

public class AnnotateINS {

     public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        RecordFile recordFile = new SpreadsheetRecordFile(dataFile.getFile(), "InfoSheet");
        if (!recordFile.isValid()) {
            dataFile.getLogger().printExceptionById("DPL_00001");
            return null;
        } else {
            dataFile.setRecordFile(recordFile);
        }

        Map<String, String> mapCatalog = new HashMap<String, String>();
        for (Record record : dataFile.getRecordFile().getRecords()) {
            mapCatalog.put(record.getValueByColumnIndex(0), record.getValueByColumnIndex(1));
            //System.out.println(record.getValueByColumnIndex(0) + ":" + record.getValueByColumnIndex(1));
        }

        //IngestionWorker.nameSpaceGen(dataFile, mapCatalog,templateFile);

        //IngestionWorker.annotationGen(dataFile, mapCatalog, templateFile, status);

        GeneratorChain chain = new GeneratorChain();
        //RecordFile sheet = null;

        try {

            chain.setNamedGraphUri(dataFile.getUri());

            String responseOptionSheet = mapCatalog.get("ResponseOptions");
            if (responseOptionSheet == null) {
                System.out.println("[WARNING] 'ResponseOptions' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'ResponseOptions' sheet is missing.");
            } else {
                //responseOptionSheet = responseOptionSheet.replace("#", "");
                //RecordFile sheet01 = new SpreadsheetRecordFile(dataFile.getFile(), responseOptionSheet);
                //try {
                    //DataFile dataFileForSheet = (DataFile)dataFile.clone();
                    //dataFileForSheet.setRecordFile(sheet01);
                    //INSGenerator respOptionGen = new INSGenerator("responseoption",dataFileForSheet, status);
                    //respOptionGen.setNamedGraphUri(dataFileForSheet.getUri());
                    //chain.addGenerator(respOptionGen);
                //} catch (CloneNotSupportedException e) {
                //    e.printStackTrace();
                //}
            }

            String codeBookSheet = mapCatalog.get("CodeBooks");
            if (codeBookSheet == null) {
                System.out.println("[WARNING] 'CodeBooks' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'CodeBooks' sheet is missing.");
            } else {
                //codeBookSheet = codeBookSheet.replace("#", "");
                //RecordFile sheet02 = new SpreadsheetRecordFile(dataFile.getFile(), codeBookSheet);
                //try {
                    //DataFile dataFileForSheet = (DataFile)dataFile.clone();
                    //dataFileForSheet.setRecordFile(sheet02);
                    //INSGenerator codeBookGen = new INSGenerator("codebook",dataFileForSheet, status);
                    //codeBookGen.setNamedGraphUri(dataFileForSheet.getUri());
                    //chain.addGenerator(codeBookGen);
                //} catch (CloneNotSupportedException e) {
                    //e.printStackTrace();
                //}
            }

            String codeBookSlotSheet = mapCatalog.get("CodeBookSlots");
            if (codeBookSlotSheet == null) {
                System.out.println("[WARNING] 'CodeBookSlots' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'CodeBookSlots' sheet is missing.");
            } else {
                //codeBookSlotSheet = codeBookSlotSheet.replace("#", "");
                //RecordFile sheet03 = new SpreadsheetRecordFile(dataFile.getFile(), codeBookSlotSheet);
                //try {
                    //DataFile dataFileForSheet = (DataFile)dataFile.clone();
                    //dataFileForSheet.setRecordFile(sheet03);
                    //CodeBookSlotGenerator cbSlotGen = new CodeBookSlotGenerator(dataFileForSheet);
                    //cbSlotGen.setNamedGraphUri(dataFileForSheet.getUri());
                    //chain.addGenerator(cbSlotGen);
                //} catch (CloneNotSupportedException e) {
                    //e.printStackTrace();
                //}
            }

            String actuatorStemSheet = mapCatalog.get("ActuatorStems");
            if (actuatorStemSheet == null) {
                System.out.println("[WARNING] 'ActuatorStems' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'ActuatorStems' sheet is missing.");
            } else {
                //actuatorStemSheet = actuatorStemSheet.replace("#", "");
                //RecordFile sheet04 = new SpreadsheetRecordFile(dataFile.getFile(), actuatorStemSheet);
                //try {
                    //DataFile dataFileForSheet = (DataFile)dataFile.clone();
                    //dataFileForSheet.setRecordFile(sheet04);
                    //INSGenerator actStemGen = new INSGenerator("actuatorstem",dataFileForSheet, status);
                    //actStemGen.setNamedGraphUri(dataFileForSheet.getUri());
                    //chain.addGenerator(actStemGen);
                //} catch (CloneNotSupportedException e) {
                    //e.printStackTrace();
                //}
            }

            String actuatorSheet = mapCatalog.get("Actuators");
            if (actuatorSheet == null) {
                System.out.println("[WARNING] 'Actuators' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'Actuators' sheet is missing.");
            } else {
                //actuatorSheet = actuatorSheet.replace("#", "");
                //RecordFile sheet05 = new SpreadsheetRecordFile(dataFile.getFile(), actuatorSheet);
                //try {
                    //DataFile dataFileForSheet = (DataFile)dataFile.clone();
                    //dataFileForSheet.setRecordFile(sheet05);
                    //ActuatorGenerator actGen = new ActuatorGenerator(dataFileForSheet, status);
                    //actGen.setNamedGraphUri(dataFileForSheet.getUri());
                    //chain.addGenerator(actGen);
                //} catch (CloneNotSupportedException e) {
                    //e.printStackTrace();
                //}
            }

            String detectorStemSheet = mapCatalog.get("DetectorStems");
            if (detectorStemSheet == null) {
                System.out.println("[WARNING] 'DetectorStems' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'DetectorStems' sheet is missing.");
            } else {
                //detectorStemSheet = detectorStemSheet.replace("#", "");
                //RecordFile sheet06 = new SpreadsheetRecordFile(dataFile.getFile(), detectorStemSheet);
                //try {
                    //DataFile dataFileForSheet = (DataFile)dataFile.clone();
                    //dataFileForSheet.setRecordFile(sheet06);
                    //INSGenerator detStemGen = new INSGenerator("detectorstem",dataFileForSheet, status);
                    //detStemGen.setNamedGraphUri(dataFileForSheet.getUri());
                    //chain.addGenerator(detStemGen);
                //} catch (CloneNotSupportedException e) {
                    //e.printStackTrace();
                //}
            }

            String detectorSheet = mapCatalog.get("Detectors");
            if (detectorSheet == null) {
                System.out.println("[WARNING] 'Detectors' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'Detectors' sheet is missing.");
            } else {
                //detectorSheet = detectorSheet.replace("#", "");
                //RecordFile sheet07 = new SpreadsheetRecordFile(dataFile.getFile(), detectorSheet);
                //try {
                    //DataFile dataFileForSheet = (DataFile)dataFile.clone();
                    //dataFileForSheet.setRecordFile(sheet07);
                    //DetectorGenerator detGen = new DetectorGenerator(dataFileForSheet, status);
                    //detGen.setNamedGraphUri(dataFileForSheet.getUri());
                    //chain.addGenerator(detGen);
                //} catch (CloneNotSupportedException e) {
                    //e.printStackTrace();
                //}
            }

            String slotElementSheet = mapCatalog.get("SlotElements");
            if (slotElementSheet == null) {
                System.out.println("[WARNING] 'SlotElements' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'SlotElements' sheet is missing.");
            } else {
                //slotElementSheet = slotElementSheet.replace("#", "");
                //RecordFile sheet08 = new SpreadsheetRecordFile(dataFile.getFile(), slotElementSheet);
                //try {
                    //DataFile dataFileForSheet = (DataFile)dataFile.clone();
                    //dataFileForSheet.setRecordFile(sheet08);
                    //INSGenerator seGen = new INSGenerator("slotelement",dataFileForSheet, status);
                    //seGen.setNamedGraphUri(dataFileForSheet.getUri());
                    //chain.addGenerator(seGen);
                //} catch (CloneNotSupportedException e) {
                    //e.printStackTrace();
                //}
            }

            String instrumentSheet = mapCatalog.get("Instruments");
            if (instrumentSheet == null) {
                System.out.println("[WARNING] 'Instruments' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'Instruments' sheet is missing.");
            } else {
                //instrumentSheet = instrumentSheet.replace("#", "");
                //RecordFile sheet09 = new SpreadsheetRecordFile(dataFile.getFile(), instrumentSheet);
                //try {
                    //DataFile dataFileForSheet = (DataFile)dataFile.clone();
                    //dataFileForSheet.setRecordFile(sheet09);
                    //INSGenerator ins = new INSGenerator("instrument",dataFileForSheet, status);
                    //ins.setNamedGraphUri(dataFileForSheet.getUri());
                    //chain.addGenerator(ins);
                //} catch (CloneNotSupportedException e) {
                    //e.printStackTrace();
                //}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return chain;
    }

}

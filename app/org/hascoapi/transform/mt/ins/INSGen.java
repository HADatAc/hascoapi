package org.hascoapi.transform.mt.ins;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.entity.pojo.DetectorStem;
import org.hascoapi.entity.pojo.Detector;
import org.hascoapi.entity.pojo.ActuatorStem;
import org.hascoapi.entity.pojo.Actuator;
import org.hascoapi.entity.pojo.Codebook;
import org.hascoapi.entity.pojo.CodebookSlot;
import org.hascoapi.entity.pojo.ResponseOption;
import org.hascoapi.entity.pojo.AnnotationStem;
import org.hascoapi.entity.pojo.Annotation;
import org.hascoapi.entity.pojo.GenericFindWithStatus;
import org.hascoapi.utils.ConfigProp;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class INSGen {

    public static final String INFOSHEET            = "InfoSheet";
    public static final String NAMESPACES           = "#Namespaces";
    public static final String INSTRUMENTS          = "#Instruments";
    public static final String CONTAINER_SLOTS      = "#ContainerSlots";
    public static final String DETECTOR_STEMS       = "#DetectorStems";
    public static final String DETECTORS            = "#Detectors";
    public static final String ACTUATOR_STEMS       = "#ActuatorStems";
    public static final String ACTUATORS            = "#Actuators";
    public static final String CODEBOOKS            = "#CodeBooks";
    public static final String CODEBOOK_SLOTS       = "#CodeBookSlots";
    public static final String RESPONSE_OPTIONS     = "#ResponseOptions";
    public static final String ANNOTATIONS          = "#Annotations";
    public static final String ANNOTATION_STEMS     = "#AnnotationStems";

    public static final int PAGESIZE                = 20000;
    public static final int OFFSET                  = 0;

    public static String genByStatus(String status, String filename) {
        Workbook workbook = INSGen.create(filename);
        String resp = "";

        GenericFindWithStatus<Instrument> instrumentQuery = new GenericFindWithStatus<Instrument>();
        List<Instrument> instruments = instrumentQuery.findByStatusWithPages(Instrument.class, status, PAGESIZE, OFFSET);
        if (instruments != null) {
            for (Instrument instrument: instruments) {
                workbook = INSInstrument.add(workbook,instrument);
                workbook = INSContainerSlot.addByInstrument(workbook,instrument);
            }
        }
        GenericFindWithStatus<DetectorStem> detStemQuery = new GenericFindWithStatus<DetectorStem>();
        List<DetectorStem> detStems = detStemQuery.findByStatusWithPages(DetectorStem.class, status, PAGESIZE, OFFSET);
        if (detStems != null) {
            for (DetectorStem detStem: detStems) {
                workbook = INSDetectorStem.add(workbook,detStem);
            }
        }
        GenericFindWithStatus<Detector> detQuery = new GenericFindWithStatus<Detector>();
        List<Detector> dets = detQuery.findByStatusWithPages(Detector.class, status, PAGESIZE, OFFSET);
        if (dets != null) {
            for (Detector det: dets) {
                workbook = INSDetector.add(workbook,det);
            }
        }
        GenericFindWithStatus<ActuatorStem> actStemQuery = new GenericFindWithStatus<ActuatorStem>();
        List<ActuatorStem> actStems = actStemQuery.findByStatusWithPages(ActuatorStem.class, status, PAGESIZE, OFFSET);
        if (actStems != null) {
            for (ActuatorStem actStem: actStems) {
                workbook = INSActuatorStem.add(workbook,actStem);
            }
        }
        GenericFindWithStatus<Actuator> actQuery = new GenericFindWithStatus<Actuator>();
        List<Actuator> acts = actQuery.findByStatusWithPages(Actuator.class, status, PAGESIZE, OFFSET);
        if (acts != null) {
            for (Actuator act: acts) {
                workbook = INSActuator.add(workbook,act);
            }
        }
        GenericFindWithStatus<Codebook> cbQuery = new GenericFindWithStatus<Codebook>();
        List<Codebook> cbs = cbQuery.findByStatusWithPages(Codebook.class, status, PAGESIZE, OFFSET);
        if (cbs != null) {
            for (Codebook cb: cbs) {
                workbook = INSCodebook.add(workbook,cb);
                workbook = INSCodebookSlot.addByCodebook(workbook,cb);
            }
        }
        GenericFindWithStatus<ResponseOption> respOptionQuery = new GenericFindWithStatus<ResponseOption>();
        List<ResponseOption> respOptions = respOptionQuery.findByStatusWithPages(ResponseOption.class, status, PAGESIZE, OFFSET);
        if (respOptions != null) {
            for (ResponseOption respOption: respOptions) {
                workbook = INSResponseOption.add(workbook,respOption);
            }
        }
        GenericFindWithStatus<AnnotationStem> annStemQuery = new GenericFindWithStatus<AnnotationStem>();
        List<AnnotationStem> annStems = annStemQuery.findByStatusWithPages(AnnotationStem.class, status, PAGESIZE, OFFSET);
        if (annStems != null) {
            for (AnnotationStem annStem: annStems) {
                workbook = INSAnnotationStem.add(workbook,annStem);
            }
        }
        GenericFindWithStatus<Annotation> annQuery = new GenericFindWithStatus<Annotation>();
        List<Annotation> anns = annQuery.findByStatusWithPages(Annotation.class, status, PAGESIZE, OFFSET);
        if (anns != null) {
            for (Annotation ann: anns) {
                workbook = INSAnnotation.add(workbook,ann);
            }
        }
        return INSGen.save(workbook,filename);
    }

    public static String genByInstrument(Instrument instrument, String filename) {
        Workbook workbook = INSGen.create(filename);
        return "";
    }

    public static String genByManager(String useremail, String status, String filename) {
        Workbook workbook = INSGen.create(filename);
        boolean withCurrent = false; // this assures that the retrieval of just elements of the requested type.

        GenericFindWithStatus<Instrument> instrumentQuery = new GenericFindWithStatus<Instrument>();
        List<Instrument> instruments = instrumentQuery.findByStatusManagerEmailWithPages(Instrument.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (instruments == null) {
            System.out.println("genByManager: Instruments is NULL");
        } else {
            System.out.println("genByManager: Instruments has " + instruments.size() + " elements");
        }
        if (instruments != null) {
            for (Instrument instrument: instruments) {
                System.out.println("genByManager: Instrument has URI " + instrument.getUri());
                INSInstrument.add(workbook,instrument);
                INSContainerSlot.addByInstrument(workbook,instrument);
            }
        }
        GenericFindWithStatus<DetectorStem> detStemQuery = new GenericFindWithStatus<DetectorStem>();
        List<DetectorStem> detStems = detStemQuery.findByStatusManagerEmailWithPages(DetectorStem.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (detStems != null) {
            for (DetectorStem detStem: detStems) {
                INSDetectorStem.add(workbook,detStem);
            }
        }
        GenericFindWithStatus<Detector> detQuery = new GenericFindWithStatus<Detector>();
        List<Detector> dets = detQuery.findByStatusManagerEmailWithPages(Detector.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (dets != null) {
            for (Detector det: dets) {
                INSDetector.add(workbook,det);
            }
        }
        GenericFindWithStatus<ActuatorStem> actStemQuery = new GenericFindWithStatus<ActuatorStem>();
        List<ActuatorStem> actStems = actStemQuery.findByStatusManagerEmailWithPages(ActuatorStem.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (actStems != null) {
            for (ActuatorStem actStem: actStems) {
                INSActuatorStem.add(workbook,actStem);
            }
        }
        GenericFindWithStatus<Actuator> actQuery = new GenericFindWithStatus<Actuator>();
        List<Actuator> acts = actQuery.findByStatusManagerEmailWithPages(Actuator.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (acts != null) {
            for (Actuator act: acts) {
                INSActuator.add(workbook,act);
            }
        }
        GenericFindWithStatus<Codebook> cbQuery = new GenericFindWithStatus<Codebook>();
        List<Codebook> cbs = cbQuery.findByStatusManagerEmailWithPages(Codebook.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (cbs != null) {
            for (Codebook cb: cbs) {
                INSCodebook.add(workbook,cb);
                INSCodebookSlot.addByCodebook(workbook,cb);
            }
        }
        GenericFindWithStatus<ResponseOption> respOptionQuery = new GenericFindWithStatus<ResponseOption>();
        List<ResponseOption> respOptions = respOptionQuery.findByStatusManagerEmailWithPages(ResponseOption.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (respOptions != null) {
            for (ResponseOption respOption: respOptions) {
                INSResponseOption.add(workbook,respOption);
            }
        }
        GenericFindWithStatus<AnnotationStem> annStemQuery = new GenericFindWithStatus<AnnotationStem>();
        List<AnnotationStem> annStems = annStemQuery.findByStatusManagerEmailWithPages(AnnotationStem.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (annStems != null) {
            for (AnnotationStem annStem: annStems) {
                INSAnnotationStem.add(workbook,annStem);
            }
        }
        GenericFindWithStatus<Annotation> annQuery = new GenericFindWithStatus<Annotation>();
        List<Annotation> anns = annQuery.findByStatusManagerEmailWithPages(Annotation.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (anns != null) {
            for (Annotation ann: anns) {
                INSAnnotation.add(workbook,ann);
            }
        }
        return INSGen.save(workbook,filename);
    }

    public static Workbook create(String filename) {

        // Create a new workbook
        Workbook workbook = new XSSFWorkbook();

        // Create sheet named 'InfoSheet'
        Sheet infoSheet = workbook.createSheet("InfoSheet");

        // Create the header row for InfoSheet
        Row isHeaderRow = infoSheet.createRow(0);
        Cell isHeaderCell1 = isHeaderRow.createCell(0);
        isHeaderCell1.setCellValue("Attribute");
        Cell isHeaderCell2 = isHeaderRow.createCell(1);
        isHeaderCell2.setCellValue("Value");

        Row dataRow1 = infoSheet.createRow(1);
        Cell isDataCell1_1 = dataRow1.createCell(0);
        isDataCell1_1.setCellValue("hasDependencies");
        Cell isDataCell1_2 = dataRow1.createCell(1);
        isDataCell1_2.setCellValue(INSGen.NAMESPACES);

        Row dataRow2 = infoSheet.createRow(2);
        Cell isDataCell2_1 = dataRow2.createCell(0);
        isDataCell2_1.setCellValue("Instruments");
        Cell isDataCell2_2 = dataRow2.createCell(1);
        isDataCell2_2.setCellValue(INSGen.INSTRUMENTS);

        Row isDataRow3 = infoSheet.createRow(3);
        Cell isDataCell3_1 = isDataRow3.createCell(0);
        isDataCell3_1.setCellValue("ContainerSlots");
        Cell isDataCell3_2 = isDataRow3.createCell(1);
        isDataCell3_2.setCellValue(INSGen.CONTAINER_SLOTS);

        Row isDataRow4 = infoSheet.createRow(4);
        Cell isDataCell4_1 = isDataRow4.createCell(0);
        isDataCell4_1.setCellValue("DetectorStems");
        Cell isDataCell4_2 = isDataRow4.createCell(1);
        isDataCell4_2.setCellValue(INSGen.DETECTOR_STEMS);

        Row isDataRow5 = infoSheet.createRow(5);
        Cell isDataCell5_1 = isDataRow5.createCell(0);
        isDataCell5_1.setCellValue("Detectors");
        Cell isDataCell5_2 = isDataRow5.createCell(1);
        isDataCell5_2.setCellValue(INSGen.DETECTORS);

        Row isDataRow6 = infoSheet.createRow(6);
        Cell isDataCell6_1 = isDataRow6.createCell(0);
        isDataCell6_1.setCellValue("ActuatorStems");
        Cell isDataCell6_2 = isDataRow6.createCell(1);
        isDataCell6_2.setCellValue(INSGen.ACTUATOR_STEMS);

        Row isDataRow7 = infoSheet.createRow(7);
        Cell isDataCell7_1 = isDataRow7.createCell(0);
        isDataCell7_1.setCellValue("Actuators");
        Cell isDataCell7_2 = isDataRow7.createCell(1);
        isDataCell7_2.setCellValue(INSGen.ACTUATORS);

        Row isDataRow8 = infoSheet.createRow(8);
        Cell isDataCell8_1 = isDataRow8.createCell(0);
        isDataCell8_1.setCellValue("CodeBooks");
        Cell isDataCell8_2 = isDataRow8.createCell(1);
        isDataCell8_2.setCellValue(INSGen.CODEBOOKS);

        Row isDataRow9 = infoSheet.createRow(9);
        Cell isDataCell9_1 = isDataRow9.createCell(0);
        isDataCell9_1.setCellValue("CodeBookSlots");
        Cell isDataCell9_2 = isDataRow9.createCell(1);
        isDataCell9_2.setCellValue(INSGen.CODEBOOK_SLOTS);

        Row isDataRow10 = infoSheet.createRow(10);
        Cell isDataCell10_1 = isDataRow10.createCell(0);
        isDataCell10_1.setCellValue("ResponseOptions");
        Cell isDataCell10_2 = isDataRow10.createCell(1);
        isDataCell10_2.setCellValue(INSGen.RESPONSE_OPTIONS);

        Row isDataRow11 = infoSheet.createRow(11);
        Cell isDataCell11_1 = isDataRow11.createCell(0);
        isDataCell11_1.setCellValue("Annotations");
        Cell isDataCell11_2 = isDataRow11.createCell(1);
        isDataCell11_2.setCellValue(INSGen.ANNOTATIONS);

        Row isDataRow12 = infoSheet.createRow(12);
        Cell isDataCell12_1 = isDataRow12.createCell(0);
        isDataCell12_1.setCellValue("AnnotationStems");
        Cell isDataCell12_2 = isDataRow12.createCell(1);
        isDataCell12_2.setCellValue(INSGen.ANNOTATION_STEMS);

        // Create sheet named 'Namespaces'
        Sheet nsSheet = workbook.createSheet(INSGen.NAMESPACES);
        String[] nsHeaders = { "hasPrefix", "hasNameSpace", "hasFormat", "hasSource" };

        // Create header row
        Row nsHeaderRow = nsSheet.createRow(0);
        for (int i = 0; i < nsHeaders.length; i++) {
            Cell cell = nsHeaderRow.createCell(i);
            cell.setCellValue(nsHeaders[i]);
        }
        for (int i = 0; i < nsHeaders.length; i++) {
            nsSheet.autoSizeColumn(i);
        }

        // Create sheet named 'Instruments'
        Sheet instrumentSheet = workbook.createSheet(INSGen.INSTRUMENTS);
        String[] instrumentHeaders = { "hasURI", "hasco:hascoType", "rdfs:subClassOf", "rdfs:label", "vstoi:hasShortName", "vstoi:hasLanguage",
        	"vstoi:hasVersion", "hasco:hasMaker", "rdfs:comment", "hasco:hasImage", "vstoi:maxLoggedMeasurements", "vstoi:minOperatingTemperature", 
            "vstoi:maxOperatingTemperature", "hasco:hasOperatingTemperatureUnit", "vstoi:hasWebDocumentation"};

        // Create header row
        Row instrumentHeaderRow = instrumentSheet.createRow(0);
        for (int i = 0; i < instrumentHeaders.length; i++) {
            Cell cell = instrumentHeaderRow.createCell(i);
            cell.setCellValue(instrumentHeaders[i]);
        }
        for (int i = 0; i < instrumentHeaders.length; i++) {
            instrumentSheet.autoSizeColumn(i);
        }

        // Create sheet named 'ContainerSlots'
        Sheet containerSlotSheet = workbook.createSheet(INSGen.CONTAINER_SLOTS);
        String[] containerSlotHeaders = { "instrument", "hasco:originalID", "vstoi:belongsTo", "vstoi:hasComponent" };

        // Create header row
        Row containerSlotHeaderRow = containerSlotSheet.createRow(0);
        for (int i = 0; i < containerSlotHeaders.length; i++) {
            Cell cell = containerSlotHeaderRow.createCell(i);
            cell.setCellValue(containerSlotHeaders[i]);
        }
        for (int i = 0; i < containerSlotHeaders.length; i++) {
            containerSlotSheet.autoSizeColumn(i);
        }

        // Create sheet named 'DetectorStem'
        Sheet detectorStemSheet = workbook.createSheet(INSGen.DETECTOR_STEMS);
        String[] detectorStemHeaders = { "hasURI", "hasco:hascoType", "rdfs:subClassOf", "rdfs:label",	"vstoi:hasContent", "vstoi:hasLanguage",	
            "vstoi:hasVersion", "hasco:hasMaker", "rdfs:comment:", "hasco:hasImage", "vstoi:hasWebDocumentation" };

        // Create header row
        Row detectorStemHeaderRow = detectorStemSheet.createRow(0);
        for (int i = 0; i < detectorStemHeaders.length; i++) {
            Cell cell = detectorStemHeaderRow.createCell(i);
            cell.setCellValue(detectorStemHeaders[i]);
        }
        for (int i = 0; i < detectorStemHeaders.length; i++) {
            detectorStemSheet.autoSizeColumn(i);
        }

        // Create sheet named 'Detector'
        Sheet detectorSheet = workbook.createSheet(INSGen.DETECTORS);
        String[] detectorHeaders = { "hasURI", "hasco:hascoType", "rdf:type", "rdfs:label", "vstoi:hasDetectorStem", "vstoi:hasCodebook" };

        // Create header row
        Row detectorHeaderRow = detectorSheet.createRow(0);
        for (int i = 0; i < detectorHeaders.length; i++) {
            Cell cell = detectorHeaderRow.createCell(i);
            cell.setCellValue(detectorHeaders[i]);
        }
        for (int i = 0; i < detectorHeaders.length; i++) {
            detectorSheet.autoSizeColumn(i);
        }

        // Create sheet named 'ActuatorStem'
        Sheet actuatorStemSheet = workbook.createSheet(INSGen.ACTUATOR_STEMS);
        String[] actuatorStemHeaders = { "hasURI", "hasco:hascoType", "rdfs:subClassOf", "rdfs:label",	"vstoi:hasContent", "vstoi:hasLanguage",	
            "vstoi:hasVersion", "hasco:hasMaker", "rdfs:comment:", "hasco:hasImage", "vstoi:hasWebDocumentation" };

        // Create header row
        Row actuatorStemHeaderRow = actuatorStemSheet.createRow(0);
        for (int i = 0; i < actuatorStemHeaders.length; i++) {
            Cell cell = actuatorStemHeaderRow.createCell(i);
            cell.setCellValue(actuatorStemHeaders[i]);
        }
        for (int i = 0; i < actuatorStemHeaders.length; i++) {
            actuatorStemSheet.autoSizeColumn(i);
        }

        // Create sheet named 'Actuator'
        Sheet actuatorSheet = workbook.createSheet(INSGen.ACTUATORS);
        String[] actuatorHeaders = { "hasURI", "hasco:hascoType", "rdf:type", "rdfs:label", "vstoi:hasActuatorStem", "vstoi:hasCodebook" };

        // Create header row
        Row actuatorHeaderRow = actuatorSheet.createRow(0);
        for (int i = 0; i < actuatorHeaders.length; i++) {
            Cell cell = actuatorHeaderRow.createCell(i);
            cell.setCellValue(actuatorHeaders[i]);
        }
        for (int i = 0; i < actuatorHeaders.length; i++) {
            actuatorSheet.autoSizeColumn(i);
        }

        // Create sheet named 'CodeBook'
        Sheet codeBookSheet = workbook.createSheet(INSGen.CODEBOOKS);
        String[] codeBookHeaders = { "hasURI", "hasco:hascoType", "rdf:type", "rdfs:label", "vstoi:hasContent", "vstoi:hasLanguage", 	
            "vstoi:hasVersion", "rdfs:comment", "hasco:hasImage", "vstoi:hasWebDocumentation" };

        // Create header row
        Row codeBookHeaderRow = codeBookSheet.createRow(0);
        for (int i = 0; i < codeBookHeaders.length; i++) {
            Cell cell = codeBookHeaderRow.createCell(i);
            cell.setCellValue(codeBookHeaders[i]);
        }
        for (int i = 0; i < codeBookHeaders.length; i++) {
            codeBookSheet.autoSizeColumn(i);
        }

        // Create sheet named 'CodeBookSlot'
        Sheet codeBookSlotSheet = workbook.createSheet(INSGen.CODEBOOK_SLOTS);
        String[] codeBookSlotHeaders = { "hasURI", "hasco:hascoType", "rdf:type", "vstoi:belongsTo", "vstoi:hasResponseOption", "vstoi:hasPriority" };

        // Create header row
        Row codeBookSlotHeaderRow = codeBookSlotSheet.createRow(0);
        for (int i = 0; i < codeBookSlotHeaders.length; i++) {
            Cell cell = codeBookSlotHeaderRow.createCell(i);
            cell.setCellValue(codeBookSlotHeaders[i]);
        }
        for (int i = 0; i < codeBookSlotHeaders.length; i++) {
            codeBookSlotSheet.autoSizeColumn(i);
        }

        // Create sheet named 'ResponseOption'
        Sheet responseOptionSheet = workbook.createSheet(INSGen.RESPONSE_OPTIONS);
        String[] responseOptionHeaders = { "hasURI", "hasco:hascoType", "rdf:type", "rdfs:label", "vstoi:hasContent", "vstoi:hasLanguage", "vstoi:hasVersion", 	
            "rdfs:comment", "hasco:hasImage", "vstoi:hasWebDocumentation" };

        // Create header row
        Row responseOptionHeaderRow = responseOptionSheet.createRow(0);
        for (int i = 0; i < responseOptionHeaders.length; i++) {
            Cell cell = responseOptionHeaderRow.createCell(i);
            cell.setCellValue(responseOptionHeaders[i]);
        }
        for (int i = 0; i < responseOptionHeaders.length; i++) {
            responseOptionSheet.autoSizeColumn(i);
        }

        // Create sheet named 'Annotation'
        Sheet annotationSheet = workbook.createSheet(INSGen.ANNOTATIONS);
        String[] annotationHeaders = { "hasURI", "hasco:hascoType", "rdf:type", "rdfs:label", "vstoi:belongsTo", "vstoi:hasAnnotationStem", "vstoi:hasPosition", 
        	"vstoi:hasContentWithStyle", "rdfs:comment", "hasco:hasImage", "vstoi:hasWebDocumentation" };

        // Create header row
        Row annotationHeaderRow = annotationSheet.createRow(0);
        for (int i = 0; i < annotationHeaders.length; i++) {
            Cell cell = annotationHeaderRow.createCell(i);
            cell.setCellValue(annotationHeaders[i]);
        }
        for (int i = 0; i < annotationHeaders.length; i++) {
            annotationSheet.autoSizeColumn(i);
        }

        // Create sheet named 'AnnotationStem'
        Sheet annotationStemSheet = workbook.createSheet(INSGen.ANNOTATION_STEMS);
        String[] annotationStemHeaders = { "hasURI", "hasco:hascoType", "rdf:type", "rdfs:label", "vstoi:hasContent", "vstoi:hasLanguage", 	
            "vstoi:hasVersion", "rdfs:comment", "hasco:hasImage", "vstoi:hasWebDocumentation" };

        // Create header row
        Row annotationStemHeaderRow = annotationStemSheet.createRow(0);
        for (int i = 0; i < annotationStemHeaders.length; i++) {
            Cell cell = annotationStemHeaderRow.createCell(i);
            cell.setCellValue(annotationStemHeaders[i]);
        }
        for (int i = 0; i < annotationStemHeaders.length; i++) {
            annotationStemSheet.autoSizeColumn(i);
        }


        // Define the permanent file path
        String pathString = ConfigProp.getPathIngestion() + filename;

        // Write the workbook content to a file
        try (FileOutputStream fileOut = new FileOutputStream(pathString)) {
            workbook.write(fileOut);
            System.out.println("Empty INS workbook created successfully!");
            System.out.println("INS workbook path is [" + pathString + "]");
        } catch (IOException e) {
            System.out.println("Error occurred while writing the workbook: " + e.getMessage());
        } 
            //finally {
            // Close the workbook to release resources
            //try {
            //    workbook.close();
            //} catch (IOException e) {
            //    e.printStackTrace();
            //}
        
        return workbook;
    }


    public static String save(Workbook workbook, String filename) {
        // Define the permanent file path
        String pathString = ConfigProp.getPathIngestion() + filename;

        String resp = "";
        // Write the workbook content to a file
        try (FileOutputStream fileOut = new FileOutputStream(pathString)) {
            workbook.write(fileOut);
            System.out.println("INS workbook save successfully!");
        } catch (IOException e) {
            resp = "Error occurred while writing the workbook: " + e.getMessage();
            System.out.println("Error occurred while writing the workbook: " + e.getMessage());
        } 
            //finally {
            // Close the workbook to release resources
            //try {
            //    workbook.close();
            //} catch (IOException e) {
            //    e.printStackTrace();
            //}
        
        return resp;
    }
}

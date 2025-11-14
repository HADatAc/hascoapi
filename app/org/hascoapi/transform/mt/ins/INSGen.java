package org.hascoapi.transform.mt.ins;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.entity.pojo.Component;
import org.hascoapi.entity.pojo.ComponentStem;
import org.hascoapi.entity.pojo.Codebook;
import org.hascoapi.entity.pojo.SlotElement;
import org.hascoapi.entity.pojo.ResponseOption;
import org.hascoapi.entity.pojo.AnnotationStem;
import org.hascoapi.entity.pojo.Annotation;
import org.hascoapi.entity.pojo.GenericFindWithStatus;
import org.hascoapi.utils.ConfigProp;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class INSGen {

    public static final String INFOSHEET            = "InfoSheet";
    public static final String NAMESPACES           = "Namespaces";
    public static final String INSTRUMENTS          = "Instruments";
    public static final String SLOT_ELEMENTS        = "SlotElements";
    public static final String COMPONENT_STEMS      = "ComponentStems";
    public static final String COMPONENTS           = "Components";
    public static final String CODEBOOKS            = "CodeBooks";
    public static final String CODEBOOK_SLOTS       = "CodeBookSlots";
    public static final String RESPONSE_OPTIONS     = "ResponseOptions";
    public static final String ANNOTATIONS          = "Annotations";
    public static final String ANNOTATION_STEMS     = "AnnotationStems";

    public static final int PAGESIZE                = 20000;
    public static final int OFFSET                  = 0;

    public static String genByStatus(String status, String filename, String mediaFolder, String verifyUri) {
        INSGenHelper helper = new INSGenHelper();
        helper.workbook = INSGen.create(filename);
        String resp = "";

        GenericFindWithStatus<Instrument> instrumentQuery = new GenericFindWithStatus<Instrument>();
        List<Instrument> instruments = instrumentQuery.findByStatusWithPages(Instrument.class, status, PAGESIZE, OFFSET);
        if (instruments != null) {
            for (Instrument instrument: instruments) {
                helper = INSInstrument.add(helper,instrument);
                helper = INSSlotElement.addByInstrument(helper,instrument);
            }
        }

        GenericFindWithStatus<ComponentStem> componentStemQuery = new GenericFindWithStatus<ComponentStem>();
        List<ComponentStem> componentStems = componentStemQuery.findByStatusWithPages(ComponentStem.class, status, PAGESIZE, OFFSET);
        if (componentStems != null) {
            for (ComponentStem componentStem: componentStems) {
                helper = INSComponentStem.add(helper,componentStem);
            }
        }

        GenericFindWithStatus<Component> componentQuery = new GenericFindWithStatus<Component>();
        List<Component> components = componentQuery.findByStatusWithPages(Component.class, status, PAGESIZE, OFFSET);
        if (components != null) {
            for (Component component: components) {
                helper = INSComponent.add(helper,component);
            }
        }

        GenericFindWithStatus<Codebook> cbQuery = new GenericFindWithStatus<Codebook>();
        List<Codebook> cbs = cbQuery.findByStatusWithPages(Codebook.class, status, PAGESIZE, OFFSET);
        if (cbs != null) {
            for (Codebook cb: cbs) {
                helper = INSCodebook.add(helper,cb);
                helper = INSCodebookSlot.addByCodebook(helper,cb);
            }
        }
        GenericFindWithStatus<ResponseOption> respOptionQuery = new GenericFindWithStatus<ResponseOption>();
        List<ResponseOption> respOptions = respOptionQuery.findByStatusWithPages(ResponseOption.class, status, PAGESIZE, OFFSET);
        if (respOptions != null) {
            for (ResponseOption respOption: respOptions) {
                helper = INSResponseOption.add(helper,respOption);
            }
        }
        GenericFindWithStatus<AnnotationStem> annStemQuery = new GenericFindWithStatus<AnnotationStem>();
        List<AnnotationStem> annStems = annStemQuery.findByStatusWithPages(AnnotationStem.class, status, PAGESIZE, OFFSET);
        if (annStems != null) {
            for (AnnotationStem annStem: annStems) {
                helper = INSAnnotationStem.add(helper,annStem);
            }
        }
        GenericFindWithStatus<Annotation> annQuery = new GenericFindWithStatus<Annotation>();
        List<Annotation> anns = annQuery.findByStatusWithPages(Annotation.class, status, PAGESIZE, OFFSET);
        if (anns != null) {
            for (Annotation ann: anns) {
                helper = INSAnnotation.add(helper,ann);
            }
        }
        return INSGen.save(helper,filename);
    }

    public static String genByInstrument(Instrument instrument, String filename, String mediaFolder, String verifyUri) {
        if (instrument == null) {
            return "";
        }
        INSGenHelper helper = new INSGenHelper();
        helper.workbook = INSGen.create(filename);
        
        helper = INSInstrument.add(helper,instrument);
        helper = INSSlotElement.addByInstrument(helper,instrument);
        if (helper.components.size() > 0) {
            for (Component component : helper.components.values()) {
                helper = INSComponent.add(helper,component);
            }
        }
        if (helper.componentStems.size() > 0) {
            for (ComponentStem componentStem : helper.componentStems.values()) {
                helper = INSComponentStem.add(helper,componentStem);
            }
        }
        if (helper.codebooks.size() > 0) {
            for (Codebook codebook : helper.codebooks.values()) {
                helper = INSCodebook.add(helper,codebook);
                helper = INSCodebookSlot.addByCodebook(helper,codebook);
            }            
        }
        if (helper.respOptions.size() > 0) {
            for (ResponseOption responseOption : helper.respOptions.values()) {
                helper = INSResponseOption.add(helper,responseOption);
            }            
        }

        return INSGen.save(helper, filename);
    }

    public static String genByManager(String useremail, String status, String filename, String mediaFolder, String verifyUri) {
        INSGenHelper helper = new INSGenHelper();
        helper.workbook = INSGen.create(filename);
        boolean withCurrent = false; // this assures that the retrieval of just elements of the requested type.

        GenericFindWithStatus<Instrument> instrumentQuery = new GenericFindWithStatus<Instrument>();
        List<Instrument> instruments = instrumentQuery.findByStatusManagerEmailWithPages(Instrument.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (instruments != null) {
            for (Instrument instrument: instruments) {
                INSInstrument.add(helper,instrument);
                INSSlotElement.addByInstrument(helper,instrument);
            }
        }
        GenericFindWithStatus<ComponentStem> componentStemQuery = new GenericFindWithStatus<ComponentStem>();
        List<ComponentStem> componentStems = componentStemQuery.findByStatusManagerEmailWithPages(ComponentStem.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (componentStems != null) {
            for (ComponentStem componentStem: componentStems) {
                INSComponentStem.add(helper,componentStem);
            }
        }
        GenericFindWithStatus<Component> componentQuery = new GenericFindWithStatus<Component>();
        List<Component> components = componentQuery.findByStatusManagerEmailWithPages(Component.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (components != null) {
            for (Component component: components) {
                INSComponent.add(helper,component);
            }
        }
        GenericFindWithStatus<Codebook> cbQuery = new GenericFindWithStatus<Codebook>();
        List<Codebook> cbs = cbQuery.findByStatusManagerEmailWithPages(Codebook.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (cbs != null) {
            for (Codebook cb: cbs) {
                INSCodebook.add(helper,cb);
                INSCodebookSlot.addByCodebook(helper,cb);
            }
        }
        GenericFindWithStatus<ResponseOption> respOptionQuery = new GenericFindWithStatus<ResponseOption>();
        List<ResponseOption> respOptions = respOptionQuery.findByStatusManagerEmailWithPages(ResponseOption.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (respOptions != null) {
            for (ResponseOption respOption: respOptions) {
                INSResponseOption.add(helper,respOption);
            }
        }
        GenericFindWithStatus<AnnotationStem> annStemQuery = new GenericFindWithStatus<AnnotationStem>();
        List<AnnotationStem> annStems = annStemQuery.findByStatusManagerEmailWithPages(AnnotationStem.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (annStems != null) {
            for (AnnotationStem annStem: annStems) {
                INSAnnotationStem.add(helper,annStem);
            }
        }
        GenericFindWithStatus<Annotation> annQuery = new GenericFindWithStatus<Annotation>();
        List<Annotation> anns = annQuery.findByStatusManagerEmailWithPages(Annotation.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (anns != null) {
            for (Annotation ann: anns) {
                INSAnnotation.add(helper,ann);
            }
        }
        return INSGen.save(helper,filename);
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
        isDataCell1_2.setCellValue("#" + INSGen.NAMESPACES);

        Row dataRow2 = infoSheet.createRow(2);
        Cell isDataCell2_1 = dataRow2.createCell(0);
        isDataCell2_1.setCellValue("Instruments");
        Cell isDataCell2_2 = dataRow2.createCell(1);
        isDataCell2_2.setCellValue("#" + INSGen.INSTRUMENTS);

        Row isDataRow3 = infoSheet.createRow(3);
        Cell isDataCell3_1 = isDataRow3.createCell(0);
        isDataCell3_1.setCellValue("SlotElements");
        Cell isDataCell3_2 = isDataRow3.createCell(1);
        isDataCell3_2.setCellValue("#" + INSGen.SLOT_ELEMENTS);

        Row isDataRow4 = infoSheet.createRow(4);
        Cell isDataCell4_1 = isDataRow4.createCell(0);
        isDataCell4_1.setCellValue("ComponentStems");
        Cell isDataCell4_2 = isDataRow4.createCell(1);
        isDataCell4_2.setCellValue("#" + INSGen.COMPONENT_STEMS);

        Row isDataRow5 = infoSheet.createRow(5);
        Cell isDataCell5_1 = isDataRow5.createCell(0);
        isDataCell5_1.setCellValue("Components");
        Cell isDataCell5_2 = isDataRow5.createCell(1);
        isDataCell5_2.setCellValue("#" + INSGen.COMPONENTS);

        Row isDataRow6 = infoSheet.createRow(6);
        Cell isDataCell6_1 = isDataRow6.createCell(0);
        isDataCell6_1.setCellValue("CodeBooks");
        Cell isDataCell6_2 = isDataRow6.createCell(1);
        isDataCell6_2.setCellValue("#" + INSGen.CODEBOOKS);

        Row isDataRow7 = infoSheet.createRow(7);
        Cell isDataCell7_1 = isDataRow7.createCell(0);
        isDataCell7_1.setCellValue("CodeBookSlots");
        Cell isDataCell7_2 = isDataRow7.createCell(1);
        isDataCell7_2.setCellValue("#" + INSGen.CODEBOOK_SLOTS);

        Row isDataRow8 = infoSheet.createRow(8);
        Cell isDataCell8_1 = isDataRow8.createCell(0);
        isDataCell8_1.setCellValue("ResponseOptions");
        Cell isDataCell8_2 = isDataRow8.createCell(1);
        isDataCell8_2.setCellValue("#" + INSGen.RESPONSE_OPTIONS);

        Row isDataRow9 = infoSheet.createRow(9);
        Cell isDataCell9_1 = isDataRow9.createCell(0);
        isDataCell9_1.setCellValue("Annotations");
        Cell isDataCell9_2 = isDataRow9.createCell(1);
        isDataCell9_2.setCellValue("#" + INSGen.ANNOTATIONS);

        Row isDataRow10 = infoSheet.createRow(10);
        Cell isDataCell10_1 = isDataRow10.createCell(0);
        isDataCell10_1.setCellValue("AnnotationStems");
        Cell isDataCell10_2 = isDataRow10.createCell(1);
        isDataCell10_2.setCellValue("#" + INSGen.ANNOTATION_STEMS);

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
            "vstoi:maxOperatingTemperature", "hasco:hasOperatingTemperatureUnit", "hasco:hasWebDocument", "vstoi:hasFirst"};

        // Create header row
        Row instrumentHeaderRow = instrumentSheet.createRow(0);
        for (int i = 0; i < instrumentHeaders.length; i++) {
            Cell cell = instrumentHeaderRow.createCell(i);
            cell.setCellValue(instrumentHeaders[i]);
        }
        for (int i = 0; i < instrumentHeaders.length; i++) {
            instrumentSheet.autoSizeColumn(i);
        }

        // Create sheet named 'SlotElements'
        Sheet slotElementSheet = workbook.createSheet(INSGen.SLOT_ELEMENTS);
        // OLD: "instrument", "hasco:originalID", "vstoi:belongsTo", "rdfs:label", "vstoi:hasComponent" };
        String[] slotElementHeaders = { "hasURI", "hasco:hascoType", "vstoi:belongsTo", "vstoi:hasComponent", "vstoi:hasNext", "vstoi:hasPrevious", 
            "vstoi:hasFirst", "vstoi:hasPriority", "rdfs:label" };

        // Create header row
        Row slotElementHeaderRow = slotElementSheet.createRow(0);
        for (int i = 0; i < slotElementHeaders.length; i++) {
            Cell cell = slotElementHeaderRow.createCell(i);
            cell.setCellValue(slotElementHeaders[i]);
        }
        for (int i = 0; i < slotElementHeaders.length; i++) {
            slotElementSheet.autoSizeColumn(i);
        }

        // Create sheet named 'ComponentStem'
        Sheet componentStemSheet = workbook.createSheet(INSGen.COMPONENT_STEMS);
        String[] componentStemHeaders = { "hasURI", "hasco:hascoType", "rdfs:subClassOf", "rdfs:label",	"vstoi:hasContent", "vstoi:hasLanguage",	
            "vstoi:hasVersion", "hasco:hasMaker", "rdfs:comment:", "hasco:hasImage", "hasco:hasWebDocument" };

        // Create header row
        Row componentStemHeaderRow = componentStemSheet.createRow(0);
        for (int i = 0; i < componentStemHeaders.length; i++) {
            Cell cell = componentStemHeaderRow.createCell(i);
            cell.setCellValue(componentStemHeaders[i]);
        }
        for (int i = 0; i < componentStemHeaders.length; i++) {
            componentStemSheet.autoSizeColumn(i);
        }

        // Create sheet named 'Component'
        Sheet componentSheet = workbook.createSheet(INSGen.COMPONENTS);
        String[] componentHeaders = { "hasURI", "hasco:hascoType", "rdf:type", "rdfs:label", "vstoi:hasComponentStem", "vstoi:hasCodebook", 
            "vstoi:isAttributeOf" , "hasco:hasWebDocument"};

        // Create header row
        Row componentHeaderRow = componentSheet.createRow(0);
        for (int i = 0; i < componentHeaders.length; i++) {
            Cell cell = componentHeaderRow.createCell(i);
            cell.setCellValue(componentHeaders[i]);
        }
        for (int i = 0; i < componentHeaders.length; i++) {
            componentSheet.autoSizeColumn(i);
        }

        // Create sheet named 'CodeBook'
        Sheet codeBookSheet = workbook.createSheet(INSGen.CODEBOOKS);
        String[] codeBookHeaders = { "hasURI", "hasco:hascoType", "rdf:type", "rdfs:label", "vstoi:hasContent", "vstoi:hasLanguage", 	
            "vstoi:hasVersion", "rdfs:comment", "hasco:hasImage", "hasco:hasWebDocument" };

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
            "hasco:hasMaker", "rdfs:comment", "hasco:hasImage", "hasco:hasWebDocument" };

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
        	"vstoi:hasContentWithStyle", "rdfs:comment", "hasco:hasImage", "hasco:hasWebDocument" };

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
            "vstoi:hasVersion", "rdfs:comment", "hasco:hasImage", "hasco:hasWebDocument" };

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


    public static String save(INSGenHelper helper, String filename) {
        // Define the permanent file path
        String pathString = ConfigProp.getPathIngestion() + filename;

        String resp = "";
        // Write the workbook content to a file
        try (FileOutputStream fileOut = new FileOutputStream(pathString)) {
            helper.workbook.write(fileOut);
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

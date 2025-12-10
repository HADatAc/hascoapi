package org.hascoapi.transform.mt.dsg;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.GenericFindWithStatus;
import org.hascoapi.entity.pojo.NameSpace;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hascoapi.utils.IngestionLogger;

/*
DSGGen builds an Excel workbook for studies:
genByStatus queries studies by status,
create initializes the workbook (InfoSheet + SSD/STD/VD + Namespaces),
STD/SSD rows are added per study,
InfoSheet references are set (first study URI/title and first namespace URI),
Namespaces is populated from the helper map or in-memory namespaces, and save writes and closes the file.
*/


public class DSGGen {

    public static final String INFOSHEET            = "InfoSheet";
    public static final String NAMESPACES           = "Namespaces";
    public static final String SSD                  = "SSD";
    public static final String STD                  = "STD";
    public static final String VD                   = "VD";

    public static final int PAGESIZE                = 20000;
    public static final int OFFSET                  = 0;

    public static String genByStatus(String status, String filename, String mediaFolder, String verifyUri) {
        System.out.println("[DSGGen] genByStatus START status=" + status + ", filename=" + filename);
        DSGGenHelper helper = new DSGGenHelper();
        List<Study> studies = null;
        try {
            GenericFindWithStatus<Study> studyQuery = new GenericFindWithStatus<>();
            System.out.println("[DSGGen] Querying ALL studies with pageSize=" + PAGESIZE + ", offset=" + OFFSET);
            // Per request: do not filter by status; get all studies
            studies = (List<Study>) (List<?>) studyQuery.findAllStudiesWithPages(PAGESIZE, OFFSET);
            System.out.println("[DSGGen] Retrieved studies count=" + (studies == null ? 0 : studies.size()));
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR fetching studies: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: fetching studies - " + t.getMessage();
        }

        try {
            helper.workbook = DSGGen.create(filename, studies);
            if (helper.workbook == null) {
                System.err.println("[DSGGen] ERROR: workbook creation returned null");
                return "FAILURE: workbook creation returned null";
            }
            System.out.println("[DSGGen] Workbook created");
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR creating workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: creating workbook - " + t.getMessage();
        }

        if (studies != null && !studies.isEmpty()) {
            System.out.println("[DSGGen] Iterating studies to populate STD/SSD");
            int idx = 0;
            for (Study study: studies) {
                idx++;
                if (study == null) {
                    System.out.println("[DSGGen] WARN: study[" + idx + "] is null, skipping");
                    continue;
                }
                System.out.println("[DSGGen] Processing study[" + idx + "] uri=" + study.getUri() + ", title=" + study.getTitle());
                try {
                    helper = DSGSTD.add(helper, study);
                    System.out.println("[DSGGen] STD added for study[" + idx + "]");
                } catch (Throwable t) {
                    System.err.println("[DSGGen] ERROR adding STD for study uri=" + study.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
                try {
                    helper = DSGSSD.addByStudy(helper, study);
                    System.out.println("[DSGGen] SSD added for study[" + idx + "]");
                } catch (Throwable t) {
                    System.err.println("[DSGGen] ERROR adding SSD for study uri=" + study.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
            }
        } else {
            System.out.println("[DSGGen] No studies found for status=" + status + "; STD/SSD population skipped");
        }

        String saveResult;
        try {
            saveResult = DSGGen.save(helper, filename);
            System.out.println("[DSGGen] Save result=" + saveResult);
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR saving workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: saving workbook - " + t.getMessage();
        }
        System.out.println("[DSGGen] genByStatus END");
        return saveResult;
    }

    public static String genByStudy(Study study, String filename, String mediaFolder, String verifyUri) {
        System.out.println("[DSGGen] genByStudy START filename=" + filename);
        DSGGenHelper helper = new DSGGenHelper();
        if (study == null) {
            System.err.println("[DSGGen] ERROR: study is null");
            return "FAILURE: study is null";
        }
        try {
            java.util.List<Study> studies = new java.util.ArrayList<>();
            studies.add(study);
            helper.workbook = DSGGen.create(filename, studies);
            if (helper.workbook == null) {
                System.err.println("[DSGGen] ERROR: workbook creation returned null");
                return "FAILURE: workbook creation returned null";
            }
            System.out.println("[DSGGen] Workbook created for single study uri=" + study.getUri());
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR creating workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: creating workbook - " + t.getMessage();
        }

        try {
            helper = DSGSTD.add(helper, study);
            System.out.println("[DSGGen] STD added");
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR adding STD: " + t.getMessage());
            t.printStackTrace();
        }
        try {
            helper = DSGSSD.addByStudy(helper, study);
            System.out.println("[DSGGen] SSD added");
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR adding SSD: " + t.getMessage());
            t.printStackTrace();
        }

        String saveResult;
        try {
            saveResult = DSGGen.save(helper, filename);
            System.out.println("[DSGGen] Save result=" + saveResult);
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR saving workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: saving workbook - " + t.getMessage();
        }
        System.out.println("[DSGGen] genByStudy END");
        return saveResult;
    }

    public static String genByManager(String useremail, String status, String filename, String mediaFolder, String verifyUri) {
        IngestionLogger logger = new IngestionLogger((org.hascoapi.entity.pojo.MessageTopic) null);
        logger.println("[DSGGen] genByManager START status=" + status + ", useremail=" + useremail + ", filename=" + filename);

        DSGGenHelper helper = new DSGGenHelper();
        java.util.List<Study> studies = null;
        boolean withCurrent = false; // retrieve just the elements of the requested status
        try {
            GenericFindWithStatus<Study> studyQuery = new GenericFindWithStatus<>();
            logger.println("[DSGGen] Querying studies by manager with pageSize=" + PAGESIZE + ", offset=" + OFFSET);
            studies = (java.util.List<Study>) (java.util.List<?>) studyQuery.findByStatusManagerEmailWithPages(Study.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
            logger.println("[DSGGen] Retrieved studies count=" + (studies == null ? 0 : studies.size()));
        } catch (Throwable t) {
            logger.printExceptionById("GBL_00032");
            logger.printException("[DSGGen] ERROR fetching studies by manager: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: fetching studies by manager - " + t.getMessage();
        }

        try {
            helper.workbook = DSGGen.create(filename, studies);
            if (helper.workbook == null) {
                logger.printException("[DSGGen] ERROR: workbook creation returned null");
                return "FAILURE: workbook creation returned null";
            }
            logger.println("[DSGGen] Workbook created");
        } catch (Throwable t) {
            logger.printException("[DSGGen] ERROR creating workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: creating workbook - " + t.getMessage();
        }

        if (studies != null && !studies.isEmpty()) {
            logger.println("[DSGGen] Iterating studies to populate STD/SSD");
            int idx = 0;
            for (Study study : studies) {
                idx++;
                if (study == null) {
                    logger.printWarning("[DSGGen] WARN: study[" + idx + "] is null, skipping");
                    continue;
                }
                logger.println("[DSGGen] Processing study[" + idx + "] uri=" + study.getUri() + ", title=" + study.getTitle());
                try {
                    helper = DSGSTD.add(helper, study);
                    logger.println("[DSGGen] STD added for study[" + idx + "]");
                } catch (Throwable t) {
                    logger.printException("[DSGGen] ERROR adding STD for study uri=" + study.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
                try {
                    helper = DSGSSD.addByStudy(helper, study);
                    logger.println("[DSGGen] SSD added for study[" + idx + "]");
                } catch (Throwable t) {
                    logger.printException("[DSGGen] ERROR adding SSD for study uri=" + study.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
            }
        } else {
            logger.printWarningByIdWithArgs("GBL_00003", "Studies");
            logger.println("[DSGGen] No studies found for manager/status; STD/SSD population skipped");
        }

        String saveResult;
        try {
            saveResult = DSGGen.save(helper, filename);
            logger.println("[DSGGen] Save result=" + saveResult);
        } catch (Throwable t) {
            logger.printException("[DSGGen] ERROR saving workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: saving workbook - " + t.getMessage();
        }
        logger.println("[DSGGen] genByManager END");
        return saveResult;
    }

    public static Workbook create(String filename, List<Study> studies) {
        // Create a new workbook
        Workbook workbook = new XSSFWorkbook();

        // Create 'InfoSheet'
        Sheet infoSheet = workbook.createSheet(DSGGen.INFOSHEET);

        // Header for InfoSheet
        Row isHeaderRow = infoSheet.createRow(0);
        isHeaderRow.createCell(0).setCellValue("Attribute");
        isHeaderRow.createCell(1).setCellValue("Value");

        // Dependency rows
        Row dataRow1 = infoSheet.createRow(1);
        dataRow1.createCell(0).setCellValue("hasDependencies");
        dataRow1.createCell(1).setCellValue("#" + DSGGen.NAMESPACES);

        Row dataRow2 = infoSheet.createRow(2);
        dataRow2.createCell(0).setCellValue("hasStudyURI");
        String studyUri = "";
        if (studies != null && !studies.isEmpty() && studies.get(0) != null) {
            Study first = studies.get(0);
            studyUri = first.getUri() != null && !first.getUri().isEmpty() ? first.getUri() : safe(first.getTitle());
        }
        dataRow2.createCell(1).setCellValue(studyUri);

        Row dataRow3 = infoSheet.createRow(3);
        dataRow3.createCell(0).setCellValue("hasStudyKG");
        // Use namespace prefix (label) instead of URI
        Map<String, NameSpace> nsMap = DSGGenHelper.getNamespaces();
        String firstNamespacePrefix = "";
        if (nsMap != null && !nsMap.isEmpty()) {
            NameSpace ns = nsMap.values().iterator().next();
            if (ns != null) {
                String label = ns.getLabel();
                firstNamespacePrefix = (label != null && !label.isEmpty()) ? label : safe(ns.toString());
            }
        } else {
            List<NameSpace> inMem = NameSpace.findInMemory();
            if (inMem != null && !inMem.isEmpty()) {
                NameSpace ns = inMem.get(0);
                if (ns != null) {
                    String label = ns.getLabel();
                    firstNamespacePrefix = (label != null && !label.isEmpty()) ? label : safe(ns.toString());
                }
            }
        }
        dataRow3.createCell(1).setCellValue(firstNamespacePrefix);

        Row dataRow4 = infoSheet.createRow(4);
        dataRow4.createCell(0).setCellValue("hasStudyDescription");
        dataRow4.createCell(1).setCellValue("#" + DSGGen.STD);

        Row dataRow5 = infoSheet.createRow(5);
        dataRow5.createCell(0).setCellValue("hasEntityDesign");
        dataRow5.createCell(1).setCellValue("#" + DSGGen.SSD);

        Row dataRow6 = infoSheet.createRow(6);
        dataRow6.createCell(0).setCellValue("hasVariableDesign");
        dataRow6.createCell(1).setCellValue("#" + DSGGen.VD);

        Row dataRow7 = infoSheet.createRow(7);
        dataRow7.createCell(0).setCellValue("hasVersion");
        dataRow7.createCell(1).setCellValue("1"); // Placeholder version

        // Create sheet named 'Namespaces'
        Sheet nsSheet = workbook.createSheet(DSGGen.NAMESPACES);
        String[] nsHeaders = { "hasPrefix", "hasNameSpace", "hasFormat", "hasSource" };

        // Header row
        Row nsHeaderRow = nsSheet.createRow(0);
        for (int i = 0; i < nsHeaders.length; i++) {
            nsHeaderRow.createCell(i).setCellValue(nsHeaders[i]);
        }
        for (int i = 0; i < nsHeaders.length; i++) {
            nsSheet.autoSizeColumn(i);
        }

        // Populate namespace rows: prefer helper map, else in-memory ordered namespaces
        int nsRowNum = 1;
        if (nsMap != null && !nsMap.isEmpty()) {
            for (NameSpace ns : nsMap.values()) {
                Row row = nsSheet.createRow(nsRowNum++);
                row.createCell(0).setCellValue(safe(ns.getLabel()));       // hasPrefix
                row.createCell(1).setCellValue(safe(ns.getUri()));         // hasNameSpace
                row.createCell(2).setCellValue(safe(ns.getSourceMime()));  // hasFormat
                row.createCell(3).setCellValue(safe(ns.getSource()));      // hasSource
            }
        } else {
            List<NameSpace> inMem = NameSpace.findInMemory();
            if (inMem != null) {
                for (NameSpace ns : inMem) {
                    Row row = nsSheet.createRow(nsRowNum++);
                    row.createCell(0).setCellValue(safe(ns.getLabel()));
                    row.createCell(1).setCellValue(safe(ns.getUri()));
                    row.createCell(2).setCellValue(safe(ns.getSourceMime()));
                    row.createCell(3).setCellValue(safe(ns.getSource()));
                }
            }
        }

        // Create data sheets
        Sheet ssdSheet = workbook.createSheet(DSGGen.SSD);
        Sheet stdSheet = workbook.createSheet(DSGGen.STD);
        Sheet vdSheet = workbook.createSheet(DSGGen.VD);

        // Initialize STD headers
        Row stdHeaderRow = stdSheet.createRow(0);
        stdHeaderRow.createCell(0).setCellValue("Study ID");
        stdHeaderRow.createCell(1).setCellValue("Title");
        stdHeaderRow.createCell(2).setCellValue("Specific Aims");
        stdHeaderRow.createCell(3).setCellValue("Significance");
        stdHeaderRow.createCell(4).setCellValue("Institution");
        stdHeaderRow.createCell(5).setCellValue("Principal Investigator");
        stdHeaderRow.createCell(6).setCellValue("PI Address");
        stdHeaderRow.createCell(7).setCellValue("PI City");
        stdHeaderRow.createCell(8).setCellValue("PI State");
        stdHeaderRow.createCell(9).setCellValue("PI Zip Code");
        stdHeaderRow.createCell(10).setCellValue("Email");
        stdHeaderRow.createCell(11).setCellValue("PI Phone");
        stdHeaderRow.createCell(12).setCellValue("Co-PI 1 First Name");
        stdHeaderRow.createCell(13).setCellValue("Co-PI 1 Last Name");
        stdHeaderRow.createCell(14).setCellValue("Co-PI 1 Email");
        stdHeaderRow.createCell(15).setCellValue("Co-PI 2 First Name");
        stdHeaderRow.createCell(16).setCellValue("Co-PI 2 Last Name");
        stdHeaderRow.createCell(17).setCellValue("Co-PI 2 Email");
        stdHeaderRow.createCell(18).setCellValue("Contact First Name");
        stdHeaderRow.createCell(19).setCellValue("Contact Last Name");
        stdHeaderRow.createCell(20).setCellValue("Contact Email");
        stdHeaderRow.createCell(21).setCellValue("Project Created Date");
        stdHeaderRow.createCell(22).setCellValue("Project Last Updated Date");
        stdHeaderRow.createCell(23).setCellValue("DC Access?");

        // Initialize SSD headers
        Row ssdHeaderRow = ssdSheet.createRow(0);
        ssdHeaderRow.createCell(0).setCellValue("sheet");
        ssdHeaderRow.createCell(1).setCellValue("hasURI");
        ssdHeaderRow.createCell(2).setCellValue("type");
        ssdHeaderRow.createCell(3).setCellValue("hasSOCReference");
        ssdHeaderRow.createCell(4).setCellValue("comment");
        ssdHeaderRow.createCell(5).setCellValue("label");
        ssdHeaderRow.createCell(6).setCellValue("definition");
        ssdHeaderRow.createCell(7).setCellValue("groundingLabel");
        ssdHeaderRow.createCell(8).setCellValue("hasScope");
        ssdHeaderRow.createCell(9).setCellValue("hasTimeScope");
        ssdHeaderRow.createCell(10).setCellValue("hasSpaceScope");
        ssdHeaderRow.createCell(11).setCellValue("source");

        // Initialize VD headers (basic variable design template)
        Row vdHeaderRow = vdSheet.createRow(0);
        vdHeaderRow.createCell(0).setCellValue("sheet");
        vdHeaderRow.createCell(1).setCellValue("hasURI");
        vdHeaderRow.createCell(2).setCellValue("variableLabel");
        vdHeaderRow.createCell(3).setCellValue("variableDefinition");
        vdHeaderRow.createCell(4).setCellValue("hasUnit");
        vdHeaderRow.createCell(5).setCellValue("hasCodebook");
        vdHeaderRow.createCell(6).setCellValue("hasAttribute");
        vdHeaderRow.createCell(7).setCellValue("hasAttributeOf");
        vdHeaderRow.createCell(8).setCellValue("hasScale");
        vdHeaderRow.createCell(9).setCellValue("isAbout");

        return workbook;
    }

    private static String safe(String v) { return v == null ? "" : v; }

    public static String save(DSGGenHelper helper, String filename) {
        String basePath;
        try {
            basePath = org.hascoapi.utils.ConfigProp.getPathIngestion();
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR reading ingestion path: " + t.getMessage());
            t.printStackTrace();
            basePath = ""; // fallback to current working directory
        }
        String pathString = (basePath == null ? "" : basePath) + filename;
        String resp = "SUCCESS";
        System.out.println("[DSGGen] Saving workbook to: " + pathString);
        try (FileOutputStream fileOut = new FileOutputStream(pathString)) {
            if (helper == null || helper.workbook == null) {
                System.err.println("[DSGGen] ERROR: helper or workbook is null");
                return "FAILURE: helper or workbook is null";
            }
            helper.workbook.write(fileOut);
            System.out.println("[DSGGen] DSG workbook saved successfully!");
        } catch (IOException e) {
            resp = "FAILURE: Error writing workbook - " + e.getMessage();
            System.err.println("[DSGGen] " + resp);
        } finally {
            try {
                if (helper != null && helper.workbook != null) {
                    helper.workbook.close();
                }
            } catch (IOException ioe) {
                System.err.println("[DSGGen] WARN: error closing workbook: " + ioe.getMessage());
            }
        }
        return resp;
    }
}

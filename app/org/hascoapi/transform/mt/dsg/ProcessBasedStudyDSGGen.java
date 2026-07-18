package org.hascoapi.transform.mt.dsg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.hascoapi.entity.pojo.ProcessBasedStudy;
import org.hascoapi.entity.pojo.Process;
import org.hascoapi.entity.pojo.Task;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;

/**
 * ProcessBasedStudyDSGGen - Generate DSG Excel from ProcessBasedStudy
 * 
 * This generator creates a formal DSG study template from a ProcessBasedStudy instance.
 * It uses the Process/Workflow structure to populate study design elements.
 * 
 * Related to: PMSR ProcessBasedStudy migration - Phase 3 (DSG Generation)
 */
public class ProcessBasedStudyDSGGen {

    public static final String INFOSHEET = "InfoSheet";
    public static final String NAMESPACES = "Namespaces";
    public static final String SSD = "SSD";
    public static final String STD = "STD";
    public static final String VD = "VD";

    /**
     * Generate DSG Excel file from ProcessBasedStudy
     * 
     * @param study ProcessBasedStudy instance
     * @param filename Output filename
     * @param mediaFolder Output directory
     * @return Path to generated file or null on failure
     */
    public static String generateFromProcessBasedStudy(ProcessBasedStudy study, String filename, String mediaFolder) {
        System.out.println("[ProcessBasedStudyDSGGen] START: study=" + study.getUri());
        
        if (study == null || study.getUri() == null) {
            System.out.println("[ProcessBasedStudyDSGGen] ERROR: Invalid study");
            return null;
        }

        try {
            // Create workbook
            DSGGenHelper helper = new DSGGenHelper();
            helper.workbook = new XSSFWorkbook();
            
            // Create sheets in order
            createInfoSheet(helper, study);
            createNamespacesSheet(helper);
            createSTDSheet(helper, study);
            createSSDSheet(helper, study);
            // VD sheet not needed for ProcessBasedStudy (WKF provides structure)
            
            // Save workbook
            String outputPath = mediaFolder + File.separator + filename;
            FileOutputStream out = new FileOutputStream(outputPath);
            helper.workbook.write(out);
            out.close();
            helper.workbook.close();
            
            System.out.println("[ProcessBasedStudyDSGGen] SUCCESS: Generated " + outputPath);
            return outputPath;
            
        } catch (Exception e) {
            System.out.println("[ProcessBasedStudyDSGGen] ERROR: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create InfoSheet with study reference
     */
    private static void createInfoSheet(DSGGenHelper helper, ProcessBasedStudy study) {
        Sheet sheet = helper.workbook.createSheet(INFOSHEET);
        
        // Header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Attribute");
        headerRow.createCell(1).setCellValue("Value");
        
        // hasStudyURI
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("hasStudyURI");
        String studyUriAbbrev = URIUtils.replaceNameSpaceEx(safe(study.getUri()));
        row1.createCell(1).setCellValue(studyUriAbbrev);
        
        // hasStudyKG
        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("hasStudyKG");
        row2.createCell(1).setCellValue("pmsr"); // or from config
        
        // Dependencies
        Row row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue("hasDependency");
        row3.createCell(1).setCellValue("Namespaces;STD;SSD");
        
        System.out.println("[ProcessBasedStudyDSGGen] Created InfoSheet");
    }

    /**
     * Create Namespaces sheet with standard prefixes
     */
    private static void createNamespacesSheet(DSGGenHelper helper) {
        Sheet sheet = helper.workbook.createSheet(NAMESPACES);
        
        // Header
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Prefix");
        headerRow.createCell(1).setCellValue("URI");
        
        // Add standard namespaces
        String[][] namespaces = {
            {"hasco", "http://hadatac.org/ont/hasco#"},
            {"vstoi", "http://hadatac.org/ont/vstoi#"},
            {"pmsr", "http://pmsr.net/ont/pmsr#"},
            {"sio", "http://semanticscience.org/resource/"},
            {"schema", "http://schema.org/"}
        };
        
        int rowNum = 1;
        for (String[] ns : namespaces) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(ns[0]);
            row.createCell(1).setCellValue(ns[1]);
        }
        
        System.out.println("[ProcessBasedStudyDSGGen] Created Namespaces sheet");
    }

    /**
     * Create STD sheet with ProcessBasedStudy metadata
     */
    private static void createSTDSheet(DSGGenHelper helper, ProcessBasedStudy study) {
        Sheet sheet = helper.workbook.createSheet(STD);
        
        // Header row (same as DSGSTD)
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Study ID");
        headerRow.createCell(1).setCellValue("Title");
        headerRow.createCell(2).setCellValue("Specific Aims");
        headerRow.createCell(3).setCellValue("Significance");
        headerRow.createCell(4).setCellValue("Institution");
        headerRow.createCell(5).setCellValue("Principal Investigator");
        headerRow.createCell(6).setCellValue("PI Address");
        headerRow.createCell(7).setCellValue("PI City");
        headerRow.createCell(8).setCellValue("PI State");
        headerRow.createCell(9).setCellValue("PI Zip Code");
        headerRow.createCell(10).setCellValue("Email");
        headerRow.createCell(11).setCellValue("PI Phone");
        headerRow.createCell(12).setCellValue("Co-PI 1 First Name");
        headerRow.createCell(13).setCellValue("Co-PI 1 Last Name");
        headerRow.createCell(14).setCellValue("Co-PI 1 Email");
        headerRow.createCell(15).setCellValue("Co-PI 2 First Name");
        headerRow.createCell(16).setCellValue("Co-PI 2 Last Name");
        headerRow.createCell(17).setCellValue("Co-PI 2 Email");
        headerRow.createCell(18).setCellValue("Contact First Name");
        headerRow.createCell(19).setCellValue("Contact Last Name");
        headerRow.createCell(20).setCellValue("Contact Email");
        headerRow.createCell(21).setCellValue("Project Created Date");
        headerRow.createCell(22).setCellValue("Project Last Updated Date");
        headerRow.createCell(23).setCellValue("DC Access?");
        
        // Data row - use ProcessBasedStudy metadata
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue(safe(study.getStudyID()));
        row.createCell(1).setCellValue(safe(study.getStudyTitle()));
        row.createCell(2).setCellValue(safe(study.getSpecificAims()));
        row.createCell(3).setCellValue(safe(study.getSignificance()));
        row.createCell(4).setCellValue(safe(study.getInstitutionName()));
        row.createCell(5).setCellValue(safe(study.getPrincipalInvestigator()));
        row.createCell(6).setCellValue(""); // PI Address - not in ProcessBasedStudy
        row.createCell(7).setCellValue(""); // PI City
        row.createCell(8).setCellValue(""); // PI State
        row.createCell(9).setCellValue(""); // PI Zip
        row.createCell(10).setCellValue(safe(study.getContactEmail()));
        row.createCell(11).setCellValue(""); // PI Phone
        row.createCell(12).setCellValue(""); // Co-PI 1 First
        row.createCell(13).setCellValue(""); // Co-PI 1 Last
        row.createCell(14).setCellValue(""); // Co-PI 1 Email
        row.createCell(15).setCellValue(""); // Co-PI 2 First
        row.createCell(16).setCellValue(""); // Co-PI 2 Last
        row.createCell(17).setCellValue(""); // Co-PI 2 Email
        row.createCell(18).setCellValue(""); // Contact First
        row.createCell(19).setCellValue(""); // Contact Last
        row.createCell(20).setCellValue(safe(study.getContactEmail()));
        row.createCell(21).setCellValue(safe(study.getStartDate()));
        row.createCell(22).setCellValue(safe(study.getEndDate()));
        row.createCell(23).setCellValue("");
        
        System.out.println("[ProcessBasedStudyDSGGen] Created STD sheet");
    }

    /**
     * Create SSD sheet with Process structure
     * Maps Process/Tasks to study scope chains
     */
    private static void createSSDSheet(DSGGenHelper helper, ProcessBasedStudy study) {
        Sheet sheet = helper.workbook.createSheet(SSD);
        
        // Header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("SOC ID");
        headerRow.createCell(1).setCellValue("Study ID");
        headerRow.createCell(2).setCellValue("SOC Label");
        headerRow.createCell(3).setCellValue("SOC Comment");
        headerRow.createCell(4).setCellValue("hasEntity");
        headerRow.createCell(5).setCellValue("isVirtual");
        headerRow.createCell(6).setCellValue("hasRole");
        headerRow.createCell(7).setCellValue("hasTimeScopeUri");
        headerRow.createCell(8).setCellValue("hasSpaceScopeUri");
        headerRow.createCell(9).setCellValue("groundingScopeUri");
        
        // Get Process from study
        String processUri = study.getProcessUri();
        if (processUri == null || processUri.isEmpty()) {
            System.out.println("[ProcessBasedStudyDSGGen] WARNING: No process URI in study");
            return;
        }
        
        Process process = Process.find(processUri);
        if (process == null) {
            System.out.println("[ProcessBasedStudyDSGGen] WARNING: Process not found: " + processUri);
            return;
        }
        
        // Generate SOC from Process
        // For now, create one SOC per Process representing the workflow execution context
        int rowNum = 1;
        Row row = sheet.createRow(rowNum);
        
        String processLabel = process.getLabel() != null ? process.getLabel() : "Process";
        String socId = "SOC-" + processLabel.toUpperCase().replaceAll("[^A-Z0-9]", "-");
        
        row.createCell(0).setCellValue(socId);
        row.createCell(1).setCellValue(safe(study.getStudyID()));
        row.createCell(2).setCellValue("Process Execution: " + processLabel);
        row.createCell(3).setCellValue("Study object collection representing workflow execution");
        row.createCell(4).setCellValue("vstoi:Process"); // Entity type
        row.createCell(5).setCellValue("false"); // Not virtual
        row.createCell(6).setCellValue("vstoi:ProcessRole"); // Role type
        row.createCell(7).setCellValue(""); // Time scope - could derive from study dates
        row.createCell(8).setCellValue(""); // Space scope
        row.createCell(9).setCellValue(""); // Grounding scope
        
        System.out.println("[ProcessBasedStudyDSGGen] Created SSD sheet with " + rowNum + " SOCs");
    }

    private static String safe(String val) {
        return val == null ? "" : val;
    }
}

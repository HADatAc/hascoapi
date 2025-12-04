package org.hascoapi.transform.mt.dsg;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.GenericFindWithStatus;
import org.hascoapi.utils.ConfigProp;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class DSGGen {

    public static final String INFOSHEET            = "InfoSheet";
    public static final String NAMESPACES           = "Namespaces";
    public static final String SSD                  = "SSD";
    public static final String STD                  = "STD";
    public static final String VD                   = "VD";
    // Removida a constante SOC_NHANES_SUBJECTS

    public static final int PAGESIZE                = 20000;
    public static final int OFFSET                  = 0;

    public static String genByStatus(String status, String filename, String mediaFolder, String verifyUri) {
        DSGGenHelper helper = new DSGGenHelper();
        helper.workbook = DSGGen.create(filename);
        String resp = "";

        // O DSG é a união de SSD e STD. O ponto de partida é o Study (STD).
        GenericFindWithStatus<Study> studyQuery = new GenericFindWithStatus<Study>();
        List<Study> studies = studyQuery.findByStatusWithPages(Study.class, status, PAGESIZE, OFFSET);

        if (studies != null) {
            for (Study study: studies) {
                // 1. Adiciona o Study (STD)
                helper = DSGSTD.add(helper, study);

                // 2. Adiciona os EntityDesigns (SSD) associados ao Study
                // A implementação de addByStudy em DSGSSD deve buscar os EntityDesigns
                helper = DSGSSD.addByStudy(helper, study);


                // 4. Removida a lógica de DataAcquisition (folha de dados)
            }
        }

        return DSGGen.save(helper, filename);
    }

    // Removido o metodo genByInstrument e genByManager, pois não foram solicitados.

    public static Workbook create(String filename) {

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
        dataRow2.createCell(1).setCellValue(""); // placeholder; set later by DSGSTD.add

        Row dataRow3 = infoSheet.createRow(3);
        dataRow3.createCell(0).setCellValue("hasStudyKG");
        dataRow3.createCell(1).setCellValue("nhanes");

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
        dataRow7.createCell(1).setCellValue("1");

        // Create 'Namespaces' sheet
        Sheet nsSheet = workbook.createSheet(DSGGen.NAMESPACES);
        Row nsHeaderRow = nsSheet.createRow(0);
        nsHeaderRow.createCell(0).setCellValue("hasPrefix");
        nsHeaderRow.createCell(1).setCellValue("hasNameSpace");

        // Create data sheets
        workbook.createSheet(DSGGen.SSD);
        workbook.createSheet(DSGGen.STD);
        workbook.createSheet(DSGGen.VD);

        return workbook;
    }

    public static String save(DSGGenHelper helper, String filename) {
        try {
            FileOutputStream fileOut = new FileOutputStream(filename);
            helper.workbook.write(fileOut);
            fileOut.close();
            helper.workbook.close();
            return "SUCCESS";
        } catch (IOException e) {
            e.printStackTrace();
            return "FAILURE: " + e.getMessage();
        }
    }
}

package org.hascoapi.transform.mt.dsg;

import java.util.List;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.GenericFindWithStatus;
import org.apache.poi.ss.usermodel.*;

public class DSGSTD {

    public static DSGGenHelper add(DSGGenHelper helper, Study study) {
        Sheet sheet = helper.workbook.getSheet(DSGGen.STD);
        if (sheet == null) {
            sheet = helper.workbook.createSheet(DSGGen.STD);
            // Colunas baseadas na estrutura de metadados de Study (STD)
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
        }
        /*
        // Lógica para adicionar o Study (STD)
        int rowNum = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(study.getUri());
        row.createCell(1).setCellValue(study.getLabel());
        row.createCell(2).setCellValue(study.getHasVersion()); 
        row.createCell(3).setCellValue(study.getHasStatus());
        row.createCell(4).setCellValue(study.getTitle());
        row.createCell(5).setCellValue(study.getProject());
        row.createCell(6).setCellValue(study.getExternalSource());
        row.createCell(7).setCellValue(study.getInstitutionUri());
        row.createCell(8).setCellValue(study.getPiUri());
        row.createCell(9).setCellValue(study.getStartedAt());
        row.createCell(10).setCellValue(study.getEndedAt());
        row.createCell(11).setCellValue(study.getHasSIRManagerEmail());
        row.createCell(12).setCellValue(study.getComment()); // Assumindo que hasDescription é getComment()
        row.createCell(13).setCellValue(study.getPurpose());
        row.createCell(14).setCellValue(study.getMethod());
        row.createCell(15).setCellValue(study.getLimitations());
        row.createCell(16).setCellValue(study.getContact());
        row.createCell(17).setCellValue(study.getFunding());
        row.createCell(18).setCellValue(study.getLicense());
        row.createCell(19).setCellValue(study.getAccess());
        row.createCell(20).setCellValue(study.getCitation());
        row.createCell(21).setCellValue(study.getPublication());
        row.createCell(22).setCellValue(study.getDataFileUri()); // Assumindo que hasDataFile é getDataFileUri()
        row.createCell(23).setCellValue(study.getDataAcquisitionUri()); // Assumindo que hasDataAcquisition é getDataAcquisitionUri()


         */
        return helper;
    }

    public static DSGGenHelper addByStatus(DSGGenHelper helper, String status) {
        // A lógica de busca real deve ser implementada aqui, usando GenericFindWithStatus<Study>
        return helper;
    }
}

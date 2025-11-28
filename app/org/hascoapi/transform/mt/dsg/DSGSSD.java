package org.hascoapi.transform.mt.dsg;

import java.util.List;

import org.hascoapi.entity.pojo.SDD;
import org.hascoapi.entity.pojo.SemanticDataDictionary;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.GenericFindWithStatus;
import org.apache.poi.ss.usermodel.*;

public class DSGSSD {

    public static DSGGenHelper add(DSGGenHelper helper, SemanticDataDictionary sdd) {
        Sheet sheet = helper.workbook.getSheet(DSGGen.SSD);
        String ssd =  DSGGen.SSD;
        if (sheet == null) {
            sheet = helper.workbook.createSheet(DSGGen.SSD);
            // Colunas baseadas no Excel SSD (9 linhas x 12 colunas)
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("sheet");
            headerRow.createCell(1).setCellValue("hasURI");
            headerRow.createCell(2).setCellValue("type");
            headerRow.createCell(3).setCellValue("hasSOCReference");
            headerRow.createCell(4).setCellValue("comment");
            headerRow.createCell(5).setCellValue("label");
            headerRow.createCell(6).setCellValue("definition");
            headerRow.createCell(7).setCellValue("groundingLabel");
            headerRow.createCell(8).setCellValue("hasScope");
            headerRow.createCell(9).setCellValue("hasTimeScope");
            headerRow.createCell(10).setCellValue("hasSpaceScope");
            headerRow.createCell(11).setCellValue("source");
        }
        
        // Lógica para adicionar o SemanticDataDictionary (SSD)
        int rowNum = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(sdd.getLabel());
        row.createCell(1).setCellValue(sdd.getLabel());
        row.createCell(2).setCellValue(sdd.getHasVersion());
        row.createCell(3).setCellValue(sdd.getHasStatus());
        // Assumindo que o SDD tem getters para as propriedades abaixo,
        // seguindo a lógica de metadados do hascoapi.
        row.createCell(4).setCellValue(sdd.getStudyUri());
        row.createCell(5).setCellValue(sdd.getEntityUri());
        row.createCell(6).setCellValue(sdd.getAttributeUri());
        row.createCell(7).setCellValue(sdd.getUnitUri());
        row.createCell(8).setCellValue(sdd.getRoleUri());
        row.createCell(9).setCellValue(sdd.getInRelationToUri());
        row.createCell(10).setCellValue(sdd.getSourceUri());
        row.createCell(11).setCellValue(sdd.getOriginalId());
        
        return helper;
    }

    public static DSGGenHelper addByStudy(DSGGenHelper helper, Study study) {
        // A lógica de busca real deve ser implementada aqui, usando GenericFindWithStatus<SemanticDataDictionary>
        return helper;
    }
}

package org.hascoapi.transform.mt.ins;

import org.hascoapi.entity.pojo.Codebook;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSCodebook {

    public static INSGenHelper add(INSGenHelper helper, Codebook codebook) {

        if (helper == null) {
            System.out.println("[ERROR] INSCodebook: helper is null");
            return helper;
        }

        if (helper.workbook == null) {
            System.out.println("[ERROR] INSCodebook: helper's workbook is null");
            return helper;
        }

        if (codebook == null) {
            return helper;
        }

        // Get the "Codebooks" sheet
        Sheet codebookSheet = helper.workbook.getSheet(INSGen.CODEBOOKS);

        // Calculate the index for the new row
        int rowIndex = codebookSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = codebookSheet.createRow(rowIndex);

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(codebook.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(codebook.getHascoTypeUri()));  

        // "rdf:type"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(codebook.getTypeUri()));  

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(codebook.getLabel());  

        // "vstoi:hasContent"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue("");  

        // "vstoi:hasLanguage",
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(codebook.getHasLanguage());

        // "vstoi:hasVersion"
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(codebook.getHasVersion());

        // "rdfs:comment"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue(codebook.getComment());

        // "hasco:hasImage"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(codebook.getHasImageUri());

        // "hasco:hasWebDocument"};
        Cell cell10 = newRow.createCell(9);
        cell10.setCellValue(codebook.getHasWebDocument());

        return helper;
    }

}

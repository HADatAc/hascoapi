package org.hascoapi.transform.mt.ins;

import org.hascoapi.entity.pojo.ResponseOption;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSResponseOption {

    public static Workbook add(Workbook workbook, ResponseOption responseOption) {

        if (responseOption == null) {
            return workbook;
        }

        // Get the "ResponseOptions" sheet
        Sheet responseOptionSheet = workbook.getSheet(INSGen.RESPONSE_OPTIONS);

        // Calculate the index for the new row
        int rowIndex = responseOptionSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = responseOptionSheet.createRow(rowIndex);

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(responseOption.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(responseOption.getHascoTypeUri()));  

        // "rdf:type"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(responseOption.getTypeUri()));  

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(responseOption.getLabel());  

        // "vstoi:hasContent"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(responseOption.getHasContent());  

        // "vstoi:hasLanguage",
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(responseOption.getHasLanguage());

        // "vstoi:hasVersion"
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(responseOption.getHasVersion());

        // "hasco:hasMaker"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue("");

        // "rdfs:comment"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(responseOption.getComment());

        // "hasco:hasImage"
        Cell cell10 = newRow.createCell(9);
        cell10.setCellValue(responseOption.getHasImageUri());

        // "vstoi:hasWebDocumentation"};
        Cell cell11 = newRow.createCell(10);
        cell11.setCellValue(responseOption.getHasWebDocument());

        return workbook;
    }

}

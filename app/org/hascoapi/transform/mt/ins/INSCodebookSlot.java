package org.hascoapi.transform.mt.ins;

import org.hascoapi.entity.pojo.CodebookSlot;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSCodebookSlot {

    public static Workbook add(Workbook workbook, CodebookSlot codebookSlot) {

        if (codebookSlot == null) {
            return workbook;
        }

        // Get the "CodebookSlots" sheet
        Sheet codebookSlotSheet = workbook.getSheet(INSGen.CODEBOOK_SLOTS);

        // Calculate the index for the new row
        int rowIndex = codebookSlotSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = codebookSlotSheet.createRow(rowIndex);

        // "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(codebookSlot.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(codebookSlot.getHascoTypeUri()));  

        // "rdf:type"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(codebookSlot.getTypeUri()));  

        // "vstoi:belongsTo"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(codebookSlot.getBelongsTo());  

        // "vstoi:hasCodebookSlot"
        Cell cell5 = newRow.createCell(4);
        if (codebookSlot != null && codebookSlot.getHasResponseOption() != null) {
            cell5.setCellValue(URIUtils.replaceNameSpaceEx(codebookSlot.getHasResponseOption()));  
        } else {
            cell5.setCellValue("");
        }

        // "vstoi:hasPosition", 
        Cell cell6 = newRow.createCell(5);
        if (codebookSlot != null && codebookSlot.getHasPriority() != null) {
            cell6.setCellValue(codebookSlot.getHasPriority());  
        } else {
            cell6.setCellValue("");
        }

        return workbook;
    }

}

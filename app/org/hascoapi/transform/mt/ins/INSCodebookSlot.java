package org.hascoapi.transform.mt.ins;

import java.util.List;
import org.hascoapi.entity.pojo.Codebook;
import org.hascoapi.entity.pojo.CodebookSlot;
import org.hascoapi.entity.pojo.ResponseOption;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSCodebookSlot {

    public static INSGenHelper addByCodebook(INSGenHelper helper, Codebook codebook) {

        if (helper == null) {
            System.out.println("[ERROR] INSCodebookSlot.addByCodebook(): helper is null");
            return helper;
        }

        if (helper.workbook == null) {
            System.out.println("[ERROR] INSCodebookSlot.addByCodebook(): helper's workbook is null");
            return helper;
        }

        if (codebook == null) {
            return helper;
        }

        List<CodebookSlot> cbSlots = codebook.getCodebookSlots();
        if (cbSlots != null && cbSlots.size() > 0) {
            for (CodebookSlot cbSlot: cbSlots) {
                helper = INSCodebookSlot.add(helper, cbSlot);
            }
        }
        return helper;

    }

    private static INSGenHelper add(INSGenHelper helper, CodebookSlot codebookSlot) {

        if (helper == null) {
            System.out.println("[ERROR] INSCodebookSlot.add(): helper is null");
            return helper;
        }

        if (helper.workbook == null) {
            System.out.println("[ERROR] INSCodebookSlot.add(): helper's workbook is null");
            return helper;
        }

        if (codebookSlot == null) {
            return helper;
        }

        // Get the "CodebookSlots" sheet
        Sheet codebookSlotSheet = helper.workbook.getSheet(INSGen.CODEBOOK_SLOTS);

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
        cell4.setCellValue(URIUtils.replaceNameSpaceEx(codebookSlot.getBelongsTo()));  

        // "vstoi:hasCodebookSlot"
        Cell cell5 = newRow.createCell(4);
        if (codebookSlot != null && codebookSlot.getHasResponseOption() != null) {
            cell5.setCellValue(URIUtils.replaceNameSpaceEx(codebookSlot.getHasResponseOption()));  
            ResponseOption responseOption = ResponseOption.find(codebookSlot.getHasResponseOption());
            if (responseOption != null) {
                helper.respOptions.put(responseOption.getUri(),responseOption);
            } 
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

        return helper;
    }

}

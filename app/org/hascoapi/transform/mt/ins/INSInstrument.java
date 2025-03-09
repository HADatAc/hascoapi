package org.hascoapi.transform.mt.ins;

import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSInstrument {

    public static Workbook add(Workbook workbook, Instrument inst) {

        if (inst == null) {
            return workbook;
        }

        // Get the "Instruments" sheet
        Sheet instrumentSheet = workbook.getSheet(INSGen.INSTRUMENTS);

        // Calculate the index for the new row
        int rowIndex = instrumentSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = instrumentSheet.createRow(rowIndex);

        // Add data to the new row

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(inst.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(inst.getHascoTypeUri()));  

        // "rdfs:subClassOf"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(inst.getSuperUri()));  

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(inst.getLabel());  

        // "vstoi:hasShortName"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(inst.getHasShortName());  

        // "vstoi:hasLanguage",
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(inst.getHasLanguage());

        // "vstoi:hasVersion"
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(inst.getHasVersion());

        // "hasco:hasMaker"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue("");

        // "rdfs:comment"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(inst.getComment());

        // "hasco:hasImage"
        Cell cell10 = newRow.createCell(9);
        cell10.setCellValue(inst.getHasImageUri());

        // "vstoi:maxLoggedMeasurements"
        Cell cell11 = newRow.createCell(10);
        cell11.setCellValue("");

        // "vstoi:minOperatingTemperature", 
        Cell cell12 = newRow.createCell(11);
        cell12.setCellValue("");

        // "vstoi:maxOperatingTemperature"
        Cell cell13 = newRow.createCell(12);
        cell13.setCellValue("");

        // "hasco:hasOperatingTemperatureUnit"
        Cell cell14 = newRow.createCell(13);
        cell14.setCellValue("");

        // "vstoi:hasWebDocumentation"};
        Cell cell15 = newRow.createCell(14);
        cell15.setCellValue(inst.getHasWebDocument());

        return workbook;
    }

}

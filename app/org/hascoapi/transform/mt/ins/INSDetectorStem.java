package org.hascoapi.transform.mt.ins;

import org.hascoapi.entity.pojo.DetectorStem;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSDetectorStem {

    public static INSGenHelper add(INSGenHelper helper,  DetectorStem detectorStem) {

        if (detectorStem == null) {
            return helper;
        }

        // Get the "DetectorStems" sheet
        Sheet detectorStemSheet = helper.workbook.getSheet(INSGen.DETECTOR_STEMS);

        // Calculate the index for the new row
        int rowIndex = detectorStemSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = detectorStemSheet.createRow(rowIndex);

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(detectorStem.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(detectorStem.getHascoTypeUri()));  

        // "rdfs:subClassOf"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(detectorStem.getSuperUri()));  

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(detectorStem.getLabel());  

        // "vstoi:hasContent"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(detectorStem.getHasContent());  

        // "vstoi:hasLanguage",
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(detectorStem.getHasLanguage());

        // "vstoi:hasVersion"
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(detectorStem.getHasVersion());

        // "hasco:hasMaker"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue("");

        // "rdfs:comment"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(detectorStem.getComment());

        // "hasco:hasImage"
        Cell cell10 = newRow.createCell(9);
        cell10.setCellValue(detectorStem.getHasImageUri());

        // "hasco:hasWebDocument"};
        Cell cell11 = newRow.createCell(10);
        cell11.setCellValue(detectorStem.getHasWebDocument());

        return helper;
    }

}

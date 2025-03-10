package org.hascoapi.transform.mt.ins;

import org.hascoapi.entity.pojo.AnnotationStem;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSAnnotationStem {

    public static INSGenHelper add(INSGenHelper helper, AnnotationStem annotationStem) {

        if (helper == null) {
            System.out.println("[ERROR] INSAnnotationStem: helper is null");
            return helper;
        }

        if (helper.workbook == null) {
            System.out.println("[ERROR] INSAnnotationStem: helper's workbook is null");
            return helper;
        }

        if (annotationStem == null) {
            return helper;
        }

        // Get the "AnnotationStems" sheet
        Sheet annotationStemSheet = helper.workbook.getSheet(INSGen.ANNOTATION_STEMS);

        // Calculate the index for the new row
        int rowIndex = annotationStemSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = annotationStemSheet.createRow(rowIndex);

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(annotationStem.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(annotationStem.getHascoTypeUri()));  

        // "rdf:type"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(annotationStem.getTypeUri()));  

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(annotationStem.getLabel());  

        // "vstoi:hasContentWithStyle"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(annotationStem.getHasContent());  

        // "vstoi:hasLanguage",
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(annotationStem.getHasLanguage());

        // "vstoi:hasVersion"
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(annotationStem.getHasVersion());

        // "rdfs:comment"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue(annotationStem.getComment());

        // "hasco:hasImage"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(annotationStem.getHasImageUri());

        // "vstoi:hasWebDocumentation"};
        Cell cell10 = newRow.createCell(9);
        cell10.setCellValue(annotationStem.getHasWebDocument());

        return helper;

    }

}

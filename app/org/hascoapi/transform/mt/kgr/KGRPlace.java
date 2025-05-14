package org.hascoapi.transform.mt.kgr;

import org.hascoapi.entity.pojo.Place;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class KGRPlace {

    public static KGRGenHelper add(KGRGenHelper helper, Place place) {
        if (place == null) {
            return helper;
        }

        // Get the "Places" sheet
        Sheet placeSheet = helper.workbook.getSheet(KGRGen.PLACES);

        // Calculate the index for the new row
        int rowIndex = placeSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = placeSheet.createRow(rowIndex);

        // "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(place.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(place.getHascoTypeUri()));
        
        // "rdfs:label"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(place.getLabel());
        
        // "schema:alternateName"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(place.getHasShortName() != null ? place.getHasShortName() : "");
        
        // "rdf:type"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(URIUtils.replaceNameSpaceEx(place.getTypeUri()));
        
        // "foaf:name"
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(place.getName() != null ? place.getName() : "");
        
        // "hasco:hasImage"
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(place.getHasImageUri() != null ? place.getHasImageUri() : "");
        
        // "schema:containedInPlace"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue(place.getContainedInPlace() != null ? URIUtils.replaceNameSpaceEx(place.getContainedInPlace()) : "");
        
        // "schema:identifier"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(place.getHasIdentifier() != null ? place.getHasIdentifier() : "");
        
        // "schema:geo"
        Cell cell10 = newRow.createCell(9);
        cell10.setCellValue(place.getHasGeo() != null ? place.getHasGeo() : "");
        
        // "schema:latitude"
        Cell cell11 = newRow.createCell(10);
        cell11.setCellValue(place.getHasLatitude() != null ? place.getHasLatitude() : "");
        
        // "schema:longitude"
        Cell cell12 = newRow.createCell(11);
        cell12.setCellValue(place.getHasLongitude() != null ? place.getHasLongitude() : "");
        
        // "schema:url"
        Cell cell13 = newRow.createCell(12);
        cell13.setCellValue(place.getHasWebDocument() != null ? place.getHasWebDocument() : "");

        return helper;
    }
}

package org.hascoapi.transform.mt.kgr;

import org.hascoapi.entity.pojo.PostalAddress;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class KGRPostalAddress {

    public static KGRGenHelper add(KGRGenHelper helper, PostalAddress address) {
        if (address == null) {
            return helper;
        }

        // Get the "PostalAddresses" sheet
        Sheet addressSheet = helper.workbook.getSheet(KGRGen.POSTAL_ADDRESSES);

        // Calculate the index for the new row
        int rowIndex = addressSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = addressSheet.createRow(rowIndex);

        // "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(address.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(address.getHascoTypeUri()));
        
        // "rdfs:label"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(address.getLabel());
        
        // "rdf:type"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(URIUtils.replaceNameSpaceEx(address.getTypeUri()));
        
        // "schema:streetAddress"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(address.getHasStreetAddress() != null ? address.getHasStreetAddress() : "");
        
        // "schema:addressLocality"
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(URIUtils.replaceNameSpaceEx(address.getHasAddressLocalityUri()));
        
        // "schema:addressRegion"
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(URIUtils.replaceNameSpaceEx(address.getHasAddressRegionUri()));
        
        // "schema:addressCountry"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue(URIUtils.replaceNameSpaceEx(address.getHasAddressCountryUri()));
        
        // "schema:postalCode"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(address.getHasPostalCode() != null ? address.getHasPostalCode() : "");

        return helper;
    }
}


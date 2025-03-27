package org.hascoapi.transform.mt.kgr;

import org.hascoapi.entity.pojo.FundingScheme;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class KGRFundingScheme {

    public static KGRGenHelper add(KGRGenHelper helper, FundingScheme scheme) {

        if (scheme == null) {
            return helper;
        }

        // Get the "fundingScheme" sheet
        Sheet fundingSchemeSheet = helper.workbook.getSheet(KGRGen.FUNDING_SCHEMES);

        // Calculate the index for the new row
        int rowIndex = fundingSchemeSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = fundingSchemeSheet.createRow(rowIndex);

        // "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(scheme.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(scheme.getHascoTypeUri()));
        
        // "rdfs:label"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(scheme.getLabel());
        
        // "schema:alternateName"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(scheme.getHasShortName() != null ? scheme.getHasShortName() : "");
        
        // "schema:funder"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(scheme.getFunderUri() != null ? scheme.getFunderUri() : "");
        
        // "schema:sponsor"
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(scheme.getSponsorUri() != null ? scheme.getSponsorUri() : "");
        
        // "schema:startDate"
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(scheme.getStartDate() != null ? scheme.getStartDate() : "");
        
        // "schema:endDate"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue(scheme.getEndDate() != null ? scheme.getEndDate() : "");
        
        // "schema:amount"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(scheme.getAmount() != null ? scheme.getAmount() : "");

        return helper;
    }
}

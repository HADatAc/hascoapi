package org.hascoapi.transform.mt.wkf;

import org.hascoapi.entity.pojo.WKF;
import org.hascoapi.entity.pojo.ProcessStem;
import org.apache.poi.ss.usermodel.*;
import org.hascoapi.utils.URIUtils;

import java.util.List;

public class WKFProcessStems {

    public static WKFGenHelper addByWkf(WKFGenHelper helper, WKF wkf) {
        if (wkf == null || wkf.getUri() == null || wkf.getUri().isEmpty()) {
            System.out.println("[WKFProcessStems] WARN: addByWkf called with null/empty wkf URI; skipping");
            return helper;
        }

        Sheet sheet = helper.workbook.getSheet(WKFGen.PROCESSSTEMS);
        if (sheet == null) {
            System.out.println("[WKFProcessStems] ERROR: ProcessStems sheet not found in workbook");
            return helper;
        }

        // Query ProcessStems associated with this WKF
        // The relationship is typically through the WKF's DataFile named graph
        String namedGraph = wkf.getNamedGraph();
        if (namedGraph == null || namedGraph.isEmpty()) {
            // Try to get from DataFile URI
            if (wkf.getHasDataFileUri() != null && !wkf.getHasDataFileUri().isEmpty()) {
                namedGraph = wkf.getHasDataFileUri();
            }
        }

        if (namedGraph == null || namedGraph.isEmpty()) {
            System.out.println("[WKFProcessStems] WARN: No named graph found for WKF uri=" + wkf.getUri() + "; skipping");
            return helper;
        }

        System.out.println("[WKFProcessStems] Querying ProcessStems for WKF uri=" + wkf.getUri() + ", namedGraph=" + namedGraph);

        // Build SPARQL query to find ProcessStems in the named graph
        String queryString = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT ?uri WHERE { "
                + "   GRAPH <" + namedGraph + "> { "
                + "     ?uri a vstoi:ProcessStem . "
                + "   } "
                + " }";

        List<ProcessStem> processStems = null;
        try {
            processStems = org.hascoapi.entity.pojo.GenericFind.findByQuery(ProcessStem.class, queryString);
            System.out.println("[WKFProcessStems] Found " + (processStems == null ? 0 : processStems.size()) + " ProcessStems");
        } catch (Exception e) {
            System.err.println("[WKFProcessStems] ERROR querying ProcessStems: " + e.getMessage());
            e.printStackTrace();
            return helper;
        }

        if (processStems == null || processStems.isEmpty()) {
            System.out.println("[WKFProcessStems] No ProcessStems found for WKF uri=" + wkf.getUri());
            return helper;
        }

        // Add each ProcessStem as a row
        for (ProcessStem ps : processStems) {
            if (ps == null) {
                continue;
            }

            int rowNum = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(rowNum);

            // Map ProcessStem fields to columns
            row.createCell(0).setCellValue(URIUtils.replaceNameSpaceEx(safe(ps.getUri())));
            row.createCell(1).setCellValue(URIUtils.replaceNameSpaceEx(safe(ps.getTypeUri())));
            row.createCell(2).setCellValue(URIUtils.replaceNameSpaceEx(safe(ps.getHascoTypeUri())));
            row.createCell(3).setCellValue(safe(ps.getLabel()));
            row.createCell(4).setCellValue(safe(ps.getComment()));
            row.createCell(5).setCellValue(safe(ps.getHasStatus()));
            row.createCell(6).setCellValue(safe(ps.getHasContent()));
            row.createCell(7).setCellValue(safe(ps.getHasLanguage()));
            row.createCell(8).setCellValue(safe(ps.getHasVersion()));
            row.createCell(9).setCellValue(URIUtils.replaceNameSpaceEx(safe(ps.getWasDerivedFrom())));
            row.createCell(10).setCellValue(URIUtils.replaceNameSpaceEx(safe(ps.getWasGeneratedBy())));
            row.createCell(11).setCellValue(safe(ps.getHasReviewNote()));
            row.createCell(12).setCellValue(safe(ps.getHasSIRManagerEmail()));
            row.createCell(13).setCellValue(safe(ps.getHasEditorEmail()));
            row.createCell(14).setCellValue(URIUtils.replaceNameSpaceEx(safe(ps.getHasImageUri())));
            row.createCell(15).setCellValue(safe(ps.getHasWebDocument()));

            System.out.println("[WKFProcessStems] Added ProcessStem row: uri=" + ps.getUri());
        }

        return helper;
    }

    public static WKFGenHelper addProcessStem(WKFGenHelper helper, ProcessStem ps) {
        if (ps == null) {
            System.out.println("[WKFProcessStems] WARN: addProcessStem called with null ProcessStem; skipping");
            return helper;
        }

        Sheet sheet = helper.workbook.getSheet(WKFGen.PROCESSSTEMS);
        if (sheet == null) {
            System.out.println("[WKFProcessStems] ERROR: ProcessStems sheet not found in workbook");
            return helper;
        }

        int rowNum = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(rowNum);

        // Map ProcessStem fields to columns
        row.createCell(0).setCellValue(URIUtils.replaceNameSpaceEx(safe(ps.getUri())));
        row.createCell(1).setCellValue(URIUtils.replaceNameSpaceEx(safe(ps.getTypeUri())));
        row.createCell(2).setCellValue(URIUtils.replaceNameSpaceEx(safe(ps.getHascoTypeUri())));
        row.createCell(3).setCellValue(safe(ps.getLabel()));
        row.createCell(4).setCellValue(safe(ps.getComment()));
        row.createCell(5).setCellValue(safe(ps.getHasStatus()));
        row.createCell(6).setCellValue(safe(ps.getHasContent()));
        row.createCell(7).setCellValue(safe(ps.getHasLanguage()));
        row.createCell(8).setCellValue(safe(ps.getHasVersion()));
        row.createCell(9).setCellValue(URIUtils.replaceNameSpaceEx(safe(ps.getWasDerivedFrom())));
        row.createCell(10).setCellValue(URIUtils.replaceNameSpaceEx(safe(ps.getWasGeneratedBy())));
        row.createCell(11).setCellValue(safe(ps.getHasReviewNote()));
        row.createCell(12).setCellValue(safe(ps.getHasSIRManagerEmail()));
        row.createCell(13).setCellValue(safe(ps.getHasEditorEmail()));
        row.createCell(14).setCellValue(URIUtils.replaceNameSpaceEx(safe(ps.getHasImageUri())));
        row.createCell(15).setCellValue(safe(ps.getHasWebDocument()));

        System.out.println("[WKFProcessStems] Added ProcessStem row: uri=" + ps.getUri() + ", label=" + ps.getLabel());

        return helper;
    }

    private static String safe(String val) {
        return val == null ? "" : val;
    }
}

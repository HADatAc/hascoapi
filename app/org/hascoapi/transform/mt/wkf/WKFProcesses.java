package org.hascoapi.transform.mt.wkf;

import org.hascoapi.entity.pojo.WKF;
import org.hascoapi.entity.pojo.Process;
import org.apache.poi.ss.usermodel.*;
import org.hascoapi.utils.URIUtils;

import java.util.List;

public class WKFProcesses {

    public static WKFGenHelper addByWkf(WKFGenHelper helper, WKF wkf) {
        if (wkf == null || wkf.getUri() == null || wkf.getUri().isEmpty()) {
            System.out.println("[WKFProcesses] WARN: addByWkf called with null/empty wkf URI; skipping");
            return helper;
        }

        Sheet sheet = helper.workbook.getSheet(WKFGen.PROCESSES);
        if (sheet == null) {
            System.out.println("[WKFProcesses] ERROR: Processes sheet not found in workbook");
            return helper;
        }

        // Query Processes associated with this WKF
        String namedGraph = wkf.getNamedGraph();
        if (namedGraph == null || namedGraph.isEmpty()) {
            if (wkf.getHasDataFileUri() != null && !wkf.getHasDataFileUri().isEmpty()) {
                namedGraph = wkf.getHasDataFileUri();
            }
        }

        if (namedGraph == null || namedGraph.isEmpty()) {
            System.out.println("[WKFProcesses] WARN: No named graph found for WKF uri=" + wkf.getUri() + "; skipping");
            return helper;
        }

        System.out.println("[WKFProcesses] Querying Processes for WKF uri=" + wkf.getUri() + ", namedGraph=" + namedGraph);

        // Build SPARQL query to find Processes in the named graph
        String queryString = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT ?uri WHERE { "
                + "   GRAPH <" + namedGraph + "> { "
                + "     ?uri a vstoi:Process . "
                + "   } "
                + " }";

        List<Process> processes = null;
        try {
            processes = org.hascoapi.entity.pojo.GenericFind.findByQuery(Process.class, queryString);
            System.out.println("[WKFProcesses] Found " + (processes == null ? 0 : processes.size()) + " Processes");
        } catch (Exception e) {
            System.err.println("[WKFProcesses] ERROR querying Processes: " + e.getMessage());
            e.printStackTrace();
            return helper;
        }

        if (processes == null || processes.isEmpty()) {
            System.out.println("[WKFProcesses] No Processes found for WKF uri=" + wkf.getUri());
            return helper;
        }

        // Add each Process as a row
        for (Process p : processes) {
            if (p == null) {
                continue;
            }

            int rowNum = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(rowNum);

            // Map Process fields to columns
            row.createCell(0).setCellValue(URIUtils.replaceNameSpaceEx(safe(p.getUri())));
            row.createCell(1).setCellValue(URIUtils.replaceNameSpaceEx(safe(p.getTypeUri())));
            row.createCell(2).setCellValue(URIUtils.replaceNameSpaceEx(safe(p.getHascoTypeUri())));
            row.createCell(3).setCellValue(safe(p.getLabel()));
            row.createCell(4).setCellValue(safe(p.getComment()));
            row.createCell(5).setCellValue(safe(p.getHasStatus()));
            row.createCell(6).setCellValue(safe(p.getHasLanguage()));
            row.createCell(7).setCellValue(safe(p.getHasVersion()));
            row.createCell(8).setCellValue(URIUtils.replaceNameSpaceEx(safe(p.getWasDerivedFrom())));
            row.createCell(9).setCellValue(safe(p.getHasReviewNote()));
            row.createCell(10).setCellValue(safe(p.getHasSIRManagerEmail()));
            row.createCell(11).setCellValue(safe(p.getHasEditorEmail()));
            row.createCell(12).setCellValue(URIUtils.replaceNameSpaceEx(safe(p.getHasTopTaskUri())));
            row.createCell(13).setCellValue(URIUtils.replaceNameSpaceEx(safe(p.getHasImageUri())));
            row.createCell(14).setCellValue(safe(p.getHasWebDocument()));

            System.out.println("[WKFProcesses] Added Process row: uri=" + p.getUri());
        }

        return helper;
    }

    public static WKFGenHelper addProcess(WKFGenHelper helper, Process p) {
        if (p == null) {
            System.out.println("[WKFProcesses] WARN: addProcess called with null Process; skipping");
            return helper;
        }

        Sheet sheet = helper.workbook.getSheet(WKFGen.PROCESSES);
        if (sheet == null) {
            System.out.println("[WKFProcesses] ERROR: Processes sheet not found in workbook");
            return helper;
        }

        int rowNum = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(rowNum);

        // Map Process fields to columns
        row.createCell(0).setCellValue(URIUtils.replaceNameSpaceEx(safe(p.getUri())));
        row.createCell(1).setCellValue(URIUtils.replaceNameSpaceEx(safe(p.getTypeUri())));
        row.createCell(2).setCellValue(URIUtils.replaceNameSpaceEx(safe(p.getHascoTypeUri())));
        row.createCell(3).setCellValue(safe(p.getLabel()));
        row.createCell(4).setCellValue(safe(p.getComment()));
        row.createCell(5).setCellValue(safe(p.getHasStatus()));
        row.createCell(6).setCellValue(safe(p.getHasLanguage()));
        row.createCell(7).setCellValue(safe(p.getHasVersion()));
        row.createCell(8).setCellValue(URIUtils.replaceNameSpaceEx(safe(p.getWasDerivedFrom())));
        row.createCell(9).setCellValue(safe(p.getHasReviewNote()));
        row.createCell(10).setCellValue(safe(p.getHasSIRManagerEmail()));
        row.createCell(11).setCellValue(safe(p.getHasEditorEmail()));
        row.createCell(12).setCellValue(URIUtils.replaceNameSpaceEx(safe(p.getHasTopTaskUri())));
        row.createCell(13).setCellValue(URIUtils.replaceNameSpaceEx(safe(p.getHasImageUri())));
        row.createCell(14).setCellValue(safe(p.getHasWebDocument()));

        System.out.println("[WKFProcesses] Added Process row: uri=" + p.getUri() + ", label=" + p.getLabel());

        return helper;
    }

    private static String safe(String val) {
        return val == null ? "" : val;
    }
}

package org.hascoapi.transform.mt.wkf;

import org.hascoapi.entity.pojo.WKF;
import org.hascoapi.entity.pojo.RequiredInstrument;
import org.apache.poi.ss.usermodel.*;
import org.hascoapi.utils.URIUtils;

import java.util.List;

public class WKFRequiredInstruments {

    public static WKFGenHelper addByWkf(WKFGenHelper helper, WKF wkf) {
        if (wkf == null || wkf.getUri() == null || wkf.getUri().isEmpty()) {
            System.out.println("[WKFRequiredInstruments] WARN: addByWkf called with null/empty wkf URI; skipping");
            return helper;
        }

        Sheet sheet = helper.workbook.getSheet(WKFGen.REQUIREDINSTRUMENTS);
        if (sheet == null) {
            System.out.println("[WKFRequiredInstruments] ERROR: RequiredInstruments sheet not found in workbook");
            return helper;
        }

        // Query RequiredInstruments associated with this WKF
        String namedGraph = wkf.getNamedGraph();
        if (namedGraph == null || namedGraph.isEmpty()) {
            if (wkf.getHasDataFileUri() != null && !wkf.getHasDataFileUri().isEmpty()) {
                namedGraph = wkf.getHasDataFileUri();
            }
        }

        if (namedGraph == null || namedGraph.isEmpty()) {
            System.out.println("[WKFRequiredInstruments] WARN: No named graph found for WKF uri=" + wkf.getUri() + "; skipping");
            return helper;
        }

        System.out.println("[WKFRequiredInstruments] Querying RequiredInstruments for WKF uri=" + wkf.getUri() + ", namedGraph=" + namedGraph);

        // Build SPARQL query to find RequiredInstruments in the named graph
        String queryString = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT ?uri WHERE { "
                + "   GRAPH <" + namedGraph + "> { "
                + "     ?uri a vstoi:RequiredInstrument . "
                + "   } "
                + " }";

        List<RequiredInstrument> requiredInstruments = null;
        try {
            requiredInstruments = org.hascoapi.entity.pojo.GenericFind.findByQuery(RequiredInstrument.class, queryString);
            System.out.println("[WKFRequiredInstruments] Found " + (requiredInstruments == null ? 0 : requiredInstruments.size()) + " RequiredInstruments");
        } catch (Exception e) {
            System.err.println("[WKFRequiredInstruments] ERROR querying RequiredInstruments: " + e.getMessage());
            e.printStackTrace();
            return helper;
        }

        if (requiredInstruments == null || requiredInstruments.isEmpty()) {
            System.out.println("[WKFRequiredInstruments] No RequiredInstruments found for WKF uri=" + wkf.getUri());
            return helper;
        }

        // Add each RequiredInstrument as a row
        for (RequiredInstrument ri : requiredInstruments) {
            if (ri == null) {
                continue;
            }

            int rowNum = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(rowNum);

            // Map RequiredInstrument fields to columns
            row.createCell(0).setCellValue(URIUtils.replaceNameSpaceEx(safe(ri.getUri())));
            row.createCell(1).setCellValue(URIUtils.replaceNameSpaceEx(safe(ri.getTypeUri())));
            row.createCell(2).setCellValue(URIUtils.replaceNameSpaceEx(safe(ri.getHascoTypeUri())));
            row.createCell(3).setCellValue(safe(ri.getLabel()));
            row.createCell(4).setCellValue(safe(ri.getComment()));
            row.createCell(5).setCellValue(URIUtils.replaceNameSpaceEx(safe(ri.getUsesInstrument())));

            // hasRequiredComponent is a list - join with pipes
            String componentsStr = joinUriList(ri.getHasRequiredComponents());
            row.createCell(6).setCellValue(componentsStr);

            row.createCell(7).setCellValue(URIUtils.replaceNameSpaceEx(safe(ri.getHasImageUri())));
            row.createCell(8).setCellValue(safe(ri.getHasWebDocument()));

            System.out.println("[WKFRequiredInstruments] Added RequiredInstrument row: uri=" + ri.getUri());
        }

        return helper;
    }

    private static String safe(String val) {
        return val == null ? "" : val;
    }

    private static String joinUriList(List<String> uris) {
        if (uris == null || uris.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < uris.size(); i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            String uri = uris.get(i);
            if (uri != null && !uri.isEmpty()) {
                sb.append(URIUtils.replaceNameSpaceEx(uri));
            }
        }
        return sb.toString();
    }
}

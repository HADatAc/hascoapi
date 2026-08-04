package org.hascoapi.transform.mt.wkf;

import org.hascoapi.entity.pojo.WKF;
import org.hascoapi.entity.pojo.Task;
import org.apache.poi.ss.usermodel.*;
import org.hascoapi.utils.URIUtils;

import java.util.List;

public class WKFTasks {

    public static WKFGenHelper addByWkf(WKFGenHelper helper, WKF wkf) {
        if (wkf == null || wkf.getUri() == null || wkf.getUri().isEmpty()) {
            System.out.println("[WKFTasks] WARN: addByWkf called with null/empty wkf URI; skipping");
            return helper;
        }

        Sheet sheet = helper.workbook.getSheet(WKFGen.TASKS);
        if (sheet == null) {
            System.out.println("[WKFTasks] ERROR: Tasks sheet not found in workbook");
            return helper;
        }

        // Query Tasks associated with this WKF
        String namedGraph = wkf.getNamedGraph();
        if (namedGraph == null || namedGraph.isEmpty()) {
            if (wkf.getHasDataFileUri() != null && !wkf.getHasDataFileUri().isEmpty()) {
                namedGraph = wkf.getHasDataFileUri();
            }
        }

        if (namedGraph == null || namedGraph.isEmpty()) {
            System.out.println("[WKFTasks] WARN: No named graph found for WKF uri=" + wkf.getUri() + "; skipping");
            return helper;
        }

        System.out.println("[WKFTasks] Querying Tasks for WKF uri=" + wkf.getUri() + ", namedGraph=" + namedGraph);

        // Build SPARQL query to find Tasks in the named graph
        String queryString = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT ?uri WHERE { "
                + "   GRAPH <" + namedGraph + "> { "
            + "     { ?uri hasco:hascoType vstoi:Task . } "
            + "     UNION { ?uri a ?taskType . ?taskType rdfs:subClassOf* vstoi:Task . } "
                + "   } "
                + " }";

        List<Task> tasks = null;
        try {
            tasks = org.hascoapi.entity.pojo.GenericFind.findByQuery(Task.class, queryString);
            System.out.println("[WKFTasks] Found " + (tasks == null ? 0 : tasks.size()) + " Tasks");
        } catch (Exception e) {
            System.err.println("[WKFTasks] ERROR querying Tasks: " + e.getMessage());
            e.printStackTrace();
            return helper;
        }

        if (tasks == null || tasks.isEmpty()) {
            System.out.println("[WKFTasks] No Tasks found for WKF uri=" + wkf.getUri());
            return helper;
        }

        // Add each Task as a row
        for (Task t : tasks) {
            if (t == null) {
                continue;
            }

            int rowNum = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(rowNum);

            // Map Task fields to columns
            row.createCell(0).setCellValue(URIUtils.replaceNameSpaceEx(safe(t.getUri())));
            String typeUri = safe(t.getTypeUri());
            if (typeUri.isEmpty()) {
                typeUri = "vstoi:Task";
            }
            row.createCell(1).setCellValue(URIUtils.replaceNameSpaceEx(typeUri));
            row.createCell(2).setCellValue("vstoi:Task");
            row.createCell(3).setCellValue(safe(t.getLabel()));
            row.createCell(4).setCellValue(safe(t.getComment()));
            row.createCell(5).setCellValue(safe(t.getHasStatus()));
            row.createCell(6).setCellValue(safe(t.getHasLanguage()));
            row.createCell(7).setCellValue(safe(t.getHasVersion()));
            row.createCell(8).setCellValue(URIUtils.replaceNameSpaceEx(safe(t.getWasDerivedFrom())));
            row.createCell(9).setCellValue(safe(t.getHasReviewNote()));
            row.createCell(10).setCellValue(safe(t.getHasSIRManagerEmail()));
            row.createCell(11).setCellValue(safe(t.getHasEditorEmail()));
            row.createCell(12).setCellValue(URIUtils.replaceNameSpaceEx(safe(t.getHasSupertaskUri())));

            // hasSubtask is a list - join with pipes
            String subtasksStr = joinUriList(t.getHasSubtaskUris());
            row.createCell(13).setCellValue(subtasksStr);

            row.createCell(14).setCellValue(safe(t.getHasTemporalDependency()));

            // hasRequiredInstrument is a list - join with pipes
            String requiredInstrumentsStr = joinUriList(t.getHasRequiredInstrumentUris());
            row.createCell(15).setCellValue(requiredInstrumentsStr);

            row.createCell(16).setCellValue(URIUtils.replaceNameSpaceEx(safe(t.getHasImageUri())));
            row.createCell(17).setCellValue(safe(t.getHasWebDocument()));
            row.createCell(18).setCellValue(safe(t.getHasIterationConstraint()));
            row.createCell(19).setCellValue(safe(t.getSupportsObjective()));

            System.out.println("[WKFTasks] Added Task row: uri=" + t.getUri());
        }

        return helper;
    }

    public static WKFGenHelper addTask(WKFGenHelper helper, Task t) {
        if (t == null) {
            System.out.println("[WKFTasks] WARN: addTask called with null Task; skipping");
            return helper;
        }

        Sheet sheet = helper.workbook.getSheet(WKFGen.TASKS);
        if (sheet == null) {
            System.out.println("[WKFTasks] ERROR: Tasks sheet not found in workbook");
            return helper;
        }

        int rowNum = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(rowNum);

        // Map Task fields to columns
        row.createCell(0).setCellValue(URIUtils.replaceNameSpaceEx(safe(t.getUri())));

        // Fallback for TypeUri
        String typeUri = t.getTypeUri();
        if (typeUri == null || typeUri.trim().isEmpty()) {
            typeUri = "vstoi:Task";
        }
        row.createCell(1).setCellValue(URIUtils.replaceNameSpaceEx(typeUri));

        // WKF v1.2.1: task archetype is fixed in hasco:hascoType.
        row.createCell(2).setCellValue("vstoi:Task");

        row.createCell(3).setCellValue(safe(t.getLabel()));
        row.createCell(4).setCellValue(safe(t.getComment()));
        row.createCell(5).setCellValue(safe(t.getHasStatus()));
        row.createCell(6).setCellValue(safe(t.getHasLanguage()));
        row.createCell(7).setCellValue(safe(t.getHasVersion()));
        row.createCell(8).setCellValue(URIUtils.replaceNameSpaceEx(safe(t.getWasDerivedFrom())));
        row.createCell(9).setCellValue(safe(t.getHasReviewNote()));
        row.createCell(10).setCellValue(safe(t.getHasSIRManagerEmail()));
        row.createCell(11).setCellValue(safe(t.getHasEditorEmail()));
        row.createCell(12).setCellValue(URIUtils.replaceNameSpaceEx(safe(t.getHasSupertaskUri())));

        // hasSubtask is a list - join with pipes
        String subtasksStr = joinUriList(t.getHasSubtaskUris());
        row.createCell(13).setCellValue(subtasksStr);

        row.createCell(14).setCellValue(safe(t.getHasTemporalDependency()));

        // hasRequiredInstrument is a list - join with pipes
        String requiredInstrumentsStr = joinUriList(t.getHasRequiredInstrumentUris());
        row.createCell(15).setCellValue(requiredInstrumentsStr);

        row.createCell(16).setCellValue(URIUtils.replaceNameSpaceEx(safe(t.getHasImageUri())));
        row.createCell(17).setCellValue(safe(t.getHasWebDocument()));
        row.createCell(18).setCellValue(safe(t.getHasIterationConstraint()));
        row.createCell(19).setCellValue(safe(t.getSupportsObjective()));

        System.out.println("[WKFTasks] Added Task row: uri=" + t.getUri() + ", label=" + t.getLabel() + ", typeUri=" + typeUri);

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
                sb.append(";");
            }
            String uri = uris.get(i);
            if (uri != null && !uri.isEmpty()) {
                sb.append(URIUtils.replaceNameSpaceEx(uri));
            }
        }
        return sb.toString();
    }
}

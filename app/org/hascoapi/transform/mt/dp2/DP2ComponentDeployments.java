package org.hascoapi.transform.mt.dp2;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.ComponentInstance;
import org.hascoapi.entity.pojo.Deployment;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public class DP2ComponentDeployments {

    public static void setHeaders(Sheet sheet) {
        String[] headers = {
            "hasURI",
            "rdf:type",
            "hasco:hascoDeployment",
            "hasco:hasInstrumentSlot",
            "hasco:hasComponentInstance"
        };

        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public static DP2GenHelper addFromDeployment(DP2GenHelper helper, Deployment deployment) {
        if (helper == null || helper.workbook == null || deployment == null) {
            return helper;
        }

        List<String> componentUris = deployment.getComponentInstanceUri();
        if (componentUris == null || componentUris.isEmpty()) {
            return helper;
        }

        String instrumentInstanceUri = deployment.getInstrumentInstanceUri();
        Sheet sheet = helper.workbook.getSheet(DP2Gen.COMPONENTDEPLOYMENTS);

        for (String componentUri : componentUris) {
            if (componentUri == null || componentUri.trim().isEmpty()) {
                continue;
            }

            List<String> slotUris = findMatchingSlots(instrumentInstanceUri, componentUri);
            if (slotUris.isEmpty()) {
                addRow(sheet, deployment.getUri(), "", componentUri);
                continue;
            }

            for (String slotUri : slotUris) {
                addRow(sheet, deployment.getUri(), slotUri, componentUri);
            }
        }

        return helper;
    }

    private static void addRow(Sheet sheet, String deploymentUri, String slotUri, String componentInstanceUri) {
        int rowIndex = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(buildComponentDeploymentUri(deploymentUri, slotUri, componentInstanceUri));
        row.createCell(1).setCellValue("vstoi:ComponentDeployment");
        row.createCell(2).setCellValue(deploymentUri == null ? "" : URIUtils.replaceNameSpaceEx(deploymentUri));
        row.createCell(3).setCellValue(slotUri == null ? "" : URIUtils.replaceNameSpaceEx(slotUri));
        row.createCell(4).setCellValue(componentInstanceUri == null ? "" : URIUtils.replaceNameSpaceEx(componentInstanceUri));
    }

    private static String buildComponentDeploymentUri(String deploymentUri, String slotUri, String componentInstanceUri) {
        String dep = deploymentUri == null ? "" : URIUtils.replaceNameSpaceEx(deploymentUri);
        String slot = slotUri == null ? "" : URIUtils.replaceNameSpaceEx(slotUri);
        String comp = componentInstanceUri == null ? "" : URIUtils.replaceNameSpaceEx(componentInstanceUri);

        String prefix = "pmsr";
        if (dep.contains(":")) {
            String candidate = dep.substring(0, dep.indexOf(':'));
            if (!candidate.startsWith("http") && !candidate.isEmpty()) {
                prefix = candidate;
            }
        }

        CRC32 crc32 = new CRC32();
        String seed = dep + "|" + slot + "|" + comp;
        crc32.update(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String suffix = String.format("%010d", crc32.getValue());
        return prefix + ":DPC" + suffix;
    }

    private static List<String> findMatchingSlots(String instrumentInstanceUri, String componentInstanceUri) {
        List<String> out = new ArrayList<String>();
        if (instrumentInstanceUri == null || instrumentInstanceUri.trim().isEmpty()) {
            return out;
        }

        String query = NameSpaces.getInstance().printSparqlNameSpaceList()
                + "SELECT DISTINCT ?slot WHERE { "
                + " ?slot hasco:hascoType vstoi:ContainerSlot . "
                + " ?slot vstoi:belongsTo <" + instrumentInstanceUri + "> . "
                + " ?slot vstoi:hasComponentInstance <" + componentInstanceUri + "> . "
                + "}";

        try {
            ResultSetRewindable rs = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                    query);
            while (rs != null && rs.hasNext()) {
                QuerySolution sol = rs.next();
                if (sol.getResource("slot") != null) {
                    out.add(sol.getResource("slot").getURI());
                }
            }
        } catch (Exception e) {
            // Ignore and fallback below.
        }

        // Fallback: infer by component type if explicit slot->componentInstance is unavailable.
        if (out.isEmpty()) {
            ComponentInstance ci = ComponentInstance.find(componentInstanceUri);
            String compTypeUri = ci != null ? ci.getTypeUri() : null;
            if (compTypeUri != null && !compTypeUri.isEmpty()) {
                String q2 = NameSpaces.getInstance().printSparqlNameSpaceList()
                        + "SELECT DISTINCT ?slot WHERE { "
                        + " ?slot hasco:hascoType vstoi:ContainerSlot . "
                        + " ?slot vstoi:belongsTo <" + instrumentInstanceUri + "> . "
                        + " ?slot vstoi:hasComponent <" + compTypeUri + "> . "
                        + "}";
                try {
                    ResultSetRewindable rs2 = SPARQLUtils.select(
                            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                            q2);
                    while (rs2 != null && rs2.hasNext()) {
                        QuerySolution sol = rs2.next();
                        if (sol.getResource("slot") != null) {
                            out.add(sol.getResource("slot").getURI());
                        }
                    }
                } catch (Exception e) {
                    // Ignore and return what we have.
                }
            }
        }

        return out;
    }
}

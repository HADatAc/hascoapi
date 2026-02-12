package org.hascoapi.transform.mt.dp2;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.hascoapi.entity.pojo.*;
import org.hascoapi.utils.ConfigProp;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class DP2Gen {

    public static final String INFOSHEET                 = "InfoSheet";
    public static final String NAMESPACES                = "Namespaces";
    public static final String DEPLOYMENTS               = "Deployments";
    public static final String PLATFORMS                 = "Platforms";
    public static final String PLATFORMINTANCES          = "PlatformInstances";
    public static final String FIELDSOFVIEW              = "FieldsOfView";
    public static final String INSTRUMENTINSTANCES       = "InstrumentInstances";
    public static final String COMPONENTINSTANCES        = "ComponentInstances";
    public static final String SENSINGPERSPECTIVE        = "SensingPerspective";

    public static Workbook create(String filename) {
        Workbook workbook = new XSSFWorkbook();

        // Create sheet named 'InfoSheet'
        Sheet infoSheet = workbook.createSheet(INFOSHEET);

        // Create the header row for InfoSheet
        Row isHeaderRow = infoSheet.createRow(0);
        Cell isHeaderCell1 = isHeaderRow.createCell(0);
        isHeaderCell1.setCellValue("Attribute");
        Cell isHeaderCell2 = isHeaderRow.createCell(1);
        isHeaderCell2.setCellValue("Value");

        Row dataRow1 = infoSheet.createRow(1);
        Cell isDataCell1_1 = dataRow1.createCell(0);
        isDataCell1_1.setCellValue("hasDependencies");
        Cell isDataCell1_2 = dataRow1.createCell(1);
        isDataCell1_2.setCellValue("#" + NAMESPACES);

        Row dataRow2 = infoSheet.createRow(2);
        Cell isDataCell2_1 = dataRow2.createCell(0);
        isDataCell2_1.setCellValue("Deployments");
        Cell isDataCell2_2 = dataRow2.createCell(1);
        isDataCell2_2.setCellValue("#" + DEPLOYMENTS);

        Row dataRow3 = infoSheet.createRow(3);
        Cell isDataCell3_1 = dataRow3.createCell(0);
        isDataCell3_1.setCellValue("Platforms");
        Cell isDataCell3_2 = dataRow3.createCell(1);
        isDataCell3_2.setCellValue("#" + PLATFORMS);

        Row dataRow4 = infoSheet.createRow(4);
        Cell isDataCell4_1 = dataRow4.createCell(0);
        isDataCell4_1.setCellValue("PlatformInstances");
        Cell isDataCell4_2 = dataRow4.createCell(1);
        isDataCell4_2.setCellValue("#" + PLATFORMINTANCES);

        Row dataRow5 = infoSheet.createRow(5);
        Cell isDataCell5_1 = dataRow5.createCell(0);
        isDataCell5_1.setCellValue("InstrumentInstances");
        Cell isDataCell5_2 = dataRow5.createCell(1);
        isDataCell5_2.setCellValue("#" + INSTRUMENTINSTANCES);

        Row dataRow6 = infoSheet.createRow(6);
        Cell isDataCell6_1 = dataRow6.createCell(0);
        isDataCell6_1.setCellValue("ComponentInstances");
        Cell isDataCell6_2 = dataRow6.createCell(1);
        isDataCell6_2.setCellValue("#" + COMPONENTINSTANCES);

        Row dataRow7 = infoSheet.createRow(7);
        Cell isDataCell7_1 = dataRow7.createCell(0);
        isDataCell7_1.setCellValue("FieldsOfView");
        Cell isDataCell7_2 = dataRow7.createCell(1);
        isDataCell7_2.setCellValue("#" + FIELDSOFVIEW);

        Row dataRow8 = infoSheet.createRow(8);
        Cell isDataCell8_1 = dataRow8.createCell(0);
        isDataCell8_1.setCellValue("SensingPerspective");
        Cell isDataCell8_2 = dataRow8.createCell(1);
        isDataCell8_2.setCellValue("#" + SENSINGPERSPECTIVE);

        // Create sheets
        workbook.createSheet(NAMESPACES);
        workbook.createSheet(DEPLOYMENTS);
        workbook.createSheet(PLATFORMS);
        workbook.createSheet(PLATFORMINTANCES);
        workbook.createSheet(FIELDSOFVIEW);
        workbook.createSheet(INSTRUMENTINSTANCES);
        workbook.createSheet(COMPONENTINSTANCES);
        workbook.createSheet(SENSINGPERSPECTIVE);

        // IMPORTANT: set headers for each sheet so add() methods write into the correct columns
        DP2Deployments.setHeaders(workbook.getSheet(DEPLOYMENTS));
        DP2Plataforms.setHeaders(workbook.getSheet(PLATFORMS));
        DP2PlataformInstances.setHeaders(workbook.getSheet(PLATFORMINTANCES));
        DP2FieldsOfView.setHeaders(workbook.getSheet(FIELDSOFVIEW));
        DP2InstrumentInstances.setHeaders(workbook.getSheet(INSTRUMENTINSTANCES));
        DP2ComponentsInstances.setHeaders(workbook.getSheet(COMPONENTINSTANCES));
        // DP2SensingPerspective headers are not implemented yet

        return workbook;
    }

    public static void saveNamespaces(DP2GenHelper helper) {
        if (helper == null || helper.workbook == null) {
            return;
        }

        Sheet namespacesSheet = helper.workbook.getSheet(NAMESPACES);

        // Expected DP2 namespace sheet schema
        Row row = namespacesSheet.createRow(0);
        row.createCell(0).setCellValue("hasPrefix");
        row.createCell(1).setCellValue("hasNameSpace");
        row.createCell(2).setCellValue("hasFormat");
        row.createCell(3).setCellValue("hasSource");

        // Always include required prefixes first (so the workbook is self-contained)
        int rowIndex = 1;
        rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, "ahead", "http://hadatac.org/ont/arrowhead/", "text/turtle", "http://hadatac.org/ont/arrowhead/");
        rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, "vstoi", "http://hadatac.org/ont/vstoi#", "text/turtle", "http://hadatac.org/ont/vstoi#");

        // Then include anything collected during generation (avoid duplicates by prefix)
        java.util.Set<String> seenPrefixes = new java.util.HashSet<>();
        seenPrefixes.add("ahead");
        seenPrefixes.add("vstoi");

        for (NameSpace namespace: helper.namespaces.values()) {
            if (namespace == null) continue;
            String prefix = namespace.getLabel();
            String nsUri = namespace.getUri();
            if (prefix == null) continue;
            if (seenPrefixes.contains(prefix)) continue;
            seenPrefixes.add(prefix);

            rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, prefix, nsUri, "text/turtle", nsUri);
        }
    }

    private static int writeNamespaceRow(Sheet sheet, int rowIndex, String prefix, String nsUri, String format, String source) {
        Row newRow = sheet.createRow(rowIndex);
        newRow.createCell(0).setCellValue(prefix == null ? "" : prefix);
        newRow.createCell(1).setCellValue(nsUri == null ? "" : nsUri);
        newRow.createCell(2).setCellValue(format == null ? "" : format);
        newRow.createCell(3).setCellValue(source == null ? "" : source);
        return rowIndex + 1;
    }

    public static String save(DP2GenHelper helper, String filename) {
        System.out.println("\n========== DP2Gen.save() START ==========");
        System.out.println("  filename parameter: " + filename);

        if (helper == null) {
            System.out.println("  ✗ helper is NULL, returning empty string");
            return "";
        }
        if (helper.workbook == null) {
            System.out.println("  ✗ workbook is NULL, returning empty string");
            return "";
        }

        System.out.println("  ✓ helper and workbook are valid");

        // Save namespaces
        System.out.println("  → Saving namespaces...");
        DP2Gen.saveNamespaces(helper);
        System.out.println("  ✓ Namespaces saved");

        try {
            java.io.File outputFile = new java.io.File(filename);
            System.out.println("  Output file absolute path: " + outputFile.getAbsolutePath());
            System.out.println("  Output file parent directory: " + outputFile.getParent());
            System.out.println("  Parent directory exists: " + (outputFile.getParentFile() != null && outputFile.getParentFile().exists()));

            // Ensure parent directory exists
            if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
                System.out.println("  Creating parent directory...");
                boolean created = outputFile.getParentFile().mkdirs();
                System.out.println("  Parent directory created: " + created);
            }

            System.out.println("  → Writing workbook to file...");
            FileOutputStream fileOut = new FileOutputStream(outputFile);
            helper.workbook.write(fileOut);
            fileOut.close();
            helper.workbook.close();

            System.out.println("  ✓ File written successfully");
            System.out.println("  File size: " + outputFile.length() + " bytes");
            System.out.println("  File exists: " + outputFile.exists());
            System.out.println("  File can read: " + outputFile.canRead());
            System.out.println("========== DP2Gen.save() END (SUCCESS) ==========\n");

            return filename;
        } catch (IOException e) {
            System.err.println("  ✗ IOException during file write:");
            System.err.println("     " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            System.out.println("========== DP2Gen.save() END (FAILURE) ==========\n");
            return "Error: " + e.getMessage();
        }
    }

    public static final int PAGESIZE                = 20000;
    public static final int OFFSET                  = 0;

    public static String genByDeployments(Deployment deployment, String filename, String mediaFolder, String verifyUri) {
        if (deployment == null) {
            return "";
        }
        DP2GenHelper helper = new DP2GenHelper();
        helper.workbook = DP2Gen.create(filename);

        helper = DP2Deployments.add(helper,deployment);


        return DP2Gen.save(helper, filename);
    }

    public static String genByPlatforms(Platform platform, String filename, String mediaFolder, String verifyUri) {
        if (platform == null) {
            return "";
        }
        DP2GenHelper helper = new DP2GenHelper();
        helper.workbook = DP2Gen.create(filename);

        helper = DP2Plataforms.add(helper,platform);

        return DP2Gen.save(helper, filename);
    }

    // Backward-compatible entrypoint (old callers didn't pass datafile URI)
    public static String genByStatus(String status, String filename, String mediaFolder, String verifyUri) {
        return genByStatus(null, status, filename, mediaFolder, verifyUri);
    }

    /**
     * Generate a DP2 workbook for a given status, scoped to a specific datafile when provided.
     *
     * Why: DP2 ingestion currently doesn't assign `vstoi:hasStatus` to each DP2 element (deployment, instances...).
     * Status belongs to the DP2 MT itself. So we:
     * 1) Resolve the DP2 MT (and its datafile) by status
     * 2) Fetch DP2 elements by hasco:hasDataFile (not by vstoi:hasStatus)
     */
    public static String genByStatus(String dataFileUri, String status, String filename, String mediaFolder, String verifyUri) {
        System.out.println("\n========== DP2Gen.genByStatus() START ==========");
        System.out.println("Input parameters:");
        System.out.println("  dataFileUri: " + dataFileUri);
        System.out.println("  status: " + status);
        System.out.println("  filename: " + filename);
        System.out.println("  mediaFolder: " + mediaFolder);

        DP2GenHelper helper = new DP2GenHelper();

        // Normalize filename so mtGetGenerated() can always retrieve it from ConfigProp.getPathIngestion().
        String baseName = filename;
        if (baseName != null) {
            baseName = new java.io.File(baseName).getName();
        }
        System.out.println("  baseName (after extraction): " + baseName);

        String basePath = ConfigProp.getPathIngestion();
        System.out.println("  basePath (from ConfigProp): " + basePath);

        if (basePath == null || basePath.trim().isEmpty()) {
            basePath = "";
        }

        // Normalize path separators for the current OS
        if (!basePath.isEmpty()) {
            basePath = basePath.replace("/", java.io.File.separator).replace("\\", java.io.File.separator);
            if (!basePath.endsWith(java.io.File.separator)) {
                basePath = basePath + java.io.File.separator;
            }
        }
        System.out.println("  basePath (normalized): " + basePath);

        // Build the full output path
        String outFilename;
        if (basePath.isEmpty()) {
            outFilename = baseName;
        } else {
            // Ensure the directory exists
            java.io.File dir = new java.io.File(basePath);
            if (!dir.exists()) {
                System.out.println("  Creating directory: " + dir.getAbsolutePath());
                boolean created = dir.mkdirs();
                if (!created) {
                    System.err.println("[ERROR] Failed to create directory: " + dir.getAbsolutePath());
                }
            }
            outFilename = basePath + baseName;
        }

        System.out.println("  outFilename (final): " + outFilename);
        System.out.println("  outFilename (absolute): " + new java.io.File(outFilename).getAbsolutePath());

        helper.workbook = DP2Gen.create(outFilename);

        final String ns = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList();

        // We must NOT use the datafile URI of the DP2 MT being (re)generated as a filter to SELECT the MT.
        // Otherwise we end up selecting the DP2 object that is being created for this *generation request*
        // (self-reference), which has no ingested content.
        final String requestedDataFileUriCanonical = canonicalizeUri(dataFileUri);

        // 1) Resolve DP2 MT (and its datafile) by status + datafile (optional)
        java.util.List<DP2> dp2s = null;

        try {
            String requestedStatus = status == null ? "" : status.trim();
            String draftStatus = org.hascoapi.vocabularies.VSTOI.DRAFT;

            String diag = ns + " SELECT DISTINCT ?uri WHERE { ?uri a <" + org.hascoapi.vocabularies.HASCO.DP2 + "> . }";
            java.util.List<DP2> allDp2s = GenericFind.findByQuery(DP2.class, diag);
            System.out.println("DP2Gen.genByStatus: dp2 MT candidates found=" + (allDp2s == null ? 0 : allDp2s.size()));

            java.util.Map<String, DP2> byCanonical = new java.util.LinkedHashMap<>();
            if (allDp2s != null) {
                for (DP2 d : allDp2s) {
                    if (d == null || d.getUri() == null) {
                        continue;
                    }

                    String canonical = d.getUri().replace("https://", "http://");
                    String rawStatus = d.getHasStatus();
                    String effective = (rawStatus == null || rawStatus.isEmpty()) ? draftStatus : rawStatus;
                    String df = d.getHasDataFileUri();
                    String dfCanonical = canonicalizeUri(df);

                    // compact diagnostic per candidate
                    System.out.println("  [DP2Gen] candidate uri=" + d.getUri()
                            + " status(raw)=" + rawStatus
                            + " status(eff)=" + effective
                            + " dataFile=" + dfCanonical);

                    boolean include = true;
                    if (!requestedStatus.isEmpty()) {
                        include = effective.equals(requestedStatus);
                    }

                    // If caller provided an explicit original datafile URI filter, apply it.
                    // BUT: ignore it if it matches the dp2's own hasDataFile (we are likely looking at
                    // the freshly-created DP2 MT for this generation request).
                    if (include && requestedDataFileUriCanonical != null && !requestedDataFileUriCanonical.isEmpty()) {
                        if (dfCanonical != null && dfCanonical.equals(requestedDataFileUriCanonical)) {
                            // self-reference; skip it
                            include = false;
                        } else if (dataFileUri != null && !dataFileUri.trim().isEmpty()) {
                            // keep the old behavior only if we actually want to constrain to another df
                            // (use canonical comparisons)
                            include = (dfCanonical != null && dfCanonical.equals(canonicalizeUri(dataFileUri)));
                        }
                    }

                    if (!include) {
                        continue;
                    }

                    // Prefer https variant if duplicates exist
                    DP2 existing = byCanonical.get(canonical);
                    if (existing == null) {
                        byCanonical.put(canonical, d);
                    } else {
                        if (existing.getUri() != null && existing.getUri().startsWith("http://") && d.getUri().startsWith("https://")) {
                            byCanonical.put(canonical, d);
                        }
                    }
                }
            }

            dp2s = new java.util.ArrayList<>(byCanonical.values());

        } catch (Exception e) {
            System.out.println("[ERROR] DP2Gen.genByStatus: error while resolving DP2 MTs by status/datafile: " + e.getMessage());
            e.printStackTrace();
        }

        if (dp2s == null || dp2s.isEmpty()) {
            System.out.println("DP2Gen.genByStatus: no DP2 MT found for status=" + status + " dataFileUri=" + dataFileUri + "; generating empty workbook");
            return DP2Gen.save(helper, outFilename);
        }

        DP2 dp2 = dp2s.get(0);
        String scopedDataFileUri = dp2.getHasDataFileUri();
        System.out.println("DP2Gen.genByStatus: resolved dp2=" + dp2.getUri() + " hasDataFile=" + canonicalizeUri(scopedDataFileUri) + " hasStatus=" + dp2.getHasStatus());

        String canonicalDataFileUri = canonicalizeUri(scopedDataFileUri);
        if (canonicalDataFileUri == null || canonicalDataFileUri.trim().isEmpty()) {
            System.out.println("[ERROR] DP2Gen.genByStatus: resolved DP2 has invalid hasDataFileUri='" + scopedDataFileUri + "' (canonical='" + canonicalDataFileUri + "'); cannot scope element queries.");
            return DP2Gen.save(helper, outFilename);
        }

        final String graphUriHttps = canonicalDataFileUri.trim();
        final String graphUriHttp = graphUriHttps.replace("https://", "http://");
        System.out.println("DP2Gen.genByStatus(scoped): graphUri(https)=" + graphUriHttps);
        System.out.println("DP2Gen.genByStatus(scoped): graphUri(http)=" + graphUriHttp);

        // IMPORTANT: DP2 ingestion stores type as hasco:hascoType (often as prefixed string),
        // not necessarily rdf:type. So generation must query by hasco:hascoType.
        final String htDeployment = "?uri hasco:hascoType \"vstoi:Deployment\" .";
        final String htPlatform = "?uri hasco:hascoType \"vstoi:Platform\" .";
        final String htPlatformInstance = "?uri hasco:hascoType \"vstoi:PlatformInstance\" .";
        final String htInstrumentInstance = "?uri hasco:hascoType \"vstoi:InstrumentInstance\" .";
        final String htComponentInstance = "?uri hasco:hascoType \"vstoi:ComponentInstance\" .";
        final String htFieldOfView = "?uri hasco:hascoType \"vstoi:FieldOfView\" .";

        java.util.List<Deployment> deployments = null;
        java.util.List<Platform> platforms = null;
        java.util.List<PlatformInstance> platformInstances = null;
        java.util.List<InstrumentInstance> instrumentInstances = null;
        java.util.List<ComponentInstance> componentInstances = null;
        java.util.List<FieldOfView> fieldsOfView = null;

        boolean gotAny = false;

        // -----------------------------
        // GRAPH PASS #1: try https graph
        // -----------------------------
        try {
            final String graphUri = graphUriHttps;
            int tot = graphTripleCount(ns, graphUri);
            System.out.println("[DP2Gen] graph(https) tripleCount=" + tot);

            String qDeploymentsGraph = ns + " SELECT DISTINCT ?uri WHERE { GRAPH <" + graphUri + "> { "
                    + htDeployment + " OPTIONAL { ?uri rdfs:label ?label . } } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;
            String qPlatformsGraph = ns + " SELECT DISTINCT ?uri WHERE { GRAPH <" + graphUri + "> { "
                    + htPlatform + " OPTIONAL { ?uri rdfs:label ?label . } } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;
            String qPlatformInstancesGraph = ns + " SELECT DISTINCT ?uri WHERE { GRAPH <" + graphUri + "> { "
                    + htPlatformInstance + " OPTIONAL { ?uri rdfs:label ?label . } } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;
            String qInstrumentInstancesGraph = ns + " SELECT DISTINCT ?uri WHERE { GRAPH <" + graphUri + "> { "
                    + htInstrumentInstance + " OPTIONAL { ?uri rdfs:label ?label . } } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;
            String qComponentInstancesGraph = ns + " SELECT DISTINCT ?uri WHERE { GRAPH <" + graphUri + "> { "
                    + htComponentInstance + " OPTIONAL { ?uri rdfs:label ?label . } } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;
            String qFieldsOfViewGraph = ns + " SELECT DISTINCT ?uri WHERE { GRAPH <" + graphUri + "> { "
                    + htFieldOfView + " OPTIONAL { ?uri rdfs:label ?label . } } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;

            deployments = org.hascoapi.entity.pojo.GenericFind.findByQuery(Deployment.class, qDeploymentsGraph);
            platforms = org.hascoapi.entity.pojo.GenericFind.findByQuery(Platform.class, qPlatformsGraph);
            platformInstances = org.hascoapi.entity.pojo.GenericFind.findByQuery(PlatformInstance.class, qPlatformInstancesGraph);
            instrumentInstances = org.hascoapi.entity.pojo.GenericFind.findByQuery(InstrumentInstance.class, qInstrumentInstancesGraph);
            componentInstances = org.hascoapi.entity.pojo.GenericFind.findByQuery(ComponentInstance.class, qComponentInstancesGraph);
            fieldsOfView = org.hascoapi.entity.pojo.GenericFind.findByQuery(FieldOfView.class, qFieldsOfViewGraph);

            gotAny =
                    (deployments != null && !deployments.isEmpty()) ||
                    (platforms != null && !platforms.isEmpty()) ||
                    (platformInstances != null && !platformInstances.isEmpty()) ||
                    (instrumentInstances != null && !instrumentInstances.isEmpty()) ||
                    (componentInstances != null && !componentInstances.isEmpty()) ||
                    (fieldsOfView != null && !fieldsOfView.isEmpty());

            System.out.println("[DP2Gen] graph(https) counts deployments=" + (deployments == null ? 0 : deployments.size())
                    + " platforms=" + (platforms == null ? 0 : platforms.size())
                    + " platformInstances=" + (platformInstances == null ? 0 : platformInstances.size())
                    + " instrumentInstances=" + (instrumentInstances == null ? 0 : instrumentInstances.size())
                    + " componentInstances=" + (componentInstances == null ? 0 : componentInstances.size())
                    + " fieldsOfView=" + (fieldsOfView == null ? 0 : fieldsOfView.size()));

        } catch (Exception e) {
            System.out.println("[ERROR][DP2Gen] graph(https) query failed: " + e.getMessage());
            e.printStackTrace();
            gotAny = false;
        }

        // -----------------------------
        // GRAPH PASS #2: http graph fallback
        // -----------------------------
        if (!gotAny && !graphUriHttp.equals(graphUriHttps)) {
            try {
                final String graphUri = graphUriHttp;
                int tot = graphTripleCount(ns, graphUri);
                System.out.println("[DP2Gen] graph(http) tripleCount=" + tot);

                String qDeploymentsGraph = ns + " SELECT DISTINCT ?uri WHERE { GRAPH <" + graphUri + "> { "
                        + htDeployment + " OPTIONAL { ?uri rdfs:label ?label . } } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;
                String qPlatformsGraph = ns + " SELECT DISTINCT ?uri WHERE { GRAPH <" + graphUri + "> { "
                        + htPlatform + " OPTIONAL { ?uri rdfs:label ?label . } } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;
                String qPlatformInstancesGraph = ns + " SELECT DISTINCT ?uri WHERE { GRAPH <" + graphUri + "> { "
                        + htPlatformInstance + " OPTIONAL { ?uri rdfs:label ?label . } } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;
                String qInstrumentInstancesGraph = ns + " SELECT DISTINCT ?uri WHERE { GRAPH <" + graphUri + "> { "
                        + htInstrumentInstance + " OPTIONAL { ?uri rdfs:label ?label . } } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;
                String qComponentInstancesGraph = ns + " SELECT DISTINCT ?uri WHERE { GRAPH <" + graphUri + "> { "
                        + htComponentInstance + " OPTIONAL { ?uri rdfs:label ?label . } } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;
                String qFieldsOfViewGraph = ns + " SELECT DISTINCT ?uri WHERE { GRAPH <" + graphUri + "> { "
                        + htFieldOfView + " OPTIONAL { ?uri rdfs:label ?label . } } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;

                deployments = org.hascoapi.entity.pojo.GenericFind.findByQuery(Deployment.class, qDeploymentsGraph);
                platforms = org.hascoapi.entity.pojo.GenericFind.findByQuery(Platform.class, qPlatformsGraph);
                platformInstances = org.hascoapi.entity.pojo.GenericFind.findByQuery(PlatformInstance.class, qPlatformInstancesGraph);
                instrumentInstances = org.hascoapi.entity.pojo.GenericFind.findByQuery(InstrumentInstance.class, qInstrumentInstancesGraph);
                componentInstances = org.hascoapi.entity.pojo.GenericFind.findByQuery(ComponentInstance.class, qComponentInstancesGraph);
                fieldsOfView = org.hascoapi.entity.pojo.GenericFind.findByQuery(FieldOfView.class, qFieldsOfViewGraph);

                gotAny =
                        (deployments != null && !deployments.isEmpty()) ||
                        (platforms != null && !platforms.isEmpty()) ||
                        (platformInstances != null && !platformInstances.isEmpty()) ||
                        (instrumentInstances != null && !instrumentInstances.isEmpty()) ||
                        (componentInstances != null && !componentInstances.isEmpty()) ||
                        (fieldsOfView != null && !fieldsOfView.isEmpty());

                System.out.println("[DP2Gen] graph(http) counts deployments=" + (deployments == null ? 0 : deployments.size())
                        + " platforms=" + (platforms == null ? 0 : platforms.size())
                        + " platformInstances=" + (platformInstances == null ? 0 : platformInstances.size())
                        + " instrumentInstances=" + (instrumentInstances == null ? 0 : instrumentInstances.size())
                        + " componentInstances=" + (componentInstances == null ? 0 : componentInstances.size())
                        + " fieldsOfView=" + (fieldsOfView == null ? 0 : fieldsOfView.size()));

            } catch (Exception e) {
                System.out.println("[ERROR][DP2Gen] graph(http) query failed: " + e.getMessage());
                e.printStackTrace();
                gotAny = false;
            }
        }

        // Fallback: hasco:hasDataFile scoping (if ingestion added it)
        if (!gotAny) {
            System.out.println("[DP2Gen] graph-scoped empty; trying hasco:hasDataFile scoping using dataFileUri=" + canonicalDataFileUri);

            String qDeploymentsDf = ns + " SELECT DISTINCT ?uri WHERE { " + htDeployment +
                    " ?uri hasco:hasDataFile <" + canonicalDataFileUri + "> . OPTIONAL { ?uri rdfs:label ?label . } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;
            String qPlatformsDf = ns + " SELECT DISTINCT ?uri WHERE { " + htPlatform +
                    " ?uri hasco:hasDataFile <" + canonicalDataFileUri + "> . OPTIONAL { ?uri rdfs:label ?label . } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;
            String qPlatformInstancesDf = ns + " SELECT DISTINCT ?uri WHERE { " + htPlatformInstance +
                    " ?uri hasco:hasDataFile <" + canonicalDataFileUri + "> . OPTIONAL { ?uri rdfs:label ?label . } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;
            String qInstrumentInstancesDf = ns + " SELECT DISTINCT ?uri WHERE { " + htInstrumentInstance +
                    " ?uri hasco:hasDataFile <" + canonicalDataFileUri + "> . OPTIONAL { ?uri rdfs:label ?label . } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;
            String qComponentInstancesDf = ns + " SELECT DISTINCT ?uri WHERE { " + htComponentInstance +
                    " ?uri hasco:hasDataFile <" + canonicalDataFileUri + "> . OPTIONAL { ?uri rdfs:label ?label . } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;
            String qFieldsOfViewDf = ns + " SELECT DISTINCT ?uri WHERE { " + htFieldOfView +
                    " ?uri hasco:hasDataFile <" + canonicalDataFileUri + "> . OPTIONAL { ?uri rdfs:label ?label . } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET;

            deployments = org.hascoapi.entity.pojo.GenericFind.findByQuery(Deployment.class, qDeploymentsDf);
            platforms = org.hascoapi.entity.pojo.GenericFind.findByQuery(Platform.class, qPlatformsDf);
            platformInstances = org.hascoapi.entity.pojo.GenericFind.findByQuery(PlatformInstance.class, qPlatformInstancesDf);
            instrumentInstances = org.hascoapi.entity.pojo.GenericFind.findByQuery(InstrumentInstance.class, qInstrumentInstancesDf);
            componentInstances = org.hascoapi.entity.pojo.GenericFind.findByQuery(ComponentInstance.class, qComponentInstancesDf);
            fieldsOfView = org.hascoapi.entity.pojo.GenericFind.findByQuery(FieldOfView.class, qFieldsOfViewDf);

            System.out.println("[DP2Gen] hasDataFile counts deployments=" + (deployments == null ? 0 : deployments.size())
                    + " platforms=" + (platforms == null ? 0 : platforms.size())
                    + " platformInstances=" + (platformInstances == null ? 0 : platformInstances.size())
                    + " instrumentInstances=" + (instrumentInstances == null ? 0 : instrumentInstances.size())
                    + " componentInstances=" + (componentInstances == null ? 0 : componentInstances.size())
                    + " fieldsOfView=" + (fieldsOfView == null ? 0 : fieldsOfView.size()));
        }

        // Last fallback: global by hasco:hascoType
        boolean stillEmpty =
                (deployments == null || deployments.isEmpty()) &&
                (platforms == null || platforms.isEmpty()) &&
                (platformInstances == null || platformInstances.isEmpty()) &&
                (instrumentInstances == null || instrumentInstances.isEmpty()) &&
                (componentInstances == null || componentInstances.isEmpty()) &&
                (fieldsOfView == null || fieldsOfView.isEmpty());

        if (stillEmpty) {
            System.out.println("[DP2Gen] WARNING: still empty after graph+hasDataFile. Falling back to global by hasco:hascoType.");

            deployments = org.hascoapi.entity.pojo.GenericFind.findByQuery(Deployment.class,
                    ns + " SELECT DISTINCT ?uri WHERE { " + htDeployment + " OPTIONAL { ?uri rdfs:label ?label . } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET);
            platforms = org.hascoapi.entity.pojo.GenericFind.findByQuery(Platform.class,
                    ns + " SELECT DISTINCT ?uri WHERE { " + htPlatform + " OPTIONAL { ?uri rdfs:label ?label . } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET);
            platformInstances = org.hascoapi.entity.pojo.GenericFind.findByQuery(PlatformInstance.class,
                    ns + " SELECT DISTINCT ?uri WHERE { " + htPlatformInstance + " OPTIONAL { ?uri rdfs:label ?label . } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET);
            instrumentInstances = org.hascoapi.entity.pojo.GenericFind.findByQuery(InstrumentInstance.class,
                    ns + " SELECT DISTINCT ?uri WHERE { " + htInstrumentInstance + " OPTIONAL { ?uri rdfs:label ?label . } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET);
            componentInstances = org.hascoapi.entity.pojo.GenericFind.findByQuery(ComponentInstance.class,
                    ns + " SELECT DISTINCT ?uri WHERE { " + htComponentInstance + " OPTIONAL { ?uri rdfs:label ?label . } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET);
            fieldsOfView = org.hascoapi.entity.pojo.GenericFind.findByQuery(FieldOfView.class,
                    ns + " SELECT DISTINCT ?uri WHERE { " + htFieldOfView + " OPTIONAL { ?uri rdfs:label ?label . } } ORDER BY ASC(?label) LIMIT " + PAGESIZE + " OFFSET " + OFFSET);
        }

        System.out.println("DP2Gen.genByStatus(scoped): final counts deployments=" + (deployments == null ? 0 : deployments.size()) +
                " platforms=" + (platforms == null ? 0 : platforms.size()) +
                " platformInstances=" + (platformInstances == null ? 0 : platformInstances.size()) +
                " instrumentInstances=" + (instrumentInstances == null ? 0 : instrumentInstances.size()) +
                " componentInstances=" + (componentInstances == null ? 0 : componentInstances.size()) +
                " fieldsOfView=" + (fieldsOfView == null ? 0 : fieldsOfView.size()));

        if (deployments != null) {
            for (Deployment deployment : deployments) {
                helper = DP2Deployments.add(helper, deployment);
            }
        }
        if (platforms != null) {
            for (Platform platform : platforms) {
                helper = DP2Plataforms.add(helper, platform);
            }
        }
        if (platformInstances != null) {
            for (PlatformInstance platformInstance : platformInstances) {
                helper = DP2PlataformInstances.add(helper, platformInstance);
            }
        }
        if (instrumentInstances != null) {
            for (InstrumentInstance instrumentInstance : instrumentInstances) {
                helper = DP2InstrumentInstances.add(helper, instrumentInstance);
            }
        }
        if (componentInstances != null) {
            for (ComponentInstance componentInstance : componentInstances) {
                helper = DP2ComponentsInstances.add(helper, componentInstance);
            }
        }
        if (fieldsOfView != null) {
            for (FieldOfView fieldOfView : fieldsOfView) {
                helper = DP2FieldsOfView.add(helper, fieldOfView);
            }
        }

        System.out.println("\n→ All data added to workbook, calling save()...");
        System.out.println("  Output filename: " + outFilename);
        String saveResult = DP2Gen.save(helper, outFilename);
        System.out.println("  Save result: " + saveResult);
        System.out.println("========== DP2Gen.genByStatus() END ==========\n");

        return saveResult;
    }


    public static String genByManager(String useremail, String status, String filename, String mediaFolder, String verifyUri) {
        DP2GenHelper helper = new DP2GenHelper();

        String outFilename = filename;
        if (outFilename != null && !outFilename.isEmpty()) {
            java.io.File f = new java.io.File(outFilename);
            if (!f.isAbsolute()) {
                outFilename = ConfigProp.getPathIngestion() + outFilename;
            }
        }

        helper.workbook = DP2Gen.create(outFilename);
        boolean withCurrent = false; // this assures that the retrieval of just elements of the requested type.

        GenericFindWithStatus<Deployment> deploymentQuery = new GenericFindWithStatus<Deployment>();
        List<Deployment> deployments = deploymentQuery.findByStatusManagerEmailWithPages(Deployment.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (deployments != null) {
            for (Deployment deployment: deployments) {
                helper = DP2Deployments.add(helper,deployment);
            }
        }

        GenericFindWithStatus<Platform> platformQuery = new GenericFindWithStatus<Platform>();
        List<Platform> platforms = platformQuery.findByStatusManagerEmailWithPages(Platform.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (platforms != null) {
            for (Platform platform: platforms) {
                helper = DP2Plataforms.add(helper,platform);
            }
        }

        GenericFindWithStatus<PlatformInstance> plataformInstancesQuery = new GenericFindWithStatus<PlatformInstance>();
        List<PlatformInstance> platformInstances = plataformInstancesQuery.findByStatusManagerEmailWithPages(PlatformInstance.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (platformInstances != null) {
            for (PlatformInstance platformInstance: platformInstances) {
                helper = DP2PlataformInstances.add(helper,platformInstance);
            }
        }

        GenericFindWithStatus<InstrumentInstance> instrumentInstanceQuery = new GenericFindWithStatus<InstrumentInstance>();
        List<InstrumentInstance> instrumentInstances = instrumentInstanceQuery.findByStatusManagerEmailWithPages(InstrumentInstance.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (instrumentInstances != null) {
            for (InstrumentInstance instrumentInstance: instrumentInstances) {
                helper = DP2InstrumentInstances.add(helper,instrumentInstance);
            }
        }
        GenericFindWithStatus<ComponentInstance> componentInstanceQuery = new GenericFindWithStatus<ComponentInstance>();
        List<ComponentInstance> componentInstances = componentInstanceQuery.findByStatusManagerEmailWithPages(ComponentInstance.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (componentInstances != null) {
            for (ComponentInstance componentInstance: componentInstances) {
                helper = DP2ComponentsInstances.add(helper,componentInstance);
            }
        }
        GenericFindWithStatus<FieldOfView> fieldOfViewQuery = new GenericFindWithStatus<FieldOfView>();
        List<FieldOfView> fieldsOfView = fieldOfViewQuery.findByStatusManagerEmailWithPages(FieldOfView.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (fieldsOfView != null) {
            for (FieldOfView fieldOfView: fieldsOfView) {
                helper = DP2FieldsOfView.add(helper,fieldOfView);
            }
        }
        /*
        GenericFindWithStatus<SensingPerspective> sensingPerspectiveQuery = new GenericFindWithStatus<SensingPerspective>();
        List<SensingPerspective> sensingPerspectives = sensingPerspectiveQuery.findByStatusManagerEmailWithPages(SensingPerspective.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (sensingPerspectives != null) {
            for (SensingPerspective sensingPerspective: sensingPerspectives) {
                helper = DP2SensingPerspective.add(helper,sensingPerspective);
            }
        }

         */
        return DP2Gen.save(helper, outFilename);
    }

    /**
     * Canonicalizes URIs similarly to DSGGen (decode, strip brackets, expand prefix, normalize #/ and scheme).
     * This avoids mismatches between http/https and encoded/angle-bracket forms.
     */
    private static String canonicalizeUri(String uri) {
        if (uri == null) return null;
        String s = uri.trim();
        if (s.isEmpty()) return null;

        // Best-effort URL-decode (twice, to handle double-encoding)
        for (int i = 0; i < 2; i++) {
            if (s.contains("%")) {
                try {
                    s = java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8.name());
                } catch (Exception ignored) {
                }
            }
        }

        // Strip any surrounding angle brackets
        while (s.startsWith("<") && s.endsWith(">") && s.length() > 2) {
            s = s.substring(1, s.length() - 1).trim();
        }

        // Expand prefixes if possible
        try {
            String expanded = org.hascoapi.utils.URIUtils.replacePrefixEx(s);
            if (expanded != null && !expanded.isEmpty()) {
                s = expanded;
            }
        } catch (Exception ignored) {
        }

        // IMPORTANT: Preserve the common pattern "...hadatac#/...".
        // Some code-paths generate "...hadatac#DFL..." (missing '/'), which breaks graph matching.
        // Normalize ONLY the missing slash after '#', without collapsing '#/'.
        int hashPos = s.indexOf('#');
        if (hashPos >= 0 && hashPos + 1 < s.length() && s.charAt(hashPos + 1) != '/') {
            s = s.substring(0, hashPos + 1) + "/" + s.substring(hashPos + 1);
        }

        // Accept full URIs, otherwise keep a sane prefixed URI form
        if (s.startsWith("http://") || s.startsWith("https://")) {
            return s;
        }
        if (s.matches("^[A-Za-z_][A-Za-z0-9_-]*:.*")) {
            return s;
        }
        return null;
    }

    private static int graphTripleCount(String ns, String graphUri) {
        try {
            String q = ns + " SELECT (COUNT(?s) AS ?tot) WHERE { GRAPH <" + graphUri + "> { ?s ?p ?o } }";
            org.apache.jena.query.ResultSetRewindable rs = org.hascoapi.utils.SPARQLUtils.select(
                    org.hascoapi.utils.CollectionUtil.getCollectionPath(org.hascoapi.utils.CollectionUtil.Collection.SPARQL_QUERY), q);
            if (rs != null && rs.hasNext()) {
                org.apache.jena.query.QuerySolution soln = rs.next();
                return soln.getLiteral("tot").getInt();
            }
        } catch (Exception ignored) {
        }
        return -1;
    }
}

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

        return workbook;
    }

    public static void saveNamespaces(DP2GenHelper helper) {
        if (helper == null || helper.workbook == null) {
            return;
        }

        Sheet namespacesSheet = helper.workbook.getSheet(NAMESPACES);
        Row row = namespacesSheet.createRow(0);
        Cell cell0 = row.createCell(0);
        cell0.setCellValue("prefix");
        Cell cell1 = row.createCell(1);
        cell1.setCellValue("uri");

        int rowIndex = 1;
        for (NameSpace namespace: helper.namespaces.values()) {
            Row newRow = namespacesSheet.createRow(rowIndex);
            Cell newCell0 = newRow.createCell(0);
            newCell0.setCellValue(namespace.getLabel());
            Cell newCell1 = newRow.createCell(1);
            newCell1.setCellValue(namespace.getUri());
            rowIndex++;
        }
        
    }

    public static String save(DP2GenHelper helper, String filename) {
        if (helper == null) {
            return "";
        }
        if (helper.workbook == null) {
            return "";
        }
        // Save namespaces
        DP2Gen.saveNamespaces(helper);

        try {
            FileOutputStream fileOut = new FileOutputStream(filename);
            helper.workbook.write(fileOut);
            fileOut.close();
            helper.workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
        return filename;
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

    public static String genByStatus(String status, String filename, String mediaFolder, String verifyUri) {
        DP2GenHelper helper = new DP2GenHelper();
        helper.workbook = DP2Gen.create(filename);

        GenericFindWithStatus<Deployment> deploymentQuery = new GenericFindWithStatus<Deployment>();
        List<Deployment> deployments = deploymentQuery.findByStatusWithPages(Deployment.class, status, PAGESIZE, OFFSET);
        if (deployments != null) {
            for (Deployment deployment: deployments) {
                helper = DP2Deployments.add(helper,deployment);
            }
        }

        GenericFindWithStatus<Platform> platformQuery = new GenericFindWithStatus<Platform>();
        List<Platform> platforms = platformQuery.findByStatusWithPages(Platform.class, status, PAGESIZE, OFFSET);
        if (platforms != null) {
            for (Platform platform: platforms) {
                helper = DP2Plataforms.add(helper,platform);
            }
        }

        GenericFindWithStatus<PlatformInstance> plataformInstancesQuery = new GenericFindWithStatus<PlatformInstance>();
        List<PlatformInstance> platformInstances = plataformInstancesQuery.findByStatusWithPages(PlatformInstance.class, status, PAGESIZE, OFFSET);
        if (platformInstances != null) {
            for (PlatformInstance platformInstance: platformInstances) {
                helper = DP2PlataformInstances.add(helper,platformInstance);
            }
        }

        GenericFindWithStatus<InstrumentInstance> instrumentInstanceQuery = new GenericFindWithStatus<InstrumentInstance>();
        List<InstrumentInstance> instrumentInstances = instrumentInstanceQuery.findByStatusWithPages(InstrumentInstance.class, status, PAGESIZE, OFFSET);
        if (instrumentInstances != null) {
            for (InstrumentInstance instrumentInstance: instrumentInstances) {
                helper = DP2InstrumentInstances.add(helper,instrumentInstance);
            }
        }
        GenericFindWithStatus<ComponentInstance> componentInstanceQuery = new GenericFindWithStatus<ComponentInstance>();
        List<ComponentInstance> componentInstances = componentInstanceQuery.findByStatusWithPages(ComponentInstance.class, status, PAGESIZE, OFFSET);
        if (componentInstances != null) {
            for (ComponentInstance componentInstance: componentInstances) {
                helper = DP2ComponentsInstances.add(helper,componentInstance);
            }
        }
        GenericFindWithStatus<FieldOfView> fieldOfViewQuery = new GenericFindWithStatus<FieldOfView>();
        List<FieldOfView> fieldsOfView = fieldOfViewQuery.findByStatusWithPages(FieldOfView.class, status, PAGESIZE, OFFSET);
        if (fieldsOfView != null) {
            for (FieldOfView fieldOfView: fieldsOfView) {
                helper = DP2FieldsOfView.add(helper,fieldOfView);
            }
        }
        /*
        GenericFindWithStatus<SensingPerspective> sensingPerspectiveQuery = new GenericFindWithStatus<SensingPerspective>();
        List<SensingPerspective> sensingPerspectives = sensingPerspectiveQuery.findByStatusWithPages(SensingPerspective.class, status, PAGESIZE, OFFSET);
        if (sensingPerspectives != null) {
            for (SensingPerspective sensingPerspective: sensingPerspectives) {
                helper = DP2SensingPerspective.add(helper,sensingPerspective);
            }
        }

         */
        return DP2Gen.save(helper,filename);
    }

    public static String genByManager(String useremail, String status, String filename, String mediaFolder, String verifyUri) {
        DP2GenHelper helper = new DP2GenHelper();
        helper.workbook = DP2Gen.create(filename);
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
        return DP2Gen.save(helper,filename);
    }
}

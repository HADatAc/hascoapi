package org.hascoapi.transform.mt.kgr;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.hascoapi.annotations.PropertyField;
import org.hascoapi.entity.pojo.FundingScheme;
import org.hascoapi.entity.pojo.Project;
import org.hascoapi.entity.pojo.Organization;
import org.hascoapi.entity.pojo.Person;
import org.hascoapi.entity.pojo.Place;
import org.hascoapi.entity.pojo.PostalAddress;
import org.hascoapi.entity.pojo.GenericFindWithStatus;
import org.hascoapi.utils.ConfigProp;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class KGRGen {

    public static final String INFOSHEET            = "InfoSheet";
    public static final String NAMESPACES           = "Namespaces";
    public static final String FUNDING_SCHEMES      = "FundingSchemes";
    public static final String PROJECTS             = "Projects";
    public static final String ORGANIZATIONS        = "Organizations";
    public static final String PERSONS              = "Persons";
    public static final String PLACES               = "Places";
    public static final String POSTAL_ADDRESSES     = "PostalAddresses";

    public static final int PAGESIZE                = 20000;
    public static final int OFFSET                  = 0;

    public static String genByStatus(String status, String filename) {
        KGRGenHelper helper = new KGRGenHelper();
        helper.workbook = KGRGen.create(filename);
        String resp = "";

        GenericFindWithStatus<FundingScheme> fundingSchemeQuery = new GenericFindWithStatus<FundingScheme>();
        List<FundingScheme> fundingSchemes = fundingSchemeQuery.findByStatusWithPages(FundingScheme.class, status, PAGESIZE, OFFSET);
        if (fundingSchemes != null) {
            for (FundingScheme fundingScheme: fundingSchemes) {
                helper = KGRFundingScheme.add(helper,fundingScheme);
            }
        }
        GenericFindWithStatus<Project> projectQuery = new GenericFindWithStatus<Project>();
        List<Project> projects = projectQuery.findByStatusWithPages(Project.class, status, PAGESIZE, OFFSET);
        if (projects != null) {
            for (Project project: projects) {
                helper = KGRProject.add(helper,project);
            }
        }
        GenericFindWithStatus<Organization> organizationQuery = new GenericFindWithStatus<Organization>();
        List<Organization> organizations = organizationQuery.findByStatusWithPages(Organization.class, status, PAGESIZE, OFFSET);
        if (organizations != null) {
            for (Organization organization: organizations) {
                helper = KGROrganization.add(helper,organization);
            }
        }
        GenericFindWithStatus<Person> personQuery = new GenericFindWithStatus<Person>();
        List<Person> persons = personQuery.findByStatusWithPages(Person.class, status, PAGESIZE, OFFSET);
        if (persons != null) {
            for (Person person: persons) {
                helper = KGRPerson.add(helper,person);
            }
        }
        GenericFindWithStatus<Place> placeQuery = new GenericFindWithStatus<Place>();
        List<Place> places = placeQuery.findByStatusWithPages(Place.class, status, PAGESIZE, OFFSET);
        if (places != null) {
            for (Place place: places) {
                helper = KGRPlace.add(helper,place);
            }
        }
        GenericFindWithStatus<PostalAddress> postalAddressQuery = new GenericFindWithStatus<PostalAddress>();
        List<PostalAddress> pas = postalAddressQuery.findByStatusWithPages(PostalAddress.class, status, PAGESIZE, OFFSET);
        if (pas != null) {
            for (PostalAddress pa: pas) {
                helper = KGRPostalAddress.add(helper,pa);
            }
        }
        return KGRGen.save(helper,filename);
    }

    public static String genByProject(Project project, String filename) {
        if (project == null) {
            return "";
        }
        System.out.println("KGRGen: genByProject with project=[" + project.getLabel() + "]");
        KGRGenHelper helper = new KGRGenHelper();
        helper.workbook = KGRGen.create(filename);
        System.out.println("KGRGen: genByProject created file with filename=[" + filename + "]");
        
        helper = KGRProject.add(helper,project);
        if (helper.fundingSchemes.size() > 0) {
            for (FundingScheme fundingScheme : helper.fundingSchemes.values()) {
                helper = KGRFundingScheme.add(helper,fundingScheme);
            }
        }
        if (helper.organizations.size() > 0) {
            for (Organization organization : helper.organizations.values()) {
                helper = KGROrganization.add(helper,organization);
            }
        }
        if (helper.persons.size() > 0) {
            for (Person person : helper.persons.values()) {
                helper = KGRPerson.add(helper,person);
            }
        }
        if (helper.places.size() > 0) {
            for (Place place : helper.places.values()) {
                helper = KGRPlace.add(helper,place);
            }            
        }
        if (helper.postalAddresses.size() > 0) {
            for (PostalAddress postalAddress : helper.postalAddresses.values()) {
                helper = KGRPostalAddress.add(helper,postalAddress);
            }            
        }

        return KGRGen.save(helper, filename);
    }

    public static String genByManager(String useremail, String status, String filename) {
        KGRGenHelper helper = new KGRGenHelper();
        helper.workbook = KGRGen.create(filename);
        boolean withCurrent = false; // this assures that the retrieval of just elements of the requested type.

        GenericFindWithStatus<FundingScheme> fundingSchemeQuery = new GenericFindWithStatus<FundingScheme>();
        List<FundingScheme> fundingSchemes = fundingSchemeQuery.findByStatusManagerEmailWithPages(FundingScheme.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (fundingSchemes != null) {
            for (FundingScheme fundingScheme: fundingSchemes) {
                KGRFundingScheme.add(helper,fundingScheme);
            }
        }
        GenericFindWithStatus<Project> projectQuery = new GenericFindWithStatus<Project>();
        List<Project> projects = projectQuery.findByStatusManagerEmailWithPages(Project.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (projects != null) {
            for (Project project: projects) {
                KGRProject.add(helper,project);
            }
        }
        GenericFindWithStatus<Organization> organizationQuery = new GenericFindWithStatus<Organization>();
        List<Organization> organizations = organizationQuery.findByStatusManagerEmailWithPages(Organization.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (organizations != null) {
            for (Organization organization: organizations) {
                KGROrganization.add(helper,organization);
            }
        }
        GenericFindWithStatus<Person> personQuery = new GenericFindWithStatus<Person>();
        List<Person> persons = personQuery.findByStatusManagerEmailWithPages(Person.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (persons != null) {
            for (Person person: persons) {
                KGRPerson.add(helper,person);
            }
        }
        GenericFindWithStatus<Place> placeQuery = new GenericFindWithStatus<Place>();
        List<Place> places = placeQuery.findByStatusManagerEmailWithPages(Place.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (places != null) {
            for (Place place: places) {
                KGRPlace.add(helper,place);
            }
        }
        GenericFindWithStatus<PostalAddress> paQuery = new GenericFindWithStatus<PostalAddress>();
        List<PostalAddress> pas = paQuery.findByStatusManagerEmailWithPages(PostalAddress.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
        if (pas != null) {
            for (PostalAddress pa: pas) {
                KGRPostalAddress.add(helper,pa);
            }
        }
        return KGRGen.save(helper,filename);
    }

    public static Workbook create(String filename) {

        // Create a new workbook
        Workbook workbook = new XSSFWorkbook();

        // Create sheet named 'InfoSheet'
        Sheet infoSheet = workbook.createSheet("InfoSheet");

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
        isDataCell1_2.setCellValue("#" + KGRGen.NAMESPACES);

        Row dataRow2 = infoSheet.createRow(2);
        Cell isDataCell2_1 = dataRow2.createCell(0);
        isDataCell2_1.setCellValue("FundingSchemes");
        Cell isDataCell2_2 = dataRow2.createCell(1);
        isDataCell2_2.setCellValue("#" + KGRGen.FUNDING_SCHEMES);

        Row isDataRow3 = infoSheet.createRow(3);
        Cell isDataCell3_1 = isDataRow3.createCell(0);
        isDataCell3_1.setCellValue("Projects");
        Cell isDataCell3_2 = isDataRow3.createCell(1);
        isDataCell3_2.setCellValue("#" + KGRGen.PROJECTS);

        Row isDataRow4 = infoSheet.createRow(4);
        Cell isDataCell4_1 = isDataRow4.createCell(0);
        isDataCell4_1.setCellValue("Organizations");
        Cell isDataCell4_2 = isDataRow4.createCell(1);
        isDataCell4_2.setCellValue("#" + KGRGen.ORGANIZATIONS);

        Row isDataRow5 = infoSheet.createRow(5);
        Cell isDataCell5_1 = isDataRow5.createCell(0);
        isDataCell5_1.setCellValue("Persons");
        Cell isDataCell5_2 = isDataRow5.createCell(1);
        isDataCell5_2.setCellValue("#" + KGRGen.PERSONS);

        Row isDataRow6 = infoSheet.createRow(6);
        Cell isDataCell6_1 = isDataRow6.createCell(0);
        isDataCell6_1.setCellValue("Places");
        Cell isDataCell6_2 = isDataRow6.createCell(1);
        isDataCell6_2.setCellValue("#" + KGRGen.PLACES);

        Row isDataRow7 = infoSheet.createRow(7);
        Cell isDataCell7_1 = isDataRow7.createCell(0);
        isDataCell7_1.setCellValue("PostalAddresses");
        Cell isDataCell7_2 = isDataRow7.createCell(1);
        isDataCell7_2.setCellValue("#" + KGRGen.POSTAL_ADDRESSES);

          // Create sheet named 'Namespaces'
        Sheet nsSheet = workbook.createSheet(KGRGen.NAMESPACES);
        String[] nsHeaders = { "hasPrefix", "hasNameSpace", "hasFormat", "hasSource" };

        // Create header row
        Row nsHeaderRow = nsSheet.createRow(0);
        for (int i = 0; i < nsHeaders.length; i++) {
            Cell cell = nsHeaderRow.createCell(i);
            cell.setCellValue(nsHeaders[i]);
        }
        for (int i = 0; i < nsHeaders.length; i++) {
            nsSheet.autoSizeColumn(i);
        }

        // Create sheet named 'FundingSchemes'
        Sheet fundingSchemeSheet = workbook.createSheet(KGRGen.FUNDING_SCHEMES);
        String[] fundingSchemeHeaders = { "hasURI", "hasco:hascoType", "rdfs:label",     
            "schema:alternateName", "schema:funder", "schema:sponsor", "schema:startDate", 
            "schema:endDate", "schema:amount" };

        // Create header row
        Row fundingSchemeHeaderRow = fundingSchemeSheet.createRow(0);
        for (int i = 0; i < fundingSchemeHeaders.length; i++) {
            Cell cell = fundingSchemeHeaderRow.createCell(i);
            cell.setCellValue(fundingSchemeHeaders[i]);
        }
        for (int i = 0; i < fundingSchemeHeaders.length; i++) {
            fundingSchemeSheet.autoSizeColumn(i);
        }

        // Create sheet named 'Projects'
        Sheet projectSheet = workbook.createSheet(KGRGen.PROJECTS);
        String[] projectHeaders = { "hasURI", "hasco:hascoType",  "schema:alternateName", 
            "rdfs:label", "schema:url",  "rdfs:comment", 
            "schema:funding", "schema:startDate", "schema:endDate" };

        // Create header row
        Row projectHeaderRow = projectSheet.createRow(0);
        for (int i = 0; i < projectHeaders.length; i++) {
            Cell cell = projectHeaderRow.createCell(i);
            cell.setCellValue(projectHeaders[i]);
        }
        for (int i = 0; i < projectHeaders.length; i++) {
            projectSheet.autoSizeColumn(i);
        }

        // Create sheet named 'Organization'
        Sheet organizationSheet = workbook.createSheet(KGRGen.ORGANIZATIONS);
        String[] organizationHeaders = { "hasURI", "hasco:hascoType", "rdfs:label", 
            "originalID", 	"rdf:type", "foaf:name", "foaf:mbox", 
        	"schema:telephone", "schema:url", "schema:parentOrganization" };

        // Create header row
        Row organizationHeaderRow = organizationSheet.createRow(0);
        for (int i = 0; i < organizationHeaders.length; i++) {
            Cell cell = organizationHeaderRow.createCell(i);
            cell.setCellValue(organizationHeaders[i]);
        }
        for (int i = 0; i < organizationHeaders.length; i++) {
            organizationSheet.autoSizeColumn(i);
        }

        // Create sheet named 'Person'
        Sheet personSheet = workbook.createSheet(KGRGen.PERSONS);
        String[] personHeaders = { "hasURI", "hasco:hascoType", "rdfs:label", 
            "originalID", "rdf:type", "foaf:givenName", "foaf:familyName", 
        	"foaf:mbox", "foaf:member" };

        // Create header row
        Row personHeaderRow = personSheet.createRow(0);
        for (int i = 0; i < personHeaders.length; i++) {
            Cell cell = personHeaderRow.createCell(i);
            cell.setCellValue(personHeaders[i]);
        }
        for (int i = 0; i < personHeaders.length; i++) {
            personSheet.autoSizeColumn(i);
        }

        // Create sheet named 'Place'
        Sheet placeSheet = workbook.createSheet(KGRGen.PLACES);
        String[] placeHeaders = { "hasURI", "hasco:hascoType", "rdfs:label", 
            "originalID", "rdf:type", "foaf:name", "hasco:hasImage", 	
            "schema:containedInPlace", 	"schema:identifier", "schema:geo",	"schema:latitude", 
            "schema:longitude",	"schema:url" };

        // Create header row
        Row placeHeaderRow = placeSheet.createRow(0);
        for (int i = 0; i < placeHeaders.length; i++) {
            Cell cell = placeHeaderRow.createCell(i);
            cell.setCellValue(placeHeaders[i]);
        }
        for (int i = 0; i < placeHeaders.length; i++) {
            placeSheet.autoSizeColumn(i);
        }

        // Create sheet named 'PostalAddress'
        Sheet postalAddressSheet = workbook.createSheet(KGRGen.POSTAL_ADDRESSES);
        String[] postalAddressHeaders = { "hasURI", "hasco:hascoType", "rdfs:label", 
            "rdf:type", "schema:streetAddress", 	
            "schema:addressLocality", "schema:addressRegion", "schema:addressCountry", "schema:postalCode" };

        // Create header row
        Row postalAddressHeaderRow = postalAddressSheet.createRow(0);
        for (int i = 0; i < postalAddressHeaders.length; i++) {
            Cell cell = postalAddressHeaderRow.createCell(i);
            cell.setCellValue(postalAddressHeaders[i]);
        }
        for (int i = 0; i < postalAddressHeaders.length; i++) {
            postalAddressSheet.autoSizeColumn(i);
        }

        // Define the permanent file path
        String pathString = ConfigProp.getPathIngestion() + filename;

        // Write the workbook content to a file
        try (FileOutputStream fileOut = new FileOutputStream(pathString)) {
            workbook.write(fileOut);
            System.out.println("Empty KGR workbook created successfully!");
            System.out.println("KGR workbook path is [" + pathString + "]");
        } catch (IOException e) {
            System.out.println("Error occurred while writing the workbook: " + e.getMessage());
        } 
        
        return workbook;
    }


    public static String save(KGRGenHelper helper, String filename) {
        // Define the permanent file path
        String pathString = ConfigProp.getPathIngestion() + filename;

        String resp = "";
        // Write the workbook content to a file
        try (FileOutputStream fileOut = new FileOutputStream(pathString)) {
            helper.workbook.write(fileOut);
            System.out.println("KGR workbook saved successfully!");
        } catch (IOException e) {
            resp = "Error occurred while writing the workbook: " + e.getMessage();
            System.out.println("Error occurred while writing the workbook: " + e.getMessage());
        } 

        return resp;
    }
}

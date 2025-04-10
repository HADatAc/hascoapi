package org.hascoapi.ingestion;

import java.lang.String;
import java.util.*;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.KGR;

public class AnnotateKGR {

    public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        System.out.println("AnnotateKGR.buildChain(): Processing KGR file ...");
 
        System.out.println("AnnotateKGR.buildChain(): Build chain 1 of 8 - Reading catalog and template");

        RecordFile recordFile = new SpreadsheetRecordFile(dataFile.getFile(), "InfoSheet");
        if (!recordFile.isValid()) {
            dataFile.getLogger().printExceptionById("DPL_00001");
            return null;
        } else {
            dataFile.setRecordFile(recordFile);
        }

        Map<String, String> mapCatalog = new HashMap<String, String>();
        for (Record record : dataFile.getRecordFile().getRecords()) {
            mapCatalog.put(record.getValueByColumnIndex(0), record.getValueByColumnIndex(1));
            System.out.println(record.getValueByColumnIndex(0) + ":" + record.getValueByColumnIndex(1));
        }
        
        KGR kgr = new KGR(dataFile, templateFile);
        kgr.setHasDataFileUri(dataFile.getUri());
        kgr.setHasSIRManagerEmail(dataFile.getHasSIRManagerEmail());
        System.out.println("DataFileUri: [" + dataFile.getUri() + "]");
        System.out.println("DataFileUri: [" + kgr.getHasDataFileUri() + "]");

        // the template is needed to process individual sheets
        kgr.setTemplates(templateFile);

        String hasMediaFolder = null;
        if (mapCatalog.get("hasMediaFolder") != null) {
            hasMediaFolder = mapCatalog.get("hasMediaFolder");
        }

        System.out.println("AnnotateKGR.buildChain(): Build chain 2 of 8 - Creating empty generator chain");

        GeneratorChain chain = new GeneratorChain();
        
        RecordFile sheet = null;

        System.out.println("AnnotateKGR.buildChain(): Build chain 3 of 8 - Adding place into generation chain");
        String placeSheet = mapCatalog.get("Places");
        if (placeSheet == null) {
            System.out.println("[WARNING] 'Places' sheet is missing.");
            dataFile.getLogger().println("[WARNING] 'Places' sheet is missing.");
        } else {
            placeSheet.replace("#", "");
            sheet = new SpreadsheetRecordFile(dataFile.getFile(), placeSheet);
            try {
                DataFile dataFileForSheet = (DataFile)dataFile.clone();
                dataFileForSheet.setRecordFile(sheet);
                KGRGenerator placeGen = new KGRGenerator("place", status, dataFileForSheet, hasMediaFolder);
                placeGen.setNamedGraphUri(dataFileForSheet.getUri());
                chain.addGenerator(placeGen);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("AnnotateKGR.buildChain(): Build chain 4 of 8 - Adding postal address into generation chain");
        String postalAddressSheet = mapCatalog.get("PostalAddresses");
        if (postalAddressSheet == null) {
            System.out.println("[WARNING] 'PostalAddresses' sheet is missing.");
            dataFile.getLogger().println("[WARNING] 'PostalAddresses' sheet is missing.");
        } else {
            postalAddressSheet.replace("#", "");
            sheet = new SpreadsheetRecordFile(dataFile.getFile(), postalAddressSheet);
            try {
                DataFile dataFileForSheet = (DataFile)dataFile.clone();
                dataFileForSheet.setRecordFile(sheet);
                KGRGenerator postalAddressGen = new KGRGenerator("postaladdress", status, dataFileForSheet, hasMediaFolder);
                postalAddressGen.setNamedGraphUri(dataFileForSheet.getUri());
                chain.addGenerator(postalAddressGen);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("AnnotateKGR.buildChain(): Build chain 5 of 8 - Adding organization into generation chain");
        String organizationSheet = mapCatalog.get("Organizations");
        if (organizationSheet == null) {
            System.out.println("[WARNING] 'Organizations' sheet is missing.");
            dataFile.getLogger().println("[WARNING] 'Organizations' sheet is missing.");
        } else {
            organizationSheet.replace("#", "");
            sheet = new SpreadsheetRecordFile(dataFile.getFile(), organizationSheet);
            try {
                DataFile dataFileForSheet = (DataFile)dataFile.clone();
                dataFileForSheet.setRecordFile(sheet);
                KGRGenerator organizationGen = new KGRGenerator("organization", status, dataFileForSheet, hasMediaFolder);
                organizationGen.setNamedGraphUri(dataFileForSheet.getUri());
                chain.addGenerator(organizationGen);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("AnnotateKGR.buildChain(): Build chain 6 of 8 - Adding person into generation chain");
        String personSheet = mapCatalog.get("Persons");
        if (personSheet == null) {
            System.out.println("[WARNING] 'Persons' sheet is missing.");
            dataFile.getLogger().println("[WARNING] 'Persons' sheet is missing.");
        } else {
            personSheet.replace("#", "");
            sheet = new SpreadsheetRecordFile(dataFile.getFile(), personSheet);
            try {
                DataFile dataFileForSheet = (DataFile)dataFile.clone();
                dataFileForSheet.setRecordFile(sheet);
                KGRGenerator personGen = new KGRGenerator("person", status, dataFileForSheet, hasMediaFolder);
                personGen.setNamedGraphUri(dataFileForSheet.getUri());
                chain.addGenerator(personGen);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("AnnotateKGR.buildChain(): Build chain 7 of 8 - Adding project into generation chain");
        String projectSheet = mapCatalog.get("Projects");
        if (projectSheet == null) {
            System.out.println("[WARNING] 'Projects' sheet is missing.");
            dataFile.getLogger().println("[WARNING] 'Projects' sheet is missing.");
        } else {
            projectSheet.replace("#", "");
            sheet = new SpreadsheetRecordFile(dataFile.getFile(), projectSheet);
            try {
                DataFile dataFileForSheet = (DataFile)dataFile.clone();
                dataFileForSheet.setRecordFile(sheet);
                KGRGenerator projectGen = new KGRGenerator("project", status, dataFileForSheet, hasMediaFolder);
                projectGen.setNamedGraphUri(dataFileForSheet.getUri());
                chain.addGenerator(projectGen);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("AnnotateKGR.buildChain(): Build chain 8 of 8 - Adding funding scheme into generation chain");
        String fundingSchemeSheet = mapCatalog.get("FundingSchemes");
        if (fundingSchemeSheet == null) {
            System.out.println("[WARNING] 'FundingSchemes' sheet is missing.");
            dataFile.getLogger().println("[WARNING] 'FundingSchemes' sheet is missing.");
        } else {
            fundingSchemeSheet.replace("#", "");
            sheet = new SpreadsheetRecordFile(dataFile.getFile(), fundingSchemeSheet);
            try {
                DataFile dataFileForSheet = (DataFile)dataFile.clone();
                dataFileForSheet.setRecordFile(sheet);
                KGRGenerator fundingSchemeGen = new KGRGenerator("fundingscheme", status, dataFileForSheet, hasMediaFolder);
                fundingSchemeGen.setNamedGraphUri(dataFileForSheet.getUri());
                chain.addGenerator(fundingSchemeGen);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        return chain;
    }

}

package org.hascoapi.ingestion;

import java.lang.String;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
//import org.hascoapi.data.api.DataFactory;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.KGR;
import org.hascoapi.entity.pojo.Organization;
import org.hascoapi.entity.pojo.Person;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.NameSpaces;

import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

public class IngestKGR {

    public static void exec(KGR kgr, DataFile dataFile, File file, String templateFile) {

        System.out.println("IngestKGR.exec(): Step 1 of 3: " + 
            "Processing file [" + dataFile.getFilename() + "] " + 
            "Last process time [" + dataFile.getLastProcessTime() + "]");
        String fileName = dataFile.getFilename();
        dataFile.getLogger().println(String.format("Processing file: %s", fileName));

        System.out.println("IngestKGR.exec() Step 2 of 3: Adding file content into RecordFile");

        // file is rejected if it has an invalid extension. Otherwise, it is added into RecordFile
        RecordFile recordFile = null;
        if (fileName.endsWith(".xlsx")) {
            recordFile = new SpreadsheetRecordFile(file);
        } else {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00003", fileName);
            return;
        }
        dataFile.setRecordFile(recordFile);
        
        boolean bSucceed = false;
        GeneratorChain chain = null;

        System.out.println("IngestKGR.exec(): Step 3 of 3: Calling IngestKGR.buildChain()");
        if (fileName.startsWith("KGR-")) {
            chain = buildChain(kgr, dataFile, file, templateFile);           
        } 

        if (chain != null) {
            System.out.println("Executing generation chain...");
            bSucceed = chain.generate();
            System.out.println("Generation chain has been executed.");
        }

        if (bSucceed) {
            dataFile.setFileStatus(DataFile.PROCESSED);
            dataFile.setCompletionTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
            dataFile.save();
            return;
        } 
        return;
    }

    /****************************
     *    KGR                   *
     ****************************/    
    
    public static GeneratorChain buildChain(KGR kgr, DataFile dataFile, File file, String templateFile) {
        System.out.println("IngestKGR.buildChain(): Processing KGR file ...");
        
        RecordFile recordFile = new SpreadsheetRecordFile(file, "InfoSheet");
        if (!recordFile.isValid()) {
            dataFile.getLogger().printExceptionById("SDD_00001");
            return null;
        } 

        System.out.println("IngestKGR.buildChain(): Build chain 1 of 9 - Reading catalog and template");

        Map<String, String> mapCatalog = kgr.getCatalog();
        for (Record record : recordFile.getRecords()) {
            mapCatalog.put(record.getValueByColumnIndex(0), record.getValueByColumnIndex(1));
            System.out.println(record.getValueByColumnIndex(0) + ":" + record.getValueByColumnIndex(1));
        }

        // the template is needed to process individual sheets
        kgr.setTemplates(templateFile);

        System.out.println("IngestKGR.buildChain(): Build chain 2 of 9 - Separating sheets apart");

        RecordFile placeRecordFile = null;
        RecordFile organizationRecordFile = null;
        RecordFile personRecordFile = null;
        boolean placeOkay = false;
        boolean organizationOkay = false;
        boolean personOkay = false;

        String fileName = dataFile.getFilename();

        if (fileName.endsWith(".xlsx")) {

            if (mapCatalog.get("Place") != null) {
                System.out.print("Extracting place sheet from spreadsheet... ");
                placeRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get("Place"));
                System.out.println(placeRecordFile.getSheetName());
                placeOkay = true;
            }
            
            if (mapCatalog.get("Organization") != null) { 
                System.out.print("Extrating organization sheet from spreadsheet... ");
                organizationRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get("Organization"));
                System.out.println(organizationRecordFile.getSheetName());
                organizationOkay = true;
            }
            
            if (mapCatalog.get("Person") != null) {
                System.out.print("Extracting person sheet from spreadsheet... ");
                personRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get("Person"));
                //replace("#","")
                System.out.println(personRecordFile.getSheetName());
                personOkay = true;
            }
            
        }

        System.out.println("IngestKGR.buildChain(): Build chain 3 of 9 - Processing Place Sheet");

        if (placeOkay && !kgr.readPlaces(placeRecordFile)) {
            System.out.println("[ERROR] failed KGR's read places");
            placeOkay = false;
        }

        System.out.println("IngestKGR.buildChain(): Build chain 4 of 9 - Processing Organization Sheet");

        if (organizationOkay && !kgr.readOrganizations(organizationRecordFile)) {
            System.out.println("[ERROR] failed KGR's read organizations");
            organizationOkay = false;
        }

        System.out.println("IngestKGR.buildChain(): Build chain 5 of 9 - Processing Person Sheet");

        if (personOkay && !kgr.readPersons(personRecordFile)) {
            dataFile.getLogger().printWarningById("SDD_00005");
            System.out.println("[ERROR] failed KGR's read persons");
            personOkay = false;
        }

        System.out.println("IngestSDD.buildChain(): Build chain 6 of 9 - Creating empty generator chain");

        GeneratorChain chain = new GeneratorChain();
        
        System.out.println("IngestSDD.buildChain(): Build chain 7 of 9 - Adding PlaceGenerator into generation chain");

        if (placeOkay && placeRecordFile != null && placeRecordFile.isValid()) {
            DataFile placeFile;
            try {
                placeFile = (DataFile)dataFile.clone();
                placeFile.setRecordFile(placeRecordFile);
                PlaceGenerator placeGen = new PlaceGenerator(placeFile, templateFile, kgr.getHasSIRManagerEmail());
                placeGen.setNamedGraphUri(kgr.getHasDataFile());
                chain.addGenerator(placeGen);
                System.out.println("Adding PlaceGenerator into generation chain...");
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                System.out.println("[ERROR] failed KGR's adding PlaceGenerator into generation chain ");
                placeOkay = false;
            }
        }

        System.out.println("IngestSDD.buildChain(): Build chain 8 of 9 - Adding OrganizationGenerator into generation chain");

        if (organizationOkay && organizationRecordFile != null && organizationRecordFile.isValid()) {
            DataFile organizationFile;
            try {
                organizationFile = (DataFile)dataFile.clone();
                organizationFile.setRecordFile(organizationRecordFile);
                OrganizationGenerator orgGen = new OrganizationGenerator(organizationFile, templateFile, kgr.getHasSIRManagerEmail());
                orgGen.setNamedGraphUri(kgr.getHasDataFile());
                chain.addGenerator(orgGen);
                System.out.println("Adding OrganizationGenerator into generation chain...");
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                System.out.println("[ERROR] failed KGR's adding OrganizationGenerator into generation chain ");
                organizationOkay = false;
            }
        }

        System.out.println("IngestSDD.buildChain(): Build chain 9 of 9 - Adding PersonGenerator into generation chain");

        // persons needs to be processed after data organizations because or persons 
        // need to be assigned members of organizations
                
        if (personOkay && personRecordFile != null && personRecordFile.isValid()) {
            DataFile personFile;
            try {
                personFile = (DataFile)dataFile.clone();
                personFile.setRecordFile(personRecordFile);
                PersonGenerator perGen = new PersonGenerator(personFile, templateFile, kgr.getHasSIRManagerEmail());
                perGen.setNamedGraphUri(kgr.getHasDataFile());
                chain.addGenerator(perGen);
                System.out.println("Adding PersonGenerator into generation chain...");
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                System.out.println("[ERROR] failed KGR's adding PersonGenerator into generation chain ");
                personOkay = false;
            }
        }

        if (!placeOkay) {
            System.out.println("[ERROR] failed KGR's Place generation");
        }

        if (!personOkay) {
            System.out.println("[ERROR] failed KGR's Organization generation");
        }

        if (!personOkay) {
            System.out.println("[ERROR] failed KGR's Person generation");
        }

        return chain;
    }
}

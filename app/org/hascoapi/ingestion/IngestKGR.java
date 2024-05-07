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

        System.out.println("IngestKGR.exec(): Step 1 of 5: Processing file: " + dataFile.getFilename());

        String fileName = dataFile.getFilename();

        System.out.println("IngestKGR.exec() Step 2 of 5: Last process time [" + dataFile.getLastProcessTime() + "]");

        System.out.println("IngestKGR.exec() Step 3 of 5: adding file content into RecordFile");

        dataFile.getLogger().println(String.format("Processing file: %s", fileName));

        // file is rejected if it has an invalid extension
        RecordFile recordFile = null;
        if (fileName.endsWith(".xlsx")) {
            recordFile = new SpreadsheetRecordFile(file);
        } else {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00003", fileName);
            return;
        }
        
        System.out.println("IngestKGR.exec() Step 4 of 5: adding RecordFile into DataFile");

        dataFile.setRecordFile(recordFile);

        boolean bSucceed = false;
        GeneratorChain chain = null;

        if (fileName.startsWith("KGR-")) {
            System.out.println("IngestKGR.exec(): Step 5 of 5: calling IngestKGR.buildChain()");
            chain = buildChain(kgr, dataFile, file, templateFile);           
            System.out.println("AQUI");
        } 

        if (chain != null) {
            System.out.println("Executing generation chain...");
            bSucceed = chain.generate();
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
        System.out.println("IngestKGR.buildChain(): Build chain 1 of 10 - reading catalog and template");

        Map<String, String> mapCatalog = kgr.getCatalog();
        for (Record record : recordFile.getRecords()) {
            mapCatalog.put(record.getValueByColumnIndex(0), record.getValueByColumnIndex(1));
            System.out.println(record.getValueByColumnIndex(0) + ":" + record.getValueByColumnIndex(1));
        }

        // the template is needed to process individual sheets
        kgr.setTemplates(templateFile);

        System.out.println("IngestKGR.buildChain(): Build chain 2  of 10 - separating sheets apart");

        RecordFile organizationRecordFile = null;
        RecordFile personRecordFile = null;

        String fileName = dataFile.getFilename();

        if (fileName.endsWith(".xlsx")) {

            if (mapCatalog.get("Organization") != null) { 
                System.out.print("Extrating organization sheet from spreadsheet... ");
                organizationRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get("Organization"));
                System.out.println(organizationRecordFile.getSheetName());
            }
            
            if (mapCatalog.get("Person") != null) {
                System.out.print("Extracting person sheet from spreadsheet... ");
                personRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get("Person"));
                //replace("#","")
                System.out.println(personRecordFile.getSheetName());
            }
            
        }

        System.out.println("IngestKGR.buildChain(): Build chain 3 of 10 - Processing Organization Sheet");

        if (!kgr.readOrganizations(organizationRecordFile)) {
            System.out.println("[ERROR] failed KGR's read organizations");
            return null;
        }

        System.out.println("IngestKGR.buildChain(): Build chain 3 of 10 - Processing Organization Sheet");

        if (!kgr.readPersons(personRecordFile)) {
            dataFile.getLogger().printWarningById("SDD_00005");
        }

        System.out.println("IngestSDD.buildChain(): Build chain 4 of 10: Creating empty generator chain");

        GeneratorChain chain = new GeneratorChain();
        
        System.out.println("IngestSDD.buildChain(): Build chain 5 of 10: adding OrganizationGenerator into generation chain");

        if (organizationRecordFile != null && organizationRecordFile.isValid()) {
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
            }
        }

        System.out.println("IngestSDD.buildChain(): Build chain 6 of 6: adding PersonGenerator into generation chain");

        // persons needs to be processed after data organizations because or persons 
        // need to be assigned members of organizations
                
        if (personRecordFile != null && personRecordFile.isValid()) {
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
            }
        }

        return chain;
    }
}

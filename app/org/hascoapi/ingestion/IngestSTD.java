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
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.NameSpaces;

import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

public class IngestSTD {

    public static void exec(Study study, DataFile dataFile, File file, String templateFile) {

        System.out.println("IngestSTD.exec(): Step 1 of 5 = Processing file: " + dataFile.getFilename());

        String fileName = dataFile.getFilename();

        System.out.println("IngestSTD.exec() Step 2 of 5. [" + dataFile.getLastProcessTime() + "]");

        // file is rejected if it already exists in the folder of processed files
        //if (dataFile.getLastProcessTime() != null) {
        //    dataFile.getLogger().printExceptionByIdWithArgs("GBL_00002", fileName);
        //    return;
        //}

        System.out.println("IngestSTD.exec() Step 3 of 5: check if file has right extension");

        dataFile.getLogger().println(String.format("Processing file: %s", fileName));

        // file is rejected if it has an invalid extension
        RecordFile recordFile = null;
        if (fileName.endsWith(".csv")) {
            recordFile = new CSVRecordFile(file);
        } else {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00003", fileName);
            return;
        }
        
        System.out.println("IngestSTD.exec() Step 4 of 5");

        dataFile.setRecordFile(recordFile);

        boolean bSucceed = false;
        GeneratorChain chain = null;

        if (fileName.startsWith("STD-")) {
            System.out.println("IngestSTD.exec(): Step 5 of 5 - calling IngestSTD.buildChain()");
            chain = buildChain(study, dataFile, templateFile);           
        } 

        if (chain != null) {
            System.out.println("IngestSTD.exec(): EXCUTING");
            bSucceed = chain.generate();
        }

        if (bSucceed) {
            System.out.println("IngestSTD.exec(): SUCCESSFULLY DONE");
            dataFile.setFileStatus(DataFile.PROCESSED);
            dataFile.setCompletionTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
            dataFile.save();
            return;
        }

        System.out.println("IngestSTD.exec(): FAILED");
        return;
    }

    /****************************
     *    STD                   *
     ****************************/    
    
    public static GeneratorChain buildChain(Study study, DataFile dataFile, String templateFile) {

        try {
            System.out.println("hascoapi.templates.template_filename:" + templateFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        GeneratorChain chain = new GeneratorChain();
        System.out.println("IngestSTD.exec(): Adding StudyGenerator.");
        chain.addGenerator(new StudyGenerator(study, dataFile, templateFile));
        System.out.println("IngestSTD.exec(): Adding AgentGenerator.");
        chain.addGenerator(new AgentGenerator(study, dataFile, templateFile));
        System.out.println("IngestSTD.exec(): chain is BUILT.");
        chain.setNamedGraphUri(study.getUri());
        return chain;
    }

}

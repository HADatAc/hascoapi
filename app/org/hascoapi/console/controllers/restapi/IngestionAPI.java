package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.KGR;
import org.hascoapi.entity.pojo.SDD;
import org.hascoapi.entity.pojo.STD;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.ingestion.IngestKGR;
import org.hascoapi.ingestion.IngestSDD;
import org.hascoapi.ingestion.IngestSTD;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.VSTOI;
import com.typesafe.config.Config;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import static org.hascoapi.Constants.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.inject.Inject;

public class IngestionAPI extends Controller {

    private final Config config;
    
    @Inject
    public IngestionAPI(Config config) {
        this.config = config;
    }

    public String templateFile() {
        return config.getString("hascoapi.templates.template_filename");
    }

    public Result uploadTemplate(String elementType, String elementUri, Http.Request request) {
        System.out.println(" ");
        System.out.println(" ");
        System.out.println("== NEW " + elementType + " =========================================================== ");
        System.out.println("IngestionAPI.uploadTemplate() with elementUri = " + elementUri);

        System.out.println("templateFile :" + templateFile());

        File file = request.body().asRaw().asFile();

        if (file == null) {
            return ok(ApiUtil.createResponse("No file has been provided for ingestion.", false));
        }
        System.out.println("IngestionAPI.uploadTemplate(): API has received file content");

        if (elementType.equals("sdd")) {
            SDD sdd = SDD.find(elementUri);
            if (sdd == null) {
                return ok(ApiUtil.createResponse("File FAILED to be ingested: could not retrieve " + elementType + ". ",false));
            }
            System.out.println("IngestionAPI.uploadSDD(): API has read draft " + elementType + " from triplestore");
            DataFile dataFile = DataFile.find(sdd.getHasDataFile());
            if (dataFile != null) {
                dataFile.setLastProcessTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
                dataFile.setFileStatus(DataFile.WORKING);
                dataFile.getLogger().resetLog();
                dataFile.save();
                System.out.println("IngestionAPI.uploadTemplate(): API has read DataFile from triplestore");
            } else {
                return ok(ApiUtil.createResponse("File FAILED to be ingested: could not retrieve DataFile. ",false));
            }
            CompletableFuture.runAsync(() -> {
                IngestSDD.exec(sdd, dataFile,file, templateFile());
            });
            System.out.println("IngestionAPI.uploadTemplate(): API has just called IngestSDD.exec()");
        } else if (elementType.equals("std")) {
            System.out.println("IngestionAPI.uploadTemplate(): inside elementType=[" + elementType + "]");
            Study study = Study.find(elementUri);
            if (study == null) {
                return ok(ApiUtil.createResponse("File FAILED to be ingested: could not retrieve " + elementType + ". ",false));
            }
            System.out.println("IngestionAPI.uploadTemplate(): API has read draft " + elementType + " from triplestore");
            DataFile dataFile = DataFile.find(study.getHasDataFile());
            if (dataFile != null) {
                dataFile.setLastProcessTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
                dataFile.setFileStatus(DataFile.WORKING);
                dataFile.getLogger().resetLog();
                dataFile.save();
                System.out.println("IngestionAPI.uploadTemplate(): API has read DataFile from triplestore");
            } else {
                return ok(ApiUtil.createResponse("File FAILED to be ingested: could not retrieve DataFile. ",false));
            }
            CompletableFuture.runAsync(() -> {
                IngestSTD.exec(study, dataFile, file, templateFile());
            });
            System.out.println("IngestionAPI.uploadTemplate(): API has just called IngestSTD.exec()");
        } else if (elementType.equals("kgr")) {
            System.out.println("IngestionAPI.uploadTemplate(): inside elementType=[" + elementType + "]");
            KGR kgr = KGR.find(elementUri);
            if (kgr == null) {
                return ok(ApiUtil.createResponse("File FAILED to be ingested: could not retrieve " + elementType + ". ",false));
            }
            System.out.println("IngestionAPI.uploadTemplate(): API has read draft " + elementType + " from triplestore");
            DataFile dataFile = DataFile.find(kgr.getHasDataFile());
            if (dataFile != null) {
                dataFile.setLastProcessTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
                dataFile.setFileStatus(DataFile.WORKING);
                dataFile.getLogger().resetLog();
                dataFile.save();
                System.out.println("IngestionAPI.uploadTemplate(): API has read DataFile from triplestore");
            } else {
                return ok(ApiUtil.createResponse("File FAILED to be ingested: could not retrieve DataFile. ",false));
            }
            CompletableFuture.runAsync(() -> {
                IngestKGR.exec(kgr, dataFile, file, templateFile());
            });
            System.out.println("IngestionAPI.uploadTemplate(): API has just called IngestKGR.exec()");
        } else {
            return ok(ApiUtil.createResponse("Could not find ingestion procedure for element type " + elementType,false));
        }

        return ok(ApiUtil.createResponse("File submitted for ingestion. Check file's log for ingestion status ",true));

    }

    public Result uningest(String dataFileUri) {
        DataFile dataFile = DataFile.find(dataFileUri);
        if (dataFile != null) {
            dataFile.delete();
            return ok(ApiUtil.createResponse("DataFile <" + dataFileUri + "> has been DELETED.", true));
        }
        return ok(ApiUtil.createResponse("unable to retrieve datafile for " + dataFileUri, false));
    }

    public Result getLog(String dataFileUri){
        DataFile dataFile = DataFile.find(dataFileUri);
        if (dataFile == null) {
            return ok(ApiUtil.createResponse("unable to retrieve datafile for " + dataFileUri, false));
        }
        if (dataFile.getLog() == null) {
            return ok(ApiUtil.createResponse("unable to retrieve the log for datafile with uri " + dataFileUri, false));
        }
        return ok(ApiUtil.createResponse(dataFile.getLogger().getLog(), true));
    }

}

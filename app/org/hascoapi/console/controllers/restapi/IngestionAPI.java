package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.GenericInstance;
import org.hascoapi.entity.pojo.KGR;
import org.hascoapi.entity.pojo.SDD;
import org.hascoapi.entity.pojo.STD;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.ingestion.IngestKGR;
import org.hascoapi.ingestion.IngestSDD;
import org.hascoapi.ingestion.IngestSTD;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.HASCO;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public Result ingest(String elementType, String elementUri, Http.Request request) {
        System.out.println(" ");
        System.out.println(" ");
        System.out.println("== NEW " + elementType + " =========================================================== ");
        System.out.println("IngestionAPI.ingest() with elementUri = " + elementUri);

        System.out.println("templateFile :" + templateFile());

        // Get the uploaded file
        File file = request.body().asRaw().asFile();

        if (file == null) {
            return ok(ApiUtil.createResponse("No file has been provided for ingestion.", false));
        }

        System.out.println("IngestionAPI.ingest(): API has received file content");

        if (elementType.equals("sdd")) {
            SDD sdd = SDD.find(elementUri);
            if (sdd == null) {
                return ok(ApiUtil.createResponse("File FAILED to be ingested: could not retrieve " + elementType + ". ",false));
            }
            System.out.println("IngestionAPI.ingest(): API has read draft " + elementType + " from triplestore");
            DataFile dataFile = DataFile.find(sdd.getHasDataFile());
            if (dataFile != null) {
                dataFile.setLastProcessTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
                dataFile.setFileStatus(DataFile.WORKING);
                dataFile.getLogger().resetLog();
                dataFile.save();
                System.out.println("IngestionAPI.ingest(): API has read DataFile from triplestore");
            } else {
                return ok(ApiUtil.createResponse("File FAILED to be ingested: could not retrieve DataFile. ",false));
            }
            CompletableFuture.runAsync(() -> {
                IngestSDD.exec(sdd, dataFile,file, templateFile());
            });
            System.out.println("IngestionAPI.ingest(): API has just called IngestSDD.exec()");
        } else if (elementType.equals("std")) {
            System.out.println("IngestionAPI.ingest(): inside elementType=[" + elementType + "]");
            Study study = Study.find(elementUri);
            if (study == null) {
                return ok(ApiUtil.createResponse("File FAILED to be ingested: could not retrieve " + elementType + ". ",false));
            }
            System.out.println("IngestionAPI.ingest(): API has read draft " + elementType + " from triplestore");
            DataFile dataFile = DataFile.find(study.getHasDataFile());
            if (dataFile != null) {
                dataFile.setLastProcessTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
                dataFile.setFileStatus(DataFile.WORKING);
                dataFile.getLogger().resetLog();
                dataFile.save();
                System.out.println("IngestionAPI.ingest(): API has read DataFile from triplestore");
            } else {
                return ok(ApiUtil.createResponse("File FAILED to be ingested: could not retrieve DataFile. ",false));
            }
            CompletableFuture.runAsync(() -> {
                IngestSTD.exec(study, dataFile, file, templateFile());
            });
            System.out.println("IngestionAPI.ingest(): API has just called IngestSTD.exec()");
        } else if (elementType.equals("kgr")) {
            System.out.println("IngestionAPI.ingest(): inside elementType=[" + elementType + "]");
            KGR kgr = KGR.find(elementUri);
            if (kgr == null) {
                return ok(ApiUtil.createResponse("IngestionAPI.ingest(): File FAILED to be ingested: could not retrieve " + elementType + "from the triple store. ",false));
            }
            DataFile dataFile = DataFile.find(kgr.getHasDataFile());
            if (dataFile != null) {
                dataFile.setLastProcessTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
                dataFile.setFileStatus(DataFile.WORKING);
                dataFile.getLogger().resetLog();
                dataFile.save();
                System.out.println("IngestionAPI.ingest(): API has read DataFile from triplestore");
            } else {
                return ok(ApiUtil.createResponse("File FAILED to be ingested: could not retrieve DataFile. ",false));
            }
            File filePerm = IngestionAPI.saveFileAsPermanent(file,dataFile.getFilename());
            if (dataFile != null & filePerm != null) {
                CompletableFuture.runAsync(() -> {
                    IngestKGR.exec(kgr, dataFile, filePerm, templateFile());
                });
                System.out.println("IngestionAPI.ingest(): API has just called IngestKGR.exec()");
            } else {
                return ok(ApiUtil.createResponse("Could not prepare ingestion for element type " + elementType,false));
            }
        } else {
            return ok(ApiUtil.createResponse("Could not find ingestion procedure for element type " + elementType,false));
        }

        return ok(ApiUtil.createResponse("File submitted for ingestion. Check file's log for ingestion status ",true));

    }

    /**
     * Copies a temporary file to a permanent file named "test.csv".
     *
     * @param tempFile The temporary file to be copied.
     * @param fileName Name of permanent copy 
     * @return The permanent file if the copy is successful, null otherwise.
     */
    public static File saveFileAsPermanent(File tempFile, String fileName) {
        if (tempFile == null) {
            return null;
        }

        // Define the permanent file path
        String pathString = ConfigProp.getPathIngestion() + fileName;
        Path permanentPath = Paths.get(pathString);

        try {
            // Ensure the temporary file is deleted on exit
            tempFile.deleteOnExit();

            // Copy the file to the permanent location
            Files.copy(tempFile.toPath(), permanentPath);

            System.out.println("Temporary file has been successfully saved as " + pathString);

            // Return the permanent file
            return permanentPath.toFile();
        } catch (IOException e) {
            System.err.println("[ERROR] IngestionAPI.saveFileAsPermanent(): Error saving file " + e.getMessage());
            return null;
        }
    }

    /**
     * Deletes a permanent file.
     *
     * @param fileName The name of the file to be deleted.
     * @return true if the file was successfully deleted, false otherwise.
     */
    public static boolean deletePermanentFile(String fileName) {

        // Define the permanent file path
        String pathString = ConfigProp.getPathIngestion() + fileName;
        File permanentFile = new File(pathString);

        // Check if the file exists
        if (permanentFile.exists()) {
            // Attempt to delete the file
            boolean isDeleted = permanentFile.delete();
            if (isDeleted) {
                System.out.println("File " + fileName + " was successfully deleted.");
                return true;
            } else {
                System.err.println("[ERROR] IngestionAPI.deletePermanentFile(): Failed to delete file " + fileName);
                return false;
            }
        } else {
            System.err.println("[ERROR] IngestionAPI.deletePermanentFile(): File " + fileName + " does not exist.");
            return false;
        }
    }
    
    public Result uningestDataFile(String dataFileUri) {
        DataFile dataFile = DataFile.find(dataFileUri);
        if (dataFile != null) {
            dataFile.delete();
            return ok(ApiUtil.createResponse("DataFile <" + dataFileUri + "> has been DELETED.", true));
        }
        return ok(ApiUtil.createResponse("unable to retrieve datafile for " + dataFileUri, false));
    }

    public Result uningestMetadataTemplate(String metadataTemplateUri) {
        System.out.println(" ");
        System.out.println(" ");
        System.out.println("IngestionAPI.uningestMetadataTemplate() with metadataTemplateUri = " + metadataTemplateUri);

        if (metadataTemplateUri == null || metadataTemplateUri.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate(): No metadataTemplateUri has been provided. ";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }

        String mtType = null;
        GenericInstance mtRaw = GenericInstance.find(metadataTemplateUri);
        if (mtRaw == null) {
            String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve document with metadataTemplateUri = " + metadataTemplateUri;
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        } else {
            if (mtRaw.getHascoTypeUri().equals(HASCO.KNOWLEDGE_GRAPH)) {
                mtType = HASCO.KNOWLEDGE_GRAPH;
                System.out.println("IngestionAPI.uningestMetadataTemplate() read KGR");
            } else if (mtRaw.getHascoTypeUri().equals(HASCO.SDD)) {
                mtType = HASCO.SDD;
                System.out.println("IngestionAPI.uningestMetadataTemplate() read SDD");
            } else if (mtRaw.getHascoTypeUri().equals(HASCO.STD)) {
                mtType = HASCO.STD;
                System.out.println("IngestionAPI.uningestMetadataTemplate() read SDD");
            }
        }

        if (mtType == null) {
            String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate(): metadataTemplateUri " + metadataTemplateUri + 
                " returned no valid metadata template type. ";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }

        if (mtType.equals(HASCO.KNOWLEDGE_GRAPH)) {
            KGR kgr = KGR.find(metadataTemplateUri);
            if (kgr == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve KGR with metadataTemplateUri = " + metadataTemplateUri;
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }
            DataFile dataFile = DataFile.find(kgr.getHasDataFile());
            if (dataFile == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve KGR's dataFile = " + kgr.getHasDataFile();
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }

            System.out.println("IngestionAPI.ingest(): API has able to retrieve KGR from triplestore");

            // Delete API copy of metadata template
            boolean deletedFile = IngestionAPI.deletePermanentFile(dataFile.getFilename());

            // Uningest Datafile content
            dataFile.delete();

            // update datafile's processing record
            //if (dataFile != null) {
            //    dataFile.setLastProcessTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
            //    dataFile.setFileStatus(DataFile.WORKING);
            //    dataFile.getLogger().resetLog();
            //    dataFile.save();
            //    System.out.println("IngestionAPI.ingest(): API has read DataFile from triplestore");
            //} else {
            //    return ok(ApiUtil.createResponse("File FAILED to be ingested: could not retrieve DataFile. ",false));
            //}

            String msg = "IngestionAPI.uningestMetadataTemplate(): successfully ingested metadataTemplateUri " + metadataTemplateUri;
            System.out.println(msg);
            return ok(ApiUtil.createResponse(msg,true));
        }
        String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate(): metadataTemplateUri " + metadataTemplateUri + 
            " returned no valid metadata template.";
        System.out.println(errorMsg);
        return ok(ApiUtil.createResponse(errorMsg,false));
    
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

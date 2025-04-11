package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.ingestion.IngestionWorker;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.GenericInstance;
import org.hascoapi.entity.pojo.DP2;
import org.hascoapi.entity.pojo.DSG;
import org.hascoapi.entity.pojo.INS;
import org.hascoapi.entity.pojo.KGR;
import org.hascoapi.entity.pojo.SDD;
import org.hascoapi.entity.pojo.STR;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.Project;
import org.hascoapi.transform.mt.ins.INSGen;
import org.hascoapi.transform.mt.kgr.KGRGen;
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
import java.util.logging.Level;
import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
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

    public Result ingest(String status, String elementType, String elementUri, Http.Request request) {
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

        if (!elementType.equals("dp2") && 
            !elementType.equals("dsg") &&
            !elementType.equals("ins") &&
            !elementType.equals("kgr") &&
            !elementType.equals("sdd") && 
            !elementType.equals("str")) {

            return ok(ApiUtil.createResponse("Could not find ingestion procedure for element type " + elementType,false));
        }

        System.out.println("IngestionAPI.ingest(): inside elementType=[" + elementType + "]");
        DataFile dataFile = null;
        if (elementType.equals("dp2")) {
            DP2 dp2 = DP2.find(elementUri);
            if (dp2 == null) {
                return ok(ApiUtil.createResponse("IngestionAPI.ingest(): File FAILED to be ingested: could not retrieve " + elementType + ". ",false));
            }
            dataFile = DataFile.find(dp2.getHasDataFileUri());
        } else if (elementType.equals("dsg")) {
            DSG dsg = DSG.find(elementUri);
            if (dsg == null) {
                return ok(ApiUtil.createResponse("IngestionAPI.ingest(): File FAILED to be ingested: could not retrieve " + elementType + ". ",false));
            }
            dataFile = DataFile.find(dsg.getHasDataFileUri());
        } else if (elementType.equals("ins")) {
            INS ins = INS.find(elementUri);
            if (ins == null) {
                return ok(ApiUtil.createResponse("IngestionAPI.ingest(): File FAILED to be ingested: could not retrieve " + elementType + ". ",false));
            }
            dataFile = DataFile.find(ins.getHasDataFileUri());
        } else if (elementType.equals("kgr")) {
            KGR kgr = KGR.find(elementUri);
            if (kgr == null) {
                return ok(ApiUtil.createResponse("IngestionAPI.ingest(): File FAILED to be ingested: could not retrieve " + elementType + ". ",false));
            }
            dataFile = DataFile.find(kgr.getHasDataFileUri());
        } else if (elementType.equals("sdd")) {
            SDD sdd = SDD.find(elementUri);
            if (sdd == null) {
                return ok(ApiUtil.createResponse("IngestionAPI.ingest(): File FAILED to be ingested: could not retrieve " + elementType + ". ",false));
            }
            dataFile = DataFile.find(sdd.getHasDataFileUri());
        } else if (elementType.equals("str")) {
            STR str = STR.find(elementUri);
            if (str == null) {
                return ok(ApiUtil.createResponse("IngestionAPI.ingest(): File FAILED to be ingested: could not retrieve " + elementType + ". ",false));
            }
            dataFile = DataFile.find(str.getHasDataFileUri());
        }
        if (dataFile != null) {
            dataFile.setLastProcessTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
            if (elementType.equals("dsg")) {
                dataFile.setFileStatus(DataFile.WORKING_STD);
            } else {
                dataFile.setFileStatus(DataFile.WORKING);
            }
            dataFile.getLogger().resetLog();
            dataFile.save();
            System.out.println("IngestionAPI.ingest(): API has read DataFile from triplestore");
        } 
        File filePerm = this.saveFileAsPermanent(file,dataFile.getFilename());
        if (dataFile != null & filePerm != null) {
            final DataFile finalDataFile = dataFile; 
            CompletableFuture.runAsync(() -> {
                IngestionWorker.ingest(finalDataFile, filePerm, templateFile(), status);
            });
            System.out.println("IngestionAPI.ingest(): API has just called IngestionWorker.ingest()");
        } else {
            return ok(ApiUtil.createResponse("Could not prepare ingestion for element type " + elementType,false));
        }

        System.out.println("IngestionAPI.ingest(): API has just called IngestionWorker.ingest()");
        return ok(ApiUtil.createResponse("File submitted for ingestion. Check file's log for ingestion status ",true));

    }

    /**
     * Copies a temporary file to a permanent file
     *
     * @param tempFile The temporary file to be copied.
     * @param fileName Name of the permanent copy.
     * @return The permanent file if the copy is successful, null otherwise.
     */
    public File saveFileAsPermanent(File tempFile, String fileName) {
        if (tempFile == null || fileName == null || fileName.trim().isEmpty()) {
            System.out.println("[ERROR] Invalid input: tempFile or fileName is null/empty.");
            return null;
        }

        String destinationDir = config.getString("hascoapi.paths.ingestion");
        if (destinationDir == null || destinationDir.trim().isEmpty()) {
            System.out.println("[ERROR] ConfigProp.getPathIngestion() returned an invalid path.");
            return null;
        }

        Path permanentPath = Paths.get(destinationDir, fileName);

        try {
            // Ensure the destination directory exists
            Files.createDirectories(permanentPath.getParent());

            // Define file copy options
            CopyOption[] options = {
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            };

            // Copy the file
            Files.copy(tempFile.toPath(), permanentPath, options);
            //System.out.println("File successfully saved to: " + permanentPath);

            // Optionally delete temp file manually
            if (!tempFile.delete()) {
                System.out.println("[ERROR] Failed to delete temporary file: " + tempFile.getAbsolutePath());
            }

            return permanentPath.toFile();
        } catch (IOException e) {
            System.out.println("[ERROR] While saving file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Deletes a permanent file.
     *
     * @param fileName The name of the file to be deleted.
     * @return true if the file was successfully deleted, false otherwise.
     */
    public boolean deletePermanentFile(String fileName) {

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
       // System.out.println("IngestionAPI.uningestMetadataTemplate() with metadataTemplateUri = " + metadataTemplateUri);

        if (metadataTemplateUri == null || metadataTemplateUri.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate(): No metadataTemplateUri has been provided. ";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }

        String mtType = null;
        GenericInstance mtRaw = GenericInstance.find(metadataTemplateUri);
        //System.out.println("metadataTemplate URI: [" + metadataTemplateUri + "]");
        //System.out.println("metadataTemplate hasco type: [" + mtRaw.getHascoTypeUri() + "]");
        if (mtRaw == null) {
            String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve document with metadataTemplateUri = " + metadataTemplateUri;
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        } else {
            if (mtRaw.getHascoTypeUri().equals(HASCO.KGR)) {
                mtType = HASCO.KGR;
                System.out.println("IngestionAPI.uningestMetadataTemplate() read KGR");
            } else if (mtRaw.getHascoTypeUri().equals(HASCO.SDD)) {
                mtType = HASCO.SDD;
                System.out.println("IngestionAPI.uningestMetadataTemplate() read SDD");
            } else if (mtRaw.getHascoTypeUri().equals(HASCO.DP2)) {
                mtType = HASCO.DP2;
                System.out.println("IngestionAPI.uningestMetadataTemplate() read DP2");
            } else if (mtRaw.getHascoTypeUri().equals(HASCO.DSG)) {
                mtType = HASCO.DSG;
                System.out.println("IngestionAPI.uningestMetadataTemplate() read DSG");
            } else if (mtRaw.getHascoTypeUri().equals(HASCO.INS)) {
                mtType = HASCO.INS;
                System.out.println("IngestionAPI.uningestMetadataTemplate() read INS");
            } else if (mtRaw.getHascoTypeUri().equals(HASCO.STR)) {
                mtType = HASCO.STR;
                System.out.println("IngestionAPI.uningestMetadataTemplate() read STR");
            }
        }

        if (mtType == null) {
            String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate(): metadataTemplateUri " + metadataTemplateUri + 
                " returned no valid metadata template type. ";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }

        if (mtType.equals(HASCO.KGR)) {
            KGR kgr = KGR.find(metadataTemplateUri);
            if (kgr == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve KGR with metadataTemplateUri = " + metadataTemplateUri;
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }
            DataFile dataFile = DataFile.find(kgr.getHasDataFileUri());
            if (dataFile == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve KGR's dataFile = " + kgr.getHasDataFile();
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }

            System.out.println("IngestionAPI.ingest(): API has able to retrieve KGR from triplestore");

            // Delete API copy of metadata template
            boolean deletedFile = this.deletePermanentFile(dataFile.getFilename());

            // Uningest Datafile content
            dataFile.delete();

            String msg = "IngestionAPI.uningestMetadataTemplate(): successfully ingested metadataTemplateUri " + metadataTemplateUri;
            System.out.println(msg);
            return ok(ApiUtil.createResponse(msg,true));

        } else if (mtType.equals(HASCO.DSG)) {

            DSG dsg = DSG.find(metadataTemplateUri);
            if (dsg == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve DSG with metadataTemplateUri = " + metadataTemplateUri;
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }
            DataFile dataFile = DataFile.find(dsg.getHasDataFileUri());
            if (dataFile == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve DSG's dataFile = " + dsg.getHasDataFileUri();
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }

            System.out.println("IngestionAPI.ingest(): API has able to retrieve DSG from triplestore");

            // Delete API copy of metadata template
            boolean deletedFile = this.deletePermanentFile(dataFile.getFilename());

            // Uningest Datafile content
            dataFile.delete();

            String msg = "IngestionAPI.uningestMetadataTemplate(): successfully ingested metadataTemplateUri " + metadataTemplateUri;
            System.out.println(msg);
            return ok(ApiUtil.createResponse(msg,true));

        } else if (mtType.equals(HASCO.DP2)) {

            DP2 dp2 = DP2.find(metadataTemplateUri);
            if (dp2 == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve DP2 with metadataTemplateUri = " + metadataTemplateUri;
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }
            DataFile dataFile = DataFile.find(dp2.getHasDataFileUri());
            if (dataFile == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve DP2's dataFile = " + dp2.getHasDataFileUri();
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }

            System.out.println("IngestionAPI.ingest(): API has able to retrieve DP2 from triplestore");

            // Delete API copy of metadata template
            boolean deletedFile = this.deletePermanentFile(dataFile.getFilename());

            // Uningest Datafile content
            dataFile.delete();

            String msg = "IngestionAPI.uningestMetadataTemplate(): successfully ingested metadataTemplateUri " + metadataTemplateUri;
            System.out.println(msg);
            return ok(ApiUtil.createResponse(msg,true));

        } else if (mtType.equals(HASCO.INS)) {

            INS ins = INS.find(metadataTemplateUri);
            if (ins == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve INS with metadataTemplateUri = " + metadataTemplateUri;
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }
            DataFile dataFile = DataFile.find(ins.getHasDataFileUri());
            if (dataFile == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve INS's dataFile = " + ins.getHasDataFileUri();
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }

            System.out.println("IngestionAPI.ingest(): API has able to retrieve DSG from triplestore");

            // Delete API copy of metadata template
            boolean deletedFile = this.deletePermanentFile(dataFile.getFilename());

            // Uningest Datafile content
            dataFile.delete();

            String msg = "IngestionAPI.uningestMetadataTemplate(): successfully ingested metadataTemplateUri " + metadataTemplateUri;
            System.out.println(msg);
            return ok(ApiUtil.createResponse(msg,true));

        } else if (mtType.equals(HASCO.SDD)) {

            SDD sdd = SDD.find(metadataTemplateUri);
            if (sdd == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve SDD with metadataTemplateUri = " + metadataTemplateUri;
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }
            DataFile dataFile = DataFile.find(sdd.getHasDataFileUri());
            if (dataFile == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve SDD's dataFile = " + sdd.getHasDataFileUri();
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }

            System.out.println("IngestionAPI.ingest(): API has able to retrieve SDD from triplestore");

            // Delete API copy of metadata template
            boolean deletedFile = this.deletePermanentFile(dataFile.getFilename());

            // Uningest Datafile content
            dataFile.delete();

            String msg = "IngestionAPI.uningestMetadataTemplate(): successfully ingested metadataTemplateUri " + metadataTemplateUri;
            System.out.println(msg);
            return ok(ApiUtil.createResponse(msg,true));

        } else if (mtType.equals(HASCO.STR)) {

            STR str = STR.find(metadataTemplateUri);
            if (str == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve STR with metadataTemplateUri = " + metadataTemplateUri;
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }
            DataFile dataFile = DataFile.find(str.getHasDataFileUri());
            if (dataFile == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve STR's dataFile = " + str.getHasDataFileUri();
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }

            System.out.println("IngestionAPI.ingest(): API has able to retrieve STR from triplestore");

            // Delete API copy of metadata template
            boolean deletedFile = this.deletePermanentFile(dataFile.getFilename());

            // Uningest Datafile content
            dataFile.delete();

            String msg = "IngestionAPI.uningestMetadataTemplate(): successfully ingested metadataTemplateUri " + metadataTemplateUri;
            System.out.println(msg);
            return ok(ApiUtil.createResponse(msg,true));

        }

        String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate(): metadataTemplateUri " + metadataTemplateUri + 
            " returned template that cannot be uningested.";
        System.out.println(errorMsg);
        return ok(ApiUtil.createResponse(errorMsg,false));
    
    }

    public Result mtGenByStatus(String elementtype, String status, String filename) {
        if (elementtype == null || elementtype.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByStatus() requires elementtype";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (status == null || status.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByStatus() requires status";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (filename == null || filename.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByStatus() requires filename";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        switch (elementtype) {
            case "ins":
                INSGen.genByStatus(status,filename);
                break;
            case "kgr":
                KGRGen.genByStatus(status,filename);
                break;
            default:
                String errorMsg = "[ERROR] IngestionAPI.mtGenByStatus() invalid elementtype=[" + elementtype + "]";
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
        }
        return ok(ApiUtil.createResponse("", true));
    }

    public Result mtGenByInstrument(String elementtype, String instrumenturi, String filename) {
        if (elementtype == null || elementtype.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByInstrument() requires elementtype";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (instrumenturi == null || instrumenturi.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByInstrument() requires instrumenturi";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        Instrument instrument = Instrument.find(instrumenturi);
        if (instrument == null) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByInstrument() cannot retrieve instrument with uri=[" + instrumenturi + "]";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (filename == null || filename.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByInstrument() requires filename";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        String resp = "";
        switch (elementtype) {
            case "ins":
                resp = INSGen.genByInstrument(instrument,filename);
                break;
            default:
                String errorMsg = "[ERROR] IngestionAPI.mtGenByInstrument() invalid elementtype=[" + elementtype + "]";
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (resp.equals("")) {
            return ok(ApiUtil.createResponse(resp, true));
        } else {
            return ok(ApiUtil.createResponse(resp, false));
        }
    }

    public Result mtGenByProject(String elementtype, String projecturi, String filename) {
       System.out.println("IngestionAPI.mtGenByProject() with projectUri = " + projecturi);
       if (elementtype == null || elementtype.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByProject() requires elementtype";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (projecturi == null || projecturi.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByProject() requires projecturi";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        Project project = Project.find(projecturi);
        if (project == null) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByproject() cannot retrieve project with uri=[" + projecturi + "]";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (filename == null || filename.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByProject() requires filename";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        String resp = "";
        switch (elementtype) {
            case "kgr":
                resp = KGRGen.genByProject(project,filename);
                break;
            default:
                String errorMsg = "[ERROR] IngestionAPI.mtGenByProject() invalid elementtype=[" + elementtype + "]";
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (resp.equals("")) {
            return ok(ApiUtil.createResponse(resp, true));
        } else {
            return ok(ApiUtil.createResponse(resp, false));
        }
    }

    public Result mtGenByManager(String elementtype, String useremail, String status, String filename) {
        if (elementtype == null || elementtype.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByStatus() requires elementtype";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (useremail == null || useremail.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByStatus() requires useremail";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (status == null || status.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByStatus() requires status";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (filename == null || filename.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByStatus() requires filename";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        switch (elementtype) {
            case "ins":
                INSGen.genByManager(useremail, status, filename);
                break;
            case "kgr":
                KGRGen.genByManager(useremail, status, filename);
                break;
            default:
                String errorMsg = "[ERROR] IngestionAPI.mtGenByStatus() invalid elementtype=[" + elementtype + "]";
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
        }
        return ok(ApiUtil.createResponse("", true));
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

package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.apache.jena.update.UpdateFactory;
import org.hascoapi.entity.pojo.WKF;
import org.hascoapi.entity.pojo.NameSpace;
import org.hascoapi.transform.mt.wkf.WKFGen;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.Constants;
import org.hascoapi.ingestion.IngestionWorker;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.GenericInstance;
import org.hascoapi.entity.pojo.HADatAcThing;
import org.hascoapi.entity.pojo.DP2;
import org.hascoapi.entity.pojo.DSG;
import org.hascoapi.entity.pojo.INS;
import org.hascoapi.entity.pojo.KGR;
import org.hascoapi.entity.pojo.SDD;
import org.hascoapi.entity.pojo.STR;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.FundingScheme;
import org.hascoapi.entity.pojo.Place;
import org.hascoapi.entity.pojo.Project;
import org.hascoapi.entity.pojo.Organization;
import org.hascoapi.transform.mt.dp2.DP2Gen;
import org.hascoapi.transform.mt.dsg.DSGGen;
import org.hascoapi.transform.mt.ins.INSGen;
import org.hascoapi.transform.mt.kgr.KGRGen;
import org.hascoapi.utils.*;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;
import com.typesafe.config.Config;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import static org.hascoapi.Constants.*;

import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

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
        System.out.println("Request content-type: " + request.contentType().orElse("not specified"));
        System.out.println("Request has body: " + request.hasBody());
        System.out.println("templateFile :" + templateFile());

        if (!elementType.equals("dp2") &&
            !elementType.equals("dsg") &&
            !elementType.equals("ins") &&
            !elementType.equals("kgr") &&
            !elementType.equals("sdd") &&
            !elementType.equals("str") &&
            !elementType.equals("wkf")) {

            return ok(ApiUtil.createResponse("Could not find ingestion procedure for element type " + elementType,false));
        }

        System.out.println("IngestionAPI.ingest(): inside elementType=[" + elementType + "]");
        System.out.println("IngestionAPI.ingest(): Retrieving MT instance and DataFile for element type: " + elementType);

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
        } else if (elementType.equals("wkf")) {
            WKF wkf = WKF.find(elementUri);
            if (wkf == null) {
                return ok(ApiUtil.createResponse("IngestionAPI.ingest(): File FAILED to be ingested: could not retrieve " + elementType + ". ",false));
            }
            dataFile = DataFile.find(wkf.getHasDataFileUri());
        }

        if (dataFile == null) {
            return ok(ApiUtil.createResponse("IngestionAPI.ingest(): File FAILED to be ingested: could not retrieve DataFile.",false));
        }

        System.out.println("IngestionAPI.ingest(): DataFile retrieved - URI: " + dataFile.getUri() + ", Filename: " + dataFile.getFilename());

        File fileToIngest = null;

        // FIRST: Check if file already exists in filesystem (pre-uploaded in resources/{DFL...}/)
        String basePath = config.getString("hascoapi.paths.ingestion");
        if (basePath != null && dataFile.getUri() != null && dataFile.getFilename() != null) {
            String uriTerm = URIUtils.uriLastSegment(dataFile.getUri());
            Path preUploadedPath = Paths.get(basePath, Constants.RESOURCE_FOLDER, uriTerm, dataFile.getFilename());
            File preUploadedFile = preUploadedPath.toFile();

            System.out.println("IngestionAPI.ingest(): Checking for pre-uploaded file at: " + preUploadedPath.toAbsolutePath());
            if (preUploadedFile.exists() && preUploadedFile.length() > 0) {
                System.out.println("IngestionAPI.ingest(): Found pre-uploaded file!");
                fileToIngest = preUploadedFile;
            } else {
                System.out.println("IngestionAPI.ingest(): Pre-uploaded file NOT found or empty (exists: " + preUploadedFile.exists() + ", size: " + (preUploadedFile.exists() ? preUploadedFile.length() : "N/A") + ")");
            }
        }

        // SECOND: If not pre-uploaded, try to get file from request body
        if (fileToIngest == null) {
            File fileFromRequest = null;

            // Try asRaw() first (legacy workflow)
            if (request.body() != null && request.body().asRaw() != null) {
                fileFromRequest = request.body().asRaw().asFile();
            }

            // Try asMultipartFormData() if asRaw() failed
            if (fileFromRequest == null && request.body() != null && request.body().asMultipartFormData() != null) {
                play.mvc.Http.MultipartFormData multipart = request.body().asMultipartFormData();
                play.mvc.Http.MultipartFormData.FilePart<Object> filePart = multipart.getFile("file");

                if (filePart != null) {
                    Object fileObj = filePart.getRef();
                    if (fileObj instanceof File) {
                        fileFromRequest = (File) fileObj;
                    } else if (fileObj instanceof play.api.libs.Files.TemporaryFile) {
                        play.api.libs.Files.TemporaryFile tempFile = (play.api.libs.Files.TemporaryFile) fileObj;
                        fileFromRequest = tempFile.path().toFile();
                    }
                }
            }

            // Try asBytes() if both asRaw() and multipart failed
            if (fileFromRequest == null && request.body() != null && request.body().asBytes() != null) {
                akka.util.ByteString bytes = request.body().asBytes();

                if (bytes != null && bytes.size() > 0) {
                    try {
                        // Create temp file from bytes
                        Path tempPath = Files.createTempFile("upload-", "-" + dataFile.getFilename());
                        Files.write(tempPath, bytes.toArray());
                        fileFromRequest = tempPath.toFile();
                    } catch (IOException e) {
                        System.err.println("[ERROR] IngestionAPI.ingest(): Failed to create temp file from bytes: " + e.getMessage());
                    }
                }
            }

            // Use fileFromRequest if we successfully extracted it from body
            if (fileFromRequest != null && fileFromRequest.exists() && fileFromRequest.length() > 0) {
                fileToIngest = fileFromRequest;
            }
        } // End of: if (fileToIngest == null)

        // THIRD: If still no file, try the old fallback logic (wait for uploaded file)
        if (fileToIngest == null) {
            System.out.println("IngestionAPI.ingest(): No file found, will wait for uploaded file in filesystem...");

            if (basePath == null || basePath.trim().isEmpty()) {
                System.out.println("[ERROR] IngestionAPI.ingest(): Invalid file storage path from config.");
                return internalServerError(ApiUtil.createResponse("[ERROR] IngestionAPI.ingest(): Invalid file storage path.", false));
            }

            // Validate DataFile properties
            if (dataFile.getUri() == null || dataFile.getUri().trim().isEmpty()) {
                System.out.println("[ERROR] IngestionAPI.ingest(): DataFile URI is null or empty");
                return ok(ApiUtil.createResponse("DataFile URI is invalid. Cannot locate uploaded file.", false));
            }

            if (dataFile.getFilename() == null || dataFile.getFilename().trim().isEmpty()) {
                System.out.println("[ERROR] IngestionAPI.ingest(): DataFile filename is null or empty");
                return ok(ApiUtil.createResponse("DataFile filename is invalid. Cannot locate uploaded file.", false));
            }

            // Extract the URI last segment (DFL{id}) to build the correct path
            String uriTerm = URIUtils.uriLastSegment(dataFile.getUri());
            Path uploadedFilePath = Paths.get(basePath, Constants.RESOURCE_FOLDER, uriTerm, dataFile.getFilename());
            File uploadedFile = uploadedFilePath.toFile();

            System.out.println("IngestionAPI.ingest(): Looking for file at path: " + uploadedFilePath.toAbsolutePath());

            // Wait for file to be available (uploadFile is async)
            // Increased to 20 attempts with 1 second delay = 20 seconds total wait time
            int maxRetries = 20;
            int retryDelay = 1000; // milliseconds
            for (int i = 0; i < maxRetries && (!uploadedFile.exists() || uploadedFile.length() == 0); i++) {
                try {
                    if (i == 0 || i % 5 == 0) { // Log every 5 attempts to reduce noise
                        System.out.println("IngestionAPI.ingest(): Waiting for file... (attempt " + (i + 1) + "/" + maxRetries + ")");
                    }
                    Thread.sleep(retryDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (!uploadedFile.exists() || uploadedFile.length() == 0) {
                System.out.println("[ERROR] IngestionAPI.ingest(): File not found or empty after " + maxRetries + " attempts at: " + uploadedFilePath);
                return ok(ApiUtil.createResponse("File not found or upload not completed. Please ensure the file was uploaded before triggering ingestion.", false));
            }

            System.out.println("IngestionAPI.ingest(): Found uploaded file at: " + uploadedFilePath + " (size: " + uploadedFile.length() + " bytes)");
            fileToIngest = uploadedFile;
        }

        dataFile.setLastProcessTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
        dataFile.setFileStatus(DataFile.WORKING);
        dataFile.getLogger().resetLog();
        dataFile.save();
        System.out.println("IngestionAPI.ingest(): API has read DataFile from triplestore");

        // Copy file to correct ingestion directory (resources/{DFL...}/) for processing
        File filePerm = this.saveFileAsPermanent(fileToIngest, dataFile);
        if (filePerm != null) {
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
     * Copies a temporary file to a permanent file in the resources/{DFL...}/ directory
     * This ensures the file is in the same location where ingest() looks for it
     *
     * @param tempFile The temporary file to be copied.
     * @param dataFile The DataFile object containing URI and filename information.
     * @return The permanent file if the copy is successful, null otherwise.
     */
    public File saveFileAsPermanent(File tempFile, DataFile dataFile) {
        if (tempFile == null || dataFile == null) {
            System.out.println("[ERROR] saveFileAsPermanent(): tempFile or dataFile is null.");
            return null;
        }

        if (dataFile.getUri() == null || dataFile.getUri().trim().isEmpty()) {
            System.out.println("[ERROR] saveFileAsPermanent(): DataFile URI is null or empty.");
            return null;
        }

        if (dataFile.getFilename() == null || dataFile.getFilename().trim().isEmpty()) {
            System.out.println("[ERROR] saveFileAsPermanent(): DataFile filename is null or empty.");
            return null;
        }

        String basePath = config.getString("hascoapi.paths.ingestion");
        if (basePath == null || basePath.trim().isEmpty()) {
            System.out.println("[ERROR] saveFileAsPermanent(): Invalid base path from config.");
            return null;
        }

        // Extract URI last segment (e.g., DFL1770641907769921)
        String uriTerm = URIUtils.uriLastSegment(dataFile.getUri());

        // Build target directory: basePath/resources/{DFL...}/
        Path targetDir = Paths.get(basePath, Constants.RESOURCE_FOLDER, uriTerm);

        // Build target file path: basePath/resources/{DFL...}/filename.xlsx
        Path targetFile = targetDir.resolve(dataFile.getFilename());

        try {
            // Create directory structure if it doesn't exist
            Files.createDirectories(targetDir);
            System.out.println("[INFO] saveFileAsPermanent(): Target directory created/verified: " + targetDir);

            // If the file already exists in the correct location, don't overwrite it
            if (targetFile.toFile().exists() && targetFile.toFile().length() > 0) {
                System.out.println("[INFO] saveFileAsPermanent(): File already exists at target location: " + targetFile);
                return targetFile.toFile();
            }

            // Copy the file to the permanent location
            Files.copy(
                tempFile.toPath(),
                targetFile,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            );

            System.out.println("[SUCCESS] saveFileAsPermanent(): File saved to: " + targetFile);

            // Only delete temp file if it's NOT already in the resources folder
            // (to avoid deleting the file we just saved)
            if (!tempFile.getAbsolutePath().contains(Constants.RESOURCE_FOLDER)) {
                if (!tempFile.delete()) {
                    System.out.println("[WARN] saveFileAsPermanent(): Failed to delete temp file: " + tempFile.getAbsolutePath());
                } else {
                    System.out.println("[INFO] saveFileAsPermanent(): Temp file deleted: " + tempFile.getAbsolutePath());
                }
            } else {
                System.out.println("[INFO] saveFileAsPermanent(): Temp file is in resources folder, not deleting.");
            }

            return targetFile.toFile();

        } catch (IOException e) {
            System.out.println("[ERROR] saveFileAsPermanent(): Failed to save file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deletes a permanent file in the resources/{DFL...}/ directory.
     *
     * @param dataFile The DataFile object containing URI and filename information.
     * @return true if the file was successfully deleted, false otherwise.
     */
    public boolean deletePermanentFile(DataFile dataFile) {
        if (dataFile == null) {
            System.err.println("[ERROR] IngestionAPI.deletePermanentFile(): DataFile is null.");
            return false;
        }

        if (dataFile.getUri() == null || dataFile.getUri().trim().isEmpty()) {
            System.err.println("[ERROR] IngestionAPI.deletePermanentFile(): DataFile URI is null or empty.");
            return false;
        }

        if (dataFile.getFilename() == null || dataFile.getFilename().trim().isEmpty()) {
            System.err.println("[ERROR] IngestionAPI.deletePermanentFile(): DataFile filename is null or empty.");
            return false;
        }

        String basePath = config.getString("hascoapi.paths.ingestion");
        if (basePath == null || basePath.trim().isEmpty()) {
            System.err.println("[ERROR] IngestionAPI.deletePermanentFile(): Invalid base path from config.");
            return false;
        }

        // Extract URI last segment (e.g., DFL1770641907769921)
        String uriTerm = URIUtils.uriLastSegment(dataFile.getUri());

        // Build file path: basePath/resources/{DFL...}/filename.xlsx
        Path filePath = Paths.get(basePath, Constants.RESOURCE_FOLDER, uriTerm, dataFile.getFilename());
        File permanentFile = filePath.toFile();

        // Check if the file exists
        if (permanentFile.exists()) {
            // Attempt to delete the file
            boolean isDeleted = permanentFile.delete();
            if (isDeleted) {
                System.out.println("[INFO] IngestionAPI.deletePermanentFile(): File successfully deleted: " + filePath);

                // Try to delete the parent directory if it's empty
                File parentDir = permanentFile.getParentFile();
                if (parentDir != null && parentDir.isDirectory()) {
                    String[] contents = parentDir.list();
                    if (contents != null && contents.length == 0) {
                        if (parentDir.delete()) {
                            System.out.println("[INFO] IngestionAPI.deletePermanentFile(): Empty directory deleted: " + parentDir);
                        }
                    }
                }

                return true;
            } else {
                System.err.println("[ERROR] IngestionAPI.deletePermanentFile(): Failed to delete file: " + filePath);
                return false;
            }
        } else {
            System.err.println("[WARN] IngestionAPI.deletePermanentFile(): File does not exist: " + filePath);
            return false;
        }
    }

    /**
     * Deletes a permanent file (legacy method - kept for backward compatibility).
     * WARNING: This method uses the old path structure and may not work correctly.
     * Use deletePermanentFile(DataFile dataFile) instead.
     *
     * @param fileName The name of the file to be deleted.
     * @return true if the file was successfully deleted, false otherwise.
     */
    @Deprecated
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
            } else if (mtRaw.getHascoTypeUri().equals(HASCO.WKF)) {
                mtType = HASCO.WKF;
                System.out.println("IngestionAPI.uningestMetadataTemplate() read WKF");
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

            // Delete the named graph (ingested data) but keep the DataFile metadata
            dataFile.delete();

            // Reset DataFile to UNPROCESSED status (keep the file and metadata)
            dataFile.resetForUnprocessed();
            dataFile.save();

            String msg = "IngestionAPI.uningestMetadataTemplate(): successfully uningest metadataTemplateUri " + metadataTemplateUri;
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

            // Delete the named graph (ingested data) but keep the DataFile metadata
            dataFile.delete();

            // Reset DataFile to UNPROCESSED status (keep the file and metadata)
            dataFile.resetForUnprocessed();
            dataFile.save();

            String msg = "IngestionAPI.uningestMetadataTemplate(): successfully uningest metadataTemplateUri " + metadataTemplateUri;
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

            // IMPORTANT: DP2 MT is stored outside the DataFile named graph (typically in the repository default graph).
            // Deleting only the DataFile graph leaves the DP2 MT behind, so generation keeps seeing stale DP2s.
            try {
                dp2.delete();
            } catch (Exception e) {
                System.out.println("[WARNING] IngestionAPI.uningestMetadataTemplate(): failed to delete DP2 MT resource (best-effort): " + e.getMessage());
            }

            // Delete the named graph (ingested data) but keep the DataFile metadata
            dataFile.delete();

            // Reset DataFile to UNPROCESSED status (keep the file and metadata)
            dataFile.resetForUnprocessed();
            dataFile.save();

            String msg = "IngestionAPI.uningestMetadataTemplate(): successfully uningest metadataTemplateUri " + metadataTemplateUri;
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

            System.out.println("IngestionAPI.ingest(): API has able to retrieve INS from triplestore");

            // Delete the named graph (ingested data) but keep the DataFile metadata
            dataFile.delete();

            // Reset DataFile to UNPROCESSED status (keep the file and metadata)
            dataFile.resetForUnprocessed();
            dataFile.save();

            String msg = "IngestionAPI.uningestMetadataTemplate(): successfully uningest metadataTemplateUri " + metadataTemplateUri;
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

            // Delete the named graph (ingested data) but keep the DataFile metadata
            dataFile.delete();

            // Reset DataFile to UNPROCESSED status (keep the file and metadata)
            dataFile.resetForUnprocessed();
            dataFile.save();

            String msg = "IngestionAPI.uningestMetadataTemplate(): successfully uningest metadataTemplateUri " + metadataTemplateUri;
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

            // Delete the named graph (ingested data) but keep the DataFile metadata
            dataFile.delete();

            // Reset DataFile to UNPROCESSED status (keep the file and metadata)
            dataFile.resetForUnprocessed();
            dataFile.save();

            String msg = "IngestionAPI.uningestMetadataTemplate(): successfully uningest metadataTemplateUri " + metadataTemplateUri;
            System.out.println(msg);
            return ok(ApiUtil.createResponse(msg,true));

        } else if (mtType.equals(HASCO.WKF)) {

            System.out.println("=== IngestionAPI.uningestMetadataTemplate() WKF BRANCH ===");
            System.out.println("  metadataTemplateUri: " + metadataTemplateUri);

            WKF wkf = WKF.find(metadataTemplateUri);
            if (wkf == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve WKF with metadataTemplateUri = " + metadataTemplateUri;
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }

            System.out.println("  WKF found: " + wkf.getLabel());
            System.out.println("  WKF DataFileURI: " + wkf.getHasDataFileUri());

            DataFile dataFile = DataFile.find(wkf.getHasDataFileUri());
            if (dataFile == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve WKF's dataFile = " + wkf.getHasDataFileUri();
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }

            System.out.println("  DataFile found: " + dataFile.getFilename());
            System.out.println("IngestionAPI.uningestMetadataTemplate(): API has able to retrieve WKF from triplestore");

            // IMPORTANT: UNINGEST should NOT delete the physical file!
            // It should only:
            // 1. Delete the ingested content from the named graph (but keep WKF and DataFile metadata)
            // 2. Reset the DataFile status to UNPROCESSED
            // This allows the user to re-ingest the same file later.

            // CRITICAL FIX: DO NOT delete the entire named graph!
            // The WKF metadata is stored IN THE SAME named graph as the ingested content!
            // If we delete everything, the WKF disappears from the listing because it loses hasco:hasDataFile.
            //
            // Instead, we delete ONLY the ingested content (ResponseOptions, Codebooks, Components, etc.)
            // and preserve the WKF and DataFile metadata.
            System.out.println("  Deleting ONLY ingested content from named graph: " + dataFile.getUri());
            System.out.println("  (Preserving WKF and DataFile metadata in the same graph)");

            String namedGraphUri = dataFile.getUri();
            String wkfUri = wkf.getUri();

            // Build a SPARQL DELETE query that excludes WKF and DataFile triples
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
            queryString += "WITH <" + namedGraphUri + "> ";
            queryString += "DELETE { ?s ?p ?o } WHERE { ";
            queryString += "  ?s ?p ?o . ";
            queryString += "  FILTER(?s != <" + wkfUri + "> && ?s != <" + namedGraphUri + ">) ";
            queryString += "} ";

            try {
                UpdateRequest req = UpdateFactory.create(queryString);
                UpdateProcessor processor = UpdateExecutionFactory.createRemote(req,
                        CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE));
                processor.execute();
                System.out.println("  ✓ Ingested content deleted successfully");
                System.out.println("  ✓ WKF metadata preserved: " + wkfUri);
                System.out.println("  ✓ DataFile metadata preserved: " + namedGraphUri);
            } catch (Exception e) {
                System.out.println("  [ERROR] Failed to delete ingested content: " + e.getMessage());
                e.printStackTrace();
            }

            // Reset DataFile to UNPROCESSED status (keep the file and metadata)
            System.out.println("  Resetting DataFile to UNPROCESSED...");
            dataFile.resetForUnprocessed();
            dataFile.save();
            System.out.println("  DataFile reset to UNPROCESSED and saved");

            String msg = "IngestionAPI.uningestMetadataTemplate(): successfully uningest metadataTemplateUri " + metadataTemplateUri;
            System.out.println(msg);
            return ok(ApiUtil.createResponse(msg,true));

        }

        String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate(): metadataTemplateUri " + metadataTemplateUri +
            " returned template that cannot be uningested.";
        System.out.println(errorMsg);
        return ok(ApiUtil.createResponse(errorMsg,false));

    }

    // Health check endpoint to verify routes are loaded
    public Result mtGenHealthCheck() {
        System.out.println("[HEALTH CHECK] mtGenHealthCheck() called - routes are loaded!");
        return ok(ApiUtil.createResponse("DP2 generation routes are active", true));
    }

    public Result mtGenByStatus(String elementtype, String datafileuri, String status, String filename, String mediaFolder, String verifyUri) {
        System.out.println("\n========== IngestionAPI.mtGenByStatus() START ==========");
        System.out.println("✓ mtGenByStatus endpoint was called successfully!");
        System.out.println("Parameters:");
        System.out.println("  elementtype: [" + elementtype + "]");
        System.out.println("  datafileuri: [" + datafileuri + "]");
        System.out.println("  status: [" + status + "]");
        System.out.println("  filename: [" + filename + "]");
        System.out.println("  mediaFolder: [" + mediaFolder + "]");
        System.out.println("  verifyUri: [" + verifyUri + "]");

        // DEBUG: Check ConfigProp path
        try {
            String configPath = ConfigProp.getPathIngestion();
            System.out.println("  ConfigProp.getPathIngestion(): [" + configPath + "]");
            java.io.File testDir = new java.io.File(configPath);
            System.out.println("  Path exists: " + testDir.exists());
            System.out.println("  Path is directory: " + testDir.isDirectory());
            System.out.println("  Path can write: " + testDir.canWrite());
        } catch (Exception e) {
            System.err.println("  ❌ ERROR getting ingestion path: " + e.getMessage());
            e.printStackTrace();
        }

        if (elementtype == null || elementtype.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByStatus() requires elementtype";
            System.out.println(errorMsg);
            System.out.println("========== IngestionAPI.mtGenByStatus() END (ERROR) ==========\n");
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (status == null || status.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByStatus() requires status";
            System.out.println(errorMsg);
            System.out.println("========== IngestionAPI.mtGenByStatus() END (ERROR) ==========\n");
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (filename == null || filename.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByStatus() requires filename";
            System.out.println(errorMsg);
            System.out.println("========== IngestionAPI.mtGenByStatus() END (ERROR) ==========\n");
            return ok(ApiUtil.createResponse(errorMsg,false));
        }

        System.out.println("✓ All required parameters present");
        System.out.println("→ Calling generator for elementtype: " + elementtype);
        String generationResult = null;

        try {
            switch (elementtype) {
                case "ins":
                    System.out.println("  Calling INSGen.genByStatus()...");
                    generationResult = INSGen.genByStatus(status,filename,mediaFolder,verifyUri);
                    System.out.println("  INSGen.genByStatus() returned: [" + generationResult + "]");
                    break;
                case "dp2":
                    // DP2 status is held at the DP2 MT level; scope generation by the DataFile URI.
                    System.out.println("  Calling DP2Gen.genByStatus()...");
                    generationResult = DP2Gen.genByStatus(datafileuri, status, filename, mediaFolder, verifyUri);
                    System.out.println("  DP2Gen.genByStatus() returned: [" + generationResult + "]");
                    break;
                case "dsg":
                    System.out.println("  Calling DSGGen.genByStatus()...");
                    generationResult = DSGGen.genByStatus(status,filename,mediaFolder,verifyUri);
                    System.out.println("  DSGGen.genByStatus() returned: [" + generationResult + "]");
                    break;
                case "kgr":
                    System.out.println("  Calling KGRGen.genByStatus()...");
                    generationResult = KGRGen.genByStatus(status,filename,mediaFolder,verifyUri);
                    System.out.println("  KGRGen.genByStatus() returned: [" + generationResult + "]");
                    break;
                case "wkf":
                    System.out.println("  WKF generation requested...");
                    String errorMsg = "WKF generation (WKFGen.java) is not implemented yet. " +
                                     "Only ingestion is currently supported for WKF. " +
                                     "To implement: create WKFGen.java similar to DP2Gen.java with methods to generate Excel from triple store data.";
                    System.out.println("  ❌ ERROR: " + errorMsg);
                    System.out.println("========== IngestionAPI.mtGenByStatus() END (NOT IMPLEMENTED) ==========\n");
                    return ok(ApiUtil.createResponse(errorMsg, false));
                default:
                    String errorMsg2 = "[ERROR] IngestionAPI.mtGenByStatus() invalid elementtype=[" + elementtype + "]. " +
                                      "Supported types: ins, dp2, dsg, kgr. Note: wkf generation not yet implemented.";
                    System.out.println(errorMsg2);
                    System.out.println("========== IngestionAPI.mtGenByStatus() END (ERROR) ==========\n");
                    return ok(ApiUtil.createResponse(errorMsg2,false));
            }
        } catch (Exception e) {
            System.err.println("  ❌ EXCEPTION during generation:");
            System.err.println("     Exception type: " + e.getClass().getName());
            System.err.println("     Message: " + e.getMessage());
            e.printStackTrace();
            System.out.println("========== IngestionAPI.mtGenByStatus() END (EXCEPTION) ==========\n");
            return ok(ApiUtil.createResponse("Generation failed with exception: " + e.getMessage(), false));
        }

        System.out.println("  Generation result: [" + generationResult + "]");
        System.out.println("  Generation result is null: " + (generationResult == null));
        System.out.println("  Generation result is empty: " + (generationResult != null && generationResult.isEmpty()));
        System.out.println("  Generation result length: " + (generationResult != null ? generationResult.length() : "N/A"));

        // Check if generation failed (contains "Error" or "FAILURE")
        boolean isFailed = generationResult != null &&
                          (generationResult.toLowerCase().contains("error") ||
                           generationResult.toLowerCase().contains("failure"));

        if (isFailed) {
            System.out.println("  ❌ Generation FAILED: " + generationResult);
            System.out.println("========== IngestionAPI.mtGenByStatus() END (FAILURE) ==========\n");
            return ok(ApiUtil.createResponse(generationResult, false));
        }

        // IMPORTANT: Ensure we always return a valid filename, never null
        // Empty string, null, or "SUCCESS" means generation worked but didn't return path
        if (generationResult == null || generationResult.isEmpty() ||
            generationResult.equals("SUCCESS") || generationResult.equals("null")) {
            System.out.println("  ⚠️ WARNING: Generator returned null/empty/SUCCESS: [" + generationResult + "]");
            System.out.println("  → Using filename parameter as fallback: [" + filename + "]");

            // Safety check: filename itself might be "null" string or null
            if (filename == null || filename.isEmpty() || filename.equals("null")) {
                String errorMsg = "[ERROR] Generator returned null/empty AND filename parameter is invalid: [" + filename + "]";
                System.err.println(errorMsg);
                System.out.println("========== IngestionAPI.mtGenByStatus() END (ERROR) ==========\n");
                return ok(ApiUtil.createResponse(errorMsg, false));
            }

            generationResult = filename; // Use the filename that was passed in
        }

        System.out.println("✓ Generation completed successfully");
        System.out.println("  Final filename for response: [" + generationResult + "]");
        System.out.println("========== IngestionAPI.mtGenByStatus() END (SUCCESS) ==========\n");

        // FINAL SAFETY CHECK: Never return null or "null" string
        if (generationResult == null || generationResult.equals("null")) {
            String safeResponse = "generated-file.xlsx"; // Ultimate fallback
            System.err.println("⚠️ CRITICAL: Final response was null, using fallback: " + safeResponse);
            return ok(ApiUtil.createResponse(safeResponse, true));
        }

        // Return the filename so the client knows what file to download
        return ok(ApiUtil.createResponse(generationResult, true));
    }

    public Result mtGenByElement(String elementtype, String datafileuri, String elementuri, String filename, String mediaFolder, String verifyUri) {
        System.out.println("IngestionAPI.mtGenByElement");
        System.out.println("  ElementType: [" + elementtype + "] DataFileUri: [" + datafileuri + "]");
        System.out.println("  ElementUri: [" + elementuri + "] VerifyUri: [" + verifyUri + "]");

        if (elementtype == null || elementtype.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByElement() requires elementtype";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (elementuri == null || elementuri.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByElement() requires elementuri";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }

        // The UI historically sends the generator MT type (e.g., 'dsg') as elementtype,
        // but the backend logic here was written expecting selectors like 'study', 'instrument', etc.
        // Normalize to a selector type by inferring it from the generator MT type.
        String selectorType = elementtype;
        if ("dsg".equals(elementtype)) {
            selectorType = "study";
        } else if ("ins".equals(elementtype)) {
            selectorType = "instrument";
        } else if ("kgr".equals(elementtype)) {
            // KGR can be generated by multiple selectors; keep as-is if the caller already distinguishes.
            // If the UI ever sends 'kgr' here, we can't infer which selector to use.
            selectorType = elementtype;
        } else if ("dp2".equals(elementtype) || "sdd".equals(elementtype) || "str".equals(elementtype)) {
            // Not supported by this endpoint today.
            selectorType = elementtype;
        }

        HADatAcThing element = null;
        if (selectorType.equals("study")) {
            // Some clients pass the Study ID (e.g., STD-...); resolve that into the Study URI.
            element = (HADatAcThing)Study.findById(elementuri);
            if (element == null) {
                element = (HADatAcThing)Study.find(elementuri);
            }
        } else if (selectorType.equals("instrument")) {
            element = (HADatAcThing)Instrument.find(elementuri);
        } else if (selectorType.equals("organization")) {
            element = (HADatAcThing)Organization.find(elementuri);
        } else if (selectorType.equals("place")) {
            element = (HADatAcThing)Place.find(elementuri);
        } else if (selectorType.equals("project")) {
            element = (HADatAcThing)Project.find(elementuri);
        } else if (selectorType.equals("fundingscheme")) {
            element = (HADatAcThing)FundingScheme.find(elementuri);
        } else {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByElement() has invalid elementtype";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }

        if (element == null) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByElement() cannot retrieve element with uri=[" + elementuri + "]";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (filename == null || filename.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.mtGenByElement() requires filename";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg,false));
        }

        String resp = "";
        switch (selectorType) {
            case "study":
                resp = DSGGen.genByStudy((Study)element,filename,mediaFolder,verifyUri);
                break;
            case "instrument":
                resp = INSGen.genByInstrument((Instrument)element,filename,mediaFolder,verifyUri);
                break;
            case "organization":
                System.out.println("Calling KGR.genByOrganization()");
                resp = KGRGen.genByOrganization((Organization)element,filename,mediaFolder,verifyUri);
                break;
            case "place":
                resp = KGRGen.genByPlace((Place)element,filename,mediaFolder,verifyUri);
                break;
            case "project":
                resp = KGRGen.genByProject((Project)element,filename,mediaFolder,verifyUri);
                break;
            case "fundingscheme":
                resp = KGRGen.genByFundingScheme((FundingScheme)element,filename,mediaFolder,verifyUri);
                break;
            default:
                String errorMsg = "[ERROR] IngestionAPI.mtGenByElement() invalid elementtype=[" + elementtype + "]";
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
        }
        if (resp.equals("")) {
            return ok(ApiUtil.createResponse(resp, true));
        } else {
            return ok(ApiUtil.createResponse(resp, false));
        }
    }

    public Result mtGenByManager(String elementtype, String datafileuri, String useremail, String status, String filename, String mediaFolder, String verifyUri) {
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
                INSGen.genByManager(useremail, status, filename, mediaFolder, verifyUri);
                break;
            case "kgr":
                KGRGen.genByManager(useremail, status, filename, mediaFolder, verifyUri);
                break;
            case "dsg":
                DSGGen.genByManager(useremail, status, filename, mediaFolder, verifyUri);
                break;
            case "wkf":
                WKFGen.genByManager(useremail, status, filename, mediaFolder, verifyUri);
                break;
            default:
                String errorMsg = "[ERROR] IngestionAPI.mtGenByStatus() invalid elementtype=[" + elementtype + "]";
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
        }
        return ok(ApiUtil.createResponse("", true));
    }

    public Result mtGetGenerated(String filename) {
        // Validate filename
        if (filename == null || filename.trim().isEmpty()) {
            return badRequest(ApiUtil.createResponse(
                "[ERROR] IngestionAPI.mtGetGenerated(): No filename provided.", false));
        }

        // Get ingestion base path
        String basePath = ConfigProp.getPathIngestion();
        if (basePath == null || basePath.trim().isEmpty()) {
            System.err.println("[ERROR] IngestionAPI.mtGetGenerated(): Invalid ingestion path from ConfigProp.getPathIngestion()");
            return internalServerError(ApiUtil.createResponse(
                "[ERROR] IngestionAPI.mtGetGenerated(): Invalid file storage path.", false));
        }

        // Build file path (must match the save() method)
        Path filePath = Paths.get(basePath, filename);
        File file = filePath.toFile();

        // Validate file existence
        if (!file.exists() || !file.isFile()) {
            System.err.println("[ERROR] IngestionAPI.mtGetGenerated(): File not found - " + filePath);
            return notFound(ApiUtil.createResponse(
                "[ERROR] IngestionAPI.mtGetGenerated(): File not found.", false));
        }

        // Serve file for download
        return ok(file)
            .as("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") // Proper MIME for Excel (XLSX)
            .withHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
    }

    public Result getLog(String dataFileUri) {
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


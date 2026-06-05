package org.hascoapi.console.controllers.restapi;

import org.hascoapi.entity.pojo.WKF;
import org.hascoapi.transform.mt.wkf.WKFGen;
import org.hascoapi.Constants;
import org.hascoapi.ingestion.IngestionWorker;
import org.hascoapi.entity.pojo.DA;
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
import org.hascoapi.entity.pojo.ProcessStem;
import org.hascoapi.entity.pojo.FundingScheme;
import org.hascoapi.entity.pojo.Place;
import org.hascoapi.entity.pojo.Project;
import org.hascoapi.entity.pojo.Organization;
import org.hascoapi.transform.mt.dp2.DP2Gen;
import org.hascoapi.transform.mt.dsg.DSGGen;
import org.hascoapi.transform.mt.ins.INSGen;
import org.hascoapi.transform.mt.kgr.KGRGen;
import org.hascoapi.transform.mt.sdd.SDDGen;
import org.hascoapi.transform.mt.soc.SOCGen;

import org.hascoapi.utils.*;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.ConfigProp;

import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.utils.NameSpaces;

import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import com.typesafe.config.Config;

import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import static org.hascoapi.Constants.*;


import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
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

        // NOTE: "da" is NOT an official element type in hascoapi
        // It is ONLY allowed here for DA-SOC-* files (DASOC ingestion)
        // IngestionWorker validates filename must start with "DA-SOC-"
        // General DA ingestion is not supported at this time
        if (!elementType.equals("dp2") &&
            !elementType.equals("dsg") &&
            !elementType.equals("da") &&
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
            
            // ✅ CRITICAL FIX: Propagate hasSIRManagerEmail from DSG to DataFile
            // This ensures instruments created from DSG are visible in frontend
            // filtering by manager email (endpoint: /instrument/manageremail/{email})
            if (dataFile != null) {
                if (dsg.getHasSIRManagerEmail() != null && !dsg.getHasSIRManagerEmail().isEmpty()) {
                    dataFile.setHasSIRManagerEmail(dsg.getHasSIRManagerEmail());
                    dataFile.save();
                    System.out.println("[INGESTION FIX] Set DataFile.hasSIRManagerEmail from DSG: " + dsg.getHasSIRManagerEmail());
                } else {
                    System.out.println("[WARNING] DSG has no hasSIRManagerEmail - instruments created may not be visible in frontend");
                    System.out.println("[WARNING] Please ensure DSG includes hasSIRManagerEmail property");
                }
            }
        } else if (elementType.equals("da")) {
            // IMPORTANT: "da" is accepted ONLY for DA-SOC-* files
            // IngestionWorker enforces DA-SOC-* filename validation
            System.out.println("\n=== [INGESTION PATH] IngestionAPI.ingest() ===");
            System.out.println("[INGESTION PATH] Element Type: da");
            System.out.println("[INGESTION PATH] Element URI: " + elementUri);

            DA da = DA.find(elementUri);
            if (da == null) {
                // For DA-SOC files, the DA may not exist yet - AnnotateDASOC will create it
                // Create a temporary DataFile to hold the upload and trigger processing
                System.out.println("[INGESTION PATH] DA not found, creating placeholder DataFile for DA-SOC processing");

                // Extract filename: FIRST from multipart, THEN from query parameter
                String filename = null;
                
                // Try to get filename from multipart/form-data
                if (request.body() != null && request.body().asMultipartFormData() != null) {
                    play.mvc.Http.MultipartFormData multipart = request.body().asMultipartFormData();
                    play.mvc.Http.MultipartFormData.FilePart<Object> filePart = multipart.getFile("file");
                    
                    if (filePart != null) {
                        filename = filePart.getFilename();
                        System.out.println("[INGESTION PATH] Filename from multipart: " + filename);
                    }
                }
                
                // Fallback: try query parameter
                if (filename == null || filename.isEmpty()) {
                    filename = request.getQueryString("filename");
                    if (filename != null && !filename.isEmpty()) {
                        System.out.println("[INGESTION PATH] Filename from query parameter: " + filename);
                    }
                }
                
                // Last resort fallback
                if (filename == null || filename.isEmpty()) {
                    filename = "DA-SOC-UNKNOWN.csv";
                    System.out.println("[INGESTION PATH] Using fallback filename: " + filename);
                }

                // Create temporary DataFile with unique ID
                String dataFileId = "DFL" + System.currentTimeMillis();
                dataFile = DataFile.create(dataFileId, filename, "", DataFile.UNPROCESSED);
                String tempUri = ConfigProp.getKbPrefix() + dataFileId;
                dataFile.setUri(tempUri);
                dataFile.setNamedGraph(tempUri); // Set named graph to allow saving
                dataFile.setDasocDataAcquisitionUri(elementUri); // Store DA URI for AnnotateDASOC
                dataFile.save();

                System.out.println("[INGESTION PATH] Created placeholder DataFile: " + dataFile.getUri());
                System.out.println("[INGESTION PATH] Stored DA URI in DataFile: " + elementUri);
                System.out.println("[INGESTION PATH] Filename: " + filename);
            } else {
                dataFile = DataFile.find(da.getHasDataFileUri());
                if (dataFile != null) {
                    // Ensure DA URI is set for DASOC processing
                    dataFile.setDasocDataAcquisitionUri(elementUri);
                    dataFile.save();
                    System.out.println("[INGESTION PATH] Found existing DA, stored DA URI in DataFile: " + elementUri);
                }
            }
        } else if (elementType.equals("ins")) {
            // ⚠️ DEPRECATION WARNING
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("⚠️  WARNING: INS format is DEPRECATED - use DSG + DA-SOC instead");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("INS files are deprecated. Please migrate to DSG + DA-SOC workflow.");
            System.out.println("See: docs/INS-TO-DSG-TRANSFORMATION-PLAN.md");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
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
            Path candidatePath;
            File preUploadedFile;
            Path filenamePath = Paths.get(dataFile.getFilename());
            if (filenamePath.isAbsolute()) {
                candidatePath = filenamePath;
                preUploadedFile = candidatePath.toFile();
                System.out.println("IngestionAPI.ingest(): [ABSOLUTE] Checking for pre-uploaded file at: " + candidatePath.toAbsolutePath());
            } else {
                candidatePath = Paths.get(basePath, Constants.RESOURCE_FOLDER, uriTerm, dataFile.getFilename());
                preUploadedFile = candidatePath.toFile();
                System.out.println("IngestionAPI.ingest(): [RELATIVE] Checking for pre-uploaded file at: " + candidatePath.toAbsolutePath());
            }
            if (preUploadedFile.exists() && preUploadedFile.length() > 0) {
                System.out.println("IngestionAPI.ingest(): Found pre-uploaded file!");
                fileToIngest = preUploadedFile;
            } else {
                System.out.println("IngestionAPI.ingest(): Pre-uploaded file NOT found or empty (exists: " + preUploadedFile.exists() + ", size: " + (preUploadedFile.exists() ? preUploadedFile.length() : "N/A") + ")");
            }
        }

        // SECOND: If not pre-uploaded, try to get file from request body
        if (fileToIngest == null) {
            System.out.println("[DEBUG] IngestionAPI.ingest(): Attempting to extract file from request body...");
            System.out.println("[DEBUG] IngestionAPI.ingest(): Request body exists: " + (request.body() != null));
            System.out.println("[DEBUG] IngestionAPI.ingest(): Request has asRaw: " + (request.body() != null && request.body().asRaw() != null));
            System.out.println("[DEBUG] IngestionAPI.ingest(): Request has asMultipartFormData: " + (request.body() != null && request.body().asMultipartFormData() != null));
            System.out.println("[DEBUG] IngestionAPI.ingest(): Request has asBytes: " + (request.body() != null && request.body().asBytes() != null));

            String ct = request.contentType().orElse("").toLowerCase();
            boolean bodyIsFileUpload = ct.contains("multipart/form-data") || ct.equals("application/octet-stream");
            if (!bodyIsFileUpload) {
                // Avoid interpreting JSON bodies (e.g., "{}") as a binary file upload.
                // Ingestion should rely on the pre-uploaded DataFile under resources/{DFL...}/.
                // If it's missing, the fallback logic below will instruct the caller to upload first.
                System.out.println("[DEBUG] IngestionAPI.ingest(): Request content-type is not file upload (" + ct + "), skipping body file extraction.");
            } else {
            File fileFromRequest = null;

            // Try asRaw() first (legacy workflow)
            if (request.body() != null && request.body().asRaw() != null) {
                System.out.println("[DEBUG] IngestionAPI.ingest(): Trying to extract file from asRaw()...");
                fileFromRequest = request.body().asRaw().asFile();
                if (fileFromRequest != null) {
                    System.out.println("[DEBUG] IngestionAPI.ingest(): Extracted file from asRaw(): " + fileFromRequest.getAbsolutePath() + " (size: " + fileFromRequest.length() + ")");
                } else {
                    System.out.println("[DEBUG] IngestionAPI.ingest(): asRaw().asFile() returned null");
                }
            }

            // Try asMultipartFormData() if asRaw() failed
            if (fileFromRequest == null && request.body() != null && request.body().asMultipartFormData() != null) {
                System.out.println("[DEBUG] IngestionAPI.ingest(): Trying to extract file from multipartFormData...");
                play.mvc.Http.MultipartFormData multipart = request.body().asMultipartFormData();
                play.mvc.Http.MultipartFormData.FilePart<Object> filePart = multipart.getFile("file");

                if (filePart != null) {
                    System.out.println("[DEBUG] IngestionAPI.ingest(): FilePart found, filename: " + filePart.getFilename());
                    Object fileObj = filePart.getRef();
                    System.out.println("[DEBUG] IngestionAPI.ingest(): FilePart reference type: " + (fileObj != null ? fileObj.getClass().getName() : "null"));
                    
                    if (fileObj instanceof File) {
                        fileFromRequest = (File) fileObj;
                        System.out.println("[DEBUG] IngestionAPI.ingest(): Extracted File: " + fileFromRequest.getAbsolutePath() + " (size: " + fileFromRequest.length() + ")");
                    } else if (fileObj instanceof play.api.libs.Files.TemporaryFile) {
                        play.api.libs.Files.TemporaryFile tempFile = (play.api.libs.Files.TemporaryFile) fileObj;
                        fileFromRequest = tempFile.path().toFile();
                        System.out.println("[DEBUG] IngestionAPI.ingest(): Extracted TemporaryFile (Scala): " + fileFromRequest.getAbsolutePath() + " (size: " + fileFromRequest.length() + ")");
                    } else if (fileObj instanceof play.libs.Files.TemporaryFile) {
                        // Java API version of TemporaryFile
                        play.libs.Files.TemporaryFile tempFile = (play.libs.Files.TemporaryFile) fileObj;
                        fileFromRequest = tempFile.path().toFile();
                        System.out.println("[DEBUG] IngestionAPI.ingest(): Extracted TemporaryFile (Java): " + fileFromRequest.getAbsolutePath() + " (size: " + fileFromRequest.length() + ")");
                    } else {
                        // Try reflection as last resort to get the file
                        try {
                            java.lang.reflect.Method pathMethod = fileObj.getClass().getMethod("path");
                            Object pathObj = pathMethod.invoke(fileObj);
                            if (pathObj instanceof java.nio.file.Path) {
                                fileFromRequest = ((java.nio.file.Path) pathObj).toFile();
                                System.out.println("[DEBUG] IngestionAPI.ingest(): Extracted file via reflection: " + fileFromRequest.getAbsolutePath() + " (size: " + fileFromRequest.length() + ")");
                            }
                        } catch (Exception e) {
                            System.out.println("[ERROR] IngestionAPI.ingest(): FilePart reference is of unknown type and reflection failed: " + e.getMessage());
                        }
                    }
                } else {
                    System.out.println("[ERROR] IngestionAPI.ingest(): FilePart is null! multipart.getFile('file') returned null");
                }
            } else {
                System.out.println("[DEBUG] IngestionAPI.ingest(): Skipping multipartFormData extraction (fileFromRequest: " + (fileFromRequest != null) + ", request.body: " + (request.body() != null) + ", multipartFormData: " + (request.body() != null && request.body().asMultipartFormData() != null) + ")");
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
                // IMPORTANTE: Copiar arquivo para o local esperado pelo IngestionWorker
                // O arquivo temporário do Play está em /tmp, mas o IngestionWorker espera em /resources/DFL.../
                if (basePath != null && dataFile.getUri() != null && dataFile.getFilename() != null) {
                    try {
                        String uriTerm = URIUtils.uriLastSegment(dataFile.getUri());
                        Path targetDir = Paths.get(basePath, Constants.RESOURCE_FOLDER, uriTerm);
                        Path targetFile = targetDir.resolve(dataFile.getFilename());
                        
                        // Criar diretório se não existir
                        if (!Files.exists(targetDir)) {
                            Files.createDirectories(targetDir);
                            System.out.println("IngestionAPI.ingest(): Created directory: " + targetDir);
                        }
                        
                        // Copiar arquivo do multipart para o local correto
                        Files.copy(fileFromRequest.toPath(), targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("IngestionAPI.ingest(): Copied multipart file to: " + targetFile);
                        System.out.println("IngestionAPI.ingest(): File size: " + Files.size(targetFile) + " bytes");
                        
                        // Usar o arquivo no local correto
                        fileToIngest = targetFile.toFile();
                        
                    } catch (IOException e) {
                        System.err.println("[ERROR] Failed to copy multipart file to target location: " + e.getMessage());
                        e.printStackTrace();
                        // Fallback: usar arquivo temporário mesmo
                        fileToIngest = fileFromRequest;
                    }
                } else {
                    fileToIngest = fileFromRequest;
                }
            }
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
            Path uploadedFilePath;
            Path filenamePath = Paths.get(dataFile.getFilename());
            if (filenamePath.isAbsolute()) {
                uploadedFilePath = filenamePath;
                System.out.println("IngestionAPI.ingest(): [ABSOLUTE] Looking for file at path: " + uploadedFilePath.toAbsolutePath());
            } else {
                uploadedFilePath = Paths.get(basePath, Constants.RESOURCE_FOLDER, uriTerm, dataFile.getFilename());
                System.out.println("IngestionAPI.ingest(): [RELATIVE] Looking for file at path: " + uploadedFilePath.toAbsolutePath());
            }
            File uploadedFile = uploadedFilePath.toFile();

            System.out.println("IngestionAPI.ingest(): Waiting for file to be available (uploadFile is async)...");

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
                String errorMessage = "File not found or upload not completed.\n\n" +
                    "Expected file location: " + uploadedFilePath.toAbsolutePath() + "\n\n" +
                    "WORKFLOW REQUIRED:\n" +
                    "1. Create WKF metadata: POST /hascoapi/api/wkf/create/{json}\n" +
                    "2. Upload file: POST /hascoapi/api/uploadFile/{wkfUri}/{filename} (with multipart form data)\n" +
                    "3. Trigger ingestion: POST /hascoapi/api/ingest/{status}/wkf/{wkfUri}\n\n" +
                    "It appears step 2 (uploadFile) was not called. Please upload the file before triggering ingestion.\n\n" +
                    "See docs/WKF-INGESTION-WORKFLOW.md for complete instructions.";
                return ok(ApiUtil.createResponse(errorMessage, false));
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
            } else if (mtRaw.getHascoTypeUri().equals(HASCO.DATA_ACQUISITION)) {
                mtType = HASCO.DATA_ACQUISITION;
                System.out.println("IngestionAPI.uningestMetadataTemplate() read DA (DataAcquisition)");
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

        } else if (mtType.equals(HASCO.DATA_ACQUISITION)) {

            System.out.println("=== IngestionAPI.uningestMetadataTemplate() DA (DataAcquisition) BRANCH ===");
            System.out.println("  metadataTemplateUri: " + metadataTemplateUri);

            DA da = DA.find(metadataTemplateUri);
            if (da == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve DA with metadataTemplateUri = " + metadataTemplateUri;
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }

            System.out.println("  DA found: " + da.getLabel());
            System.out.println("  DA DataFileURI: " + da.getHasDataFileUri());

            DataFile dataFile = DataFile.find(da.getHasDataFileUri());
            if (dataFile == null) {
                String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve DA's dataFile = " + da.getHasDataFileUri();
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
            }

            System.out.println("  DataFile found: " + dataFile.getFilename());
            System.out.println("IngestionAPI.uningestMetadataTemplate(): API has able to retrieve DA from triplestore");

            // DA-SOC files enrich existing entities (Instruments, Components, etc.)
            // Deleting the entire named graph will remove these enrichments
            // We delete only the DA-specific triples and preserve the enriched entities
            System.out.println("  Deleting DA annotation triples from named graph: " + dataFile.getUri());

            String namedGraphUri = dataFile.getUri();
            String daUri = da.getUri();

            // Build a SPARQL DELETE query that removes DA-specific triples
            // but preserves enriched entity data
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
            queryString += "WITH <" + namedGraphUri + "> ";
            queryString += "DELETE { ?s ?p ?o } WHERE { ";
            queryString += "  ?s ?p ?o . ";
            queryString += "  FILTER(?s != <" + daUri + "> && ?s != <" + namedGraphUri + ">) ";
            queryString += "} ";

            try {
                UpdateRequest req = UpdateFactory.create(queryString);
                UpdateProcessor processor = UpdateExecutionFactory.createRemote(req,
                        CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE));
                processor.execute();
                System.out.println("  ✓ DA annotation triples deleted successfully");
                System.out.println("  ✓ DA metadata preserved: " + daUri);
                System.out.println("  ✓ DataFile metadata preserved: " + namedGraphUri);
            } catch (Exception e) {
                System.out.println("  [ERROR] Failed to delete DA annotation triples: " + e.getMessage());
                e.printStackTrace();
            }

            // Reset DataFile to UNPROCESSED status
            System.out.println("  Resetting DataFile to UNPROCESSED...");
            dataFile.resetForUnprocessed();
            dataFile.save();
            System.out.println("  DataFile reset to UNPROCESSED and saved");

            String msg = "IngestionAPI.uningestMetadataTemplate(): successfully uningest DA metadataTemplateUri " + metadataTemplateUri;
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

    public Result mtGenByStatus(String elementtype, String datafileuri, String status, String filename, String mediaFolder, String verifyUri, Boolean generateDASOCs) {
        System.out.println("\n========== IngestionAPI.mtGenByStatus() START ==========");
        System.out.println("✓ mtGenByStatus endpoint was called successfully!");
        System.out.println("Parameters:");
        System.out.println("  elementtype: [" + elementtype + "]");
        System.out.println("  datafileuri: [" + datafileuri + "]");
        System.out.println("  status: [" + status + "]");
        System.out.println("  filename: [" + filename + "]");
        System.out.println("  mediaFolder: [" + mediaFolder + "]");
        System.out.println("  verifyUri: [" + verifyUri + "]");
        System.out.println("  generateDASOCs: [" + generateDASOCs + "]");
        
        // Default to false if not provided
        boolean shouldGenerateDASOCs = (generateDASOCs != null && generateDASOCs);

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
                    generationResult = DSGGen.genByStatus(status,filename,mediaFolder,verifyUri,shouldGenerateDASOCs);
                    System.out.println("  DSGGen.genByStatus() returned: [" + generationResult + "]");
                    break;
                case "kgr":
                    System.out.println("  Calling KGRGen.genByStatus()...");
                    generationResult = KGRGen.genByStatus(status,filename,mediaFolder,verifyUri);
                    System.out.println("  KGRGen.genByStatus() returned: [" + generationResult + "]");
                    break;
                case "sdd":
                    System.out.println("  Calling SDDGen.genByStatus()...");
                    generationResult = SDDGen.genByStatus(datafileuri, status, filename, mediaFolder, datafileuri);
                    System.out.println("  SDDGen.genByStatus() returned: [" + generationResult + "]");
                    break;
                case "wkf":
                    System.out.println("  Calling WKFGen.genByStatus()...");
                    generationResult = WKFGen.genByStatus(status, filename, mediaFolder, verifyUri, datafileuri);
                    System.out.println("  WKFGen.genByStatus() returned: [" + generationResult + "]");
                    break;
                case "soc":
                    // SOC generation requires a study URI instead of datafileuri
                    // The frontend should pass the study URI in the datafileuri parameter for SOC generation
                    System.out.println("  Calling SOCGen.genByStudy()...");
                    generationResult = SOCGen.genByStudy(datafileuri, filename, mediaFolder, verifyUri);
                    System.out.println("  SOCGen.genByStudy() returned: [" + generationResult + "]");
                    break;
                default:
                    String errorMsg2 = "[ERROR] IngestionAPI.mtGenByStatus() invalid elementtype=[" + elementtype + "]. " +
                                      "Supported types: ins, dp2, dsg, kgr, sdd, wkf, soc.";
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

    public Result mtGenByElement(String elementtype, String datafileuri, String elementuri, String filename, String mediaFolder, String verifyUri, Boolean generateDASOCs) {
        System.out.println("IngestionAPI.mtGenByElement");
        System.out.println("  ElementType: [" + elementtype + "] DataFileUri: [" + datafileuri + "]");
        System.out.println("  ElementUri: [" + elementuri + "] VerifyUri: [" + verifyUri + "]");
        System.out.println("  generateDASOCs: [" + generateDASOCs + "]");
        
        // Default to false if not provided
        boolean shouldGenerateDASOCs = (generateDASOCs != null && generateDASOCs);

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
        } else if ("wkf".equals(elementtype)) {
            selectorType = "processstem";
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
        } else if (selectorType.equals("processstem")) {
            element = (HADatAcThing)ProcessStem.find(elementuri);
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
                resp = DSGGen.genByStudy((Study)element,filename,mediaFolder,verifyUri,shouldGenerateDASOCs);
                break;
            case "instrument":
                resp = INSGen.genByInstrument((Instrument)element,filename,mediaFolder,verifyUri);
                break;
            case "processstem":
                resp = WKFGen.genByProcessStem((ProcessStem)element,filename,mediaFolder,verifyUri);
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

        boolean isFailed = resp != null &&
                (resp.toLowerCase().contains("error") || resp.toLowerCase().contains("failure"));
        if (isFailed) {
            return ok(ApiUtil.createResponse(resp, false));
        }

        if (resp == null || resp.isEmpty() || resp.equals("SUCCESS") || resp.equals("null")) {
            resp = filename;
        }

        return ok(ApiUtil.createResponse(resp, true));
    }

    public Result mtGenByManager(String elementtype, String datafileuri, String useremail, String status, String filename, String mediaFolder, String verifyUri, Boolean generateDASOCs) {
        System.out.println("\n========== IngestionAPI.mtGenByManager() START ==========");
        System.out.println("  elementtype: [" + elementtype + "]");
        System.out.println("  useremail: [" + useremail + "]");
        System.out.println("  status: [" + status + "]");
        System.out.println("  filename: [" + filename + "]");
        System.out.println("  generateDASOCs: [" + generateDASOCs + "]");
        
        // Default to false if not provided
        boolean shouldGenerateDASOCs = (generateDASOCs != null && generateDASOCs);
        
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
        String generationResult = null;
        switch (elementtype) {
            case "ins":
                generationResult = INSGen.genByManager(useremail, status, filename, mediaFolder, verifyUri);
                break;
            case "kgr":
                generationResult = KGRGen.genByManager(useremail, status, filename, mediaFolder, verifyUri);
                break;
            case "dsg":
                generationResult = DSGGen.genByManager(useremail, status, filename, mediaFolder, verifyUri, shouldGenerateDASOCs);
                break;
            case "wkf":
                generationResult = WKFGen.genByManager(useremail, status, filename, mediaFolder, verifyUri);
                break;
            default:
                String errorMsg = "[ERROR] IngestionAPI.mtGenByStatus() invalid elementtype=[" + elementtype + "]";
                System.out.println(errorMsg);
                return ok(ApiUtil.createResponse(errorMsg,false));
        }

        boolean isFailed = generationResult != null &&
                (generationResult.toLowerCase().contains("error") || generationResult.toLowerCase().contains("failure"));
        if (isFailed) {
            return ok(ApiUtil.createResponse(generationResult, false));
        }

        if (generationResult == null || generationResult.isEmpty() || generationResult.equals("SUCCESS") || generationResult.equals("null")) {
            generationResult = filename;
        }

        return ok(ApiUtil.createResponse(generationResult, true));
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

        String lowerName = file.getName().toLowerCase();
        String mimeType;
        if (lowerName.endsWith(".zip")) {
            mimeType = "application/zip";
        } else if (lowerName.endsWith(".csv")) {
            mimeType = "text/csv";
        } else {
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }

        // Serve file for download
        return ok(file)
            .as(mimeType)
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

    /**
     * Uningest DASOC (Data Acquisition - Study Object Collection)
     * Removes all triples that were added by DASOC ingestion from the named graph
    *
     * @param daUri URI of the DataAcquisition containing the DASOC data
     * @return Result indicating success/failure
     */
    public Result uningestDASOC(String daUri) {
        System.out.println(" ");
        System.out.println(" ");
        System.out.println("== UNINGEST DASOC =========================================================== ");
        System.out.println("IngestionAPI.uningestDASOC() with daUri = " + daUri);

        // Validate inputs
        if (daUri == null || daUri.isEmpty()) {
            String errorMsg = "[ERROR] IngestionAPI.uningestDASOC(): No DataAcquisition URI has been provided.";
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg, false));
        }

        // Find DA
        DA da = DA.find(daUri);
        if (da == null) {
            String errorMsg = "[ERROR] IngestionAPI.uningestDASOC(): Unable to retrieve DA with URI = " + daUri;
            System.out.println(errorMsg);
            return ok(ApiUtil.createResponse(errorMsg, false));
        }

        // Get associated DataFile
        DataFile dataFile = null;
        if (da.getHasDataFileUri() != null && !da.getHasDataFileUri().isEmpty()) {
            dataFile = DataFile.find(da.getHasDataFileUri());
        }

        // Delete all triples from the named graph (DA URI)
        // DASOC stores all its triples in a named graph using the DA URI
        try {
            String queryString = "";
            queryString += NameSpaces.getInstance().printSparqlNameSpaceList();
            queryString += "WITH <" + daUri + "> ";
            queryString += "DELETE { ?s ?p ?o } WHERE { ?s ?p ?o . } ";

            System.out.println("IngestionAPI.uningestDASOC(): Deleting all triples from named graph <" + daUri + ">");

            UpdateRequest req = UpdateFactory.create(queryString);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(req,
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE));
            processor.execute();

            System.out.println("IngestionAPI.uningestDASOC(): Successfully deleted triples from named graph");

            // Update DataFile status if it exists
            if (dataFile != null) {
                dataFile.setFileStatus(DataFile.UNPROCESSED);
                dataFile.getLogger().println("DASOC data uningested successfully from DA: " + daUri);
                dataFile.save();
                System.out.println("IngestionAPI.uningestDASOC(): DataFile status reset to UNPROCESSED");
            }

            String msg = "IngestionAPI.uningestDASOC(): Successfully uningested DASOC data from DA URI " + daUri;
            System.out.println(msg);
            return ok(ApiUtil.createResponse(msg, true));

        } catch (Exception e) {
            String errorMsg = "[ERROR] IngestionAPI.uningestDASOC(): Exception while deleting named graph: " + e.getMessage();
            System.err.println(errorMsg);
            e.printStackTrace();

            if (dataFile != null) {
                dataFile.getLogger().printException("Failed to uningest DASOC: " + e.getMessage());
                try {
                    dataFile.save();
                } catch (Exception saveEx) {
                    System.err.println("[ERROR] Could not save DataFile after uningest failure: " + saveEx.getMessage());
                }
            }

            return ok(ApiUtil.createResponse(errorMsg, false));
        }
    }

    /**
     * Ingest DASOC (Data Acquisition - Study Object Collection) CSV file
     * This endpoint adds properties to existing Study Objects in a SOC
     *
     * @param daUri URI of the DataAcquisition being created
     * @param socUri URI of the StudyObjectCollection containing the objects
     * @param request HTTP request containing the CSV file
     * @return Result indicating success/failure
     */
    public Result ingestDASOC(String daUri, String socUri, Http.Request request) {
        System.out.println(" ");
        System.out.println(" ");
        System.out.println("== NEW DASOC =========================================================== ");
        System.out.println("IngestionAPI.ingestDASOC() with daUri = " + daUri);
        System.out.println("IngestionAPI.ingestDASOC() with socUri = " + socUri);
        System.out.println("Request content-type: " + request.contentType().orElse("not specified"));
        System.out.println("Request has body: " + request.hasBody());

        // Validate inputs
        if (daUri == null || daUri.isEmpty()) {
            return ok(ApiUtil.createResponse("DataAcquisition URI is required for DASOC ingestion", false));
        }

        if (socUri == null || socUri.isEmpty()) {
            return ok(ApiUtil.createResponse("StudyObjectCollection URI is required for DASOC ingestion", false));
        }

        // Get DA and retrieve its associated DataFile
        DA da = DA.find(daUri);
        if (da == null) {
            return ok(ApiUtil.createResponse(
                "IngestionAPI.ingestDASOC(): File FAILED to be ingested: could not retrieve DA.", 

                false));
        }

        DataFile dataFile = DataFile.find(da.getHasDataFileUri());
        if (dataFile == null) {
            return ok(ApiUtil.createResponse(
                "IngestionAPI.ingestDASOC(): File FAILED to be ingested: could not retrieve DataFile.",
                false));
        }

        System.out.println("IngestionAPI.ingestDASOC(): DataFile retrieved - URI: " + dataFile.getUri() + 
            ", Filename: " + dataFile.getFilename());

        // Get file from request body or file system
        File file = request.body().asRaw().asFile();
        if (file == null) {
            // Try to get from file system
            String basePath = config.getString("hascoapi.paths.ingestion");

            if (basePath != null && !basePath.trim().isEmpty() && 
                dataFile.getFilename() != null && !dataFile.getFilename().isEmpty()) {
                
                String uriTerm = org.hascoapi.utils.URIUtils.uriLastSegment(dataFile.getUri());
                Path uploadedFilePath = Paths.get(basePath, Constants.RESOURCE_FOLDER, uriTerm, dataFile.getFilename());
                file = uploadedFilePath.toFile();
                
                if (!file.exists()) {
                    return ok(ApiUtil.createResponse(
                        "No file provided in request and uploaded file not found at: " + uploadedFilePath, 

                        false));
                }
            } else {
                return ok(ApiUtil.createResponse("No file has been provided for DASOC ingestion.", false));
            }
        }

        System.out.println("IngestionAPI.ingestDASOC(): File retrieved, preparing for ingestion");

        // Set status in DataFile
        dataFile.setLastProcessTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
        dataFile.setFileStatus(DataFile.WORKING);
        dataFile.getLogger().resetLog();
        dataFile.save();

        // Run DASOC ingestion asynchronously
        final DataFile finalDataFile = dataFile;
        final File finalFile = file;
        final String finalDaUri = daUri;
        final String finalSocUri = socUri;

        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("IngestionAPI.ingestDASOC(): Starting asynchronous DASOC ingestion");
                org.hascoapi.ingestion.AnnotateDASOC.IngestionResult result = 
                    org.hascoapi.ingestion.AnnotateDASOC.exec(finalDataFile, finalFile, finalDaUri, finalSocUri);
                
                System.out.println("IngestionAPI.ingestDASOC(): " + result.toString());
                
                if (result.isSuccess()) {
                    finalDataFile.setFileStatus(DataFile.PROCESSED);
                    finalDataFile.getLogger().println(
                        String.format("✅ DASOC ingestion completed successfully: %d rows processed", 
                            result.getRowCount()));
                } else {
                    finalDataFile.setFileStatus(DataFile.ERROR);
                    finalDataFile.getLogger().printException(
                        String.format("❌ DASOC ingestion failed: %s", result.getErrorMessage()));
                }
                
                finalDataFile.save();
                
            } catch (Exception e) {
                System.err.println("IngestionAPI.ingestDASOC(): Exception during ingestion: " + e.getMessage());
                e.printStackTrace();
                finalDataFile.setFileStatus(DataFile.ERROR);
                finalDataFile.getLogger().printException("DASOC ingestion exception: " + e.getMessage());
                try {
                    finalDataFile.save();
                } catch (Exception saveEx) {
                    System.err.println("Failed to save DataFile after error: " + saveEx.getMessage());
                }
            }
        });

        System.out.println("IngestionAPI.ingestDASOC(): DASOC ingestion submitted asynchronously");
        return ok(ApiUtil.createResponse(
            "DASOC file submitted for ingestion. Check file's log for ingestion status.", 
            true));
    }

}


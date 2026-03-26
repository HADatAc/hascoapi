package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.GenericInstance;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.zip.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static play.mvc.Results.internalServerError;
import static play.mvc.Results.ok;

public class DataFileAPI extends Controller {

    private static final Logger logger = Logger.getLogger(DataFileAPI.class.getName());

    /**
     * Returns JSON response with a list of DataFiles.
     */
    public static Result getDataFiles(List<DataFile> results) {
        if (results == null || results.isEmpty()) {
            return ok(ApiUtil.createResponse("No data file has been found", false));
        }

        ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL, HASCO.DATAFILE);
        JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
        return ok(ApiUtil.createResponse(jsonObject, true));
    }

    /**
     * Handles file upload and saves it permanently.
     */
    public Result uploadFile(String elementUri, String filename, Http.Request request) {
        System.out.println("\n=== DataFileAPI.uploadFile() START ===");
        System.out.println("[INFO] DataFileAPI.uploadFile() called with:");
        System.out.println("  elementUri: " + elementUri);
        System.out.println("  filename: " + filename);
        System.out.println("  Request content-type: " + request.contentType().orElse("not specified"));
        System.out.println("  Request has body: " + request.hasBody());
        System.out.println("  Request method: " + request.method());
        System.out.println("  Request path: " + request.path());
        System.out.println("  Request headers:");
        request.getHeaders().toMap().forEach((key, values) -> {
            System.out.println("    " + key + ": " + String.join(", ", values));
        });

        if (elementUri == null || elementUri.trim().isEmpty()) {
            System.out.println("[ERROR] DataFileAPI.uploadFile(): No elementUri provided");
            return ok(ApiUtil.createResponse("[ERROR] DataFileAPI.uploadFile(): No elementUri value has been provided.", false));
        }
    
        if (filename == null || filename.trim().isEmpty()) {
            System.out.println("[ERROR] DataFileAPI.uploadFile(): No filename provided");
            return ok(ApiUtil.createResponse("[ERROR] DataFileAPI.uploadFile(): No value for filename has been provided.", false));
        }
    
        // Try to find typed instance dynamically using reflection
        // This allows us to get the correct hasDataFileUri from MetadataTemplate subclasses
        String dataFileUri = null;
        Object typedInstance = null;

        // First, get a generic instance to check the hascoType
        GenericInstance genericCheck = GenericInstance.find(elementUri);
        if (genericCheck == null) {
            System.out.println("[ERROR] DataFileAPI.uploadFile(): No instance found for uri [" + elementUri + "]");
            return ok(ApiUtil.createResponse("[ERROR] DataFileAPI.uploadFile(): No instance found for uri [" + elementUri + "]", false));
        }

        String hascoTypeUri = genericCheck.getHascoTypeUri();

        // If the provided elementUri already identifies a DataFile, we can use it directly.
        // This is the common case for the Drupal frontend, which calls uploadFile(DFL_URI, filename).
        if (hascoTypeUri != null && hascoTypeUri.contains("DataFile")) {
            dataFileUri = elementUri;
            System.out.println("[INFO] DataFileAPI.uploadFile(): Element is a DataFile; using elementUri as dataFileUri: " + dataFileUri);
        }

        // Extract the type name from hascoTypeUri (e.g., "WKF" from "http://hadatac.org/ont/hasco/WKF")
        if (hascoTypeUri != null && !hascoTypeUri.trim().isEmpty()) {
            String typeName = URIUtils.uriLastSegment(hascoTypeUri);

            // Try to load the class dynamically
            try {
                String className = "org.hascoapi.entity.pojo." + typeName;
                Class<?> typeClass = Class.forName(className);

                // Try to call the static find method
                java.lang.reflect.Method findMethod = typeClass.getMethod("find", String.class);
                typedInstance = findMethod.invoke(null, elementUri);

                if (typedInstance != null) {
                    System.out.println("[INFO] DataFileAPI.uploadFile(): ✅ Found typed instance: " + typeName);

                    // Try to get hasDataFileUri from the typed instance
                    try {
                        java.lang.reflect.Method getDataFileUriMethod = typeClass.getMethod("getHasDataFileUri");
                        Object dataFileUriObj = getDataFileUriMethod.invoke(typedInstance);
                        if (dataFileUriObj != null && !dataFileUriObj.toString().trim().isEmpty()) {
                            dataFileUri = dataFileUriObj.toString();
                            System.out.println("[INFO] DataFileAPI.uploadFile(): ✅ Got DataFile URI from typed instance: " + dataFileUri);
                        }
                    } catch (NoSuchMethodException e) {
                        // Element doesn't have getHasDataFileUri() method
                    }
                }
            } catch (ClassNotFoundException e) {
                // Class not found for type, will use GenericInstance
            } catch (Exception e) {
                System.out.println("[WARN] DataFileAPI.uploadFile(): Error finding typed instance: " + e.getMessage());
            }
        }

        // Use genericCheck as fallback
        GenericInstance instance = (typedInstance == null) ? genericCheck : null;
        if (instance != null) {
            System.out.println("[INFO] DataFileAPI.uploadFile(): Using GenericInstance as fallback");
        }

        // Try multiple ways to extract the file from the request
        File tempFile = null;

        // Try 1: asRaw() - for direct binary uploads
        if (request.body() != null && request.body().asRaw() != null) {
            tempFile = request.body().asRaw().asFile();
        }

        // Try 2: asMultipartFormData() - for form-based uploads
        if (tempFile == null && request.body() != null && request.body().asMultipartFormData() != null) {
            play.mvc.Http.MultipartFormData<?> multipart = request.body().asMultipartFormData();
            play.mvc.Http.MultipartFormData.FilePart<?> filePart = multipart.getFile("file");

            if (filePart != null) {
                Object fileObj = filePart.getRef();
                if (fileObj instanceof File) {
                    tempFile = (File) fileObj;
                } else if (fileObj instanceof play.api.libs.Files.TemporaryFile) {
                    play.api.libs.Files.TemporaryFile scalaTemp = (play.api.libs.Files.TemporaryFile) fileObj;
                    tempFile = scalaTemp.path().toFile();
                }
            }
        }

        // Try 3: asBytes() - for byte array uploads
        if (tempFile == null && request.body() != null && request.body().asBytes() != null) {
            akka.util.ByteString bytes = request.body().asBytes();
            if (bytes != null && !bytes.isEmpty()) {
                try {
                    Path tempPath = Files.createTempFile("upload-", "-" + filename);
                    Files.write(tempPath, bytes.toArray());
                    tempFile = tempPath.toFile();
                } catch (IOException e) {
                    System.out.println("[ERROR] DataFileAPI.uploadFile(): Failed to create temp file from bytes: " + e.getMessage());
                }
            }
        }

        if (tempFile == null || !tempFile.exists() || tempFile.length() == 0) {
            System.out.println("[ERROR] DataFileAPI.uploadFile(): No valid file in request body");
            System.out.println("[ERROR]   tempFile: " + tempFile);
            System.out.println("[ERROR]   hasBody: " + request.hasBody());
            System.out.println("[ERROR]   content-type: " + request.contentType().orElse("not set"));
            return ok(ApiUtil.createResponse("[ERROR] DataFileAPI.uploadFile(): No file has been provided for ingestion.", false));
        }

        System.out.println("[SUCCESS] DataFileAPI.uploadFile(): File extracted successfully - " + tempFile.getAbsolutePath() + " (" + tempFile.length() + " bytes)");

        // Get DataFile URI - CRITICAL: Must use DataFile URI, not element URI!
        // Already obtained above if typed instance found, otherwise try reflection

        if (dataFileUri == null || dataFileUri.trim().isEmpty()) {
            // Try to get from GenericInstance using reflection
            if (instance != null) {
                // First, check if element has a DataFile association via getHasDataFileUri method
                try {
                    java.lang.reflect.Method getDataFileMethod = instance.getClass().getMethod("getHasDataFileUri");
                    Object dataFileUriObj = getDataFileMethod.invoke(instance);
                    if (dataFileUriObj != null && !dataFileUriObj.toString().trim().isEmpty()) {
                        dataFileUri = dataFileUriObj.toString();
                        System.out.println("[INFO] DataFileAPI.uploadFile(): Got DataFile URI from element.getHasDataFileUri(): " + dataFileUri);
                    }
                } catch (NoSuchMethodException e) {
                    // Element doesn't have getHasDataFileUri() method
                } catch (Exception e) {
                    System.out.println("[WARN] DataFileAPI.uploadFile(): Error calling getHasDataFileUri(): " + e.getMessage());
                }

                // If not found, check if element has hasDataFile property
                if (dataFileUri == null || dataFileUri.trim().isEmpty()) {
                    try {
                        java.lang.reflect.Method getDataFileMethod = instance.getClass().getMethod("getHasDataFile");
                        Object dataFileObj = getDataFileMethod.invoke(instance);
                        if (dataFileObj != null) {
                            // It's a DataFile object, get its URI
                            java.lang.reflect.Method getUriMethod = dataFileObj.getClass().getMethod("getUri");
                            Object uriObj = getUriMethod.invoke(dataFileObj);
                            if (uriObj != null && !uriObj.toString().trim().isEmpty()) {
                                dataFileUri = uriObj.toString();
                                System.out.println("[INFO] DataFileAPI.uploadFile(): Got DataFile URI from element.getHasDataFile().getUri(): " + dataFileUri);
                            }
                        }
                    } catch (NoSuchMethodException e) {
                        // Element doesn't have getHasDataFile() method
                    } catch (Exception e) {
                        System.out.println("[WARN] DataFileAPI.uploadFile(): Error calling getHasDataFile(): " + e.getMessage());
                    }
                }

                // If still not found, check the hascoType to determine if this IS a DataFile
                if (dataFileUri == null || dataFileUri.trim().isEmpty()) {
                    String hascoType = instance.getHascoTypeUri();

                    if (hascoType != null && hascoType.contains("DataFile")) {
                        // This element IS a DataFile
                        dataFileUri = elementUri;
                        System.out.println("[INFO] DataFileAPI.uploadFile(): Element IS a DataFile, using elementUri: " + dataFileUri);
                    }
                }
            }
        } else {
            System.out.println("[INFO] DataFileAPI.uploadFile(): ✅ DataFile URI already obtained from typed instance: " + dataFileUri);
        }

        if (dataFileUri == null || dataFileUri.trim().isEmpty()) {
            System.out.println("[ERROR] DataFileAPI.uploadFile(): Could not determine DataFile URI");
            System.out.println("[ERROR]   Element URI: " + elementUri);
            System.out.println("[ERROR]   Element hascoType: " + instance.getHascoTypeUri());
            return ok(ApiUtil.createResponse("[ERROR] DataFileAPI.uploadFile(): Could not determine DataFile URI from element.", false));
        }

        System.out.println("[INFO] DataFileAPI.uploadFile(): ✅ Using DataFile URI: " + dataFileUri);

        // CRITICAL: Force DFL prefix - convert WKF/INS/etc to DFL
        String uriSegment = URIUtils.uriLastSegment(dataFileUri);
        if (uriSegment == null || uriSegment.isEmpty()) {
            System.out.println("[ERROR] DataFileAPI.uploadFile(): Could not extract URI segment from: " + dataFileUri);
            return ok(ApiUtil.createResponse("[ERROR] DataFileAPI.uploadFile(): Invalid DataFile URI.", false));
        }

        String uriTerm = uriSegment;

        // If URI doesn't start with DFL, FORCE it to be DFL
        if (!uriTerm.startsWith("DFL")) {
            System.out.println("[WARN] DataFileAPI.uploadFile(): URI segment doesn't start with DFL: " + uriTerm);

            // Extract the numeric/timestamp part and force DFL prefix
            String numericPart = uriTerm.replaceFirst("^[A-Z]+", ""); // Remove prefix (WKF, INS, etc)
            uriTerm = "DFL" + numericPart;

            System.out.println("[FIX] DataFileAPI.uploadFile(): ✅ Forced DFL prefix: " + uriTerm);

            // Update dataFileUri to match
            String baseUri = dataFileUri.substring(0, dataFileUri.lastIndexOf('/') + 1);
            dataFileUri = baseUri + uriTerm;
            System.out.println("[FIX] DataFileAPI.uploadFile(): ✅ Updated DataFile URI: " + dataFileUri);
        }

        String basePath = ConfigProp.getPathIngestion();
        if (basePath == null || basePath.trim().isEmpty()) {
            System.out.println("[ERROR] DataFileAPI.uploadFile(): Invalid file storage path from ConfigProp.getPathIngestion()");
            return ok(ApiUtil.createResponse("[ERROR] DataFileAPI.uploadFile(): Invalid file storage path.", false));
        }
        Path destinationDir = Paths.get(basePath, Constants.RESOURCE_FOLDER, uriTerm);
    
        // Generate the permanent file path
        Path permanentPath = destinationDir.resolve(filename);
        
        System.out.println("DataFileAPI.uploadFile(): Will save to: " + permanentPath);
        System.out.println("DataFileAPI.uploadFile(): destinationDir: " + destinationDir);
        System.out.println("DataFileAPI.uploadFile(): uriTerm: " + uriTerm);

        // Save file SYNCHRONOUSLY to ensure it's available immediately
        // This prevents race conditions where ingest() is called before the file is saved
        try {
            System.out.println("[INFO] DataFileAPI.uploadFile(): Saving file synchronously...");

            // Create directories
            Files.createDirectories(destinationDir);
            System.out.println("[INFO] DataFileAPI.uploadFile(): Directories created: " + destinationDir);

            // Copy file
            Files.copy(tempFile.toPath(), permanentPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[SUCCESS] DataFileAPI.uploadFile(): File saved to: " + permanentPath);
            System.out.println("[SUCCESS] DataFileAPI.uploadFile(): File exists: " + Files.exists(permanentPath));
            System.out.println("[SUCCESS] DataFileAPI.uploadFile(): File size: " + Files.size(permanentPath) + " bytes");

            // Only delete temp file if it's NOT in the resources folder (to avoid deleting what we just saved)
            if (tempFile.exists() && !tempFile.getAbsolutePath().contains("resources")) {
                if (tempFile.delete()) {
                    System.out.println("[INFO] DataFileAPI.uploadFile(): Temp file deleted: " + tempFile.getAbsolutePath());
                } else {
                    System.out.println("[WARN] DataFileAPI.uploadFile(): Failed to delete temp file: " + tempFile.getAbsolutePath());
                }
            }

        } catch (IOException e) {
            System.out.println("[ERROR] DataFileAPI.uploadFile(): Failed to save file: " + e.getMessage());
            e.printStackTrace();
            return internalServerError(ApiUtil.createResponse("[ERROR] Failed to save file: " + e.getMessage(), false));
        }

        System.out.println("=== DataFileAPI.uploadFile() END (file saved successfully) ===");

        return ok(ApiUtil.createResponse("File uploaded and saved successfully.", true));
    }

    /**
     * Handles media upload and saves it permanently.
     */
    public Result uploadMedia(String foldername, String filename, Http.Request request) {
        if (foldername == null || foldername.trim().isEmpty()) {
            return ok(ApiUtil.createResponse("[ERROR] DataFileAPI.uploadMedia(): No foldername value has been provided.", false));
        }
    
        if (filename == null || filename.trim().isEmpty()) {
            return ok(ApiUtil.createResponse("[ERROR] DataFileAPI.uploadMedia(): No value for filename has been provided.", false));
        }
    
        File tempFile = request.body().asRaw().asFile();
        if (tempFile == null) {
            return ok(ApiUtil.createResponse("[ERROR] DataFileAPI.uploadMedia(): No media has been provided for ingestion.", false));
        }
    
        String basePath = ConfigProp.getPathIngestion();
        if (basePath == null || basePath.trim().isEmpty()) {
            System.out.println("[ERROR] DataFileAPI.uploadMedia(): Invalid file storage path from ConfigProp.getPathIngestion()");
            return internalServerError(ApiUtil.createResponse("[ERROR] DataFileAPI.uploadMedia(): Invalid file storage path.", false));
        }
    
        Path destinationDir = Paths.get(basePath, Constants.MEDIA_FOLDER, foldername);
    
        // Generate the permanent file path
        //Path permanentPath = destinationDir.resolve(filename);
    
        // Save file asynchronously to avoid blocking request handling
        CompletableFuture.runAsync(() -> unzipAndSave(tempFile, destinationDir));
    
        return ok(ApiUtil.createResponse("File upload in progress. It will be saved shortly.", true));
    }

    /**
     * Handles file download for a given URI.
     */
    public Result downloadFile(String elementUri, String filename) {
        if (elementUri == null || elementUri.trim().isEmpty()) {
            return badRequest(ApiUtil.createResponse("[ERROR] DataFileAPI.downloadFile(): No elementUri value provided.", false));
        }
    
        if (filename == null || filename.trim().isEmpty()) {
            return badRequest(ApiUtil.createResponse("[ERROR] DataFileAPI.downloadFile(): No filename provided.", false));
        }
    
        String basePath = ConfigProp.getPathIngestion();
        if (basePath == null || basePath.trim().isEmpty()) {
            System.out.println("[ERROR] DataFileAPI.downloadFile(): Invalid file storage path from ConfigProp.getPathIngestion()");
            return internalServerError(ApiUtil.createResponse("[ERROR] DataFileAPI.downloadFile(): Invalid file storage path.", false));
        }
    
        // Extract the last segment of the elementUri to determine the folder name
        String uriTerm = URIUtils.uriLastSegment(elementUri);
        Path filePath = Paths.get(basePath, Constants.RESOURCE_FOLDER, uriTerm, filename);
        File file = filePath.toFile();
    
        if (!file.exists() || !file.isFile()) {
            System.out.println("[ERROR] DataFileAPI.downloadFile(): File not found - " + filePath);
            return notFound(ApiUtil.createResponse("[ERROR] DataFileAPI.downloadFile(): File not found.", false));
        }
    
        // Serve the file as a response
        return ok(file)
            .as("application/octet-stream") // Generic MIME type for binary file downloads
            .withHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
    }
    
    /**
     * Saves a file to a permanent location and handles errors.
     */
    public static void saveFile(File tempFile, Path permanentPath) {
        System.out.println("[DEBUG] DataFileAPI.saveFile() START");
        System.out.println("[DEBUG] tempFile: " + (tempFile != null ? tempFile.getAbsolutePath() : "null"));
        System.out.println("[DEBUG] tempFile exists: " + (tempFile != null && tempFile.exists()));
        System.out.println("[DEBUG] tempFile size: " + (tempFile != null && tempFile.exists() ? tempFile.length() : "N/A"));
        System.out.println("[DEBUG] permanentPath: " + permanentPath);

        try {
            if (tempFile == null || !tempFile.exists()) {
                System.out.println("[ERROR] DataFileAPI.saveFile(): tempFile is null or doesn't exist!");
                return;
            }

            System.out.println("[DEBUG] Creating directories: " + permanentPath.getParent());
            Files.createDirectories(permanentPath.getParent());

            System.out.println("[DEBUG] Copying file from " + tempFile.toPath() + " to " + permanentPath);
            Files.copy(tempFile.toPath(), permanentPath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("[SUCCESS] File successfully saved to: " + permanentPath);
            System.out.println("[SUCCESS] File exists: " + Files.exists(permanentPath));
            System.out.println("[SUCCESS] File size: " + Files.size(permanentPath));

        } catch (IOException e) {
            System.out.println("[ERROR] DataFileAPI.saveFile(): Error saving file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    System.out.println("[WARN] DataFileAPI.saveFile(): Failed to delete temporary file: " + tempFile.getAbsolutePath());
                } else {
                    System.out.println("[INFO] DataFileAPI.saveFile(): Temporary file deleted: " + tempFile.getAbsolutePath());
                }
            }
        }
        System.out.println("[DEBUG] DataFileAPI.saveFile() END");
    }

    /**
     * Extracts a zip file and saves its contents permanently.
     */
    public void unzipAndSave(File zipFile, Path destinationDir) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    System.out.println("Skipping directory: " + entry.getName());
                    continue; // Skip directories
                }
                Path filePath = destinationDir.resolve(entry.getName());
                Files.createDirectories(filePath.getParent());
                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile()))) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = zis.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }
                }
                zis.closeEntry();
                System.out.println("Extracted: " + filePath);
            }
            System.out.println("Extraction complete.");
        } catch (IOException e) {
            System.out.println("Error extracting zip file: " + e.getMessage());
        } finally {
            if (zipFile.exists() && !zipFile.delete()) {
                System.out.println("Failed to delete zip file: " + zipFile.getAbsolutePath());
            }
        }
    }

}

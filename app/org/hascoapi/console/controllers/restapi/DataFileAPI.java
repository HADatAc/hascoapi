package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.GenericInstance;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.HAScOMapper;
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
        if (elementUri == null || elementUri.trim().isEmpty()) {
            return ok(ApiUtil.createResponse("[ERROR] DataFileAPI.uploadFile(): No elementUri value has been provided.", false));
        }
    
        if (filename == null || filename.trim().isEmpty()) {
            return ok(ApiUtil.createResponse("[ERROR] DataFileAPI.uploadFile(): No value for filename has been provided.", false));
        }
    
        GenericInstance instance = GenericInstance.find(elementUri);
        if (instance == null) {
            System.out.println("[ERROR] DataFileAPI.uploadFile(): No generic instance found for uri [" + elementUri + "]");
            return null;
        }

        File tempFile = request.body().asRaw().asFile();
        if (tempFile == null) {
            return ok(ApiUtil.createResponse("[ERROR] DataFileAPI.uploadFile(): No file has been provided for ingestion.", false));
        }
    
        String basePath = ConfigProp.getPathIngestion();
        if (basePath == null || basePath.trim().isEmpty()) {
            System.out.println("[ERROR] DataFileAPI.uploadFile(): Invalid file storage path from ConfigProp.getPathIngestion()");
            return internalServerError(ApiUtil.createResponse("[ERROR] DataFileAPI.uploadFile(): Invalid file storage path.", false));
        }
    
        String uriTerm = DataFileAPI.uriLastSegment(elementUri);
        Path destinationDir = Paths.get(basePath, Constants.RESOURCE_FOLDER, uriTerm);
    
        // Generate the permanent file path
        Path permanentPath = destinationDir.resolve(filename);
    
        // Save file asynchronously to avoid blocking request handling
        CompletableFuture.runAsync(() -> saveFile(tempFile, permanentPath));
    
        return ok(ApiUtil.createResponse("File upload in progress. It will be saved shortly.", true));
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
        Path permanentPath = destinationDir.resolve(filename);
    
        // Save file asynchronously to avoid blocking request handling
        CompletableFuture.runAsync(() -> unzipAndSave(tempFile, permanentPath));
    
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
        String uriTerm = uriLastSegment(elementUri);
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
    private void saveFile(File tempFile, Path permanentPath) {
        try {
            Files.createDirectories(permanentPath.getParent());
            Files.copy(tempFile.toPath(), permanentPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File successfully saved to: " + permanentPath);
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        } finally {
            if (tempFile.exists() && !tempFile.delete()) {
                System.out.println("Failed to delete temporary file: " + tempFile.getAbsolutePath());
            }
        }
    }

    /**
     * Extracts a zip file and saves its contents permanently.
     */
    public void unzipAndSave(File zipFile, Path destinationDir) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = destinationDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile()))) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            bos.write(buffer, 0, bytesRead);
                        }
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

    private static String uriLastSegment(String uri) {
        if (uri == null || uri.isEmpty()) {
            return uri;
        }
    
        // Remove trailing slashes or hashes
        uri = uri.replaceAll("[/#]+$", "");
    
        // Find the last occurrence of "/" or "#"
        int lastSlash = uri.lastIndexOf("/");
        int lastHash = uri.lastIndexOf("#");
    
        // Determine the actual last segment position
        int lastIndex = Math.max(lastSlash, lastHash);
    
        // If no delimiter is found, return unchanged
        if (lastIndex == -1) {
            return uri;
        }
    
        // Return the last segment
        return uri.substring(lastIndex + 1);
    }
    
    

}

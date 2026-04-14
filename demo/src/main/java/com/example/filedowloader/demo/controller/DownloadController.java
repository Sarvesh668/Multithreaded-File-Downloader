package com.example.filedowloader.demo.controller;

import com.example.filedowloader.demo.dto.DownloadRequest;
import com.example.filedowloader.demo.service.DownloadManagerService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

<<<<<<< HEAD
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
=======
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.example.filedowloader.demo.model.DownloadTask;

>>>>>>> 9b29fcd (feat: improve download flow and secure local config)

@RestController
@RequestMapping("/api/downloads")
// @CrossOrigin is handled by your CorsConfig, but keeping it here doesn't hurt
@CrossOrigin(origins = "http://localhost:3000")
public class DownloadController {

     private static final Logger log = LoggerFactory.getLogger(DownloadController.class);
    private final DownloadManagerService downloadService;

    public DownloadController(DownloadManagerService downloadService) {
        this.downloadService = downloadService;
    }

    @GetMapping("/ping")
<<<<<<< HEAD
    public String ping() {
        return "Server is alive";
=======
public String ping(){
     System.out.println("Ping endpoint hit");
    return "Server is alive";
}


    @PostMapping(produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<ByteArrayResource> newDownloadRequest(@RequestBody DownloadRequest request) throws IOException, InterruptedException {
        DownloadTask task = downloadService.startDownload(request.getUrl(), request);
        
        // Wait for download and compression to complete
        boolean completed = waitForTaskCompletion(task.getId(), 120000); // 2 minute timeout
        
        if (!completed) {
            return ResponseEntity.status(503).build(); // Service Unavailable
        }
        
        // Read the file (either compressed or original)
        String fileName = task.getOriginalFileName();
        String filePath = null;
        String fileToDownload = null;
        
        // Try compressed file first
        String compressedFileName = fileName + "_compressed_" + task.getId() + ".pdf";
        String compressedPath = task.getSaveDirectory() + compressedFileName;
        
        if (Files.exists(Paths.get(compressedPath))) {
            filePath = compressedPath;
            fileToDownload = compressedFileName;
        } else {
            // Fall back to original file
            filePath = task.getSaveDirectory() + fileName;
            fileToDownload = fileName;
        }
        
        if (!Files.exists(Paths.get(filePath))) {
            return ResponseEntity.status(404).build(); // File not found
        }
        
        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
        ByteArrayResource resource = new ByteArrayResource(fileBytes);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileToDownload + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(fileBytes.length)
                .body(resource);
>>>>>>> 9b29fcd (feat: improve download flow and secure local config)
    }

    /**
     * This is the main endpoint. It now returns the actual PDF Resource.
     */
    /*@PostMapping
    public ResponseEntity<Resource> triggerDownload(@RequestBody DownloadRequest request) {
        try {
            // 1. Trigger the download and WAIT for the final compressed file path
            // We need to make sure startDownload returns a String (the path)
            String finalPath = downloadService.startDownload(request.getUrl(), request);

            Path path = Paths.get(finalPath);
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("File not found on disk: " + finalPath);
            }

            // 2. Return the actual file bytes with PDF headers
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    // This header tells the browser to download the file instead of just showing text
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                    .body(resource);

        } catch (Exception e) {
            System.err.println("Download failed: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }*/

    @GetMapping("/{taskId}")
    public ResponseEntity<String> checkStatus(@PathVariable Long taskId) {
        return ResponseEntity.ok("Status checked for task: " + taskId);
    }

    @PostMapping
public ResponseEntity<Resource> newDownloadRequest(@RequestBody DownloadRequest request) {
    try {
        // This returns the FULL PATH string from the service
        String finalPath = downloadService.startDownload(request.getUrl(), request);

        Path path = Paths.get(finalPath);
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                .body(resource);

    } catch (Exception e) {
        log.error("Controller Error: {}", e.getMessage());
        return ResponseEntity.internalServerError().build();
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<ByteArrayResource> downloadPdf(String url) throws IOException, InterruptedException {
        byte[] fileBytes = downloadFileFromUrl(url);
        
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        if (fileName.isEmpty()) {
            fileName = "download.pdf";
        }
        
        ByteArrayResource resource = new ByteArrayResource(fileBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(fileBytes.length)
                .body(resource);
    }

    private byte[] downloadFileFromUrl(String fileUrl) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fileUrl))
                .build();
        
        HttpResponse<byte[]> response = client.send(request, 
                HttpResponse.BodyHandlers.ofByteArray());
        
        if (response.statusCode() != 200) {
            throw new IOException("Failed to download file. Status code: " + response.statusCode());
        }
        
        return response.body();
    }

    private boolean waitForTaskCompletion(Long taskId, long timeoutMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            DownloadTask task = downloadService.getTaskById(taskId);
            
            if (task != null && task.getStatus() != null) {
                String status = task.getStatus().name();
                if ("COMPLETED".equals(status)) {
                    return true;
                }
                if ("FAILED".equals(status)) {
                    return false;
                }
            }
            
            Thread.sleep(100); // Poll every 100ms
        }
        
        return false; // Timeout reached
    }

    
}
}
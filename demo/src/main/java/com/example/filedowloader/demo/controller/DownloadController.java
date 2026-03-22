package com.example.filedowloader.demo.controller;

import com.example.filedowloader.demo.dto.DownloadRequest;
import com.example.filedowloader.demo.service.DownloadManagerService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public String ping() {
        return "Server is alive";
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
}
}
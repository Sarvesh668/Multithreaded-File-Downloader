package com.example.filedowloader.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.filedowloader.demo.dto.DownloadRequest;
import com.example.filedowloader.demo.service.DownloadManagerService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/downloads")
@CrossOrigin(origins = "http://localhost:3000")
public class DownloadController {
    
    private final DownloadManagerService downloadService;

    public DownloadController(DownloadManagerService downloadService){
        this.downloadService=downloadService;
    }

    @GetMapping("/ping")
public String ping(){
     System.out.println("Ping endpoint hit");
    return "Server is alive";
}


    //Response entity class is a class which represents the HTTP codes in springboot
    @PostMapping
    public ResponseEntity<String> newDownloadRequest(@RequestBody DownloadRequest request){
         downloadService.startDownload(request.getUrl(), request);
        return ResponseEntity.ok("Download request recieved for: "+request.getUrl());
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<String> checkStatus(@PathVariable Long taskId){
        return ResponseEntity.ok("Status checked for task:"+ taskId);
    }

    @PutMapping("/{taskId}/pause")
    public ResponseEntity<String> pauseDownload(@PathVariable Long taskId){
        return ResponseEntity.ok("Paused task:" +taskId);
    }

    @PutMapping("/{taskId}/cancel")
    public ResponseEntity<String> cancelDownload(@PathVariable Long taskId){
        return ResponseEntity.ok("Cancelled task: "+taskId);
    }
}

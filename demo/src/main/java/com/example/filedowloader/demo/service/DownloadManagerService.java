package com.example.filedowloader.demo.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.filedowloader.demo.dto.DownloadRequest;
import com.example.filedowloader.demo.event.DownloadCompleteEvent;
import com.example.filedowloader.demo.model.DownloadChunk;
import com.example.filedowloader.demo.model.DownloadTask;
import com.example.filedowloader.demo.model.Role;
import com.example.filedowloader.demo.model.User;
import com.example.filedowloader.demo.repository.ChunkRepository;
import com.example.filedowloader.demo.repository.DownloadRepository;
import com.example.filedowloader.demo.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class DownloadManagerService {

    private static final Logger log = LoggerFactory.getLogger(DownloadManagerService.class);

    private final DownloadRepository taskRepository;
    private final UserRepository userRepository;
    private final ChunkRepository chunkRepository;

    @Value("${application.file.save-directory}")
    private String baseSaveDirectory;

    private final ExecutorService downloadPool = Executors.newVirtualThreadPerTaskExecutor();
    private final ApplicationEventPublisher eventPublisher;

    public DownloadManagerService(
            DownloadRepository taskRepository,
            UserRepository userRepository,
            ChunkRepository chunkRepository,
            ApplicationEventPublisher eventPublisher) {

        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.chunkRepository = chunkRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public String startDownload(String fileUrl, DownloadRequest request) {

        log.info("DownloadManagerService triggered for URL: {}", fileUrl);

        try {
            // --- 1. PRE-DOWNLOAD CHECK ---
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest headRequest = HttpRequest.newBuilder()
                    .uri(URI.create(fileUrl))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response =
                    client.send(headRequest, HttpResponse.BodyHandlers.discarding());

            long totalBytes = response.headers()
                    .firstValueAsLong("Content-Length")
                    .orElse(0L);

            // --- 2. USER HANDLING ---
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User taskOwner;

            if ("MAX".equalsIgnoreCase(request.getTier())) {
                if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                    throw new RuntimeException("401 Unauthorized: MAX tier requires login.");
                }

                taskOwner = userRepository.findByUsername(auth.getName())
                        .orElseThrow(() -> new RuntimeException("Logged in user not found in DB"));
            } else {
                taskOwner = userRepository.findByUsername("guest_user")
                        .orElseGet(() -> {
                            User newGuest = new User("guest_user", "nopassword", Role.GUEST);
                            return userRepository.save(newGuest);
                        });
            }

            // --- 3. FILE NAME ---
            String originalFileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            if (originalFileName.isEmpty() || !originalFileName.contains(".")) {
                originalFileName = "download_" + System.currentTimeMillis() + ".pdf";
            }

            // --- 4. TASK CREATION ---
            DownloadTask task = new DownloadTask(
                    taskOwner,
                    originalFileName,
                    fileUrl,
                    baseSaveDirectory,
                    totalBytes,
                    LocalDateTime.now()
            );

            int numOfThreads = 4;
            long chunkSize = totalBytes / numOfThreads;

            for (int i = 0; i < numOfThreads; ++i) {
                long startByte = i * chunkSize;
                long endByte = startByte + chunkSize - 1;

                if (i == numOfThreads - 1) {
                    endByte = totalBytes - 1;
                }

                DownloadChunk chunk = new DownloadChunk(task, startByte, endByte);
                task.getChunks().add(chunk);
            }

            taskRepository.save(task);

            // --- 5. EXECUTION USING COMPLETABLE FUTURE ---
            List<CompletableFuture<Void>> futures = task.getChunks().stream()
                    .map(chunkyBoi -> {
                        FileWorkerService worker = new FileWorkerService(
                                chunkyBoi,
                                fileUrl,
                                baseSaveDirectory,
                                chunkRepository,
                                userRepository,
                                taskRepository,
                                eventPublisher
                        );

                        return CompletableFuture.runAsync(worker, downloadPool);
                    })
                    .collect(Collectors.toList());

            // WAIT for all chunks
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            eventPublisher.publishEvent(new DownloadCompleteEvent(task));

            log.info("All chunks downloaded for {}", originalFileName);

            // --- 6. RETURN FINAL PATH ---
            return baseSaveDirectory + originalFileName;

        } catch (Exception e) {
            log.error("Download failed", e);
            throw new RuntimeException(e.getMessage());
        }
    }
}
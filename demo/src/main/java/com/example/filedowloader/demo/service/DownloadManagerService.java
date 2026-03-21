package com.example.filedowloader.demo.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.tomcat.util.buf.UEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.RuntimeErrorException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import com.example.filedowloader.demo.dto.DownloadRequest;
import com.example.filedowloader.demo.model.DownloadChunk;
import com.example.filedowloader.demo.model.DownloadTask;
import com.example.filedowloader.demo.model.Role;
import com.example.filedowloader.demo.model.Status;
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

    private final ExecutorService downloadPool= Executors.newVirtualThreadPerTaskExecutor();

    private final ApplicationEventPublisher eventPublisher;

    public DownloadManagerService(DownloadRepository taskRepository, UserRepository userRepository, 
                                  ChunkRepository chunkRepository, ApplicationEventPublisher eventPublisher){
        this.taskRepository=taskRepository;
        this.userRepository= userRepository;
        this.chunkRepository= chunkRepository;
        this.eventPublisher=eventPublisher;
    }
    
    @Transactional
    public void startDownload(String fileUrl, DownloadRequest pls){

        // System.out.println("DownloadManagerService triggered!");
        //System.out.println("URL: " + fileUrl);
        log.info("DownloadManagerService triggered for URL: {}", fileUrl);
        try{
            HttpClient client= HttpClient.newHttpClient();

            HttpRequest request= HttpRequest.newBuilder()
                                .uri(URI.create(fileUrl))
                                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                                .build();

            //This line sends the request to ? and tells java to ignore the body is the server sends the file
            HttpResponse<Void> response= client.send(request, HttpResponse.BodyHandlers.discarding());

            long totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(0L);

            String contentType = response.headers().firstValue("Content-Type").orElse("unknown");
            System.out.println("Detected Content-Type: "+contentType);

            if(contentType.contains("text/html")){
                throw new RuntimeException("Invaid file type. Cannot download HTML webpages");
            }

            System.out.println("Target file size: " + totalBytes + " bytes");


             /*========================================================================= */

           /*User defaultUser= userRepository.findAll().stream().findFirst().orElseGet(()->{

                        System.out.println("Database is empty. Creating default user...");
                        User newUser= new User("Happy happy", "Itsokay", Role.REGISTERED);
                        return userRepository.save(newUser);
           });*/

           String currentUsername= SecurityContextHolder.getContext().getAuthentication().getName();

           User currentUser= userRepository.findByUsername(currentUsername)
                                           .orElseThrow(()-> new RuntimeException("User not found in database"));

           Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User taskOwner;

            // 2. Safely get the tier to prevent null pointer exceptions
            String requestedTier = pls.getTier() != null ? pls.getTier().toUpperCase() : "STANDARD";

            // 3. The Tier Bouncer
            if("MAX".equals(requestedTier)){
                 if(auth==null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")){
                     throw new RuntimeException("401 Unauthorized: Extreme compression requires a logged-in account.");
                 }

                 String currentUserName = auth.getName();
                 taskOwner = userRepository.findByUsername(currentUserName)
                                 .orElseThrow(() -> new RuntimeException("Authenticated User not found"));
            }
            else{
                 // It is STANDARD tier (Guest).
                 taskOwner = userRepository.findByUsername("guest_user").orElseGet(()->{
                     User newGuest = new User("guest_user", "nopassword", Role.GUEST);
                     return userRepository.save(newGuest);
                 });
            }

            String originalFileName= fileUrl.substring(fileUrl.lastIndexOf("/")+1);

            if(originalFileName.isEmpty() || !originalFileName.contains(".")){
                originalFileName= "download_"+System.currentTimeMillis()+".pdf";
            }

           DownloadTask task= new DownloadTask(
                    currentUser,
                    originalFileName,
                    fileUrl,
                    baseSaveDirectory,
                    totalBytes,
                    LocalDateTime.now()
           );
            

           int numOfThreads= 4;

           long chunkSize= totalBytes/numOfThreads;


           for(int i=0; i<numOfThreads; ++i){
                long startByte= i*chunkSize;
                long endByte= startByte+chunkSize-1;

                if(i==numOfThreads-1){
                    endByte= totalBytes-1;
                }

                //=============================================================
                DownloadChunk chunk = new DownloadChunk(task, startByte, endByte);
                chunk.setStatus(Status.PENDING);
                chunk.setDownloadedBytes(0L);
                task.getChunks().add(chunk);

                System.out.println("The start byte for thread "+i+" is: "+ startByte);
                System.out.println("The end byte for thread "+i+" is: "+ endByte);
           }
           taskRepository.save(task);
           System.out.println("Successfully saved Task and Chunks to the database!");
            for(DownloadChunk chunkyBoi: task.getChunks()){
                    FileWorkerService worker= new FileWorkerService(chunkyBoi, fileUrl, task.getSaveDirectory(),
                                                     chunkRepository, userRepository, taskRepository, eventPublisher);
                   // Thread thread= new Thread(worker);
                    //thread.start();
                    downloadPool.submit(worker);
                }

        }

        catch(Exception e){
           // System.err.println("Failed to fetch file metdata: "+e.getMessage());
           log.error("Failed to fetch file metadata", e);
        }
        finally{
           // System.out.println("VIRTUAL THREADS were used for the task");
            log.info("VIRTUAL THREADS were used for the task");
        }
    }
}

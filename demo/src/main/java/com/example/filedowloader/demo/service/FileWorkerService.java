package com.example.filedowloader.demo.service;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationEventPublisher;

import com.example.filedowloader.demo.event.DownloadCompleteEvent;
import com.example.filedowloader.demo.model.DownloadChunk;
import com.example.filedowloader.demo.model.DownloadTask;
import com.example.filedowloader.demo.model.Status;
import com.example.filedowloader.demo.repository.ChunkRepository;
import com.example.filedowloader.demo.repository.DownloadRepository;
import com.example.filedowloader.demo.repository.UserRepository;

public class FileWorkerService implements Runnable {
    
    private static final Logger log = LoggerFactory.getLogger(FileWorkerService.class);
    private final DownloadChunk chunk;
    private final String fileUrl;
    private final String saveDirectory;
    private final ChunkRepository chunkRepository;
    private final UserRepository userRepository;
    private final DownloadRepository taskRepository;
    private final ApplicationEventPublisher eventPublisher;

    public FileWorkerService(DownloadChunk chunk, String fileUrl, String saveDirectory, ChunkRepository chunkRepository,
                             UserRepository userRepository, DownloadRepository taskRepository,
                             ApplicationEventPublisher eventPublisher){
        this.chunk=chunk;
        this.fileUrl=fileUrl;
        this.saveDirectory=saveDirectory;
        this.chunkRepository=chunkRepository;
        this.userRepository= userRepository;
        this.taskRepository= taskRepository;
        this.eventPublisher= eventPublisher;
    }

    @Override
    public void run(){
        System.out.println("Worker started for Chunk ID: "+chunk.getId()+" [" + chunk.getStartBytes() + " to " + chunk.getEndBytes() + "]");

        try{
            HttpClient client= HttpClient.newHttpClient();

            String rangeHeader= "bytes= "+ chunk.getStartBytes()+ "-"+ chunk.getEndBytes();

            HttpRequest request= HttpRequest.newBuilder()
                        .uri(URI.create(fileUrl))
                        .header("Range", rangeHeader)
                        .GET()
                        .build();

            //The body is ignored for testing purposes
            HttpResponse<InputStream> response= client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            String dynamicFileName= chunk.getDownloadTask().getOriginalFileName();

            String fullFilePath= saveDirectory+dynamicFileName;

            try (InputStream inputStream= response.body();
                  RandomAccessFile file = new RandomAccessFile(fullFilePath, "rw")) {

                    file.seek(chunk.getStartBytes());

                    byte[] buffer= new byte[8192];
                    int bytesRead;

                    while((bytesRead= inputStream.read(buffer)) != -1){
                        file.write(buffer, 0, bytesRead);
                    }

                    System.out.println("Chunk " + chunk.getId() + " finished writing to disk!");

                    chunk.setStatus(Status.COMPLETED);

                  long bytesDownloaded= chunk.getEndBytes()-chunk.getStartBytes();
                  chunk.setDownloadedBytes(bytesDownloaded);
                  chunkRepository.save(chunk);
                  System.out.println("Chunk "+ chunk.getId()+" database status updated to COMPLETED");
            } 
            System.out.println("Chunk " + chunk.getId() + " received HTTP Status: " + response.statusCode());

            DownloadTask masterTask= chunk.getDownloadTask();

            synchronized(masterTask){
                boolean allDone= true;

                for(DownloadChunk c: masterTask.getChunks()){
                    DownloadChunk dbChunk= chunkRepository.findById(c.getId()).orElse(c);
                    if(dbChunk.getStatus() != Status.COMPLETED){
                        allDone=false;
                        break;
                    }
                }

                if(allDone && masterTask.getStatus() != Status.COMPLETED){
                    masterTask.setStatus(Status.COMPLETED);
                    masterTask.setDownloadedBytes(masterTask.getTotalBytes());
                    taskRepository.save(masterTask);
                   // System.out.println("🎉 ALL CHUNKS COMPLETE! Master Task " + masterTask.getId() + " is COMPLETED.");
                   log.info("ALL CHUNKS COMPLETE! Master Task {} is COMPLETED.", masterTask.getId());

                   eventPublisher.publishEvent(new DownloadCompleteEvent(masterTask));
                }
            }
        }
        catch(Exception e){
            System.err.println("Chunk " + chunk.getId() + " failed: " + e.getMessage());
        }
    }
}

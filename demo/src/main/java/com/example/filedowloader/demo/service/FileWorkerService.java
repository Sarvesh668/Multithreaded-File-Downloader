package com.example.filedowloader.demo.service;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import com.example.filedowloader.demo.dto.DownloadRequest;
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
    private final DownloadRequest request;

    public FileWorkerService(
            DownloadChunk chunk,
            String fileUrl,
            String saveDirectory,
            ChunkRepository chunkRepository,
            UserRepository userRepository,
            DownloadRepository taskRepository,
            ApplicationEventPublisher eventPublisher,
            DownloadRequest request) {

        this.chunk = chunk;
        this.fileUrl = fileUrl;
        this.saveDirectory = saveDirectory;
        this.chunkRepository = chunkRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.eventPublisher = eventPublisher;
        this.request = request;
    }

    @Override
    public void run() {
        log.info(
                "Worker started for Chunk ID: {} [{} to {}]",
                chunk.getId(),
                chunk.getStartBytes(),
                chunk.getEndBytes());

        try {
            HttpClient client = HttpClient.newHttpClient();

            String rangeHeader = "bytes=" + chunk.getStartBytes() + "-" + chunk.getEndBytes();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fileUrl))
                    .header("Range", rangeHeader)
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 206) {
                throw new RuntimeException(
                        "Expected HTTP 206 but got " + response.statusCode());
            }

            String dynamicFileName = chunk.getDownloadTask().getOriginalFileName();
            String fullFilePath = saveDirectory + dynamicFileName;

            long expectedBytes = chunk.getEndBytes() - chunk.getStartBytes() + 1;
            long actualBytes = 0;
            long currentPosition = chunk.getStartBytes();

            try (
                    InputStream inputStream = response.body();
                    RandomAccessFile raf = new RandomAccessFile(fullFilePath, "rw");
                    FileChannel channel = raf.getChannel()) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);

                    while (byteBuffer.hasRemaining()) {
                        int written = channel.write(byteBuffer, currentPosition);
                        currentPosition += written;
                    }

                    actualBytes += bytesRead;
                }

                channel.force(true);
            }

            if (actualBytes != expectedBytes) {
                throw new RuntimeException(
                        "Chunk " + chunk.getId()
                                + " incomplete. Expected "
                                + expectedBytes
                                + " bytes but downloaded "
                                + actualBytes);
            }

            chunk.setDownloadedBytes(actualBytes);
            chunk.setStatus(Status.COMPLETED);
            chunkRepository.save(chunk);

            log.info(
                    "Chunk {} completed successfully. Downloaded {} bytes.",
                    chunk.getId(),
                    actualBytes);

            DownloadTask masterTask = chunk.getDownloadTask();

            synchronized (masterTask) {
                boolean allDone = true;

                for (DownloadChunk c : masterTask.getChunks()) {
                    DownloadChunk dbChunk = chunkRepository.findById(c.getId()).orElse(c);

                    if (dbChunk.getStatus() != Status.COMPLETED) {
                        allDone = false;
                        break;
                    }
                }

                if (allDone && masterTask.getStatus() != Status.COMPLETED) {
                    masterTask.setStatus(Status.COMPLETED);
                    masterTask.setDownloadedBytes(masterTask.getTotalBytes());

                    taskRepository.save(masterTask);

                    log.info(
                            "ALL CHUNKS COMPLETE! Master Task {} is COMPLETED.",
                            masterTask.getId());

                    // Only trigger compression for buttons 2 and 3
                    if (this.request != null
                            && this.request.getTier() != null
                            && !"DOWNLOAD_ONLY".equalsIgnoreCase(this.request.getTier())) {

                        log.info(
                                "Triggering compression for task {} with tier {}",
                                masterTask.getId(),
                                this.request.getTier());

                        eventPublisher.publishEvent(new DownloadCompleteEvent(masterTask));
                    } else {
                        log.info(
                                "Download-only request detected. Compression skipped for task {}",
                                masterTask.getId());
                    }
                }
            }

        } catch (Exception e) {
            log.error(
                    "Chunk {} failed [{} - {}]: {}",
                    chunk.getId(),
                    chunk.getStartBytes(),
                    chunk.getEndBytes(),
                    e.getMessage(),
                    e);

            chunk.setStatus(Status.FAILED);
            chunkRepository.save(chunk);
        }
    }
}
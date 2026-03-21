package com.example.filedowloader.demo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import com.example.filedowloader.demo.model.Status;

@Entity
@Table(name="download_task")
public class DownloadTask {
    
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "task_seq"
    )
    @SequenceGenerator(
        name="task_seq",
        sequenceName = "task_seq",
        allocationSize= 1
    )
    private Long id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    private String originalFileName;
    private String fileUrl;
    private String saveDirectory;

    @Enumerated(EnumType.STRING)
    private Status status;
    private Long totalBytes;
    private Long downloadedBytes; 
    private LocalDateTime createdAt= LocalDateTime.now();

    // THIS IS THE ONE-TO-MANY MAPPING
    // cascade = CascadeType.ALL means if we delete the task, it deletes the chunks.
    // orphanRemoval = true cleans up disconnected chunks.
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DownloadChunk> chunks= new ArrayList<>();


    protected DownloadTask(){}

    public DownloadTask( User user, String originalFileName, String fileUrl, String saveDirectory, Long totalBytes, LocalDateTime createdAt){
        this.user=user;
        this.originalFileName=originalFileName;
        this.fileUrl=fileUrl;
        this.saveDirectory= saveDirectory;
        this.totalBytes=totalBytes;
        this.createdAt=createdAt;
    }

    public void setStatus(Status status){
        this.status= status;
    }

    //What is the use of this repetition for downloaded bytes it is also present in download chunk
    public void setDownloadedBytes(Long downloadedBytes){
        this.downloadedBytes=downloadedBytes;
    }

    public Long getId(){
        return id;
    }

    public User getUser(){
        return user;
    }

    public String getOriginalFileName(){
        return originalFileName;
    }

    public String getFileUrl(){
        return fileUrl;
    }

    public String getSaveDirectory(){
        return saveDirectory;
    }

    public Long getTotalBytes(){
        return totalBytes;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public List<DownloadChunk> getChunks() {
        return chunks;
    }

    public Status getStatus(){
        return status;
    }
}


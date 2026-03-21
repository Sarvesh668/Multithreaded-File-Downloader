package com.example.filedowloader.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name="downloadchunk")
public class DownloadChunk {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chunk_seq")
    @SequenceGenerator(name="chunk_seq", sequenceName = "chunk_seq", allocationSize = 1)
    private Long Id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="task_id") //This is the foreign key in my table
    private DownloadTask task;
    private Long startBytes;
    private Long endBytes;
    private Long downloadedBytes;

    @Enumerated(EnumType.STRING)
    private Status status;

    protected DownloadChunk(){}


     public void setDownloadedBytes(Long downloadedBytes){
        this.downloadedBytes=downloadedBytes;
    }

    public void setStatus(Status status){
        this.status=status;
    }

    public DownloadChunk(DownloadTask task, Long startBytes, Long endBytes){
        this.task= task;
        this.startBytes= startBytes;
        this.endBytes=endBytes;
    }

    public Long getId(){
        return Id;
    }

    public DownloadTask getDownloadTask(){
        return task;
    }

    public Long getStartBytes(){
        return startBytes;
    }

    public Long getEndBytes(){
        return endBytes;
    }

    public Status getStatus(){
        return status;
    }
}

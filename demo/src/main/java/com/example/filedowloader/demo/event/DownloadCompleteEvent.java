package com.example.filedowloader.demo.event;

import com.example.filedowloader.demo.model.DownloadTask;

public class DownloadCompleteEvent {

    private final DownloadTask task;

    public DownloadCompleteEvent(DownloadTask task){
        this.task=task;
    }

    public DownloadTask getTask(){
        return task;
    }
}

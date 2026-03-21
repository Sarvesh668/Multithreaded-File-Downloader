package com.example.filedowloader.demo.dto;

public class DownloadRequest {
    
    private String url;
    private String tier;
    /*Here a libary named Jackson will be used, it requires an empty constructor  */
    public DownloadRequest(){};

    public DownloadRequest(String url, String tier){
        this.url=url;
        this.tier= tier;
    }

    public String getUrl(){
        return url;
    }

    //This set method is for jackson
    public void setUrl(String url){
        this.url= url;
    }

    public String getTier(){
        return tier;
    }
}

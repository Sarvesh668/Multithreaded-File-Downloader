package com.example.filedowloader.demo.dto;

public class DownloadRequest {
    
    private String url;
    private String tier;
    
    /* Here a library named Jackson will be used, it requires an empty constructor */
    public DownloadRequest() {}

    public DownloadRequest(String url, String tier) {
        this.url = url;
        this.tier = tier;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTier() {
        return tier;
    }

    // THIS WAS MISSING: Jackson needs this to inject the tier!
    public void setTier(String tier) {
        this.tier = tier;
    }
}
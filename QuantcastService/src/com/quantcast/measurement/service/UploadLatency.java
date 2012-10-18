package com.quantcast.measurement.service;

class UploadLatency {

    private String uploadId;
    private long uploadTime;
    
    public UploadLatency(String uploadId, long uploadTime) {
        this.uploadId = uploadId;
        this.uploadTime = uploadTime;
    }

    public String getUploadId() {
        return uploadId;
    }

    public long getUploadTime() {
        return uploadTime;
    }
    
}

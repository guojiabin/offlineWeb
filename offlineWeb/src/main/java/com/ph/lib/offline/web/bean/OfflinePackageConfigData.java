package com.ph.lib.offline.web.bean;

public class OfflinePackageConfigData {
    private String packageId;
    private String version;
    private int status = -1;
    private boolean is_patch = false;
    private String downloadUrl;
    private String baseUrl;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isIs_patch() {
        return is_patch;
    }

    public void setIs_patch(boolean is_patch) {
        this.is_patch = is_patch;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}

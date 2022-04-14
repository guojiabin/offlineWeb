package com.ph.lib.offline.web.core;

import android.text.TextUtils;

/**
 * created by guojiabin on 2022/1/24
 * 离线包信息
 */
public class PackageInfo {
    /**
     * 离线包ID
     */
    private String packageId;
    /***
     * 离线包下载地址
     * */
    private String downloadUrl;
    /***
     *
     * 离线包版本号
     * */
    private String version;

    /***
     * 离线包的状态 {@link PackageStatus}
     * */
    private int status = PackageStatus.onLine;

    /**
     * 是否是patch包
     */
    private boolean is_patch;

    /**
     * 离线包md值 由后端下发
     */
    private String md5;

    private String baseUrl;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public int getDownFailCount() {
        return downFailCount;
    }

    public void setDownFailCount(int downFailCount) {
        this.downFailCount = downFailCount;
    }

    // 下载离线包失败次数 下载最多重新下载5次
    private int downFailCount;

    public String getPackageId() {
        return packageId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getVersion() {
        return version;
    }

    public int getStatus() {
        return status;
    }

    public boolean isPatch() {
        return is_patch;
    }

    public void setIsPatch(boolean isPatch){
        this.is_patch = isPatch;
    }

    public String getMd5() {
        return md5;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PackageInfo)) {
            return false;
        }
        PackageInfo that = (PackageInfo) obj;
        return TextUtils.equals(packageId, that.packageId);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = result * 37 + packageId == null ? 0 : packageId.hashCode();
        return result;
    }
}

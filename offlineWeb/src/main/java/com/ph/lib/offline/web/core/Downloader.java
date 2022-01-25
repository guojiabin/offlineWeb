package com.ph.lib.offline.web.core;

/**
 * created by guojiabin on 2022/1/24
 * 离线包下载器
 */
public interface Downloader {
    /***
     * 离线包下载
     * */
    void  download(PackageInfo packageInfo, DownloadCallback callback);

    interface DownloadCallback {
        void onSuccess(String packageId);

        void onFailure(String packageID);
    }
}

package com.ph.lib.offline.web.inner;

import android.content.Context;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadSampleListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.ph.lib.offline.web.core.Downloader;
import com.ph.lib.offline.web.core.PackageInfo;
import com.ph.lib.offline.web.core.util.FileUtils;
import com.ph.lib.offline.web.core.util.Logger;

/**
 * created by guojiabin on 2022/1/24
 * 下载器实现类
 */
public class DownloaderImpl implements Downloader {
    private Context context;

    public DownloaderImpl(Context context) {
        this.context = context;
    }

    @Override
    public void download(PackageInfo packageInfo, final DownloadCallback callback) {
        BaseDownloadTask downloadTask = FileDownloader.getImpl()
            .create(packageInfo.getDownloadUrl())
            .setTag(packageInfo.getPackageId())
            .setPath(FileUtils.getPackageDownloadName(context, packageInfo.getPackageId(), packageInfo.getVersion()))
            .setListener(new FileDownloadSampleListener() {
                @Override
                protected void completed(BaseDownloadTask task) {
                    super.completed(task);
                    if (callback != null && task.getStatus() == FileDownloadStatus.completed) {
                        callback.onSuccess((String) task.getTag());
                    } else if (callback != null) {
                        callback.onFailure((String) task.getTag());
                    }
                }

                @Override
                protected void error(BaseDownloadTask task, Throwable e) {
                    super.error(task, e);
                    Logger.e("pacakgeResource download error [" + e.getMessage() + "]");
                    if (callback != null) {
                        callback.onFailure((String) task.getTag());
                    }
                }
            });
        downloadTask.start();
    }
}

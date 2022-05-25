package com.ph.lib.offline.web.inner;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.dianping.logan.Logan;
import com.ph.lib.offline.web.core.PackageEntity;
import com.ph.lib.offline.web.core.PackageInfo;
import com.ph.lib.offline.web.core.PackageInstaller;
import com.ph.lib.offline.web.core.util.FileUtils;
import com.ph.lib.offline.web.core.util.GsonUtils;
import com.ph.lib.offline.web.core.util.Logger;
import com.yjj.utils.DiffUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * created by guojiabin on 2022/1/26
 * 单个离线包安装器
 */
public class PackageInstallerImpl implements PackageInstaller {
    private Context context;
    private final static String TAG = "PackageInstallerImpl";

    public PackageInstallerImpl(Context context) {
        this.context = context;
    }

    /**
     * 下载文件 download.zip
     * 如果是patch文件 merge.zip
     * 更新后的zip目录 update.zip
     */
    @Override
    public boolean install(PackageInfo packageInfo, boolean isAssets) {
        /**
         * 获取下载目录
         * */
        String downloadFile =
            isAssets ? FileUtils.getPackageAssetsName(context, packageInfo.getPackageId(), packageInfo.getVersion())
                : FileUtils.getPackageDownloadName(context, packageInfo.getPackageId(), packageInfo.getVersion());
        String willCopyFile = downloadFile;
        /**
         * 获取update.zip名称
         * */
        String updateFile =
            FileUtils.getPackageUpdateName(context, packageInfo.getPackageId(), packageInfo.getVersion());
        boolean isSuccess = true;
        String lastVersion = getLastVersion(packageInfo.getPackageId());
        if (packageInfo.isPatch() && TextUtils.isEmpty(lastVersion)) {
            Logger.e("资源为patch ,但是上个版本信息没有数据，无法patch!");
            return false;
        }
        /**
         * merge离线增量
         */
        if (packageInfo.isPatch()) {
            String baseFile = FileUtils.getPackageUpdateName(context, packageInfo.getPackageId(), lastVersion);
            String mergePatch =
                FileUtils.getPackageMergePatch(context, packageInfo.getPackageId(), packageInfo.getVersion());
            int status = -1;
            try {
                status = DiffUtils.patch(baseFile, mergePatch, downloadFile);
            } catch (Exception ignore) {
                Logger.e("patch error " + ignore.getMessage());
            }
            if (status == 0) {
                willCopyFile = mergePatch;
                FileUtils.deleteFile(downloadFile);
            } else {
                isSuccess = false;
            }
        }
        if (!isSuccess) {
            Logger.e("资源patch merge 失败！");
            return false;
        }

        /***
         * 复制zip
         * */
        isSuccess = FileUtils.copyFileCover(willCopyFile, updateFile);
        if (!isSuccess) {
            Logger.e("[" + packageInfo.getPackageId() + "] : " + "copy file error ");
            Logan.w("[OfflinePackageManager]--"+"[" + packageInfo.getPackageId() + "] : " + "copy file error ",1);
            return false;
        }
        isSuccess = FileUtils.delFile(willCopyFile);
        if (!isSuccess) {
            Logger.e("[" + packageInfo.getPackageId() + "] : " + "delete will copy file error ");
            Logan.w("[OfflinePackageManager]--"+"[" + packageInfo.getPackageId() + "] : " + "delete will copy file error ",1);
            return false;
        }
        /**
         *
         * 解压成功
         */
        String workPath = FileUtils.getPackageWorkName(context, packageInfo.getPackageId(), packageInfo.getVersion());
        Logan.w("[OfflinePackageManager]--解压目录"+workPath,1);
        try {
            isSuccess = FileUtils.unZipFolder(updateFile, workPath);
        } catch (Exception e) {
            isSuccess = false;
        }
        if (!isSuccess) {
            Logger.e("[" + packageInfo.getPackageId() + "] : " + "unZipFolder error ");
            Logan.w("[OfflinePackageManager]--解压失败--->"+"[" + packageInfo.getPackageId() + "]",1);
            return false;
        }
        if (isSuccess) {
            FileUtils.deleteFile(willCopyFile);
            cleanOldFileIfNeed(packageInfo.getPackageId(), packageInfo.getVersion(), lastVersion);
        }
        return isSuccess;
    }

    private void cleanOldFileIfNeed(String packageId, String version, String lastVersion) {
        String path = FileUtils.getPackageRootByPackageId(context, packageId);
        File file = new File(path);
        if (!file.exists() || !file.isDirectory()) {
            return;
        }
        File[] versionList = file.listFiles();
        if (versionList == null || versionList.length == 0) {
            return;
        }
        List<File> deleteFiles = new ArrayList<>();
        for (File item : versionList) {
            if (TextUtils.equals(version, item.getName()) || TextUtils.equals(lastVersion, item.getName())) {
                continue;
            }
            deleteFiles.add(item);
        }
        for (File file1 : deleteFiles) {
            FileUtils.deleteDir(file1);
        }
    }

    private String getLastVersion(String packageId) {
        String packageIndexFile = FileUtils.getPackageIndexFileName(context);
        FileInputStream indexFis = null;
        try {
            indexFis = new FileInputStream(packageIndexFile);
        } catch (FileNotFoundException e) {

        }
        if (indexFis == null) {
            return "";
        }
        PackageEntity localPackageEntity = GsonUtils.fromJsonIgnoreException(indexFis, PackageEntity.class);
        if (localPackageEntity == null || localPackageEntity.getList() == null) {
            return "";
        }
        List<PackageInfo> list = localPackageEntity.getList();
        PackageInfo info = new PackageInfo();
        info.setPackageId(packageId);
        int index = list.indexOf(info);
        if (index >= 0) {
            return list.get(index).getVersion();
        }
        return "";
    }
}

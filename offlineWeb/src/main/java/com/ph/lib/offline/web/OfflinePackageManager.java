package com.ph.lib.offline.web;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.liulishuo.filedownloader.FileDownloader;
import com.ph.lib.offline.web.cache.CacheManage;
import com.ph.lib.offline.web.cache.DiskLruCacheImpl;
import com.ph.lib.offline.web.core.AssetResourceLoader;
import com.ph.lib.offline.web.core.Downloader;
import com.ph.lib.offline.web.core.PackageEntity;
import com.ph.lib.offline.web.core.PackageInfo;
import com.ph.lib.offline.web.core.PackageInstaller;
import com.ph.lib.offline.web.core.PackageStatus;
import com.ph.lib.offline.web.core.ResourceManager;
import com.ph.lib.offline.web.core.ResoureceValidator;
import com.ph.lib.offline.web.core.util.FileUtils;
import com.ph.lib.offline.web.core.util.GsonUtils;
import com.ph.lib.offline.web.core.util.Logger;
import com.ph.lib.offline.web.core.util.MD5Utils;
import com.ph.lib.offline.web.core.util.VersionUtils;
import com.ph.lib.offline.web.inner.AssetResourceLoaderImpl;
import com.ph.lib.offline.web.inner.DownloaderImpl;
import com.ph.lib.offline.web.inner.PackageInstallerImpl;
import com.ph.lib.offline.web.inner.ResourceManagerImpl;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * created by guojiabin on 2022/1/24
 * 离线包管理器
 */
public class OfflinePackageManager {
    private static final int WHAT_DOWNLOAD_SUCCESS = 1;
    private static final int WHAT_DOWNLOAD_FAILURE = 2;
    private static final int WHAT_START_UPDATE = 3;
    private static final int WHAT_INIT_ASSETS = 4;

    private static final int STATUS_PACKAGE_CANUSE = 1;
    private volatile static OfflinePackageManager instance;

    public String baseUrl = "https://www.yunoa.com";

    private Context context;
    private ResourceManager resourceManager;
    private PackageInstaller packageInstaller;
    private AssetResourceLoader assetResourceLoader;
    private volatile boolean isUpdating = false;
    private Handler packageHandler;
    private HandlerThread packageThread;
    private PackageEntity localPackageEntity;
    /**
     * 即将下载的packageInfoList
     */
    private List<PackageInfo> willDownloadPackageInfoList;
    /***
     * 需要更新资源
     * */
    private List<PackageInfo> onlyUpdatePackageInfoList;
    private Lock resourceLock;
    private PackageValidator validator;
    private PackageValidator assetValidator;
    private Map<String, Integer> packageStatusMap = new HashMap<>();
    private PackageConfig config = new PackageConfig();
    private CacheManage mCacheManage;


    public static OfflinePackageManager getInstance() {
        if (instance == null) {
            synchronized (OfflinePackageManager.class) {
                if (instance == null) {
                    instance = new OfflinePackageManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context,String baseUrl) {
        this.context = context;
        resourceManager = new ResourceManagerImpl(context);
        packageInstaller = new PackageInstallerImpl(context);
        FileDownloader.init(context);
        this.baseUrl = baseUrl;
        validator = new DefaultPackageValidator(context);
        mCacheManage = CacheManage.build(context).setCacheMode(new DiskLruCacheImpl(context));
//        if (config.isEnableAssets() && !TextUtils.isEmpty(config.getAssetPath())) {
//            assetResourceLoader = new AssetResourceLoaderImpl(context);
//            assetValidator = new DefaultAssetPackageValidator(context);
//            ensurePacakageThread();
//            packageHandler.sendEmptyMessage(WHAT_INIT_ASSETS);
//        }
    }

    public void setResouceValidator(ResoureceValidator validator) {
        resourceManager.setResourceValidator(validator);
    }

    public void setPackageValidator(PackageValidator validator) {
        this.validator = validator;
    }

    private OfflinePackageManager() {
        resourceLock = new ReentrantLock();
    }

    /**
     * 更新离线包信息
     *
     * @param packageStr package json字符串
     */
    public void update(String packageStr) {
        if (isUpdating) {
            return;
        }
        if (packageStr == null) {
            packageStr = "";
        }
        ensurePacakageThread();
        Message message = Message.obtain();
        message.what = WHAT_START_UPDATE;
        message.obj = packageStr;
        packageHandler.sendMessage(message);
    }

    private void ensurePacakageThread() {
        if (packageThread == null) {
            packageThread = new HandlerThread("offline_package_thread");
            packageThread.start();
            packageHandler = new DownloadHandler(packageThread.getLooper());
        }
    }

    /**
     * package thread执行
     *
     * @param packageStr json 字符串
     */
    private void performUpdate(String packageStr) {
        String packageIndexFileName = FileUtils.getPackageIndexFileName(context);
        File packageIndexFile = new File(packageIndexFileName);
        /***
         * 是否是第一次加载离线包
         * */
        boolean isFirstLoadPackage = false;
        if (!packageIndexFile.exists()) {
            isFirstLoadPackage = true;
        }
        PackageEntity netEntity = null;
        netEntity = GsonUtils.fromJsonIgnoreException(packageStr, PackageEntity.class);
        willDownloadPackageInfoList = new ArrayList<>();
        if (netEntity != null && netEntity.getList() != null) {
            willDownloadPackageInfoList.addAll(netEntity.getList());
        }
        /**
         * 不是第一次Load package
         */
        if (!isFirstLoadPackage) {
            initLocalEntity(packageIndexFile);
        }
        List<PackageInfo> packageInfoList = new ArrayList<>(willDownloadPackageInfoList.size());
        for (PackageInfo packageInfo : willDownloadPackageInfoList) {
            if (packageInfo.getStatus() == PackageStatus.offLine) {
                continue;
            }
            packageInfoList.add(packageInfo);
        }
        willDownloadPackageInfoList.clear();
        willDownloadPackageInfoList.addAll(packageInfoList);

        for (PackageInfo packageInfo : willDownloadPackageInfoList) {
            Downloader downloader = new DownloaderImpl(context);
            downloader.download(packageInfo, new DownloadCallback(this));
        }
        if (onlyUpdatePackageInfoList != null && onlyUpdatePackageInfoList.size() > 0) {
            for (PackageInfo packageInfo : onlyUpdatePackageInfoList) {
                resourceManager.updateResource(packageInfo.getPackageId(), packageInfo.getVersion());
//                updateIndexFile(packageInfo.getPackageId(), packageInfo.getVersion());
                synchronized (packageStatusMap) {
                    packageStatusMap.put(packageInfo.getPackageId(), STATUS_PACKAGE_CANUSE);
                }
            }
        }
    }

    private void initLocalEntity(File packageIndexFile) {
        FileInputStream indexFis = null;
        try {
            indexFis = new FileInputStream(packageIndexFile);
        } catch (FileNotFoundException e) {

        }
        if (indexFis == null) {
            return;
        }
        localPackageEntity = GsonUtils.fromJsonIgnoreException(indexFis, PackageEntity.class);
        if (localPackageEntity == null || localPackageEntity.getList() == null) {
            return;
        }
        int index = 0;
        for (PackageInfo localInfo : localPackageEntity.getList()) {
            if ((index = willDownloadPackageInfoList.indexOf(localInfo)) < 0) {
                continue;
            }
            PackageInfo info = willDownloadPackageInfoList.get(index);
            if (VersionUtils.compareVersion(info.getVersion(), localInfo.getVersion()) <= 0) {
                if (!checkResourceFileValid(info.getPackageId(), info.getVersion())) {
                    return;
                }
                willDownloadPackageInfoList.remove(index);
                if (onlyUpdatePackageInfoList == null) {
                    onlyUpdatePackageInfoList = new ArrayList<>();
                }
                if (info.getStatus() == PackageStatus.onLine) {
                    onlyUpdatePackageInfoList.add(localInfo);
                }
                localInfo.setStatus(info.getStatus());
            } else {
                if (!info.isPatch()) {
                    info.setIsPatch(false);
                } else {
                    info.setIsPatch(true);
                }
                localInfo.setStatus(info.getStatus());
                localInfo.setVersion(info.getVersion());
            }
        }
    }

    private boolean checkResourceFileValid(String packageId, String version) {
        File indexFile = FileUtils.getResourceIndexFile(context, packageId, version);
        return indexFile.exists() && indexFile.isFile();
    }

    private void updateIndexFile(String packageId, String version) {
        String packageIndexFileName = FileUtils.getPackageIndexFileName(context);
        File packageIndexFile = new File(packageIndexFileName);
        if (!packageIndexFile.exists()) {
            boolean isSuccess = true;
            try {
                isSuccess = packageIndexFile.createNewFile();
            } catch (IOException e) {
                isSuccess = false;
            }
            if (!isSuccess) {
                return;
            }
        }
        if (localPackageEntity == null) {
            FileInputStream indexFis = null;
            try {
                indexFis = new FileInputStream(packageIndexFile);
            } catch (FileNotFoundException e) {

            }
            if (indexFis == null) {
                return;
            }
            localPackageEntity = GsonUtils.fromJsonIgnoreException(indexFis, PackageEntity.class);
        }
        if (localPackageEntity == null) {
            localPackageEntity = new PackageEntity();
        }
        List<PackageInfo> packageInfoList = new ArrayList<>();
        if (localPackageEntity.getList() != null) {
            packageInfoList.addAll(localPackageEntity.getList());
        }
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackageId(packageId);
        int index = 0;
        if ((index = packageInfoList.indexOf(packageInfo)) >= 0) {
            packageInfoList.get(index).setVersion(version);
        } else {
            packageInfo.setStatus(PackageStatus.onLine);
            packageInfo.setVersion(version);
            packageInfoList.add(packageInfo);
        }
        localPackageEntity.setList(packageInfoList);
        if (localPackageEntity == null || localPackageEntity.getList() == null
                || localPackageEntity.getList().size() == 0) {
            return;
        }
        String updateStr = new Gson().toJson(localPackageEntity);
        try {
            FileOutputStream outputStream = new FileOutputStream(packageIndexFile);
            try {
                outputStream.write(updateStr.getBytes());
            } catch (IOException ignore) {
                Logger.e("write packageIndex file error");
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception ignore) {
            Logger.e("read packageIndex file error");
        }
    }

    public WebResourceResponse getResource(String url) {
//        synchronized (packageStatusMap) {
//            String packageId = resourceManager.getPackageId(url);
//            Integer status = packageStatusMap.get(packageId);
//            if (status == null) {
//                return null;
//            }
//            if (status != STATUS_PACKAGE_CANUSE) {
//                return null;
//            }
//        }
        WebResourceResponse resourceResponse = null;
        synchronized (resourceManager) {
            resourceResponse = resourceManager.getResource(url);
        }
//        if (!resourceLock.tryLock()) {
//            return null;
//        }

//        resourceLock.unlock();
        return resourceResponse;
    }

    private void downloadSuccess(String packageId) {
        if (packageHandler == null) {
            return;
        }
        Message message = Message.obtain();
        message.what = WHAT_DOWNLOAD_SUCCESS;
        message.obj = packageId;
        packageHandler.sendMessage(message);
    }

    private void downloadFailure(String packageId) {
        if (packageHandler == null) {
            return;
        }
        Message message = Message.obtain();
        message.what = WHAT_DOWNLOAD_FAILURE;
        message.obj = packageId;
        packageHandler.sendMessage(message);
    }

    private void performDownloadSuccess(String packageId) {
        if (willDownloadPackageInfoList == null) {
            return;
        }
        PackageInfo packageInfo = null;
        PackageInfo tmp = new PackageInfo();
        tmp.setPackageId(packageId);
        int pos = willDownloadPackageInfoList.indexOf(tmp);
        if (pos >= 0) {
            packageInfo = willDownloadPackageInfoList.remove(pos);
        }
        allResouceUpdateFinished();
        installPackage(packageId, packageInfo, false);
    }

    private void installPackage(String packageId, PackageInfo packageInfo, boolean isAssets) {
        /**
         * 安装
         * */
        if (packageInfo != null) {
            try {
                resourceLock.lock();

                boolean isSuccess = packageInstaller.install(packageInfo, isAssets);
                resourceLock.unlock();
                /**
                 * 安装失败情况下，不做任何处理，因为资源既然资源需要最新资源，失败了，就没有必要再用缓存了
                 */
                if (isSuccess) {
                    resourceManager.updateResource(packageInfo.getPackageId(), packageInfo.getVersion());
                    updateIndexFile(packageInfo.getPackageId(), packageInfo.getVersion());
                    synchronized (packageStatusMap) {
                        packageStatusMap.put(packageId, STATUS_PACKAGE_CANUSE);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    /**
     * 处理离线下载失败
     * 在package thread中执行
     *
     * @param packageId 离线包id
     */
    private void performDownloadFailure(String packageId) {
        if (willDownloadPackageInfoList == null) {
            return;
        }
        PackageInfo packageInfo = null;
        PackageInfo temp = new PackageInfo();
        temp.setPackageId(packageId);
        int pos = willDownloadPackageInfoList.indexOf(packageId);
        if (pos >= 0) {
            willDownloadPackageInfoList.get(pos);
        }
        if (packageInfo != null && packageInfo.getDownFailCount() < 5){
            packageInfo.setDownFailCount(packageInfo.getDownFailCount()+1);
            Downloader downloader = new DownloaderImpl(context);
            downloader.download(packageInfo,new DownloadCallback(this));
        }else{
            if(pos >= 0){
                willDownloadPackageInfoList.remove(pos);
            }
            allResouceUpdateFinished();
        }

    }

    public CacheManage getmCacheManage(){
        return mCacheManage;
    }

    private void allResouceUpdateFinished() {
        if (willDownloadPackageInfoList.size() == 0) {
            isUpdating = false;
        }
    }

    /**
     * 离线包handler处理器
     */
    class DownloadHandler extends Handler {
        public DownloadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WHAT_DOWNLOAD_SUCCESS:
                    performDownloadSuccess((String) msg.obj);
                    break;
                case WHAT_DOWNLOAD_FAILURE:
                    performDownloadFailure((String) msg.obj);
                    break;
                case WHAT_START_UPDATE:
                    performUpdate((String) msg.obj);
                    break;
                case WHAT_INIT_ASSETS:
                    performLoadAssets();
                    break;
                default:
                    break;
            }
        }
    }

    private void performLoadAssets() {
        if (assetResourceLoader == null) {
            return;
        }
        PackageInfo packageInfo = assetResourceLoader.load(config.getAssetPath());
        if (packageInfo == null) {
            return;
        }
        installPackage(packageInfo.getPackageId(), packageInfo, true);
    }

    static class DownloadCallback implements Downloader.DownloadCallback {
        private OfflinePackageManager packageManager;

        public DownloadCallback(OfflinePackageManager packageManager) {
            this.packageManager = packageManager;
        }

        @Override
        public void onSuccess(String packageId) {
            packageManager.downloadSuccess(packageId);
        }

        @Override
        public void onFailure(String packageId) {
            packageManager.downloadFailure(packageId);
        }
    }

    static class DefaultPackageValidator implements PackageValidator {
        private Context context;

        public DefaultPackageValidator(Context context) {
            this.context = context;
        }

        @Override
        public boolean validate(PackageInfo packageInfo) {
            String downloadFilePath =
                    FileUtils.getPackageDownloadName(context, packageInfo.getPackageId(), packageInfo.getVersion());
            File downloadFile = new File(downloadFilePath);
            if (downloadFile.exists() && MD5Utils.checkMD5(packageInfo.getMd5(), downloadFile)) {
                return true;
            }
            return false;
        }
    }

    static class DefaultAssetPackageValidator implements PackageValidator {
        private Context context;

        DefaultAssetPackageValidator(Context context) {
            this.context = context;
        }

        @Override
        public boolean validate(PackageInfo packageInfo) {
            String downloadFilePath =
                    FileUtils.getPackageAssetsName(context, packageInfo.getPackageId(), packageInfo.getVersion());
            File downloadFile = new File(downloadFilePath);
            if (downloadFile.exists() && MD5Utils.checkMD5(packageInfo.getMd5(), downloadFile)) {
                return true;
            }
            return false;
        }
    }
}

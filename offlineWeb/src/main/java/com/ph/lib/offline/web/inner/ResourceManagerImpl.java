package com.ph.lib.offline.web.inner;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.ph.lib.offline.web.OfflinePackageManager;
import com.ph.lib.offline.web.core.Contants;
import com.ph.lib.offline.web.core.ResourceInfo;
import com.ph.lib.offline.web.core.ResourceInfoEntity;
import com.ph.lib.offline.web.core.ResourceKey;
import com.ph.lib.offline.web.core.ResourceManager;
import com.ph.lib.offline.web.core.ResoureceValidator;
import com.ph.lib.offline.web.core.util.FileUtils;
import com.ph.lib.offline.web.core.util.GsonUtils;
import com.ph.lib.offline.web.core.util.Logger;
import com.ph.lib.offline.web.core.util.MD5Utils;
import com.ph.lib.offline.web.core.util.MimeTypeUtils;
import com.ph.lib.offline.web.core.util.VersionUtils;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * created by guojiabin on 2022/1/24
 * 资源管理类实现
 */
public class ResourceManagerImpl implements ResourceManager {
    private Map<ResourceKey, ResourceInfo> resourceInfoMap;
    private Context context;
    private Lock lock;
    private ResoureceValidator validator;

    public ResourceManagerImpl(Context context) {
        resourceInfoMap = new ConcurrentHashMap<>(16);
        this.context = context;
        lock = new ReentrantLock();
        validator = new DefaultResourceValidator();
    }

    /**
     * 获取资源信息
     * 会做md5校验
     *
     * @param url 请求地址
     */
    @Override
    public WebResourceResponse getResource(String url) {
        ResourceKey key = new ResourceKey(url);
//        if (!lock.tryLock()) {
//            return null;
//        }
//        Log.d("WebResourceResponse",url + "---"+)
        ResourceInfo resourceInfo = resourceInfoMap.get(key);
//        lock.unlock();
        if (resourceInfo == null) {
            resourceInfo = OfflinePackageManager.getInstance().getmCacheManage().getResourceInfo(key);
            if (resourceInfo == null){
                return null;
            }else{
                resourceInfoMap.put(key,resourceInfo);
            }
            return null;
        }
        if (!MimeTypeUtils.checkIsSupportMimeType(resourceInfo.getMimeType())) {
            Logger.d("getResource [" + url + "]" + " is not support mime type");
            safeRemoveResource(key);
            return null;
        }
        InputStream inputStream = FileUtils.getInputStream(resourceInfo.getLocalPath());
        if (inputStream == null) {
            Logger.d("getResource [" + url + "]" + " inputStream is null");
            safeRemoveResource(key);
            return null;
        }
        if (validator != null && !validator.validate(resourceInfo)) {
            safeRemoveResource(key);
            return null;
        }
        WebResourceResponse response;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Map<String, String> header = new HashMap<>(2);
            header.put("Access-Control-Allow-Origin", "*");
            header.put("Access-Control-Allow-Headers", "Content-Type");
            response = new WebResourceResponse(resourceInfo.getMimeType(), "UTF-8", 200, "ok", header, inputStream);
        } else {
            response = new WebResourceResponse(resourceInfo.getMimeType(), "UTF-8", inputStream);
        }
        return response;
    }

    private void safeRemoveResource(ResourceKey key) {
        if (lock.tryLock()) {
            resourceInfoMap.remove(key);
            lock.unlock();
        }
    }

    @Override
    public boolean updateResource(String packageId, String version) {
        boolean isSuccess = false;
        String indexFileName =
            FileUtils.getPackageWorkName(context, packageId, version) + File.separator + Contants.RESOURCE_MIDDLE_PATH
                + File.separator+Contants.PACKAGE+File.separator + Contants.RESOURCE_INDEX_NAME;
        Logger.d("updateResource indexFileName: " + indexFileName);
        File indexFile = new File(indexFileName);
        if (!indexFile.exists()) {
            Logger.e("updateResource indexFile is not exists ,update Resource error ");
            return isSuccess;
        }
        if (!indexFile.isFile()) {
            Logger.e("updateResource indexFile is not file ,update Resource error ");
            return isSuccess;
        }
        FileInputStream indexFis = null;
        try {
            indexFis = new FileInputStream(indexFile);
        } catch (FileNotFoundException e) {

        }
        if (indexFis == null) {
            Logger.e("updateResource indexStream is error,  update Resource error ");
            return isSuccess;
        }

        ResourceInfoEntity entity = GsonUtils.fromJsonIgnoreException(indexFis, ResourceInfoEntity.class);
        if (indexFis != null) {
            try {
                indexFis.close();
            } catch (IOException e) {

            }
        }
        if (entity == null) {
            return isSuccess;
        }
        List<ResourceInfo> resourceInfos = entity.getItems();
        isSuccess = true;
        if (resourceInfos == null) {
            return isSuccess;
        }
        String workPath = FileUtils.getPackageWorkName(context, packageId, version);
        for (ResourceInfo resourceInfo : resourceInfos) {
            if (TextUtils.isEmpty(resourceInfo.getPath())) {
                continue;
            }
            resourceInfo.setPackageId(packageId);
            String path = resourceInfo.getPath();
            path = path.startsWith(File.separator) ? path.substring(1) : path;
            resourceInfo.setLocalPath(
                workPath + File.separator + Contants.RESOURCE_MIDDLE_PATH + File.separator+Contants.PACKAGE+File.separator + path);
            lock.lock();

            String remoteUrl = VersionUtils.getBaseUrl(OfflinePackageManager.getInstance().baseUrl)+resourceInfo.getRemoteUrl();
            ResourceKey key = new ResourceKey(remoteUrl);
            if(resourceInfoMap.get(key) == null){
                resourceInfoMap.put(key,resourceInfo);
            }
            if(OfflinePackageManager.getInstance().getmCacheManage().getResourceInfo(key) == null){
                OfflinePackageManager.getInstance().getmCacheManage().saveResourceInfo(key,resourceInfo);
            }
            lock.unlock();
        }
        return isSuccess;
    }

    @Override
    public void setResourceValidator(ResoureceValidator validator) {
        this.validator = validator;
    }

    @Override
    public String getPackageId(String url) {
        if (!lock.tryLock()) {
            return null;
        }
        ResourceInfo resourceInfo = resourceInfoMap.get(new ResourceKey(url));
        lock.unlock();
        if (resourceInfo != null) {
            return resourceInfo.getPackageId();
        }
        return null;
    }

    static class DefaultResourceValidator implements ResoureceValidator {
        @Override
        public boolean validate(ResourceInfo resourceInfo) {
            String rMd5 = resourceInfo.getMd5();
            if (!TextUtils.isEmpty(rMd5) && !MD5Utils.checkMD5(rMd5, new File(resourceInfo.getLocalPath()))) {
                return false;
            }
            int size = 0;
            InputStream inputStream = null;
            try {
                inputStream = FileUtils.getInputStream(resourceInfo.getLocalPath());
                size = inputStream.available();
            } catch (IOException e) {
                Logger.e("resource file is error " + e.getMessage());
            }finally {
                if(inputStream != null){
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (size == 0) {
                Logger.e("resource file is error ");
                return false;
            }
            return true;
        }
    }
}

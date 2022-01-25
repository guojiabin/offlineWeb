package com.ph.lib.offline.web.inner;

import android.content.Context;
import android.text.TextUtils;

import com.ph.lib.offline.web.core.AssetResourceLoader;
import com.ph.lib.offline.web.core.PackageInfo;
import com.ph.lib.offline.web.core.PackageStatus;
import com.ph.lib.offline.web.core.ResourceInfoEntity;
import com.ph.lib.offline.web.core.util.FileUtils;
import com.ph.lib.offline.web.core.util.GsonUtils;
import com.ph.lib.offline.web.core.util.MD5Utils;
import com.ph.lib.offline.web.core.util.VersionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * created by guojiabin on 2022/1/24
 * asset 资源加载
 */
public class AssetResourceLoaderImpl implements AssetResourceLoader {
    private Context context;

    public AssetResourceLoaderImpl(Context context) {
        this.context = context;
    }

    @Override
    public PackageInfo load(String path) {
        InputStream inputStream = null;

        inputStream = openAssetInputStream(path);
        String indexInfo = FileUtils.getStringForZip(inputStream);
        if (TextUtils.isEmpty(indexInfo)) {
            return null;
        }
        ResourceInfoEntity assetEntity = GsonUtils.fromJsonIgnoreException(indexInfo, ResourceInfoEntity.class);
        if (assetEntity == null) {
            return null;
        }
        inputStream = openAssetInputStream(path);
        if (inputStream == null) {
            return null;
        }
        File file =
            new File(FileUtils.getPackageUpdateName(context, assetEntity.getPackageId(), assetEntity.getVersion()));
        ResourceInfoEntity localEntity = null;
        FileInputStream fileInputStream = null;
        if (file.exists()) {
            try {
                fileInputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {

            }
        }
        String lo = null;
        if (fileInputStream != null) {
            lo = FileUtils.getStringForZip(fileInputStream);
        }
        if (!TextUtils.isEmpty(lo)) {
            localEntity = GsonUtils.fromJsonIgnoreException(lo, ResourceInfoEntity.class);
        }
        if (localEntity != null
            && VersionUtils.compareVersion(assetEntity.getVersion(), localEntity.getVersion()) <= 0) {
            return null;
        }
        String assetPath =
            FileUtils.getPackageAssetsName(context, assetEntity.getPackageId(), assetEntity.getVersion());
        boolean isSuccess = FileUtils.copyFile(inputStream, assetPath);
        if (!isSuccess) {
            return null;
        }
        FileUtils.safeCloseFile(inputStream);
        String md5 = MD5Utils.calculateMD5(new File(assetPath));
        if (TextUtils.isEmpty(md5)) {
            return null;
        }
        PackageInfo info = new PackageInfo();
        info.setPackageId(assetEntity.getPackageId());
        info.setStatus(PackageStatus.onLine);
        info.setVersion(assetEntity.getVersion());
        info.setMd5(md5);
        return info;
    }

    private InputStream openAssetInputStream(String path) {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(path);
        } catch (IOException e) {
        }
        if (inputStream == null) {
            return null;
        }
        return inputStream;
    }
}

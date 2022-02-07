package com.ph.lib.offline.web.cache;

import android.content.Context;

import com.ph.lib.offline.web.core.ResourceInfo;
import com.ph.lib.offline.web.core.ResourceKey;

public class CacheManage {
    private long DISK_CACHE_SIZE = 1024 * 1024 * 100;
    private Context context;
    private ICache mICache;
    private CacheManage(Context context){
        this.context = context.getApplicationContext();
    }

    public static CacheManage build(Context context){
        return new CacheManage(context);
    }

    public ResourceInfo getResourceInfo(ResourceKey key){
        if(mICache != null){
            return mICache.getPackageInfo(key);
        }
        return null;
    }

    public void saveResourceInfo(ResourceKey key,ResourceInfo resourceInfo){
        if(mICache != null){
            mICache.savePackageInfo(key,resourceInfo);
        }
    }

    public CacheManage setCacheSize(long size){
        this.DISK_CACHE_SIZE = size;
        return this;
    }

    public CacheManage setCacheMode(ICache cacheMode){
        this.mICache = cacheMode;
        return this;
    }
}

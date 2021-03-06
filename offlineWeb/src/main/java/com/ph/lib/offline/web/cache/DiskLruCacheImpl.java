package com.ph.lib.offline.web.cache;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.jakewharton.disklrucache.DiskLruCache;
import com.ph.lib.offline.web.core.ResourceInfo;
import com.ph.lib.offline.web.core.ResourceKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DiskLruCacheImpl implements ICache{

    private Context mContext;
    private long DISK_CACHE_SIZE = 1024 * 1024 * 100;
    private DiskLruCache mDiskLruCache;
    public DiskLruCacheImpl(Context context){
        this.mContext = context;
    }
    
    public DiskLruCacheImpl setCacheSize(long size){
        this.DISK_CACHE_SIZE = size;
        return this;
    }
    
    @Override
    public void savePackageInfo(ResourceKey key, ResourceInfo packageInfo) {
        if(mDiskLruCache == null){
            initDiskCache();
        }
        ObjectOutputStream fos = null;
        OutputStream outputStream = null;
        DiskLruCache.Editor editor = null;
        try {
            editor = mDiskLruCache.edit(hasKeyFromUrl(key.getUrl()));
            Log.e("guojiabin","editor----"+editor);
            outputStream = editor.newOutputStream(0);
            Log.e("guojiabin-","outputStream-----"+outputStream);
            fos = new ObjectOutputStream(outputStream);
            fos.writeObject(packageInfo);
            editor.commit();
            mDiskLruCache.flush();
        }catch (IOException e){
            try {
                editor.abort();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            Log.d("guojiabin--111-",e.getMessage());
//            e.printStackTrace();
        }finally {
            try {
                editor.abort();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(fos != null){
                try {
                    fos.close();
                }catch (IOException e){
//                    e.printStackTrace();
                }
            }
            if(outputStream != null){
                try {
                    outputStream.close();
                }catch (IOException e){
//                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * ??????MD5???key
     * @param url
     * @return
     */
    private String hasKeyFromUrl(String url) {
        String cacheKey;
        try {
            if (TextUtils.isEmpty(url)){
                return null;
            }
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(url.getBytes());
            cacheKey = byteToHexString(messageDigest.digest());
        }catch (NoSuchAlgorithmException e){
            cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }

    /**
     * ???byte?????????16???????????????
     * @param digest
     * @return
     */
    private String byteToHexString(byte[] digest) {
        StringBuilder sb = new StringBuilder();
        for (int i= 0;i< digest.length; i++){
            String hex = Integer.toHexString(0xFF & digest[i]);
            if(hex.length() == 1){
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    private void initDiskCache() {
        File diskCacheDir = getDiskCacheDir(mContext,"'package'");
        if(!diskCacheDir.exists()){
            diskCacheDir.mkdirs();
        }
        if(getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE){
            try {
                mDiskLruCache = DiskLruCache.open(diskCacheDir,1,1,DISK_CACHE_SIZE);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }


    /**
     * ????????????????????????
     * @param diskCacheDir
     * @return
     */
    private long getUsableSpace(File diskCacheDir) {
        return diskCacheDir.getUsableSpace();
    }

    /**
     * ?????????????????????
     * @param context
     * @param uniqueName
     * @return
     */
    private File getDiskCacheDir(Context context,String uniqueName){
        boolean externalStorageAvailable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if(externalStorageAvailable){
            cachePath = context.getExternalCacheDir().getPath();
        }else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    @Override
    public ResourceInfo getPackageInfo(ResourceKey key) {
        if(mDiskLruCache == null){
            return null;
        }
        ResourceInfo resourceInfo = null;
        ObjectInputStream ois = null;
        FileInputStream fis = null;
        try {
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(hasKeyFromUrl(key.getUrl()));
            if(snapshot != null){
                fis = (FileInputStream) snapshot.getInputStream(0);
                ois = new ObjectInputStream(fis);
                try {
                    resourceInfo = (ResourceInfo) ois.readObject();
                    return resourceInfo;
                }catch (ClassNotFoundException e){
                    e.printStackTrace();
                }finally {
                    if(ois != null){
                        try {
                            ois.close();
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                    if(fis != null){
                        try {
                            fis.close();
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return resourceInfo;
    }
}

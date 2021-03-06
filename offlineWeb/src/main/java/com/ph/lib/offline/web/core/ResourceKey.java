package com.ph.lib.offline.web.core;

import android.net.Uri;
import android.text.TextUtils;

import java.util.List;

/**
 * created by guojiabin on 2022/1/24
 * 资源请求键
 */
public class ResourceKey {
    private String host;
    private String schema;
    private List<String> pathList;
    private String url;

    public ResourceKey(String url) {
        this.url = url;
        Uri uri = Uri.parse(url);
        host = uri.getHost();
        schema = uri.getScheme();
        pathList = uri.getPathSegments();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = result * 37 + hashNotNull(host);
        result = result * 37 + hashNotNull(schema);
        if (pathList != null) {
            for (String pathSeg : pathList) {
                result = result * 37 + hashNotNull(pathSeg);
            }
        }
        return result;
    }

    private int hashNotNull(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ResourceKey)) {
            return false;
        }
        ResourceKey that = (ResourceKey) obj;
        if (!TextUtils.equals(host, that.host)) {
            return false;
        }
        if (!TextUtils.equals(schema, that.schema)) {
            return false;
        }
        if (this.pathList == that.pathList) {
            return true;
        }
        if (pathList == null && that.pathList != null) {
            return false;
        }
        if (pathList != null && that.pathList == null) {
            return false;
        }
        boolean isEqual = true;
        for (String pa : pathList) {
            if (!that.pathList.contains(pa)) {
                isEqual = false;
                break;
            }
        }

        return isEqual;
    }
}

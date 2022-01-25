package com.ph.lib.offline.web.core;

/**
 * created by guojiabin on 2022/1/24
 * asset资源加载器
 */
public interface AssetResourceLoader {
    /**
     * asset资源路径信息
     * @param path
     */
    PackageInfo load(String path);
}

package com.ph.lib.offline.web.core;


import com.tencent.smtt.export.external.interfaces.WebResourceResponse;

/**
 * created by guojiabin on 2022/1/24
 * 资源管理器
 */
public interface ResourceManager {
    WebResourceResponse getResource(String url);

    boolean updateResource(String packageId, String version,String baseUrl);

    void setResourceValidator(ResoureceValidator validator);

    String getPackageId(String url);
}

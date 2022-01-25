package com.ph.lib.offline.web;

import com.ph.lib.offline.web.core.PackageInfo;

/**
 * created by guojiabin on 2022/1/24
 * 校验资源信息的有效性
 */
public interface PackageValidator {
    boolean validate(PackageInfo info);
}

package com.ph.lib.offline.web.core;

import java.util.List;

/**
 * created by guojiabin on 2022/1/24
 * 离线包Index信息
 */
public class PackageEntity {
    private List<PackageInfo> list;

    public void setList(List<PackageInfo> items) {
        this.list = items;
    }

    public List<PackageInfo> getList() {
        return list;
    }
}

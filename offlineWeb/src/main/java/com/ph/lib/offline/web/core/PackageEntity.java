package com.ph.lib.offline.web.core;

import java.util.List;

/**
 * created by guojiabin on 2022/1/24
 * 离线包Index信息
 */
public class PackageEntity {
    private List<PackageInfo> items;

    public void setItems(List<PackageInfo> items) {
        this.items = items;
    }

    public List<PackageInfo> getItems() {
        return items;
    }
}

package com.ph.lib.offline.web.cache;

import com.ph.lib.offline.web.core.ResourceInfo;
import com.ph.lib.offline.web.core.ResourceKey;

public interface ICache {
    void savePackageInfo(ResourceKey key, ResourceInfo packageInfo);
    ResourceInfo getPackageInfo(ResourceKey key);
}

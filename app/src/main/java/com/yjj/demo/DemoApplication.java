package com.yjj.demo;

import android.app.Application;

import com.ph.lib.offline.web.OfflinePackageManager;


/**
 * created by yangjianjun on 2019/5/7
 */
public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        OfflinePackageManager.getInstance().init(this,"");
        OfflinePackageManager.getInstance().update(getPackageInfo());
    }

    private String getPackageInfo() {
        return "";
    }

    private String getPackageInfoPatch() {
        return "";
    }
}

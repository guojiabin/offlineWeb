package com.ph.lib.offline.web.core.util;

import android.util.Log;

/**
 * created by guojiabin on 2022/1/24
 */
public class Logger {
    private static final String TAG = "webpackagekit";
    public static boolean DEBUG = true;

    public static void d(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }

    public static void e(String msg) {
        if (DEBUG) {
            Log.e(TAG, msg);
        }
    }

    public static void w(String msg) {
        if (DEBUG) {
            Log.w(TAG, msg);
        }
    }
}

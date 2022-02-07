package com.ph.lib.offline.web.core.util;

import android.text.TextUtils;

import com.ph.lib.offline.web.core.Contants;

/**
 * created by guojiabin on 2022/1/24
 * 版本工具
 */
public class VersionUtils {

    public static int compareVersion(String version1, String version2) {

        if (version1 == null || version2 == null) {
            return 0;
        }
        //注意此处为正则匹配，不能用"."；
        String[] versionArray1 = version1.split("\\.");
        //如果位数只有一位则自动补零（防止出现一个是04，一个是5 直接以长度比较）
        for (int i = 0; i < versionArray1.length; i++) {
            if (versionArray1[i].length() == 1) {
                versionArray1[i] = "0" + versionArray1[i];
            }
        }
        String[] versionArray2 = version2.split("\\.");
        //如果位数只有一位则自动补零
        for (int i = 0; i < versionArray2.length; i++) {
            if (versionArray2[i].length() == 1) {
                versionArray2[i] = "0" + versionArray2[i];
            }
        }
        int idx = 0;
        //取最小长度值
        int minLength = Math.min(versionArray1.length, versionArray2.length);
        int diff = 0;
        //先比较长度再比较字符
        while (idx < minLength && (diff = versionArray1[idx].length() - versionArray2[idx].length()) == 0
            && (diff = versionArray1[idx].compareTo(versionArray2[idx])) == 0) {
            ++idx;
        }
        //如果已经分出大小，则直接返回，如果未分出大小，则再比较位数，有子版本的为大；
        diff = (diff != 0) ? diff : versionArray1.length - versionArray2.length;
        return diff;
    }

    /**
     * 比较2个版本 currentVersion 长度和newVersion长度不一致，返回true
     * @param currentVersion
     * @param newVersion
     * @return
     */
    public static boolean compareVersionName(String currentVersion,String newVersion){
        if(TextUtils.isEmpty(currentVersion) || TextUtils.isEmpty(newVersion)){
            return false;
        }
        String[] currentVersionArray = currentVersion.split("\\.");
        String[] newVersionArray = newVersion.split("\\.");
        if (currentVersionArray == null || currentVersionArray.length <= 0 || newVersionArray == null  || newVersionArray.length <= 0){
            return false;
        }
        if (currentVersionArray.length != newVersionArray.length){
            return true;
        }
        boolean newFlag = false;
        for (int i = 0; i < currentVersionArray.length; i++) {
            if (Integer.parseInt(currentVersionArray[i]) > Integer.parseInt(newVersionArray[i])){
                newFlag = false;
                break;
            }else if (Integer.parseInt(currentVersionArray[i]) < Integer.parseInt(newVersionArray[i])){
                newFlag = true;
                break;
            }
        }
        return newFlag;
    }

    // 根据不同的环境，h5离线包放置在不同的服务器上
    public static String getBaseUrl(String baseUrl){
        String baseTypeUrl = Contants.BASE_URL;

        if (TextUtils.isEmpty(baseUrl)){
            return baseTypeUrl;
        }

        if(baseUrl.contains("ali-test")){
            baseTypeUrl = Contants.ALI_TEST_BASE_URL;
        }else if(baseUrl.contains("demo-pre") || baseUrl.contains("demo")){
            baseTypeUrl = Contants.DEMO_PRE_BASE_URL;
        }
        return baseTypeUrl;
    }

    // 根据不同的环境，H5离线包放置在不同的文件夹中
    public static String getPackageDir(String baseUrl){
        String baseTypeUrl = "prod";
        if(baseUrl.contains("ali-test")){
            baseTypeUrl = "ali-test";
        }else if (baseUrl.contains("demo-pre")){
            baseTypeUrl = "demo-pre";
        }else if (baseUrl.contains("demo")){
            baseTypeUrl = "demo";
        }
        return baseTypeUrl;
    }

}

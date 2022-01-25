package com.ph.lib.offline.web.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * created by guojiabin on 2022/1/24
 * mimeType工具类
 */
public class MimeTypeUtils {
    private static List<String> supportMineTypeList = new ArrayList<>(2);

    static {
        supportMineTypeList.add("application/x-javascript");
        supportMineTypeList.add("image/jpeg");
        supportMineTypeList.add("image/tiff");
        supportMineTypeList.add("text/css");
        supportMineTypeList.add("image/gif");
        supportMineTypeList.add("image/png");
    }

    public static boolean checkIsSupportMimeType(String mimeType) {
        return supportMineTypeList.contains(mimeType);
    }
}

package com.yjj.demo;

import android.app.Activity;
import android.os.Bundle;

import com.ph.lib.offline.web.OfflineWebViewClient;
import com.tencent.smtt.sdk.WebView;


/**
 * created by yangjianjun on 2019/5/7
 */
public class WebActivity extends Activity {

    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        webView = findViewById(R.id.web);
        webView.setWebViewClient(new OfflineWebViewClient(null));
        webView.loadUrl("http://image.baidu.com/search/wiseindex?tn=wiseindex&wiseps=1");
    }
}

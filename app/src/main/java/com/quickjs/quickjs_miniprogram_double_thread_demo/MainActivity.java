package com.quickjs.quickjs_miniprogram_double_thread_demo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private QuickJSEngine javascriptEngine;
    private WebViewRender webViewRender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webViewRender = new WebViewRender(this);
        javascriptEngine = new QuickJSEngine(this, webViewRender);
        setContentView(webViewRender);
        webViewRender.loadHtmlFile("sample/sample.html");
        javascriptEngine.executeModule("sample/sample.js");
    }


    @Override
    protected void onDestroy() {
        javascriptEngine.close();
        webViewRender.destroy();
        super.onDestroy();
    }
}


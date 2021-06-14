package com.quickjs.quickjs_miniprogram_double_thread_demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebView;

import com.quickjs.JSArray;
import com.quickjs.JSContext;
import com.quickjs.JSObject;
import com.quickjs.JavaVoidCallback;
import com.quickjs.QuickJS;
import com.quickjs.plugin.ConsolePlugin;
import com.quickjs.plugin.SetTimeoutPlugin;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    WebView webView;
    QuickJS quickJS;
    JSContext context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/sample.html");
        quickJS = QuickJS.createRuntimeWithEventQueue();
        context = quickJS.createContext();
        context.registerJavaMethod((receiver, args) -> {
            JSONObject jsonObject = args.getObject(0).toJSONObject();
            setData(jsonObject);
        }, "setData");
        context.addPlugin(new SetTimeoutPlugin());
        context.addPlugin(new ConsolePlugin());
        context.executeVoidScript(FileUtils.readAssetText(this, "sample.js"), null);
    }

    void setData(JSONObject jsonObject) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("javascript:setData(" + jsonObject.toString() + ")");
            }
        });
    }
}
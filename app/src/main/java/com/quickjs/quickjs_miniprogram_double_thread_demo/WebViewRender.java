package com.quickjs.quickjs_miniprogram_double_thread_demo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.LinkedList;

class WebViewRender extends WebView implements Render {
    private final LinkedList<Runnable> events = new LinkedList<>();
    private boolean init = false;
    private JavascriptEngine engine;

    public WebViewRender(@NonNull Context context) {
        super(context);
        getSettings().setJavaScriptEnabled(true);
        setWebChromeClient(new WebChromeClient());
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                init = true;
                postEvent(null);
            }
        });
        this.addJavascriptInterface(new FrameworkHandler(), "_framework");
    }

    private class FrameworkHandler {

        @JavascriptInterface
        public void invokeFunction(String name, String params) {
            engine.invokeFunction(name, params);
        }
    }

    private void postEvent(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (runnable != null) {
                events.add(runnable);
            }
            if (init) {
                while (!events.isEmpty()) {
                    Runnable first = events.pollFirst();
                    if (first != null) {
                        first.run();
                    }
                }
            }
        });

    }

    @Override
    public void setData(JSONObject data) {
        postEvent(() -> loadUrl("javascript:setData(" + data.toString() + ")"));
    }

    @Override
    public void initRender(JavascriptEngine engine, JSONObject data, JSONObject methods) {
        this.engine = engine;
        postEvent(() -> loadUrl(String.format("javascript:initRender(%s,%s)", data.toString(), methods.toString())));
    }

    public void loadHtmlFile(String fileName) {
        String baseUrl = "file:///android_asset/" + fileName;
        String framework = FileUtils.readAssetText(getContext(), "framework/webview.html");
        String data = FileUtils.readAssetText(getContext(), fileName);
        framework = framework.replace("@CONENT", data);
        loadDataWithBaseURL(baseUrl, framework, "text/html", "utf-8", null);
    }
}


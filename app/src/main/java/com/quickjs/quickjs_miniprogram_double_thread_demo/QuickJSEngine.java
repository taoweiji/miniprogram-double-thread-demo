package com.quickjs.quickjs_miniprogram_double_thread_demo;

import android.content.Context;

import com.quickjs.JSArray;
import com.quickjs.JSContext;
import com.quickjs.JSObject;
import com.quickjs.QuickJS;
import com.quickjs.plugin.ConsolePlugin;
import com.quickjs.plugin.SetTimeoutPlugin;

public class QuickJSEngine implements JavascriptEngine {
    private final Context context;
    private QuickJS quickJS;
    private JSContext jsContext;
    private final Render render;

    public QuickJSEngine(Context context, Render render) {
        this.context = context;
        this.render = render;
        initEngine();
    }

    private void initEngine() {
        quickJS = QuickJS.createRuntimeWithEventQueue();
        jsContext = quickJS.createContext();
        initRenderHandler();
        jsContext.addPlugin(new SetTimeoutPlugin());
        jsContext.addPlugin(new ConsolePlugin());
        executeModule("framework/quickjs.js");
    }

    private void initRenderHandler() {
        JSObject renderHandler = new JSObject(jsContext);
        renderHandler.registerJavaMethod((receiver, args) -> {
            render.setData(args.getObject(0).toJSONObject());
        }, "setData");
        renderHandler.registerJavaMethod((receiver, args) -> {
            JSObject data = args.getObject(0);
            JSObject methods = args.getObject(1);
            render.initRender(this, data.toJSONObject(), methods.toJSONObject());
        }, "initRender");
        this.jsContext.set("render", renderHandler);
    }

    public void close() {
        quickJS.close();
    }

    public void executeScript(String script, String fileName) {
        jsContext.postEventQueue(new Runnable() {
            @Override
            public void run() {
                jsContext.executeVoidScript(script, fileName);
            }
        });
    }

    public void executeModule(String fileName) {
        jsContext.postEventQueue(new Runnable() {
            @Override
            public void run() {
                jsContext.executeVoidScript(FileUtils.readAssetText(context, fileName), fileName);
            }
        });
    }

    @Override
    public void invokeFunction(String name, String params) {
        jsContext.postEventQueue(new Runnable() {
            @Override
            public void run() {
                jsContext.executeFunction("invokeFunction", new JSArray(jsContext).push(name).push(params));
            }
        });
    }
}
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
    private final Render render;
    private final QuickJS quickJS;
    private final JSContext jsContext;

    public QuickJSEngine(Context context, Render render) {
        this.context = context;
        this.render = render;
        this.quickJS = QuickJS.createRuntimeWithEventQueue();
        this.jsContext = quickJS.createContext();
        initEngine();
    }

    private void initEngine() {
        initRenderHandler();
        jsContext.addPlugin(new SetTimeoutPlugin());
        jsContext.addPlugin(new ConsolePlugin());
        executeModule("framework/quickjs.js");
    }

    /**
     * 注册一个 render 对象，提供 setData 和 initRender 的回调方法
     */
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

    public void executeModule(String fileName) {
        jsContext.executeVoidScript(FileUtils.readAssetText(context, fileName), fileName);
    }

    @Override
    public void invokeFunction(String name, String params) {
        jsContext.executeFunction("invokeFunction", new JSArray(jsContext).push(name).push(params));
    }
}
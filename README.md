# 使用 quickjs-android 和 Vue.js 模仿微信小程序的双线程渲染架构

微信小程序和网页最大的不同是小程序基于 WebView + JS引擎实现的双线程渲染架构，参考微信小程序的 [渲染层和逻辑层](https://developers.weixin.qq.com/miniprogram/dev/framework/quickstart/framework.html#%E6%B8%B2%E6%9F%93%E5%B1%82%E5%92%8C%E9%80%BB%E8%BE%91%E5%B1%82) 文档，实际上是多个 WebView 加上一个 JS引擎，在 Android 上是使用 [Google v8](https://github.com/v8/v8) 引擎。之所以使用双线程模式，主要是为了安全性，有限提供JS能力，避免开发者肆无忌惮地滥用小程序能力。

![](https://upload-images.jianshu.io/upload_images/2431302-43323294f2ffc61c.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

我最近也在研究相关的小程序技术，也开源了一个JS引擎相关的框架 [quickjs-android](https://github.com/taoweiji/quickjs-android)。小程序的[ Page ](https://developers.weixin.qq.com/miniprogram/dev/reference/api/Page.html)的设计和[ Vue.js ](https://cn.vuejs.org/v2/guide/index.html)很相似，所以使用  quickjs-android 和 Vue.js 模仿微信小程序的双线程架构，实现状态更新和事件触发。

![](https://upload-images.jianshu.io/upload_images/2431302-b3d97a4e6fac27b4.gif?imageMogr2/auto-orient/strip)

### 框架分析
#### Page
```javascript
Page({
    data: {
        counter: 0
    },
    onMinusClick: function () {
        this.setData({ counter: this.data.counter - 1 })
    },
    onAddClick: function () {
        this.setData({ counter: this.data.counter + 1 })
    }
});
```
#### Vue.js
```javascript
var example1 = new Vue({
    el: '#example-1',
    data: {
        counter: 0
    },
    methods: {
        onMinusClick: function () {
            this.counter--
        },
        onAddClick: function () {
            this.counter++
        }
    }
})
```
对比微信小程序和 Vue.js，使用方式很相似，但是底层是相差非常的大。

### Native 层代码
##### Render 接口
```java
public interface Render {
    /**
     * 对应微信小程序的setData方法
     */
    void setData(JSONObject data);

    /**
     * 初始化渲染层
     *
     * @param engine  引擎对象
     * @param data    对应 page 的 data
     * @param methods 对应 page 的函数
     */
    void initRender(JavascriptEngine engine, JSONObject data, JSONObject methods);
}
```
##### JavascriptEngine 接口
```java
public interface JavascriptEngine {
    /**
     * 触发引擎的 page 函数
     * @param name 函数名
     * @param params 参数
     */
    void invokeFunction(String name, String params);
}
```

##### quickjs-android 引擎实现
```java
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
```
##### WebView 实现
```java
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
```
### 逻辑层框架代码
```
// framework/quickjs.js
function Page(page) {
    globalThis._page = page;
    if (page['data'] == undefined) {
        page.data = {}
    }

    page.setData = function (data) {
        var keys = Object.keys(data);
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            page.data[key] = data[key];
        }
        render.setData(data);
    }
    var methods = {};
    Object.keys(page).forEach(function (key) {
        var obj = page[key];
        if (typeof obj === "function") {
            methods[key] = {}
        }
    });
    render.initRender(page.data, methods);
}
function invokeFunction(name, params) {
    globalThis._page[name](params);
}
```
### 渲染层框架代码
framework/webview.html
```
<!DOCTYPE html>
<head>
  <title>A page written in english</title>
</head>
<body>
  <div id="app">
    @CONENT
  </div>
  <script src="https://cdn.jsdelivr.net/npm/vue@2/dist/vue.js"></script>
  <script>
    function initRender(data, methods) {
      var page = {};
      page.el = '#app';
      page.data = data;
      page.methods = {};
      Object.keys(methods).forEach(function (name) {
        page.methods[name] = function (params) {
          _framework.invokeFunction(name, JSON.stringify(params));
        }
      });
      self.app = new Vue(page);
    };
    function setData(obj) {
      var keys = Object.keys(obj);
      keys.forEach(element => {
        self.app[element] = obj[element];
      });
    };
  </script>
</body>
</html>
```

### 示例
逻辑层代码
```
Page({
    data: {
        counter: 0
    },
    onMinusClick: function () {
        this.setData({ counter: this.data.counter - 1 })
    },
    onAddClick: function () {
        this.setData({ counter: this.data.counter + 1 })
    }
});
```
页面
```
<div>
  {{ counter }}
  <div style="margin-top: 20px;">
    <button v-on:click="onMinusClick">-</button>
    <button v-on:click="onAddClick">+</button>
  </div>
</div>
```


### Demo 完整代码
[https://github.com/taoweiji/miniprogram-double-thread-demo](https://github.com/taoweiji/miniprogram-double-thread-demo)





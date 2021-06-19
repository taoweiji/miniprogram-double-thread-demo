package com.quickjs.quickjs_miniprogram_double_thread_demo;

import org.json.JSONObject;

public interface Render {
    void setData(JSONObject data);

    void initRender(JavascriptEngine engine,JSONObject data, JSONObject methods);
}

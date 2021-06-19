package com.quickjs.quickjs_miniprogram_double_thread_demo;

import org.json.JSONObject;

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

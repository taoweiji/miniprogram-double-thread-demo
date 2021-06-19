package com.quickjs.quickjs_miniprogram_double_thread_demo;

public interface JavascriptEngine {
    /**
     * 触发引擎的 page 函数
     * @param name 函数名
     * @param params 参数
     */
    void invokeFunction(String name, String params);
}
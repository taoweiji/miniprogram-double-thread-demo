package com.quickjs.quickjs_miniprogram_double_thread_demo;

import android.content.Context;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    public static String readAssetText(Context context, String filename) {
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            InputStream is = context.getAssets().open(filename);
            byte[] bytes = new byte[1024];
            int len;
            while ((len = is.read(bytes)) != -1) {
                result.write(bytes, 0, len);
            }
            is.close();
            String str = result.toString();
            result.close();
            return str;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}

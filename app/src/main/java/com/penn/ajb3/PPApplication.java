package com.penn.ajb3;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import static java.security.AccessController.getContext;

/**
 * Created by penn on 31/08/2017.
 */

public class PPApplication extends Application {
    public enum PPValueType {
        ARRAY,
        INT,
        LONG,
        STRING,
        OBJECT
    }

    private static Context appContext;

    private static String APP_NAME = "PPJ";
    //preference keys
    public static String AUTH_BODY = "AUTH_BODY";

    //设置pref值
    public static void setPrefStringValue(String key, String value) {
        appContext.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE).edit().putString(key, value).apply();
    }

    public static void setPrefIntValue(String key, int value) {
        appContext.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE).edit().putInt(key, value).apply();
    }

    public static void setPrefLongValue(String key, long value) {
        appContext.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE).edit().putLong(key, value).apply();
    }

    //删除pref值
    public static void removePrefItem(String key) {
        appContext.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE).edit().remove(key).apply();
    }

    //取pref值
    public static String getPrefStringValue(String key, String defaultValue) {
        return appContext.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE).getString(key, defaultValue);
    }

    public static int getPrefIntValue(String key, int defaultValue) {
        return appContext.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE).getInt(key, defaultValue);
    }

    //ppFromString
    //解析json字符串(带空的默认值)
    public static JsonElement ppFromString(String json, String path, PPValueType type) {
        JsonElement jsonElement = ppFromString(json, path);
        if (jsonElement == null || jsonElement.isJsonNull()) {
            switch (type) {
                case ARRAY:
                    return new JsonArray();
                case INT:
                    return new JsonPrimitive(0);
                case LONG:
                    return new JsonPrimitive(0);
                case STRING:
                    return new JsonPrimitive("");
                case OBJECT:
                    return new JsonObject();
                default:
                    Log.v("ppLog", "should not happen5");
                    return null;
            }
        }

        return jsonElement;
    }

    public static JsonElement ppFromString(String json, String path) {
        try {
            JsonParser parser = new JsonParser();
            JsonElement item = parser.parse(json);
            return ppParser(item, path);
        } catch (Exception e) {
            Log.v("ppLog", "Json解析错误:" + e);
            return null;
        }
    }

    public static JsonElement ppParser(JsonElement item, String path) {
        if (item.isJsonNull() || TextUtils.isEmpty(path)) {
            //JsonNull or no path specified
            return null;
        }

        String[] pathArr = path.split("\\.");

        if (TextUtils.isEmpty(pathArr[0])) {
            Log.v("ppLog", "should not happen1");
            return null;
        }
        if (pathArr.length == 1) {
            if (item.isJsonArray()) {
                return item.getAsJsonArray().get(Integer.parseInt(pathArr[0]));
            } else if (item.isJsonObject()) {
                return item.getAsJsonObject().get(pathArr[0]);
            } else {
                Log.v("ppLog", "should not happen2");
                return null;
            }
        } else {
            String newPath = "";
            for (int i = 1; i < pathArr.length; i++) {
                if (i == (pathArr.length - 1)) {
                    newPath += pathArr[i];
                } else {
                    newPath += pathArr[i] + ".";
                }
            }

            if (item.isJsonArray()) {
                JsonElement newItem = item.getAsJsonArray().get(Integer.parseInt(pathArr[0]));

                return ppParser(newItem, newPath);
            } else if (item.isJsonObject()) {
                JsonElement newItem = item.getAsJsonObject().get(pathArr[0]);

                return ppParser(newItem, newPath);
            } else {
                Log.v("ppLog", "should not happen3");
                return null;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;
    }
}

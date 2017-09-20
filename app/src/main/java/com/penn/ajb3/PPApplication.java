package com.penn.ajb3;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.databinding.BindingAdapter;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.penn.ajb3.messageEvent.RelatedUserChanged;
import com.penn.ajb3.messageEvent.UserLogout;
import com.penn.ajb3.messageEvent.UserSignIn;
import com.penn.ajb3.realm.RMBlockUser;
import com.penn.ajb3.realm.RMMyProfile;
import com.penn.ajb3.realm.RMRelatedUser;
import com.penn.ajb3.util.PPRetrofit;
import com.qiniu.android.common.FixedZone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.util.ArrayList;

import de.jonasrottmann.realmbrowser.RealmBrowser;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import retrofit2.HttpException;

import static com.penn.ajb3.R.id.email;
import static java.security.AccessController.getContext;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

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

    public static Context appContext;

    public static String uuid;

    public static final String qiniuBase = "http://oemogmm69.bkt.clouddn.com/";

    private static final String APP_NAME = "PPJ";

    //preference keys
    public static final String AUTH_BODY = "AUTH_BODY";
    public static final String MY_ID = "MY_ID";
    public static final String USERNAME = "USERNAME";
    public static final String NICKNAME = "NICKNAME";
    public static final String AVATAR = "AVATAR";

    private static Configuration config = new Configuration.Builder()
            .zone(FixedZone.zone0)
            .build();

    private static UploadManager uploadManager = new UploadManager(config);

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

            if (TextUtils.isEmpty(path)) {
                return item;
            } else {
                return ppParser(item, path);
            }
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

    public static void initLocalData(String username) {
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .name(username + ".realm")
                .build();

        boolean clearData = false;
        if (clearData) {
            Realm.deleteRealm(config);
        }

        Realm.setDefaultConfiguration(config);

        reconnectToServer();
    }

    public static void reconnectToServer() {
        Observable<String> result = PPRetrofit.getInstance().getPPService().getMyProfile();

        Consumer<Object> callSuccess = new Consumer<Object>() {
            @Override
            public void accept(@NonNull final Object sObj) throws Exception {
                final String s = sObj.toString();
                try (Realm realm = Realm.getDefaultInstance()) {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            String itemStr = ppFromString(s, null).toString();
                            RMMyProfile obj = realm.where(RMMyProfile.class).findFirst();

                            if (obj == null) {
                                obj = new RMMyProfile();
                                obj._id = ppFromString(itemStr, "_id").getAsString();
                            }
                            obj.username = ppFromString(itemStr, "username").getAsString();
                            obj.nickname = ppFromString(itemStr, "nickname").getAsString();
                            obj.sex = ppFromString(itemStr, "sex").getAsString();
                            obj.avatar = ppFromString(itemStr, "avatar").getAsString();
                            obj.updateTime = ppFromString(itemStr, "updateTime").getAsLong();

                            // This will update an existing object with the same primary key
                            // or create a new object if an object with no primary key = _id
                            realm.copyToRealmOrUpdate(obj);
                        }
                    });
                }

                getNewFollows();
                getNewFans();
                getNewFriends();
                getNewBlocks();
            }
        };

        PPApplication.apiRequest(result, callSuccess, PPApplication.callFailure, null);
    }

    public static void getMyProfile() {
        Observable<String> result = PPRetrofit.getInstance().getPPService().getMyProfile();

        Consumer<Object> callSuccess = new Consumer<Object>() {
            @Override
            public void accept(@NonNull final Object sObj) throws Exception {
                final String s = sObj.toString();
                try (Realm realm = Realm.getDefaultInstance()) {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            String itemStr = ppFromString(s, null).toString();
                            RMMyProfile obj = realm.where(RMMyProfile.class).findFirst();

                            if (obj == null) {
                                obj = new RMMyProfile();
                                obj._id = ppFromString(itemStr, "_id").getAsString();
                            }
                            obj.username = ppFromString(itemStr, "username").getAsString();
                            obj.nickname = ppFromString(itemStr, "nickname").getAsString();
                            obj.sex = ppFromString(itemStr, "sex").getAsString();
                            obj.avatar = ppFromString(itemStr, "avatar").getAsString();
                            obj.updateTime = ppFromString(itemStr, "updateTime").getAsLong();

                            // This will update an existing object with the same primary key
                            // or create a new object if an object with no primary key = _id
                            realm.copyToRealmOrUpdate(obj);
                        }
                    });
                }
            }
        };

        PPApplication.apiRequest(result, callSuccess, PPApplication.callFailure, null);
    }

    public static void getNewFollows() {
        long startTime;
        try (Realm realm = Realm.getDefaultInstance()) {
            startTime = realm.where(RMMyProfile.class).findFirst().getNewFollowsTime;
        }
        Observable<String> result = PPRetrofit.getInstance().getPPService().getNewFollows(startTime);

        Consumer<Object> callSuccess = new Consumer<Object>() {
            @Override
            public void accept(@NonNull final Object sObj) throws Exception {
                String s = sObj.toString();
                final JsonArray users = ppFromString(s, null).getAsJsonArray();

                try (Realm realm = Realm.getDefaultInstance()) {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            long time = -1;

                            for (JsonElement item : users) {

                                String itemStr = item.toString();

                                long itemTime = ppFromString(itemStr, "updateTime").getAsLong();
                                if (itemTime > time) {
                                    time = itemTime;
                                }

                                String _id = ppFromString(itemStr, "targetUserId._id").getAsString();
                                boolean delete = ppFromString(itemStr, "deleted").getAsBoolean();

                                RMRelatedUser obj = realm.where(RMRelatedUser.class).equalTo("_id", _id).findFirst();

                                if (delete) {
                                    if (obj != null) {
                                        obj.isFollows = false;
                                        obj.updateTime = ppFromString(itemStr, "updateTime").getAsLong();
                                        Log.v("ppLog", "getNewFollows delete start");
                                        obj.delete();
                                        Log.v("ppLog", "getNewFollows delete end");
                                    }
                                } else {
                                    if (obj == null) {
                                        obj = new RMRelatedUser();
                                        obj._id = _id;
                                    }
                                    obj.username = ppFromString(itemStr, "targetUserId.username").getAsString();
                                    obj.nickname = ppFromString(itemStr, "targetUserId.nickname").getAsString();
                                    obj.sex = ppFromString(itemStr, "targetUserId.sex").getAsString();
                                    obj.avatar = ppFromString(itemStr, "targetUserId.avatar").getAsString();
                                    obj.updateTime = ppFromString(itemStr, "updateTime").getAsLong();
                                    obj.isFollows = true;

                                    // This will update an existing object with the same primary key
                                    // or create a new object if an object with no primary key = _id
                                    realm.copyToRealmOrUpdate(obj);
                                }
                            }

                            //更新时间戳
                            if (time > -1) {
                                realm.where(RMMyProfile.class).findFirst().getNewFollowsTime = time;
                            }
                        }
                    });
                }

                ArrayList<String> relatedUserIds = new ArrayList<String>();

                for (JsonElement item : users) {

                    String itemStr = item.toString();

                    String _id = ppFromString(itemStr, "targetUserId._id").getAsString();
                    relatedUserIds.add(_id);
                }

                EventBus.getDefault().post(new RelatedUserChanged(relatedUserIds));
                //如果有用Blockuser渲染的页面记录, 需要在这里post对应Event, 便于APP中相应记录重新渲染
            }
        };

        apiRequest(result, callSuccess, PPApplication.callFailure, null);
    }

    public static void getNewFans() {
        long startTime;
        try (Realm realm = Realm.getDefaultInstance()) {
            startTime = realm.where(RMMyProfile.class).findFirst().getNewFansTime;
        }
        Observable<String> result = PPRetrofit.getInstance().getPPService().getNewFans(startTime);

        Consumer<Object> callSuccess = new Consumer<Object>() {
            @Override
            public void accept(@NonNull final Object sObj) throws Exception {
                String s = sObj.toString();
                final JsonArray users = ppFromString(s, null).getAsJsonArray();

                try (Realm realm = Realm.getDefaultInstance()) {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            long time = -1;

                            for (JsonElement item : users) {
                                String itemStr = item.toString();

                                long itemTime = ppFromString(itemStr, "updateTime").getAsLong();
                                if (itemTime > time) {
                                    time = itemTime;
                                }

                                String _id = ppFromString(itemStr, "targetUserId._id").getAsString();
                                boolean delete = ppFromString(itemStr, "deleted").getAsBoolean();

                                RMRelatedUser obj = realm.where(RMRelatedUser.class).equalTo("_id", _id).findFirst();

                                if (delete) {
                                    if (obj != null) {
                                        obj.isFans = false;
                                        obj.updateTime = ppFromString(itemStr, "updateTime").getAsLong();
                                        obj.delete();
                                    }
                                } else {
                                    if (obj == null) {
                                        obj = new RMRelatedUser();
                                        obj._id = _id;
                                    }
                                    obj.username = ppFromString(itemStr, "targetUserId.username").getAsString();
                                    obj.nickname = ppFromString(itemStr, "targetUserId.nickname").getAsString();
                                    obj.sex = ppFromString(itemStr, "targetUserId.sex").getAsString();
                                    obj.avatar = ppFromString(itemStr, "targetUserId.avatar").getAsString();
                                    obj.updateTime = ppFromString(itemStr, "updateTime").getAsLong();
                                    obj.isFans = true;

                                    // This will update an existing object with the same primary key
                                    // or create a new object if an object with no primary key = _id
                                    realm.copyToRealmOrUpdate(obj);
                                }
                            }

                            //更新时间戳
                            if (time > -1) {
                                realm.where(RMMyProfile.class).findFirst().getNewFansTime = time;
                            }
                        }
                    });

                    ArrayList<String> relatedUserIds = new ArrayList<String>();

                    for (JsonElement item : users) {

                        String itemStr = item.toString();

                        String _id = ppFromString(itemStr, "targetUserId._id").getAsString();
                        relatedUserIds.add(_id);
                    }

                    EventBus.getDefault().post(new RelatedUserChanged(relatedUserIds));
                }
            }
        };

        PPApplication.apiRequest(result, callSuccess, PPApplication.callFailure, null);
    }

    public static void getNewFriends() {
        long startTime;
        try (Realm realm = Realm.getDefaultInstance()) {
            startTime = realm.where(RMMyProfile.class).findFirst().getNewFriendsTime;
        }
        Observable<String> result = PPRetrofit.getInstance().getPPService().getNewFriends(startTime);

        Consumer<Object> callSuccess = new Consumer<Object>() {
            @Override
            public void accept(@NonNull final Object sObj) throws Exception {
                String s = sObj.toString();
                try (Realm realm = Realm.getDefaultInstance()) {
                    final JsonArray users = ppFromString(s, null).getAsJsonArray();

                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            long time = -1;

                            for (JsonElement item : users) {
                                Log.v("ppLog Friends", item.toString());

                                String itemStr = item.toString();

                                long itemTime = ppFromString(itemStr, "updateTime").getAsLong();
                                if (itemTime > time) {
                                    time = itemTime;
                                }

                                String _id = ppFromString(itemStr, "targetUserId._id").getAsString();
                                boolean delete = ppFromString(itemStr, "deleted").getAsBoolean();
                                RMRelatedUser obj = realm.where(RMRelatedUser.class).equalTo("_id", _id).findFirst();

                                if (delete) {
                                    if (obj != null) {
                                        obj.isFriends = false;
                                        obj.updateTime = ppFromString(itemStr, "updateTime").getAsLong();
                                        obj.delete();
                                    }
                                } else {
                                    if (obj == null) {
                                        obj = new RMRelatedUser();
                                        obj._id = _id;
                                    }
                                    obj.username = ppFromString(itemStr, "targetUserId.username").getAsString();
                                    obj.nickname = ppFromString(itemStr, "targetUserId.nickname").getAsString();
                                    obj.sex = ppFromString(itemStr, "targetUserId.sex").getAsString();
                                    obj.avatar = ppFromString(itemStr, "targetUserId.avatar").getAsString();
                                    obj.updateTime = ppFromString(itemStr, "updateTime").getAsLong();
                                    obj.isFriends = true;

                                    // This will update an existing object with the same primary key
                                    // or create a new object if an object with no primary key = _id
                                    realm.copyToRealmOrUpdate(obj);
                                }
                            }

                            //更新时间戳
                            if (time > -1) {
                                realm.where(RMMyProfile.class).findFirst().getNewFriendsTime = time;
                            }
                        }
                    });

                    ArrayList<String> relatedUserIds = new ArrayList<String>();

                    for (JsonElement item : users) {

                        String itemStr = item.toString();

                        String _id = ppFromString(itemStr, "targetUserId._id").getAsString();
                        relatedUserIds.add(_id);
                    }

                    EventBus.getDefault().post(new RelatedUserChanged(relatedUserIds));
                }
            }
        };

        PPApplication.apiRequest(result, callSuccess, PPApplication.callFailure, null);
    }

    public static void getNewBlocks() {
        long startTime;
        try (Realm realm = Realm.getDefaultInstance()) {
            startTime = realm.where(RMMyProfile.class).findFirst().getNewBlocksTime;
        }
        Observable<String> result = PPRetrofit.getInstance().getPPService().getNewBlocks(startTime);

        Consumer<Object> callSuccess = new Consumer<Object>() {
            @Override
            public void accept(@NonNull final Object sObj) throws Exception {
                String s = sObj.toString();
                Log.v("ppLog", "original s:" + s);
                try (Realm realm = Realm.getDefaultInstance()) {
                    final JsonArray users = ppFromString(s, null).getAsJsonArray();

                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            long time = -1;

                            for (JsonElement item : users) {
                                Log.v("ppLog Blocks", item.toString());

                                String itemStr = item.toString();

                                Log.v("ppLog", "ppt1");

                                long itemTime = ppFromString(itemStr, "updateTime").getAsLong();
                                if (itemTime > time) {
                                    time = itemTime;
                                }

                                Log.v("ppLog", "ppt2");

                                String _id = ppFromString(itemStr, "_id").getAsString();
                                boolean delete = ppFromString(itemStr, "deleted").getAsBoolean();
                                RMBlockUser obj = realm.where(RMBlockUser.class).equalTo("_id", _id).findFirst();

                                Log.v("ppLog", "ppt3");

                                if (delete) {
                                    if (obj != null) {
                                        obj.deleteFromRealm();
                                    }
                                } else {
                                    if (obj == null) {
                                        obj = new RMBlockUser();
                                        obj._id = _id;
                                    }

                                    Log.v("ppLog", "ppt4");
                                    obj.ownerUserId = ppFromString(itemStr, "ownerUserId._id").getAsString();
                                    Log.v("ppLog", "ppt5");
                                    obj.ownerUsername = ppFromString(itemStr, "ownerUserId.username").getAsString();
                                    Log.v("ppLog", "ppt6");
                                    obj.targetUserId = ppFromString(itemStr, "targetUserId._id").getAsString();
                                    Log.v("ppLog", "ppt7");
                                    obj.targetUsername = ppFromString(itemStr, "targetUserId.username").getAsString();
                                    Log.v("ppLog", "ppt8");

                                    // This will update an existing object with the same primary key
                                    // or create a new object if an object with no primary key = _id
                                    realm.copyToRealmOrUpdate(obj);
                                }
                            }

                            Log.v("ppLog", "ppt9");

                            //更新时间戳
                            if (time > -1) {
                                realm.where(RMMyProfile.class).findFirst().getNewBlocksTime = time;
                            }
                        }
                    });

                    ArrayList<String> relatedUserIds = new ArrayList<String>();

                    for (JsonElement item : users) {

                        String itemStr = item.toString();

                        String _id = ppFromString(itemStr, "targetUserId._id").getAsString();
                        relatedUserIds.add(_id);
                    }

                    EventBus.getDefault().post(new RelatedUserChanged(relatedUserIds));
                }
            }
        };

        PPApplication.apiRequest(result, callSuccess, PPApplication.callFailure, null);
    }

    public static void getPush(String type) {
        Log.v("ppLog", "getPush:" + type);
        if (type.equals("get_new_friends")) {
            getNewFriends();
        } else if (type.equals("get_new_fans")) {
            getNewFans();
        } else if (type.equals("get_new_follows")) {
            getNewFollows();
        } else if (type.equals("get_new_blocks")) {
            getNewBlocks();
        } else if (type.equals("profile_updated")) {
            getMyProfile();
        } else {
            Log.v("ppLog", "getPush none");
        }
    }

    public static void showError(String error) {
        Toast.makeText(appContext, error, Toast.LENGTH_SHORT).show();
    }

    public static void showDB() {
        RealmBrowser.startRealmModelsActivity(appContext, Realm.getDefaultConfiguration());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;

        Realm.init(appContext);

        uuid = Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static void logout() {
        EventBus.getDefault().post(new UserLogout());
        removePrefItem(MY_ID);
        removePrefItem(AUTH_BODY);
        removePrefItem(USERNAME);
        removePrefItem(NICKNAME);
        removePrefItem(AVATAR);

        Intent errorActivity = new Intent("com.error.activity");//this has to match your intent filter
        errorActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(appContext, 0, errorActivity, 0);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void apiRequest(Observable<String> result, Consumer<Object> callSuccess, Consumer<Throwable> callFailure, Action finalAction) {
        if (finalAction == null) {
            finalAction = new Action() {
                @Override
                public void run() throws Exception {
                    //do nothing
                }
            };
        }

        result.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(finalAction)
                .subscribe(callSuccess, callFailure);
    }

    public static Consumer<Object> callSuccess = new Consumer<Object>() {
        @Override
        public void accept(@NonNull final Object s) {
            //do nothing
            Log.v("ppLog", "callSuccess:" + s.toString());
        }
    };

    public static Consumer<Throwable> callFailure = new Consumer<Throwable>() {
        @Override
        public void accept(@NonNull Throwable throwable) {
            try {
                if (throwable instanceof HttpException) {
                    //http非200返回code错误
                    HttpException exception = (HttpException) throwable;
                    String errorBodyString = exception.response().errorBody().string();
                    Log.v("ppLog", errorBodyString);
                    int code = PPApplication.ppFromString(errorBodyString, "code", PPApplication.PPValueType.INT).getAsInt();
                    if (code < 0) {
                        //用户自定义错误
                        String error = PPApplication.ppFromString(errorBodyString, "error").getAsString();
                        Log.v("ppLog", "http exception:" + error);
                        PPApplication.showError("http exception:" + error);
                        if (code == -1000) {
                            PPApplication.logout();
                        }
                    } else {
                        //http常规错误
                        Log.v("ppLog", "http exception:" + errorBodyString);
                        PPApplication.showError("http exception:" + errorBodyString);
                    }
                } else {
                    //执行callSuccess过程中错误
                    Log.v("ppLog", throwable.toString());
                    PPApplication.showError(throwable.toString());
                }
            } catch (Exception e) {
                //执行callFailure过程中错误
                Log.v("ppLog", e.toString());
                PPApplication.showError(e.toString());
            }
        }
    };

    public interface DoOnCallFailure {
        void needToDo();
    }

    public static class CallFailure {
        private DoOnCallFailure doOnCallFailure;
        private Consumer<Throwable> callFailure;

        public CallFailure(final DoOnCallFailure doOnCallFailure) {
            this.doOnCallFailure = doOnCallFailure;
            this.callFailure = new Consumer<Throwable>() {
                @Override
                public void accept(@NonNull Throwable throwable) {
                    doOnCallFailure.needToDo();
                    try {
                        if (throwable instanceof HttpException) {
                            //http非200返回code错误
                            HttpException exception = (HttpException) throwable;
                            String errorBodyString = exception.response().errorBody().string();
                            Log.v("ppLog", errorBodyString);
                            int code = PPApplication.ppFromString(errorBodyString, "code", PPApplication.PPValueType.INT).getAsInt();
                            if (code < 0) {
                                //用户自定义错误
                                String error = PPApplication.ppFromString(errorBodyString, "error").getAsString();
                                Log.v("ppLog", "http exception:" + error);
                                PPApplication.showError("http exception:" + error);
                                if (code == -1000) {
                                    PPApplication.logout();
                                }
                            } else {
                                //http常规错误
                                Log.v("ppLog", "http exception:" + errorBodyString);
                                PPApplication.showError("http exception:" + errorBodyString);
                            }
                        } else {
                            //执行callSuccess过程中错误
                            Log.v("ppLog", throwable.toString());
                            PPApplication.showError(throwable.toString());
                        }
                    } catch (Exception e) {
                        //执行callFailure过程中错误
                        Log.v("ppLog", e.toString());
                        PPApplication.showError(e.toString());
                    }
                }
            };
        }

        public Consumer<Throwable> getCallFailure() {
            return this.callFailure;
        }
    }

    @BindingAdapter({"bind:avatar_image_name"})
    public static void setAvatarImageName(final ImageView imageView, String imageName) {
        setImageViewResource(imageView, imageName, 80);
    }

    public static void setImageViewResource(final ImageView imageView, String pic, int size) {
        Picasso.with(appContext)
                .load(getImageUrl(pic, size))
                .placeholder(android.R.drawable.stat_notify_sync)
                .error(android.R.drawable.stat_notify_error)
                .into(imageView);
    }

    private static String getImageUrl(String imageName, int size) {
        //如果为空则用默认图片
        if (TextUtils.isEmpty(imageName)) {
            imageName = "default";
        }

        if (imageName.startsWith("http")) {
            //如果直接是网络地址则直接用
            return imageName;
        } else {
            String result = qiniuBase + imageName + "?imageView2/1/w/" + size + "/h/" + size + "/interlace/1/";
            Log.v("ppLog", "image url:" + result);

            return result;
        }
    }

    public static Observable<String> uploadSingleImage(final byte[] data, final String key, final String token) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                uploadManager.put(data, key, token,
                        new UpCompletionHandler() {
                            @Override
                            public void complete(String key, ResponseInfo info, JSONObject res) {
                                //res包含hash、key等信息，具体字段取决于上传策略的设置
                                if (info.isOK()) {
                                    Log.i("qiniu", "Upload Success");
                                    emitter.onNext(key);
                                    emitter.onComplete();
                                } else {
                                    Log.i("qiniu", "Upload Fail");
                                    //如果失败，这里可以把info信息上报自己的服务器，便于后面分析上传错误原因
                                    Exception apiError = new Exception("七牛上传:" + key + "失败", new Throwable(info.error.toString()));
                                    emitter.onError(apiError);
                                }
                            }
                        },
                        new UploadOptions(null, null, false,
                                new UpProgressHandler() {
                                    public void progress(String key, double percent) {
                                        Log.i("qiniu", key + ": " + percent);
                                    }
                                }, null));
            }
        });
    }

    public static String[] getCurGeo() {
        //todo replace fake geo
        return new String[]{"121.0", "31.0"};
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }

    public static void setupUI(final Activity activity, final View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setFocusable(true);
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(activity);

                    view.requestFocus();

                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(activity, innerView);
            }
        }
    }

    public static void updateAvatar(final String userId, final String avatar) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    RMRelatedUser obj = realm.where(RMRelatedUser.class).equalTo("_id", userId).findFirst();

                    if (obj != null) {
                        obj.avatar = avatar;
                    }
                }
            });
        }
    }
}
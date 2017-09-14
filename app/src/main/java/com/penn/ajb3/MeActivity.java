package com.penn.ajb3;

import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoActivity;
import com.jph.takephoto.compress.CompressConfig;
import com.jph.takephoto.model.TResult;
import com.penn.ajb3.databinding.ActivityMeBinding;
import com.penn.ajb3.realm.RMMyProfile;
import com.penn.ajb3.util.PPRetrofit;
import com.qiniu.android.common.FixedZone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.ObjectChangeSet;
import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.RealmObjectChangeListener;

public class MeActivity extends TakePhotoActivity {

    private ActivityMeBinding binding;
    private Realm realm;
    private RMMyProfile rmMyProfile;
    private TakePhoto takePhoto;
    private CompressConfig config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_me);

        setup();
    }

    private void setup() {
        takePhoto = getTakePhoto();
        config = new CompressConfig.Builder()
                .setMaxSize(4096000)
                .setMaxPixel(4096)
                .create();

        realm = Realm.getDefaultInstance();
        rmMyProfile = realm.where(RMMyProfile.class).findFirst();
        rmMyProfile.addChangeListener(new RealmObjectChangeListener<RMMyProfile>() {
            @Override
            public void onChange(RMMyProfile realmModel, ObjectChangeSet changeSet) {
                String[] changeFields = changeSet.getChangedFields();
                String[] expectChangeFields = {"nickname", "sex", "avatar"};
                for (String item : expectChangeFields) {
                    if (Arrays.asList(changeFields).contains(item)) {
                        binding.setData(realmModel);
                        break;
                    }
                }
            }
        });

        binding.mainCiv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto.onEnableCompress(config, true);
                takePhoto.onPickFromDocuments();
            }
        });

        binding.setData(rmMyProfile);
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    @Override
    public void takeCancel() {
        super.takeCancel();
        Log.v("ppLog", "takeCancel");
    }

    @Override
    public void takeFail(TResult result, String msg) {
        super.takeFail(result, msg);
        Log.v("ppLog", "takeFail");
    }

    @Override
    public void takeSuccess(TResult result) {
        super.takeSuccess(result);
        if (result == null) {
            Log.v("ppLog", "takeSuccess null");
        }
        String path = result.getImage().getCompressPath();

        Log.v("ppLog", "getCompressPath:" + path);

        File file = new File(path);
        int size = (int) file.length();
        final byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Log.v("ppLog", "takeSuccess:path:" + path + ", size:" + bytes.length);

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        binding.mainCiv.setImageBitmap(bitmap);

        Observable<String> updateAvatarResult =
                PPRetrofit.getInstance().getPPService().getQiniuToken()
                        .observeOn(Schedulers.newThread())
                        .flatMap(new Function<Object, Observable<String>>() {
                            @Override
                            public Observable<String> apply(@io.reactivex.annotations.NonNull Object sObj) throws Exception {
                                String s = sObj.toString();
                                String tokenStr = PPApplication.ppFromString(s, "token").getAsString();
                                String key = PPApplication.uuid + "_" + System.currentTimeMillis();

                                return PPApplication.uploadSingleImage(bytes, key, tokenStr);
                            }
                        })
                        .observeOn(Schedulers.newThread())
                        .flatMap(new Function<String, Observable<String>>() {
                            @Override
                            public Observable<String> apply(@io.reactivex.annotations.NonNull String s) throws Exception {
                                return PPRetrofit.getInstance().getPPService().updateAvatar(s);
                            }
                        });

        PPApplication.apiRequest(updateAvatarResult, PPApplication.callSuccess, PPApplication.callFailure, null);
    }
}

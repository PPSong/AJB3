package com.penn.ajb3;

import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
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
        Log.v("ppLog", "MeActivity onCreate");
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

        ExifInterface exif = null;
        try {
            exif = new ExifInterface(file.getPath());
        } catch (IOException e) {
            Log.v("ppLog", "takeFail:" + e.toString());
            e.printStackTrace();

            return;
        }
        int orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);

        int angle = 0;

        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            angle = 90;
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
            angle = 180;
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            angle = 270;
        }

        Matrix mat = new Matrix();
        mat.postRotate(angle);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;

        Bitmap oldBmp = null;
        try {
            oldBmp = BitmapFactory.decodeStream(new FileInputStream(file),
                    null, options);
        } catch (FileNotFoundException e) {
            Log.v("ppLog", "takeFail:" + e.toString());
            e.printStackTrace();

            return;
        }

        Bitmap newBmp = Bitmap.createBitmap(oldBmp, 0, 0, oldBmp.getWidth(),
                oldBmp.getHeight(), mat, true);

        binding.mainCiv.setImageBitmap(newBmp);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        newBmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        final byte[] bytes = stream.toByteArray();
        Log.v("ppLog", "bytes:" + bytes.length);

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

package com.penn.ajb3;

import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoActivity;
import com.jph.takephoto.compress.CompressConfig;
import com.jph.takephoto.model.TResult;
import com.penn.ajb3.databinding.ActivityPhotoBinding;
import com.penn.ajb3.util.PPRetrofit;
import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

import static com.penn.ajb3.AllUsersActivity.RelatedUserListAdapter.LOADING_NOT_START;
import static com.penn.ajb3.AllUsersActivity.RelatedUserListAdapter.LOAD_ALL;
import static com.penn.ajb3.AllUsersActivity.RelatedUserListAdapter.LOAD_FAILED;
import static com.penn.ajb3.PPApplication.ppFromString;

public class PhotoActivity extends TakePhotoActivity {

    private TakePhoto takePhoto;
    private ActivityPhotoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_photo);

        takePhoto = getTakePhoto();
        CompressConfig config = new CompressConfig.Builder()
                .setMaxSize(4096000)
                .setMaxPixel(4096)
                .create();
        takePhoto.onEnableCompress(config, true);
        takePhoto.onPickFromDocuments();
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
//        byte[] tmpByte = result.getImage().getCompressPath().getBytes();
        String path = result.getImage().getCompressPath();

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

        binding.mainIv.setImageBitmap(bitmap);

//        Picasso.with(this)
//                .load(new File(path))
//                .placeholder(android.R.drawable.ic_menu_myplaces)
//                .error(android.R.drawable.stat_notify_error)
//                .into(binding.mainIv);

        Observable<String> tokenResult = PPRetrofit.getInstance().getPPService().getQiniuToken();

        Consumer<Object> callSuccess = new Consumer<Object>() {
            @Override
            public void accept(@NonNull final Object sObj) throws Exception {
                String s = sObj.toString();
                String tokenStr = PPApplication.ppFromString(s, "token").getAsString();
                Log.v("ppLog", "s:" + PPApplication.ppFromString(s, "token").getAsString());

                Configuration config = new Configuration.Builder()
                        .zone(FixedZone.zone0)
                        .build();
                UploadManager uploadManager = new UploadManager(config);

                byte[] data = bytes;
                String key = PPApplication.uuid + "_" + System.currentTimeMillis();
                String token = tokenStr;

                uploadManager.put(data, key, token,
                        new UpCompletionHandler() {
                            @Override
                            public void complete(String key, ResponseInfo info, JSONObject res) {
                                //res包含hash、key等信息，具体字段取决于上传策略的设置
                                if (info.isOK()) {
                                    Log.i("qiniu", "Upload Success");
                                } else {
                                    Log.i("qiniu", "Upload Fail");
                                    //如果失败，这里可以把info信息上报自己的服务器，便于后面分析上传错误原因
                                }
                                Log.i("qiniu", key + ",\r\n " + info + ",\r\n " + res);
                            }
                        },
                        new UploadOptions(null, null, false,
                                new UpProgressHandler() {
                                    public void progress(String key, double percent) {
                                        Log.i("qiniu", key + ": " + percent);
                                    }
                                }, null));
            }
        };

        PPApplication.DoOnCallFailure doOnCallFailure = new PPApplication.DoOnCallFailure() {
            @Override
            public void needToDo() {

            }
        };

        Consumer<Throwable> callFailure = new PPApplication.CallFailure(doOnCallFailure).getCallFailure();

        PPApplication.apiRequest(tokenResult, callSuccess, callFailure, null);


    }
}

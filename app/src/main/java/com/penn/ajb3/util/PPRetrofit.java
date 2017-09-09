package com.penn.ajb3.util;

import com.penn.ajb3.PPApplication;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by penn on 31/08/2017.
 */

public class PPRetrofit {
    private static PPRetrofit instance = null;
    public static final String BASE = "http://192.168.8.100";
//    public static final String BASE = "http://192.168.100.100";
//    public static final String BASE = "http://192.168.100.103";
    public static final String BASE_URL = BASE + ":3000/";
    public static final String SOCKET_URL = BASE + ":3001";

    private PPService ppService;

    public static PPRetrofit getInstance() {
        if (instance == null) {
            instance = new PPRetrofit();
        }

        return instance;
    }

    private PPRetrofit() {
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        builder.readTimeout(5, TimeUnit.SECONDS);
        builder.connectTimeout(5, TimeUnit.SECONDS);
        builder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                String token = "JWT " + PPApplication.getPrefStringValue(PPApplication.AUTH_BODY, "");
                Request request = chain.request().newBuilder().addHeader("Authorization", token).build();
                return chain.proceed(request);
            }
        });

        OkHttpClient client = builder.build();

        Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(new PPConverterFactory())
                .client(client)
                .baseUrl(BASE_URL)
                .build();

        ppService = retrofit.create(PPService.class);
    }

    public PPService getPPService() {
        return ppService;
    }
}

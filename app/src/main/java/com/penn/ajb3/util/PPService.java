package com.penn.ajb3.util;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Created by penn on 31/08/2017.
 */

public interface PPService {
    @FormUrlEncoded
    @POST("users/login")
    Observable<String> login(
            @Field("username") String username,
            @Field("password") String password
    );

    @POST("getOtherUsers")
    Observable<String> getOtherUsers();
}

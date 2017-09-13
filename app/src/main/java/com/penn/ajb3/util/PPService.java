package com.penn.ajb3.util;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

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

    @POST("getOtherUsers/{afterUsername}")
    Observable<String> getOtherUsers(@Path("afterUsername") String afterUsername);

    @POST("getNewFollows/{startTime}")
    Observable<String> getNewFollows(@Path("startTime") long startTime);

    @POST("getNewFans/{startTime}")
    Observable<String> getNewFans(@Path("startTime") long startTime);

    @POST("getNewFriends/{startTime}")
    Observable<String> getNewFriends(@Path("startTime") long startTime);

    @POST("getMyProfile")
    Observable<String> getMyProfile();

    @POST("unFollow/{userId}")
    Observable<String> unFollow(@Path("userId") String userId);

    @POST("unFriend/{userId}")
    Observable<String> unFriend(@Path("userId") String userId);

    @POST("follow/{userId}")
    Observable<String> follow(@Path("userId") String userId);

    @POST("getQiniuToken")
    Observable<String> getQiniuToken();
}

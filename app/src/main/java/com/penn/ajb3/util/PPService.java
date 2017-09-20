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

    @POST("getNewBlocks/{startTime}")
    Observable<String> getNewBlocks(@Path("startTime") long startTime);

    @POST("getMyProfile")
    Observable<String> getMyProfile();

    @POST("unFollow/{userId}")
    Observable<String> unFollow(@Path("userId") String userId);

    @POST("unFriend/{userId}")
    Observable<String> unFriend(@Path("userId") String userId);

    @POST("follow/{userId}")
    Observable<String> follow(@Path("userId") String userId);

    @POST("block/{userId}")
    Observable<String> block(@Path("userId") String userId);

    @POST("unBlock/{userId}")
    Observable<String> unBlock(@Path("userId") String userId);

    @POST("getQiniuToken")
    Observable<String> getQiniuToken();

    @POST("updateAvatar/{avatarImageName}")
    Observable<String> updateAvatar(@Path("avatarImageName") String avatarImageName);

    @POST("getMessage/{lnt}/{lat}/{startTime}")
    Observable<String> getMessage(@Path("lnt") String lnt, @Path("lat") String lat, @Path("startTime") long startTime);

    @POST("sendMessage/{id}/{body}/{createTime}/{lnt}/{lat}")
    Observable<String> sendMessage(@Path("id") String id, @Path("body") String body, @Path("createTime") long createTime, @Path("lnt") String lnt, @Path("lat") String lat);

    @POST("getMoment/{lnt}/{lat}/{startTime}")
    Observable<String> getMoment(@Path("lnt") String lnt, @Path("lat") String lat, @Path("startTime") long startTime);

    @POST("getMomentDetail/{momentId}")
    Observable<String> getMomentDetail(@Path("momentId") String momentId);

    @POST("getComments/{momentId}")
    Observable<String> getComments(@Path("momentId") String momentId);

    @POST("test")
    Observable<String> test();
}

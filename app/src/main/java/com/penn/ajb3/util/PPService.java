package com.penn.ajb3.util;

import java.util.ArrayList;

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
            @Field("_id") String _id,
            @Field("password") String password
    );

    @POST("getOtherUsers/{afterUserId}")
    Observable<String> getOtherUsers(@Path("afterUserId") String afterUserId);

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

    @FormUrlEncoded
    @POST("block")
    Observable<String> block(@Field("userIds") String userIds);

    @FormUrlEncoded
    @POST("unBlock")
    Observable<String> unBlock(@Field("userIds") String userIds);

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

    @POST("getMyMoment/{startTime}")
    Observable<String> getMyMoment(@Path("startTime") long startTime);

    @POST("getMomentDetail/{momentId}")
    Observable<String> getMomentDetail(@Path("momentId") String momentId);

    @POST("sendComment/{id}/{momentId}/{body}/{createTime}")
    Observable<String> sendComment(@Path("id") String id, @Path("momentId") String momentId, @Path("body") String body, @Path("createTime") long createTime);

    @POST("getComments/{momentId}")
    Observable<String> getComments(@Path("momentId") String momentId);

    @POST("deleteComment/{momentId}")
    Observable<String> deleteComment(@Path("momentId") String momentId);

    @POST("likeMoment/{momentId}")
    Observable<String> likeMoment(@Path("momentId") String momentId);

    @POST("unLikeMoment/{momentId}")
    Observable<String> unLikeMoment(@Path("momentId") String momentId);

    @POST("test")
    Observable<String> test();
}

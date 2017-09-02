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

    @POST("getOtherUsers")
    Observable<String> getOtherUsers();

    @POST("getNewFollows/{startTime}")
    Observable<String> getNewFollows(@Path("startTime") long startTime);

    @POST("getNewFans/{startTime}")
    Observable<String> getNewFans(@Path("startTime") long startTime);

    @POST("getNewFriends/{startTime}")
    Observable<String> getNewFriends(@Path("startTime") long startTime);

    @POST("getMyProfile")
    Observable<String> getMyProfile();


}

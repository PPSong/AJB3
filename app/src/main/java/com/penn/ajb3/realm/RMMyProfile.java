package com.penn.ajb3.realm;

import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 01/09/2017.
 */

public class RMMyProfile {
    @PrimaryKey
    public String _id;
    public String username;
    public String nickname;
    public String sex;
    public String avatar;
    public String token;
    public long updateTime;
}

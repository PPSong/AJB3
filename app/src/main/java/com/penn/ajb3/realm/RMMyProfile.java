package com.penn.ajb3.realm;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 01/09/2017.
 */

public class RMMyProfile extends RealmObject {
    @PrimaryKey
    public String _id;
    public String username;
    public String nickname;
    public String sex;
    public String avatar;
    public long updateTime;
    public long getNewFollowsTime;
    public long getNewFansTime;
    public long getNewFriendsTime;

}

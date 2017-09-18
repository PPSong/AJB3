package com.penn.ajb3.realm;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 17/09/2017.
 */

public class RMNearMoment extends RealmObject {
    @PrimaryKey
    public String _id;
    public String userId;
    public String nickname;
    public String avatar;
    public String body;
    public long createTime;
}

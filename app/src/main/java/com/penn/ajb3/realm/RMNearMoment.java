package com.penn.ajb3.realm;

import com.penn.ajb3.util.PPRetrofit;

import io.reactivex.Observable;
import io.reactivex.Observer;
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
    public String image;
    public boolean like;
    public long createTime;

    public String getLikeStr() {
        if (like) {
            return "like";
        } else {
            return "not like";
        }
    }

    public Observable<String> toggleLike() {
        if (like) {
            return PPRetrofit.getInstance().getPPService().unLikeMoment(_id);
        } else {
            return PPRetrofit.getInstance().getPPService().likeMoment(_id);
        }
    }
}

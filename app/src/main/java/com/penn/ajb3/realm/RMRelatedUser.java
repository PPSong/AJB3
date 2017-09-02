package com.penn.ajb3.realm;

import android.util.Log;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 01/09/2017.
 */

public class RMRelatedUser extends RealmObject {
    @PrimaryKey
    public String _id;
    public String username;
    public String nickname;
    public String sex;
    public String avatar;
    public long updateTime;

    public boolean isFollows;
    public boolean isFans;
    public boolean isFriends;

    public boolean delete() {
        Log.v("ppLog", "delete1 1");
        if (!isFollows && !isFans && !isFriends) {
            Log.v("ppLog", "delete1 2");
            deleteFromRealm();
            return true;
        } else {
            return false;
        }
    }

    public boolean delete2() {
        Log.v("ppLog", "delete2 1");
        if (!isFollows && !isFans && !isFriends) {
            Log.v("ppLog", "delete2 2");
            deleteFromRealm();
            return true;
        } else {
            return false;
        }
    }

    public String followState() {
        Log.v("ppLog", "followState");

        if (isFriends) {
            return "互相关注";
        } else {
            return "已关注";
        }
    }
}

package com.penn.ajb3.realm;

import android.util.Log;
import android.view.View;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 01/09/2017.
 */

public class RMRelatedUser extends RealmObject {
    @PrimaryKey
    public String _id;
    public String nickname;
    public String sex;
    public String avatar;
    public long updateTime;

    public boolean isFollows;
    public boolean isFans;
    public boolean isFriends;

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public boolean isFollows() {
        return isFollows;
    }

    public void setFollows(boolean follows) {
        isFollows = follows;
    }

    public boolean isFans() {
        return isFans;
    }

    public void setFans(boolean fans) {
        isFans = fans;
    }

    public boolean isFriends() {
        return isFriends;
    }

    public void setFriends(boolean friends) {
        isFriends = friends;
    }

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

    public String followState() {
        Log.v("ppLog", "followState");

        if (isFriends) {
            return "互相关注";
        } else {
            return "已关注";
        }
    }

    public String fansState() {
        Log.v("ppLog", "fansState");

        if (isFriends) {
            return "互相关注";
        } else {
            return "已关注你";
        }
    }

    public int unFollowable() {
        return (isFollows && !isFriends) ? View.VISIBLE : View.INVISIBLE;
    }

    public int unFriendable() {
        return isFriends ? View.VISIBLE : View.INVISIBLE;
    }

    public int followable() {
        return (isFans && !isFollows && !isFriends) ? View.VISIBLE : View.INVISIBLE;
    }
}

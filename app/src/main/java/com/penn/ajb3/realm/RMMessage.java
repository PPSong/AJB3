package com.penn.ajb3.realm;

import android.view.View;

import com.penn.ajb3.PPApplication;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 17/09/2017.
 */

public class RMMessage extends RealmObject {
    @PrimaryKey
    public String _id;
    public String userId;
    public String nickname;
    public String avatar;
    public String body;
    public long createTime;
    public String status;

    public String createTimeStr() {
        return "" + createTime;
    }
}

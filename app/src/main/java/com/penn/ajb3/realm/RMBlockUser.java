package com.penn.ajb3.realm;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 15/09/2017.
 */

public class RMBlockUser extends RealmObject {
    @PrimaryKey
    public String _id;
    public String ownerUserId;
    public String targetUserId;
}

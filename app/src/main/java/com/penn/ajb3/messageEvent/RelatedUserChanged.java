package com.penn.ajb3.messageEvent;

import java.util.ArrayList;

/**
 * Created by penn on 03/09/2017.
 */

public class RelatedUserChanged {
    public ArrayList<String> userIds;

    public RelatedUserChanged(ArrayList<String> userIds) {
        this.userIds = userIds;
    }
}

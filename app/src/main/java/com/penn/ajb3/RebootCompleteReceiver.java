package com.penn.ajb3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.penn.ajb3.util.SocketService;

public class RebootCompleteReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("ppLog", "Restarted");
        Intent i = new Intent(context, SocketService.class);
        context.startService(i);
    }
}

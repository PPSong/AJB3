package com.penn.ajb3.util;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.penn.ajb3.PPApplication;
import com.penn.ajb3.messageEvent.UserLogout;
import com.penn.ajb3.messageEvent.UserSignIn;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.URISyntaxException;
import java.util.ArrayList;

import static com.github.nkzawa.socketio.client.Socket.EVENT_CONNECT;
import static com.github.nkzawa.socketio.client.Socket.EVENT_DISCONNECT;
import static com.penn.ajb3.PPApplication.ppFromString;

/**
 * Created by penn on 06/09/2017.
 */

public class SocketService extends Service {

    private com.github.nkzawa.socketio.client.Socket socket;

    private Runnable mStatusChecker;

    private int mInterval = 5000; // 5 seconds by default, can be changed later
    private Handler mHandler;

    private boolean started;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void UserSignIn(UserSignIn event) {
        startSocket();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void UserLogout(UserLogout event) {
        stopSocket();
    }

    private void startSocket() {
        if (started) {
            //防止重复启动
            return;
        }

        startRepeatingTask();
        try {
            socket = IO.socket(PPRetrofit.SOCKET_URL);
        } catch (URISyntaxException e) {
            Log.v("ppLog", "socket exception:" + e.toString());
        }

        socket.on("pushEvent", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                String typeString = args[0].toString();
                PPApplication.getPush(ppFromString(typeString, "type").getAsString());
            }
        });

        socket.on("needYourUserId", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                socket.emit("giveMyUserId", PPApplication.getPrefStringValue("MY_ID", "NONE"));
            }
        });

        socket.on(EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.v("ppLog", "EVENT_CONNECT");
                String myId = PPApplication.getPrefStringValue(PPApplication.MY_ID, "NONE");
                if (!(myId.equals("NONE"))) {
                    PPApplication.reconnectToServer();
                }
            }

        });

        socket.on(EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.v("ppLog", "EVENT_DISCONNECT");
            }

        });

        //todo 如果没连接成功, 是否会一直尝试连接
        socket.connect();
        //这里只要保证都在主线程执行, 不用担心started这个变量的线程安全性
        started = true;
    }

    private void stopSocket() {
        stopRepeatingTask();
        socket.close();
        socket = null;
        started = false;
    }

    //Private constructor prevents instantiating and subclassing
    public SocketService() {
        EventBus.getDefault().register(this);

        mHandler = new Handler();

        mStatusChecker = new Runnable() {
            @Override
            public void run() {
                try {
                    if (socket != null && socket.connected()) {
                        String myId = PPApplication.getPrefStringValue(PPApplication.MY_ID, "NONE");
                        if (!(myId.equals("NONE"))) {
                            socket.emit("heartBeat", myId);
                        }
                    }
                } catch (Exception err) {
                    Log.v("ppLog", "mStatusChecker:" + err.toString());
                } finally {
                    // 100% guarantee that this always happens, even if
                    // your update method throws an exception
                    mHandler.postDelayed(mStatusChecker, mInterval);
                }
            }
        };

        //在强行退出或手机重启后, 如果getPrefStringValue中MY_ID有值, 说明在用户已在登录状态
        String myId = PPApplication.getPrefStringValue(PPApplication.MY_ID, "NONE");
        if (!(myId.equals("NONE"))) {
            startSocket();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void startRepeatingTask() {
        mStatusChecker.run();
    }

    private void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}

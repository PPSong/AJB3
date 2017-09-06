package com.penn.ajb3.util;

import android.os.Handler;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.penn.ajb3.PPApplication;

import java.net.URISyntaxException;

import static com.github.nkzawa.socketio.client.Socket.EVENT_CONNECT;
import static com.github.nkzawa.socketio.client.Socket.EVENT_DISCONNECT;
import static com.penn.ajb3.PPApplication.ppFromString;

/**
 * Created by penn on 06/09/2017.
 */

public class SocketService {
    private static SocketService instance;

    private com.github.nkzawa.socketio.client.Socket socket;

    private int mInterval = 5000; // 5 seconds by default, can be changed later
    private Handler mHandler;

    //Private constructor prevents instantiating and subclassing
    private SocketService() {
        mHandler = new Handler();
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
                PPApplication.reconnectToServer();
            }

        });

        socket.on(EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.v("ppLog", "EVENT_DISCONNECT");
            }

        });

        socket.connect();
    }

    //Static 'instance' method
    public static SocketService getInstance() {
        if (instance == null) {
            instance = new SocketService();
        }
        return instance;
    }

    public void close() {
        stopRepeatingTask();
        socket.close();
        socket = null;
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                socket.emit("heartBeat", PPApplication.getPrefStringValue("USERNAME", "NONE"));
            } catch (Exception err) {
                Log.v("ppLog", "mStatusChecker:" + err.toString());
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    private void startRepeatingTask() {
        mStatusChecker.run();
    }

    private void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

}

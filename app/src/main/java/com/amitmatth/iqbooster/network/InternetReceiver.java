package com.amitmatth.iqbooster.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class InternetReceiver extends BroadcastReceiver {
    public static final String ACTION_INTERNET_DISCONNECTED = "com.amitmatth.iqbooster.INTERNET_DISCONNECTED";
    public static final String ACTION_INTERNET_CONNECTED = "com.amitmatth.iqbooster.INTERNET_CONNECTED";

    private static Boolean wasConnected = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            boolean isConnectedNow = NetworkUtils.isInternetAvailable(context);

            if (wasConnected == null) {
                wasConnected = isConnectedNow;
                if (!isConnectedNow) {
                    Intent disconnectedIntent = new Intent(ACTION_INTERNET_DISCONNECTED);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(disconnectedIntent);
                }
                return;
            }

            if (wasConnected && !isConnectedNow) {
                Intent disconnectedIntent = new Intent(ACTION_INTERNET_DISCONNECTED);
                LocalBroadcastManager.getInstance(context).sendBroadcast(disconnectedIntent);
            } else if (!wasConnected && isConnectedNow) {
                Toast.makeText(context, "Internet is back!", Toast.LENGTH_SHORT).show();
                Intent connectedIntent = new Intent(ACTION_INTERNET_CONNECTED);
                LocalBroadcastManager.getInstance(context).sendBroadcast(connectedIntent);
            }
            wasConnected = isConnectedNow;
        }
    }

    public static void resetConnectionState() {
        wasConnected = null;
    }
}
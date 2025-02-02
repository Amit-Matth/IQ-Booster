package com.amitmatth.iqbooster.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.widget.Toast;

public class InternetReceiver extends BroadcastReceiver {
    private static boolean isFirstCheck = true;  // Ignore the first check when app starts
    private static boolean wasDisconnected = false;  // Track if internet was lost

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null &&
                intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

            boolean isConnected = NetworkUtils.isInternetAvailable(context);

            // Ignore the first check on app launch
            if (isFirstCheck) {
                isFirstCheck = false;
                return;
            }

            // If internet is lost, set flag
            if (!isConnected) {
                wasDisconnected = true;
                Toast.makeText(context, "Please connect to internet connection!", Toast.LENGTH_SHORT).show();
            }
            // If internet is back and was previously lost, show the toast
            else if (wasDisconnected) {
                wasDisconnected = false;  // Reset flag
                Toast.makeText(context, "Internet is back!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

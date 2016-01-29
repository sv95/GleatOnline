package com.example.sonal.gleatonline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to listen for broadcasts to monitor connectivity changes
 */
public class ConnectionChangeReceiver extends BroadcastReceiver {

    public static final String CONNECTIVITY_ACTION_LOLLIPOP = "com.example.sonal.gleatonline.CONNECTIVITY_ACTION_LOLLIPOP";

    protected List<NetworkStateReceiverListener> listeners;
    protected Boolean connected;

    public ConnectionChangeReceiver() {
        listeners = new ArrayList<NetworkStateReceiverListener>();
        connected = null;
    }

    public void onReceive(Context context, Intent intent) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && TextUtils.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION) ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && TextUtils.equals(intent.getAction(), CONNECTIVITY_ACTION_LOLLIPOP)) {

            if (intent.getExtras() != null) {
                final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                    Log.d("receiver test", "detected on");
                }

                Log.d("receiver test", Boolean.toString(intent.getExtras().getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY)));
                if (intent.getExtras().getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                    Log.d("receiver test", "detected off");
                }
            }
        }

        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = manager.getActiveNetworkInfo();

        if (ni != null && ni.getState() == NetworkInfo.State.CONNECTED) {
            connected = true;
        } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
            connected = false;
        }

        notifyStateToAll();
    }

    private void notifyStateToAll() {
        for (NetworkStateReceiverListener listener : listeners)
            notifyState(listener);
    }

    private void notifyState(NetworkStateReceiverListener listener) {
        if (connected == null || listener == null) {
            return;
        }
        if (connected) {
            listener.networkAvailable();
        } else {
            listener.networkUnavailable();
        }
    }

    public void addListener(NetworkStateReceiverListener l) {
        listeners.add(l);
        notifyState(l);
    }

    public void removeListener(NetworkStateReceiverListener l) {
        listeners.remove(l);
    }

    public interface NetworkStateReceiverListener {
        public void networkAvailable();

        public void networkUnavailable();
    }
}
package com.example.sonal.gleatonline;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * The fragment to check and display internet connectivity
 */
public class ConnectivityCheckerFragment extends Fragment implements ConnectionChangeReceiver.NetworkStateReceiverListener {

    Activity parentActivity;
    TextView mConnectivityTV;
    ImageView mConnectivityIV;
    ConnectionChangeReceiver ccReceiver;
    IntentFilter intentFilter;
    ConnectivityManager.NetworkCallback lollipopCallback;
    private String cStatus = "Connectivity status:\n";

    public ConnectivityCheckerFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_connection_checker, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mConnectivityTV = (TextView) parentActivity.findViewById(R.id.connectivityTextView);
        mConnectivityIV = (ImageView) parentActivity.findViewById(R.id.connectivityImageView);
        ccReceiver = new ConnectionChangeReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(ConnectionChangeReceiver.CONNECTIVITY_ACTION_LOLLIPOP);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        parentActivity = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        parentActivity = null;
    }

    public void networkAvailable() {
        mConnectivityTV.setText(cStatus + "Connected");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mConnectivityIV.setImageDrawable(getResources().getDrawable(R.drawable.connected, parentActivity.getApplicationContext().getTheme()));
        } else {
            mConnectivityIV.setImageDrawable(getResources().getDrawable(R.drawable.connected));
        }
    }

    public void networkUnavailable() {
        mConnectivityTV.setText(cStatus + "Not connected");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mConnectivityIV.setImageDrawable(getResources().getDrawable(R.drawable.not_connected, parentActivity.getApplicationContext().getTheme()));
        } else {
            mConnectivityIV.setImageDrawable(getResources().getDrawable(R.drawable.not_connected));
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void registerConnectivityActionLollipop() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) parentActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        lollipopCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Intent intent = new Intent(ConnectionChangeReceiver.CONNECTIVITY_ACTION_LOLLIPOP);
                intent.putExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, true);

                parentActivity.sendBroadcast(intent);
            }

            @Override
            public void onLost(Network network) {
                Intent intent = new Intent(ConnectionChangeReceiver.CONNECTIVITY_ACTION_LOLLIPOP);
                intent.putExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

                parentActivity.sendBroadcast(intent);
            }
        };
        connectivityManager.registerNetworkCallback(builder.build(), lollipopCallback);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void unregisterConnectivityActionLollipop() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) parentActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.unregisterNetworkCallback(lollipopCallback);
    }

    //Necessary to override onPause() and onResume() to avoid memory leaks
    @Override
    public void onPause() {
        super.onPause();
        ccReceiver.removeListener(this);
        parentActivity.unregisterReceiver(ccReceiver);
        unregisterConnectivityActionLollipop();
    }

    @Override
    public void onResume() {
        super.onResume();
        ccReceiver.addListener(this);
        parentActivity.registerReceiver(ccReceiver, intentFilter);
        registerConnectivityActionLollipop();
    }
}
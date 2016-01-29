package com.example.sonal.gleatonline;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.parse.Parse;
import com.parse.ParseFacebookUtils;

/**
 * Created by Sonal on 23/01/2016.
 */
public class ParseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FacebookSdk.sdkInitialize(getApplicationContext());
        Parse.enableLocalDatastore(getApplicationContext());
        Parse.initialize(getApplicationContext());
        ParseFacebookUtils.initialize(getApplicationContext());
    }
}
package com.example.sonal.gleatonline;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.parse.ParseUser;

/**
 * Use a splash screen while checking is currentUser == null
 * This will take the user to wither LoginActivity or MainActivity
 * This check could be done in LoginActivity itself but this enhances the UX
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //postDelayed will run the code provided in run() after SPLASH_TIME_OUT milliseconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ParseUser mCurrentUser = ParseUser.getCurrentUser();
                Intent intent;
                //Create the correct intent
                if (mCurrentUser == null) {
                    //no user is logged in
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                }
                startActivity(intent);
                //The SplashActivity is no longer needed
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}
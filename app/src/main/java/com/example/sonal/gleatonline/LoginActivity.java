package com.example.sonal.gleatonline;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.widget.LoginButton;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A login screen that offers login via email/password, or through Facebook.
 * All details are stored using parse.com
 */
public class LoginActivity extends Activity {

    private static final String LOG_TAG = LoginActivity.class.getSimpleName();
    LoginButton mFbButton;
    ParseUser parseUser;
    String name = null, email = "", birthday = "";
    ScrollView mLoginFormView;
    ProgressBar mProgressView;
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;

    public static Bitmap DownloadImageBitmap(String url) {
        Bitmap bm = null;
        try {
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error getting bitmap", e);
        }
        return bm;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ParseUser mCurrentUser = ParseUser.getCurrentUser();

        //if a user has not yet logged in, take them to the login screen
        //otherwise go straight to the MainAcitivty
        if (mCurrentUser == null) {
            //We could also use "user_birthday" as another parameter
            // but this requires Facebook to review the app and allow it to use this permission
            final List<String> mPermissions = Arrays.asList("public_profile", "email");

            mFbButton = (LoginButton) findViewById(R.id.fb_login_button);
            mFbButton.setReadPermissions(mPermissions);

            mLoginFormView = (ScrollView) findViewById(R.id.login_form);
            mProgressView = (ProgressBar) findViewById(R.id.login_progress);


            mFbButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //check whether or not user has internet access to log in with FB
                    ConnectivityManager cm = (ConnectivityManager) LoginActivity.this
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    if (activeNetwork != null &&
                            (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE || activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)) {
                        //User is connected to the internet
                        //show a loading circle while getting the data from Fb
                        //Otherwise the user will see the login screen while tasks are happening in the background
                        //and might think something went wrong
                        showProgress(true);
                        ParseFacebookUtils.logInWithReadPermissionsInBackground(LoginActivity.this, mPermissions, new LogInCallback() {
                            @Override
                            public void done(ParseUser user, ParseException err) {
                                if (err == null) {
                                    if (user == null) {
                                        showProgress(false);
                                        Log.d(LOG_TAG, "Uh oh. The user cancelled the Facebook login.");
                                    } else if (user.isNew()) {
                                        Log.d(LOG_TAG, "User signed up and logged in through Facebook!");
                                        getUserDetailsFromFB();
                                    } else {
                                        Log.d(LOG_TAG, "User logged in through Facebook!");
                                        getUserDetailsFromParse();
                                    }
                                } else {
                                    showProgress(false);
                                    err.printStackTrace();
                                }
                            }
                        });
                    } else {
                        //User is not connected to the internet
                        Toast.makeText(LoginActivity.this,
                                "You can't log in with Facebook without an internet connection",
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            });

            // Set up the login form.
            mEmailView = (EditText) findViewById(R.id.email);

            mPasswordView = (EditText) findViewById(R.id.password);
            mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                    if (id == getResources().getInteger(R.integer.login_ime_id) || id == EditorInfo.IME_NULL) {
                        hideKeyboard();
                        return true;
                    }
                    return false;
                }
            });

            Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
            mSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    hideKeyboard();
                    checkFieldValidity(0);
                }
            });

            Button mRegisterButton = (Button) findViewById(R.id.register_button);
            mRegisterButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    hideKeyboard();
                    checkFieldValidity(1);
                }
            });

            final CheckBox mCheckboxView = (CheckBox) findViewById(R.id.showPasswordCheckbox);
            mCheckboxView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (!isChecked) {
                        mPasswordView.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    } else {
                        // hide password
                        mPasswordView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    }
                }
            });

            hideKeyboard();
            //}
        } else {
            startMainActivity();
        }
    }

    private void getUserDetailsFromFB() {
        Bundle parameters = new Bundle();
        //add "birthday" if app has been reviewed for user_birthday permission by Facebook
        parameters.putString("fields", "email,name,picture");
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me",
                parameters,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
         /* handle the result */
                        try {
                            email = response.getJSONObject().getString("email");
                        } catch (JSONException e) {
                            //The user has refused to give the app their email through Fb
                            email = "";
                        }
                        try {
                            name = response.getJSONObject().getString("name");
                            Log.e(LOG_TAG, "name is " + name + " and email is " + email);
                            JSONObject picture = response.getJSONObject().getJSONObject("picture");
                            JSONObject data = picture.getJSONObject("data");
                            //  Returns a 50x50 profile picture
                            String pictureUrl = data.getString("url");
                            new ProfilePhotoAsync(pictureUrl).execute();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }

    private void getUserDetailsFromParse() {
        parseUser = ParseUser.getCurrentUser();
        Log.e(LOG_TAG, "email is " + parseUser.getEmail() + ", username is " + parseUser.getUsername() + ", DOB is " + parseUser.getString("dob"));
        Toast.makeText(LoginActivity.this, "Welcome back " + parseUser.getUsername(), Toast.LENGTH_SHORT).show();
        startMainActivity();
    }

    private void saveNewUser(Bitmap profileImageBitmap) {
        parseUser = ParseUser.getCurrentUser();
        parseUser.setUsername(name);
        parseUser.setEmail(email);
        parseUser.put("dob", birthday);
//        Saving profile photo as a ParseFile
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        profileImageBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        byte[] data = stream.toByteArray();
        String thumbName = parseUser.getUsername().replaceAll("\\s+", "");
        final ParseFile parseFile = new ParseFile(thumbName + "_thumb.jpg", data);
        //we can use saveInBackground() in this method because this data is from Fb
        //so the user must have an internet connection
        parseFile.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                parseUser.put("profileThumb", parseFile);
                //Finally save all the user details
                parseUser.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        Toast.makeText(LoginActivity.this, "New user: " + name + " Signed up", Toast.LENGTH_SHORT).show();
                        //Only start the next activity after this one is done
                        startMainActivity();
                    }
                });
            }
        });
    }

    private void startMainActivity() {
        Log.e(LOG_TAG, "start the online checker activity with the current user");
        Intent startMainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(startMainActivityIntent);
        Log.e(LOG_TAG, "finish the login activity because it's work is all done");
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }


    public void checkFieldValidity(int buttonClicked) {
        //if buttonClicked == 1 then we want to register, else we log in
        //either way we have to check the validity of the inputs
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            if (buttonClicked == 0) {
                registerUser(password);
            } else if (buttonClicked == 1) {
                attemptLogin(email, password);
            } else {
                Log.e(LOG_TAG, "Invalid argument for checkFieldValidity");
            }
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin(String email, String password) {
        Toast.makeText(this, "Logging in", Toast.LENGTH_SHORT).show();
        // perform the user login attempt.
        showProgress(true);
        ParseUser.logInInBackground(email, password, new LogInCallback() {
            public void done(ParseUser user, ParseException e) {
                if (user != null) {
                    startMainActivity();
                    // Hooray! The user is logged in.
                } else {
                    showProgress(false);
                    // Signup failed. Look at the ParseException to see what happened.
                    if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {
                        Toast.makeText(LoginActivity.this, "The username or password is incorrect. If you registered using Facebook, please sign in the same way.", Toast.LENGTH_SHORT).show();
                    } else if (e.getCode() == ParseException.CONNECTION_FAILED) {
                        //Unfortunately there's no logInEventually() method for obvious reasons -
                        //There's no way to tell if the user is old or new without checking the server
                        //so we wouldn't know what to display on the next activity
                        //It would be like signing in to Facebook without internet
                        //Instead we just ask the user to try again later, like most apps do
                        Toast.makeText(LoginActivity.this, "You don't have an internet connection. Please check your connectivity and try again.", Toast.LENGTH_SHORT).show();
                    } else if (e.getCode() == ParseException.INTERNAL_SERVER_ERROR) {
                        //Nothing we can do about a backend error when we don't control the backend
                        Toast.makeText(LoginActivity.this, "Unfortunately the server is down. Please try again in a few minutes.", Toast.LENGTH_SHORT).show();
                    } else {
                        //Allow testers to report bugs easily. Remove this before finalising code for production.
                        Toast.makeText(LoginActivity.this, "Sorry, something went wrong. Please tell the developer the error code is: Login_" + e.getCode(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    public void registerUser(String password) {
        Toast.makeText(this, "Registering user " + email + " with password " + password, Toast.LENGTH_SHORT).show();
        // perform the user registration attempt.

        ParseUser user = new ParseUser();
        //we set the username to be the email as it is a required field
        //we also set email to be the email so it follows the pattern from the Fb data
        user.setUsername(email);
        user.setPassword(password);
        user.setEmail(email);
        user.put("dob", birthday);

        user.signUpInBackground(new SignUpCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    // Hooray! Let them use the app now.
                    startMainActivity();
                } else {
                    // Sign up didn't succeed. Look at the ParseException
                    // to figure out what went wrong
                    if (e.getCode() == ParseException.USERNAME_TAKEN || e.getCode() == ParseException.EMAIL_TAKEN) {
                        Toast.makeText(LoginActivity.this, "This email is already associated with an account. If you registered using Facebook, use Facebook to sign in too.", Toast.LENGTH_SHORT).show();
                    } else if (e.getCode() == ParseException.CONNECTION_FAILED) {
                        //Unfortunately there's no logInEventually() method for obvious reasons -
                        //There's no way to tell if the user is old or new without checking the server
                        //so we wouldn't know what to display on the next activity
                        //It would be like signing in to Facebook without internet
                        //Instead we just ask the user to try again later, like most apps do
                        Toast.makeText(LoginActivity.this, "You don't have an internet connection. Please check your connectivity and try again.", Toast.LENGTH_SHORT).show();
                    } else if (e.getCode() == ParseException.INTERNAL_SERVER_ERROR) {
                        //Nothing we can do about a backend error when we don't control the backend
                        Toast.makeText(LoginActivity.this, "Unfortunately the server is down. Please try again in a few minutes.", Toast.LENGTH_SHORT).show();
                    } else {
                        //Allow testers to report bugs easily. Remove this before finalising code for production.
                        Toast.makeText(LoginActivity.this, "Sorry, something went wrong. Please tell the developer the error code is: Register_" + e.getCode(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    public void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            //if no view has focus the keyboard cannot be up so cannot be hidden
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();

    }

    private boolean isPasswordValid(String password) {
        String strongPasswordRegexp = "((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%!?£&*]).{6,20})";
        return Pattern.compile(strongPasswordRegexp).matcher(password).matches();
    }

    /**
     * Shows the progress UI and hides the login form.
     */

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        Log.e("showProgress", show ? "showing progress" : "unshowing progress");
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    class ProfilePhotoAsync extends AsyncTask<String, String, String> {
        public Bitmap bitmap;
        String url;

        public ProfilePhotoAsync(String url) {
            this.url = url;
        }

        @Override
        protected String doInBackground(String... params) {
            // Fetching data from URI and storing in bitmap
            bitmap = DownloadImageBitmap(url);
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            saveNewUser(bitmap);
        }
    }
}

package com.example.sonal.gleatonline;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MainActivity extends ActionBarActivity {

    //titles for data storage on Parse.com
    public static final String emailTitle = "email";
    public static final String phoneTitle = "phoneNumber";
    public static final String dobTitle = "dob";
    public static final String profilePicTitle = "profileThumb";
    //generic tag for logging outputs when using Log.e or Log.d etc
    private static String LOG_TAG = MainActivity.class.getSimpleName();
    ConnectivityCheckerFragment ccFragment;
    ImageView mProfilePicture;

    //whichever AlertDialog is shown and whether or not is needs to be displayed if the activity restarts
    AlertDialog alertDialog;
    boolean alertDialogIsShown;

    //which details need to be filled in in the backend database
    ArrayList<String> missingDetails;

    //does the user have a custom profile picture?
    boolean profileImageSet;

    //profilePicIsFresh iff user has selected a new profile picture but it hasn't been uploaded to the server yet
    boolean profilePicIsFresh;

    //has the log out button been pressed? Was the user already logging out when the activity was created?
    boolean loggingOut;
    boolean loggingOutFrmStart;

    ScrollView mMainView;
    ProgressBar mProgressView;
    private ParseUser mCurrentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(Html.fromHtml("<font color=\"red\">" + getString(R.string.app_name) + "</font>"));
        }
        setContentView(R.layout.activity_main);
        mMainView = (ScrollView) findViewById(R.id.main_activity_view);
        mProgressView = (ProgressBar) findViewById(R.id.logout_progress);
        loggingOut = false;
        loggingOutFrmStart = false;
        if (savedInstanceState != null) {
            loggingOut = savedInstanceState.getBoolean("loggingOut");
            loggingOutFrmStart = loggingOut;
        }
        if (!loggingOut) {
            mCurrentUser = ParseUser.getCurrentUser();

            TextView mWelcomeTV = (TextView) findViewById(R.id.welcomeTextview);
            mWelcomeTV.setText("Welcome. You're logged in as " + mCurrentUser.getUsername() + "\nNot you? Use the menu in the top right to log out.");
            mProfilePicture = (ImageView) findViewById(R.id.userAvatar);

            if (savedInstanceState == null) {
                ccFragment = new ConnectivityCheckerFragment();
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.main_fragment, ccFragment)
                        .commit();

                missingDetails = new ArrayList<>();
                //if the profile for the current user has missing details,
                // make sure they are filled in before continuing to the connectivity checker
                // The following if statements check which details need to be filled in
                //This cannot be done in a for loop with an interator over all possible details
                //because the method calls are different each time
                //e.g. getEmail(), getString(), getParseFile()
                if ("".equals(mCurrentUser.getEmail()) || mCurrentUser.getEmail() == null) {
                    //User signed up with Facebook but did not allow access to their email
                    Log.e("missing detail", emailTitle);
                    missingDetails.add(emailTitle);
                } else {
                    Log.e("not missing", mCurrentUser.getEmail());
                }
                if (mCurrentUser.getString("dob").equals("")) {
                    Log.e("missing detail", dobTitle);
                    missingDetails.add(dobTitle);
                } else {
                    Log.e("not missing", mCurrentUser.getString(dobTitle));
                }
                if (mCurrentUser.getParseFile(profilePicTitle) == null) {
                    Log.e("missing detail", profilePicTitle);
                    missingDetails.add(profilePicTitle);
                    profileImageSet = false;
                    TextView profilePicCaption = (TextView) findViewById(R.id.profilePictureCaption);
                    profilePicCaption.setText(getResources().getString(R.string.profile_picture_caption));
                } else {
                    profileImageSet = true;
                    try {
                        ParseFile parseFile = mCurrentUser.getParseFile("profileThumb");
                        byte[] data = parseFile.getData();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        mProfilePicture.setImageBitmap(bitmap);
                        TextView profilePicCaption = (TextView) findViewById(R.id.profilePictureCaption);
                        profilePicCaption.setText(getResources().getString(R.string.profile_pic_instruction));
                    } catch (ParseException pe) {
                        pe.printStackTrace();
                    }
                }
                if (mCurrentUser.getString(phoneTitle) == null) {
                    Log.e("missing detail", phoneTitle);
                    missingDetails.add(phoneTitle);
                } else {
                    Log.e("not missing", mCurrentUser.getString(phoneTitle));
                }

                //although no alertdialog is shown just yet, this will allow onResume to show one
                alertDialogIsShown = (missingDetails.size() > 0);
            } else {
                profileImageSet = savedInstanceState.getBoolean("profileImageSet");
                alertDialogIsShown = savedInstanceState.getBoolean("alertDialogIsShown");
                missingDetails = savedInstanceState.getStringArrayList("missingDetails");
                profilePicIsFresh = savedInstanceState.getBoolean("profilePicIsFresh");
                Log.e("on start", "profileImageSet = " + profileImageSet + "");
                Log.e("on start", "alertDialogIsShown = " + alertDialogIsShown + "");
                Log.e("on start", "missingDetails = " + missingDetails + "");
                Log.e("on start", "profilePicIsFresh = " + profilePicIsFresh + "");

                ccFragment = (ConnectivityCheckerFragment)
                        getSupportFragmentManager().getFragment(savedInstanceState, "ccFragment");
                byte[] data;
                if (profilePicIsFresh) {
                    Log.e("oncreate", "profile pic is fresh so use saved data");
                    data = savedInstanceState.getByteArray("profileBitmapData");
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    mProfilePicture.setImageBitmap(bitmap);
                    TextView profilePicCaption = (TextView) findViewById(R.id.profilePictureCaption);
                    profilePicCaption.setText(getResources().getString(R.string.fresh_profile_picture_caption));
                } else {
                    if (profileImageSet) {
                        try {
                            Log.e("oncreate", "profile pic is old so get pic from server");
                            ParseFile parseFile = mCurrentUser.getParseFile(profilePicTitle);
                            data = parseFile.getData();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            mProfilePicture.setImageBitmap(bitmap);
                            TextView profilePicCaption = (TextView) findViewById(R.id.profilePictureCaption);
                            profilePicCaption.setText(getResources().getString(R.string.profile_pic_instruction));
                        } catch (ParseException pe) {
                            pe.printStackTrace();
                        }
                    } else {
                        TextView profilePicCaption = (TextView) findViewById(R.id.profilePictureCaption);
                        profilePicCaption.setText(getResources().getString(R.string.profile_picture_caption));
                    }
                }
            }
        } else {
            //user rotated the screen while logging out so just display a loading circle
            showProgress(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!loggingOutFrmStart) {
            //there is data to be saved
            getSupportFragmentManager().putFragment(outState, "ccFragment", ccFragment);
            Log.e("on save", "profileImageSet = " + profileImageSet + "");
            Log.e("on save", "alertDialogIsShown = " + alertDialogIsShown + "");
            Log.e("on save", "missingDetails = " + missingDetails + "");
            Log.e("on save", "profilePicIsFresh = " + profilePicIsFresh + "");

            outState.putBoolean("alertDialogIsShown", alertDialogIsShown);
            outState.putStringArrayList("missingDetails", missingDetails);
            outState.putBoolean("profileImageSet", profileImageSet);
            outState.putBoolean("profilePicIsFresh", profilePicIsFresh);
            if (profilePicIsFresh) {
                //only save the image if we really must, to save memory
                Log.e("on save instance state", "profile pic is fresh is true");
                Bitmap profileImageBitmap = ((BitmapDrawable) mProfilePicture.getDrawable()).getBitmap();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                profileImageBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
                byte[] data = stream.toByteArray();
                outState.putByteArray("profileBitmapData", data);
            }
        }
        outState.putBoolean("loggingOut", loggingOut);
        Log.e("main activity", "on save instance state finished");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_logout:
                showProgress(true);
                loggingOut = true;
                ConnectivityManager cm = (ConnectivityManager) MainActivity.this
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork == null ||
                        (activeNetwork.getType() != ConnectivityManager.TYPE_MOBILE && activeNetwork.getType() != ConnectivityManager.TYPE_WIFI)) {
                    String logoutWarningText = getResources().getString(R.string.logout_warning);
                    Toast.makeText(MainActivity.this, logoutWarningText, Toast.LENGTH_SHORT).show();
                }
                //No reason to keep the user waiting so do this in a background thread
                ParseUser.logOutInBackground(new LogOutCallback() {
                    @Override
                    public void done(ParseException e) {
                        //All of this needs to be in a callback or else the activity will finish()
                        // before the LoginActivity starts
                        Intent startMainActivityIntent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(startMainActivityIntent);
                        finish();
                    }
                });
        }

        return super.onOptionsItemSelected(item);
    }

    private void askForMoreDetails() {
        Log.e("ask for more details", "missingDetails = " + missingDetails);
        int numOfMissingDetails = missingDetails.size();
        if (numOfMissingDetails == 0) {
            //no details are missing, so the user must simply be changing their profile picture
            showProfilePictureAlert();
        }
        if (numOfMissingDetails > 1) {
            //At least one of phone number, email or birthday is missing so open the extra details dialog
            buildExtraDetailsAlertDialog(MainActivity.this);
        } else if (numOfMissingDetails == 1) {
            if (missingDetails.contains(profilePicTitle)) {
                //only the profile picture is missing
                Log.e("ask for more details", "calling show profile pic");
                showProfilePictureAlert();
            } else {
                //either the phone number, email or birthday is missing but the profile picture has been set
                buildExtraDetailsAlertDialog(MainActivity.this);
            }
        }
    }

    private void buildExtraDetailsAlertDialog(Context ctx) {
        LayoutInflater li = LayoutInflater.from(MainActivity.this);
        View promptsView = li.inflate(R.layout.user_details_prompt, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText emailInput = (EditText) promptsView
                .findViewById(R.id.email_edittext);
        final EditText phoneInput = (EditText) promptsView
                .findViewById(R.id.phone_edittext);
        final LinearLayout dobInput = (LinearLayout) promptsView
                .findViewById(R.id.dob_edittext_ll);
        final EditText[] dobEdittexts = new EditText[3];
        for (int i = 0; i < dobInput.getChildCount(); i++) {
            dobEdittexts[i] = (EditText) dobInput.getChildAt(i);
        }

        //Decide whether or not to show the fields based on whether or not the data is missing
        if (!missingDetails.contains(emailTitle)) {
            emailInput.setVisibility(View.GONE);
            promptsView.findViewById(R.id.email_title).setVisibility(View.GONE);
        }
        if (!missingDetails.contains(phoneTitle)) {
            phoneInput.setVisibility(View.GONE);
            promptsView.findViewById(R.id.phone_title).setVisibility(View.GONE);
        }
        if (!missingDetails.contains(dobTitle)) {
            dobInput.setVisibility(View.GONE);
            promptsView.findViewById(R.id.dob_title).setVisibility(View.GONE);
        }

        // set dialog message
        String okButtonText;
        if (missingDetails.contains(profilePicTitle)) {
            okButtonText = "NEXT";
        } else {
            promptsView.findViewById(R.id.next_instruction).setVisibility(View.GONE);
            okButtonText = "DONE";
        }
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(okButtonText,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //This will be overridden after calling show()
                                //so the inputs can be validated
                            }
                        });

        // create alert dialog
        alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
        alertDialogIsShown = true;

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check input validity
                Boolean closeDialog = true;
                //instantiate a dummy view and make it unfocusable
                View errorView = new View(MainActivity.this);
                errorView.setFocusable(false);
                String dateOfBirth = "";
                if (missingDetails.contains(dobTitle)) {
                    Integer[] dobSegmentLengths = new Integer[3];
                    dobSegmentLengths[0] = 2;
                    dobSegmentLengths[1] = 2;
                    dobSegmentLengths[2] = 4;
                    for (int i = 0; i < 3; i++) {
                        String dobSegment = dobEdittexts[i].getText().toString();
                        dateOfBirth += dobSegment;
                        closeDialog = closeDialog && (dobSegment.length() == dobSegmentLengths[i]);
                        if (i < 2) {
                            dateOfBirth += "/";
                        }
                        Log.e("dob builder", "dob = " + dateOfBirth);
                    }
                    if (closeDialog) {
                        //if the lengths of text entered were wrong, don't bother doing anything else
                        try {
                            DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                            df.setLenient(false);
                            df.parse(dateOfBirth);
                        } catch (java.text.ParseException e) {
                            closeDialog = false;
                        }
                    }
                    if (!closeDialog) {
                        for (int i = 0; i < 3; i++) {
                            dobEdittexts[i].setError(getString(R.string.dob_error));
                        }
                        errorView = dobEdittexts[0];
                    }
                }
                if (missingDetails.contains(phoneTitle)) {
                    phoneInput.setError(null);
                    String phoneNumber = phoneInput.getText().toString();
                    Log.e("phone", "length check: " + (phoneNumber.length() == 11));
                    Log.e("phone", "first digit: " + (phoneNumber.startsWith("0")));
                    boolean phoneCorrect = (phoneNumber.length() == 11) && (phoneNumber.startsWith("0"));
                    if (!phoneCorrect) {
                        errorView = phoneInput;
                        phoneInput.setError(getString(R.string.phone_error));
                    }
                    closeDialog = closeDialog && phoneCorrect;
                }
                if (missingDetails.contains(emailTitle)) {
                    emailInput.setError(null);
                    boolean emailCorrect = android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput.getText()).matches();
                    if (!emailCorrect) {
                        errorView = emailInput;
                        emailInput.setError(getString(R.string.email_error));
                    }
                    closeDialog = closeDialog && emailCorrect;
                }
                if (closeDialog) {
                    //if inout is valid, update the user's profile and close the dialog
                    // get user input and set it to result
                    // edit text
                    String toastString = "";
                    boolean requireNewLineInToast = false;
                    if (missingDetails.contains(emailTitle)) {
                        mCurrentUser.setEmail(emailInput.getText().toString());
                        Log.e("main activity prompt", "email set to " + emailInput.getText().toString());
                        toastString += "email set to " + emailInput.getText().toString();
                        requireNewLineInToast = true;
                    }
                    if (missingDetails.contains(phoneTitle)) {
                        mCurrentUser.put(phoneTitle, phoneInput.getText().toString());
                        Log.e("main activity prompt", "phone number set to " + phoneInput.getText().toString());
                        if (requireNewLineInToast) {
                            toastString += "\n";
                        }
                        toastString += "phone number set to " + phoneInput.getText().toString();
                        requireNewLineInToast = true;
                    }
                    if (missingDetails.contains(dobTitle)) {
                        mCurrentUser.put(dobTitle, dateOfBirth);
                        Log.e("main activity prompt", "date of birth set to " + dateOfBirth);
                        if (requireNewLineInToast) {
                            toastString += "\n";
                        }
                        toastString += "date of birth set to " + dateOfBirth;
                    }
                    //use saveEventually() in case the user does not have internet access right now
                    mCurrentUser.saveEventually();
                    Toast.makeText(MainActivity.this, toastString, Toast.LENGTH_SHORT).show();
                    if (missingDetails.contains(profilePicTitle)) {
                        askForProfilePicture();
                    }
                    alertDialogIsShown = false;
                    missingDetails.remove(emailTitle);
                    missingDetails.remove(phoneTitle);
                    missingDetails.remove(dobTitle);
                    alertDialog.dismiss();
                } else {
                    //input is invalid so dialog stays open
                    errorView.requestFocus();
                }
            }
        });
    }

    public void requestNewProfilePicture(View v) {
        showProfilePictureAlert();
    }

    public void showProfilePictureAlert() {
        Log.e("show profile pic alert", "calling builder");
        buildProfilePictureAlertDialog(MainActivity.this);
        // show it
        Log.e("show profile pic alert", "showing dialog");
        alertDialog.show();
        alertDialogIsShown = true;
        Log.e("show profile pic alert", "showed dialog");
    }

    private void buildProfilePictureAlertDialog(Context ctx) {
        Resources res = getResources();

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
        if (!profileImageSet) {
            alertDialogBuilder.setTitle(res.getString(R.string.profile_picture_dialog_title));
            alertDialogBuilder.setMessage(res.getString(R.string.profile_picture_reminder));
        } else {
            alertDialogBuilder.setTitle(res.getString(R.string.profile_picture_change_dialog_title));
            alertDialogBuilder.setMessage(res.getString(R.string.profile_picture_change_reminder));
        }
        alertDialogBuilder
                .setPositiveButton(res.getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //don't show the dialog again when the activity resumes after choosing a picture
                                alertDialog = null;
                                alertDialogIsShown = false;
                                askForProfilePicture();
                            }
                        })
                .setNegativeButton(res.getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                alertDialogIsShown = false;
                                dialog.dismiss();
                            }
                        });

        // create alert dialog
        alertDialog = alertDialogBuilder.create();
    }

    private void askForProfilePicture() {
        //make the gallery and camera intents
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT, null);
        galleryIntent.setType("image/*");
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);

        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        //make the chooser intent that lets the user choose from the gallery and camera intents
        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
        chooser.putExtra(Intent.EXTRA_INTENT, galleryIntent);
        chooser.putExtra(Intent.EXTRA_TITLE, getResources().getString(R.string.profile_picture_intent_title));
        Intent[] intentArray = {cameraIntent};
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
        startActivityForResult(chooser, getResources().getInteger(R.integer.pick_image));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == getResources().getInteger(R.integer.pick_image)) {
            profilePicIsFresh = true;
            TextView profilePicCaption = (TextView) findViewById(R.id.profilePictureCaption);
            profilePicCaption.setText(getResources().getString(R.string.fresh_profile_picture_caption));
            ImageView profilePicture = (ImageView) findViewById(R.id.userAvatar);
            Bitmap profileBitmap;
            if ("inline-data".equals(data.getAction())) {
                //an image was taken using the camera
                Log.e(LOG_TAG, "if block; camera used");
                profileBitmap = (Bitmap) data.getExtras().get("data");
            } else {
                Log.e(LOG_TAG, "else block; gallery used");
                //an image was selected from the images on the device
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(
                        selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                cursor.close();
                profileBitmap = BitmapFactory.decodeFile(filePath);
            }
            //set the profile picture and save it to parse.com
            profilePicture.setImageBitmap(profileBitmap);
            saveNewProfilePicture(profileBitmap);
            profileImageSet = true;
        }
    }

    private void saveNewProfilePicture(Bitmap profileImageBitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        profileImageBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        byte[] data = stream.toByteArray();
        String thumbName = mCurrentUser.getUsername().replaceAll("\\s+", "");
        final ParseFile parseFile = new ParseFile(thumbName + "_thumb.jpg", data);
        parseFile.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    mCurrentUser.put("profileThumb", parseFile);
                    //Finally save all the user details
                    mCurrentUser.saveEventually(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                profilePicIsFresh = false;
                                ((TextView) findViewById(R.id.profilePictureCaption))
                                        .setText(getResources().getString(R.string.profile_pic_instruction));
                            } else {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    e.printStackTrace();
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        Log.e("showProgress", show ? "showing progress" : "unshowing progress");
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mMainView.setVisibility(show ? View.GONE : View.VISIBLE);
            mMainView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMainView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mMainView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (alertDialogIsShown) {
            alertDialog.dismiss();
            Log.e("onPause", "dismissing alert dialog");
        }
        Log.e("main activity", "on pause finished");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("onResume", alertDialogIsShown + "");
        if (alertDialogIsShown) {
            //The if condition ensures this dialog will not be duplicated if it was already created in OnCreate()
            askForMoreDetails();
        }
        Log.e("onResume", "finished resuming");
    }
}
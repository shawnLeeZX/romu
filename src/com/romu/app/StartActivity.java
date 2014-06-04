package com.romu.app;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class StartActivity extends Activity
{
    public static final String LOG_TAG = "Romu: StartActivity";

    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST  = 3;

    private FragmentManager fragmentManager;

    // Use info.
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        fragmentManager = getFragmentManager();

        // If Google Play Service is not available, the app will just quit.
        if(isGooglePlayServiceAvailable())
        {
            sharedPref = getSharedPreferences(
                    getString(R.string.user_info_file_key),
                    Context.MODE_PRIVATE
                    );

            // If user preference exists we continue to RomuActivity.
            String macAddr = sharedPref.getString(
                    getString(R.string.mac_address),
                    null
                    );
            if(macAddr != null)
            {
                Log.i(LOG_TAG, "Romu is not running for the first time." + "\n"
                        + "Skip welcome and info page.");
                Intent intent = new Intent(this, RomuActivity.class);
                startActivity(intent);
                finish();
            }
            // Otherwise show more time of welcome page and get user info using
            // GetInfoActivity.
            else
            {
                setContentView(R.layout.welcome);

                Log.i(LOG_TAG, "Romu is running for the first time.");

                new Handler().postDelayed(new Runnable()
                        {

                            public void run()
                            {
                                Intent intent = new Intent(StartActivity.this, GetInfoActivity.class);
                                startActivity(intent);
                                StartActivity.this.finish();
                            }
                        }, 3000);
            }
        }

    }

    // Misc.
    // ================================================================================
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                {
                    // If google play service resolves the problem, do nothing.
                    if(resultCode == RESULT_OK)
                        break;
                    // If not, Show the dialog to inform user google play
                    // service must be present to use the app and quit.
                    Dialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.googleplay_service_prompt)
                        .setPositiveButton(R.string.prompt_dialog_quit,
                                new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog, int whichButton)
                                    {
                                        finish();
                                    }
                                }
                        )
                        .create();

                    RomuDialogFragment fragment = new RomuDialogFragment();
                    fragment.setDialog(dialog);

                    fragment.show(fragmentManager, "google_play_service_prompt");
                    break;
                }
            default:
                Log.e(LOG_TAG, "Activity result out of range.");
        }
    }

    /**
     * Check and handle the availability of Google Play Service, which is
     * essential for LocationService provided by android. If the service is not
     * available, it will prompt user to install or update it.
     */
    private boolean isGooglePlayServiceAvailable()
    {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode)
        {
            // In debug mode, log the status
            Log.i("Location Updates",
                    "Google Play services is available.");
            // Continue
            return true;
        }
        // Google Play services was not available for some reason
        else
        {
            // Get the error code
            int errorCode = resultCode;
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    errorCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null)
            {
                // Create a new DialogFragment for the error dialog
                RomuDialogFragment errorFragment =
                        new RomuDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(fragmentManager,
                        "Wrong with Google Play Service");
            }

            // No matter whether the error has been resolved by Google Play
            // Service or not, as long as Google Play Service is not detected in
            // the first time, the method will return false.
            return false;
        }
    }

}

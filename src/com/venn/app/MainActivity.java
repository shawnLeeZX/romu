/**
 * The workflow of this app works like the following:
 * Initialization:
 *  The user is given a list to choose the correponding wearable deivce
 *  according to mac address. After initialization, the app automatically
 *  connect to the device chosen at start up.
 *
 * Normal workflow(after initialization):
 * At startup:
 *  if bluetooth is not enabled, it prompts user to enable bluetooth. If the
 *  user does not permit the action, permission of bluetooth will be asked each
 *  time it is needed.
 *
 *  Then the main interface is loaded, which is a map that gives the user a
 *  visual display on map of where she or he is.
 *
 *  In this interface, user can decide to start navigation by clicking
 *  'navigation' button.
 *
 * When user decide to do navigation:
 *  After the 'navigation' is clicked, the user is prompted to input a start
 *  location and a destination. User confirms the input by clicking the button
 *  'ok' 
 *
 * Upon confirmation:
 *  Given two locations, we want to go from the startLocation to destination.
 *  1. make a http requestion using Google Direction API.
 *  2. parse the response into meaningful results.
 *  3. process each segment of the route results one by one.
 *  TODO: more refinement needed.
 */
package com.venn.app;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity implements
    GooglePlayServicesClient.ConnectionCallbacks,
    GooglePlayServicesClient.OnConnectionFailedListener
{
    public static final String LOG_TAG = "Venn:MainActivity";

    // UI.
    private FragmentManager fragmentManager = null;

    // Requestion code for user interaction activities.
    private static final int ENABLE_BLUETOOTH_REQUEST               = 1;
    private static final int FETCH_START_AND_DESTINATION_REQUEST    = 2;
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST  = 3;

    // Necessity class for Google services.
    private BluetoothAdapter bluetooth      = null;
    private GoogleMap map                   = null;
    private LocationClient locationClient   = null;

    // Global naviation info.
    String startAddr = null;
    String destAddr  = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        fragmentManager = getFragmentManager();

        // Since bluetooth plays a central role of this app, it will ask the
        // user to enable bluetooth at startup.
        // TODO: change bluetooth to BT Gatt.
        bluetooth = BluetoothAdapter.getDefaultAdapter();
        enableBluetooth();

        // TODO: error handler and location listener for locationClient undone.
        if(servicesConnected())
            locationClient = new LocationClient(this, this, this);
        else
            Log.e(LOG_TAG, "Cannot connect to Google Play Service. Program should not reach here.");

        renderMap();
        Log.d(LOG_TAG, "Map render finishes.");
        Log.d(LOG_TAG, "MainActivity initialized.");
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Set up map object if it is destroyed.
        setUpMapIfNeeded();
    }

    // Called when the Activity becomes visible.
    @Override
    protected void onStart()
    {
        super.onStart();

        // Connect the location service.
        locationClient.connect();
    }

    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        locationClient.disconnect();
        super.onStop();
    }

    // Do a null check to confirm that we have initiated the map.
    // During app's lifetime, This prevents map being destroyed after suspended.
    private void setUpMapIfNeeded()
    {
        // Get the map if not.
        if(map == null)
        {
            map = ((MapFragment) fragmentManager.findFragmentById(R.id.map))
                    .getMap();
            // If we cannot get the map, prompt user to fix the problem.
            // Otherwise functions concerning map may not work.
            if(map == null)
            {
                Log.d(LOG_TAG, "Failed to instantiate google map");
                // TODO: Give prompt to let user fix the problem to let the map
                // running. For instance, enable network.
            }
            else
                Log.d(LOG_TAG, "Successfully instantiate google map.");
        }
    }

    /**
     * Initial rendering of Google Map at app startup.
     */
    private void renderMap()
    {
        setUpMapIfNeeded();
        map.setMyLocationEnabled(true);
    }

    // Unfinished.
    public void enableBluetooth()
    {
        // Enable bluetooth if not, otherwise do nothing.
        Log.d(LOG_TAG, "Trying to enable bluetooth.");
        if(!bluetooth.isEnabled())
        {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, ENABLE_BLUETOOTH_REQUEST);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            // Prompt user to enable bluetooth.
            case ENABLE_BLUETOOTH_REQUEST:
                {
                    Log.d(LOG_TAG, "User cancelled enable bluetooth dialog. Confirming.");
                    if(resultCode != RESULT_OK)
                    {
                        showBluetoothConfirmDialog();
                    }
                    break;
                }
            case FETCH_START_AND_DESTINATION_REQUEST:
                {
                    // Fetch the start location and destination from user input.
                    Log.d(LOG_TAG, "Location fetcher returned.");
                    if(resultCode != RESULT_OK)
                    {
                        Log.d(LOG_TAG, "There is something wrong with location fetcher.");
                        // TODO: code the error handler.
                        break;
                    }
                    Log.d(LOG_TAG, "Location fetcher finished successfully.");
                    Bundle bundle   = data.getExtras();
                    startAddr    = bundle.getString(LocationFetcherActivity.START_ADDR_STRING);
                    Log.d(LOG_TAG, "Start location fetched: " + startAddr);
                    destAddr     = bundle.getString(LocationFetcherActivity.DEST_ADDR_STRING);
                    Log.d(LOG_TAG, "Destination fetched: " + destAddr);

                    break;
                }
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                {
                    // If google play service resolves the problem, do the
                    // request again.
                    if(resultCode == RESULT_OK)
                        servicesConnected();
                    else
                    {
                        // Show the dialog to inform user google play service
                        // must be present to use the app and quit.
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

                        WayfarerDialogFragment fragment = new WayfarerDialogFragment();
                        fragment.setDialog(dialog);

                        fragment.show(fragmentManager, "google_play_service_prompt");
                    }
                }
            default:
                Log.e(LOG_TAG, "Activity result out of range.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_search) {
            // openSearch();
            return true;
        } else if (itemId == R.id.action_settings) {
            // openSettings();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Called by Location Services when the request to connect the client
     * finishes successfully. At this point, you can request the current
     * location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Log.d(LOG_TAG, "Location service connnected.");
        Toast.makeText(this, "Location Service Connected", Toast.LENGTH_SHORT).show();
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
        Log.d(LOG_TAG, "Location service disconnnected.");
        Toast.makeText(this, "Location Serice Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /**
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        // if (connectionResult.hasResolution()) {
            // try {
                // // Start an Activity that tries to resolve the error
                // connectionResult.startResolutionForResult(
                        // this,
                        // CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /**
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            // } catch (IntentSender.SendIntentException e) {
                // // Log the error
                // e.printStackTrace();
            // }
        // } else {
            /**
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            // showErrorDialog(connectionResult.getErrorCode());
        // }
    }

    // This is the entry point for haptic navigation. It will start an activity
    // to let user specify start location and destination.
    public void onNavigate(View view)
    {
            Intent intent = new Intent(this, LocationFetcherActivity.class);
            startActivityForResult(intent, FETCH_START_AND_DESTINATION_REQUEST);
    }

    /**
     * Check and handle the availability of Google Play Service, which is
     * essential for LocationService provided by android.
     */
    private boolean servicesConnected()
    {
            // Check that Google Play services is available
            int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
            // If Google Play services is available
            if (ConnectionResult.SUCCESS == resultCode) {
                // In debug mode, log the status
                Log.d("Location Updates",
                        "Google Play services is available.");
                // Continue
                return true;
            // Google Play services was not available for some reason
            } else {
                // Get the error code
                int errorCode = resultCode;
                // Get the error dialog from Google Play services
                Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                        errorCode,
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);

                // If Google Play services can provide an error dialog
                if (errorDialog != null) {
                    // Create a new DialogFragment for the error dialog
                    WayfarerDialogFragment errorFragment =
                            new WayfarerDialogFragment();
                    // Set the dialog in the DialogFragment
                    errorFragment.setDialog(errorDialog);
                    // Show the error dialog in the DialogFragment
                    errorFragment.show(fragmentManager,
                            "Location Updates");
                }

                // If no error dialog obtained, just return false. We cannot
                // connect to Google Play Service.
                return false;
            }
        }

    /**
     * Double confirmation that the user should enable bluetooth using dialog.
     * TODO: swtich this to bluetooth Gatt.
     */
    private void showBluetoothConfirmDialog()
    {
        // Create an dialog and pass it to ConfirmationDialogFragment to render.
        Dialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.bluetooth_prompt)
            .setPositiveButton(R.string.prompt_dialog_ok,
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton)
                        {
                            enableBluetooth();
                        }
                    }
            )
            .setNegativeButton(R.string.prompt_dialog_cancel,
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton)
                        {
                            // Does nothing but quit the confirmation dialogue.
                            Log.d(LOG_TAG, "User decided not to open bluetooth. Just continue.");
                        }
                    }
            )
            .create();

        WayfarerDialogFragment fragment = new WayfarerDialogFragment();
        fragment.setDialog(dialog);

        fragment.show(fragmentManager, "bluetooth_comfirmation");
    }

}

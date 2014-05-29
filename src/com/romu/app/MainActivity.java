/**
 * Romu introduction:
 * ==========================
 *
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
 *  time it is needed.(This part could be changed since I am not responsible for
 *  the bluetooth part and the function to communicate with arduino is not implemented yet.)
 *
 *  Then the main interface is loaded, which is a map that gives the user a
 *  visual display on map of where she or he is.
 *
 *  In this interface, user can decide to start navigation by clicking
 *  'navigation' button.
 *
 * When user decide to do navigation:
 *  After the 'navigation' is clicked, the user is prompted to input a start
 *  location and a destination. Autocompletion will happen in this phase to give
 *  right address decription to user as typing. User confirms the input by
 *  clicking the button 'ok' 
 *
 * Upon confirmation:
 *  After getting two address, utilizing Google Direction API service, all
 *  information needed to travel from origin to destination is fetched and
 *  stored in the {@link Route} class. At the same time, route will be displayed
 *  on the map.
 *  @Andrew, this is where you should take over. Route class have all the
 *  information needed.
 *  For now, location service is buggy. More specifically, the availability of
 *  Google Play Service is not solid. When I disable location service, the app
 *  crashes. And bluetooth is not functioning at all. You can remove any code
 *  concerning bluetooth.
 */
package com.romu.app;

import java.net.MalformedURLException;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.*;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.romu.app.RomuService.LocalBinder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

public class MainActivity extends Activity
{
    public static final String LOG_TAG = "Romu: MainActivity";

    // UI.
    private FragmentManager fragmentManager = null;
    private AutoCompleteTextView destAddrAutoCompleteTextView = null;

    // Requestion code for user interaction activities.
    private static final int FETCH_START_AND_DESTINATION_REQUEST    = 2;
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST  = 3;

    // Necessity class for Google services.
    private GoogleMap map = null;
    private boolean googleServicesAvailableInitially = false;

    // Global naviation info.
    private String startAddr = null;
    private String destAddr  = null;
    private Route currentRoute = null;
    private boolean isNavigationStopped = true;

    // Interaction with Romu service.
    private BroadcastReceiver romuUpdateReciever = null;
    private ServiceConnection serviceConnection = null; 
    private RomuService romuService = null;

    // Life Cycle
    // =====================================================================

    /**
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        fragmentManager = getFragmentManager();

        if(isGooglePlayServiceAvailable())
        {
            googleServicesAvailableInitially = true;
            setContentView(R.layout.main);

            Log.d(LOG_TAG, "Romu service initializing.");
            initializeRomuService();

            initNavigationUI();

            renderMap();
            Log.d(LOG_TAG, "Map render finishes.");

            Log.d(LOG_TAG, "MainActivity initialized.");
        }
        else
        {
            googleServicesAvailableInitially = false;
            setContentView(R.layout.empty);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    /**
     * Called when the Activity becomes visible.
     */
    @Override
    protected void onStart()
    {
        super.onStart();

        // Check the availability of Google Play Service.
        if(googleServicesAvailableInitially)
        {
            if(romuService != null)
            {
                romuService.startLocationService();
            }

            // Set up map object if it is destroyed.
            setUpMapIfNeeded();
        }
    }

    /**
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop()
    {
        if(googleServicesAvailableInitially)
        {
            // If Romu is not in the navigation model, disable location service
            // for saving battery.
            if(isNavigationStopped)
            {
                if(romuService != null)
                {
                    romuService.stopLocationService();
                }
            }
        }
        super.onStop();
    }

    /**
     * Called when the Activity is going to be destroyed.
     */
    @Override
    protected void onDestroy()
    {
        if(googleServicesAvailableInitially)
        {
            stopRomuService();
        }
        super.onDestroy();
    }

    // Private methods.
    // ========================================================================

    /**
     * Do a null check to confirm that we have initiated the map.
     * During app's lifetime, This prevents map being destroyed after suspended.
     */
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

    private void initNavigationUI()
    {
        destAddrAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.main_dest_addr);
        destAddrAutoCompleteTextView.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.list_item));
    }

    /**
     * Initial rendering of Google Map at app startup.
     */
    private void renderMap()
    {
        setUpMapIfNeeded();
        // TODO: reimplement this button when refining.
        map.setMyLocationEnabled(true);
    }

    private void initializeRomuService()
    {
        Intent romuServiceIntent = new Intent(this, RomuService.class);
        Log.d(LOG_TAG, "Starting Romu Service...");
        startService(romuServiceIntent);
        // Connection with Romu service for better interface.
        serviceConnection = new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service)
            {
                Log.d(LOG_TAG, "Romu service connected.");
                LocalBinder binder = (LocalBinder) service;
                romuService = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName)
            {
                Log.d(LOG_TAG, "Romu service disconnected.");
                romuService = null;
            }
        };

        // BroadcastReceiver to receive updates broadcast from Romu service.
        romuUpdateReciever = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                final String action = intent.getAction();
                // TODO: receive broadcast from Romu service.
                /**
                 * if(RomuService.DEVICE_CONNECTED.equals(action)){
                 *     updateConnectionState(CONNECTED);
                 *     return;
                 * }
                 * if(RomuService.DEVICE_SERVICES_DISCOVERED.equals(action)){
                 *     updateConnectionState(DISCOVERED);
                 *     return;
                 * }
                 * if(RomuService.BEGIN_NAVIGATION.equals(action)){
                 *     updateConnectionState(NAV_MODE);
                 *     return;
                 * }
                 * if(RomuService.DEVICE_DISCONNECTED.equals(action)){
                 *     updateConnectionState(DISCONNECTED);
                 *     return;
                 * }
                 * if(RomuService.DEVICE_FOUND.equals(action)){
                 *     updateConnectionState(FOUND);
                 *     return;
                 * }
                 * if(RomuService.STOP_NAVIGATION.equals(action)){
                 *     updateConnectionState(STOP_NAV_MODE);
                 *     return;
                 * }
                 * if(RomuService.NAVIGATION_COMPLETE.equals(action)){
                 *     updateConnectionState(NAVIGATION_COMPLETE);
                 *     return;
                 * }
                 * if(RomuService.PAUSE_NAVIGATION.equals(action)){
                 *     updateConnectionState(PAUSE_NAV_MODE);
                 *     return;
                 * }
                 * if(RomuService.LOCATION_CONNECTION_SUCCESS.equals(action)){
                 *     notifyUser(action);
                 *     return;
                 * }
                 * if(RomuService.LOCATION_SERVICE_CONNECTION_FAIL.equals(action)){
                 *     notifyUser(action);
                 *     return;
                 * }
                 * if(RomuService.LOCATION_SERVICE_DISCONNECTED.equals(action)){
                 *     notifyUser(action);
                 *     return;
                 * }
                 */
            }
        };
        registerReceiver(romuUpdateReciever, romuUpdateIntentFilter());

        Log.d(LOG_TAG, "Binding romu service...");
        bindService(romuServiceIntent, serviceConnection, BIND_AUTO_CREATE);


    }

    private void stopRomuService()
    {
        unregisterReceiver(romuUpdateReciever);
        unbindService(serviceConnection);
        romuService = null;

        Intent romuServiceIntent = new Intent(this, RomuService.class);
        stopService(romuServiceIntent);
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
            Log.d("Location Updates",
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

    // Naptic Navigation Related.
    // =================================================================================

    /**
     * This is the entry point for haptic navigation. It will start an activity
     * to let user specify start location and destination.
     */
    public void onNavigate(View view)
    {
        // STOP
        Intent intent = new Intent(this, LocationFetcherActivity.class);
        startActivityForResult(intent, FETCH_START_AND_DESTINATION_REQUEST);
    }

    // TODO: this functions is reserved for navigation capable of choosing
    // origin and destination. No button is associated with this yet.
    public void onTwoPointRoute(View view)
    {
        Intent intent = new Intent(this, LocationFetcherActivity.class);
        startActivityForResult(intent, FETCH_START_AND_DESTINATION_REQUEST);
    }

    /**
     * Callback for Route button in the navigation bar, which will obtain and
     * store relevant route information from Google and display the route
     * visually on map.
     */
    public void onRoute(View view)
    {
        destAddr = destAddrAutoCompleteTextView.getText().toString();

        // Replace space with %20.
        destAddr = destAddr.replace(" ", "%20");

        // Get current location's latitude and longitude.
        getRouteByRequestingGoogle(true);
    }

    /**
     * The listener for Stop button, which controls the whether the user wants
     * to navigate using current route. When clicked, it will stop current
     * navigation process. In this case, when the app becomes invisible, the
     * connection to location service of google will be stopped.
     */
    public void onStopNavigation(View view)
    {
        isNavigationStopped = true;
        // TODO: stop navigation process of bluetooth.
    }

    /**
     * Make request to Google Direction API to get route from start address to
     * destination address in another thread.
     *
     * start_addr, dest_addr and route are all class memebers, so no parameters
     * are passed.
     */
    private void getRouteByRequestingGoogle(boolean useLatLng)
    {
        if(useLatLng)
        {
            // If romu service is not ready, just return.
            if(romuService == null)
            {
                Toast.makeText(this, "Romu Service not ready.", Toast.LENGTH_SHORT).show();
                return;
            }

            // If current location is unavailable, we just ignore.
            LatLng currentLatLng = romuService.getCurrentLatLgn();
            if(currentLatLng == null)
                return;

            // Else, keep going.
            new GetRoutes(
                    useLatLng,
                    currentLatLng,
                    destAddr
                    ).execute();
        }
        else
        {
            new GetRoutes(
                    useLatLng,
                    startAddr,
                    destAddr
                    ).execute();
        }
    }

    /**
     * Inner class responsible for retrieve route from Google Direction Service.
     */
    private class GetRoutes extends AsyncTask<String, Void, Void>
    {
        private boolean useLatLng = false;
        private String startAddr = null;
        private String destAddr = null;
        private LatLng startLatLng = null;

        public GetRoutes(boolean useLatLng, String startAddr, String destAddr)
        {
            this.useLatLng = useLatLng;
            this.startAddr = startAddr;
            this.destAddr = destAddr;
        }

        public GetRoutes(boolean useLatLng, LatLng startLatLng, String destAddr)
        {
            this.useLatLng = useLatLng;
            this.startLatLng = startLatLng;
            this.destAddr = destAddr;
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected Void doInBackground(String... params)
        {
            if(useLatLng)
                currentRoute = directions(startLatLng, destAddr);
            else
                currentRoute = directions(startAddr, destAddr);

            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            // Draw route on the map.
            PolylineOptions routePolylineOptions = new PolylineOptions();
            routePolylineOptions.addAll(currentRoute.getPoints());
            map.addPolyline(routePolylineOptions);

            // Draw marker on origin and destination.
            map.addMarker(new MarkerOptions()
                    .position(currentRoute.getStartLocation())
                    .title(currentRoute.getStartAddr())
                    );
            map.addMarker(new MarkerOptions()
                    .position(currentRoute.getEndLocation())
                    .title(currentRoute.getDestAddr())
                    );

            // Set camera to the route.
            // TODO: adjust the padding when refining.
            // TODO: add animation when moving camera.
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(currentRoute.getBounds(), 0));
        }

        /**
         * Given two address, get route from the origin address to the
         * destination. This method uses Google API. Note that the address
         * should be retrieved from Google to ensure its validity. Random
         * arbitrary address will raise error.
         *
         * @param   startAddr   The origin address of the route.
         * @param   destAddr    The detination address.
         *
         * @return  Route       A class encapsule all information from Google.
         *                      See {@link Route}.
         */
        private Route directions(String startAddr, String destAddr)
        {
            Route route = null;

            // Construct http request to Google Direction API service.
            String jsonURL = "http://maps.googleapis.com/maps/api/directions/json?";
            StringBuilder sBuilder = new StringBuilder(jsonURL);
            sBuilder.append("origin=");
            sBuilder.append(startAddr);
            sBuilder.append("&destination=");
            sBuilder.append(destAddr);
            sBuilder.append("&sensor=true&mode=walking&key" + Utilities.API_KEY);

            String requestUrl = sBuilder.toString();
            try {
                final GoogleDirectionParser parser = new GoogleDirectionParser(requestUrl);
                route = parser.parse();
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "Error when parsing url.");
            }
            return route;
        }

        /**
         * Use latitude and longitude for start location. Other description
         * please see {@link #directions(String startAddr, String destAddr)
         * directions}.
         */
        private Route directions(LatLng startLatLng, String destAddr)
        {
            Route route = null;

            // Construct http request to Google Direction API service.
            String jsonURL = "http://maps.googleapis.com/maps/api/directions/json?";
            StringBuilder sBuilder = new StringBuilder(jsonURL);
            sBuilder.append("origin=");
            sBuilder.append(startLatLng.latitude);
            sBuilder.append(",");
            sBuilder.append(startLatLng.longitude);
            sBuilder.append("&destination=");
            sBuilder.append(destAddr);
            sBuilder.append("&sensor=true&mode=walking&key" + Utilities.API_KEY);

            String requestUrl = sBuilder.toString();
            return directionRequestToGoogle(requestUrl);
        }

        /**
         * Make request to Google Direction Service
         *
         * @param   requestUrl  String of http request to Google Direction Service.
         * 
         * @return  Route       {@Route} Class encapsuled all information
         *                      returned.
         */
        private Route directionRequestToGoogle(String requestUrl)
        {
            Route route = null;

            try {
                final GoogleDirectionParser parser = new GoogleDirectionParser(requestUrl);
                route = parser.parse();
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "Error when parsing url.");
            }
            return route;
        }
    }

    // Communication with Romu service.
    // =================================================================================
    private static IntentFilter romuUpdateIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();

        // intentFilter.addAction(RomuService.DEVICE_CONNECTED);
        // intentFilter.addAction(RomuService.DEVICE_DISCONNECTED);
        // intentFilter.addAction(RomuService.BEGIN_NAVIGATION);
        // intentFilter.addAction(RomuService.DEVICE_FOUND);
        // intentFilter.addAction(RomuService.STOP_NAVIGATION);
        // intentFilter.addAction(RomuService.PAUSE_NAVIGATION);
        // intentFilter.addAction(RomuService.NAVIGATION_COMPLETE);
        // intentFilter.addAction(RomuService.LOCATION_SERVICE_CONNECTION_FAIL);
        // intentFilter.addAction(RomuService.DEVICE_SERVICES_DISCOVERED);
        // intentFilter.addAction(RomuService.LOCATION_CONNECTION_SUCCESS);
        // intentFilter.addAction(RomuService.LOCATION_SERVICE_DISCONNECTED);

        return intentFilter;
    }

    // Misc.
    // ================================================================================
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
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

                    getRouteByRequestingGoogle(false);

                    break;
                }
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

    // TODO: Clean up needed.
    // ================================================================================
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

}

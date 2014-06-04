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

import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class RomuActivity extends Activity
    implements TopNavBarFragment.TopNavBarAttachedListener,
               BottomCtrlBarFragment.BottomCtrlBarAttachedListener
{
    public static final String LOG_TAG = "Romu: RomuActivity";

    // UI.
    private FragmentManager fragmentManager;
    private AutoCompleteTextView destAddrAutoCompleteTextView;
    // State of UI of bottom control bar.
    private static final int BOTTOM_CTRL_NAVIGATION_INIT = 0;
    private static final int BOTTOM_CTRL_IN_NAVIGATING = 1;
    private static final int BOTTOM_CTRL_NAVIGATION_PAUSE = 2;
    private static final int BOTTOM_CTRL_NAVIGATION_STOP = 3;
    private static final int BOTTOM_CTRL_INFO = 4;
    // Left Drawer.
    private String[] drawerItems = { SETTINGS };
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    // Option list item.
    private static final String SETTINGS = "Settings";


    // Requestion code for user interaction activities.
    private static final int ENABLE_BT_REQUEST = 0;
    private static final int FETCH_START_AND_DESTINATION_REQUEST    = 2;

    // Necessity class for Google services.
    private GoogleMap map = null;

    // Global naviation info.
    private String startAddr = null;
    private String destAddr  = null;
    private Route currentRoute = null;
    private boolean isNavigationStopped;
    private boolean romuConnected;
    private boolean infoShowed;

    // Interaction with Romu service.
    private BroadcastReceiver romuUpdateReciever = null;
    private ServiceConnection serviceConnection = null; 
    private RomuService romuService = null;
    private LocalBroadcastManager broadcastManager;

    // Bluetooth related.
    private boolean bluetoothEnabled = false;

    // For savedBundle.
    private static final String CURRENT_ROUTE = "Current Route";
    private static final String ROMU_BINDER = "Romu Bundle";
    private static final String ROMU_CONNECTION_STATE = "Romu Connection Status";
    private static final String NAVIGATION_STATUS = "Navigation Status";

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
        broadcastManager = LocalBroadcastManager.getInstance(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if(savedInstanceState == null)
        {
            isNavigationStopped = true;
            romuConnected = false;

            Log.i(LOG_TAG, "Romu service initializing.");
            initRomuService();
        }
        else
        {
            currentRoute = savedInstanceState.getParcelable(CURRENT_ROUTE);
            isNavigationStopped = savedInstanceState.getBoolean(NAVIGATION_STATUS);
            romuConnected = savedInstanceState.getBoolean(ROMU_CONNECTION_STATE);

            Intent romuServiceIntent = new Intent(this, RomuService.class);
            bindRomuService(romuServiceIntent);
        }

        setContentView(R.layout.main);

        initNavigationUI();
        setUpMapIfNeeded();
        Log.i(LOG_TAG, "Map render finishes.");

        Log.i(LOG_TAG, "MainActivity initialized.");
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

        // Wait for some time to let map finish rendering.
        Timer timer = new Timer();
        TimerTask task = new TimerTask()
        {
            @Override
            public void run()
            {
                runOnUiThread(new Runnable()
                        {
                            public void run()
                                {
                                    drawMapAndMoveCamera();
                                }
                        });
            }
        };
        timer.schedule(task, 1000);

        // Set up map object if it is destroyed.
        setUpMapIfNeeded();
    }

    /**
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onPause()
    {
        super.onPause();
    }

    /**
     * Called when the Activity is going to be destroyed.
     */
    @Override
    protected void onDestroy()
    {
        broadcastManager.unregisterReceiver(romuUpdateReciever);
        unbindService(serviceConnection);
        romuService = null;
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        // Save current route.
        savedInstanceState.putParcelable(CURRENT_ROUTE, currentRoute);
        // Save bindding to Romu service.
        savedInstanceState.putBoolean(ROMU_CONNECTION_STATE, romuConnected);
        savedInstanceState.putBoolean(NAVIGATION_STATUS, isNavigationStopped);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }


    // Private methods.
    // ========================================================================

    private void drawMapAndMoveCamera()
    {
        if(currentRoute != null)
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
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(currentRoute.getBounds(), 200));
        }
    }

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
                Log.i(LOG_TAG, "Failed to instantiate google map");
                // TODO: Give prompt to let user fix the problem to let the map
                // running. For instance, enable network.
                return;
            }
            else
                Log.i(LOG_TAG, "Successfully instantiate google map.");
        }

        map.setMyLocationEnabled(true);
    }

    private void initNavigationUI()
    {
        // Initial config for left drawer.
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.option_list);

        drawerList.setAdapter(new ArrayAdapter<String>(
                    this, R.layout.drawer_list_item, drawerItems
                    ));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        // Top navigation bar.
        FragmentTransaction ft = fragmentManager.beginTransaction();
        Fragment fragment = null;
        fragment = new TopNavBarFragment();
        ft.add(R.id.top_toolbar, fragment);
        ft.commit();

    }

    private void updateConnectionIndicator()
    {
        ImageView connectionView = (ImageView) findViewById(R.id.connection_indicator);

        if(romuConnected)
        {
            Log.d(LOG_TAG, "Make indicator show connected.");
            connectionView.setImageResource(R.drawable.connected);
        }
        else
        {
            Log.d(LOG_TAG, "Make indicator show disconnected.");
            connectionView.setImageResource(R.drawable.disconnected);
        }

        connectionView.postInvalidate();
    }

    private void updateBottomCtrlBarState(final int MODE)
    {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        Fragment fragment = null;

        switch(MODE)
        {
            case BOTTOM_CTRL_NAVIGATION_INIT:
                {
                    fragment = new BottomCtrlBarFragment();
                    ft.add(R.id.bottom_ctrl_bar_placeholder, fragment);
                    ft.commit();
                    break;
                }
            case BOTTOM_CTRL_IN_NAVIGATING:
                {
                    ImageButton stateButton = (ImageButton) findViewById(R.id.state_button);
                    stateButton.setBackgroundResource(R.drawable.pause);
                    stateButton.postInvalidate();
                    break;
                }
            case BOTTOM_CTRL_NAVIGATION_PAUSE:
                {
                    ImageButton stateButton = (ImageButton) findViewById(R.id.state_button);
                    if(romuConnected)
                    {
                        stateButton.setBackgroundResource(R.drawable.play);
                        stateButton.setEnabled(true);
                    }
                    else
                    {
                        stateButton.setBackgroundResource(R.drawable.cannot_play);
                        stateButton.setEnabled(false);
                    }
                    stateButton.postInvalidate();
                    break;
                }
            case BOTTOM_CTRL_NAVIGATION_STOP:
                {
                    fragment = fragmentManager.findFragmentById(R.id.bottom_ctrl_bar_placeholder);
                    ft.remove(fragment);
                    ft.commit();
                    break;
                }
            default:
                Log.w(LOG_TAG, "Bottom control bar state out of range.");
        }


    }

    private void switchBottomInfoBarState()
    {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        Fragment fragment = null;

        if(infoShowed)
        {
            fragment = fragmentManager.findFragmentById(R.id.bottom_info);
            ft.remove(fragment);
            ft.commit();
            infoShowed = false;
        }
        else
        {
            fragment = new BottomInfoFragment();
            ft.add(R.id.bottom_info, fragment);
            ft.commit();
            infoShowed = true;
        }


    }

    private void initRomuService()
    {
        Intent romuServiceIntent = new Intent(this, RomuService.class);
        Log.i(LOG_TAG, "Starting Romu Service...");
        startService(romuServiceIntent);
        bindRomuService(romuServiceIntent);
    }

    private void bindRomuService(Intent romuServiceIntent)
    {
        // Connection with Romu service for better interface.
        serviceConnection = new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service)
            {
                Log.i(LOG_TAG, "Romu service connected.");
                RomuService.LocalBinder binder = (RomuService.LocalBinder) service;
                romuService = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName)
            {
                Log.i(LOG_TAG, "Romu service disconnected.");
                romuService = null;
            }
        };

        registerRomuReceiver();

        Log.i(LOG_TAG, "Binding romu service...");
        bindService(romuServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void registerRomuReceiver()
    {
        // BroadcastReceiver to receive updates broadcast from Romu service.
        romuUpdateReciever = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                final String action = intent.getAction();

                if(RomuService.ACTION_BT_NOT_ENABLED.equals(action))
                {
                    enableBluetooth();
                }
                else if(RomuService.DEVICE_FOUND.equals(action))
                {
                    // DEVICE_FOUND is for scanning device, not used for the
                    // time being.
                }
                else if(RomuService.ROMU_CONNECTED.equals(action))
                {
                    // Notify user that bluetooth device has disconnected.
                    Toast.makeText(RomuActivity.this, "Romu Connected", Toast.LENGTH_SHORT);
                    romuConnected = true;
                    updateConnectionIndicator();
                    if(findViewById(R.id.state_button) != null)
                    {
                        updateBottomCtrlBarState(BOTTOM_CTRL_NAVIGATION_PAUSE);
                    }
                }
                else if(RomuService.ROMU_DISCONNECTED.equals(action))
                {
                    // Notify user that bluetooth device has disconnected.
                    Toast.makeText(RomuActivity.this, "Romu Disconnected", Toast.LENGTH_SHORT);
                    romuConnected = false;
                    updateConnectionIndicator();
                    if(!isNavigationStopped)
                    {
                        updateBottomCtrlBarState(BOTTOM_CTRL_NAVIGATION_PAUSE);
                        romuService.stopNavigation();
                    }
                }
                else if(RomuService.ROMU_WRONG.equals(action))
                {
                    Toast.makeText(
                            RomuActivity.this,
                            "Romu is not nearby or malfunctioning...",
                            Toast.LENGTH_SHORT
                            );
                }
            }
        };
        broadcastManager.registerReceiver(romuUpdateReciever, romuUpdateIntentFilter());
    }

    private void stopRomuService()
    {
        Intent romuServiceIntent = new Intent(this, RomuService.class);
        stopService(romuServiceIntent);
    }

    private void enableBluetooth()
    {
        // If bluetooth is enabled or in progress of being enabled, since it is
        // asynchronous, do nothing.
        if(!bluetoothEnabled)
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST);
        }
    }

    /**
     * Double confirmation that the user should enable bluetooth using dialog.
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
                            Log.i(LOG_TAG, "User decided not to open bluetooth. Just continue.");
                        }
                    }
            )
            .create();

        RomuDialogFragment fragment = new RomuDialogFragment();
        fragment.setDialog(dialog);

        fragment.show(fragmentManager, "bluetooth_comfirmation");
    }

    // Naptic Navigation Related.
    // =================================================================================

    /**
     * Callback for starting haptic navigating.
     */
    public void onNavigationStateChange(View view)
    {
        if(romuService == null)
        {
            Toast.makeText(this, "Romu service is not ready.", Toast.LENGTH_SHORT).show();
        }
        else
        {
            if(isNavigationStopped)
            {
                romuService.startNavigation();
                updateBottomCtrlBarState(BOTTOM_CTRL_IN_NAVIGATING);
                isNavigationStopped = false;
            }
            else
            {
                romuService.stopNavigation();
                updateBottomCtrlBarState(BOTTOM_CTRL_NAVIGATION_PAUSE);
                isNavigationStopped = true;
            }
        }
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
        // Since we are navigating, romu serice must be present.
        assert romuService != null :
                "Romu service should not be null when trying to stop navigation.";

        romuService.stopNavigation();
        updateBottomCtrlBarState(BOTTOM_CTRL_NAVIGATION_STOP);
        isNavigationStopped = true;
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
            LatLng currentLatLng = romuService.getCurrentLatLng();
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
            drawMapAndMoveCamera();

            InputMethodManager im = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE); 
            im.hideSoftInputFromWindow(destAddrAutoCompleteTextView.getWindowToken(), 0);

            // Pass the new route to Romu service.
            romuService.setRoute(currentRoute);

            // Pop up the bottom control pane.
            infoShowed = false;
            updateBottomCtrlBarState(BOTTOM_CTRL_NAVIGATION_INIT);
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

    public void onShowInfo(View view)
    {
        switchBottomInfoBarState();
    }

    // General UI.
    // =================================================================================
    public void onQuit(View view)
    {
        stopRomuService();
        finish();
    }

    public void onToMyLocation(View view)
    {
        if(romuService == null)
        {
            Toast.makeText(this, "Romu service is not ready.", Toast.LENGTH_SHORT).show();
        }
        else
        {
            LatLng currentLatLng = romuService.getCurrentLatLng();
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        currentLatLng, 15
                        ));
        }


    }

    public void onConnect(View view)
    {
        if(romuService != null)
        {
            if(!romuService.connect())
            {
                Toast.makeText(this, "Cannot connect to device", Toast.LENGTH_SHORT);
            }
        }
    }

    // Show settings by left drawer when clicked.
    public void showDrawer(View view)
    {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        View leftDrawer = (View) findViewById(R.id.left_drawer);
        drawerLayout.openDrawer(leftDrawer);
    }

    // Drawer list click listener.
    private class DrawerItemClickListener implements ListView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            String item = parent.getItemAtPosition(position).toString();
            if(item.equals(SETTINGS))
            {
                Intent intent = new Intent(RomuActivity.this, GetInfoActivity.class);
                startActivity(intent);
            }
        }
    }

    // Communication with UI.
    // =================================================================================
    public void onTopNavBarAttached()
    {

        destAddrAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.main_dest_addr);
        destAddrAutoCompleteTextView.setAdapter(
                new PlacesAutoCompleteAdapter(this, R.layout.list_item, R.id.item)
                );

        updateConnectionIndicator();
    }

    public void onBottomCtrlBarAttached()
    {
        if(isNavigationStopped)
            updateBottomCtrlBarState(BOTTOM_CTRL_NAVIGATION_PAUSE);
        else
            updateBottomCtrlBarState(BOTTOM_CTRL_IN_NAVIGATING);
    }

    // Communication with Romu service.
    // =================================================================================
    private static IntentFilter romuUpdateIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RomuService.ROMU_CONNECTED);
        intentFilter.addAction(RomuService.ROMU_DISCONNECTED);
        intentFilter.addAction(RomuService.ROMU_WRONG);
        // intentFilter.addAction(RomuService.BEGIN_NAVIGATION);
        intentFilter.addAction(RomuService.DEVICE_FOUND);
        intentFilter.addAction(RomuService.ACTION_BT_NOT_ENABLED);
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
                    Log.i(LOG_TAG, "Location fetcher returned.");
                    if(resultCode != RESULT_OK)
                    {
                        Log.i(LOG_TAG, "There is something wrong with location fetcher.");
                        // TODO: code the error handler.
                        break;
                    }
                    Log.i(LOG_TAG, "Location fetcher finished successfully.");
                    Bundle bundle   = data.getExtras();
                    startAddr    = bundle.getString(LocationFetcherActivity.START_ADDR_STRING);
                    Log.i(LOG_TAG, "Start location fetched: " + startAddr);
                    destAddr     = bundle.getString(LocationFetcherActivity.DEST_ADDR_STRING);
                    Log.i(LOG_TAG, "Destination fetched: " + destAddr);

                    getRouteByRequestingGoogle(false);

                    break;
                }
            case ENABLE_BT_REQUEST:
                {
                    Log.i(LOG_TAG, "User cancelled enable bluetooth dialog. Confirming.");

                    if(resultCode == RESULT_OK)
                        bluetoothEnabled = true;
                    else
                        showBluetoothConfirmDialog();

                    break;
                }
            default:
                Log.e(LOG_TAG, "Activity result out of range.");
        }
    }
}

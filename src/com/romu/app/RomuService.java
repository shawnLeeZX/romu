package com.romu.app;

import java.util.ArrayList;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

/**
 * class RomuService
 * @author Shawn, Andrew
 *
 * This is the background service for Romu to connect the app with relevant
 * service and firmware(wearable). It handles the connection between the app and
 * Google Play Service, using network, the app and the firmware using bluetooth
 * low energy(Bluetooth Gatt). Google Play Services provides location service.
 *
 * Also, it provides inferfaces to application binded to it to start and stop
 * relevant service.
 *
 * TODO: code to receive action from BT and propagate it to MainActivity.
 */
public class RomuService extends Service implements
    ConnectionCallbacks,
    OnConnectionFailedListener,
    LocationListener
{
    public static final String LOG_TAG = "Romu: RomuService";

    // For location service.
    private boolean locationServiceConnected = false;

    private LocationRequest locationRequest     = null;

    private LocationClient locationClient = null;

    // Navigation status.
    private Location currentLocation = null;
    private Route currentRoute;
    private boolean listenToNavigationUpdates = false;
    private static long updateFrequencyDefault = 17000;
    private static long listenTimeInterval = 5000;
    private static long updateScalar = 7000;
    private static long currentTime;
    private static long startTime;
    private static boolean enoughTimeHasPassed=false;  
    private ArrayList<LatLng> waypoints = null;
    private static ArrayList<Location> progress;
    private int currentIndex;
    private Location currentDestination;
    private Location finalDestination;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final double GPS_RADIUS = 35;
    private int deltaOn = 0;
    private int distanceOn = 1;
    private int thresholdForLost =30;
    private static final long locationUpdateInterval = 7000;

    //Navigation commands
    public static final String PAUSE_NAV_COMMAND = "#6#1#";
    public static final String BEGIN_NAV_COMMAND = "#5#1#";
    public static final String ARRIVED_FINAL_COMMAND = "#3#1#";
    public static final String ARRIVED_CURRENT_COMMAND = "#4#1#";
    public static final String LOCATION_UPDATE = "LOCATION UPDATE";
    public static final String ARRIVED_CURRENT = "ARRIVED AT CURRENT WAYPOINT";
    public static final String ARRIVED_FINAL = "ARRIVED AT DESTINATION";


    // Romu service related.
    private static final int FOREGROUND_ID = 1337;
    private final IBinder binder = new LocalBinder();
    private LocalBroadcastManager broadcastManager;

    public static final String DEVICE_FOUND  = "DEVICE FOUND";
    public static final String ACTION_BT_NOT_ENABLED = 
        "com.romu.app.ACTION_BT_NOT_ENABLED";
    public static final String ROMU_CONNECTED = 
        "com.romu.app.ROMU_CONNECTED";
    public static final String ROMU_DISCONNECTED = 
        "com.romu.app.ROMU_DISCONNECTED";
    public static final String ROMU_WRONG = 
        "com.romu.app.ROMU_WRONG";
    public static final String EXTRA_DATA = 
         "com.romu.app.EXTRA_DATA";


    // BT service related.
    private ServiceConnection serviceConnection;
    private BluetoothLEService btService;
    private BroadcastReceiver btUpdateReciever;
    private boolean btServiceConnected;

    @Override
    public void onCreate()
    {
        Log.i(LOG_TAG, "Creating Romu Services...");

        btServiceConnected = false;

        broadcastManager = LocalBroadcastManager.getInstance(this);
        makeForegroundService();
        initLocationService();
    }

    // Private methods.
    // ================================================================================
    //compares two locations to determine the better locaiton based on accuracy, time, provider   
     private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }


    //Chanages frequency of location update based on promimity to current destiniation
    private double chooseDistanceTier(float distanceTo){
        double distanceScalar = 0;
        if(distanceTo>=250)distanceScalar = 1;
        else if(distanceTo<250&&distanceTo>=200)distanceScalar =0.8;
        else if(distanceTo<200&&distanceTo>=150)distanceScalar =0.7;
        else if(distanceTo<150&&distanceTo>=100)distanceScalar =0.6;
        else if(distanceTo<100&&distanceTo>=50)distanceScalar =0.5;
        else if(distanceTo<50)distanceScalar =0.1;
        return distanceScalar;
    }

    /*
    Uses a scalar for proximity and a scalar for "degress of lost" to modify the frequency of update.
    Only one scalar can play a role at a time
    */
    private void updateFrequencyOfLocationUpdate(double distanceScalar,double deltaScalar){
        listenTimeInterval = (long) (updateFrequencyDefault - (distanceOn*(1-distanceScalar)*updateScalar) -
         (deltaOn*deltaScalar*updateScalar));
    }

    private boolean checkRangeOf(float heading){
        return heading>=(-180)&& heading<=180;
        
    }

        /*
    function to determine if we are ready to send a new update via btUpdate

    */
    private void writeUpdate(String action, String command){
            if(listenToNavigationUpdates){
                currentTime = System.currentTimeMillis();
                if((currentTime-startTime) >=  listenTimeInterval){
                    startTime = currentTime;
                    enoughTimeHasPassed = true;
            }
            if(enoughTimeHasPassed || command.equals(ARRIVED_FINAL)){
                enoughTimeHasPassed=false;
                sendCommand(command);
            }
        }
        
    }



     /*
    Bulk of navigation computation. Uses the current location and 
    current destination to generate a signal for the bluetooth device
    based on current orientation and heading to the destination 

    */
    private void makeUseOfLocation(Location location){    
        if(location== null)return; 
        Location lastKnownLocation = locationClient.getLastLocation();
        if(isBetterLocation(lastKnownLocation, currentLocation)){
            currentLocation = lastKnownLocation;
            location = lastKnownLocation;
        }
        float delta;
        float headingToDestination = location.bearingTo(currentDestination);
        if(!checkRangeOf(headingToDestination))return;
        if(headingToDestination<0)headingToDestination+=360;
        float currentBearing = location.getBearing();
        float distanceTo = location.distanceTo(currentDestination);
        if(distanceOn==1){
            double distanceScalar = chooseDistanceTier(distanceTo);
            updateFrequencyOfLocationUpdate(distanceScalar, 0);
        }
        if(distanceTo <= location.getAccuracy()||distanceTo<=GPS_RADIUS){
            String action = ARRIVED_CURRENT;
            if(currentDestination.equals(finalDestination)){
                action = ARRIVED_FINAL;
                String arrived = ARRIVED_FINAL_COMMAND;
                writeUpdate(action, arrived);
                stopNavigation();
                return;
            }else{
                String currentArrived = ARRIVED_CURRENT_COMMAND;
                writeUpdate(action, currentArrived);
                updateCurrentDestination();
                return;
            }
            
        }
        String command="";
        if(!location.hasBearing()||currentBearing==0){
            command +="#2#";
            command += String.valueOf(headingToDestination);
            command +="#";
            writeUpdate(LOCATION_UPDATE, command);
            return;
        }
        if(currentBearing>0.0){
            delta = headingToDestination - currentBearing;
            if(delta<0)delta+=360;
            if(delta>thresholdForLost){
                distanceOn = 0;
                deltaOn = 1;
                double deltaScalar = 180-delta;
                if(deltaScalar<0)deltaScalar+=180;
                updateFrequencyOfLocationUpdate(0,(deltaScalar/180));
            }else {
                deltaOn = 0;
                distanceOn = 1;
            }
            command +="#1#";
            command += String.valueOf(delta);
            command +="#";
            writeUpdate(LOCATION_UPDATE, command);
            return;
        }
    }

    private void updateCurrentDestination() {
        
        
        //increment segment is boolean : false if next destination is final
        if(currentRoute.incrementCurrentSegment())
            currentDestination = makeLocation(currentRoute.getCurrentDestination());
        else currentDestination = finalDestination;
        //int currentIndex = currentRoute.getCurrentIndex();
        //broadcastUpdate(ARRIVED_CURRENT, currentRoute);
        listenTimeInterval = 500;
    }





    private Location makeLocation(LatLng ll)
    {
        Location location = new Location("");
        location.setLatitude(ll.latitude);
        location.setLongitude(ll.longitude);
        return location;
    }

    private void sendCommand(String command)
    {
        btService.sendCommand(command);
    }

    private void makeForegroundService()
    {
        Notification notification = makeNotification();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(FOREGROUND_ID, notification);
    }
    private void initLocationService()
    {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(locationUpdateInterval);

        locationClient = new LocationClient(this, this, this);
        Log.i(LOG_TAG, "Location service initialized.");
        startLocationService();
    }

    /**
     * Notification for foreground service. If it is clicked then we open up
     * into the main activity
     */

    private Notification makeNotification()
    {
        // TODO: Display the text instruction on notification.
        Intent intent = new Intent(this, RomuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
        notification.setContentTitle("Romu");
        notification.setSmallIcon(R.drawable.romuwhite);
        notification.setWhen(System.currentTimeMillis());
        notification.setContentText("Romu");
        notification.setTicker("Romu");
        notification.setOngoing(true);
        notification.setPriority(Notification.PRIORITY_HIGH);
        notification.setAutoCancel(false);
        notification.setContentIntent(pendIntent);

        return notification.build();
    }
    // Service related.
    // ================================================================================

    /**
     * Called when binded by other thread.
     */
    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        return super.onUnbind(intent);
    }

    /**
     * Binder to expose public methods of this service.
     */
    public class LocalBinder extends Binder
    {
        RomuService getService()
        {
            return RomuService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i("Romu Service", "Received start id " + startId + ": " + intent);
        // Keeps OS from shutting down service when activity has changed state
        return START_STICKY;
    }

    // The service is no longer used and is being destroyed
    @Override
    public void onDestroy()
    {
        Log.i(LOG_TAG, "Romu service is going to be destroyed.");

        stopLocationService();
        stopBluetoothService();

        Log.i(LOG_TAG, "Romu service destroyed.");
    }

    // Interfaces.
    // ================================================================================
    public void startLocationService()
    {
        if(!locationServiceConnected)
        {
            Log.i(LOG_TAG, "Starting location service...");
            locationClient.connect();
        }
    }

    public void stopLocationService()
    {
        if(locationServiceConnected)
        {
            Log.i(LOG_TAG, "Stopping location service...");
            locationClient.removeLocationUpdates(this);
            locationClient.disconnect();
            locationServiceConnected = false;
        }
    }

    public LatLng getCurrentLatLgn()
    {
        if(currentLocation == null)
        {
            Toast.makeText(this, "Location Service not ready.", Toast.LENGTH_SHORT).show();
            return null;
        }

        LatLng latLng = new LatLng(
            currentLocation.getLatitude(),
            currentLocation.getLongitude()
        );

        return latLng;
    }


    /**
     * Note that before calling this function, {@link #setRoute(Route route)}
     * should be called to set current route first.
     */
    public void startNavigation()
    {
        assert currentRoute != null : "Route is null. Program should not reach here!";
        Log.i(LOG_TAG, "Navigation started.");
        listenToNavigationUpdates = true;
        startTime = System.currentTimeMillis();
        finalDestination = makeLocation(currentRoute.getEndLocation());
        currentLocation = locationClient.getLastLocation();
        waypoints = currentRoute.getPoints();
        sendCommand(BEGIN_NAV_COMMAND);
        currentDestination = makeLocation(currentRoute.getCurrentDestination());
        listenTimeInterval = 1000;
    }



    public void startBluetoothService()
    {
        if(!btServiceConnected)
        {
            Intent btServiceIntent = new Intent(this, BluetoothLEService.class);
            Log.i(LOG_TAG, "Starting Bluetooth Service...");
            startService(btServiceIntent);
            // Connection with BT service for better interface.
            serviceConnection = new ServiceConnection()
            {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder service)
                {
                    Log.i(LOG_TAG, "Bluetooth service connected.");
                    BluetoothLEService.LocalBinder binder = (BluetoothLEService.LocalBinder) service;
                    btService = binder.getService();
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName)
                {
                    Log.i(LOG_TAG, "Bluetooth service disconnected.");
                    btService = null;
                }
            };

            // BroadcastReceiver to receive updates broadcast from Romu service.
            btUpdateReciever = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    final String action = intent.getAction();

                    if(action == BluetoothLEService.ACTION_BLUETOOTH_NOT_ENABLED)
                    {
                        broadcastUpdate(ACTION_BT_NOT_ENABLED);
                    }
                    else if(action == BluetoothLEService.DEVICE_FOUND)
                    {
                        broadcastUpdate(DEVICE_FOUND);
                    }
                    else if(action == BluetoothLEService.ACTION_GATT_CONNECTED)
                    {
                        broadcastUpdate(ROMU_CONNECTED);
                    }
                    else if(action == BluetoothLEService.ACTION_GATT_DISCONNECTED)
                    {
                        broadcastUpdate(ROMU_DISCONNECTED);
                    }
                    else if(action == BluetoothLEService.ACTION_GATT_WRONG)
                    {
                        broadcastUpdate(ROMU_WRONG);
                    }
                }
            };
            broadcastManager.registerReceiver(btUpdateReciever, btUpdateIntentFilter());

            Log.i(LOG_TAG, "Binding BluetoothLE service...");
            bindService(btServiceIntent, serviceConnection, BIND_AUTO_CREATE);

            btServiceConnected = true;
        }
    }

    public void stopBluetoothService()
    {
        if(btServiceConnected)
        {
            broadcastManager.unregisterReceiver(btUpdateReciever);
            unbindService(serviceConnection);
            btService = null;

            Intent btServiceIntent = new Intent(this, BluetoothLEService.class);
            stopService(btServiceIntent);
            btServiceConnected = false;
        }
    }

    public void stopNavigation()
    {
        Log.i(LOG_TAG, "Navigation stopped.");
        listenToNavigationUpdates = false;

        sendCommand(PAUSE_NAV_COMMAND);

    }

    public void setRoute(Route route)
    {
        currentRoute = route;

    }

    // Communication with RomuActivity.
    // ================================================================================
    private void broadcastUpdate(final String action)
    {
        final Intent intent = new Intent(action);
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, String extra){
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, extra);
        broadcastManager.sendBroadcast(intent);
    }


    // Communication with BluetoothLE service.
    // =================================================================================
    private static IntentFilter btUpdateIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothLEService.ACTION_BLUETOOTH_NOT_ENABLED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLEService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_WRONG);
        intentFilter.addAction(BluetoothLEService.DEVICE_FOUND);

        return intentFilter;
    }

    // Methods related to connection with Location Service.
    // ================================================================================

    /**
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle)
    {
        // Display the connection status
        Toast.makeText(this, "Location Service Connected", Toast.LENGTH_SHORT).show();
        locationServiceConnected = true;

        currentLocation = locationClient.getLastLocation();
        locationClient.requestLocationUpdates(locationRequest, this);
    }

    /**
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    public void onDisconnected()
    {
        // Display the connection status
        Log.i(LOG_TAG, "Location service disconnnected.");
        Toast.makeText(this, "Location Serice Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
        locationServiceConnected = false;
    }

    /**
     * Called by Location Services if the attempt to
     * Location Services fails.
     * TODO: clean this up when finishing writing route parsing.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        // TODO: finish this when notifyUser of this service done since it uses
        // notify.

        /**
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */

        // if (connectionResult.hasResolution())
        // {
            // try
            // {
                // // Start an Activity that tries to resolve the error
                // connectionResult.startResolutionForResult(
                        // this,
                        // CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /**
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            // } catch (IntentSender.SendIntentException e)
            // {
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

    // Define the callback method that receives location updates.
    @Override
    public void onLocationChanged(Location location)
    {
        // Keep track of current location.
        currentLocation = location;
        makeUseOfLocation(location);
    }
}

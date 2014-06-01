package com.romu.app;

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
    private long locationUpdateInterval         = 7000;

    private LocationClient locationClient = null;

    // Navigation status.
    private Location currentLocation = null;
    private Route currentRoute;

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
        startLocationService();
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

    public void startNavigation(Route route)
    {
        // TODO: enable location update.(move location update here)
        currentRoute = route;
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
        // TODO: disable location update.(move location dis update here)
        stopBluetoothService();
    }

    // Communication with RomuActivity.
    // ================================================================================
    private void broadcastUpdate(final String action)
    {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
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

        // Report to the UI that the location was updated
        // TODO: remove when finishing debugging.
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}

package com.romu.app;

import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
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
 */
public class RomuService extends Service implements
    ConnectionCallbacks, OnConnectionFailedListener
{
    public static final String LOG_TAG = "Romu: RomuService";

    // For location service.
    private boolean locationServiceConnected = false;

    private LocationRequest locationRequest     = null;
    private long locationUpdateInterval         = 7000;

    private LocationClient locationClient = null;

    // Binder given to clients.
    private final IBinder binder = new LocalBinder();

    @Override
    public void onCreate()
    {
        // TODO: set up bluetooth service.

        initializeLocationService();
    }

    // Private methods.
    // ================================================================================
    private void initializeLocationService()
    {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(locationUpdateInterval);

        locationClient = new LocationClient(this, this, this);
    }

    // Methods related to binded thread.
    // ================================================================================

    /**
     * Called when binded by other thread.
     */
    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
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

    // Interfaces.
    // ================================================================================
    public void startLocationService()
    {
        if(!locationServiceConnected)
        {
            locationClient.connect();
        }
    }

    public void stopLocationService()
    {
        if(locationServiceConnected)
        {
            locationClient.disconnect();
        }
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
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    }

    /**
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    public void onDisconnected()
    {
        // Display the connection status
        Log.d(LOG_TAG, "Location service disconnnected.");
        Toast.makeText(this, "Location Serice Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
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
}

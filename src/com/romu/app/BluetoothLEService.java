package com.romu.app;

import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * class BluetoothLEService
 * @author Shawn, Andrew
 *
 * This service is responsible for maintaining the connection with the bluetooth
 * low energy device and providing interfaces to exchange data and control
 * signal with it.
 */
public class BluetoothLEService extends Service
{
    private final String LOG_TAG = "Romu: BluetoothLEService";

    // Use info.
    private SharedPreferences sharedPref;

    // Service related.
    private final IBinder binder = new LocalBinder();
    private LocalBroadcastManager broadcastManager;

    // Bluetooth related.
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice device;
    private BluetoothGattCallback gattCallback;

    private String macAddress;
    private int connectionState;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public static String FOUND_DEVICE  = "DEVICE FOUND";

    // Action names.
    public final static String ACTION_GATT_CONNECTED =
            "com.romu.app.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.romu.app.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.romu.app.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.romu.app.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.romu.app.EXTRA_DATA";
    public final static String ACTION_GATT_RSSI = "ACTION_GATT_RSSI";

    // UUID.
    public final static UUID UUID_BLE_SHIELD_TX = UUID
            .fromString(GattAttributes.BLE_SHIELD_TX);
    public final static UUID UUID_BLE_SHIELD_RX = UUID
            .fromString(GattAttributes.BLE_SHIELD_RX);
    public final static UUID UUID_BLE_SHIELD_SERVICE = UUID
            .fromString(GattAttributes.BLE_SHIELD_SERVICE);

    @Override
    public void onCreate()
    {
        Log.d(LOG_TAG, "Creating BluetoothLE Services...");

        broadcastManager = LocalBroadcastManager.getInstance(this);
        initializeBluetooth();
    }

    // Private methods..
    // ================================================================================
    private void initializeBluetooth()
    {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Read the MAC address of bluetooth.
        sharedPref = getSharedPreferences(
                getString(R.string.mac_address_file_key),
                Context.MODE_PRIVATE
                );
        macAddress = sharedPref.getString(
                getString(R.string.mac_address_file_key),
                null
                );
        Log.d(LOG_TAG, "Mac Address Read: " + macAddress + ".");

        connectionState = STATE_DISCONNECTED;

        // Implements callback methods for GATT events that the app cares about.  For example,
        // connection change and services discovered.
        gattCallback = new BluetoothGattCallback()
        {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
            {
                String intentAction;
                switch (newState)
                {
                    case BluetoothProfile.STATE_CONNECTED:
                        {
                            intentAction = ACTION_GATT_CONNECTED;
                            connectionState = STATE_CONNECTED;
                            broadcastUpdate(intentAction);
                            Log.i(LOG_TAG, "Connected to GATT server.");
                            // Attempts to discover services after successful connection.
                            Log.i(LOG_TAG, "Attempting to start service discovery:" +
                                    bluetoothGatt.discoverServices());
                        }
                    case BluetoothProfile.STATE_DISCONNECTED:
                        {
                            intentAction = ACTION_GATT_DISCONNECTED;
                            connectionState = STATE_DISCONNECTED;
                            Log.i(LOG_TAG, "Disconnected from GATT server.");
                            broadcastUpdate(intentAction);
                        }
                }
            }

            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
            {
                if (status == BluetoothGatt.GATT_SUCCESS)
                {
                    broadcastUpdate(ACTION_GATT_RSSI, rssi);
                }
                else
                {
                    Log.w(LOG_TAG, "onReadRemoteRssi received: " + status);
                }
            };

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status)
            {
                if (status == BluetoothGatt.GATT_SUCCESS)
                {
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                } else
                {
                    Log.w(LOG_TAG, "onServicesDiscovered received: " + status);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic,
                                             int status)
            {
                if (status == BluetoothGatt.GATT_SUCCESS)
                {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic)
            {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        };
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
        BluetoothLEService getService()
        {
            return BluetoothLEService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d("BluetoothLE Service", "Received start id " + startId + ": " + intent);

        // TODO: change hard coded address to reading from prefer.
        connect("20:CD:39:9E:F1:40");

        // Keeps OS from shutting down service when activity has changed state
        return START_STICKY;
    }

    // The service is no longer used and is being destroyed
    @Override
    public void onDestroy()
    {
        Log.d(LOG_TAG, "BluetoothLE service is going to be destroyed.");


        Log.d(LOG_TAG, "BluetoothLE service destroyed.");
    }

    // Bluetooth related.
    // ================================================================================

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return  Return true if the connection is initiated successfully. The
     *          connection result is reported asynchronously through the {@code
     *          BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt,
     *          int, int)} callback.
     */
    public boolean connect(final String address)
    {
        if (address == null)
        {
            Log.w(LOG_TAG, "MAC address is malformed.");
            return false;
        }

        if(!bluetoothAdapter.isEnabled())
        {
            // TODO: Notify UI.
        }

        // Previously connected device.  Try to reconnect.
        if (macAddress != null
                && address.equals(macAddress)
                && bluetoothGatt != null
                )
        {
            Log.d(LOG_TAG, "Trying to use an existing bluetoothGatt for connection.");
            if (bluetoothGatt.connect())
            {
                connectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

       device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null)
        {
            Log.w(LOG_TAG, "Device not found. Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect parameter to false.
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
        Log.d(LOG_TAG, "Trying to create a new connection.");
        macAddress = address;
        connectionState = STATE_CONNECTING;
        return true;
    }

    private void broadcastUpdate(final String action)
    {
        final Intent intent = new Intent(action);
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, int rssi)
    {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, String.valueOf(rssi));
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic)
    {
        final Intent intent = new Intent(action);

        if (UUID_BLE_SHIELD_RX.equals(characteristic.getUuid()))
        {
            final byte[] rx = characteristic.getValue();
            intent.putExtra(EXTRA_DATA, rx);
        }
        broadcastManager.sendBroadcast(intent);
    }
}

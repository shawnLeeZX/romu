package com.romu.app;

import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

/**
 * class BluetoothLEService
 * @author Shawn, Andrew
 *
 * This service is responsible for maintaining the connection with the bluetooth
 * low energy device and providing interfaces to exchange data and control
 * signal with it.
 */
public class BluetoothLEService extends Service implements LeScanCallback
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
    private BluetoothGattService gattService;
    private BluetoothGattCharacteristic characteristicRx;
    private BluetoothGattCharacteristic characteristicTx;

    private String macAddress;
    private int connectionState;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final int STATE_READY = 3;


    // Action names.
    public final static String DEVICE_FOUND  = "DEVICE FOUND";
    public final static String ACTION_BLUETOOTH_NOT_ENABLED =
            "com.romu.app.ACTION_BLUETOOTH_NOT_ENABLED";

    public final static String ACTION_GATT_WRONG =
            "com.romu.app.ACTION_GATT_WRONG";
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
        Log.i(LOG_TAG, "Creating BluetoothLE Services...");

        broadcastManager = LocalBroadcastManager.getInstance(this);
        initBluetooth();
    }

    // Private methods.
    // ================================================================================
    private void initBluetooth()
    {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Read the MAC address of bluetooth.
        sharedPref = getSharedPreferences(
                getString(R.string.user_info_file_key),
                Context.MODE_PRIVATE
                );
        // Uncomment this when finishing debugging.
        // macAddress = "20:CD:39:9E:F1:40";
        macAddress = sharedPref.getString(
                getString(R.string.mac_address),
                null
                );
        Log.i(LOG_TAG, "Mac Address Read: " + macAddress + ".");

        connectionState = STATE_DISCONNECTED;

        // Implements callback methods for GATT events that the app cares about.  For example,
        // connection change and services discovered.
        gattCallback = new BluetoothGattCallback()
        {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
            {
                if(status == BluetoothGatt.GATT_SUCCESS)
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
                                Toast.makeText(BluetoothLEService.this, "Bluetooth Connected.", Toast.LENGTH_SHORT).show();
                                break;
                            }
                        case BluetoothProfile.STATE_DISCONNECTED:
                            {
                                intentAction = ACTION_GATT_DISCONNECTED;
                                connectionState = STATE_DISCONNECTED;
                                Log.i(LOG_TAG, "Disconnected from GATT server.");
                                broadcastUpdate(intentAction);
                                Toast.makeText(BluetoothLEService.this, "Bluetooth Disonnected.", Toast.LENGTH_SHORT).show();
                                break;
                            }
                        default:
                            Log.w(LOG_TAG, "Bluetooth state out of Range.");
                    }
                }
                else
                {
                    Log.i(LOG_TAG, "Cannot find device. Do not try connecting.");
                    bluetoothGatt.disconnect();
                    broadcastUpdate(ACTION_GATT_WRONG);
                    connectionState = STATE_DISCONNECTED;
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
                    if (bluetoothGatt == null)
                    {
                        Log.w(LOG_TAG, "GATT BROKEN");
                        return;
                    }
                    gattService = bluetoothGatt.getService(UUID_BLE_SHIELD_SERVICE);
                    assert gattService != null : "gatt Service should not be null.";
                    characteristicTx = gattService.getCharacteristic(BluetoothLEService.UUID_BLE_SHIELD_TX);
                    if(characteristicTx ==null)
                        Log.d(LOG_TAG, "Characterisitc is null");

                    characteristicRx = gattService.getCharacteristic(
                            BluetoothLEService.UUID_BLE_SHIELD_RX
                            );
                    setCharacteristicNotification(characteristicRx, true);
                    connectionState = STATE_READY;
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
                Log.i(LOG_TAG, "Signal received from hardware.");
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        };

        connect();
    }

    // Private methods.
    // ================================================================================
    // This one is not used. Just leave it.
    private void startScan()
    {
        bluetoothAdapter.startLeScan(this);
        new Handler().postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        bluetoothAdapter.stopLeScan(BluetoothLEService.this);
                    }
                }
                , 2500);

    }

    // Interfaces.
    // ================================================================================
    /**
     * Interface to send command to bluetooth device. If it succeeds beginning
     * sending, it will return true, otherwise false is returned. However, note
     * that beginning sending does not mean the write will be successful. Check
     * the result in {@link #onCharacteristicWrite() onCharacteristicWrite}.
     *
     * @param   command Command to send to bluetooth device.
     *
     * @return  boolean Whether write is successfully began.
     */
    public void sendCommand(String command)
    {
        // User may turn off bluetooth some time, do the check again.
        // If bluetooth is not ready, just ignore the command.
        if(bluetoothAdapter == null)
        {
            broadcastUpdate(ACTION_BLUETOOTH_NOT_ENABLED);
        }
        else if(connectionState == STATE_READY)
        {
            Log.i(LOG_TAG, "Writing: " + command);
            final byte[] bytes = command.getBytes();
            characteristicTx.setValue(bytes);
            bluetoothGatt.writeCharacteristic(characteristicTx);
        }
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
        Log.i("BluetoothLE Service", "Received start id " + startId + ": " + intent);

        connect(macAddress);

        // Keeps OS from shutting down service when activity has changed state
        return START_STICKY;
    }

    // The service is no longer used and is being destroyed
    @Override
    public void onDestroy()
    {
        Log.i(LOG_TAG, "BluetoothLE service is going to be destroyed.");

        bluetoothGatt.close();

        Log.i(LOG_TAG, "BluetoothLE service destroyed.");
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
    private boolean connect(final String address)
    {
        if (address == null)
        {
            Log.w(LOG_TAG, "MAC address is malformed.");
            return false;
        }

        if(!bluetoothAdapter.isEnabled())
        {
            broadcastUpdate(ACTION_BLUETOOTH_NOT_ENABLED);
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (macAddress != null
                && address.equals(macAddress)
                && bluetoothGatt != null
                )
        {
            Log.i(LOG_TAG, "Trying to use an existing bluetoothGatt for connection.");
            if (bluetoothGatt.connect())
            {
                connectionState = STATE_CONNECTING;
                stopTryingWhenCannotConnectLong();
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
        Log.i(LOG_TAG, "Trying to create a new connection.");
        macAddress = address;
        connectionState = STATE_CONNECTING;
        stopTryingWhenCannotConnectLong();
        return true;
    }

    public boolean connect()
    {
        if(connectionState == STATE_DISCONNECTED)
        {
            Toast.makeText(
                    this,
                    "Scanning and searching romu...",
                    Toast.LENGTH_SHORT
                    ).show();
            return connect(macAddress);
        }
        else
            Toast.makeText(
                    this,
                    "Already trying to search for romu...",
                    Toast.LENGTH_SHORT
                    ).show();

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
            Log.d("Bluetooth print::::::", new String(rx));
        }
        broadcastManager.sendBroadcast(intent);
    }


    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled)
    {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(LOG_TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        if (UUID_BLE_SHIELD_RX.equals(characteristic.getUuid()))
        {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG)
                    );
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the {@code
     * BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt,
     * android.bluetooth.BluetoothGattCharacteristic, int)} callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        if (bluetoothAdapter == null || bluetoothGatt == null)
        {
            Log.w(LOG_TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
    {
        Log.i(LOG_TAG, "New LE Device: " + device.getName() + " @ " + rssi);

        /*
         * We are looking for SensorTag devices only, so validate the name
         * that each device reports before adding it to our collection
         */
        if (macAddress.equals(device.getAddress()))
        {
            broadcastUpdate(DEVICE_FOUND);
            bluetoothAdapter.stopLeScan(this);
        }
    }

    // Private methods 
    // ==============================================================================
    private void stopTryingWhenCannotConnectLong()
    {
        new Handler().postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                        {
                            Log.i(LOG_TAG, "Stopping connecing if not connected...");
                            // TODO: the check always returns yes, may be a bug
                            // of the phone.
                            if(bluetoothManager.getConnectionState(device, BluetoothProfile.GATT_SERVER) 
                                != BluetoothProfile.STATE_CONNECTED)
                            {
                                Log.i(LOG_TAG, "Cannot find device. Do not try connecting.");
                                bluetoothGatt.disconnect();
                                broadcastUpdate(ACTION_GATT_WRONG);
                                connectionState = STATE_DISCONNECTED;
                            }
                        }
                }
            , 3000);
    }
}

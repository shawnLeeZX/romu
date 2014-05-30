

package com.romu.app;

import java.util.HashMap;

/**
 * TODO: understand the class and change the comments.
 * This class includes a small subset of standard GATT attributes for
 * demonstration purposes.
 */
public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String BLE_SHIELD_TX = "713d0003-503e-4c75-ba94-3148f18d941e";
    public static String BLE_SHIELD_RX = "713d0002-503e-4c75-ba94-3148f18d941e";
    public static String BLE_SHIELD_SERVICE = "713d0000-503e-4c75-ba94-3148f18d941e";

    static {
        // Sample Services.
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");

        // RBL Services.
        attributes.put("713d0000-503e-4c75-ba94-3148f18d941e", "BLE Shield Service");
        // RBL Characteristics.
        attributes.put(BLE_SHIELD_TX, "BLE Shield TX");
        attributes.put(BLE_SHIELD_RX, "BLE Shield RX");

    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}

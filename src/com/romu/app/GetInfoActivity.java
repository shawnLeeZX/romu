package com.romu.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

/**
 * class GetInfoActivity
 * @author Shawn
 *
 * This class is responsible for acquiring relevant info from user. For the time
 * being, only MAC address of bluetooth is used.
 */
public class GetInfoActivity extends Activity
{
    // Logging.
    private static final String LOG_TAG = "Romu: GetInfoActivity";
 
    // UI.
    private EditText macAddrTextField;

    // User Info.
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.info);

        sharedPref = getSharedPreferences(
                getString(R.string.user_info_file_key),
                Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        // TODO: add input verification.
        macAddrTextField = (EditText) findViewById(R.id.mac_addr);
        String macAddress = sharedPref.getString(
                getString(R.string.mac_address),
                null
                );
        macAddrTextField.setText(macAddress);
    }


    /**
     * Callback for OK button.
     */
    public void onInfoConfirm(View view)
    {
        String macAddr = macAddrTextField.getText().toString();
        editor.putString(
                getString(R.string.mac_address),
                macAddr
                );
        editor.commit();
        Log.i(LOG_TAG, "MAC Address Saved: " + macAddr + ".");

        Intent intent = new Intent(this, RomuActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed()
    {
        finish();
    }
}

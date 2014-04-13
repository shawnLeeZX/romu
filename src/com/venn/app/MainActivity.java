package com.venn.app;

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
import android.widget.EditText;

public class MainActivity extends Activity
{
    private static final String LOG_TAG = "Venn";

    public final static String EXTRA_MESSAGE = "com.venn.app.MESSAGE";

    private static final int ENABLE_BLUETOOTH = 1;

    private BluetoothAdapter bluetooth = null;
    private FragmentManager fragmentManager = null;

    private GoogleMap map;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.map_main);

        fragmentManager = getFragmentManager();
        // Since bluetooth plays a central role of this app, it will ask the
        // user to enable bluetooth at startup.
        // TODO: change the switch asychronously.
        bluetooth = BluetoothAdapter.getDefaultAdapter();
        enableBluetooth();
        renderMap();
    }

    // Do a null check to confirm that we have initiated the map.
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
                // running.
            }
            else
                Log.d(LOG_TAG, "Successfully instantiate google map.");
        }
    }

    private void renderMap()
    {
        map = ((MapFragment) fragmentManager.findFragmentById(R.id.map)).getMap();























    }

    public void enableBluetooth()
    {
        // Enable bluetooth if not, otherwise do nothing.
        if(!bluetooth.isEnabled())
        {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, ENABLE_BLUETOOTH);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case ENABLE_BLUETOOTH:
                {
                    if(resultCode != RESULT_OK)
                    {
                        showBluetoothConfirmDialog();
                    }
                    break;
                }
            default:
                Log.e("venn", "Activity result out of range.");
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

    public void sendMessage(View view)
    {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    private void showBluetoothConfirmDialog()
    {
        // Create an dialog and pass it to ConfirmationDialogFragment to render.
        Dialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.confirmation)
            .setPositiveButton(R.string.confirm_dialog_ok,
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton)
                        {
                            enableBluetooth();
                        }
                    }
            )
            .setNegativeButton(R.string.confirm_dialog_cancel,
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton)
                        {
                            // Does nothing but quit the confirmation dialogue.
                        }
                    }
            )
            .create();

        ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
        fragment.setDialog(dialog);

        fragment.show(fragmentManager, "bluetooth_comfirmation");
    }

}

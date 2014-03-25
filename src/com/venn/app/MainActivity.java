package com.venn.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
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
    public final static String EXTRA_MESSAGE = "com.venn.app.MESSAGE";

    private static final int ENABLE_BLUETOOTH = 1;
    private static final int ASK_FOR_ENABLING_BLUETOOTH = 2;

    private BluetoothAdapter bluetooth = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Since bluetooth plays a central role of this app, it will ask the
        // user to enable bluetooth at startup.
        // TODO: change the switch asychronously.
        bluetooth = BluetoothAdapter.getDefaultAdapter();
        enableBluetooth();
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
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_search:
                // openSearch();
                return true;
            case R.id.action_settings:
                // openSettings();
                return true;
            default:
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

    public void onStatusChanged(View view)
    {
                        // Search and connect to the wearable.
                        //
                        // Send status change info.
    }

    private void showBluetoothConfirmDialog()
    {
        ConfirmationDialogFragment fragment =
            ConfirmationDialogFragment.newInstance(R.string.confirmation);

        fragment.show(getFragmentManager(), "bluetooth_comfirmation");
    }

}

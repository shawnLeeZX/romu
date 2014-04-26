package com.venn.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

/**
 * class LocationFetchActivity
 * @author Shawn
 */
public class LocationFetcherActivity extends Activity
{
    // Message exchange names.
    public final static String START_LOCATION_STRING   = "com.venn.app.START_LOCATION";
    public final static String DESTINATION_STRING      = "com.venn.app.DESTINATION";

    /**
     *  Constructor for LocationFetchActivity
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(MainActivity.LOG_TAG, "Entering Location fetcher.");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.location_fetch);
    }

    // This will fetch user input from text field and return it back for caller.
    public void onConfirm(View view)
    {
        String startLocation    = ((EditText) findViewById(R.id.start_location)).getText().toString();
        String destination      = ((EditText) findViewById(R.id.destination)).getText().toString();
        Intent intent           = getIntent();

        intent.putExtra(START_LOCATION_STRING, startLocation);
        intent.putExtra(DESTINATION_STRING, destination);

        setResult(RESULT_OK, intent);
        finish();
    }
}

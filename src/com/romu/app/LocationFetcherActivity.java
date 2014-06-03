package com.romu.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;

/**
 * class LocationFetchActivity
 * @author Shawn
 * This class will utilize Goolge Place AutoComplete API to fetch start location
 * and destination from user. Locations are saved as address, which will be used
 * in MainActivity to fetch route from Google Direction API service.
 */
public class LocationFetcherActivity extends Activity
{
    // Message exchange names.
    public final static String START_ADDR_STRING   = "com.romu.app.START_ADDR";
    public final static String DEST_ADDR_STRING      = "com.romu.app.DEST_ADDR";

    // Logging.
    private static final String LOG_TAG = "Romu: LocationFetcherActivity";

    // Class global reference to UI.
    private AutoCompleteTextView startAddrAutoCompleteTextView = null;
    private AutoCompleteTextView destAddrAutoCompleteTextView = null;

    protected void onCreate(Bundle savedInstanceState)
    {
        Log.i(LOG_TAG, "Entering Location fetcher.");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.address_fetcher);

        startAddrAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.start_addr);
        startAddrAutoCompleteTextView.setAdapter(
                new PlacesAutoCompleteAdapter(this, R.layout.list_item, R.id.item)
                );

        destAddrAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.dest_addr);
        destAddrAutoCompleteTextView.setAdapter(
                new PlacesAutoCompleteAdapter(this, R.layout.list_item, R.id.item)
                );
    }

    /**
     * Callback function of Button "OK" in this activity. This will fetch user
     * input from text field and return it back for caller.
     */
    public void onConfirm(View view)
    {
        String startAddr = startAddrAutoCompleteTextView.getText().toString();
        String destAddr = destAddrAutoCompleteTextView.getText().toString();

        // Replace space with %20.
        startAddr = startAddr.replace(" ", "%20");
        destAddr = destAddr.replace(" ", "%20");

        Intent intent = getIntent();
        intent.putExtra(START_ADDR_STRING, startAddr);
        intent.putExtra(DEST_ADDR_STRING, destAddr);

        setResult(RESULT_OK, intent);
        finish();
    }

}

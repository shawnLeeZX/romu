package com.venn.app;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;

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
    public final static String START_ADDR_STRING   = "com.venn.app.START_ADDR";
    public final static String DEST_ADDR_STRING      = "com.venn.app.DEST_ADDR";

    // Google Place API needed.
    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";

    // Google Server API.
    private static final String API_KEY = "AIzaSyAb66BJt0Ri3zNOfJMbPycC09Lv3p4isHw";

    // Logging.
    private static final String LOG_TAG = "Farwayer:LocationFetcherActivity";

    // Class global reference to UI.
    AutoCompleteTextView startAddrAutoCompleteTextView = null;
    AutoCompleteTextView destAddrAutoCompleteTextView = null;

    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(MainActivity.LOG_TAG, "Entering Location fetcher.");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.address_fetcher);

        startAddrAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.start_addr);
        startAddrAutoCompleteTextView.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.list_item));

        destAddrAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.dest_addr);
        destAddrAutoCompleteTextView.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.list_item));
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

    /**
     * This adaptor class is used by AutoCompleteTextView to fill up auto
     * complete list.
     */
    private class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable
    {
        private ArrayList<String> resultList;

        public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString());

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    }
                    else {
                        notifyDataSetInvalidated();
                    }
                }};
            return filter;
        }
    }

    /**
     * PlacesAutoCompleteAdapter calls this function to use Google Place
     * AutoComplete API to fetch potential places.
     *
     * @param input partial input string needed completion by Google Place
     *              AutoComplete Service.
     * @return      ArrayList<String> to be loaded by AutoCompleteTextView.
     */
    private ArrayList<String> autocomplete(String input)
    {
        ArrayList<String> resultList = null;

        HttpURLConnection conn = null;
        String jsonResults = null;
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            // TODO: enable location sensor when refining.
            sb.append("?sensor=false&key=" + API_KEY);
            // TODO: try automatically determine country when refining.
            sb.append("&components=country:cn");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();

            // Load the results into a String.
            jsonResults = Utilities.convertStreamToString(conn.getInputStream());

            Log.d(LOG_TAG, jsonResults);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Check whether Place Autocomplete API functions normally. If not,
            // return.
            JSONObject jsonObj = new JSONObject(jsonResults);
            String autocompleteResponceStatus = jsonObj.getString("status");
            if(!autocompleteResponceStatus.equals("OK"))
            {
                Log.d(LOG_TAG, "Place API malfunctioning. Responce status is: " 
                        + autocompleteResponceStatus
                        + "\n"
                        + "Error message: " + jsonObj.getString("error_message"));
                return resultList;
            }

            // Create a JSON object hierarchy from the results
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList<String>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }

        return resultList;
    }
}

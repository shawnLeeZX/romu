package com.romu.app;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

/**
 * Adaptor class is used by AutoCompleteTextView to fill up auto
 * complete list.
 */
public class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable
{
    // Logging.
    private static final String LOG_TAG = "Romu: PlacesAutoCompleteAdapter";

    // Google Place API needed.
    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";

    private ArrayList<String> resultList;

    public PlacesAutoCompleteAdapter(Context context, int resource, int textViewResourceId)
    {
        super(context, resource, textViewResourceId);
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
            sb.append("?sensor=false&key=" + Utilities.API_KEY);
            // TODO: try automatically determine country when refining.
            // sb.append("&components=country:cn");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();

            // Load the results into a String.
            jsonResults = Utilities.convertStreamToString(conn.getInputStream());
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
                Log.i(LOG_TAG, "Place API malfunctioning. Responce status is: " 
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


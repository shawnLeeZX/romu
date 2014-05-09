package com.wayfarer.app;

import java.io.IOException;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import android.util.Log;

/**
 * class GoogleDirectionParser
 * Class sends request to Google Direction API service, parse its result and
 * return all relevant information encapsuled in {@link Route}.
 *
 * @author Shawn
 */
public class GoogleDirectionParser
{
    private static final String LOG_TAG = "Wayfarer: GoogleDirectionParser";

    private URL requestUrl = null;
    private HttpURLConnection conn = null;

    /**
     *  Constructor for GoogleDirectionParser.
     *
     *  @param urlString                    The url string to be sent when
     *                                      making request.
     *  @exception MalformedURLException    Exception is thrown when malformed
     *                                      url is passed in. Error handle is
     *                                      delegated to caller.
     */
    public GoogleDirectionParser(String urlString) throws MalformedURLException
    {
        // Comment this when debugging.
        requestUrl = new URL(urlString);
        // Uncomment this when debugging, through which you do not need to input
        // origin and destination in LocationFetchActivity.
        // requestUrl = new URL("https://maps.googleapis.com/maps/api/directions/json?origin=Beijing%20Railway%20Station,%20Beijing%20Station%20East%20Street,%20Dongcheng,%20Beijing,%20China,%20100010&destination=Beijing,%20China&sensor=true&mode=walking&key=AIzaSyAb66BJt0Ri3zNOfJMbPycC09Lv3p4isHw");
    }

    /**
     * Make request to Google Direction API service using global variable
     * <code>requestUrl</code>.
     *
     * @exception   IOException when making Internet connection.
     * @return      Stream returned by the service.
     */
    private InputStream getInputStream() throws IOException
    {
        conn = (HttpURLConnection) requestUrl.openConnection();
        InputStream is = conn.getInputStream();

        return is;
    }

    public Route parse()
    {
        String result = null;
        try {
            final InputStream is = getInputStream();
            result = Utilities.convertStreamToString(is);
        } catch (IOException e) {
            // TODO: more error check here. Maybe popup prompt to let user check
            // internet connection.
            Log.e(LOG_TAG, "Error when making request to Google Direction API service.");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        Route route = new Route();

        try {
            // Tranform the string into a json object.
            final JSONObject json = new JSONObject(result);

            // Check response status. If not ok, just return.
            final String status = json.getString("status");
            if(!status.equals("OK"))
            {
                Log.d(LOG_TAG, "The response status of Google Direction API is not OK.");
                return null;
            }

            // Otherwise, continue.
            //
            // Get the route object.
            final JSONObject jsonRoute = json.getJSONArray("routes").getJSONObject(0);

            // Get the leg, only one leg as we don't support waypoints.
            final JSONObject leg = jsonRoute.getJSONArray("legs").getJSONObject(0);
            // Get the steps for this leg.
            final JSONArray steps = leg.getJSONArray("steps");
            // Number of steps for use in for loop.
            final int numSteps = steps.length();
            // Set the name of this route using the start & end addresses.
            route.setName(
                    leg.getString("start_address")
                    + " to "
                    + leg.getString("end_address")
                    );

            // Set bounds of the route.
            JSONObject routeBound = jsonRoute.getJSONObject("bounds");
            JSONObject northeastBound = routeBound.getJSONObject("northeast");
            LatLng northeast = new LatLng(
                    northeastBound.getDouble("lat"),
                    northeastBound.getDouble("lng")
                    );
            JSONObject southwestBound = routeBound.getJSONObject("southwest");
            LatLng southwest = new LatLng(
                    southwestBound.getDouble("lat"),
                    southwestBound.getDouble("lng")
                    );
            route.setBounds(new LatLngBounds(southwest, northeast));

            // Get info of origin and destination.
            route.setStartAddr(leg.getString("start_address"));
            JSONObject startLocation = leg.getJSONObject("start_location");
            route.setStartLocation(new LatLng(
                startLocation.getDouble("lat"),
                startLocation.getDouble("lng")
            ));
            route.setDestAddr(leg.getString("end_address"));
            JSONObject endLocation = leg.getJSONObject("end_location");
            route.setEndLocation(new LatLng(
                endLocation.getDouble("lat"),
                endLocation.getDouble("lng")
            ));

            // Get google's copyright notice (tos requirement).
            route.setCopyright(jsonRoute.getString("copyrights"));
            // Get the total length of the route.
            route.setLength(leg.getJSONObject("distance").getInt("value"));
            // Get any warnings provided (tos requirement).
            if (!jsonRoute.getJSONArray("warnings").isNull(0))
            {
                route.setWarning(jsonRoute.getJSONArray("warnings").getString(0));
            }

           /**
            * Loop through the steps, creating a segment for each one and
            * decoding any polylines found as we go to add to the route
            * object's map array. Using an explicit for loop because it is
            * faster!
            */
            for (int i = 0; i < numSteps; i++)
            {
                Segment segment = new Segment();

                // Get the individual step.
                final JSONObject step = steps.getJSONObject(i);

                // Get the start position for this step and set it on the
                // segment.
                final JSONObject start = step.getJSONObject("start_location");
                final LatLng position = new LatLng(
                        start.getDouble("lat"),
                        start.getDouble("lng")
                  );
                segment.setStart(position);

                // Set the length of this segment in metres.
                final int length = step.getJSONObject("distance").getInt("value");
                segment.setLength(length);

                // Set html instruction.
                segment.setInstruction(step.getString("html_instructions"));

                // Retrieve & decode this segment's polyline and add it to the
                // route.
                route.addPoints(
                    decodePolyLine(
                        step.getJSONObject("polyline").getString("points")
                        )
                    );
                // Add the segment to route.
                route.addSegment(segment);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error when parsing json response of Google Direction API.");
        }

        return route;
    }

   /**
    * Decode a polyline string into a list of LatLngs.
    * 
    * @param poly   polyline encoded string to decode.
    * @return       the list of LatLngs represented by this polystring.
    */
    private ArrayList<LatLng> decodePolyLine(final String poly)
    {
        int len = poly.length();
        int index = 0;
        ArrayList<LatLng> decoded = new ArrayList<LatLng>();
        int lat = 0;
        int lng = 0;

        while (index < len)
        {
            int b;
            int shift = 0;
            int result = 0;
            do
            {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do 
            {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            decoded.add(
                    new LatLng((double)lat / 1E5, (double)lng / 1E5)
                    );
        }

        return decoded;
    }
}

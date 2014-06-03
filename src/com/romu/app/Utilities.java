package com.romu.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;

import com.google.android.gms.maps.model.LatLng;

import android.util.Log;

/**
 * class Utilities
 * Class functions as Mixin class since Java does not support multiple
 * inheritance, which contains common utility methods and global shared
 * variables.
 *
 * @author Shawn
 */
public class Utilities
{
    private static final String LOG_TAG = "Romu: Utilities";

    // Google server API.
    public static final String API_KEY = "AIzaSyAb66BJt0Ri3zNOfJMbPycC09Lv3p4isHw";

    /**
      * Convert an inputstream to a string.
      * 
      * @param input    inputstream to convert.
      * @return         String of the inputstream.
      */
    public static String convertStreamToString(final InputStream input)
    {
        final BufferedReader reader =
            new BufferedReader(new InputStreamReader(input));
        final StringBuilder sBuilder = new StringBuilder();

        String line = null;

        try {
            while ((line = reader.readLine()) != null)
            {
                sBuilder.append(line);
            }
        }
        catch (IOException e)
        {
            Log.e(LOG_TAG, "Error when reading input stream.");
        }
        finally
        {
            try
            {
                input.close();
            }
            catch (IOException e)
            {
                Log.e(LOG_TAG, "Error when closing input stream.");
            }
        }

        return sBuilder.toString();
    }


   /**
    * Decode a polyline string into a list of LatLngs.
    * 
    * @param poly   polyline encoded string to decode.
    * @return       the list of LatLngs represented by this polystring.
    */
    public static ArrayList<LatLng> decodePolyLine(final String poly)
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

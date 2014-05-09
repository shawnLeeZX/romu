package com.wayfarer.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.util.Log;

/**
 * class Utilities
 * Class functions as Mixin class since Java does not support multiple
 * inheritance, which contains common utility methods.
 *
 * @author Shawn
 */
public class Utilities
{
    private static final String LOG_TAG = "Farwayer: Utilities";

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
}

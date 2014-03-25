package com.venn.app;

import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;

/**
 * class DisplayMessageActivity
 * @author Shawn
 */
public class DisplayMessageActivity extends Activity
{

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Add this will cause the program crash. I do not know why yet.
        // // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        // if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        // {
            // // Show the Up button in the action bar.
            // getActionBar().setDisplayHomeAsUpEnabled(true);
        // }

        // Get the message from the intent.
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        // TODO: remove this when finishing setting status hint part.
        // String messsage = "You have successfully set your hint!!!";

        // Get the text view.
        TextView textView = new TextView(this);
        textView.setTextSize(40);
        textView.setText(message);

        // Set the text view as the activity layout.
        setContentView(textView);
    }

    @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
            case android.R.id.home:
                // NavUtils.navigateUpFromSameTask(this);
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
}

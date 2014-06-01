package com.romu.app;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * The init phase of top navigation bar.
 */
public class TopNavBarFragment extends Fragment
{
    private TopNavBarAttachedListener listener = null;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
            )
    {
        return inflater.inflate(R.layout.top_nav_bar, container, false);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        listener.onTopNavBarAttached();
    }

    public interface TopNavBarAttachedListener
    {
        public void onTopNavBarAttached();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);


        try {
            listener = (TopNavBarAttachedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement TopNavBarAttachedListener");
        }

    }
}

package com.romu.app;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * The init phase of top navigation bar.
 */
public class InitTopNavBarFragment extends Fragment
{
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
            )
    {
        return inflater.inflate(R.layout.init_top_nav_bar, container, false);
    }
}

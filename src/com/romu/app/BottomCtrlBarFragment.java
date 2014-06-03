package com.romu.app;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Bottom control bar when starting navigating.
 */
public class BottomCtrlBarFragment extends Fragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.bottom_ctrl_fragment, container, false);
    }

    private BottomCtrlBarAttachedListener listener = null;

    @Override
    public void onStart()
    {
        super.onStart();

        listener.onBottomCtrlBarAttached();
    }

    public interface BottomCtrlBarAttachedListener
    {
        public void onBottomCtrlBarAttached();
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);


        try {
            listener = (BottomCtrlBarAttachedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement BottomCtrlBarAttachedListener");
        }

    }
}

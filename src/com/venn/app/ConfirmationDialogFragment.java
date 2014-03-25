package com.venn.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * class ConfirmationDialogue
 * @author Shawn
 */
public class ConfirmationDialogFragment extends DialogFragment
{
    private Dialog dialog = null;

    // The following code is left as reference to show how to use bundle to pass
    // parameters. They are not useful.
    //
    // public static ConfirmationDialogFragment newInstance(int title)
    // {
        // ConfirmationDialogFragment confirmDialogFragment = new ConfirmationDialogFragment();

        // Bundle args = new Bundle();
        // args.putInt("title", title);
        // confirmDialogFragment.setArguments(args);
        // return confirmDialogFragment;
    // }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        return dialog;
    }

    public void setDialog(Dialog dialog)
    {
        this.dialog = dialog;
    }
}

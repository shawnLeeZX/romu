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

    public static ConfirmationDialogFragment newInstance(int title)
    {
        ConfirmationDialogFragment confirmDialogFragment = new ConfirmationDialogFragment();

        Bundle args = new Bundle();
        args.putInt("title", title);
        confirmDialogFragment.setArguments(args);
        return confirmDialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        int title = getArguments().getInt("title");

        return new AlertDialog.Builder(getActivity())
            .setTitle(title)
            .setPositiveButton(R.string.confirm_dialog_ok,
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton)
                        {
                            ((MainActivity)getActivity()).enableBluetooth();
                        }
                    }
            )
            .setNegativeButton(R.string.confirm_dialog_cancel,
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton)
                        {
                            ((MainActivity)getActivity()).finish();
                        }
                    }
            )
            .create();
    }
}

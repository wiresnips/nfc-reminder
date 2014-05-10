package com.novorobo.tracker.app;

import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.content.SharedPreferences;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;


public class HelpWidgetCreationDialog extends DialogFragment {
	public static final String SHOW_WIDGET_GUIDE_DIALOG = "ShowWidgetGuideDialog";

    public Dialog onCreateDialog (Bundle savedInstanceState) {
    	View view = LayoutInflater.from(getActivity()).inflate(R.layout.help_widget_creation, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle( R.string.help_widget_creation );
        builder.setView( view );
        builder.setPositiveButton( android.R.string.ok, null );

        builder.setNegativeButton( R.string.help_widget_dialog_noshow, new OnClickListener() {
            public void onClick (DialogInterface dialog, int whichButton) {
                SharedPreferences.Editor editor = getActivity().getSharedPreferences(MetricList.SHARED_PREFS, 0).edit();
                editor.putBoolean(SHOW_WIDGET_GUIDE_DIALOG, false);
                editor.commit();
            }
        });

        return builder.create();
    }
}
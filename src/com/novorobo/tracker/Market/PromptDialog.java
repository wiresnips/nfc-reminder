package com.novorobo.tracker.app;

import com.novorobo.util.database.Database;
import com.novorobo.tracker.metric.*;
import com.novorobo.tracker.datapoint.*;
import com.novorobo.constants.*;
import com.novorobo.market.*;

import com.novorobo.util.ugh.DialogFragmentWithDismissCallback;

import android.util.Log;
import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.DialogFragment;

import android.app.PendingIntent;
import android.content.IntentSender.SendIntentException;

import android.content.SharedPreferences;

import android.view.View;
import android.widget.TextView;
import android.view.LayoutInflater;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;


public class PromptDialog extends DialogFragmentWithDismissCallback {
    public static final String CATEGORY = "PromptDialog";

    public static final int RATE_REVIEW_APP_REQUEST_CODE = 658;
    public static final int PURCHASE_PREMIUM_REQUEST_CODE = 852;

    private static final String UPGRADE_INTENT = "upgradeIntent";
    private PendingIntent upgradeIntent = null;

    // trip this to dismiss the dialog before it's shown
    private boolean abort = false;


    public PromptDialog () {
        super();
    }

    public PromptDialog (PendingIntent upgradeIntent) {
        super();
        this.upgradeIntent = upgradeIntent;
    }


    public void onSaveInstanceState (Bundle state) {
        super.onSaveInstanceState(state);
        state.putParcelable(UPGRADE_INTENT, upgradeIntent);
    }



    public void onStart () {
        super.onStart();

        if (abort || upgradeIntent == null)
            dismiss();
    }


    public Dialog onCreateDialog (Bundle state) {
        final Activity activity = getActivity();

        SharedPreferences records = activity.getSharedPreferences(MetricList.SHARED_PREFS, 0);
        boolean alreadyRatedPerhaps = records.getBoolean(AppRater.VISITED_MARKET_TO_RATE, false);

        //*
        int promptIndex = records.getInt(AppRater.PROMPTS_ISSUED, 0);
        String[] allPrompts = activity.getResources().getStringArray(R.array.upsell_prompts);

        reportAnalytics(ACTION.PROMPT, "prompt_count", promptIndex);

        promptIndex = promptIndex % allPrompts.length;

        String[] promptContent = allPrompts[promptIndex].split("\\|");

        String title = promptContent[0].trim();
        String message = promptContent[1].trim();

        // prep the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle( title );
        builder.setMessage( message );

        builder.setNegativeButton( R.string.no_thanks, new DialogInterface.OnClickListener() {
                public void onClick (DialogInterface dialog, int whichButton) {
                    reportAnalytics(ACTION.DISMISS, null, 1);
                }
            });

        if (!alreadyRatedPerhaps) {
            builder.setNeutralButton(R.string.rate_app, new DialogInterface.OnClickListener() {
                public void onClick (DialogInterface dialog, int whichButton) {
                    reportAnalytics(ACTION.RATE, null, 1);
                    AppRater.launchMarketIntent(activity);
                }
            });
        }

        if (upgradeIntent == null && state != null)
            upgradeIntent = (PendingIntent) state.getParcelable(UPGRADE_INTENT);

        builder.setPositiveButton(R.string.upgrade_app,
            new DialogInterface.OnClickListener() {
                public void onClick (DialogInterface dialog, int whichButton) {
                    if (upgradeIntent != null) {
                        try {
                            activity.startIntentSenderForResult( upgradeIntent.getIntentSender(), 
                                PURCHASE_PREMIUM_REQUEST_CODE, new Intent(), 
                                Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0) );

                            reportAnalytics(ACTION.UPGRADE, null, 1);
                        } 
                        catch (SendIntentException e) {
                            abort = true;
                        }
                    }
                }
            }
        );

        return builder.create();
    }




    private void reportAnalytics (String action, String label, long value) {
        EasyTracker easyTracker = EasyTracker.getInstance(getActivity());
        if (easyTracker != null)
            easyTracker.send(MapBuilder.createEvent(PromptDialog.CATEGORY, action, label, value).build());
    }


}

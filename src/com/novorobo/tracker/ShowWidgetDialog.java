
package com.novorobo.tracker.app;

import com.novorobo.tracker.metric.Metric;
import com.novorobo.tracker.datapoint.Datapoint;
import com.novorobo.util.database.Database;

import com.novorobo.market.MarketActivity;
import com.novorobo.market.MarketInterface.InitListener;

import java.util.List;

import android.util.Log;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;

import android.app.PendingIntent;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;

import com.novorobo.util.ugh.DialogFragmentWithDismissCallback.DismissCallback;


public class ShowWidgetDialog extends MarketActivity implements DismissCallback {

    // dialog tags
    private static String DATA_ENTRY_DIALOG = "DataEntryDialog";
    private static String PROMPT_DIALOG = "PromptDialog";

    private DataEntryDialog dialog = null;

    // this prevents PromptDialog firing if the DataEntryDialog was dismissed prematurely
    private boolean activityTornDown = false;


    public ShowWidgetDialog() {
        super();
    }


    public void onCreate (Bundle state) {
        Database.init(this);
        super.onCreate(state);

        addMarketInitListener( new InitListener () {
            public void onInit () {
                if (dialog != null && !isUserPremium())
                    dialog.setShowBanner(true);
            }
        });

        // if we've already got everything set up and stashed, we're good to go
        if (state != null) {
            dialog = (DataEntryDialog) getFragmentManager().findFragmentByTag(DATA_ENTRY_DIALOG);
            return;
        }


        // have we opted out of analytics?
        SharedPreferences records = getSharedPreferences(MetricList.SHARED_PREFS, 0);
        boolean useAnalytics = records.getBoolean(About.USE_ANALYTICS, true);
        GoogleAnalytics.getInstance(this).setAppOptOut( !useAnalytics );


        Intent intent = getIntent();
        long metricID = intent.getLongExtra(Metric.ID, 0);
        Metric metric =  (metricID == 0) ? null : Database.get(Metric.class, metricID);

        // if we've got no metric, we can't do anything
        if (metric == null) {
            // phone home a refresh. Something's fishy- the metric might no longer exist.
            int widgetID = intent.getIntExtra(GraphWidgetProvider.WIDGET_ID, 0);
            if (widgetID != 0)
                GraphWidgetProvider.updateWidget(this, widgetID);

            finish();
            return;
        }

        Datapoint datapoint = new Datapoint(metric);
        dialog = datapoint.getDialog();
        dialog.show( getFragmentManager(), DATA_ENTRY_DIALOG );
    }



    // if the activity is taken down (ie, for orientation change), I shouldn't fire a prompt
    public void onPause () {
        super.onPause();
        activityTornDown = true;
    }

    public void onResume () {
        super.onResume();
        activityTornDown = false;
    }

    public void onStart () {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }





    public void onDialogDismissed (String dialogTag) {

        // we're only going to do anything if we come by way of the DataEntryDialog
        if (dialogTag == null || !dialogTag.equals(DATA_ENTRY_DIALOG)) {
            finish();
            return;
        }
        
        // all sorts of good reasons not to show a prompt
        PendingIntent upgradeIntent = getUpgradeIntent();
        boolean skipPrompt = activityTornDown || isUserPremium() || upgradeIntent == null || !AppRater.canShowPrompt(this);

        if (skipPrompt) {
            finish();
            return;
        }

        // if none of them stuck, show the prompt!
        AppRater.recordPrompt(this);
        PromptDialog dialog = new PromptDialog(upgradeIntent);
        dialog.show(getFragmentManager(), PROMPT_DIALOG);
    }


    // okay, I should be receiving answers from purchase attempts here
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // candidate requestCodes
        // AppRater.RATE_REVIEW_APP_REQUEST_CODE
        // PurchaseList.PURCHASE_PREMIUM_REQUEST_CODE

        // uhh.. do I actually care what happens here?
        // kinda think I don't...
        // guess we'll just clean up and be on our way, then

        finish();
    }


}




package com.novorobo.tracker.app;

import com.novorobo.tracker.metric.Metric;
import com.novorobo.util.database.Database;

import android.provider.BaseColumns;

import android.util.Log;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;


import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.view.Window;

import java.util.List;

import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.

/**
 * The configuration screen for the ExampleAppWidgetProvider widget sample.
 */
public class SetWidgetMetric extends Activity {

    private int widgetID;

    public SetWidgetMetric() {
        super();
    }


    public void onCreate (Bundle state) {
        Database.init(this);
        super.onCreate(state);

        // default to failure if we exit without declaring success
        Intent failure = new Intent();
        setResult(RESULT_CANCELED, failure);

        // get the widget ID, or die trying!
        widgetID = AppWidgetManager.INVALID_APPWIDGET_ID;

        Bundle intentData =  getIntent().getExtras();
        if (intentData != null)
            widgetID = intentData.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        
        if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // once we get a widgetID, we can have a marginally smarter failure
        failure.putExtra( AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID );
        setResult(RESULT_CANCELED, failure);


        // get a list of Metrics, and their names
        final List<Metric> metrics = Database.get(Metric.class, null, null, BaseColumns._ID + " desc");
        CharSequence[] names = new CharSequence[metrics.size()];

        for (int i = 0; i < names.length; i++)
            names[i] = metrics.get(i).name;

        // erect the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.widget_metric_link_title);

        // present a list of existing widgets to choose from
        if (names.length == 0)
             builder.setMessage(R.string.widget_no_existing_metrics);

        else {
            builder.setSingleChoiceItems( names, -1, new OnClickListener() {
                public void onClick (DialogInterface dialog, int item) {
                    dialog.dismiss();
                    SetWidgetMetric.this.setWidgetMetric( widgetID, metrics.get(item).getID() );
                }
            });
        }

        // and a button allowing a new widget to be created
        builder.setPositiveButton(R.string.widget_create_metric,
            new DialogInterface.OnClickListener() {
                public void onClick (DialogInterface dialog, int whichButton) {
                    Intent edit = new Intent(SetWidgetMetric.this, MetricEdit.class);
                    SetWidgetMetric.this.startActivityForResult( edit, MetricEdit.REQUEST_CODE_EDIT );
                    dialog.dismiss();
                }
            }
        );

        // also allow an unceremonious exit, I guess
        builder.setNegativeButton(android.R.string.cancel,
            new DialogInterface.OnClickListener() {
                public void onClick (DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                    SetWidgetMetric.this.finish();
                }
            }
        );

        builder.show();
    }


    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MetricEdit.REQUEST_CODE_EDIT && resultCode == RESULT_OK) {
            long metricID = data.getLongExtra(Metric.ID, 0);

            if (metricID != 0)
                setWidgetMetric( widgetID, metricID );
        }

        finish();
    }


    private void setWidgetMetric (int widgetID, long metricID) {
        
        // create the metric linkage, stash it in the preferences
        SharedPreferences widgetMap = getSharedPreferences(GraphWidgetProvider.WIDGET_METRIC_MAP, 0);
        SharedPreferences.Editor editor = widgetMap.edit();
        editor.putLong( "" + widgetID, metricID );
        editor.apply();

        // update the widget
        GraphWidgetProvider.updateWidget( this, widgetID );

        // return from configuring
        Intent result = new Intent();
        result.putExtra( AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID );
        setResult(RESULT_OK, result);
        finish();
    }


    public void onStart () {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }


}



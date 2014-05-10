package com.novorobo.tracker.app;

import com.novorobo.util.database.Database;
import com.novorobo.tracker.metric.*;
import com.novorobo.tracker.datapoint.*;
import com.novorobo.tracker.graph.GraphStyle;
import com.novorobo.constants.*;

import android.app.Activity;

import android.util.Log;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;

import android.appwidget.AppWidgetManager;

import android.app.Dialog;
import android.app.AlertDialog;
import android.app.DialogFragment;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

public class MetricDeletionDialog extends DialogFragment {
    private Metric metric;

    public interface OnDeleteListener { 
        public void onMetricDeleted (Metric metric);
    }
    private OnDeleteListener onDeleteListener = null;


    public MetricDeletionDialog () {
        super();
        metric = new Metric();
    }

    public MetricDeletionDialog (Metric metric) {
        super();
        this.metric = metric;
    }

    public void onSaveInstanceState (Bundle state) {
        super.onSaveInstanceState(state);
        state.putBundle(Metric.BUNDLE, metric.bundle());
    }

    public Dialog onCreateDialog (Bundle state) {

        if (state != null)
            metric.setValues(state.getBundle(Metric.BUNDLE));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIconAttribute( R.attr.icon_warning );

        if (metric.name == null || metric.name.isEmpty())
            builder.setTitle( R.string.confirm_delete );
        else
            builder.setTitle( getString(R.string.confirm_delete) + ": " + metric.name );
        
        builder.setMessage( R.string.confirm_delete_message );
        builder.setNegativeButton( android.R.string.cancel, cancel );
        builder.setPositiveButton( R.string.delete, delete );

        return builder.create();
    }


    private OnClickListener cancel = new OnClickListener () {
        public void onClick (DialogInterface dialog, int id) { 
            dialog.dismiss(); 
        }
    };

    private OnClickListener delete = new OnClickListener () {
        public void onClick (DialogInterface dialog, int id) { 
            long metricID = metric.getID();

            // scrub the database
            Database.delete( metric );
            Database.delete( GraphStyle.class, metric.graphStyle_id );
            Database.delete( Datapoint.class, "metric_id = ?", new String[] {"" + metric.getID()} );

            // update the widgets
            GraphWidgetProvider.updateMetric( getActivity(), metricID );

            // alert the troops
            if (onDeleteListener != null)
                onDeleteListener.onMetricDeleted( metric );

            dialog.dismiss();
        }
    };




    public void onAttach (Activity activity) {
        super.onAttach(activity);
        
        // if our owner implements our interface, GREAT
        try { onDeleteListener = (OnDeleteListener) activity; } 

        // if not, that's also fine
        catch (ClassCastException e) {}
    }



    private void reportAnalytics () {
        EasyTracker easyTracker = EasyTracker.getInstance(getActivity());

        if (easyTracker == null)
            return;

        easyTracker.send(MapBuilder.createEvent(
            Metric.CATEGORY, 
            ACTION.DELETE, 
            metric.datatype.name(), 
            null).build());
    }

}
package com.novorobo.tracker.app;

import com.novorobo.constants.*;
import com.novorobo.util.database.Database;
import com.novorobo.tracker.metric.Metric;
import com.novorobo.tracker.app.DatapointList;
import com.novorobo.tracker.datapoint.Datapoint;

import java.util.Calendar;

import android.database.Cursor;
import android.widget.CursorAdapter;

import android.content.Context;
import android.content.Intent;

import android.app.Activity;
import android.app.AlertDialog;

import android.view.View;
import android.view.ViewGroup;

import android.util.Log;
import android.widget.Toast;
import android.widget.TextView;




public class DatapointAdapter extends CursorAdapter {

    // column order from DatapointList.onCreate
    private static final int ID = 0;
    private static final int TIMESTAMP = 1;
    private static final int VALUE = 2;

    private final Activity activity;
    private final Metric.DataType datatype;

    public DatapointAdapter (DatapointList activity, Cursor cursor, Metric.DataType datatype) {
        super(activity, cursor, 0);
        this.activity = activity;
        this.datatype = datatype;
    }

    public View newView (Context context, Cursor cursor, ViewGroup parent) {
        switch (datatype) {
            case EVENT:     return new DataListingEvent(context);
            case AMOUNT:    return new DataListingAmount(context);
            case RANGE:     return new DataListingRange(context);
            case DURATION:  return new DataListingDuration(context);
            default:        return null;
        }
    }

    // pull data out of the cursor, build a datapoint, and dump it in here
    public void bindView (View view, Context context, Cursor cursor) {
        DataListingEvent listing = (DataListingEvent) view;

        boolean showDate = false;
        long timestamp = cursor.getLong(TIMESTAMP);
        double value = cursor.getDouble(VALUE);

        if (cursor.isFirst())
            showDate = true;
        else if (cursor.moveToPrevious()) {
            long prevTimestamp = cursor.getLong(TIMESTAMP);
            showDate = !sameDay(timestamp, prevTimestamp);
        }

        listing.setShowDate( showDate );
        listing.setTimestamp( timestamp );
        listing.setValue( value );
    }


    private boolean sameDay (long timestampA, long timestampB) {
        calA.setTimeInMillis( timestampA * MILLI_PER.SECOND );
        calB.setTimeInMillis( timestampB * MILLI_PER.SECOND );

        return (calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR)) &&
               (calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR));
    }

    // no need to respawn these every time
    private Calendar calA = Calendar.getInstance();
    private Calendar calB = Calendar.getInstance();

}
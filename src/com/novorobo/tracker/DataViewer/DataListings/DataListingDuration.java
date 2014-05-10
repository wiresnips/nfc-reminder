
package com.novorobo.tracker.app;

import com.novorobo.constants.*;
import java.util.Date;
import java.text.DateFormat;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;

import android.view.View;
import android.view.LayoutInflater;

import android.widget.TextView;
import android.widget.LinearLayout;

public class DataListingDuration extends DataListingEvent {
    private static final int RANGE_RESOLUTION = 1000;
    private Resources res;

    protected TextView hoursUp;
    protected TextView minutesDown;

    public DataListingDuration (Context context, AttributeSet attrs) {
    	super(context, attrs);
        res = context.getResources();
        hoursUp = (TextView) findViewById(R.id.hours_up);
        minutesDown = (TextView) findViewById(R.id.minutes_down);        
    }

    public DataListingDuration (Context context) {
        this(context, null);
    }

    protected int getLayoutRes () {
    	return R.layout.datapoint_listing_duration;
    }

    public void setValue (double value) {
        int days  = (int) Math.floor(value / MILLI_PER.DAY);
        int hours = (int) Math.floor(value / MILLI_PER.HOUR) % 24;
        int mins  = (int) Math.floor(value / MILLI_PER.MINUTE) % 60;
        int secs  = (int) Math.floor(value / MILLI_PER.SECOND) % 60 ;
        int csecs = (int) Math.floor(value / 10L) % 10;

        int format = (days > 1)  ? R.string.format_duration_days :
                     (days > 0)  ? R.string.format_duration_day_singular :
                     (csecs > 0) ? R.string.format_duration_csecs :
                                   R.string.format_duration;

        String timeString = res.getString(format, days, hours, mins, secs, csecs);
        int split = timeString.indexOf(':');

        hoursUp.setText( timeString.substring(0, split) );
        minutesDown.setText( timeString.substring(split) );
    }
}
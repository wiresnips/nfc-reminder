
package com.novorobo.tracker.app;

import com.novorobo.constants.*;
import java.util.Date;
import java.text.DateFormat;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import android.view.View;
import android.view.LayoutInflater;

import android.widget.TextView;
import android.widget.LinearLayout;

public class DataListingEvent extends LinearLayout {

	protected View divider;
	protected TextView date;
	protected TextView timeHours;
	protected TextView timeOther;

    private DateFormat timeFormat;
    private DateFormat dateFormat;


    public DataListingEvent (Context context, AttributeSet attrs) {
    	super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(getLayoutRes(), this, true);

        timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        dateFormat = android.text.format.DateFormat.getDateFormat(context);
        
        divider = findViewById(R.id.divider);
        date = (TextView) findViewById(R.id.date);
  
        timeHours = (TextView) findViewById(R.id.time_hours);
        timeOther = (TextView) findViewById(R.id.time_other);
    }

    public DataListingEvent (Context context) {
		this(context, null);
	}

    protected int getLayoutRes () {
    	return R.layout.datapoint_listing_event;
    }

    public void setTimestamp (long timestamp) {
        Date datetime = new Date( timestamp * MILLI_PER.SECOND );
        
        String time = timeFormat.format(datetime);
        int split = time.indexOf(':');
        timeHours.setText( time.substring(0, split) );
        timeOther.setText( time.substring(split) );
        
  	    date.setText( dateFormat.format(datetime) );
    }


    public void setShowDate (boolean show) {
        if (show) {
            divider.setVisibility( View.VISIBLE );
            date.setVisibility( View.VISIBLE );
        } else {
            divider.setVisibility( View.GONE );
            date.setVisibility( View.INVISIBLE );             
        }
    }

    public void setValue (double value) {
    	// not relevant to DataListingEvent
    }
}
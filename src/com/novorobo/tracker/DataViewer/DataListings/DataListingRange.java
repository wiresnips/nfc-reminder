
package com.novorobo.tracker.app;

import java.util.Date;
import java.text.DateFormat;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import android.view.View;
import android.view.LayoutInflater;

import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.LinearLayout;

public class DataListingRange extends DataListingEvent {
    private static final int RANGE_RESOLUTION = 1000;

	protected ProgressBar range;

    public DataListingRange (Context context, AttributeSet attrs) {
    	super(context, attrs);
        range = (ProgressBar) findViewById(R.id.range);
        range.setIndeterminate(false);
        range.setMax(RANGE_RESOLUTION);
    }

    public DataListingRange (Context context) {
        this(context, null);
    }

    protected int getLayoutRes () {
    	return R.layout.datapoint_listing_range;
    }

    public void setValue (double value) {
        range.setProgress( (int) Math.round(value * RANGE_RESOLUTION) );
    }
}
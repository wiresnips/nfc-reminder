
package com.novorobo.tracker.app;

import java.util.Date;
import java.text.DateFormat;

import android.content.Context;
import android.util.AttributeSet;

import android.view.View;
import android.view.LayoutInflater;

import android.widget.TextView;
import android.widget.LinearLayout;

public class DataListingAmount extends DataListingEvent {

	protected TextView integer;
	protected TextView decimal;

    public DataListingAmount (Context context, AttributeSet attrs) {
    	super(context, attrs);
        integer = (TextView) findViewById(R.id.integer);
        decimal = (TextView) findViewById(R.id.decimal);
    }

    public DataListingAmount (Context context) {
        this(context, null);
    }

    protected int getLayoutRes () {
    	return R.layout.datapoint_listing_amount;
    }

    public void setValue (double value) {
        long roundValue = Math.round(value);

        if (value == roundValue) {
            integer.setText( roundValue + "" );
            decimal.setVisibility( View.INVISIBLE );
        }

        else {
            String valueStr = value + "";
            int dot = valueStr.indexOf('.');

            integer.setText( valueStr.substring(0, dot) );
            decimal.setText( valueStr.substring(dot) );
            decimal.setVisibility( View.VISIBLE );
        }
    }
}
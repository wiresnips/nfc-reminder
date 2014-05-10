package com.novorobo.tracker.datapoint;

import com.novorobo.util.database.Database;
import com.novorobo.tracker.metric.*;
import com.novorobo.tracker.app.*;
import com.novorobo.constants.*;

import android.database.Cursor;
import android.provider.BaseColumns;
import android.content.ContentValues;

import java.util.Date;
import java.util.Calendar;
import java.lang.Exception;

import android.util.Log;
import android.os.Bundle;

public class Datapoint implements Database.Entry {
	public static String CATEGORY = "Datapoint";

	public long metric_id;
	public Metric.DataType type;
	public long timestamp;
	public double value;

	protected Metric metric;

	public Datapoint () {
		timestamp = System.currentTimeMillis() / MILLI_PER.SECOND;
	}

	public Datapoint (Metric owner) {
		this();
		metric = owner;
		metric_id = metric.getID();
		type = metric.datatype;
	}


	public Metric getMetric () {
		if (metric_id == 0)
			return null;

		if (metric == null)
			metric = Database.get(Metric.class, metric_id);
		
		return metric;
	}

	public Date getDate () { 
		return new Date( timestamp * MILLI_PER.SECOND ); 
	}
	
	public Calendar getCalendar () {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis( timestamp * MILLI_PER.SECOND );
		return calendar;
	}




	// okay, so- I know that the following is INCREDIBLY BROKEN.
	// Any time you have a field called 'type', which is used to drive a switch,
	// you know beyond a shadow of a doubt that you should be subclassing.

	// Here's why I'm confused about this:
	//
	// 1) I'm fairly certain that neither of the following functions should be
	//		part of Datapoint at all. They're View, not Model. I don't know what
	//		I should be doing with them, though, so they're living here for now.
	//
	// 2) My automagic Database utility doesn't play nicely with subclasses.
	//		This is actually a pretty major flaw, as far as Java's concerned.
	//
	// 3) At least when they're here, I KNOW there's a problem, instead of 
	//		hiding little chunks across unrelated classes.


	public DataEntryDialog getDialog () {
		switch (type) {
            case EVENT: 	return new DialogEnterEvent(this);
            case RANGE: 	return new DialogEnterRange(this);
            case DURATION: 	return new DialogEnterDuration(this);
            case AMOUNT:	return new DialogEnterAmount(this);
        }	
        return null;
	}
	




	// DATABASE INTERFACE STUFF
    protected long _id;

    public long getID () {
        return _id;
    }

    public void setID (long id) {
        _id = id;
    }

	public static final String[] COLUMN_NAMES = new String[] {
		BaseColumns._ID, "metric_id", "type", "timestamp", "value"
	};
	public static final String[] COLUMN_TYPES = new String[] {
		Database.TableSchema.ID_TYPE_DECLARATION, "integer", "text", "integer", "real"
	};

	public ContentValues getValues () {
	    ContentValues values = new ContentValues();
		values.put( BaseColumns._ID, _id );
	    values.put( "metric_id", metric_id );
	    values.put( "type", type.name() );
	    values.put( "timestamp", timestamp );		
		values.put( "value", value );
	    return values;
	}

    public void setValues (Cursor values) {
    	_id = values.getLong(0);
    	metric_id = values.getLong(1);

    	try { type = Enum.valueOf(Metric.DataType.class, values.getString(2)); }
    	catch (IllegalArgumentException e) { type = Metric.DataType.AMOUNT; }

    	timestamp = values.getLong(3);
    	value = values.getDouble(4);
    }

    public Bundle bundle () {
        Bundle values = new Bundle();
		values.putLong( BaseColumns._ID, _id );
	    values.putLong( "metric_id", metric_id );
	    values.putString( "type", type.name() );
	    values.putLong( "timestamp", timestamp );		
		values.putDouble( "value", value );
        return values;
    }

	public void setValues (Bundle values) {
    	_id = values.getLong(BaseColumns._ID);
    	metric_id = values.getLong("metric_id");

    	try { type = Enum.valueOf(Metric.DataType.class, values.getString("type")); }
    	catch (IllegalArgumentException e) { type = Metric.DataType.AMOUNT; }

    	timestamp = values.getLong("timestamp");
    	value = values.getDouble("value");
    }





}
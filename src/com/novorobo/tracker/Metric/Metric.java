package com.novorobo.tracker.metric;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import com.novorobo.constants.*;
import com.novorobo.tracker.datapoint.*;
import com.novorobo.tracker.graph.GraphStyle;
import com.novorobo.util.database.Database;

import android.database.Cursor;
import android.provider.BaseColumns;
import android.content.ContentValues;
import android.os.Bundle;

import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.util.Log;

public class Metric implements Database.Entry {
    public static final String ID = "METRIC_ID";
    public static final String BUNDLE = "metricBundle";
    public static String CATEGORY = "Metric";

    // the various modes of the graphulation
    public enum DataType { EVENT, AMOUNT, DURATION, RANGE }
    public enum AggregatePeriod { AUTO, NONE, DAY, WEEK, MONTH, YEAR }
    public enum AggregateFunction { TOTAL, AVERAGE, MINIMUM, MAXIMUM, CUMULATIVE }

    public String name;
    public boolean hasName () {
        return name != null && !name.isEmpty();
    }

    // modes... ugh
    public DataType datatype = DataType.EVENT;
    private AggregatePeriod aggregatePeriod = AggregatePeriod.AUTO;
    private AggregateFunction aggregateFunction = AggregateFunction.TOTAL;


    public AggregatePeriod getAggregatePeriod () {
        if (aggregatePeriod == AggregatePeriod.AUTO)
            return mapToSane(getTimespanAggregatePeriod());
        return mapToSane(aggregatePeriod);
    }

    public AggregatePeriod getAggregatePeriodNoAuto () {
        return mapToSane(aggregatePeriod);
    }

    public void setAggregatePeriod (AggregatePeriod period) {
        if (getAggregatePeriod() != mapToSane(period))
            aggregatePeriod = period;
    }

    public AggregateFunction getAggregateFunction () {
        return mapToSane(aggregateFunction);
    }

    public void setAggregateFunction (AggregateFunction func) {
        if (mapToSane(aggregateFunction) != mapToSane(func))
            aggregateFunction = func;
    }


    // not all combinations of DataType/AggregatePeriod/AggregateFunction make sense
    // this constrains the options

    private AggregatePeriod mapToSane (AggregatePeriod period) {
        if (datatype == DataType.EVENT && period == AggregatePeriod.NONE)
            return AggregatePeriod.DAY;
        return period;
    }
    
    private AggregateFunction mapToSane (AggregateFunction func) {
        if (datatype == DataType.EVENT || getAggregatePeriod() == AggregatePeriod.NONE) {
            if (func != AggregateFunction.CUMULATIVE)
                return AggregateFunction.TOTAL;
        }
        return func;
    }



    private AggregatePeriod getTimespanAggregatePeriod () {
        long span = getLatestTimestamp() - getEarliestTimestamp();

        // we want to show the largest timerange that we can display three of
        if (span < SECONDS_PER.DAY * 3)       return AggregatePeriod.NONE;
        if (span < SECONDS_PER.DAY * 7 * 3)   return AggregatePeriod.DAY;
        if (span < SECONDS_PER.DAY * 30 * 3)  return AggregatePeriod.WEEK;
        if (span < SECONDS_PER.DAY * 365 * 3) return AggregatePeriod.MONTH;

        return AggregatePeriod.YEAR;
    }



    // THIS IS WHY MODES ARE BAD. BECAUSE YOU END UP WITH 
    // STUFF THAT'S ONLY RELEVANT TO SOME OF THEM, BUT NOT OTHERS

    // these are only relevant if we're measuring a Range
    public int rangeMax = 100;

    // and here's some that are only relevant to Duration
    public enum DurationPickerExt { NONE, CENTISECONDS, DAYS }
    public DurationPickerExt durationPickerExtension = DurationPickerExt.NONE;


    public long graphStyle_id;
    
    public GraphStyle getGraphStyle () {
        if (graphStyle_id == 0)
            return null;
        return Database.get(GraphStyle.class, graphStyle_id);
    }



    public double[] getGraphValues (int count) {
        return getGraphValues(count, System.currentTimeMillis() / MILLI_PER.SECOND);
    }

    public String[] getGraphLabels (int count) {
        return getGraphLabels(count, System.currentTimeMillis() / MILLI_PER.SECOND);
    }




    public double[] getGraphValues (int valueCount, long endTime) {
        
        Database.initTable(Datapoint.class); // since I'm not going through the usual methods, force the table to init
        SQLiteDatabase db = Database.getInstance().getReadableDatabase();

        String query = buildQueryString(valueCount, endTime);
        if (query.isEmpty())
            return new double[0];

        Cursor cursor = db.rawQuery(query, null);


        // sort through the results, fish out what we need
        valueCount = cursor.getCount();
        double[] values = new double[valueCount];
        
        for (int i = 0; i < valueCount && cursor.moveToNext(); i++) {
            if (!cursor.isNull(0))
                values[i] = cursor.getDouble(0);

            // Ranges are normalized for storage. Undo this normalization for display.
            if (datatype == DataType.RANGE)
                values[i] *= rangeMax;
        }

        cursor.close();
        return values;
    }


    private long getEarliestTimestamp () {
        String table = Datapoint.class.getSimpleName();
        String[] columns = new String[] { "timestamp" };
        String where = "metric_id = " + getID() + " and type = '" + datatype.name() + "'";

        Database.initTable(Datapoint.class);
        SQLiteDatabase db = Database.getInstance().getReadableDatabase();
        Cursor cursor = db.query( table, columns, where, null, null, null, "timestamp asc", "1" );
        
        long timestamp = Long.MAX_VALUE;

        if (cursor.moveToFirst())
            timestamp = cursor.getLong(0);

        cursor.close();
        return timestamp;
    }

    private long getLatestTimestamp () {
        String table = Datapoint.class.getSimpleName();
        String[] columns = new String[] { "timestamp" };
        String where = "metric_id = " + getID() + " and type = '" + datatype.name() + "'";

        Database.initTable(Datapoint.class);
        SQLiteDatabase db = Database.getInstance().getReadableDatabase();
        Cursor cursor = db.query( table, columns, where, null, null, null, "timestamp desc", "1" );

        long timestamp = Long.MAX_VALUE;

        if (cursor.moveToFirst())
            timestamp = cursor.getLong(0);

        cursor.close();
        return timestamp;
    }


    private String buildQueryString (int maxValues, long endTime) {
        AggregatePeriod aggPeriod = getAggregatePeriod();

        // shut up and let's go already
        if (aggPeriod == AggregatePeriod.NONE)
            return buildQueryStringWithoutAggregation(maxValues, endTime);

        // Initially, I used a group-by clause and let SQL do the timeslicing for me.
        // However, getting zeroes for timeslices with no entries proved troublesome.
        // Instead, I'm querying each timestamp range individually and unioning them.

        // prep the database query-building machinery
        String table = Datapoint.class.getSimpleName();
        String where = "metric_id = " + getID() + " and type = '" + datatype.name() + "'";
        
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables( table );
        queryBuilder.appendWhere( where );


        // this stuff helps build the right-sized timestamp ranges
        MetricPeriod period = new MetricPeriod(aggPeriod, endTime);
        period.walk( -(maxValues - 1) ); // back up to the first timeslice

        // don't reach further back than the first entry
        long firstEntry = getEarliestTimestamp();
        if (firstEntry == Long.MAX_VALUE)
            return "";

        if (period.getTime() < firstEntry)
            period.setTime(firstEntry);

        long timeMin = Long.MIN_VALUE;
        long timeMax = period.getTime();

        // if we're accumulating values as we go, don't advance the bottom of the timeRange
        boolean advanceTimeMin = (getAggregateFunction() != AggregateFunction.CUMULATIVE);

        // build a subquery for each timeslice.
        String[] subQueries = new String[maxValues];
        int queryCount = 0; // if we duck out early, we need to know how many subQueries we built
        
        for (queryCount = 0; queryCount < maxValues && timeMax <= endTime; queryCount++) {
            period.step();

            if (advanceTimeMin)
                timeMin = timeMax;
            timeMax = period.getTime();

            String[] columns = new String[] { getDatabaseAggregationFunc() + "(value)", "" + queryCount + " as sortOrder" };
            String selection = "timestamp between " + timeMin + " and " + timeMax;

            subQueries[queryCount] = queryBuilder.buildQuery( columns, selection, null, null, null, null );
        }

        if (queryCount == 0)
            return "";

        subQueries = Arrays.copyOf(subQueries, queryCount);
        return queryBuilder.buildUnionQuery( subQueries, "sortOrder", null );
    }


    
    private String buildQueryStringWithoutAggregation (int maxValues, long endTime) {
        String table = Datapoint.class.getSimpleName();
        String where = "metric_id = " + getID() + " and type = '" + datatype.name() + "' and timestamp <= " + endTime;

        // for individual entries, most queries are identical. Min == Max == Avg == Total. Makes things easy.
        if (getAggregateFunction() != AggregateFunction.CUMULATIVE) {
            String query = "select value, timestamp from " + table + " where " + where + " order by timestamp desc limit " + maxValues;
            return "select * from (" + query + ") order by timestamp asc"; // reverse the results
        }
        
        // Cumulative is still a pain in my ass, though.
        List<Datapoint> entries = Database.get(Datapoint.class, where, null, "timestamp desc", maxValues);
        maxValues = entries.size();

        if (maxValues == 0)
            return "";

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables( table );
        queryBuilder.appendWhere( where );

        String[] subQueries = new String[maxValues];

        for (int i = 0; i < entries.size(); i++) {
            String[] columns = new String[] { "sum(value)", "" + (maxValues - i) + " as sortOrder" };
            String selection = "timestamp <= " + entries.get(i).timestamp;
            subQueries[i] = queryBuilder.buildQuery( columns, selection, null, null, null, null );
        }

        return queryBuilder.buildUnionQuery( subQueries, "sortOrder", null );
    }


    private String getDatabaseAggregationFunc () {
        switch (getAggregateFunction()) {
            case AVERAGE:   return "avg";
            case MINIMUM:   return "min";
            case MAXIMUM:   return "max";
            case CUMULATIVE:
            case TOTAL:     return "sum";
            default:        return "";
        }
    }


    public String[] getGraphLabels (int labelCount, long endTime) {
        AggregatePeriod aggPeriod = getAggregatePeriod();

        // if we don't need labels, we can fuck right off
        if (labelCount == 0 || aggPeriod == AggregatePeriod.NONE)
            return new String[0];

        String[] labels = new String[labelCount];
        boolean appendOrdinal = (aggPeriod == AggregatePeriod.DAY);
        Calendar calendar = appendOrdinal ? Calendar.getInstance() : null;

        String format = getGraphLabelFormat();
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        
        MetricPeriod period = new MetricPeriod(aggPeriod, endTime);
        period.walk( -(labelCount-1) );

        for (int i = 0; i < labelCount; i++) {
            long timestamp = period.getTime();
            period.step();

            Date date = new Date( (timestamp * MILLI_PER.SECOND) );
            labels[i] = dateFormat.format(date);

            if (appendOrdinal) {
                calendar.setTimeInMillis(timestamp * MILLI_PER.SECOND);
                labels[i] += getOrdinal( calendar.get(Calendar.DATE) );
            }
        }

        return labels;
    }

    // ugh. This should probably be internationalized.
    private String getGraphLabelFormat () {
        switch (getAggregatePeriod()) {
            case WEEK:  return "d/M";
            case YEAR:  return "yy";
            case MONTH: return "MMM";
            case DAY:   return "d";
            case NONE:  
            default:    return "";
        }
    }

    private static String getOrdinal (final int day) {
        if (day >= 11 && day <= 13)
            return "th";

        switch (day % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
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
        BaseColumns._ID, 
        "name",
        "datatype",
        "aggregatePeriod",
        "aggregateFunction",
        "rangeMax",
        "durationPickerExtension",
        "graphStyle_id"
    };
    public static final String[] COLUMN_TYPES = new String[] {
        Database.TableSchema.ID_TYPE_DECLARATION,
        "text",
        "text",
        "text",
        "text",
        "integer",
        "text",
        "integer"
    };

    public ContentValues getValues () {
        ContentValues values = new ContentValues();
        values.put( BaseColumns._ID, _id );
        values.put( "name", name );
        values.put( "datatype", datatype.name() );
        values.put( "aggregatePeriod", aggregatePeriod.name() );
        values.put( "aggregateFunction", aggregateFunction.name() );
        values.put( "rangeMax", rangeMax );
        values.put( "durationPickerExtension", durationPickerExtension.name() );
        values.put( "graphStyle_id", graphStyle_id );
        return values;
    }

    public void setValues (Cursor values) {
        _id = values.getLong(0);
        name = values.getString(1);

        try { datatype = Enum.valueOf(DataType.class, values.getString(2)); }
        catch (IllegalArgumentException e) { datatype = DataType.AMOUNT; }

        try { aggregatePeriod = Enum.valueOf(AggregatePeriod.class, values.getString(3)); }
        catch (IllegalArgumentException e) { aggregatePeriod = AggregatePeriod.AUTO; }

        try { aggregateFunction = Enum.valueOf(AggregateFunction.class, values.getString(4)); }
        catch (IllegalArgumentException e) { aggregateFunction = AggregateFunction.TOTAL; }

        rangeMax = values.getInt(5);

        try { durationPickerExtension = Enum.valueOf(DurationPickerExt.class, values.getString(6)); }
        catch (IllegalArgumentException e) { durationPickerExtension = DurationPickerExt.NONE; }

        graphStyle_id = values.getLong(7);
    }

    public Bundle bundle () {
        Bundle values = new Bundle();
        values.putLong( BaseColumns._ID, _id );
        values.putString( "name", name );
        values.putString( "datatype", datatype.name() );
        values.putString( "aggregatePeriod", aggregatePeriod.name() );
        values.putString( "aggregateFunction", aggregateFunction.name() );
        values.putInt( "rangeMax", rangeMax );
        values.putString( "durationPickerExtension", durationPickerExtension.name() );
        values.putLong( "graphStyle_id", graphStyle_id );
        return values;
    }

    public void setValues (Bundle values) {
        _id = values.getLong(BaseColumns._ID);
        name = values.getString("name");

        try { datatype = Enum.valueOf(DataType.class, values.getString("datatype")); }
        catch (IllegalArgumentException e) { datatype = DataType.AMOUNT; }

        try { aggregatePeriod = Enum.valueOf(AggregatePeriod.class, values.getString("aggregatePeriod")); }
        catch (IllegalArgumentException e) { aggregatePeriod = AggregatePeriod.AUTO; }

        try { aggregateFunction = Enum.valueOf(AggregateFunction.class, values.getString("aggregateFunction")); }
        catch (IllegalArgumentException e) { aggregateFunction = AggregateFunction.TOTAL; }

        rangeMax = values.getInt("rangeMax");

        try { durationPickerExtension = Enum.valueOf(DurationPickerExt.class, values.getString("durationPickerExtension")); }
        catch (IllegalArgumentException e) { durationPickerExtension = DurationPickerExt.NONE; }

        graphStyle_id = values.getLong("graphStyle_id");
    }







}


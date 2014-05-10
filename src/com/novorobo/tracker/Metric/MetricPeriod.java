package com.novorobo.tracker.metric;

import com.novorobo.constants.*;
import com.novorobo.tracker.*;
import com.novorobo.tracker.metric.Metric.AggregatePeriod;

import java.util.Calendar;

public class MetricPeriod {
    private Calendar time = Calendar.getInstance();
    private AggregatePeriod period;

    private int stepField;
    private int stepSize = 1;

    public MetricPeriod (AggregatePeriod period, long timestamp) {
        this.period = period;

        // weeks are fiddly because Calendar isn't really set up to deal with them quite as well.
        if (period == AggregatePeriod.WEEK) {
            stepField = Calendar.DAY_OF_MONTH;
            stepSize = 7;
        }

        // everything else is quite straightforwards
        else if (period == AggregatePeriod.DAY)   stepField = Calendar.DAY_OF_MONTH;
        else if (period == AggregatePeriod.MONTH) stepField = Calendar.MONTH;
        else if (period == AggregatePeriod.YEAR)  stepField = Calendar.YEAR;

        // AggregatePeriod.NONE: can't do it. Shouldn't have to.
        else throw new IllegalArgumentException("Unhandled AggregatePeriod value");

        setTime(timestamp);
    }

    public void setTime (long timestamp) {
        time.setTimeInMillis( timestamp * MILLI_PER.SECOND );
        gotoPeriodStart();
    }

    public long getTime () {
        return time.getTimeInMillis() / MILLI_PER.SECOND;
    }

    public void walk (int steps) {
        time.add(stepField, stepSize * steps);
    }

    public void step () {
        walk(1);
    }


    private void gotoPeriodStart () {
        switch (period) {
            case YEAR:  time.set(Calendar.MONTH, 0);
            case MONTH: time.set(Calendar.DAY_OF_MONTH, 1);
        }

        if (period == AggregatePeriod.WEEK)
            time.set(Calendar.DAY_OF_WEEK, time.getFirstDayOfWeek());

        time.set(Calendar.HOUR_OF_DAY, 0);
        time.set(Calendar.MINUTE, 0);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);
    }

}
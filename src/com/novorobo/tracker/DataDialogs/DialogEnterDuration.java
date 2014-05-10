package com.novorobo.tracker.app;

import com.novorobo.tracker.datapoint.*;
import com.novorobo.tracker.metric.Metric;
import com.novorobo.constants.*;

import android.view.View;
import android.view.LayoutInflater;

import android.widget.NumberPicker;

public class DialogEnterDuration extends DataEntryDialog {

    protected NumberPicker days;
    protected NumberPicker hours;
    protected NumberPicker minutes;
    protected NumberPicker seconds;
    protected NumberPicker centiseconds;

    public DialogEnterDuration () { super(); }
    public DialogEnterDuration (Datapoint target) { super(target); }

    protected double getValue () {
        return centiseconds.getValue() * MILLI_PER.CENTISECOND +
               seconds.getValue() * MILLI_PER.SECOND +
               minutes.getValue() * MILLI_PER.MINUTE +
               hours.getValue() * MILLI_PER.HOUR +
               days.getValue() * MILLI_PER.DAY;
    }

    protected void setValue (double milliseconds) {
        days.setValue((int) Math.floor(milliseconds / MILLI_PER.DAY) );
        hours.setValue((int) Math.floor(milliseconds / MILLI_PER.HOUR) % 24 );
        minutes.setValue((int) Math.floor(milliseconds / MILLI_PER.MINUTE) % 60 );
        seconds.setValue((int) Math.floor(milliseconds / MILLI_PER.SECOND) % 60 );
        centiseconds.setValue((int) Math.floor(milliseconds / MILLI_PER.CENTISECOND) % 100 );
    }

    protected View getDataEntryViewSlug () {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View durationPicker = inflater.inflate(R.layout.data_entry_view_duration, null);

        days = (NumberPicker) durationPicker.findViewById(R.id.days);
        days.setMinValue(0);
        days.setMaxValue(999);
        days.setWrapSelectorWheel(false);

        hours = (NumberPicker) durationPicker.findViewById(R.id.hours);
        hours.setMinValue(0);
        hours.setMaxValue(23);
        hours.setWrapSelectorWheel(true);
        hours.setFormatter(formatter);

        minutes = (NumberPicker) durationPicker.findViewById(R.id.minutes);
        minutes.setMinValue(0);
        minutes.setMaxValue(59);
        minutes.setWrapSelectorWheel(true);
        minutes.setFormatter(formatter);

        seconds = (NumberPicker) durationPicker.findViewById(R.id.seconds);
        seconds.setMinValue(0);
        seconds.setMaxValue(59);
        seconds.setWrapSelectorWheel(true);
        seconds.setFormatter(formatter);

        centiseconds = (NumberPicker) durationPicker.findViewById(R.id.centiseconds);
        centiseconds.setMinValue(0);
        centiseconds.setMaxValue(99);
        centiseconds.setWrapSelectorWheel(true);
        centiseconds.setFormatter(formatter);

        setValue(datapoint.value);

        // deciding whether to show the extra pickers for days and centiseconds is fiddly.
        // Here's the logic:
        //  1- show the days/centiseconds picker if the settings request it
        //  2- also show a picker if it would have a non-zero value
        //  3- only show ONE of days/centiseconds. Days take precedence.

        Metric.DurationPickerExt pickerExt = datapoint.getMetric().durationPickerExtension;

        boolean showDays = (pickerExt == Metric.DurationPickerExt.DAYS);
        showDays = showDays || days.getValue() > 0;

        boolean showCentiseconds = (pickerExt == Metric.DurationPickerExt.CENTISECONDS);
        showCentiseconds = showCentiseconds || centiseconds.getValue() > 0;
        showCentiseconds = showCentiseconds && !showDays;

        if (!showCentiseconds) {
            centiseconds.setVisibility(View.GONE);
            durationPicker.findViewById(R.id.centisecond_separator).setVisibility(View.GONE);
        }

        if (!showDays) {
            days.setVisibility(View.GONE);
            durationPicker.findViewById(R.id.day_separator).setVisibility(View.GONE);
        }

        return durationPicker;
    }

    private static NumberPicker.Formatter formatter = new NumberPicker.Formatter() {
        public String format (int value) {
            if (value <= 9)
                return "0" + String.valueOf(value);
            return String.valueOf(value);
        }
    };

}
package com.novorobo.tracker.app;

import com.novorobo.tracker.datapoint.*;

import android.view.View;
import android.view.LayoutInflater;

import android.widget.SeekBar;
import android.widget.TextView;


public class DialogEnterRange extends DataEntryDialog {

    protected SeekBar slider;
    protected TextView readout;

    public DialogEnterRange () { super(); }
    public DialogEnterRange (Datapoint target) { super(target); }

    protected double getValue () {
        return (double)slider.getProgress() / (double)slider.getMax();
    }

    // appears unnecessary
    protected void setValue (double value) {}

    protected View getDataEntryViewSlug () {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View rangePicker = inflater.inflate(R.layout.data_entry_view_range, null);

        int max = datapoint.getMetric().rangeMax;
        int value = (int) Math.round(datapoint.value * max);

        slider = (SeekBar) rangePicker.findViewById(R.id.slider);
        slider.setMax( max );
        slider.setOnSeekBarChangeListener( changeListener );
        slider.setProgress( value );

        readout = (TextView) rangePicker.findViewById(R.id.number);
        readout.setText( "" + value );

        return rangePicker;
    }


    protected SeekBar.OnSeekBarChangeListener changeListener = 
        new SeekBar.OnSeekBarChangeListener () {
            public void onProgressChanged (SeekBar seekBar, int value, boolean fromUser) {
                readout.setText( "" + value );
            }
            public void onStartTrackingTouch (SeekBar seekBar) {}
            public void onStopTrackingTouch (SeekBar seekBar) {}
        };
}
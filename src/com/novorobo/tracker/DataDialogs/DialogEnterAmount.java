package com.novorobo.tracker.app;

import com.novorobo.tracker.datapoint.*;

import java.lang.Double;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.LayoutInflater;

import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageButton;

import android.widget.NumpadInput;

public class DialogEnterAmount extends DataEntryDialog {

    protected NumpadInput numpad;

    public DialogEnterAmount () { super(); }
    public DialogEnterAmount (Datapoint target) { super(target); }

    protected double getValue () {
        return numpad.getValue();
    }

    protected void setValue (double value) {
        numpad.setValue(value);
    }

    protected View getDataEntryViewSlug () {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.data_entry_view_amount, null);

        numpad = (NumpadInput) view.findViewById(R.id.numpad);
        numpad.setValue(datapoint.value);
        return view;
    }
}
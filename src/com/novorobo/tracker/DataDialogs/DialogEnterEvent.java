package com.novorobo.tracker.app;

import com.novorobo.tracker.datapoint.*;

import android.view.View;
import android.view.LayoutInflater;

public class DialogEnterEvent extends DataEntryDialog {

	public DialogEnterEvent () { super(); }
    public DialogEnterEvent (Datapoint target) { super(target); }

    protected View getDataEntryViewSlug () {
        return null;
    }

    protected double getValue () {
    	return 1;
    }

    protected void setValue (double value) {}

}
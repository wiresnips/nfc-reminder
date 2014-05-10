package com.novorobo.tracker.app;

import com.novorobo.market.MarketActivity;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Bundle;

import com.google.analytics.tracking.android.EasyTracker;

public class HelpWidgetCreation extends MarketActivity {

	public void onCreate (Bundle state) {
	    super.onCreate(state);
	    setContentView(R.layout.help_widget_creation);
	}

    public void onStart () {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

}
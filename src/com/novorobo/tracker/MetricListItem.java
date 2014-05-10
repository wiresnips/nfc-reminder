package com.novorobo.tracker.app;

import com.novorobo.util.database.Database;
import com.novorobo.tracker.metric.Metric;
import com.novorobo.tracker.datapoint.Datapoint;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;

import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.LinearLayout;



public class MetricListItem extends Preference {
	private Activity activity;

	private int metricIndex;
	private ImageView button;

	public MetricListItem (final Activity activity, final Metric metric) {
		super(activity);
		this.activity = activity;
		setWidgetLayoutResource(R.layout.metric_context_button);
		setTarget(metric);
	}

	public void setTarget (final Metric metric) {
		setTitle(metric.name);
		setOnPreferenceClickListener( new OnPreferenceClickListener () {
	        public boolean onPreferenceClick (Preference preference) {
	            Intent edit = new Intent(activity, MetricEdit.class);
	            edit.putExtra( Metric.BUNDLE, metric.bundle() );
	            activity.startActivityForResult( edit, MetricEdit.REQUEST_CODE_EDIT );
	            return true;
	        }
    	});
	}

	protected void onBindView (View view) {
		super.onBindView(view);

		button = (ImageView) view.findViewById(R.id.metric_context_button);
		activity.registerForContextMenu(button);

		button.setTag(metricIndex);
		button.setOnClickListener(contextMenu);
		button.setLongClickable(false);
	}

	public void setIndex (int index) {
		metricIndex = index;
		if (button != null)
			button.setTag(metricIndex);
	}


	private OnClickListener contextMenu = new OnClickListener () {
        public void onClick (View view) {
        	activity.openContextMenu(view);
        }
    };

}

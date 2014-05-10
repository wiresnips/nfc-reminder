package com.novorobo.tracker.app;

import com.novorobo.util.database.Database;
import com.novorobo.tracker.metric.Metric;
import com.novorobo.tracker.graph.GraphStyle;
import com.novorobo.tracker.datapoint.Datapoint;

import com.novorobo.market.MarketActivity;

import android.os.Bundle;
import android.content.Intent;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.FragmentManager;
import android.app.DialogFragment;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;

import android.app.Dialog;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;

public class MetricEdit extends MarketActivity implements MetricDeletionDialog.OnDeleteListener {
    public static final int REQUEST_CODE_EDIT = 1;
    public static final String SAVED_METRIC = "SavedMetric";


    private Metric metric;
    public Metric getMetric () { return metric; }

    private MetricSettings settingsFragment = null;

    public void onCreate (Bundle state) {
        Database.init(this);

        // if we're editing an existing metric, here it is!
        Bundle metricBundle = getIntent().getBundleExtra(Metric.BUNDLE);
        getActionBar().setTitle( metricBundle == null ? R.string.create_metric : R.string.edit_metric );

        metric = new Metric();

        // the saved instance state takes precedence over the intent, though
        if (state != null)
            metric.setValues( state.getBundle(SAVED_METRIC) );

        else if (metricBundle != null)
            metric.setValues(metricBundle);

        // once we've instantiated Metric, we can go through to the super.onCreate
        super.onCreate(state);


        setContentView(R.layout.metric_edit);
        
        // prep the content
        if (state == null) {
            settingsFragment = new MetricSettings();
            getFragmentManager().beginTransaction()
                                .add(R.id.socket, settingsFragment, MetricSettings.TAG)
                                .commit();
        } 
        else {
            settingsFragment = (MetricSettings) getFragmentManager().findFragmentByTag(MetricSettings.TAG);
        }
    }


    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.metric_edit_actionbar, menu);
        return true;
    }


    public boolean onPrepareOptionsMenu (Menu menu) {
        boolean metricSaved = metric.getID() != 0;
        menu.findItem(R.id.add_data).setEnabled(metricSaved);
        menu.findItem(R.id.view_data).setEnabled(metricSaved);
        menu.findItem(R.id.preview_graph).setEnabled(metricSaved);
        
        return super.onPrepareOptionsMenu(menu);
    }


    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()) {

            case R.id.help:
                startActivity( new Intent(this, HelpMetricEdit.class) );
                return true;

            case R.id.delete:
                MetricDeletionDialog confirm = new MetricDeletionDialog(metric);
                confirm.show(getFragmentManager(), null);
                return true;

            case R.id.done:
            case android.R.id.home:
                onBackPressed();
                return true;


            case R.id.view_data:
                if (metric.getID() == 0)
                    Toast.makeText(this, R.string.cannot_view_data_without_saving, Toast.LENGTH_LONG).show();
                else {
                    Intent showData = new Intent(this, DatapointList.class);
                    showData.putExtra( Metric.BUNDLE, metric.bundle() );
                    startActivity( showData );
                }
                return true;

            case R.id.add_data:
                if (metric.getID() == 0)
                    Toast.makeText(this, R.string.cannot_add_data_without_saving, Toast.LENGTH_LONG).show();
                else {
                    Datapoint datapoint = new Datapoint(metric);
                    DataEntryDialog dialog = datapoint.getDialog();
                    dialog.show(getFragmentManager(), null); 
                }
                return true;

            case R.id.preview_graph:
                Intent preview = new Intent(this, PreviewGraph.class);
                preview.putExtra( Metric.BUNDLE, metric.bundle() );
                startActivity(preview);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onBackPressed () {

        if (!metric.hasName()) {
            ConfirmDiscardUnnamed confirm = new ConfirmDiscardUnnamed();
            confirm.show(getFragmentManager(), null);
        }

        else {
            Intent result = new Intent();
            result.putExtra( Metric.BUNDLE, metric.bundle() );
            result.putExtra( Metric.ID, metric.getID() );
            setResult( RESULT_OK, result );
            finish();
            super.onBackPressed();
        }
    }



    public static class ConfirmDiscardUnnamed extends DialogFragment {
        private MetricEdit owner;

        public ConfirmDiscardUnnamed () { super(); }

        public void onAttach (Activity activity) {
            super.onAttach(activity);
            try { owner = (MetricEdit) activity; }
            catch (ClassCastException e) {}
        }
        public void onDismiss (DialogInterface dialog) {
            super.onDismiss(dialog);
            owner = null;
        }

        public Dialog onCreateDialog (Bundle state) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setIconAttribute( R.attr.icon_warning );
            builder.setTitle( R.string.metric_unnamed_title );
            builder.setMessage( R.string.metric_unnamed_warning );
            builder.setPositiveButton(R.string.metric_unnamed_go_back, null);
            builder.setNegativeButton(R.string.metric_unnamed_discard, new DialogInterface.OnClickListener() {
                public void onClick (DialogInterface dialog, int id) {
                    owner.setResult( RESULT_CANCELED );
                    owner.finish();
                }
            });
            return builder.create();
        }
    }


    public void onMetricDeleted (Metric metric) {
        Intent deleted = new Intent().putExtra( Metric.BUNDLE, metric.bundle() );
        setResult( RESULT_CANCELED, deleted );
        finish();
    }

    public void onSaveInstanceState (Bundle saveState) {
        super.onSaveInstanceState(saveState);
        saveState.putBundle( SAVED_METRIC, metric.bundle() );
    }



    
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == GraphStyleEdit.REQUEST_CODE_EDIT_STYLE && resultCode == RESULT_OK) {
            settingsFragment.updateStyle( data.getBundleExtra(GraphStyle.BUNDLE) );

            if (metric.getID() != 0)
                GraphWidgetProvider.updateMetric( this, metric.getID() );
        }
    }


    public void onStart () {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    public void onStop () {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
        if (metric.getID() != 0)
            GraphWidgetProvider.updateMetric( this, metric.getID() );
    }


}

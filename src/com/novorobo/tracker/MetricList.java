package com.novorobo.tracker.app;

import com.novorobo.util.database.Database;
import android.provider.BaseColumns;

import com.novorobo.tracker.metric.Metric;
import com.novorobo.tracker.datapoint.Datapoint;

import com.novorobo.market.MarketActivity;


import java.util.List;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;

import android.app.PendingIntent;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.content.SharedPreferences;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.IntentSender.SendIntentException;

import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.ads.*;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;


public class MetricList extends MarketActivity implements MetricDeletionDialog.OnDeleteListener {

    public static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWYXZabcdefghijklmnopqrstuvwyxz1234567890`~!@#$%^&*()_-+={}|[]\\:\";'<>?,./";
    public static final String SHARED_PREFS = "com.novorobo.tracker.app.SharedPrefs";
    public static final String FIRST_LAUNCH = "FirstLaunch";
    public static final int REQUEST_CODE_LIST_DATA = 2;

    protected List<Metric> metrics;
    protected List<MetricListItem> metricListItems;
    protected MetricListFragment metricListFragment;


    public void onCreate (Bundle state) {
        Database.init(this);
        super.onCreate(state);

        metrics = Database.get(Metric.class, null, null, BaseColumns._ID + " desc" );
        metricListItems = new ArrayList<MetricListItem>();

        for (int i = 0; i < metrics.size(); i++)
            metricListItems.add( new MetricListItem(this, metrics.get(i)) );

        setContentView(R.layout.metric_list);

        if (state == null) {
            metricListFragment = new MetricListFragment();
            getFragmentManager().beginTransaction()
                .add(R.id.list_socket, metricListFragment, MetricListFragment.TAG)
                .commit();
        }
        else {
            metricListFragment = (MetricListFragment) getFragmentManager().findFragmentByTag(MetricListFragment.TAG);
        }


        maybeShowFirstLaunchDialog();
    }

    public void onResume () {
        super.onResume();
        metricListFragment.refreshPreferenceItems();
        updatePrompt();


        // have we opted out of analytics?
        SharedPreferences records = getSharedPreferences(SHARED_PREFS, 0);
        boolean useAnalytics = records.getBoolean(About.USE_ANALYTICS, true);
        GoogleAnalytics.getInstance(this).setAppOptOut( !useAnalytics );
    }


    public void onStart () {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }


    public static class MetricListFragment extends PreferenceFragment {
        public static final String TAG = "METRIC_LIST_FRAGMENT";

        public void refreshPreferenceItems () {
            MetricList activity = (MetricList) getActivity();

            PreferenceScreen root = getPreferenceScreen();
            if (root == null)
                root = getPreferenceManager().createPreferenceScreen(getActivity());
            else
                root.removeAll();

            for (int i = 0; i < activity.metricListItems.size(); i++) {
                MetricListItem item = activity.metricListItems.get(i);
                item.setIndex(i); // idiot workaround for idiots (that's me)
                root.addPreference(item);
            }

            setPreferenceScreen(root);
        }
    }



    
    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.metric_list_actionbar, menu);
        return true;
    }


    // if we've already rated the app, strip out the menuitem
    public boolean onPrepareOptionsMenu (Menu menu) {
        SharedPreferences records = getSharedPreferences(SHARED_PREFS, 0);
        boolean alreadyRatedPerhaps = records.getBoolean(AppRater.VISITED_MARKET_TO_RATE, false);
        boolean marketExists = AppRater.marketExists(this);
        boolean canUpgrade = !isUserPremium() && getUpgradeIntent() != null;

        MenuItem rate = menu.findItem(R.id.rate);
        rate.setVisible(!alreadyRatedPerhaps && marketExists);
        rate.setEnabled(!alreadyRatedPerhaps && marketExists);

        MenuItem upgrade = menu.findItem(R.id.upgrade);
        upgrade.setVisible(canUpgrade && marketExists);
        upgrade.setEnabled(canUpgrade && marketExists);
        
        return super.onPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()) {
            
            case R.id.help:
                startActivity( new Intent(this, HelpIndex.class) );
                return true;

            case R.id.new_metric:
                startActivityForResult( new Intent(this, MetricEdit.class), MetricEdit.REQUEST_CODE_EDIT );
                return true;        

            case R.id.about:
                startActivity( new Intent(this, About.class) );
                return true;

            case R.id.rate:
                AppRater.launchMarketIntent(this);
                return true;

            case R.id.upgrade:
                try {
                    startIntentSenderForResult( 
                        getUpgradeIntent().getIntentSender(), PromptDialog.PURCHASE_PREMIUM_REQUEST_CODE, 
                        new Intent(), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0) );
                } 
                catch (SendIntentException e) {}                
                return true;
        }

        return super.onOptionsItemSelected(item);
    }



    // hack together my own context menu. Because shut your damn mouth, that's why.
    private static final int MENU_VIEW_DATA = 1;
    private static final int MENU_ADD_DATA = 2;
    private static final int MENU_EDIT_METRIC = 3;
    private static final int MENU_DELETE_METRIC = 4;
    private static final int MENU_PREVIEW_GRAPH = 5;

    public void onCreateContextMenu (ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        // I'm more than a little ashamed of this bit. Let's all just move on.
        int metricIndex = (Integer) view.getTag(); 

        menu.setHeaderTitle( metrics.get(metricIndex).name );
        menu.add(metricIndex, MENU_VIEW_DATA,     0, R.string.view_data);
        menu.add(metricIndex, MENU_ADD_DATA,      1, R.string.add_data);
        menu.add(metricIndex, MENU_EDIT_METRIC,   2, R.string.edit_metric);
        menu.add(metricIndex, MENU_DELETE_METRIC, 3, R.string.delete_metric);
        menu.add(metricIndex, MENU_PREVIEW_GRAPH, 4, R.string.preview_graph);
    }


    //*
    public boolean onContextItemSelected (MenuItem item) {
        // the fuck is a group identifier supposed to be? I don't fuckin' know. I'M using it to carry an index.
        Metric metric = metrics.get( item.getGroupId() ); 

        switch (item.getItemId()) {
            
            case MENU_EDIT_METRIC:
                Intent edit = new Intent(this, MetricEdit.class);
                edit.putExtra( Metric.BUNDLE, metric.bundle() );
                startActivityForResult( edit, MetricEdit.REQUEST_CODE_EDIT );
                return true;

            case MENU_DELETE_METRIC:
                MetricDeletionDialog confirm = new MetricDeletionDialog(metric);
                confirm.show(getFragmentManager(), null);
                return true;

            case MENU_ADD_DATA:
                addDataDialog(metric);
                return true;

            case MENU_VIEW_DATA:
                Intent showData = new Intent(this, DatapointList.class);
                showData.putExtra( Metric.BUNDLE, metric.bundle() );
                startActivityForResult( showData, REQUEST_CODE_LIST_DATA );
                return true;

            case MENU_PREVIEW_GRAPH:
                Intent preview = new Intent(this, PreviewGraph.class);
                preview.putExtra( Metric.BUNDLE, metric.bundle() );
                startActivity(preview);
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }
    //*/

    public void addDataDialog (Metric metric) {
        Datapoint datapoint = new Datapoint(metric);
        DataEntryDialog dialog = datapoint.getDialog();
        dialog.show(getFragmentManager(), null); 
    }


    // returning from editing 
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != MetricEdit.REQUEST_CODE_EDIT || data == null)
            return;

        Metric metric = new Metric();
        metric.setValues( data.getBundleExtra(Metric.BUNDLE) );
        
        if (resultCode == RESULT_OK)
            updateMetric( metric );

        else if (resultCode == RESULT_CANCELED)
            onMetricDeleted( metric );
    }


    private void updateMetric (Metric metric) {
        if (metric == null)
            return;

        // do we already have this metric?
        long metricID = metric.getID();

        int index = -1;
        for (int i = 0; i < metrics.size() && index == -1; i++)
            if (metrics.get(i).getID() == metricID)
                index = i;

        // if we already have it, make sure our listings are up-to-date
        if (index != -1) {
            metrics.set(index, metric);
            metricListItems.get(index).setTarget(metric);
        }

        // if we DON'T have it, add it to the list and make a new listItem for it
        else {
            metrics.add(metric);
            metricListItems.add( new MetricListItem(this, metric) );
            metricListFragment.refreshPreferenceItems();
            maybeShowWidgetGuideDialog();
        }

        updatePrompt();
    }

    protected void updatePrompt () {
        boolean showPrompt = (metrics.size() == 0);
        findViewById(R.id.prompt).setVisibility( showPrompt ? View.VISIBLE : View.GONE );
        findViewById(R.id.list_socket).setVisibility( showPrompt ? View.GONE : View.VISIBLE );
    }


    public void onMetricDeleted (Metric metric) {
        Long metricID = metric.getID();
        boolean deletion = false;

        for (int i = metrics.size() - 1; i >= 0; i--) {
            if (metrics.get(i).getID() == metricID) {
                metricListItems.remove(i);
                metrics.remove(i);
                deletion = true;
            }
        }

        if (deletion) {
            metricListFragment.refreshPreferenceItems();
            updatePrompt();
        }
    }


    // best function name A+++ would call again
    private void maybeShowWidgetGuideDialog () {
        SharedPreferences settings = getSharedPreferences(SHARED_PREFS, 0);
        boolean showDialog = settings.getBoolean( HelpWidgetCreationDialog.SHOW_WIDGET_GUIDE_DIALOG, true );
        
        if (showDialog)
            new HelpWidgetCreationDialog().show(getFragmentManager(), "tag");
    }



    private void maybeShowFirstLaunchDialog () {
        SharedPreferences records = getSharedPreferences(SHARED_PREFS, 0);
        boolean firstLaunch = records.getBoolean(FIRST_LAUNCH, true);
        
        if (!firstLaunch)
            return;

        SharedPreferences.Editor editor = records.edit();
        editor.putBoolean(FIRST_LAUNCH, false);
        editor.commit();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle( R.string.first_launch_title );
        builder.setMessage( R.string.first_launch_message );
        builder.setPositiveButton( R.string.gotcha, null );
        builder.create().show();
    }
}


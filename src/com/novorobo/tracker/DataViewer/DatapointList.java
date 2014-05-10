package com.novorobo.tracker.app;

import com.novorobo.util.database.Database;
import com.novorobo.tracker.app.DatapointAdapter;
import com.novorobo.tracker.metric.Metric;
import com.novorobo.tracker.datapoint.Datapoint;
import com.novorobo.constants.*;

import com.novorobo.market.MarketInterface;
import com.novorobo.market.MarketInterface.InitListener;

import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;

import android.app.Activity;
import android.app.ListActivity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.AlertDialog;
import android.app.FragmentManager;

import android.os.Bundle;
import android.util.Log;

import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MotionEvent;
import android.view.DragEvent;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.FrameLayout;

import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AbsListView.MultiChoiceModeListener;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import android.view.ActionMode;

import com.google.android.gms.ads.*;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

import com.novorobo.util.ugh.DialogFragmentWithDismissCallback.DismissCallback;


public class DatapointList extends ListActivity implements DismissCallback {

    private Metric metric;

    private AdView banner = null;
    private MarketInterface market = null;
    private boolean premium = true;

    public void onCreate (Bundle state) {
        Database.init(this);
        super.onCreate(state);
        setContentView(R.layout.datapoint_list);

        Intent intent = getIntent();
        Bundle metricBundle = intent.getBundleExtra(Metric.BUNDLE);

        if (metricBundle == null) {
            finish();
            return;
        }

        metric = new Metric();
        metric.setValues(metricBundle);

        setTitle( getString(R.string.view_data) + ": " + metric.name );
        setListAdapter( new DatapointAdapter(this, getCursor(), metric.datatype) );
        
        ListView list = getListView();
        list.setDivider(null);

        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        list.setMultiChoiceModeListener(batchDeleter);

        updatePrompt();


        market = new MarketInterface(this);
        market.addInitListener( new InitListener () {
            public void onInit () {
                if (market.ownsPremium())
                    return;

                premium = false;

                AdRequest request = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)        // Emulator
                    .addTestDevice("181A55C5FAB7738ACB66E81CAD811A43")  // muh PHONE!
                    .build();

                banner = (AdView) findViewById(R.id.banner);
                if (banner == null)
                    return;

                banner.setVisibility(View.VISIBLE);
                banner.loadAd(request);
            }
        });

    }

    // if we come back after leaving, refresh the list
    private boolean refreshListOnReturn = false;

    public void onStart () {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);

        if (refreshListOnReturn)
            refreshList();
        refreshListOnReturn = true;
    }

    public void onResume () {
        super.onResume();
        if (banner != null)
            banner.resume();
    }

    public void onPause () {
        super.onPause();
        if (banner != null)
            banner.pause();
    }

    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

    public void onDestroy() {
        super.onDestroy();
        
        if (market != null)
            market.dispose();

        if (banner != null)
            banner.destroy();
    }







    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.data_viewer_actionbar, menu);
        return true;
    }

    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_data:
                Datapoint datapoint = new Datapoint(metric);
                DataEntryDialog dialog = datapoint.getDialog();
                dialog.show(getFragmentManager(), null); 
                return true;        
        }

        return super.onOptionsItemSelected(item);
    }






    private Cursor getCursor () {
        String table = Datapoint.class.getSimpleName();
        String[] columns = new String[] { BaseColumns._ID, "timestamp", "value" };
        String where = "metric_id = ? and type = ?";
        String[] whereParams = new String[] { metric.getID() + "", metric.datatype.name() };
        String order = "timestamp desc";

        Database.initTable(Datapoint.class);
        SQLiteDatabase db = Database.getInstance().getReadableDatabase();

        return db.query( table, columns, where, whereParams, null, null, order );
    }




    protected void onListItemClick (ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Datapoint datapoint = Database.get(Datapoint.class, id);
        DataEntryDialog dialog = datapoint.getDialog();
        dialog.show(getFragmentManager(), null);   
    }


    private MultiChoiceModeListener batchDeleter = new MultiChoiceModeListener() {

        public boolean onCreateActionMode (ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.data_viewer_contextual, menu);
            return true;
        }

        // only one button. No need to be intelligent about it.
        public boolean onActionItemClicked (ActionMode mode, MenuItem item) {
            ConfirmDataDelete confirm = new ConfirmDataDelete();
            confirm.show(getFragmentManager(), null);   
            return true;
        }

        public void onItemCheckedStateChanged (ActionMode mode, int position, long id, boolean checked) {}
        public void onDestroyActionMode (ActionMode mode) {}
        public boolean onPrepareActionMode (ActionMode mode, Menu menu) { return false; }
    };




    protected ActionMode actionMode;
    public void onActionModeStarted (ActionMode mode) {
        super.onActionModeStarted(mode);
        actionMode = mode;
        actionMode.setTitle(R.string.datapoint_mass_delete);
    }
    public void onActionModeFinished (ActionMode mode) {
        super.onActionModeFinished(mode);
        actionMode = null;
    }


    public static class ConfirmDataDelete extends DialogFragment {
        private DatapointList owner;

        public ConfirmDataDelete () { super(); }

        public void onAttach (Activity activity) {
            super.onAttach(activity);
            try { owner = (DatapointList) activity; }
            catch (ClassCastException e) {}
        }
        public void onDismiss (DialogInterface dialog) {
            super.onDismiss(dialog);
            owner = null;
        }

        public Dialog onCreateDialog (Bundle state) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setIconAttribute( R.attr.icon_warning );
            builder.setTitle( R.string.confirm_delete_datapoint );
            builder.setMessage( R.string.confirm_delete_datapoint_message );
            
            builder.setNegativeButton(R.string.cancel, null);

            builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                public void onClick (DialogInterface dialog, int id) {
                    owner.deleteDataPoints();
                }
            });

            return builder.create();
        }
    }


    protected void deleteDataPoints () {
        String idList = "";

        long[] ids = getListView().getCheckedItemIds();
        for (long id : ids)
            idList += ", " + id;
        idList = idList.substring(2);        

        Database.delete( Datapoint.class, BaseColumns._ID + " in (" + idList + ")", null );
        GraphWidgetProvider.updateMetric( this, metric.getID() );
        refreshList();
        reportAnalytics(ids.length);
    }


    private void refreshList () {
        DatapointAdapter adapter = (DatapointAdapter) getListView().getAdapter();
        adapter.changeCursor(getCursor());
        adapter.notifyDataSetChanged();
        updatePrompt();
    }


    public void onDialogDismissed (String tag) {
        refreshList();
    }



    protected void updatePrompt () {
        boolean showPrompt = (getListAdapter().getCount() == 0);

        FrameLayout root = (FrameLayout) findViewById(android.R.id.content);    
        View prompt = root.findViewById(R.id.prompt);

        if (showPrompt && prompt == null)
            prompt = View.inflate(this, R.layout.datapoint_listing_prompt, root);

        else if (!showPrompt && prompt != null)
            root.removeView(prompt);
    }


    private void reportAnalytics (long deleteCount) {
        EasyTracker easyTracker = EasyTracker.getInstance(this);

        if (easyTracker == null)
            return;

        easyTracker.send(MapBuilder.createEvent(
            Datapoint.CATEGORY, 
            ACTION.DELETE, 
            metric.datatype.name(), 
            deleteCount).build());
    }

}


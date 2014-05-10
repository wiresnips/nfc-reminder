package com.novorobo.tracker.app;

import com.novorobo.util.database.Database;
import com.novorobo.tracker.metric.Metric;
import com.novorobo.tracker.graph.GraphStyle;
import com.novorobo.tracker.datapoint.Datapoint;

import com.novorobo.market.MarketActivity;

import android.os.Bundle;
import android.content.Context;
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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.app.Dialog;
import android.app.DialogFragment;

import android.widget.Toast;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ClipDescription;


import com.google.analytics.tracking.android.EasyTracker;


public class GraphStyleEdit extends MarketActivity {
    public static final String SAVED_STYLE = "SavedStyle";

    public static final int REQUEST_CODE_EDIT_STYLE = 23498; // whatever

    private long metricID;

    private GraphStyle style;
    public GraphStyle getStyle () { return style; }


    private GraphStyleSettings settingsFragment = null;


    public void onCreate (Bundle state) {
        Database.init(this);

        metricID = getIntent().getLongExtra(Metric.ID, 0);
        style = new GraphStyle();

        if (state != null)
            style.setValues( state.getBundle(SAVED_STYLE) );

        else {
            Bundle styleBundle = getIntent().getBundleExtra(GraphStyle.BUNDLE);
            if (styleBundle != null)
                style.setValues(styleBundle);

            // massive error? I don't even know how this would happen, but lets be graceful if we can
            else {
                setResult( RESULT_CANCELED );
                finish();
                return;
            }
        }

        // once style is initialized, we pop through to super
        super.onCreate(state);

        setContentView(R.layout.graph_style_edit);

        // prep the content
        if (state == null) {
            settingsFragment = new GraphStyleSettings();
            getFragmentManager().beginTransaction()
                                .add(R.id.socket, settingsFragment, GraphStyleSettings.TAG)
                                .commit();
        }
        else {
            settingsFragment = (GraphStyleSettings) getFragmentManager().findFragmentByTag(GraphStyleSettings.TAG);
        }
    }



    public void onResume () {
        super.onResume();
        invalidateOptionsMenu(); // if we come back from somewhere else, update the action bar
    }


    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.graphsettings_actionbar, menu);

        MenuItem paste = menu.findItem(R.id.paste);
        boolean enabled = getCopiedStyle() != null;

        paste.setEnabled(enabled);
        paste.getIcon().setAlpha( enabled ? 255 : 127 );

        return true;
    }



    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()) {

            case R.id.copy:
                ClipData clip = ClipData.newPlainText( getString(R.string.copy_style), style.toJSON() );
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, R.string.style_copied_to_clipboard, Toast.LENGTH_LONG).show();
                return true;

            case R.id.paste:
                ConfirmStyleOverwrite confirm = new ConfirmStyleOverwrite();
                confirm.show(getFragmentManager(), null);  
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public String getCopiedStyle () {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (!clipboard.hasPrimaryClip())
            return null;

        if (!clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
            return null;

        ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
        if (item == null)
            return null;

        String text = "" + item.getText();
        if (text == null || !GraphStyle.validateJSON(text))
            return null;

        return text;
    }

    protected void applyCopiedStyle () {
        String copiedStyle = getCopiedStyle();
        if (copiedStyle != null) {
            style.applyJSON(copiedStyle);
            settingsFragment.updatePrefValues();
        }
    }

    public static class ConfirmStyleOverwrite extends DialogFragment {
        private GraphStyleEdit owner;

        public ConfirmStyleOverwrite () { super(); }

        public void onAttach (Activity activity) {
            super.onAttach(activity);
            try { owner = (GraphStyleEdit) activity; }
            catch (ClassCastException e) {}
        }
        public void onDismiss (DialogInterface dialog) {
            super.onDismiss(dialog);
            owner = null;
        }

        public Dialog onCreateDialog (Bundle state) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setIconAttribute( R.attr.icon_warning );
            builder.setTitle( R.string.paste_style );
            builder.setMessage( R.string.paste_style_confirm );
            
            builder.setNegativeButton(R.string.cancel, null);

            builder.setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener() {
                public void onClick (DialogInterface dialog, int id) {
                    owner.applyCopiedStyle();
                }
            });

            return builder.create();
        }
    }



    public void onSaveInstanceState (Bundle saveState) {
        super.onSaveInstanceState(saveState);
        saveState.putBundle( SAVED_STYLE, style.bundle() );
    }


    public void onBackPressed () {
        Intent result = new Intent().putExtra( GraphStyle.BUNDLE, style.bundle() );
        setResult( RESULT_OK, result );
        finish();
        super.onBackPressed();
    }


    public void onStart () {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    public void onStop () {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
        if (metricID != 0)
            GraphWidgetProvider.updateMetric( this, metricID );
    }



}

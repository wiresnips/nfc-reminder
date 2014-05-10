package com.novorobo.tracker.app;

import com.novorobo.util.database.Database;
import com.novorobo.tracker.metric.Metric;

import com.novorobo.tracker.graph.Graph;
import com.novorobo.tracker.graph.BarGraph;

import java.util.List;

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

import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.graphics.Bitmap;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import com.novorobo.market.MarketActivity;
import com.google.analytics.tracking.android.EasyTracker;

public class PreviewGraph extends MarketActivity {

    public static final int MARGIN = 16;

    private Metric metric;
    ImageView image;    

    public void onCreate (Bundle state) {
        Database.init(this);
        super.onCreate(state);

        Intent intent = getIntent();
        Bundle metricBundle = intent.getBundleExtra(Metric.BUNDLE);

        if (metricBundle == null) {
            finish();
            return;
        }

        metric = new Metric();
        metric.setValues(metricBundle);

        setTitle( getString(R.string.preview) + " " + metric.name );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);

        setContentView(R.layout.graph_preview);
        image = (ImageView) findViewById(R.id.preview);


        // if this metric already has a widget, we want to match it's dimensions 
        // (if it has more than one, largest Y wins)
        List<Integer> widgetIDs = GraphWidgetProvider.getMetricWidgetIDs(this, metric.getID());
        SharedPreferences widgetMap = getSharedPreferences(GraphWidgetProvider.WIDGET_METRIC_MAP, 0);
        int sizeX = 0, sizeY = 0;

        for (int widgetID : widgetIDs) {
            int widgetSizeX = widgetMap.getInt("" + widgetID + GraphWidgetProvider.STASH_SUFFIX_XDIM, 0);
            int widgetSizeY = widgetMap.getInt("" + widgetID + GraphWidgetProvider.STASH_SUFFIX_YDIM, 0);

            if (widgetSizeY > sizeY || (widgetSizeY == sizeY && widgetSizeX > sizeX)) {
                sizeX = widgetSizeX;
                sizeY = widgetSizeY;
            }
        }

        if (sizeX > 0 && sizeY > 0) {
            int margin = (int) (MARGIN * getResources().getDisplayMetrics().density);

            RelativeLayout.LayoutParams layout = (RelativeLayout.LayoutParams) image.getLayoutParams();
            layout.setMargins(0,0,0,0);
            layout.width = sizeX + margin;
            layout.height = sizeY + margin;
            image.setLayoutParams(layout);
        }
    }

    public void onWindowFocusChanged (boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus)
            return;

        Graph graph = Graph.buildGraph(metric, this);
        graph.usingExternalBackground = true;

        int margin = (int) (MARGIN * getResources().getDisplayMetrics().density);
        int sizeX = image.getWidth() - margin;
        int sizeY = image.getHeight() - margin;

        Bitmap preview = graph.render( sizeX, sizeY );

        image.setImageBitmap( preview );
        image.setBackgroundColor( graph.style.backgroundColor );
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

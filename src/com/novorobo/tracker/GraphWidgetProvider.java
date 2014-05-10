package com.novorobo.tracker.app;

import com.novorobo.constants.*;
import com.novorobo.util.database.Database;
import com.novorobo.tracker.metric.Metric;
import com.novorobo.tracker.datapoint.Datapoint;

import com.novorobo.tracker.graph.Graph;
import com.novorobo.tracker.graph.BarGraph;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.net.Uri;

import android.util.Log;
import android.util.TypedValue;
import android.util.DisplayMetrics;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Configuration;

import android.content.ComponentName;
import android.content.Intent;

import android.app.AlarmManager;
import android.app.PendingIntent;

import android.view.View;
import android.widget.RemoteViews;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;

import android.os.Vibrator;
import android.os.AsyncTask;



public class GraphWidgetProvider extends AppWidgetProvider {
    public static final String WIDGET_METRIC_MAP = "com.novorobo.tracker.app.WidgetInfo";
    public static final String ADVANCE_GRAPH_ACTION = "com.novorobo.tracker.app.ADVANCE_GRAPH";

    public static final String STASH_SUFFIX_PORTRAIT  = "_port.png";
    public static final String STASH_SUFFIX_LANDSCAPE = "_land.png";

    public static final String STASH_SUFFIX_XDIM = "_XDIM";
    public static final String STASH_SUFFIX_YDIM = "_YDIM";

    public static final String WIDGET_ID = "WidgetID";


    public void onUpdate (Context context, AppWidgetManager appWidgetManager, int[] widgetIDs) {
        //Log.v( "gotcha", "GraphWidgetProvider::onUpdate" );
        for (int widgetID : widgetIDs)
            updateWidgetThreaded(context, appWidgetManager, widgetID);            
    }

    // resize? REDRAW!
    public void onAppWidgetOptionsChanged (Context context, AppWidgetManager appWidgetManager, int widgetID, Bundle newOptions) {
        //Log.v( "gotcha", "GraphWidgetProvider::onAppWidgetOptionsChanged" );
        updateWidgetThreaded(context, appWidgetManager, widgetID);
    }


    public static void updateWidgetThreaded (final Context context, final AppWidgetManager appWidgetManager, final int widgetID) {
        Log.v( "gotcha", "GraphWidgetProvider::updateWidgetThreaded" );

        Database.init(context);

        // get this widget's metric
        SharedPreferences widgetMap = context.getSharedPreferences(WIDGET_METRIC_MAP, 0);
        SharedPreferences.Editor widgetMapEditor = widgetMap.edit();

        long metricID = widgetMap.getLong( "" + widgetID, 0 );
        Metric metric = (metricID == 0) ? null : Database.get(Metric.class, metricID);

        if (metric == null) {
            widgetMapEditor.remove(""+widgetID).apply(); // if there's no metric, take it out of the listings
            showError(context, appWidgetManager, widgetID);
            return;
        }

        Bundle options = appWidgetManager.getAppWidgetOptions(widgetID);
        float scale = context.getResources().getDisplayMetrics().density;
        int portSizeX = (int) (options.getInt( AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH )  * scale);
        int portSizeY = (int) (options.getInt( AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT ) * scale);
        int landSizeX = (int) (options.getInt( AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH )  * scale);
        int landSizeY = (int) (options.getInt( AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT ) * scale);

        if (portSizeX == 0 || portSizeY == 0 || landSizeX == 0 || landSizeY == 0)
            return;

        // keep track of the size- we'll use it in PreviewGraph
        widgetMapEditor.putInt( "" + widgetID + STASH_SUFFIX_XDIM, portSizeX );
        widgetMapEditor.putInt( "" + widgetID + STASH_SUFFIX_YDIM, portSizeY );
        widgetMapEditor.apply();

        Graph graph = Graph.buildGraph(metric, context);
        graph.usingExternalBackground = true;

        // this step MUST happen in the UI thread
        final Bitmap portrait = graph.render(portSizeX, portSizeY);
        final Bitmap landscape = graph.render(landSizeX, landSizeY);

        if (portrait == null || landscape == null)
            return;

        final RemoteViews showGraph = new RemoteViews(context.getPackageName(), R.layout.widget);
        showGraph.setViewVisibility(R.id.graph_port, View.VISIBLE);
        showGraph.setViewVisibility(R.id.graph_land, View.VISIBLE);

        showGraph.setViewVisibility(R.id.loading, View.GONE);
        showGraph.setViewVisibility(R.id.metric_not_found, View.GONE);
        showGraph.setViewVisibility(R.id.prompt_add_data, graph.isEmpty() ? View.VISIBLE : View.GONE);

        showGraph.setInt(R.id.frame, "setBackgroundColor", graph.style.backgroundColor);

        // click to launch a data-entry dialog
        Intent dataEntry = new Intent(context, ShowWidgetDialog.class);
        dataEntry.putExtra(Metric.ID, metric.getID());
        dataEntry.putExtra(WIDGET_ID, widgetID);
        dataEntry.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, widgetID, dataEntry, PendingIntent.FLAG_UPDATE_CURRENT);

        showGraph.setOnClickPendingIntent(R.id.graph_port, pendingIntent);
        showGraph.setOnClickPendingIntent(R.id.graph_land, pendingIntent);


        AsyncTask<Void, Void, Boolean> render = new AsyncTask<Void, Void, Boolean> () {

            protected Boolean doInBackground(Void... params) {
                // this is the really slow bit
                Uri portURI = stashBitmap(context, portrait, "" + widgetID + STASH_SUFFIX_PORTRAIT);
                Uri landURI = stashBitmap(context, landscape, "" + widgetID + STASH_SUFFIX_LANDSCAPE);

                if (portURI == null || landURI == null)
                    return false;

                // wipe the URI first to ensure redraw
                showGraph.setImageViewUri(R.id.graph_port, Uri.parse(""));
                showGraph.setImageViewUri(R.id.graph_land, Uri.parse(""));

                showGraph.setImageViewUri(R.id.graph_port, portURI );
                showGraph.setImageViewUri(R.id.graph_land, landURI );

                return true;
            }

            protected void onPostExecute (Boolean success) {
                if (success)
                    appWidgetManager.updateAppWidget( widgetID, showGraph );
            }
        };

        render.execute();
    }

    private static Uri stashBitmap (Context context, Bitmap bitmap, String filename) {
        File file = new File(context.getCacheDir(), filename);

        double scale = context.getResources().getDisplayMetrics().density;

        // If I'd be DOWNSCALING, just don't bother.
        if (scale < 1) scale = 1; 

        // if we're an integer scale, we don't need a smoothed scaling
        boolean smoothRender = scale != Math.floor(scale);

        // apply the scaling factor to counter the auto-scaling in the Drawable at the other end
        int scaledSizeX = (int) (bitmap.getWidth() * scale);
        int scaledSizeY = (int) (bitmap.getHeight() * scale);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledSizeX, scaledSizeY, smoothRender );

        FileOutputStream output = null;

        try {
            output = new FileOutputStream(file);
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            output.flush();
        }
        catch (FileNotFoundException e) { Log.e("gotcha","FILE NOT FOUND EXCEPTION"); return null; } 
        catch (IOException e) { Log.e("gotcha","I/O EXCEPTION"); return null; }
        finally {
            try {
                if (output != null) output.close();
            } 
            catch (IOException e) { Log.e("gotcha","I/O EXCEPTION 2"); return null; }
        }

        file.setReadable(true, false);

        return Uri.fromFile(file);
    }







    private static void showError (Context context, AppWidgetManager appWidgetManager, int widgetID) {
        RemoteViews showError = new RemoteViews(context.getPackageName(), R.layout.widget);

        showError.setViewVisibility(R.id.loading, View.GONE);
        showError.setViewVisibility(R.id.graph_port, View.GONE);
        showError.setViewVisibility(R.id.graph_land, View.GONE);
        showError.setViewVisibility(R.id.prompt_add_data, View.GONE);
        showError.setViewVisibility(R.id.metric_not_found, View.VISIBLE);

        // clicking the widget should relaunch the configuration dialog
        // this isn't ideal, 'cause it stomps an existing MetricList, but it'll FUCKING DO
        Intent config = new Intent(context, SetWidgetMetric.class);
        config.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        config.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        config.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        config.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        config.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(context, widgetID, config, PendingIntent.FLAG_UPDATE_CURRENT);
        showError.setOnClickPendingIntent(R.id.metric_not_found, pendingIntent);
        appWidgetManager.updateAppWidget( widgetID, showError );
    }








    public static void updateMetric (Context context, AppWidgetManager appWidgetManager, long metricID) {
        SharedPreferences widgetMap = context.getSharedPreferences(WIDGET_METRIC_MAP, 0);

        ComponentName name = new ComponentName(context, GraphWidgetProvider.class);
        int[] widgetIDs = appWidgetManager.getAppWidgetIds(name);

        for (int widgetID : widgetIDs) {
            long widgetMetricID = widgetMap.getLong(""+widgetID, 0);
            if (widgetMetricID == metricID)
                updateWidgetThreaded(context, appWidgetManager, widgetID);
        }    
    }




    // strip out metric-mapping on widget deletion
    public void onDeleted (Context context, int[] widgetIDs) {
        SharedPreferences widgetMap = context.getSharedPreferences(WIDGET_METRIC_MAP, 0);
        SharedPreferences.Editor mapEditor = widgetMap.edit();

        for (int widgetID : widgetIDs)
            mapEditor.remove( "" + widgetID );

        mapEditor.apply();
    }




    public static void updateMetric (Context context, long metricID) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        updateMetric(context, appWidgetManager, metricID);
    }

    public static void updateWidget (Context context, int widgetID) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        updateWidgetThreaded(context, appWidgetManager, widgetID);
    }




    public static List<Integer> getMetricWidgetIDs (Context context, long metricID) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName name = new ComponentName(context, GraphWidgetProvider.class);
        
        int[] widgetIDs = appWidgetManager.getAppWidgetIds(name);
        ArrayList<Integer> metricWidgetIDs = new ArrayList<Integer>();

        SharedPreferences widgetMap = context.getSharedPreferences(WIDGET_METRIC_MAP, 0);

        for (int widgetID : widgetIDs) {
            long widgetMetricID = widgetMap.getLong( "" + widgetID, 0 );
            if (widgetMetricID == metricID)
                metricWidgetIDs.add(widgetID);
        }

        return metricWidgetIDs;
    }





    public void onEnabled (Context context) {
        super.onEnabled(context);
        Log.v( "gotcha", "onEnabled: setting up AlarmManager" );

        Calendar nextHour = Calendar.getInstance();
        nextHour.add(Calendar.HOUR_OF_DAY, 1);
        long nextHourMillis = nextHour.getTimeInMillis();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC, nextHourMillis, MILLI_PER.HOUR, createAdvanceGraphIntent(context));


        
        //AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        //alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + 10000, MILLI_PER.MINUTE * 3, createAdvanceGraphIntent(context));
    }

    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.v("gotcha", "onDisabled: tearing down AlarmManager" );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(createAdvanceGraphIntent(context));
    }

    private PendingIntent createAdvanceGraphIntent (Context context) {
        Intent intent = new Intent(ADVANCE_GRAPH_ACTION);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void onReceive (Context context, Intent intent) {
        //Log.v( "gotcha", "onReceive: " + intent.getAction() );

        if (ADVANCE_GRAPH_ACTION.equals(intent.getAction())) {

            // WHEN IS UPDATE?
            //String datetime = DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()));
            //Log.v("gotcha", "AlarmManager refresh: " + datetime );
            //((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(300);

            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), getClass().getName());
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, appWidgetManager, ids);
        }

        else
            super.onReceive(context, intent);
    }


}
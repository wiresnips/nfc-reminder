package com.novorobo.tracker.app;

import com.novorobo.constants.*;
import com.novorobo.tracker.datapoint.Datapoint;

import com.novorobo.util.database.Database;
import android.provider.BaseColumns;

import java.util.List;

import android.app.Activity;
import android.util.Log;
import android.net.Uri;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;



public class AppRater {

    public static final String VISITED_MARKET_TO_RATE = "visitedMarketToRate";

    private static final String PACKAGE = "com.novorobo.tracker.app";
    private static final String MARKET_ADDRESS = "market://details?id=%1$s";

    public static Intent getMarketIntent () {
        Uri appMarketAddress = Uri.parse( String.format(MARKET_ADDRESS, PACKAGE) );
        return new Intent( Intent.ACTION_VIEW, appMarketAddress );
    }

    public static boolean marketExists (Context context, Intent intent) {
        List<ResolveInfo> markets = context.getPackageManager().queryIntentActivities( intent, 0 );
        return markets != null && !markets.isEmpty();
    }
    public static boolean marketExists (Context context) {
        return marketExists(context, getMarketIntent());
    }

    public static void launchMarketIntent (Activity activity) {
        Intent market = getMarketIntent();

        if (!marketExists(activity, market))
            return;

        activity.startActivityForResult( market, PromptDialog.RATE_REVIEW_APP_REQUEST_CODE );

        SharedPreferences.Editor editor = activity.getSharedPreferences(MetricList.SHARED_PREFS, 0).edit();
        editor.putBoolean(VISITED_MARKET_TO_RATE, true);
        editor.commit();
    }





    
    public static final String LAST_PROMPT = "lastPrompt";
    public static final String PROMPTS_ISSUED = "promptsIssued";
    public static final String LAST_DATA_COUNT = "lastDataCount";

    private static final int[] INTERVAL_DAYS = new int[] { 3, 6, 9, 12, 15 }; // simpler than procedural
    private static final int DATA_INTERVAL = 8; // datapoints are simpler again. Fixed interval.

    public static boolean canShowPrompt (Context context) {
        //if (true) return true; // can has debug prompt?

        SharedPreferences records = context.getSharedPreferences(MetricList.SHARED_PREFS, 0);
        long now = System.currentTimeMillis();

        // if there are no recorded prompts, record one now to start the interval rolling, then leave
        long lastPrompt = records.getLong(LAST_PROMPT, now);
        if (lastPrompt == now) {
            SharedPreferences.Editor editor = records.edit();
            editor.putLong(LAST_PROMPT, now);
            editor.commit();
            return false;
        }

        // how many prompts have we issued up until now? If it's all of them, we're done
        int prompt = records.getInt(PROMPTS_ISSUED, 0);
        if (prompt >= INTERVAL_DAYS.length)
            return false;

        // if not enough days have passed since the last prompt (or the first call of this func), we duck out
        long timeSinceLast = now - lastPrompt;
        long timeUntilNext = MILLI_PER.DAY * INTERVAL_DAYS[ prompt ];

        if (timeSinceLast < timeUntilNext)
            return false;
        
        // if we haven't submitted enough datapoints since the last prompt, wait a little longer
        int dataCount = Database.count(Datapoint.class);
        int lastDataCount = records.getInt(LAST_DATA_COUNT, dataCount);

        if (dataCount - lastDataCount < DATA_INTERVAL)
            return false;

        // if there's no market installed, we can't do anything anyways
        if (!marketExists(context))
            return false;

        // if we haven't got any internet connectivity, we can't do anything anyways
        if (!isConnected(context))
            return false;
        
        return true;
    }

    public static void recordPrompt (Context context) {
        SharedPreferences records = context.getSharedPreferences(MetricList.SHARED_PREFS, 0);
        SharedPreferences.Editor editor = records.edit();

        int promptsIssued = records.getInt(PROMPTS_ISSUED, 0) + 1;
        editor.putInt( PROMPTS_ISSUED, promptsIssued );
        editor.putLong( LAST_PROMPT, System.currentTimeMillis() );
        editor.putInt( LAST_DATA_COUNT, Database.count(Datapoint.class) );

        editor.commit();
    }

    private static boolean isConnected (Context context) {
        ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

}
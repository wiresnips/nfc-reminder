package com.novorobo.tracker.app;

import com.novorobo.util.database.Database;
import android.provider.BaseColumns;

import com.novorobo.tracker.metric.Metric;
import com.novorobo.tracker.datapoint.Datapoint;

import com.novorobo.market.MarketActivity;
import com.novorobo.market.MarketInterface.InitListener;

import java.util.List;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.net.Uri;

import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

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
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.DialogFragment;

import com.google.android.gms.ads.*;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;

public class About extends MarketActivity {
    public static String USE_ANALYTICS = "collect_analytics";


    protected AboutListFragment fragment;

    public void onCreate (Bundle state) {
        super.onCreate(state);

        if (state == null) {
            fragment = new AboutListFragment();
            getFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment, AboutListFragment.TAG)
                .commit();
        }
        else {
            fragment = (AboutListFragment) getFragmentManager().findFragmentByTag(AboutListFragment.TAG);
        }
    }


    public void onResume () {
        super.onResume();

        addMarketInitListener(new InitListener () {
            public void onInit () {
                if (fragment != null)
                    fragment.setPremium(isUserPremium());
            }
        });
    }


    public void onStart () {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }


    public static class AboutListFragment extends PreferenceFragment {
        public static final String TAG = "AboutListFragment";

        public static final String VERSION = "about_version";
        public static final String CONTACT = "about_contact";
        public static final String UPGRADE = "about_upgrade_premium";
        public static final String RATE    = "about_rate_review";
        public static final String PRIVACY = "about_privacy_policy";
        public static final String APACHE  = "license_apache";
        public static final String MIT     = "license_mit";
        public static final String CHART   = "license_achartengine";
        public static final String COLOR   = "license_holocolorpicker";
        public static final String SLIDER  = "license_sliderpreference";


        public void setPremium (boolean premium) {
            final PreferenceGroup root = getPreferenceScreen();
            CheckBoxPreference upgrade = (CheckBoxPreference) root.findPreference(UPGRADE);
            
            upgrade.setChecked(premium);
            upgrade.setEnabled(!premium);

            if (premium)
                upgrade.setSummary(R.string.about_upgrade_premium_summary_thanks);
        }


        public void onCreate (Bundle state) {
            super.onCreate(state);

            addPreferencesFromResource(R.xml.about);
            final PreferenceGroup root = getPreferenceScreen();

            final String version = getVersion(getActivity());
            root.findPreference(VERSION).setSummary( version );

            root.findPreference(CONTACT).setOnPreferenceClickListener( 
                new OnPreferenceClickListener () {
                    public boolean onPreferenceClick (Preference pref) {
                        Intent send = new Intent(Intent.ACTION_SENDTO);
                        String header = 
                                "mailto:" + Uri.encode( getString(R.string.support_email) ) + 
                                "?subject=" + Uri.encode( getString(R.string.support_prefix) + version ) ;
                        
                        send.setData(Uri.parse(header));
                        startActivity( Intent.createChooser(send, getString(R.string.email_picker_title)) );
                        return true;
                    }
                });

            root.findPreference(UPGRADE).setOnPreferenceClickListener(
                new OnPreferenceClickListener () {
                    public boolean onPreferenceClick (Preference pref) {
                        try {
                            MarketActivity activity = (MarketActivity) getActivity();
                            activity.startIntentSenderForResult(
                                activity.getUpgradeIntent().getIntentSender(), 
                                PromptDialog.PURCHASE_PREMIUM_REQUEST_CODE, new Intent(), 
                                Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0) );
                        } 
                        catch (SendIntentException e) {}  
                        return true;
                    }
                });

            root.findPreference(RATE).setOnPreferenceClickListener(
                new OnPreferenceClickListener () {
                    public boolean onPreferenceClick (Preference pref) {
                        AppRater.launchMarketIntent(getActivity());
                        return true;
                    }
                });

            root.findPreference(PRIVACY).setOnPreferenceClickListener(
                new OnPreferenceClickListener () {
                    public boolean onPreferenceClick (Preference pref) {
                        SimpleDialog dialog = new SimpleDialog(R.string.about_privacy_policy, R.string.privacy_policy);
                        dialog.show(getActivity().getFragmentManager(), null);
                        return true;
                    }
                });

            root.findPreference(APACHE).setOnPreferenceClickListener(
                new OnPreferenceClickListener () {
                    public boolean onPreferenceClick (Preference pref) {
                        SimpleDialog dialog = new SimpleDialog(R.string.license_apache_title, R.string.license_apache_full);
                        dialog.show(getActivity().getFragmentManager(), null);
                        return true;
                    }
                });

            root.findPreference(MIT).setOnPreferenceClickListener(
                new OnPreferenceClickListener () {
                    public boolean onPreferenceClick (Preference pref) {
                        SimpleDialog dialog = new SimpleDialog(R.string.license_mit_title, R.string.license_mit_full);
                        dialog.show(getActivity().getFragmentManager(), null);
                        return true;
                    }
                });


            root.findPreference(CHART).setOnPreferenceClickListener(
                new OnPreferenceClickListener () {
                    public boolean onPreferenceClick (Preference pref) {
                        Uri link = Uri.parse(getString(R.string.license_achartengine_url));
                        Intent browser = new Intent(Intent.ACTION_VIEW, link);
                        startActivity(browser);
                        return true;
                    }
                });

            root.findPreference(COLOR).setOnPreferenceClickListener(
                new OnPreferenceClickListener () {
                    public boolean onPreferenceClick (Preference pref) {
                        Uri link = Uri.parse(getString(R.string.license_holocolorpicker_url));
                        Intent browser = new Intent(Intent.ACTION_VIEW, link);
                        startActivity(browser);
                        return true;
                    }
                });

            root.findPreference(SLIDER).setOnPreferenceClickListener(
                new OnPreferenceClickListener () {
                    public boolean onPreferenceClick (Preference pref) {
                        Uri link = Uri.parse(getString(R.string.license_sliderpreference_url));
                        Intent browser = new Intent(Intent.ACTION_VIEW, link);
                        startActivity(browser);
                        return true;
                    }
                });
        }
    }


    public static class SimpleDialog extends DialogFragment {
        private int title;
        private int body;

        private static String TITLE = "LicenseTitle";
        private static String BODY = "LicenseBody";

        public SimpleDialog (int title, int body) { 
            super(); 
            this.title = title;
            this.body = body;
        }

        public SimpleDialog () { super(); }

        public void onSaveInstanceState (Bundle state) {
            super.onSaveInstanceState(state);
            state.putInt(TITLE, title);
            state.putInt(BODY, body);
        }

        public Dialog onCreateDialog (Bundle state) {
            if (state != null) {
                title = state.getInt(TITLE);
                body = state.getInt(BODY);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle( title );
            builder.setMessage( body );
            builder.setNegativeButton(R.string.back, null);
            return builder.create();
        }
    }


    public static String getVersion (Activity activity) {
        try {
            PackageManager manager = activity.getPackageManager();
            PackageInfo info = manager.getPackageInfo(activity.getPackageName(), 0);
            return info.versionName + " - " + info.versionCode;
        }
        catch (NameNotFoundException e) {
            return "UNKOWN";
        }
    }

}


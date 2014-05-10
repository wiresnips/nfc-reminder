package com.novorobo.tracker.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import android.view.Menu;
import android.view.MenuItem;

import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceClickListener;

import com.novorobo.market.MarketActivity;
import com.google.analytics.tracking.android.EasyTracker;

public class HelpIndex extends MarketActivity {


	public void onCreate (Bundle state) {
	    super.onCreate(state);

        setContentView(R.layout.help_index);

        if (state == null) {
            HelpFragment fragment = new HelpFragment();
            getFragmentManager().beginTransaction()
                .add(R.id.socket, fragment, HelpFragment.TAG)
                .commit();
        }
	}


    public void onStart () {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }


	public static class HelpFragment extends PreferenceFragment {
        public static final String TAG = "HelpFragmentTag";
	    public static final String CONTACT = "contact";

	    public void onCreate (Bundle state) {
        	super.onCreate(state);
        	addPreferencesFromResource(R.xml.help_index);
            
            PreferenceGroup root = getPreferenceScreen();

        	Preference contact = root.findPreference(CONTACT);
        	contact.setOnPreferenceClickListener( new OnPreferenceClickListener () {
        		public boolean onPreferenceClick (Preference pref) {
                    Intent send = new Intent(Intent.ACTION_SENDTO);

                    String version = About.getVersion(getActivity());
                    String header = 
                            "mailto:" + Uri.encode( getString(R.string.support_email) ) + 
                            "?subject=" + Uri.encode( getString(R.string.support_prefix) + version ) ;
                    
                    send.setData(Uri.parse(header));
                    startActivity( Intent.createChooser(send, getString(R.string.email_picker_title)) );
                    return true;
        		}
        	});           
        }
	}





}
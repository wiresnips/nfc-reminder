package com.novorobo.market;

import com.novorobo.util.database.Database;
import android.provider.BaseColumns;

import com.novorobo.tracker.metric.Metric;
import com.novorobo.tracker.datapoint.Datapoint;

import com.novorobo.market.MarketInterface;
import com.novorobo.market.MarketInterface.InitListener;

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

import com.novorobo.tracker.app.R;

public class MarketActivity extends Activity {

    private MarketInterface market = null;
    private PendingIntent upgradeIntent = null;
    private boolean premium = true;

    private AdView banner = null;

    public void onCreate (Bundle state) {
        super.onCreate(state);

        market = new MarketInterface(this);
        market.addInitListener( new InitListener () {
            public void onInit () {
                if (market.ownsPremium())
                    return;

                upgradeIntent = market.getBuyPremiumIntent();
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

    public PendingIntent getUpgradeIntent () {
        return upgradeIntent;
    }

    public boolean isUserPremium () {
        return premium;
    }

    public void addMarketInitListener (InitListener listener) {
        market.addInitListener(listener);
    }


    public void onDestroy() {
        super.onDestroy();
        
        if (market != null)
            market.dispose();

        if (banner != null)
            banner.destroy();
    }

    public void onResume () {
        super.onResume();

        if (banner != null)
            banner.resume();

        // if we weren't premium before, double-check that we aren't premium now
        if (!isUserPremium()) {
            market.addInitListener( new InitListener () {
                public void onInit () {
                    market.refreshInventory();
                    premium = market.ownsPremium();
                    if (premium)
                        banner.setVisibility(View.GONE);
                }
            });
        }
    }

    public void onPause () {
        super.onPause();
        
        if (banner != null)
            banner.pause();
    }

}


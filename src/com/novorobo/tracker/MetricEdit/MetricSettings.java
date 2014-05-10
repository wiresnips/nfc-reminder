package com.novorobo.tracker.app;

import com.novorobo.util.database.Database;
import com.novorobo.util.preference.Pref;
import com.novorobo.util.preference.Pref.NumPref;
import com.novorobo.util.preference.Pref.TextPref;
import com.novorobo.util.preference.Pref.ListPref;
import com.novorobo.util.preference.Pref.CheckPref;
import com.novorobo.util.preference.Pref.ColorPref;
import com.novorobo.util.preference.Pref.SliderPref;
import com.novorobo.tracker.metric.Metric;
import com.novorobo.tracker.metric.Metric.AggregatePeriod;
import com.novorobo.tracker.metric.Metric.AggregateFunction;
import com.novorobo.tracker.graph.GraphStyle;
import com.novorobo.constants.*;

import com.novorobo.market.MarketInterface;
import com.novorobo.market.MarketInterface.InitListener;

import java.util.List;

import android.content.Intent;
import android.app.PendingIntent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.content.IntentSender.SendIntentException;

import android.provider.BaseColumns;

import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.ListPrefDisableable;
import android.preference.NumpadPreference;
import android.preference.SliderPreference;
import android.preference.ColorPickerDialogPreference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceFragment;


import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

public class MetricSettings extends PreferenceFragment {
    public static final String TAG = "MetricSettingsFragment";

    // the keys to my preferences - make sure they match with res/xml/metric_settings.xml
    public static final String NAME = "metric_name";
    public static final String DATA_TYPE = "metric_datatype";
    public static final String AGGREGATE_FUNCTION = "metric_aggregateFunction";
    public static final String AGGREGATE_PERIOD = "metric_aggregatePeriod";
    public static final String DURATION_PICKER_EXT = "metric_durationPickerExt";
    public static final String RANGE_MAX = "metric_rangeMax";
    public static final String AMOUNT_INPUT = "metric_amount_input";

    public static final String GRAPH_SECTION = "metric_settings_section_widget";
    public static final String GRAPH_STYLING = "metric_settings_widget_appearance";
    public static final String UPSELL = "upsell_checkbox";

    
    private Metric metric;
    private GraphStyle style;

    public void onAttach (Activity activity) {
        super.onAttach(activity);
        try { 
            metric = ((MetricEdit) activity).getMetric();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must be MetricEdit");
        }
    }

    public void onSaveInstanceState (Bundle saveState) {
        super.onSaveInstanceState(saveState);
        saveState.putBundle( GraphStyle.BUNDLE, style.bundle() );
    }

    public void onCreate (Bundle state) {
        super.onCreate(state);        

        style = metric.getGraphStyle();
        if (style == null) {
            style = new GraphStyle();
            if (state != null)
                style.setValues(state.getBundle(GraphStyle.BUNDLE));

            // if we haven't got existing state, try to init from the youngest style
            else {
                List<GraphStyle> prevStyle = Database.get(GraphStyle.class, null, null, BaseColumns._ID + " desc", 1 );
                if (prevStyle.size() > 0)
                    style.applyJSON( prevStyle.get(0).toJSON() );
            }
        }
        
        addPreferencesFromResource(R.xml.metric_settings);
        final PreferenceGroup root = getPreferenceScreen();


        Pref name = new TextPref( (EditTextPreference) root.findPreference(NAME) ) {
            public void updatePreferenceView () {
                boolean empty = value == null || ((String)value).isEmpty();

                if (!empty)
                    preference.setSummary( (String) value );
                
                else if (!metric.hasName())
                    preference.setSummary( R.string.metric_name_empty );                    
            }
        };
        name.setValue( metric.name );
        name.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                if (value != null) {
                    String name = ((String) value).trim();
                    if (!name.isEmpty())
                        metric.name = name;
                    propagate();
                }
            }
        });


        Pref datatype = new ListPref( (ListPreference) root.findPreference(DATA_TYPE) );
        datatype.setValue( metric.datatype.name() );
        datatype.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                metric.datatype = Enum.valueOf(Metric.DataType.class, (String) value);
                propagate();
            }
        });


        final Pref aggregateFunction = new ListPref( (ListPreference) root.findPreference(AGGREGATE_FUNCTION) );
        aggregateFunction.setValue( metric.getAggregateFunction().name() );
        aggregateFunction.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                metric.setAggregateFunction( Enum.valueOf(AggregateFunction.class, (String) value) );
                propagate();
            }
        });
        

        final Pref aggregatePeriod = new ListPref( (ListPreference) root.findPreference(AGGREGATE_PERIOD) );
        aggregatePeriod.setValue( metric.getAggregatePeriodNoAuto().name() );
        aggregatePeriod.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                AggregatePeriod period = Enum.valueOf(AggregatePeriod.class, (String) value);
                metric.setAggregatePeriod( period );
                propagate();

                // gotta get fancy if we're going auto
                if (period == AggregatePeriod.AUTO) {
                    ListPreference list = (ListPreference) aggregatePeriod.getPreference();
                    AggregatePeriod effectivePeriod = metric.getAggregatePeriod();
                    int effectiveIndex = list.findIndexOfValue( effectivePeriod.name() );
                    String effectiveSummary = ""+ list.getEntries()[effectiveIndex];
                    list.setSummary( effectiveSummary + " " + getActivity().getString(R.string.period_auto_summary) );
                }
            }
        });
        // whatever. This code isn't going to dupe itself anywhere else
        if (metric.getAggregatePeriodNoAuto() == AggregatePeriod.AUTO) {
            ListPreference list = (ListPreference) aggregatePeriod.getPreference();
            AggregatePeriod effectivePeriod = metric.getAggregatePeriod();
            int effectiveIndex = list.findIndexOfValue( effectivePeriod.name() );
            String effectiveSummary = ""+ list.getEntries()[effectiveIndex];
            list.setSummary( effectiveSummary + " " + getActivity().getString(R.string.period_auto_summary) );
        }


        Preference graphStyleScreen = root.findPreference(GRAPH_STYLING);
        graphStyleScreen.setOnPreferenceClickListener(new OnPreferenceClickListener () {
            public boolean onPreferenceClick (Preference pref) {
                Intent styler = new Intent(getActivity(), GraphStyleEdit.class);
                styler.putExtra( GraphStyle.BUNDLE, style.bundle() );
                styler.putExtra( Metric.ID, metric.getID() );
                getActivity().startActivityForResult( styler, GraphStyleEdit.REQUEST_CODE_EDIT_STYLE );
                return true;
            }
        });
        


        final Pref durationPickerExt = new ListPref( (ListPreference) root.findPreference(DURATION_PICKER_EXT) );
        durationPickerExt.setValue( metric.durationPickerExtension.name() );
        durationPickerExt.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                metric.durationPickerExtension = Enum.valueOf(Metric.DurationPickerExt.class, (String) value);
                propagate();
            }
        });

        new Pref.Dependency (datatype, durationPickerExt) {
            protected boolean getEnabled (Object value) {
                return metric.datatype == Metric.DataType.DURATION;
            }
            protected void onDisable () {
                durationPickerExt.getPreference().setSummary(R.string.metric_durationPickerExt_disabled_explanation);
            }
        };        




        NumpadPreference rangeMaxPreference = (NumpadPreference) root.findPreference(RANGE_MAX);
        final Pref rangeMax = new NumPref (rangeMaxPreference) {
            protected void setPreferenceValue () {
                double max = Math.max(1, (Double) value);
                value = (Double) max;
                super.setPreferenceValue();
            }
            public void updatePreferenceView () {
                int rangeMax = (int) Math.round((Double) value);
                preference.setSummary( getActivity().getString(R.string.metric_rangeMax_summary, rangeMax) );
            }
        };
        rangeMax.setValue( (double) metric.rangeMax );
        rangeMax.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                metric.rangeMax = (int) Math.round((Double) value);
                propagate();
            }
        });

        new Pref.Dependency (datatype, rangeMax) {
            protected boolean getEnabled (Object value) {
                return metric.datatype == Metric.DataType.RANGE;
            }
            protected void onDisable () {
                rangeMax.getPreference().setSummary(R.string.metric_rangeMax_disabled_explanation);
            }
        };






        ////////////////////////////////////////////////////////////////////////
        // Get everything set up so that options disable themselves when they should.
        // This is ugly as hell, but it'll do until I need a more elegant solution.
        // Which hopefully will be never.
        ////////////////////////////////////////////////////////////////////////

        final ListPrefDisableable aggTime = (ListPrefDisableable) aggregatePeriod.getPreference();
        final ListPrefDisableable aggFunc = (ListPrefDisableable) aggregateFunction.getPreference();
        final int index_none = aggTime.findIndexOfValue(AggregatePeriod.NONE.name());
        final int index_avg  = aggFunc.findIndexOfValue(AggregateFunction.AVERAGE.name());
        final int index_max  = aggFunc.findIndexOfValue(AggregateFunction.MAXIMUM.name());
        final int index_min  = aggFunc.findIndexOfValue(AggregateFunction.MINIMUM.name());

        boolean enabled = metric.datatype != Metric.DataType.EVENT && metric.getAggregatePeriod() != AggregatePeriod.NONE;
        aggFunc.setItemEnabled( index_avg, enabled );
        aggFunc.setItemEnabled( index_max, enabled );
        aggFunc.setItemEnabled( index_min, enabled );
        aggTime.setItemEnabled( index_none, metric.datatype != Metric.DataType.EVENT );
        
        datatype.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                aggTime.setItemEnabled( index_none, metric.datatype != Metric.DataType.EVENT );
                aggregatePeriod.setValue( metric.getAggregatePeriodNoAuto().name() );
            }
        });

        aggregatePeriod.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                boolean enabled = metric.datatype != Metric.DataType.EVENT && 
                    metric.getAggregatePeriod() != AggregatePeriod.NONE;
                aggFunc.setItemEnabled( index_avg, enabled );
                aggFunc.setItemEnabled( index_max, enabled );
                aggFunc.setItemEnabled( index_min, enabled );
                aggregateFunction.setValue(metric.getAggregateFunction().name());
            }
        });


    }



    private void propagate () {
        if (metric.hasName()) {

            if (metric.graphStyle_id == 0) {
                Database.put(style);
                metric.graphStyle_id = style.getID();
            }

            reportAnalytics();
            Database.put(metric);
        }
    }


    public void updateStyle (Bundle styleBundle) {
        if (style != null)
            style.setValues(styleBundle);
    }


    private void reportAnalytics () {
        EasyTracker easyTracker = EasyTracker.getInstance(getActivity());

        if (easyTracker == null)
            return;

        easyTracker.send(MapBuilder.createEvent(
            Metric.CATEGORY, 
            metric.getID() == 0 ? ACTION.CREATE : ACTION.UPDATE, 
            metric.datatype.name(), 
            null).build());
    }

}


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

import com.novorobo.market.MarketInterface;
import com.novorobo.market.MarketInterface.InitListener;

import android.content.Intent;
import android.app.PendingIntent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.content.IntentSender.SendIntentException;

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



public class GraphStyleSettings extends PreferenceFragment {
    public static final String TAG = "GraphStyleSettingsFragment";

    public static final String GRAPH_TYPE = "widget_graphtype";    
    public static final String VALUES_MAX = "widget_max_values";
    public static final String BAR_SPACING = "widget_bar_width";    
    public static final String LINE_THICKNESS = "widget_line_width";

    public static final String DATA_COLOR = "widget_data_color";
    public static final String BACKGROUND_COLOR = "widget_background_color";
    public static final String TEXT_COLOR = "widget_text_color";
    public static final String MARGIN_COLOR = "widget_margin_color";

    public static final String TITLE_SIZE = "widget_title_size";
    public static final String TITLE_POSITION = "widget_title_position";
    public static final String LABEL_SIZE = "widget_label_size";
    public static final String LABEL_SHOW = "widget_label_display";
    

    private GraphStyle style;

    public void onAttach (Activity activity) {
        super.onAttach(activity);
        try {
            style = ((GraphStyleEdit) activity).getStyle();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must be GraphStyleEdit");
        }
    }

    // a whole mess of references to preferences, so I can update them later
    Pref graphtype, maxGraphValues, barSpacing, lineThickness, dataColor, backgroundColor, 
         textColor, marginColor, titleSize, titlePos, labelSize, labelShow;

    public void onCreate (Bundle state) {
        super.onCreate(state);        
        final Activity activity = getActivity();
        
        addPreferencesFromResource(R.xml.graphstyle_settings);
        final PreferenceGroup root = getPreferenceScreen();

        graphtype = new ListPref( (ListPreference) root.findPreference(GRAPH_TYPE) );
        barSpacing = new SliderPref( (SliderPreference) root.findPreference(BAR_SPACING) );
        lineThickness = new SliderPref( (SliderPreference) root.findPreference(LINE_THICKNESS) );
        dataColor = new ColorPref( (ColorPickerDialogPreference) root.findPreference(DATA_COLOR) );
        backgroundColor = new ColorPref( (ColorPickerDialogPreference) root.findPreference(BACKGROUND_COLOR) );
        textColor = new ColorPref( (ColorPickerDialogPreference) root.findPreference(TEXT_COLOR) );
        marginColor = new ColorPref( (ColorPickerDialogPreference) root.findPreference(MARGIN_COLOR) );
        titleSize = new SliderPref( (SliderPreference) root.findPreference(TITLE_SIZE) );
        titlePos = new ListPref( (ListPreference) root.findPreference(TITLE_POSITION) );
        labelSize = new SliderPref( (SliderPreference) root.findPreference(LABEL_SIZE) );
        labelShow = new ListPref( (ListPreference) root.findPreference(LABEL_SHOW) );

        NumpadPreference maxGraphValuesPreference = (NumpadPreference) root.findPreference(VALUES_MAX);
        maxGraphValuesPreference.setMax(GraphStyle.MAX_VALUES);

        maxGraphValues = new NumPref (maxGraphValuesPreference) {
            protected void setPreferenceValue () {
                double max = Math.max(GraphStyle.MIN_VALUES, (Double) value);
                value = (Double) max;
                super.setPreferenceValue();
            }
            public void updatePreferenceView () {
                int maxValue = (int) Math.round((Double) value);
                preference.setSummary( getString(R.string.widget_max_values_summary, maxValue) );
            }
        };

        updatePrefValues();


        graphtype.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                style.graphType = Enum.valueOf(GraphStyle.GraphType.class, (String) value); 
                propagate();
            }
        });

        maxGraphValues.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                style.maxValues = (int) Math.round((Double) value);
                propagate();
            }
        });


        barSpacing.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                style.barWidth = 1 - (Float) value;
                propagate();
            }
        });
        new Pref.Dependency (graphtype, barSpacing) {
            protected boolean getEnabled (Object value) {
                return style.graphType == GraphStyle.GraphType.BAR;
            }
        };

        lineThickness.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                style.setNormalizedLineWidth( (Float) value );
                propagate();
            }
        });
        new Pref.Dependency (graphtype, lineThickness) {
            protected boolean getEnabled (Object value) {
                return style.graphType == GraphStyle.GraphType.LINE;
            }
        };

        dataColor.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                style.dataColor = (Integer) value;
                propagate();
            }
        });
        
        backgroundColor.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                style.backgroundColor = (Integer) value;
                propagate();
            }
        });

        textColor.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                style.textColor = (Integer) value;
                propagate();
            }
        });
        
        marginColor.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                Log.v("gotcha", "value: " + (Integer) value );
                style.marginColor = (Integer) value;
                propagate();
            }
        });                

        titleSize.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                style.setNormalizedTitleTextSize( (Float) value );
                propagate();
            }
        });
        
        titlePos.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                style.titlePosition = Enum.valueOf(GraphStyle.TitlePosition.class, (String) value); 
                propagate();
            }
        });
        
        labelSize.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                style.setNormalizedLabelTextSize( (Float) value );
                propagate();
            }
        });

        labelShow.addListener( new Pref.Listener () {
            public void onValueSet (Object value) {
                style.labelShow = Enum.valueOf(GraphStyle.LabelDisplay.class, (String) value); 
                propagate();
            }
        });
        
    }


    private void propagate () {
        if (style.getID() != 0)
            Database.put(style);
    }



    public void updatePrefValues () {
        graphtype.setValue( style.graphType.name() );
        maxGraphValues.setValue( (double) style.maxValues );
        barSpacing.setValue( 1 - style.barWidth );
        lineThickness.setValue( style.getNormalizedLineWidth() );
        dataColor.setValue( style.dataColor );
        backgroundColor.setValue( style.backgroundColor );
        textColor.setValue( style.textColor );
        marginColor.setValue( style.marginColor );
        titleSize.setValue( style.getNormalizedTitleTextSize() );
        titlePos.setValue( style.titlePosition.name() );
        labelSize.setValue( style.getNormalizedLabelTextSize() );
        labelShow.setValue( style.labelShow.name() );
    }

}


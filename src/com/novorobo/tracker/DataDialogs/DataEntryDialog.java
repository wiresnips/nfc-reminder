package com.novorobo.tracker.app;

import com.novorobo.util.database.Database;
import com.novorobo.tracker.metric.*;
import com.novorobo.tracker.datapoint.*;
import com.novorobo.constants.*;

import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.util.Log;
import android.util.TypedValue;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;

import android.appwidget.AppWidgetManager;

import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.view.Window;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.app.DatePickerDialog.OnDateSetListener;

import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.LinearLayout;

import com.google.android.gms.ads.*;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

import com.novorobo.util.ugh.DialogFragmentWithDismissCallback;
 

public abstract class DataEntryDialog extends DialogFragmentWithDismissCallback {

    // InstanceState keys
    private static final String DATE_PICKER_STATE = "DatePickerState";
    private static final String TIME_PICKER_STATE = "TimePickerState";
    private static final String DATAPOINT_STATE = "DatapointState";


    // this mess of stuff is all for the date-picking functionality	
    private View time;
    private View date;

    private TextView timeText;
    private TextView dateText;

    private TimePickerDialog timePicker = null;
    private DatePickerDialog datePicker = null;

    private Bundle datePickerState = null;
    private Bundle timePickerState = null;

    private AdView banner = null;

    protected Datapoint datapoint;


    // this functionality has to be filled in on a per-datatype basis
    protected abstract View getDataEntryViewSlug ();
    protected abstract double getValue ();
    protected abstract void setValue (double value);



    public DataEntryDialog () {
        super();
    }

    public DataEntryDialog (Datapoint target) {
        super();
        datapoint = target;
    }

    public void onCreate (Bundle state) {
        super.onCreate(state);

        // if we're loading from a saveState, we need to pull out our bundle
        if (state != null) {
            datapoint = new Datapoint();
            datapoint.setValues(state.getBundle(DATAPOINT_STATE));
            
            datePickerState = state.getBundle(DATE_PICKER_STATE);
            timePickerState = state.getBundle(TIME_PICKER_STATE);
        }
    }


    public Dialog onCreateDialog (Bundle state) {
        Context context = getActivity();
        LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.data_entry_frame, null);

        LinearLayout listView = (LinearLayout) view.findViewById(R.id.data_entry_layout);
        View dataEntryView = getDataEntryViewSlug();
        if (dataEntryView != null)
            listView.addView(dataEntryView);
        else
            listView.findViewById(R.id.divider).setVisibility(View.GONE);


        // fetch and prep our UI chunks
        banner = (AdView) view.findViewById(R.id.banner);

        date = (LinearLayout) view.findViewById(R.id.data_entry_date);
        time = (LinearLayout) view.findViewById(R.id.data_entry_time);

        date.setOnClickListener( dateClicked );        
        time.setOnClickListener( timeClicked );

        dateText = (TextView) view.findViewById(R.id.date_text);
        timeText = (TextView) view.findViewById(R.id.time_text);

        Date datetime = datapoint.getDate();
        DateFormat dateFmt = android.text.format.DateFormat.getMediumDateFormat(context);
        DateFormat timeFmt = android.text.format.DateFormat.getTimeFormat(context);

        dateText.setText( dateFmt.format(datetime) );
        timeText.setText( timeFmt.format(datetime) );

        // wire up our date/time pickers
        datePicker = new DatePickerDialog( view.getContext(), dateSet, 0, 0, 0 ) {
            public void onDateChanged (DatePicker view, int year, int month, int day) {
                datePicker.setTitle( formatDateLong(year, month, day) );
            }
        };
        datePicker.setOnCancelListener( canceled );

        boolean militaryTime = android.text.format.DateFormat.is24HourFormat(context);
        timePicker = new TimePickerDialog( view.getContext(), timeSet, 0, 0, militaryTime );
        timePicker.setOnCancelListener( canceled );

        final Dialog dialog  = new Dialog(context);

        // if we don't have room, kill the title
        float density = getResources().getDisplayMetrics().density;
        int orientation = getResources().getConfiguration().orientation;

        if (density < 1 || orientation == Configuration.ORIENTATION_LANDSCAPE)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        else
            dialog.setTitle( datapoint.getMetric().name );

        dialog.setContentView( view );

        dialog.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) {
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) {
                datapoint.value = getValue();
                reportAnalytics();
                Database.put(datapoint);

                GraphWidgetProvider.updateMetric( DataEntryDialog.this.getActivity(), datapoint.metric_id );
                dialog.dismiss();
            }
        });

        return dialog;
    }


    public void onResume () {
        super.onResume();
        if (banner != null)
            banner.resume();

        setValue(datapoint.value);

        // if this happens in onCreateDialog, the dialog shows up underneath, which is bad
        if (datePickerState != null) {
            datePicker = new DatePickerDialog( getActivity(), dateSet, 0, 0, 0 );
            datePicker.onRestoreInstanceState(datePickerState);
            datePicker.setOnCancelListener( canceled );
        }

        if (timePickerState != null) {
            timePicker = new TimePickerDialog( getActivity(), timeSet, 0, 0, false);
            timePicker.onRestoreInstanceState(timePickerState);
            timePicker.setOnCancelListener( canceled );
        }
    }

    public void onPause () {
        super.onPause();
        if (banner != null)
            banner.pause();
    }

    public void onStop () {
        super.onStop();

        if (datePicker != null)
            datePicker.dismiss();

        if (timePicker != null)
            timePicker.dismiss(); 
    }

    public void onDestroy() {
        super.onDestroy();
        if (banner != null)
            banner.destroy();
    }


    public void setShowBanner (boolean show) {
        if (banner == null)
            return;

        else if (show) {
            AdRequest request = new AdRequest.Builder()
                        .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)        // Emulator
                        .addTestDevice("181A55C5FAB7738ACB66E81CAD811A43")  // muh PHONE!
                        .build();

            banner.setVisibility( View.VISIBLE );
            banner.loadAd(request);
        }

        else {
            banner.setVisibility( View.GONE );
        }
    }



    public void onSaveInstanceState (Bundle saveState) {
        super.onSaveInstanceState(saveState);

        datapoint.value = getValue();
        saveState.putBundle( DATAPOINT_STATE, datapoint.bundle() );

        if (datePicker != null && datePicker.isShowing())
            saveState.putBundle( DATE_PICKER_STATE, datePicker.onSaveInstanceState() );

        if (timePicker != null && timePicker.isShowing())
            saveState.putBundle( TIME_PICKER_STATE, timePicker.onSaveInstanceState() );    
    }


    private OnClickListener dateClicked = new OnClickListener () {
    	public void onClick (View view) {
            Calendar calendar = datapoint.getCalendar();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DATE);
            
            datePicker.updateDate(year, month, day);
            datePicker.setTitle( formatDateLong(year, month, day) );
            datePicker.show();
        }
    };

    private OnClickListener timeClicked = new OnClickListener () {
        public void onClick (View view) {
            Calendar calendar = datapoint.getCalendar();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            timePicker.updateTime(hour, minute);
            timePicker.show();
        }
    };


    private OnDateSetListener dateSet = new OnDateSetListener() {
        public void onDateSet (DatePicker view, int year, int month, int day) {
            Calendar calendar = datapoint.getCalendar();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DATE, day);

            datapoint.timestamp = calendar.getTimeInMillis() / MILLI_PER.SECOND;

            DateFormat dateFmt = android.text.format.DateFormat.getMediumDateFormat(getActivity());
            dateText.setText( dateFmt.format( calendar.getTime() ) );
        }
    };

    private OnTimeSetListener timeSet = new OnTimeSetListener() {
        public void onTimeSet (TimePicker view, int hour, int minute) {
            Calendar calendar = datapoint.getCalendar();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);

            datapoint.timestamp = calendar.getTimeInMillis() / MILLI_PER.SECOND;
            
            DateFormat timeFmt = android.text.format.DateFormat.getTimeFormat(getActivity());
            timeText.setText( timeFmt.format( calendar.getTime() ) );
        }
    };

    private OnCancelListener canceled = new OnCancelListener () {
        public void onCancel (DialogInterface dialog) {
            datePickerState = null;
            timePickerState = null;
        }
    };



    private String formatDateLong (int year, int month, int day) {

        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DATE, day);

        String format = getActivity().getString(R.string.datepicker_title_format_long);
        SimpleDateFormat dateFmt = new SimpleDateFormat(format);                
        String formattedDate = dateFmt.format(calendar.getTime()) + getOrdinal(day);

        if (year != currentYear)
            formattedDate += ", " + year;
        
        return formattedDate;
    }

    private static String getOrdinal (final int day) {
        if (day >= 11 && day <= 13)
            return "th";

        switch (day % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }
    


    private void reportAnalytics () {
        EasyTracker easyTracker = EasyTracker.getInstance(getActivity());

        if (easyTracker == null)
            return;

        easyTracker.send(MapBuilder.createEvent(
            Datapoint.CATEGORY, 
            datapoint.getID() == 0 ? ACTION.CREATE : ACTION.UPDATE, 
            datapoint.type.name(), null).build());
    }

}

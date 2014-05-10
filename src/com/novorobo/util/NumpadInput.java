package android.widget;

import java.lang.Double;
import java.text.DecimalFormat;

import java.math.BigDecimal;
import java.lang.IllegalStateException;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.LayoutInflater;

import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.InputFilter;


import com.novorobo.tracker.app.R;



public class NumpadInput extends LinearLayout {

    private static final int DEFAULT_MAX_DIGITS = 18;
    private static final float DEFAULT_MAX = Float.MAX_VALUE;
    private static final float DEFAULT_MIN = -Float.MAX_VALUE;

    private static final boolean DEFAULT_NEGATIVE = true;
    private static final boolean DEFAULT_FRACTIONAL = true;

    private TextView readout;
    private Button decimal;

    private boolean allowNegative = DEFAULT_NEGATIVE;
    private boolean allowFractional = DEFAULT_FRACTIONAL;

    private double max = DEFAULT_MAX;
    private double min = DEFAULT_MIN;

    public NumpadInput (Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.numpad, this, true);

        readout = (TextView) findViewById(R.id.readout);

        decimal = (Button) findViewById(R.id.button_decimal);
        decimal.setOnClickListener( insertDecimal );

        View bksp = findViewById(R.id.button_backspace);
        bksp.setOnClickListener( backspace );
        bksp.setOnLongClickListener( clearEverything );        

        Button negator = (Button) findViewById(R.id.button_plus_minus);
        negator.setOnClickListener( invertPolarity );

        findViewById(R.id.button_0).setOnClickListener( numberInput );
        findViewById(R.id.button_1).setOnClickListener( numberInput );
        findViewById(R.id.button_2).setOnClickListener( numberInput );
        findViewById(R.id.button_3).setOnClickListener( numberInput );
        findViewById(R.id.button_4).setOnClickListener( numberInput );
        findViewById(R.id.button_5).setOnClickListener( numberInput );
        findViewById(R.id.button_6).setOnClickListener( numberInput );
        findViewById(R.id.button_7).setOnClickListener( numberInput );
        findViewById(R.id.button_8).setOnClickListener( numberInput );
        findViewById(R.id.button_9).setOnClickListener( numberInput );


        // get attributes specified in XML
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.NumpadPreference, 0, 0);
        try {
            max = a.getFloat(R.styleable.NumpadPreference_maxValue, DEFAULT_MAX);
            min = a.getFloat(R.styleable.NumpadPreference_minValue, DEFAULT_MIN);

            allowNegative = a.getBoolean(R.styleable.NumpadPreference_allowNegative, DEFAULT_NEGATIVE);
            allowFractional = a.getBoolean(R.styleable.NumpadPreference_allowFractional, DEFAULT_FRACTIONAL);
        }
        finally {
            a.recycle();
        }

        if (!allowFractional) {
            decimal.setOnClickListener( null );
            decimal.setEnabled(false);
        }
        if (!allowNegative) {
            negator.setOnClickListener( null );
            negator.setEnabled( false );
        }

        if  (min > max)
            throw new IllegalStateException("Min must be smaller than or equal to Max");
    }


    public double getValue () {
        try {
            return Double.valueOf( "" + readout.getText() );
        } 
        catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    public void setValue (double value) {    
        readout.setText( doubleToString(value) );
        enforceBounds();
    }


    public double getMin () { return min; }
    public void setMin (double min) { this.min = min; }

    public double getMax () { return max; }
    public void setMax (double max) { this.max = max; }



    protected View.OnClickListener numberInput = new View.OnClickListener() {
         public void onClick (View view) {
            Button button = (Button) view;
            
            String text = "" + readout.getText() + button.getText();
            text = stripLeadingZeroes(text);

            readout.setText(text);
            enforceBounds();
         }
     };


     protected OnClickListener insertDecimal = new OnClickListener() {
        public void onClick (View view) {
            String text = "" + readout.getText();

            if (text.contains("."))
                return;
            
            decimal.setEnabled(false);
            readout.setText( text + "." );
        }
     };

     protected OnClickListener invertPolarity = new OnClickListener() {
         public void onClick (View view) {
            String text = "" + readout.getText();
            text = (text.startsWith("-")) ? text.substring(1) : "-" + text;
            text = stripLeadingZeroes(text);
            readout.setText( text );
            enforceBounds();
         }
     };

     protected OnClickListener backspace = new OnClickListener() {
        public void onClick (View view) {
            String text = "" + readout.getText();

            if (!text.isEmpty())
                text = text.substring(0, text.length() - 1);
            text = stripLeadingZeroes(text);

            decimal.setEnabled( !text.contains(".") && allowNegative );
            readout.setText( text );
            enforceBounds();
        }
    };

    protected OnLongClickListener clearEverything = new OnLongClickListener() {
        public boolean onLongClick (View view) {
            decimal.setEnabled( allowNegative );
            readout.setText( "0" );
            enforceBounds();
            return true;
        }
    };


    private String stripLeadingZeroes (String value) {

        // simpler to strip off the minus sign and replace it later
        boolean negative = value.startsWith("-");
        if (negative)
            value = value.substring(1);

        while (value.startsWith("0"))
            value = value.substring(1);

        // put one zero back if we've killed EVERYTHING
        if (value.isEmpty() || value.startsWith("."))
            value = "0" + value;

        if (negative)
            value = "-" + value;

        return value;
    }

    private void enforceBounds () {
        double value = getValue();

        if (value > max)
            readout.setText( doubleToString(max) );

        else if (value < min)
            readout.setText( doubleToString(min) );
    }


    public static String doubleToString (double value) {
        return (value == Math.floor(value)) ? String.format("%d", (int) value) : String.format("%f", value);
    }




    public interface Listener {
        public void onChanged (double value);
    }

    private TextWatcher textWatcher = null;

    public void setListener (final Listener listener) {
        if (textWatcher != null)
            readout.removeTextChangedListener(textWatcher);

        textWatcher = new TextWatcher() {
            public void afterTextChanged (Editable s) { listener.onChanged(getValue()); }
            public void beforeTextChanged (CharSequence s, int start, int count, int after) {}
            public void onTextChanged (CharSequence s, int start, int count, int after) {}
        };

        readout.addTextChangedListener(textWatcher);
    }


}
/*
 * Copyright (C) 2011 Sergey Margaritov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.preference;

import java.lang.Double;
import java.text.DecimalFormat;
import java.math.BigDecimal;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.util.Log;

import android.widget.NumpadInput;

import com.novorobo.tracker.app.R;


public class NumpadPreference extends DialogPreference {

	private double value;
    private NumpadInput numpad;


    public NumpadPreference (Context context, AttributeSet attrs) {
        super(context, attrs);
        
        numpad = new NumpadInput(context, attrs);
        numpad.setId(R.id.numpad); // Give it an ID so it can be saved/restored
        numpad.setListener( new NumpadInput.Listener () {
            public void onChanged (double newValue) {
                value = newValue;
            }
        });

        setDialogLayoutResource(R.layout.numpad_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
    }


    public double getValue() {
        double min = numpad.getMin();
        double max = numpad.getMax();
    	return value < min ? min : value > max ? max : value;
    }


    public void setValue (double value) {
        if (shouldPersist())
            persistLong(d2l(value));

        if (value != this.value) {
            this.value = value;
            notifyChanged();
        }

        setSummary( NumpadInput.doubleToString(value) );
    }


    public double getMin () { return numpad.getMin(); }
    public void setMin (double min) { numpad.setMin(min); }

    public double getMax () { return numpad.getMax(); }
    public void setMax (double max) { numpad.setMax(max); }



    @Override
    protected void onBindDialogView (View view) {
        super.onBindDialogView(view);

        numpad.setValue(value);
        ViewParent oldParent = numpad.getParent();

        if (oldParent != view) {
            if (oldParent != null)
                ((ViewGroup) oldParent).removeView(numpad);
            
            ViewGroup container = (ViewGroup) view.findViewById(R.id.container);
            if (container != null)
                container.addView(numpad, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }


    @Override
    protected void onDialogClosed (boolean positiveResult) {
        if (positiveResult && callChangeListener(value))
            setValue(value);

        super.onDialogClosed(positiveResult);
    }
 


    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState myState = new SavedState(superState);
        myState.value = value;
        return myState;
    }
 
    protected void onRestoreInstanceState (Parcelable state) {
        // didn't save the state, so call superclass
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }
 
        // restore the state
        SavedState myState = (SavedState) state;
    	value = myState.value;
        super.onRestoreInstanceState(myState.getSuperState());
    }



	private static class SavedState extends BaseSavedState {
        public double value;
 
        public SavedState (Parcelable superState) { super(superState); }
        public SavedState (Parcel source) {
            super(source);
            value = source.readDouble();
        }
 
        @Override
        public void writeToParcel (Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeDouble(value);
        }
 
        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState> () {
            public SavedState createFromParcel(Parcel in) { return new SavedState(in); }
            public SavedState[] newArray(int size) { return new SavedState[size]; }
        };
    }



    @Override
    protected void onSetInitialValue (boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? l2d(getPersistedLong(d2l(value))) : (Double) defaultValue);
    }


    private static long d2l (double d) {
        return Double.doubleToRawLongBits(d);
    }
    private static double l2d (long l) {
        return Double.longBitsToDouble(l);
    }




}
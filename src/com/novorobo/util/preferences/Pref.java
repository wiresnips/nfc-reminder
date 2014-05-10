

package com.novorobo.util.preference;

import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;

import android.widget.NumpadInput;

import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.ColorPickerDialogPreference;
import android.preference.NumpadPreference;
import android.preference.SliderPreference;

import android.util.Log;

public abstract class Pref {

	protected Object value;
	protected Preference preference;

	protected List<Listener> listeners = new ArrayList<Listener>();

	public interface Listener {
		void onValueSet (Object value);
    }



	protected Pref (Preference preference) {
		this.preference = preference;

		// Pump all changes through our own channels. Short-circuit the existing ones.
		preference.setOnPreferenceChangeListener(
			new OnPreferenceChangeListener () {
				public boolean onPreferenceChange (Preference preference, Object newValue) {
					setValue(newValue);
					return false;
				}
			}
		);
	}



	public void addListener (Listener listener) {
    	listeners.add(listener);
    }

    public Preference getPreference () {
        return preference;
    }

	public Object getValue () {
    	return value;
    };

    public void setValue (Object newValue) {
   		value = newValue;

   		setPreferenceValue();
   		updatePreferenceView();	

   		for (int i = 0; i < listeners.size(); i++)
   			listeners.get(i).onValueSet(value);
    }


    // update the preference subclass's internal value-tracker
    protected abstract void setPreferenceValue ();

    // update the preference's summary (optional)
    public void updatePreferenceView () {};


    public static class TextPref extends Pref {
    	
    	public TextPref (EditTextPreference preference) {
    		super(preference); 
    	}

    	protected void setPreferenceValue () {
    		((EditTextPreference) preference).setText((String) value);
    	}

    	public void updatePreferenceView () {
    		preference.setSummary((String) value);
    	}
    }

    public static class NumPref extends Pref {   
        public NumPref (NumpadPreference preference) {
            super(preference); 
        }

        protected void setPreferenceValue () {
            ((NumpadPreference) preference).setValue( (Double) value );
        }

        public void updatePreferenceView () {
            preference.setSummary( NumpadInput.doubleToString((Double)value) );
        }
    }

    public static class ListPref extends Pref {
    	
    	public ListPref (ListPreference preference) {
    		super(preference); 
    	}

    	protected void setPreferenceValue () {
    		((ListPreference) preference).setValue((String) value);
    	}

    	public void updatePreferenceView () {
            ListPreference list = (ListPreference) preference;
    		list.setSummary( list.getEntry() );
    	}
    }


    public static class ColorPref extends Pref {
        public ColorPref (ColorPickerDialogPreference preference) {
            super(preference);
        }
        protected void setPreferenceValue () {
            ((ColorPickerDialogPreference) preference).setColor( (Integer) value );
        }
    }

    public static class SliderPref extends Pref {
        public SliderPref (SliderPreference preference) {
            super(preference);
        }
        protected void setPreferenceValue () {
            ((SliderPreference) preference).setValue((Float) value );
        }
    }    

    public static class CheckPref extends Pref {
        
        public CheckPref (CheckBoxPreference preference) {
            super(preference); 
        }

        protected void setPreferenceValue () {
            ((CheckBoxPreference) preference).setChecked((Boolean) value);
        }

        public void updatePreferenceView () {
            CheckBoxPreference box = (CheckBoxPreference) preference;

            // if we're disabled, or lack state-dependant summaries, we do nothing
            if (!box.isEnabled() || (box.getSummaryOn() == null && box.getSummaryOff() == null))
                return;

            // if we're all clear, apply the appropriate state-dependant summary
            box.setSummary( box.isChecked() ? box.getSummaryOn() : box.getSummaryOff() );
        }
    }


    public abstract static class Dependency implements Listener {
    	protected Pref child;

    	public Dependency (Pref parent, Pref child) {
    		parent.addListener(this);
    		this.child = child;

            boolean enabled = getEnabled(parent.getValue());
            child.preference.setEnabled(enabled);

    		if (enabled) onEnable();
    		else         onDisable();
    	}

    	public void onValueSet (Object value) {
    		boolean enabled = getEnabled(value);
        	boolean changed = enabled != child.preference.isEnabled();
			child.preference.setEnabled(enabled);

	        if (changed) {
	            if (enabled) onEnable();
	            else		 onDisable();
	        }
    	}

    	protected abstract boolean getEnabled (Object value);

        // by default, update the view for any changes
    	protected void onDisable () {
            child.updatePreferenceView();
        }
    	protected void onEnable () {
            child.updatePreferenceView();
        }
    }


}
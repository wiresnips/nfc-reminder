
package android.preference;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;

import java.util.List;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import android.app.AlertDialog;
import android.widget.ListView;
import android.widget.ListAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.database.DataSetObserver;

public class ListPrefDisableable extends ListPreference {
    
	private boolean[] itemEnabled;

    public void setItemEnabled (int itemPos, boolean enabled) {
        itemEnabled[itemPos] = enabled;
    }


    public ListPrefDisableable (Context context, AttributeSet attrs) {
        super(context, attrs);
        initItemEnabled();
    }

    public ListPrefDisableable (Context context) {
        this(context, null);
    }

    private void initItemEnabled () {
    	CharSequence[] entries = getEntries();
    	CharSequence[] entryValues = getEntryValues();
    	int size = (entries.length > entryValues.length) ? entries.length : entryValues.length;

        itemEnabled = new boolean[size];
        for (int i = 0; i < size; i++)
        	itemEnabled[i] = true;
    }

	public void setEntryValues (CharSequence[] entryValues) {
		super.setEntryValues(entryValues);
		initItemEnabled();
    }

    public void setEntries (CharSequence[] entries) {
        super.setEntries(entries);
        initItemEnabled();
    }

    protected void showDialog (Bundle state) {
    	super.showDialog(state);

    	AlertDialog dialog = (AlertDialog) getDialog();
    	ListView listView = dialog.getListView();

    	// slide myself in here so I can intercept & modify the views and listener
    	listView.setAdapter( new AdapterWrapper(listView.getAdapter()) );

        // and re-check whichever item was supposed to be checked (since apparently I broke that)
        int checkedItem = findIndexOfValue(getValue());
        if (checkedItem >= 0) {
            listView.setItemChecked(checkedItem, true);
            listView.setSelection(checkedItem);
        }
    }

    // Thin wrapper. Most things pass straight through.
    private class AdapterWrapper implements ListAdapter {
    	private ListAdapter adapter;
    	public AdapterWrapper (ListAdapter adapter) { this.adapter = adapter; }

    	public int getCount () { return adapter.getCount(); }
        public boolean areAllItemsEnabled () { return adapter.areAllItemsEnabled(); }
        public boolean isEnabled (int pos) { return adapter.isEnabled(pos); }
    	public Object getItem (int pos) { return adapter.getItem(pos); }
    	public long getItemId (int pos) { return adapter.getItemId(pos); }
    	public int getItemViewType (int pos) { return adapter.getItemViewType(pos); }
    	public int getViewTypeCount () { return adapter.getViewTypeCount(); }
    	public boolean hasStableIds () { return adapter.hasStableIds(); }
    	public boolean isEmpty () { return adapter.isEmpty(); }
    	public void registerDataSetObserver (DataSetObserver observer) { adapter.registerDataSetObserver(observer); }
    	public void unregisterDataSetObserver (DataSetObserver observer) { adapter.unregisterDataSetObserver(observer); }

    	// ONLY CHANGE
    	public View getView (int pos, View convertView, ViewGroup parent) {
    		View view = adapter.getView(pos, convertView, parent);
    		view.setEnabled(itemEnabled[pos]);
            view.setClickable(!itemEnabled[pos]); // if I'm disabled, I swallow my click events instead of passing them through
    		return view;
    	}
    }

}


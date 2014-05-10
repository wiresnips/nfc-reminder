
package com.novorobo.util.ugh;

import android.util.Log;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;

// Seriously, why is this a thing that I might even want to do?
// This reeks of a decision that'll seem silly once I learn how it was supposed to be done.
// But, since I haven't learned how it was supposed to be done, let's fuckin' go.

public class DialogFragmentWithDismissCallback extends DialogFragment {

    public interface DismissCallback {
        public void onDialogDismissed (String tag);
    }


	public DialogFragmentWithDismissCallback () {
        super();
    }


    private DismissCallback callback = null;

    public void onAttach (Activity activity) {
        super.onAttach(activity);
        
        // if our owner implements our interface, GREAT
        try { callback = (DismissCallback) activity; } 

        // if not, that's also fine
        catch (ClassCastException e) {}
    }

    public void onDismiss (DialogInterface dialog) {
        super.onDismiss(dialog);

        if (callback != null)
            callback.onDialogDismissed( getTag() );
    }

}

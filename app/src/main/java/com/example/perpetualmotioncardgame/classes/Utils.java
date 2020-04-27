package com.example.perpetualmotioncardgame.classes;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;

import com.example.perpetualmotioncardgame.R;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Utils
{

    public final static int sREQUEST_CODE_SETTINGS = 100, sREQUEST_CODE_LOCATION_PERMISSION = 100;

    // background
    // ----------

    public static void showHideBackground (boolean usePicBackground, ImageView background)
    {
        int visibilityMode = usePicBackground ? View.VISIBLE : View.INVISIBLE;

        // Set the ImageView's visibility to be show or hidden as per the user preference
        assert background != null;
        background.setVisibility (visibilityMode);
    }

    // Location
    public static void promptToAllowPermissionRequest (Activity activity)
    {
        DialogInterface.OnClickListener okListener =
                getLocationPromptOkOnClickListener (activity);
        DialogInterface.OnClickListener cancelListener =
                Utils.getNewEmptyOnClickListener ();

        Utils.showOkCancelDialog (activity, "Permission Denied",
                                  "The Location Permission is requested for Night Mode " +
                                          "display purposes.\n\nWould you like to again " +
                                          "be prompted to allow this permission?",
                                  okListener, cancelListener);
    }

    @NonNull private static DialogInterface.OnClickListener getLocationPromptOkOnClickListener
            (final Activity activity)
    {
        return new DialogInterface.OnClickListener ()
        {
            @Override public void onClick (DialogInterface dialog, int which)
            {
                Utils.getLocationPermission (activity, sREQUEST_CODE_LOCATION_PERMISSION);
            }
        };
    }


    // Night Mode
    // ----------
    public static void getLocationPermission (Activity activity, int code)
    {
        // Here, activity is the current activity
        if (ContextCompat.checkSelfPermission (activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions
                    (activity, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, code);
        }
    }

    public static void applyNightModePreference (Activity activity, boolean useNightMode)
    {
        int defaultNightMode = AppCompatDelegate.getDefaultNightMode ();
        int nightModeOn = Build.VERSION.SDK_INT >= 28 ?
                          AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM :
                          AppCompatDelegate.MODE_NIGHT_AUTO;
        int nightModeOff = AppCompatDelegate.MODE_NIGHT_NO;
        int userPrefNightMode = useNightMode ? nightModeOn : nightModeOff;

        if ((useNightMode && (defaultNightMode != userPrefNightMode)) ||
                (!useNightMode && defaultNightMode == nightModeOn)) {

            applyNightMode (activity, userPrefNightMode);
        }
    }

    private static void applyNightMode (final Activity activity, int userPrefNightMode)
    {
        // Inform the user so they're not staring at a white screen while the Activity is recreated
        //Toast.makeText (activity, "Applying Night Mode Preference", Toast.LENGTH_SHORT).show ();

        // Set the Default Night Mode to match the User's Preference
        AppCompatDelegate.setDefaultNightMode (userPrefNightMode);

        // recreate the Activity to make the Night Mode Setting active
        activity.recreate ();
    }

    // AlertDialog
    // -----------

    /**
     * Shows an Android (nicer) equivalent to JOptionPane
     *
     * @param strTitle Title of the Dialog box
     * @param strMsg   Message (body) of the Dialog box
     */
    private static void showAlertDialog (Context context, String strTitle, String strMsg,
                                         DialogInterface.OnClickListener okListener,
                                         DialogInterface.OnClickListener cancelListener)
    {
        AlertDialog.Builder alertDialogBuilder = getDialogBasicsADB (context, strTitle, strMsg);

        if (okListener == null) {
            // create an OK button only and use a dummy listener for that button and the dialog
            alertDialogBuilder.setNeutralButton (context.getString (android.R.string.ok),
                                                 getNewEmptyOnClickListener ());
        }
        else {
            alertDialogBuilder.setPositiveButton (context.getString (android.R.string.ok),
                                                  okListener);
            alertDialogBuilder.setNegativeButton (context.getString (android.R.string.cancel),
                                                  cancelListener);
        }

        // Create and Show the Dialog
        alertDialogBuilder.show ();
    }

    @NonNull
    private static AlertDialog.Builder getDialogBasicsADB (Context context, String strTitle,
                                                           String strMsg)
    {
        // Create the AlertDialog.Builder object
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder (context);

        // Use the AlertDialog's Builder Class methods to set the title, icon, message, et al.
        // These could all be chained as one long statement, if desired
        alertDialogBuilder.setTitle (strTitle);
        alertDialogBuilder.setMessage (strMsg);
        alertDialogBuilder.setIcon (ContextCompat.getDrawable (context, R.mipmap.ic_launcher));
        alertDialogBuilder.setCancelable (true);
        return alertDialogBuilder;
    }

    @SuppressWarnings ("WeakerAccess") @NonNull
    public static DialogInterface.OnClickListener getNewEmptyOnClickListener ()
    {
        return new DialogInterface.OnClickListener ()
        {
            @Override public void onClick (DialogInterface dialog, int which)
            {

            }
        };
    }

    public static void showInfoDialog (Context context, int titleID, int msgID)
    {
        showInfoDialog (context, context.getString (titleID), context.getString (msgID));
    }

    @SuppressWarnings ("WeakerAccess")
    public static void showInfoDialog (Context context, String strTitle, String strMsg)
    {
        showAlertDialog (context, strTitle, strMsg);
    }

    @SuppressWarnings ("WeakerAccess")
    public static void showOkCancelDialog (Context context, String strTitle, String strMsg,
                                           DialogInterface.OnClickListener okListener,
                                           DialogInterface.OnClickListener cancelListener)
    {
        showAlertDialog (context, strTitle, strMsg, okListener, cancelListener);
    }

    public static void showYesNoDialog (Context context, String strTitle, String strMsg,
                                        DialogInterface.OnClickListener okListener,
                                        DialogInterface.OnClickListener cancelListener)
    {
        showAlertDialog (context, strTitle, strMsg, okListener, cancelListener);
    }

    private static void showAlertDialog (Context context, String strTitle, String strMsg)
    {
        showAlertDialog (context, strTitle, strMsg, null, null);
    }
}
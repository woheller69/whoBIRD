package org.tensorflow.lite.examples.soundclassifier;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.preference.PreferenceManager;

public class GithubStar {
    public static void setAskForStar(boolean askForStar, Context context) {
        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefManager.edit();
        editor.putBoolean("askForStar", askForStar);
        editor.apply();
    }

    public static boolean shouldShowStarDialog(Context context) {
        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
        int versionCode = prefManager.getInt("versionCode", 0);
        boolean askForStar = prefManager.getBoolean("askForStar", true);

        if (prefManager.contains("versionCode") && BuildConfig.VERSION_CODE > versionCode && askForStar) { //not at first start, only after upgrade and only if use has not yet given a star or has declined
            SharedPreferences.Editor editor = prefManager.edit();
            editor.putInt("versionCode", BuildConfig.VERSION_CODE);
            editor.apply();
            return true;
        } else {
            SharedPreferences.Editor editor = prefManager.edit();
            editor.putInt("versionCode", BuildConfig.VERSION_CODE);
            editor.apply();
            return false;
        }
    }

    public static void starDialog(Context context, String url) {
        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefManager.getBoolean("askForStar", true)) {
            android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(context);
            alertDialogBuilder.setMessage(R.string.dialog_StarOnGitHub);
            alertDialogBuilder.setPositiveButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    setAskForStar(false, context);
                }
            });
            alertDialogBuilder.setNegativeButton(context.getString(android.R.string.no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setAskForStar(false, context);
                }
            });
            alertDialogBuilder.setNeutralButton(context.getString(R.string.dialog_Later_button), null);

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }
}
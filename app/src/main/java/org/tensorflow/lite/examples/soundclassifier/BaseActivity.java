package org.tensorflow.lite.examples.soundclassifier;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyTheme();
    }

    protected void applyTheme() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean highContrastTheme = sharedPreferences.getBoolean("theme", false);

        if (highContrastTheme) {
            setTheme(R.style.AppThemeHighContrast);
        } else {
            setTheme(R.style.AppTheme);
        }
    }
}

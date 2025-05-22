package org.tensorflow.lite.examples.soundclassifier;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class SettingsActivity extends BaseActivity {
Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        BottomNavigationView navigationView = findViewById(R.id.bottomNavigationView);
        navigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId()==R.id.action_bird_info){
                    Intent intent = new Intent(mContext, BirdInfoActivity.class);
                    startActivity(intent);
                } else if (item.getItemId()==R.id.action_mic){
                    Intent intent = new Intent(mContext, MainActivity.class);
                    startActivity(intent);
                } else if (item.getItemId()==R.id.action_view){
                    Intent intent = new Intent(mContext, ViewActivity.class);
                    startActivity(intent);
                }
                return true;
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }



    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            Preference writeWav = getPreferenceManager().findPreference("write_wav");
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) preferenceScreen.removePreference(writeWav);

            Preference reset = getPreferenceManager().findPreference("reset");

            if (reset != null) reset.setOnPreferenceClickListener(preference -> {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

                sharedPreferences.edit().remove("audio_source").apply();
                sharedPreferences.edit().remove("high_pass").apply();
                sharedPreferences.edit().remove("model_threshold").apply();
                sharedPreferences.edit().remove("play_sound").apply();
                sharedPreferences.edit().remove("write_wav").apply();
                sharedPreferences.edit().remove("theme").apply();

                onCreatePreferences(savedInstanceState,rootKey);
                return false;
            });

            Preference theme = findPreference("theme");
            theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    requireActivity().recreate();
                    return true;
                }
            });
            Preference language = getPreferenceManager().findPreference("language");
            if (language != null) language.setOnPreferenceClickListener(preference -> {
                // Create an intent to open the app's settings
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
                return true; // Return true to indicate that the click event has been handled

            });
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) preferenceScreen.removePreference(language);

            SwitchPreferenceCompat showSpectrogramPref = findPreference("show_spectrogram");
            SwitchPreferenceCompat showImagesPref = findPreference("show_images");
            showSpectrogramPref.setOnPreferenceChangeListener((preference, newValue) -> {
                if ((Boolean) newValue) {
                    // If show_spectrogram is turned on, turn off show_images
                    showImagesPref.setChecked(false);
                }
                return true; // Allow the change
            });
            showImagesPref.setOnPreferenceChangeListener((preference, newValue) -> {
                if ((Boolean) newValue) {
                    // If show_images is turned on, turn off show_spectrogram
                    showSpectrogramPref.setChecked(false);
                }
                return true; // Allow the change
            });

        }
    }
}
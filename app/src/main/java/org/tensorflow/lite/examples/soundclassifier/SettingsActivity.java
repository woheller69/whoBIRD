package org.tensorflow.lite.examples.soundclassifier;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class SettingsActivity extends AppCompatActivity {
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

                onCreatePreferences(savedInstanceState,rootKey);
                return false;
            });
        }
    }
}
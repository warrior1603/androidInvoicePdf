package com.chouchene.factures.fragments;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import com.chouchene.factures.R;

public class SettingsFactureFragment extends PreferenceFragmentCompat {

    private static final String THEME_KEY = "theme";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.settings, rootKey);

        // Find the SwitchPreferenceCompat
        SwitchPreferenceCompat darkModeSwitch = findPreference(THEME_KEY);
        if (darkModeSwitch != null) {
            darkModeSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isDarkMode = (boolean) newValue;
                AppCompatDelegate.setDefaultNightMode(
                        isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                );
                return true;
            });
        }
    }
}

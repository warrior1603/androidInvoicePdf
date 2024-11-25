package com.chouchene.factures.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;
import com.chouchene.factures.R;

public class SettingsFactureFragment extends PreferenceFragmentCompat {

    private static final String THEME_KEY = "theme";
    private static final String DIRECTORY_KEY = "directory";
    private static final String CURRENCY_KEY = "default_currency";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.settings, rootKey);

        // Find the SwitchPreferenceCompat
        SwitchPreferenceCompat darkModeSwitch = findPreference(THEME_KEY);
        if (darkModeSwitch != null) {
            darkModeSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean isDarkMode = (boolean) newValue;
                    if (isDarkMode) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(THEME_KEY, isDarkMode);
                    editor.apply();


                    requireActivity().recreate();

                    return true;
                }
            });
        }
    }
}

package com.chouchene.factures.fragments;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;
import com.chouchene.factures.R;
import com.chouchene.factures.utils.LocaleHelper;

public class GeneralSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_general, rootKey);

        SwitchPreferenceCompat darkModeSwitch = findPreference("theme");
        if (darkModeSwitch != null) {
            darkModeSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isDarkMode = (boolean) newValue;
                AppCompatDelegate.setDefaultNightMode(
                        isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                );
                return true;
            });
        }

        SwitchPreferenceCompat dynamicColorSwitch = findPreference("dynamic_colors");
        if (dynamicColorSwitch != null) {
            dynamicColorSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isEnabled = (boolean) newValue;
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .edit()
                        .putBoolean("dynamic_colors", isEnabled)
                        .apply();
                
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .edit()
                        .putBoolean("refresh_theme", true)
                        .apply();
                
                requireActivity().recreate();
                return true;
            });
        }

        ListPreference langPref = findPreference("language");
        if (langPref != null) {
            langPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String language = (String) newValue;
                LocaleHelper.setLocale(requireContext(), language);
                requireActivity().recreate();
                return true;
            });
        }
    }
}
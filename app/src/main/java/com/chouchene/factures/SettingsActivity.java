package com.chouchene.factures;

import com.chouchene.factures.utils.LocaleHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.chouchene.factures.fragments.PersonalSettingsFragment;
import com.chouchene.factures.fragments.TemplatePreviewFragment;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {

    private static final String THEME_KEY = "theme";

    SharedPreferences sharedPreferences;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "fr"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = sharedPreferences.getBoolean(THEME_KEY, false);
        setTheme(isDarkMode ? R.style.DarkTheme : R.style.LightTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);



        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }

        MaterialToolbar myToolbar = findViewById(R.id.my_toolbar1);
        setSupportActionBar(myToolbar);
        
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateToolbar);

        myToolbar.setNavigationOnClickListener(v -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        updateToolbar();
    }

    private void updateToolbar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.title_settings);
        }
    }



    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
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

            Preference profilePref = findPreference("profile_entreprise");
            if (profilePref != null) {
                profilePref.setOnPreferenceClickListener(preference -> {
                    getParentFragmentManager()
                            .beginTransaction()
                            .replace(R.id.settings, new PersonalSettingsFragment())
                            .addToBackStack(null)
                            .commit();
                    return true;
                });
            }

            Preference previewPref = findPreference("preview_templates");
            if (previewPref != null) {
                previewPref.setOnPreferenceClickListener(preference -> {
                    getParentFragmentManager()
                            .beginTransaction()
                            .replace(R.id.settings, new TemplatePreviewFragment())
                            .addToBackStack(null)
                            .commit();
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
}

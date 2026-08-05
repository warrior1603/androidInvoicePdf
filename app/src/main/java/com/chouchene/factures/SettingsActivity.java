package com.chouchene.factures;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.fragments.PersonalSettingsFragment;
import com.chouchene.factures.fragments.TemplatePreviewFragment;
import com.chouchene.factures.utils.BackupUtils;
import com.chouchene.factures.utils.LocaleHelper;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

        private ActivityResultLauncher<String> exportLauncher;
        private ActivityResultLauncher<String[]> importLauncher;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            exportLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/zip"), uri -> {
                if (uri != null) {
                    performExport(uri);
                }
            });

            importLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    confirmImport(uri);
                }
            });
        }

        private void performExport(Uri uri) {
            try (OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                String pdfDir = prefs.getString("directory", null);
                if (BackupUtils.exportData(requireContext(), os, pdfDir)) {
                    android.widget.Toast.makeText(requireContext(), R.string.msg_export_success, android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    android.widget.Toast.makeText(requireContext(), R.string.msg_export_error, android.widget.Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                android.widget.Toast.makeText(requireContext(), R.string.msg_export_error, android.widget.Toast.LENGTH_SHORT).show();
            }
        }

        private void confirmImport(Uri uri) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.pref_title_import)
                    .setMessage(R.string.msg_import_confirm)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> performImport(uri))
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }

        private void performImport(Uri uri) {
            try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                String pdfDir = prefs.getString("directory", null);

                // Close DB safely
                DatabaseClient.getInstance(requireContext()).getAppDatabase().close();

                if (BackupUtils.importData(requireContext(), is, pdfDir)) {
                    android.widget.Toast.makeText(requireContext(), R.string.msg_import_success, android.widget.Toast.LENGTH_LONG).show();
                    // Restart App
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                } else {
                    android.widget.Toast.makeText(requireContext(), R.string.msg_import_error, android.widget.Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                android.widget.Toast.makeText(requireContext(), R.string.msg_import_error, android.widget.Toast.LENGTH_SHORT).show();
            }
        }

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

            Preference exportPref = findPreference("export_data");
            if (exportPref != null) {
                exportPref.setOnPreferenceClickListener(preference -> {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    exportLauncher.launch("Backup_Factures_" + timeStamp + ".zip");
                    return true;
                });
            }

            Preference importPref = findPreference("import_data");
            if (importPref != null) {
                importPref.setOnPreferenceClickListener(preference -> {
                    importLauncher.launch(new String[]{"application/zip"});
                    return true;
                });
            }
        }
    }
}

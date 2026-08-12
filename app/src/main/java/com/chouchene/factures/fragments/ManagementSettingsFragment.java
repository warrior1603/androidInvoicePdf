package com.chouchene.factures.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import com.chouchene.factures.MainActivity;
import com.chouchene.factures.R;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.fragments.TemplatePreviewFragment;
import com.chouchene.factures.utils.BackupUtils;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ManagementSettingsFragment extends PreferenceFragmentCompat {

    private ActivityResultLauncher<String> exportLauncher;
    private ActivityResultLauncher<String[]> importLauncher;
    private ActivityResultLauncher<Uri> dirPickerLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        exportLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/zip"), uri -> {
            if (uri != null) performExport(uri);
        });

        importLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) confirmImport(uri);
        });

        dirPickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
            if (uri != null) saveDirectory(uri);
        });
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_management, rootKey);
        updateDirectorySummary();

        Preference dirPref = findPreference("directory");
        if (dirPref != null) {
            dirPref.setOnPreferenceClickListener(preference -> {
                Uri initialUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload");
                dirPickerLauncher.launch(initialUri);
                return true;
            });
        }

        ListPreference templatePref = findPreference("invoice_template");
        if (templatePref != null) {
            templatePref.setOnPreferenceClickListener(preference -> {
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.settings, new TemplatePreviewFragment())
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
                importLauncher.launch(new String[]{"application/zip", "application/x-zip-compressed", "application/octet-stream"});
                return true;
            });
        }
    }

    private void saveDirectory(Uri uri) {
        try {
            requireContext().getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception e) {
            Log.e("Settings", "Permission failed", e);
        }
        
        String path = null;
        if (uri.getPath() != null) {
            String treeId = uri.getPath();
            if (treeId.contains("primary:")) {
                String subPath = treeId.split("primary:")[1];
                path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + subPath;
            }
        }

        if (path == null) {
            path = BackupUtils.getDefaultPdfDir(requireContext());
            android.widget.Toast.makeText(requireContext(), "Dossier non compatible, retour au dossier par défaut", android.widget.Toast.LENGTH_LONG).show();
        }

        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putString("directory", path).apply();
        updateDirectorySummary();
        android.widget.Toast.makeText(requireContext(), "Dossier mis à jour", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void updateDirectorySummary() {
        Preference dirPref = findPreference("directory");
        if (dirPref != null) {
            String current = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("directory", BackupUtils.getDefaultPdfDir(requireContext()));
            dirPref.setSummary(current);
        }
    }

    private void performExport(Uri uri) {
        new Thread(() -> {
            try (OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                String pdfDir = prefs.getString("directory", BackupUtils.getDefaultPdfDir(requireContext()));
                boolean success = BackupUtils.exportData(requireContext(), os, pdfDir);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        android.widget.Toast.makeText(requireContext(), success ? R.string.msg_export_success : R.string.msg_export_error, android.widget.Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> 
                        android.widget.Toast.makeText(requireContext(), R.string.msg_export_error, android.widget.Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }).start();
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
        android.app.ProgressDialog pd = new android.app.ProgressDialog(requireContext());
        pd.setMessage("Importation en cours...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                String pdfDir = prefs.getString("directory", BackupUtils.getDefaultPdfDir(requireContext()));
                DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase().close();
                boolean success = BackupUtils.importData(requireContext(), is, pdfDir);

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        pd.dismiss();
                        if (success) {
                            android.widget.Toast.makeText(requireContext(), R.string.msg_import_success, android.widget.Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(requireContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(0);
                        } else {
                            android.widget.Toast.makeText(requireContext(), R.string.msg_import_error, android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        pd.dismiss();
                        android.widget.Toast.makeText(requireContext(), R.string.msg_import_error, android.widget.Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }
}
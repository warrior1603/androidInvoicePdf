package com.chouchene.factures.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.chouchene.factures.MainActivity;
import com.chouchene.factures.R;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.utils.BackupUtils;
import com.chouchene.factures.utils.LocaleHelper;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;
    private ActivityResultLauncher<String> exportLauncher;
    private ActivityResultLauncher<String[]> importLauncher;
    private ActivityResultLauncher<Uri> dirPickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_unified, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        view.findViewById(R.id.btn_back_header).setOnClickListener(v -> {
            if (isAdded()) {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        // --- SECTION: PREFERENCES ---
        setupSwitch(view.findViewById(R.id.item_theme), "theme", R.drawable.ic_typcn_weather_night, 
                "Mode Sombre", "Activer l'interface sombre", (v, checked) -> {
            AppCompatDelegate.setDefaultNightMode(checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        setupSwitch(view.findViewById(R.id.item_dynamic_colors), "dynamic_colors", R.drawable.ic_typcn_brush, 
                "Couleurs Dynamiques", "Utiliser les couleurs du système", (v, checked) -> {
            prefs.edit().putBoolean("refresh_theme", true).apply();
            requireActivity().recreate();
        });

        setupClickable(view.findViewById(R.id.item_language), R.drawable.ic_typcn_world, 
                "Langue", LocaleHelper.getLanguage(requireContext()).toUpperCase(), v -> {
            String current = LocaleHelper.getLanguage(requireContext());
            String next = "fr".equals(current) ? "en" : "fr";
            LocaleHelper.setLocale(requireContext(), next);
            requireActivity().recreate();
        });

        setupClickable(view.findViewById(R.id.item_currency), R.drawable.ic_typcn_cart, 
                "Devise", prefs.getString("default_currency", "EUR"), v -> {
            Toast.makeText(getContext(), "Fonctionnalité bientôt disponible", Toast.LENGTH_SHORT).show();
        });

        // --- SECTION: SECURITY ---
        setupSwitch(view.findViewById(R.id.item_biometric), "biometric_lock", R.drawable.ic_typcn_key_outline, 
                "Sécurité Biométrique", "Verrouiller l'accès à l'application", null);

        // --- SECTION: DATA ---
        String currentDir = prefs.getString("directory", BackupUtils.getDefaultPdfDir(requireContext()));
        setupClickable(view.findViewById(R.id.item_directory), R.drawable.ic_typcn_folder_open, 
                "Dossier de stockage", currentDir, v -> {
            Uri initialUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload");
            dirPickerLauncher.launch(initialUri);
        });

        setupClickable(view.findViewById(R.id.item_templates), R.drawable.ic_typcn_document, 
                "Modèles PDF", "Gérer les templates de factures", v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.settings, new TemplatePreviewFragment())
                    .addToBackStack(null)
                    .commit();
        });

        setupClickable(view.findViewById(R.id.item_export), R.drawable.ic_typcn_export_outline, 
                "Exporter les données", "Sauvegarder base de données et PDF", v -> {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            exportLauncher.launch("Backup_Factures_" + timeStamp + ".zip");
        });

        setupClickable(view.findViewById(R.id.item_import), R.drawable.ic_typcn_arrow_sync_outline, 
                "Importer les données", "Restaurer depuis une sauvegarde", v -> {
            importLauncher.launch(new String[]{"application/zip", "application/x-zip-compressed", "application/octet-stream"});
        });

        // --- SECTION: SUPPORT & INFO ---
        setupClickable(view.findViewById(R.id.item_help), R.drawable.ic_typcn_info_large, 
                "Aide & Support", "Consulter la documentation", v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.settings, new HelpFragment())
                    .addToBackStack(null)
                    .commit();
        });

        setupClickable(view.findViewById(R.id.item_about), R.drawable.ic_typcn_news, 
                "À Propos", "Version de l'application et infos", v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.settings, new AboutFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupSwitch(View view, String key, int iconRes, String title, String summary, MaterialSwitch.OnCheckedChangeListener customListener) {
        ImageView icon = view.findViewById(R.id.item_icon);
        TextView txtTitle = view.findViewById(R.id.item_title);
        TextView txtSummary = view.findViewById(R.id.item_summary);
        MaterialSwitch mSwitch = view.findViewById(R.id.item_switch);

        icon.setImageResource(iconRes);
        txtTitle.setText(title);
        txtSummary.setText(summary);
        
        boolean currentVal = prefs.getBoolean(key, false);
        mSwitch.setChecked(currentVal);

        view.setOnClickListener(v -> mSwitch.toggle());

        mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(key, isChecked).apply();
            if (customListener != null) customListener.onCheckedChanged(buttonView, isChecked);
        });
    }

    private void setupClickable(View view, int iconRes, String title, String summary, View.OnClickListener listener) {
        ImageView icon = view.findViewById(R.id.item_icon);
        TextView txtTitle = view.findViewById(R.id.item_title);
        TextView txtSummary = view.findViewById(R.id.item_summary);

        icon.setImageResource(iconRes);
        txtTitle.setText(title);
        txtSummary.setText(summary);
        view.setOnClickListener(listener);
    }

    private void saveDirectory(Uri uri) {
        try {
            requireContext().getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            
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
                Toast.makeText(requireContext(), "Dossier non compatible", Toast.LENGTH_SHORT).show();
            }

            prefs.edit().putString("directory", path).apply();
            // Refresh UI
            View directoryItem = getView() != null ? getView().findViewById(R.id.item_directory) : null;
            if (directoryItem != null) {
                setupClickable(directoryItem, R.drawable.ic_typcn_folder_open, "Dossier de stockage", path, v -> {
                    Uri initialUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload");
                    dirPickerLauncher.launch(initialUri);
                });
            }
            Toast.makeText(requireContext(), "Dossier mis à jour", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e("Settings", "Permission failed", e);
        }
    }

    private void performExport(Uri uri) {
        new Thread(() -> {
            try (OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
                String pdfDir = prefs.getString("directory", BackupUtils.getDefaultPdfDir(requireContext()));
                boolean success = BackupUtils.exportData(requireContext(), os, pdfDir);
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), success ? "Export réussi" : "Erreur d'export", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Erreur d'export", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void confirmImport(Uri uri) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Importer")
                .setMessage("Toutes les données actuelles seront remplacées. Continuer ?")
                .setPositiveButton("Oui", (dialog, which) -> performImport(uri))
                .setNegativeButton("Non", null)
                .show();
    }

    private void performImport(Uri uri) {
        new Thread(() -> {
            try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
                String pdfDir = prefs.getString("directory", BackupUtils.getDefaultPdfDir(requireContext()));
                DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase().close();
                boolean success = BackupUtils.importData(requireContext(), is, pdfDir);

                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(requireContext(), "Import réussi. Redémarrage...", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(requireContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(0);
                    } else {
                        Toast.makeText(requireContext(), "Erreur d'import", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Erreur d'import", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
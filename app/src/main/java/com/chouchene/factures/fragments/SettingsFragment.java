package com.chouchene.factures.fragments;

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

    private SharedPreferences prefs, businessPrefs, appPrefs;
    private ActivityResultLauncher<String> exportLauncher;
    private ActivityResultLauncher<String[]> importLauncher;
    private ActivityResultLauncher<Uri> dirPickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        businessPrefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        appPrefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        
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

        view.findViewById(R.id.btn_back_header).setOnClickListener(v -> {
            if (isAdded()) {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        // --- SECTION: ACCOUNT ---
        setupClickable(view.findViewById(R.id.item_profile), R.drawable.ic_typcn_entreprise,
                "Profil Entreprise", "Gérer vos coordonnées et logo", v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.settings, new EntrepriseSettingsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // --- SECTION: GENERATION PREFERENCES ---
        setupClickable(view.findViewById(R.id.item_default_tva), R.drawable.ic_outline_adjustments, 
                "TVA par défaut", businessPrefs.getString("default_tva", "10") + "%", v -> {
            final android.widget.EditText input = new android.widget.EditText(requireContext());
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setText(businessPrefs.getString("default_tva", "10"));
            
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("TVA par défaut (%)")
                    .setView(input)
                    .setPositiveButton("Enregistrer", (dialog, which) -> {
                        String val = input.getText().toString();
                        businessPrefs.edit().putString("default_tva", val).apply();
                        setupClickable(view.findViewById(R.id.item_default_tva), R.drawable.ic_outline_adjustments, 
                                "TVA par défaut", val + "%", null);
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
        });

        setupClickable(view.findViewById(R.id.item_default_payment), R.drawable.ic_outline_cash, 
                "Paiement par défaut", businessPrefs.getString("default_payment", "Virement"), v -> {
            String[] modes = {"Virement", "Carte", "Espèce", "Chèque"};
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Mode de paiement par défaut")
                    .setItems(modes, (dialog, which) -> {
                        businessPrefs.edit().putString("default_payment", modes[which]).apply();
                        setupClickable(view.findViewById(R.id.item_default_payment), R.drawable.ic_outline_cash, 
                                "Paiement par défaut", modes[which], null);
                    })
                    .show();
        });

        setupClickable(view.findViewById(R.id.item_language), R.drawable.ic_typcn_world, 
                "Langue de l'app", LocaleHelper.getLanguage(requireContext()).equals("fr") ? "Français" : "English", v -> {
            String[] langs = {"Français", "English"};
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Choisir la langue")
                    .setItems(langs, (dialog, which) -> {
                        String next = which == 0 ? "fr" : "en";
                        LocaleHelper.setLocale(requireContext(), next);
                        requireActivity().recreate();
                    })
                    .show();
        });

        setupClickable(view.findViewById(R.id.item_currency), R.drawable.ic_typcn_cart, 
                "Devise par défaut", businessPrefs.getString("default_currency", "EUR (€)"), v -> {
            String[] currencies = {"EUR (€)", "USD ($)", "GBP (£)", "CHF (CHF)", "TND (DT)"};
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Choisir la devise")
                    .setItems(currencies, (dialog, which) -> {
                        String selected = currencies[which];
                        businessPrefs.edit().putString("default_currency", selected).apply();
                        
                        // Update UI immediately
                        TextView summary = view.findViewById(R.id.item_currency).findViewById(R.id.item_summary);
                        if (summary != null) summary.setText(selected);
                        
                        Toast.makeText(getContext(), "Devise mise à jour", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });

        // --- SECTION: SECURITY & ALERTS ---
        boolean isBioAvailable = isBiometricAvailable();
        View bioItem = view.findViewById(R.id.item_biometric);
        setupSwitch(bioItem, "biometric_lock", appPrefs, R.drawable.ic_typcn_key_outline, 
                "Sécurité Biométrique", isBioAvailable ? "Verrouiller l'accès à l'application" : "Non disponible sur cet appareil", (v, checked) -> {
                    if (!isBioAvailable && checked) {
                        Toast.makeText(getContext(), "Biométrie non disponible", Toast.LENGTH_SHORT).show();
                        v.setChecked(false);
                    }
                });
        
        if (!isBioAvailable && bioItem != null) {
            bioItem.setEnabled(false);
            bioItem.setAlpha(0.5f);
        }

        setupSwitch(view.findViewById(R.id.item_notifications), "notifications_enabled", appPrefs, R.drawable.ic_typcn_bell,
                "Alertes Intelligentes", "Notifications de rappels et factures", null);

        // --- SECTION: DATA ---
        String currentDir = businessPrefs.getString("directory", BackupUtils.getDefaultPdfDir(requireContext()));
        String displayDir = simplifyPath(currentDir);
        setupClickable(view.findViewById(R.id.item_directory), R.drawable.ic_typcn_folder_open, 
                "Dossier de stockage", displayDir, v -> {
            dirPickerLauncher.launch(null);
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

        applyEntranceAnimations(view);
    }

    private void applyEntranceAnimations(View view) {
        View content = view.findViewById(R.id.settings_content_scroll);
        if (content instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) content;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child == null) continue;
                child.setAlpha(0f);
                child.setTranslationY(40f);
                child.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(500)
                        .setStartDelay(100 + (i * 60L))
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
            }
        }
    }

    private void setupSwitch(View view, String key, SharedPreferences sharedPrefs, int iconRes, String title, String summary, MaterialSwitch.OnCheckedChangeListener customListener) {
        if (view == null) return;
        ImageView icon = view.findViewById(R.id.item_icon);
        TextView txtTitle = view.findViewById(R.id.item_title);
        TextView txtSummary = view.findViewById(R.id.item_summary);
        MaterialSwitch mSwitch = view.findViewById(R.id.item_switch);

        if (icon != null) icon.setImageResource(iconRes);
        if (txtTitle != null) txtTitle.setText(title);
        if (txtSummary != null) txtSummary.setText(summary);
        
        if (mSwitch != null) {
            mSwitch.setOnCheckedChangeListener(null);
            mSwitch.setChecked(sharedPrefs.getBoolean(key, false));

            // Unified click on both container and switch
            View.OnClickListener toggle = v -> {
                if (mSwitch.isEnabled()) {
                    mSwitch.setChecked(!mSwitch.isChecked());
                }
            };
            
            view.setOnClickListener(toggle);
            mSwitch.setClickable(true);
            mSwitch.setOnClickListener(toggle);

            mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sharedPrefs.edit().putBoolean(key, isChecked).commit();
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                if (customListener != null) customListener.onCheckedChanged(buttonView, isChecked);
            });
        }
    }

    private void setupSwitch(View view, String key, int iconRes, String title, String summary, MaterialSwitch.OnCheckedChangeListener customListener) {
        setupSwitch(view, key, prefs, iconRes, title, summary, customListener);
    }

    private void setupClickable(View view, int iconRes, String title, String summary, View.OnClickListener listener) {
        if (view == null) return;
        ImageView icon = view.findViewById(R.id.item_icon);
        TextView txtTitle = view.findViewById(R.id.item_title);
        TextView txtSummary = view.findViewById(R.id.item_summary);

        if (icon != null) icon.setImageResource(iconRes);
        if (txtTitle != null) txtTitle.setText(title);
        if (txtSummary != null) txtSummary.setText(summary);
        view.setOnClickListener(listener);
    }

    private String simplifyPath(String path) {
        if (path == null) return "Défaut";
        if (path.contains("/0/")) {
            return "..." + path.substring(path.lastIndexOf("/0/") + 2);
        }
        return path;
    }

    private boolean isBiometricAvailable() {
        try {
            androidx.biometric.BiometricManager biometricManager = androidx.biometric.BiometricManager.from(requireContext());
            int result = biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG | androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            return result == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Exception e) {
            return false;
        }
    }

    private void saveDirectory(Uri uri) {
        try {
            requireContext().getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            
            String path = null;
            if (uri.getPath() != null) {
                String uriPath = uri.getPath();
                if (uriPath.contains(":")) {
                    String[] split = uriPath.split(":");
                    if (split.length > 1) {
                        path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + split[1];
                    }
                }
            }

            if (path == null) {
                path = BackupUtils.getDefaultPdfDir(requireContext());
                Toast.makeText(requireContext(), "Dossier externe non accessible, utilisation du dossier par défaut", Toast.LENGTH_SHORT).show();
            }

            businessPrefs.edit().putString("directory", path).apply();
            
            View directoryItem = getView() != null ? getView().findViewById(R.id.item_directory) : null;
            if (directoryItem != null) {
                setupClickable(directoryItem, R.drawable.ic_typcn_folder_open, "Dossier de stockage", simplifyPath(path), v -> {
                    dirPickerLauncher.launch(null);
                });
            }
            Toast.makeText(requireContext(), "Dossier mis à jour", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e("Settings", "Permission failed", e);
            Toast.makeText(requireContext(), "Erreur de permission", Toast.LENGTH_SHORT).show();
        }
    }

    private void performExport(Uri uri) {
        new Thread(() -> {
            try (OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
                String pdfDir = businessPrefs.getString("directory", BackupUtils.getDefaultPdfDir(requireContext()));
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
                String pdfDir = businessPrefs.getString("directory", BackupUtils.getDefaultPdfDir(requireContext()));
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

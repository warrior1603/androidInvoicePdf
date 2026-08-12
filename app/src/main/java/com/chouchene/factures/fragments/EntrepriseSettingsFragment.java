package com.chouchene.factures.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.chouchene.factures.R;
import com.chouchene.factures.api.FetchVilleFromCodePostale;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EntrepriseSettingsFragment extends Fragment {

    private TextInputEditText editZip, editCity, editCountry;
    private ShapeableImageView imgLogo;
    private TextView txtLogoStatus;
    
    private View contentStep1, contentStep2, contentStep3;
    private TextView indicator1, indicator2, indicator3;
    private View line1, line2;
    private TextView summary1, summary2, summary3;
    
    private ViewGroup stepperRoot;
    private int activeStep = 0;
    
    private SharedPreferences prefs;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) saveLogoLocally(selectedImageUri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entreprise_settings_inline, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        stepperRoot = view.findViewById(R.id.stepper_root);
        
        // Stepper UI Elements
        contentStep1 = view.findViewById(R.id.content_step_1);
        contentStep2 = view.findViewById(R.id.content_step_2);
        contentStep3 = view.findViewById(R.id.content_step_3);
        
        indicator1 = view.findViewById(R.id.step_indicator_1);
        indicator2 = view.findViewById(R.id.step_indicator_2);
        indicator3 = view.findViewById(R.id.step_indicator_3);
        
        line1 = view.findViewById(R.id.line_1);
        line2 = view.findViewById(R.id.line_2);
        
        summary1 = view.findViewById(R.id.summary_step_1);
        summary2 = view.findViewById(R.id.summary_step_2);
        summary3 = view.findViewById(R.id.summary_step_3);

        // Binding and initializing items
        initItem(view.findViewById(R.id.item_user), "User", R.drawable.ic_outline_users, getString(R.string.label_full_name), InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        initItem(view.findViewById(R.id.item_email), "email", R.drawable.ic_outline_mail, getString(R.string.label_email), InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        initItem(view.findViewById(R.id.item_tel), "tel", R.drawable.ic_outline_phone, getString(R.string.label_contact_number), InputType.TYPE_CLASS_PHONE);
        initItem(view.findViewById(R.id.item_street), "Street", R.drawable.ic_outline_road, getString(R.string.label_street), InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        
        editZip = initItem(view.findViewById(R.id.item_zip), "codePostale", R.drawable.ic_outline_hash, getString(R.string.label_postal_code), InputType.TYPE_CLASS_NUMBER);
        editCity = initItem(view.findViewById(R.id.item_city), "City", R.drawable.ic_outline_building, getString(R.string.label_city), InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editCountry = initItem(view.findViewById(R.id.item_country), "Country", R.drawable.ic_tab_world, getString(R.string.label_country), InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        
        initItem(view.findViewById(R.id.item_siren), "siren", R.drawable.ic_outline_adjustments, getString(R.string.label_siren), InputType.TYPE_CLASS_NUMBER);
        initItem(view.findViewById(R.id.item_tva), "tva", R.drawable.ic_outline_cash, getString(R.string.label_tva), InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        
        initItem(view.findViewById(R.id.item_chauffeur), "chauffeur", R.drawable.ic_outline_users, getString(R.string.label_driver_name), InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        initItem(view.findViewById(R.id.item_plaque), "plaque", R.drawable.ic_outline_hash, getString(R.string.label_plate_number), InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        initItem(view.findViewById(R.id.item_evtc), "evtc", R.drawable.ic_outline_receipt, getString(R.string.label_evtc_number), InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        
        initItem(view.findViewById(R.id.item_iban), "iban", R.drawable.ic_outline_cash, "IBAN", InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        initItem(view.findViewById(R.id.item_bic), "bic", R.drawable.ic_outline_building, "BIC", InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        initItem(view.findViewById(R.id.item_bank_address), "bankAddress", R.drawable.ic_outline_adjustments, getString(R.string.label_bank_address), InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);

        imgLogo = view.findViewById(R.id.img_logo_settings);
        txtLogoStatus = view.findViewById(R.id.txt_logo_status);

        loadLogo();

        view.findViewById(R.id.row_logo).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        // Stepper Navigation
        view.findViewById(R.id.header_step_1).setOnClickListener(v -> goToStep(0));
        view.findViewById(R.id.header_step_2).setOnClickListener(v -> goToStep(1));
        view.findViewById(R.id.header_step_3).setOnClickListener(v -> goToStep(2));
        
        view.findViewById(R.id.btn_next_1).setOnClickListener(v -> goToStep(1));
        view.findViewById(R.id.btn_next_2).setOnClickListener(v -> goToStep(2));
        view.findViewById(R.id.btn_finish).setOnClickListener(v -> {
            Snackbar.make(view, "Configuration terminée et enregistrée", Snackbar.LENGTH_SHORT).show();
        });

        setupZipCodeLookup();
        updateSummaries();
        refreshStepperUI();
    }

    private void goToStep(int step) {
        if (activeStep == step) return;
        activeStep = step;
        TransitionManager.beginDelayedTransition(stepperRoot);
        refreshStepperUI();
    }

    private void refreshStepperUI() {
        contentStep1.setVisibility(activeStep == 0 ? View.VISIBLE : View.GONE);
        contentStep2.setVisibility(activeStep == 1 ? View.VISIBLE : View.GONE);
        contentStep3.setVisibility(activeStep == 2 ? View.VISIBLE : View.GONE);
        
        updateIndicator(indicator1, activeStep >= 0);
        updateIndicator(indicator2, activeStep >= 1);
        updateIndicator(indicator3, activeStep >= 2);
        
        if (line1 != null) line1.setAlpha(activeStep >= 1 ? 1.0f : 0.2f);
        if (line2 != null) line2.setAlpha(activeStep >= 2 ? 1.0f : 0.2f);

        updateSummaries();
    }

    private void updateIndicator(TextView indicator, boolean active) {
        if (active) {
            indicator.setBackgroundResource(R.drawable.circle_stepper_active);
            indicator.setTextColor(android.graphics.Color.WHITE);
            indicator.setAlpha(1.0f);
        } else {
            indicator.setBackgroundResource(R.drawable.circle_stepper_inactive);
            try {
                android.util.TypedValue typedValue = new android.util.TypedValue();
                requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
                indicator.setTextColor(typedValue.data);
            } catch (Exception e) {
                indicator.setTextColor(android.graphics.Color.GRAY);
            }
            indicator.setAlpha(0.6f);
        }
    }

    private void updateSummaries() {
        String name = prefs.getString("User", "");
        String email = prefs.getString("email", "");
        summary1.setText(name.isEmpty() ? "Nom, Email, SIREN..." : name + " • " + email);

        String chauffeur = prefs.getString("chauffeur", "");
        String plaque = prefs.getString("plaque", "");
        summary2.setText(chauffeur.isEmpty() ? "Chauffeur, Plaque, EVTC..." : chauffeur + " • " + plaque);

        String iban = prefs.getString("iban", "");
        summary3.setText(iban.isEmpty() ? "IBAN, BIC..." : "IBAN: •••• " + (iban.length() > 4 ? iban.substring(iban.length() - 4) : iban));
    }

    private TextInputEditText initItem(View itemView, String key, int iconRes, String label, int inputType) {
        ImageView icon = itemView.findViewById(R.id.item_icon);
        TextView txtLabel = itemView.findViewById(R.id.item_label);
        TextInputEditText input = itemView.findViewById(R.id.item_input);

        icon.setImageResource(iconRes);
        try {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
            icon.setColorFilter(typedValue.data);
        } catch (Exception ignored) {}

        txtLabel.setText(label);
        input.setInputType(inputType);
        input.setText(prefs.getString(key, ""));

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                prefs.edit().putString(key, s.toString()).apply();
                updateSummaries();
            }
        });
        return input;
    }

    private void loadLogo() {
        String logoUri = prefs.getString("logo_uri", null);
        if (logoUri != null) {
            imgLogo.setImageURI(Uri.parse(logoUri));
            imgLogo.setPadding(0, 0, 0, 0);
            txtLogoStatus.setText("Logo configuré");
        }
    }

    private void setupZipCodeLookup() {
        if (editZip != null) {
            editZip.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (s.length() >= 5) {
                        FetchVilleFromCodePostale.fetchDataFromApiWithParams(s.toString(), editCity, editCountry);
                    }
                }
            });
        }
    }

    private void saveLogoLocally(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return;
            File file = new File(requireContext().getFilesDir(), "company_logo.png");
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            prefs.edit().putString("logo_uri", file.getAbsolutePath()).apply();
            imgLogo.setImageURI(Uri.fromFile(file));
            imgLogo.setPadding(0, 0, 0, 0);
            txtLogoStatus.setText("Logo mis à jour");
            if (isAdded()) Snackbar.make(requireView(), "Logo mis à jour", Snackbar.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("LOGO_SAVE", "Error saving logo", e);
        }
    }
}

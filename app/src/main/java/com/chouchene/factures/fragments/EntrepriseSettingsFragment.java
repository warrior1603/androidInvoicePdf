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
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
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

        // One-time emergency cleanup for corrupted data
        cleanCorruptedData();

        View btnBack = view.findViewById(R.id.btn_back_header);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (isAdded()) {
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            });
        }

        // Binding and initializing items
        initItem(view.findViewById(R.id.item_name), "User", R.drawable.ic_typcn_clients, getString(R.string.label_full_name), InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        initItem(view.findViewById(R.id.item_email), "email", R.drawable.ic_typcn_mail, getString(R.string.label_email), InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        initItem(view.findViewById(R.id.item_phone), "tel", R.drawable.ic_typcn_phone, getString(R.string.label_contact_number), InputType.TYPE_CLASS_PHONE);
        initItem(view.findViewById(R.id.item_street), "Street", R.drawable.ic_typcn_directions, getString(R.string.label_street), InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        
        editZip = initItem(view.findViewById(R.id.item_zip), "codePostale", R.drawable.ic_outline_hash, getString(R.string.label_postal_code), InputType.TYPE_CLASS_NUMBER);
        editCity = initItem(view.findViewById(R.id.item_city), "City", R.drawable.ic_outline_building, getString(R.string.label_city), InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editCountry = initItem(view.findViewById(R.id.item_country), "Country", R.drawable.ic_typcn_world, getString(R.string.label_country), InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        
        initItem(view.findViewById(R.id.item_siren), "siren", R.drawable.ic_outline_adjustments, getString(R.string.label_siren), InputType.TYPE_CLASS_NUMBER);
        initItem(view.findViewById(R.id.item_tva), "tva", R.drawable.ic_outline_cash, getString(R.string.label_tva), InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        
        initItem(view.findViewById(R.id.item_chauffeur), "chauffeur", R.drawable.ic_typcn_user, getString(R.string.label_driver_name), InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        initItem(view.findViewById(R.id.item_plaque), "plaque", R.drawable.ic_outline_hash, getString(R.string.label_plate_number), InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        initItem(view.findViewById(R.id.item_evtc), "evtc", R.drawable.ic_typcn_document, getString(R.string.label_evtc_number), InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        
        initItem(view.findViewById(R.id.item_iban), "iban", R.drawable.ic_outline_cash, "IBAN", InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        initItem(view.findViewById(R.id.item_bic), "bic", R.drawable.ic_outline_building, "BIC", InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        initItem(view.findViewById(R.id.item_bank_address), "bankAddress", R.drawable.ic_outline_adjustments, getString(R.string.label_bank_address), InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);

        imgLogo = view.findViewById(R.id.img_logo);

        loadLogo();

        view.findViewById(R.id.row_logo).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        view.findViewById(R.id.btn_save_info).setOnClickListener(v -> {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Informations enregistrées", Toast.LENGTH_SHORT).show();
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        setupZipCodeLookup();
        applyEntranceAnimations(view);
    }

    private void applyEntranceAnimations(View view) {
        View content = view.findViewById(R.id.entreprise_content_scroll);
        if (content instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) content;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
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

    private void cleanCorruptedData() {
        String userName = prefs.getString("User", "");
        String email = prefs.getString("email", "");
        if (!userName.isEmpty() && userName.equals(email) && userName.length() > 20) {
            prefs.edit().clear().apply();
        }
    }

    private TextInputEditText initItem(View itemView, String key, int iconRes, String label, int inputType) {
        if (itemView == null) return null;
        ImageView icon = itemView.findViewById(R.id.item_icon);
        TextView txtLabel = itemView.findViewById(R.id.item_label);
        TextInputEditText input = itemView.findViewById(R.id.item_input);

        if (icon != null) icon.setImageResource(iconRes);
        if (txtLabel != null) txtLabel.setText(label);
        if (input != null) {
            input.setInputType(inputType);
            input.setText(prefs.getString(key, ""));

            input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    prefs.edit().putString(key, s.toString()).apply();
                }
            });
        }
        return input;
    }

    private void loadLogo() {
        String logoUri = prefs.getString("logo_uri", null);
        if (logoUri != null) {
            imgLogo.setImageURI(Uri.parse(logoUri));
            imgLogo.setPadding(0, 0, 0, 0);
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
        Context context = getContext();
        if (context == null) return;
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return;
            File file = new File(context.getFilesDir(), "company_logo.png");
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush(); outputStream.close(); inputStream.close();
            prefs.edit().putString("logo_uri", file.getAbsolutePath()).apply();
            
            if (isAdded() && getView() != null) {
                imgLogo.setImageURI(Uri.fromFile(file));
                imgLogo.setPadding(0, 0, 0, 0);
                Snackbar.make(getView(), "Logo mis à jour", Snackbar.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e("LOGO_SAVE", "Error saving logo", e);
        }
    }
}
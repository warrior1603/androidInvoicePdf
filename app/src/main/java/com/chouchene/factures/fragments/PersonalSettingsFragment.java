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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.chouchene.factures.R;
import com.chouchene.factures.api.FetchVilleFromCodePostale;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PersonalSettingsFragment extends Fragment {

    private TextInputEditText txtUserName, txtStreet, txtCity, txtCodePostale, txtCountry, txtSiren, txtTva, txtTel, txtEmail;
    private TextInputEditText txtChauffeur, txtPlaque, txtEvtc;
    private TextInputEditText txtIban, txtBic, txtBankAddress;
    private ShapeableImageView imgLogo;
    private MaterialButton btnPickLogo, btnSaveInfo;

    private String logoUri;
    private SharedPreferences sharedPreferences;

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
        return inflater.inflate(R.layout.activity_personal_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        // One-time emergency cleanup for corrupted data
        cleanCorruptedData();

        imgLogo = view.findViewById(R.id.img_logo);
        view.findViewById(R.id.row_logo).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        logoUri = sharedPreferences.getString("logo_uri", null);
        if (logoUri != null) {
            imgLogo.setImageURI(Uri.parse(logoUri));
            imgLogo.setPadding(0, 0, 0, 0);
        }

        txtUserName = initItem(view.findViewById(R.id.item_name), "User", R.drawable.ic_typcn_clients, getString(R.string.label_full_name), InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        txtStreet = initItem(view.findViewById(R.id.item_street), "Street", R.drawable.ic_route_outline, getString(R.string.label_street), InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        txtCodePostale = initItem(view.findViewById(R.id.item_zip), "codePostale", R.drawable.ic_outline_folder, getString(R.string.label_postal_code), InputType.TYPE_CLASS_NUMBER);
        txtCity = initItem(view.findViewById(R.id.item_city), "City", R.drawable.ic_outline_building, getString(R.string.label_city), InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        txtCountry = initItem(view.findViewById(R.id.item_country), "Country", R.drawable.ic_tab_world, getString(R.string.label_country), InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        txtSiren = initItem(view.findViewById(R.id.item_siren), "siren", R.drawable.ic_outline_adjustments, getString(R.string.label_siren), InputType.TYPE_CLASS_NUMBER);
        txtTva = initItem(view.findViewById(R.id.item_tva), "tva", R.drawable.ic_outline_cash, getString(R.string.label_tva), InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        txtTel = initItem(view.findViewById(R.id.item_phone), "tel", R.drawable.ic_phone_outline, getString(R.string.label_contact_number), InputType.TYPE_CLASS_PHONE);
        txtEmail = initItem(view.findViewById(R.id.item_email), "email", R.drawable.ic_outline_mail, getString(R.string.label_email), InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        txtChauffeur = initItem(view.findViewById(R.id.item_chauffeur), "chauffeur", R.drawable.ic_typcn_clients, getString(R.string.label_driver_name), InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        txtPlaque = initItem(view.findViewById(R.id.item_plaque), "plaque", R.drawable.ic_route_outline, getString(R.string.label_plate_number), InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        txtEvtc = initItem(view.findViewById(R.id.item_evtc), "evtc", R.drawable.ic_outline_receipt, getString(R.string.label_evtc_number), InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

        txtIban = initItem(view.findViewById(R.id.item_iban), "iban", R.drawable.ic_outline_cash, "IBAN", InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        txtBic = initItem(view.findViewById(R.id.item_bic), "bic", R.drawable.ic_outline_building, "BIC", InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        txtBankAddress = initItem(view.findViewById(R.id.item_bank_address), "bankAddress", R.drawable.ic_outline_adjustments, getString(R.string.label_bank_address), InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);

        btnSaveInfo = view.findViewById(R.id.btn_save_info);
        btnSaveInfo.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            saveUserInfo();
            Snackbar.make(v, "Informations enregistrées avec succès.", Snackbar.LENGTH_LONG).show();
        });

        txtCodePostale.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() >= 5) FetchVilleFromCodePostale.fetchDataFromApiWithParams(s.toString(), txtCity, txtCountry);
            }
        });
    }

    private void cleanCorruptedData() {
        String userName = sharedPreferences.getString("User", "");
        String email = sharedPreferences.getString("email", "");
        
        // If multiple distinct fields have the exact same long string, it's corrupted
        if (!userName.isEmpty() && userName.equals(email) && userName.length() > 20) {
            sharedPreferences.edit()
                .remove("User")
                .remove("email")
                .remove("tel")
                .remove("Street")
                .remove("City")
                .remove("codePostale")
                .remove("siren")
                .remove("tva")
                .remove("chauffeur")
                .remove("plaque")
                .remove("evtc")
                .remove("iban")
                .remove("bic")
                .remove("bankAddress")
                .apply();
        }
    }

    private TextInputEditText initItem(View itemView, String key, int iconRes, String label, int inputType) {
        ImageView icon = itemView.findViewById(R.id.item_icon);
        TextView txtLabel = itemView.findViewById(R.id.item_label);
        TextInputEditText input = itemView.findViewById(R.id.item_input);

        icon.setImageResource(iconRes);
        txtLabel.setText(label);
        input.setInputType(inputType);
        input.setText(sharedPreferences.getString(key, ""));

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                sharedPreferences.edit().putString(key, s.toString()).apply();
            }
        });
        return input;
    }

    private void saveUserInfo() {
        // Already auto-saving via TextWatcher, but we can perform manual sync here if needed.
    }

    private void saveLogoLocally(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return;
            File file = new File(requireContext().getFilesDir(), "company_logo.png");
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, bytesRead);
            outputStream.flush(); outputStream.close(); inputStream.close();

            logoUri = file.getAbsolutePath();
            imgLogo.setImageURI(Uri.fromFile(file));
            imgLogo.setPadding(0, 0, 0, 0);
            sharedPreferences.edit().putString("logo_uri", logoUri).apply();
            Snackbar.make(requireView(), "Logo mis à jour", Snackbar.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("LOGO_SAVE", "Error saving logo", e);
        }
    }
}

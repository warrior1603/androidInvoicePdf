package com.chouchene.factures.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.chouchene.factures.R;
import com.chouchene.factures.api.ApiService;
import com.chouchene.factures.api.FetchVilleFromCodePostale;
import com.chouchene.factures.api.VilleDataModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PersonalSettingsFragment extends Fragment implements View.OnClickListener {
    public PersonalSettingsFragment(){
    }

    TextInputLayout txtUserName;
    TextInputLayout txtStreet;
    TextInputEditText txtCity;
    TextInputEditText txtCodePostale;
    TextInputEditText txtCountry;
    TextInputLayout txtSiren;
    TextInputLayout txtTva;
    TextInputLayout txtTel;
    TextInputEditText txtEmail;

    TextInputLayout txtChauffeur;
    TextInputLayout txtPlaque;
    TextInputLayout txtEvtc;

    String userName;
    String street;
    String city;
    String codePostale;
    String country;
    String siren;
    String tva;
    String tel;
    String email;

    String chauffeur;
    String plaque;
    String evtc;

    Button btnSaveInfo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("Entreprise et chauffeur");
        View myView = inflater.inflate(R.layout.activity_personal_settings, container, false);
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        MaterialToolbar toolbar = requireActivity().findViewById(R.id.my_toolbar);
        toolbar.setNavigationIcon(R.drawable.building_office_svgrepo_com);

        String retrievedUser = sharedPreferences.getString("User", "");
        txtUserName = myView.findViewById(R.id.edit_user_name);
        txtUserName.getEditText().setText(retrievedUser);
        String retrievedStreet = sharedPreferences.getString("Street", "");
        txtStreet = myView.findViewById(R.id.edit_street_name);
        txtStreet.getEditText().setText(retrievedStreet);
        String retrievedCity = sharedPreferences.getString("City", "");
        txtCity = myView.findViewById(R.id.edit_city);
        txtCity.setText(retrievedCity);
        String retrievedCodePostale = sharedPreferences.getString("codePostale", "");
        txtCodePostale = myView.findViewById(R.id.edit_code_postale);
        txtCodePostale.setText(retrievedCodePostale);
        String retrievedCountry = sharedPreferences.getString("Country", "");
        txtCountry = myView.findViewById(R.id.edit_country);
        txtCountry.setText(retrievedCountry);
        String retrievedSiren = sharedPreferences.getString("siren", "");
        txtSiren = myView.findViewById(R.id.edit_siren);
        txtSiren.getEditText().setText(retrievedSiren);
        String retrievedtva = sharedPreferences.getString("tva", "");
        txtTva = myView.findViewById(R.id.edit_tva);
        txtTva.getEditText().setText(retrievedtva);
        String retrievedTel = sharedPreferences.getString("tel", "");
        txtTel = myView.findViewById(R.id.edit_tel);
        txtTel.getEditText().setText(retrievedTel);
        String retrievedEmail = sharedPreferences.getString("email", "");
        txtEmail = myView.findViewById(R.id.edit_email);
        txtEmail.setText(retrievedEmail);

        String retrievedCauffeur = sharedPreferences.getString("chauffeur", "");
        txtChauffeur = myView.findViewById(R.id.edit_chauffeur);
        txtChauffeur.getEditText().setText(retrievedCauffeur);
        String retrievedPlaque = sharedPreferences.getString("plaque", "");
        txtPlaque = myView.findViewById(R.id.edit_plaque);
        txtPlaque.getEditText().setText(retrievedPlaque);
        String retrievedEvtc = sharedPreferences.getString("evtc", "");
        txtEvtc = myView.findViewById(R.id.edit_Evtc);
        txtEvtc.getEditText().setText(retrievedEvtc);

        txtCodePostale.addTextChangedListener(new TextWatcher() {
                                                  @Override
                                                  public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                                  }

                                                  @Override
                                                  public void onTextChanged(CharSequence s, int start, int before, int count) {

                                                  }

                                                  @Override
                                                  public void afterTextChanged(Editable s) {
                                                      FetchVilleFromCodePostale.fetchDataFromApiWithParams(txtCodePostale.getText().toString(), txtCity, txtCountry);
                                                  }
                                              }

        );

        btnSaveInfo = myView.findViewById(R.id.btn_save_info);
        btnSaveInfo.setOnClickListener(this);
        return myView;
    }




    @Override
    public void onClick(View v) {
        saveUserInfo();
        Toast.makeText(getActivity().getApplicationContext(), "Informations enregistrés avec succés.", Toast.LENGTH_LONG).show();
    }

    // Saving user personal information
    private void saveUserInfo() {
        userName = txtUserName.getEditText().getText().toString();
        street = txtStreet.getEditText().getText().toString();
        city = txtCity.getText().toString();
        codePostale = txtCodePostale.getText().toString();
        country = txtCountry.getText().toString();
        siren = txtSiren.getEditText().getText().toString();
        tva = txtTva.getEditText().getText().toString();
        tel = txtTel.getEditText().getText().toString();
        email = txtEmail.getText().toString();

        chauffeur = txtChauffeur.getEditText().getText().toString();
        plaque = txtPlaque.getEditText().getText().toString();
        evtc = txtEvtc.getEditText().getText().toString();


        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (userName != null && !userName.isEmpty()) {
            editor.putString("User", userName);
        }
        if (street != null && !street.isEmpty()) {
            editor.putString("Street", street);
        }
        if (city != null && !city.isEmpty()) {
            editor.putString("City", city);
        }
        if (codePostale != null && !codePostale.isEmpty()) {
            editor.putString("codePostale", codePostale);
        }
        if (country != null && !country.isEmpty()) {
            editor.putString("Country", country);
        }
        if (siren != null && !siren.isEmpty()) {
            editor.putString("siren", siren);
        }
        if (tva != null && !tva.isEmpty()) {
            editor.putString("tva", tva);
        }
        if (tel != null && !tel.isEmpty()) {
            editor.putString("tel", tel);
        }
        if (email != null && !email.isEmpty()) {
            editor.putString("email", email);
        }
        if (chauffeur != null && !chauffeur.isEmpty()) {
            editor.putString("chauffeur", chauffeur);
        }
        if (plaque != null && !plaque.isEmpty()) {
            editor.putString("plaque", plaque);
        }
        if (evtc != null && !evtc.isEmpty()) {
            editor.putString("evtc", evtc);
        }
        editor.apply();
    }

    @Override
    public void onResume() {
        super.onResume();
//        // Check if the activity has a default ActionBar
//        if (getActivity() != null) {
//            getActivity().setTitle("   Entreprise et chauffeur");  // Set the ActionBar title
//        }
//
//        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
//        // Enable the display of the home icon
//        actionBar.setDisplayShowHomeEnabled(true);
//        actionBar.setDisplayUseLogoEnabled(true);
//        // Change the ActionBar icon
//        actionBar.setLogo(R.drawable.building_office_svgrepo_com);
    }
}
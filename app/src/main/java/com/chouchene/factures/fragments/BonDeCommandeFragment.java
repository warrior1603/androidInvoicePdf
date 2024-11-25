package com.chouchene.factures.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chouchene.factures.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

public class BonDeCommandeFragment extends Fragment {
    private static final String DIRECTORY_KEY = "directory";
    private static final String CURRENCY_KEY = "default_currency";
    public BonDeCommandeFragment() {
        // Required empty public constructor
    }

    TextInputEditText editDateCommandForm;
    TextInputEditText editTimeCommandForm;
    TextInputEditText editDatePriseForm;
    TextInputEditText editTimePriseForm;

    TextInputLayout editPassager;
    TextInputLayout editPec;
    TextInputLayout editDestination;
    TextInputLayout editTarif;
    TextInputLayout editTelPassager;

    SharedPreferences sharedPreferences;
    SharedPreferences settingsSharedPreferences;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        getActivity().setTitle("Bon de commande");
        sharedPreferences = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        settingsSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        View myView = inflater.inflate(R.layout.fragment_bon_de_commande, container, false);

        MaterialToolbar toolbar = requireActivity().findViewById(R.id.my_toolbar);
        toolbar.setNavigationIcon(R.drawable.shopping_cart_svgrepo_com);

        editDateCommandForm = myView.findViewById(R.id.edit_date_commande);
        editDateCommandForm.setOnClickListener(view -> onEditDateCommandClick(editDateCommandForm));

        editDatePriseForm = myView.findViewById(R.id.edit_date_prise);
        editDatePriseForm.setOnClickListener(view -> onEditDateCommandClick(editDatePriseForm));

        editTimeCommandForm = myView.findViewById(R.id.edit_heure_commande);
        editTimeCommandForm.setOnClickListener(view -> onEditTimeCommandClick(editTimeCommandForm));

        editTimePriseForm = myView.findViewById(R.id.edit_heure_prise);
        editTimePriseForm.setOnClickListener(view -> onEditTimeCommandClick(editTimePriseForm));

        editPassager = myView.findViewById(R.id.edit_passager);
        editPec = myView.findViewById(R.id.edit_pec);
        editDestination = myView.findViewById(R.id.edit_destination);
        editTarif = myView.findViewById(R.id.edit_tarif);
        editTelPassager = myView.findViewById(R.id.edit_tel_passager);


        MaterialButton btnCreatePDF = myView.findViewById(R.id.btn_save_info_bon);
        btnCreatePDF.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            generateBonDeCommande();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
        );

        return myView;
    }

    private void generateBonDeCommande() throws IOException {

        PDFBoxResourceLoader.init(getActivity().getApplicationContext());

        String userNameEmetteur = sharedPreferences.getString("User", "");
        String streetEmetteur = sharedPreferences.getString("Street", "");
        String cityEmetteur = sharedPreferences.getString("City", "");
        String codePostaleEmetteur = sharedPreferences.getString("codePostale", "");
        String telEmetteur = sharedPreferences.getString("tel", "");
        String emailEmetteur = sharedPreferences.getString("email", "");
        String evtc = sharedPreferences.getString("evtc", "");
        String chauffeur = sharedPreferences.getString("chauffeur", "");
        String plaque = sharedPreferences.getString("plaque", "");

        String passager = editPassager.getEditText().getText().toString();
        String telPassager = editTelPassager.getEditText().getText().toString();
        String dateCommande = editDateCommandForm.getText().toString();
        String timeCommande = editTimeCommandForm.getText().toString();
        String datePrise = editDatePriseForm.getText().toString();
        String timePrise = editTimePriseForm.getText().toString();
        String priseEnCharge = editPec.getEditText().getText().toString();
        String destination = editDestination.getEditText().getText().toString();
        String tarif = editTarif.getEditText().getText().toString();

        File templateFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "template-bon-commande-output.pdf");

        InputStream inputStream = getContext().getAssets().open("bon-de-commande.pdf");
        FileUtils.copyToFile(inputStream, templateFile);

        try {
            // Load the template with form fields
            PDDocument document = PDDocument.load(templateFile);
            // Get the AcroForm from the document
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            PDField nomEmetteurField = acroForm.getField("nomEmetteur");
            nomEmetteurField.setValue(userNameEmetteur);
            PDField rueEmetteurField = acroForm.getField("rueEmetteur");
            rueEmetteurField.setValue(streetEmetteur);
            PDField codePostaleEmetteurField = acroForm.getField("codePostaleEmetteur");
            codePostaleEmetteurField.setValue(codePostaleEmetteur);
            PDField villeEmetteurField = acroForm.getField("villeEmetteur");
            villeEmetteurField.setValue(cityEmetteur);
            PDField evtcField = acroForm.getField("numeroEVTC");
            evtcField.setValue(evtc);
            PDField telEmetteurField = acroForm.getField("telEmetteur");
            telEmetteurField.setValue(telEmetteur);
            PDField nomConducteurField = acroForm.getField("nomConducteur");
            nomConducteurField.setValue(chauffeur);
            PDField nomPassagerField = acroForm.getField("nomPassager");
            nomPassagerField.setValue(passager);
            PDField telPassagerField = acroForm.getField("telPassager");
            telPassagerField.setValue(telPassager);
            PDField commandeField = acroForm.getField("dateCommande");
            commandeField.setValue(dateCommande+" "+timeCommande);
            PDField pecField = acroForm.getField("datePriseEnCharge");
            pecField.setValue(datePrise+" "+timePrise);
            PDField LieuPecField = acroForm.getField("lieuPriseEnCharge");
            LieuPecField.setValue(priseEnCharge);
            PDField destinationField = acroForm.getField("destination");
            destinationField.setValue(destination);
            PDField tarifField = acroForm.getField("tarif");
            tarifField.setValue(tarif);
            PDField conducteurField = acroForm.getField("nomChauffeur");
            conducteurField.setValue(chauffeur);
            PDField plaqueField = acroForm.getField("plaque");
            plaqueField.setValue(plaque);


            String newDirectory = settingsSharedPreferences.getString(DIRECTORY_KEY, "");
            File downloadDirectoryFromPreference = new File(newDirectory);
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String fileName = "Bon-de-commande_"+passager.trim().replace(" ", "_")+".pdf";
            File invoiceFile = new File(!downloadDirectoryFromPreference.toString().isEmpty()?  downloadDirectoryFromPreference : downloadsDir, fileName);

            // Save the filled form
            document.save(invoiceFile);
            // Close the document
            document.close();

            navigateToFragmentPreviewPdf(invoiceFile.getAbsolutePath().toString(), emailEmetteur);

            } catch (FileNotFoundException e) {
            Log.d("mylog", "Error while writing " + e.toString());
            throw new RuntimeException(e);
            } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void onEditTimeCommandClick(TextInputEditText editTimeCommandForm) {
        showTimePickerDialog(editTimeCommandForm);
    }

    private void onEditDateCommandClick(TextInputEditText editDateCommandForm) {
        showDatePickerDialog(editDateCommandForm);
    }

    private void showTimePickerDialog(TextInputEditText editTime) {
        Calendar currentTime = Calendar.getInstance();
        MaterialTimePicker materialTimePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(currentTime.get(Calendar.HOUR_OF_DAY))
                .setMinute(currentTime.get(Calendar.MINUTE))
                .setTitleText("Selectionner l'heure")
                .build();

        materialTimePicker.show(requireActivity().getSupportFragmentManager(), "TIME_PICKER");

        materialTimePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = materialTimePicker.getHour();
                int minute = materialTimePicker.getMinute();
                // Format time to string
                String formattedTime = String.format("%02d:%02d", hour, minute);
                editTime.setText(formattedTime);
            }
        });
    }

    private void showDatePickerDialog(TextInputEditText editDate){
        MaterialDatePicker.Builder builder = MaterialDatePicker.Builder.datePicker();

        builder.setTitleText("Selectionner la date");
        builder.setSelection(MaterialDatePicker.todayInUtcMilliseconds());

        final MaterialDatePicker materialDatePicker = builder.build();

        materialDatePicker.show(requireActivity().getSupportFragmentManager(), "DATE_PICKER");

        materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener() {
            @Override
            public void onPositiveButtonClick(Object selection) {
                editDate.setText(materialDatePicker.getHeaderText());
            }
        });
    }

    private void navigateToFragmentPreviewPdf(String filePath, String recipientEmail) {
        // Create an instance of FragmentB
        Fragment fragmentWebView = new WebViewPdfFragment();

        Bundle bundle = new Bundle();
        bundle.putString("file_path", filePath.toString());
        bundle.putString("mail_client", recipientEmail);// Pass file path as String

        // Set the arguments to the FragmentB
        fragmentWebView.setArguments(bundle);
        // Get FragmentManager and start a transaction
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.flFragment, fragmentWebView)
                .addToBackStack(null)
                .commit();
    }


    @Override
    public void onResume() {
        super.onResume();
//        // Check if the activity has a default ActionBar
//        if (getActivity() != null) {
//            getActivity().setTitle("   Bon de commande");  // Set the ActionBar title
//        }
//        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
//        // Enable the display of the home icon
//        actionBar.setDisplayShowHomeEnabled(true);
//        actionBar.setDisplayUseLogoEnabled(true);
//        // Change the ActionBar icon
//        actionBar.setLogo(R.drawable.shopping_cart_svgrepo_com);
    }
}
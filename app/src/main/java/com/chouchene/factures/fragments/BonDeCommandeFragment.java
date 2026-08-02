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
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.entity.Invoice;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import androidx.room.Room;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import androidx.navigation.Navigation;
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

    TextInputEditText editPassager;
    TextInputEditText editPec;
    TextInputEditText editDestination;
    TextInputEditText editTarif;
    TextInputEditText editTelPassager;

    SharedPreferences sharedPreferences;
    SharedPreferences settingsSharedPreferences;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        sharedPreferences = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        settingsSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        View myView = inflater.inflate(R.layout.fragment_bon_de_commande, container, false);

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

        String passager = editPassager.getText().toString();
        String telPassager = editTelPassager.getText().toString();
        String dateCommande = editDateCommandForm.getText().toString();
        String timeCommande = editTimeCommandForm.getText().toString();
        String datePrise = editDatePriseForm.getText().toString();
        String timePrise = editTimePriseForm.getText().toString();
        String priseEnCharge = editPec.getText().toString();
        String destination = editDestination.getText().toString();
        String tarif = editTarif.getText().toString();

        File templateFile = new File(requireContext().getCacheDir(), "template-bon-commande.pdf");

        try (InputStream inputStream = getContext().getAssets().open("bon-de-commande.pdf")) {
            FileUtils.copyToFile(inputStream, templateFile);
        }

        try {
            // Load the template with form fields
            PDDocument document = PDDocument.load(templateFile);
            // Get the AcroForm from the document
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                throw new IOException("No AcroForm found in bon-de-commande.pdf");
            }

            setField(acroForm, "nomEmetteur", userNameEmetteur);
            setField(acroForm, "rueEmetteur", streetEmetteur);
            setField(acroForm, "codePostaleEmetteur", codePostaleEmetteur);
            setField(acroForm, "villeEmetteur", cityEmetteur);
            setField(acroForm, "numeroEVTC", evtc);
            setField(acroForm, "telEmetteur", telEmetteur);
            setField(acroForm, "nomConducteur", chauffeur);
            setField(acroForm, "nomPassager", passager);
            setField(acroForm, "telPassager", telPassager);
            setField(acroForm, "dateCommande", dateCommande+" "+timeCommande);
            setField(acroForm, "datePriseEnCharge", datePrise+" "+timePrise);
            setField(acroForm, "lieuPriseEnCharge", priseEnCharge);
            setField(acroForm, "destination", destination);
            setField(acroForm, "tarif", tarif);
            setField(acroForm, "nomChauffeur", chauffeur);
            setField(acroForm, "plaque", plaque);

            acroForm.setNeedAppearances(true);
            acroForm.flatten();


            String newDirectory = settingsSharedPreferences.getString(DIRECTORY_KEY, "");
            File downloadDirectoryFromPreference = new File(newDirectory);
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String fileName = "Bon-de-commande_"+passager.trim().replace(" ", "_")+".pdf";
            File invoiceFile = new File(!downloadDirectoryFromPreference.toString().isEmpty()?  downloadDirectoryFromPreference : downloadsDir, fileName);

            // Save the filled form
            document.save(invoiceFile);
            // Close the document
            document.close();
            templateFile.delete();

            // Save to history
            AppDatabase db = Room.databaseBuilder(requireContext(), AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build();
            double finalTarif = 0;
            try { finalTarif = Double.parseDouble(tarif); } catch (Exception ignored) {}
            db.invoiceDao().insertInvoice(new Invoice(finalTarif, new java.util.Date(), passager, invoiceFile.getAbsolutePath(), "Bon"));

            navigateToFragmentPreviewPdf(invoiceFile.getAbsolutePath().toString(), emailEmetteur);

            } catch (FileNotFoundException e) {
            Log.d("mylog", "Error while writing " + e.toString());
            throw new RuntimeException(e);
            } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void setField(PDAcroForm form, String fieldName, String value) throws IOException {
        PDField field = form.getField(fieldName);
        if (field != null) {
            field.setValue(value != null ? value : "");
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
        Bundle bundle = new Bundle();
        bundle.putString("file_path", filePath);
        bundle.putString("mail_client", recipientEmail);
        Navigation.findNavController(requireView()).navigate(R.id.webViewPdfFragment, bundle);
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
//        actionBar.setLogo(R.drawable.baseline_post_add_24);
    }
}
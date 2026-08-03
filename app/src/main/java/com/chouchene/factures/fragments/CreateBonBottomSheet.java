package com.chouchene.factures.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;
import androidx.room.Room;

import com.chouchene.factures.R;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.entity.Invoice;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
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
import java.util.Date;
import java.util.Locale;

public class CreateBonBottomSheet extends BottomSheetDialogFragment {

    private static final String DIRECTORY_KEY = "directory";

    private TextInputEditText editDateCommandForm, editTimeCommandForm, editDatePriseForm, editTimePriseForm;
    private TextInputEditText editPassager, editPec, editDestination, editTarif, editTelPassager;

    private SharedPreferences sharedPreferences, settingsSharedPreferences;
    private OnBonGeneratedListener listener;

    public interface OnBonGeneratedListener {
        void onBonGenerated();
    }

    public void setOnBonGeneratedListener(OnBonGeneratedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        settingsSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        return inflater.inflate(R.layout.bottom_sheet_create_bon, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editDateCommandForm = view.findViewById(R.id.edit_date_commande);
        editDateCommandForm.setOnClickListener(v -> showDatePickerDialog(editDateCommandForm));

        editDatePriseForm = view.findViewById(R.id.edit_date_prise);
        editDatePriseForm.setOnClickListener(v -> showDatePickerDialog(editDatePriseForm));

        editTimeCommandForm = view.findViewById(R.id.edit_heure_commande);
        editTimeCommandForm.setOnClickListener(v -> showTimePickerDialog(editTimeCommandForm));

        editTimePriseForm = view.findViewById(R.id.edit_heure_prise);
        editTimePriseForm.setOnClickListener(v -> showTimePickerDialog(editTimePriseForm));

        editPassager = view.findViewById(R.id.edit_passager);
        editPec = view.findViewById(R.id.edit_pec);
        editDestination = view.findViewById(R.id.edit_destination);
        editTarif = view.findViewById(R.id.edit_tarif);
        editTelPassager = view.findViewById(R.id.edit_tel_passager);

        MaterialButton btnCreatePDF = view.findViewById(R.id.btn_save_info_bon);
        btnCreatePDF.setOnClickListener(v -> {
            try {
                generateBonDeCommande();
            } catch (IOException e) {
                Log.e("BON_GEN", "Error", e);
            }
        });
    }

    private void generateBonDeCommande() throws IOException {
        PDFBoxResourceLoader.init(requireContext().getApplicationContext());

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
        try (InputStream inputStream = requireContext().getAssets().open("bon-de-commande.pdf")) {
            FileUtils.copyToFile(inputStream, templateFile);
        }

        try {
            PDDocument document = PDDocument.load(templateFile);
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm == null) throw new IOException("No AcroForm found");

            setField(acroForm, "nomEmetteur", userNameEmetteur);
            setField(acroForm, "rueEmetteur", streetEmetteur);
            setField(acroForm, "codePostaleEmetteur", codePostaleEmetteur);
            setField(acroForm, "villeEmetteur", cityEmetteur);
            setField(acroForm, "numeroEVTC", evtc);
            setField(acroForm, "telEmetteur", telEmetteur);
            setField(acroForm, "nomConducteur", chauffeur);
            setField(acroForm, "nomPassager", passager);
            setField(acroForm, "telPassager", telPassager);
            setField(acroForm, "dateCommande", dateCommande + " " + timeCommande);
            setField(acroForm, "datePriseEnCharge", datePrise + " " + timePrise);
            setField(acroForm, "lieuPriseEnCharge", priseEnCharge);
            setField(acroForm, "destination", destination);
            setField(acroForm, "tarif", tarif);
            setField(acroForm, "nomChauffeur", chauffeur);
            setField(acroForm, "plaque", plaque);

            acroForm.setNeedAppearances(true);
            acroForm.flatten();

            String newDirectory = settingsSharedPreferences.getString(DIRECTORY_KEY, "");
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String fileName = "Bon-de-commande_" + passager.trim().replace(" ", "_") + ".pdf";
            File invoiceFile = new File(!newDirectory.isEmpty() ? new File(newDirectory) : downloadsDir, fileName);

            document.save(invoiceFile);
            document.close();
            templateFile.delete();

            AppDatabase db = Room.databaseBuilder(requireContext(), AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build();
            double finalTarif = 0;
            try { finalTarif = Double.parseDouble(tarif); } catch (Exception ignored) {}
            db.invoiceDao().insertInvoice(new Invoice(finalTarif, new Date(), passager, invoiceFile.getAbsolutePath(), "Bon"));

            if (listener != null) listener.onBonGenerated();
            navigateToFragmentPreviewPdf(invoiceFile.getAbsolutePath(), emailEmetteur);
            dismiss();

        } catch (IOException e) {
            Log.e("BON_GEN", "Error", e);
        }
    }

    private void setField(PDAcroForm form, String fieldName, String value) throws IOException {
        PDField field = form.getField(fieldName);
        if (field != null) field.setValue(value != null ? value : "");
    }

    private void showTimePickerDialog(TextInputEditText editTime) {
        Calendar currentTime = Calendar.getInstance();
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(currentTime.get(Calendar.HOUR_OF_DAY))
                .setMinute(currentTime.get(Calendar.MINUTE))
                .setTitleText("Sélectionner l'heure")
                .build();
        picker.show(requireActivity().getSupportFragmentManager(), "TIME_PICKER");
        picker.addOnPositiveButtonClickListener(v -> editTime.setText(String.format(Locale.getDefault(), "%02d:%02d", picker.getHour(), picker.getMinute())));
    }

    private void showDatePickerDialog(TextInputEditText editDate) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().setTitleText("Sélectionner la date").setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build();
        picker.show(requireActivity().getSupportFragmentManager(), "DATE_PICKER");
        picker.addOnPositiveButtonClickListener(sel -> editDate.setText(picker.getHeaderText()));
    }

    private void navigateToFragmentPreviewPdf(String path, String mail) {
        Bundle b = new Bundle();
        b.putString("file_path", path);
        b.putString("mail_client", mail);
        NavHostFragment.findNavController(this).navigate(R.id.webViewPdfFragment, b);
    }
}

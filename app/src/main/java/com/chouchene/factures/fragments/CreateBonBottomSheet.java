package com.chouchene.factures.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintResultCallbackShim;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.chouchene.factures.R;
import com.google.android.material.textfield.TextInputLayout;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.utils.BackupUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class CreateBonBottomSheet extends BottomSheetDialogFragment {

    private static final String DIRECTORY_KEY = "directory";

    private TextInputEditText editDateCommandForm, editTimeCommandForm, editDatePriseForm, editTimePriseForm;
    private TextInputEditText editPassager, editPec, editDestination, editTarif, editTelPassager, editVia;

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

        TextInputLayout passagerInput = view.findViewById(R.id.passager_input_layout);
        if (passagerInput != null) {
            passagerInput.setEndIconOnClickListener(v -> showClientPicker());
            editPassager = (com.google.android.material.textfield.TextInputEditText) passagerInput.getEditText();
        }
        editPec = view.findViewById(R.id.edit_pec);
        editDestination = view.findViewById(R.id.edit_destination);
        editTarif = view.findViewById(R.id.edit_tarif);
        editTelPassager = view.findViewById(R.id.edit_tel_passager);
        editVia = view.findViewById(R.id.edit_via);

        MaterialButton btnCreatePDF = view.findViewById(R.id.btn_save_info_bon);
        btnCreatePDF.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            try {
                generateBonDeCommande();
            } catch (IOException e) {
                Log.e("BON_GEN", "Error", e);
            }
        });
    }

    private void showClientPicker() {
        ClientPickerBottomSheet picker = new ClientPickerBottomSheet();
        picker.setOnClientSelectedListener(client -> {
            editPassager.setText(client.clientName);
            editTelPassager.setText(client.phone);
        });
        picker.show(getChildFragmentManager(), "CLIENT_PICKER");
    }

    private void generateBonDeCommande() throws IOException {
        String userNameEmetteur = sharedPreferences.getString("User", "");
        String streetEmetteur = sharedPreferences.getString("Street", "");
        String cityEmetteur = sharedPreferences.getString("City", "");
        String codePostaleEmetteur = sharedPreferences.getString("codePostale", "");
        String telEmetteur = sharedPreferences.getString("tel", "");
        String emailEmetteur = sharedPreferences.getString("email", "");
        String evtc = sharedPreferences.getString("evtc", "");
        String chauffeur = sharedPreferences.getString("chauffeur", "");
        String plaque = sharedPreferences.getString("plaque", "");
        String siren = sharedPreferences.getString("siren", "");
        String tva = sharedPreferences.getString("tva", "");

        String passager = editPassager.getText().toString();
        String telPassager = editTelPassager.getText().toString();
        String dateCommande = editDateCommandForm.getText().toString();
        String timeCommande = editTimeCommandForm.getText().toString();
        String datePrise = editDatePriseForm.getText().toString();
        String timePrise = editTimePriseForm.getText().toString();
        String priseEnCharge = editPec.getText().toString();
        String destination = editDestination.getText().toString();
        String tarif = editTarif.getText().toString();
        String via = editVia.getText().toString();

        String html = loadHtmlFromAssets("order_template.html");

        // Handle Company Logo (Base64)
        String logoPath = sharedPreferences.getString("logo_uri", null);
        String logoHtml = "";
        if (logoPath != null) {
            File logoFile = new File(logoPath);
            if (logoFile.exists()) {
                try {
                    byte[] bytes = new byte[(int) logoFile.length()];
                    try (FileInputStream fis = new FileInputStream(logoFile)) {
                        fis.read(bytes);
                    }
                    String base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
                    logoHtml = "<img src=\"data:image/png;base64," + base64 + "\" style=\"max-height: 60px;\">";
                } catch (IOException e) {
                    Log.e("BON", "Error encoding logo", e);
                }
            }
        }

        html = html.replace("{{companyLogo}}", logoHtml)
                   .replace("{{nomEmetteur}}", userNameEmetteur)
                   .replace("{{rueEmetteur}}", streetEmetteur)
                   .replace("{{codePostaleEmetteur}}", codePostaleEmetteur)
                   .replace("{{villeEmetteur}}", cityEmetteur)
                   .replace("{{telEmetteur}}", telEmetteur)
                   .replace("{{numeroEVTC}}", evtc)
                   .replace("{{issuerSiren}}", siren)
                   .replace("{{issuerTva}}", tva)
                   .replace("{{nomConducteur}}", chauffeur)
                   .replace("{{nomPassager}}", passager)
                   .replace("{{telPassager}}", telPassager)
                   .replace("{{dateCommande}}", dateCommande + " " + timeCommande)
                   .replace("{{datePriseEnCharge}}", datePrise + " " + timePrise)
                   .replace("{{lieuPriseEnCharge}}", priseEnCharge)
                   .replace("{{destination}}", destination)
                   .replace("{{tarif}}", tarif)
                   .replace("{{via}}", via.isEmpty() ? "" : via)
                   .replace("{{nomChauffeur}}", chauffeur)
                   .replace("{{plaque}}", plaque);

        String fileName = "Bon-de-commande_" + passager.trim().replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf";
        String dirPath = settingsSharedPreferences.getString(DIRECTORY_KEY, BackupUtils.getDefaultPdfDir(requireContext()));
        File outFile = new File(dirPath, fileName);

        createPdfFromHtml(html, outFile, passager, emailEmetteur);
    }

    private void createPdfFromHtml(String html, File outFile, String clientName, String clientEmail) {
        WebView webView = new WebView(requireContext());
        webView.layout(0, 0, 1024, 1448);
        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    PrintAttributes attributes = new PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build();

                    PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter("BonDeCommande");
                    adapter.onLayout(null, attributes, null, new PrintResultCallbackShim.LayoutResultCallbackShim() {
                        @Override
                        public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                            try {
                                ParcelFileDescriptor pfd = ParcelFileDescriptor.open(outFile, ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
                                adapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, pfd, null, new PrintResultCallbackShim.WriteResultCallbackShim() {
                                    @Override
                                    public void onWriteFinished(PageRange[] pages) {
                                        try {
                                            pfd.close();
                                            saveToDatabase(clientName, outFile.getAbsolutePath(), clientEmail);
                                        } catch (IOException e) {
                                            Log.e("PDF", "Error closing PFD", e);
                                        }
                                    }
                                    @Override
                                    public void onWriteFailed(CharSequence error) {
                                        if (getContext() != null) Toast.makeText(getContext(), "Erreur PDF", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (Exception e) {
                                Log.e("PDF", "Error", e);
                            }
                        }
                    }, null);
                }, 1000);
            }
        });
    }

    private void saveToDatabase(String clientName, String path, String email) {
        String tarifStr = editTarif.getText().toString();
        double amount = 0;
        try { amount = Double.parseDouble(tarifStr); } catch (Exception ignored) {}
        
        final double finalAmount = amount;
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase();
            db.invoiceDao().insertInvoice(new Invoice(finalAmount, new Date(), clientName, path, "Bon"));
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (listener != null) listener.onBonGenerated();
                    showSuccessSnackbar(path, email);
                    dismiss();
                });
            }
        });
    }

    private void showSuccessSnackbar(String filePath, String clientEmail) {
        Activity activity = getActivity();
        if (activity != null) {
            View rootView = activity.findViewById(android.R.id.content);
            if (rootView != null) {
                Snackbar.make(rootView, "Bon de commande créé", Snackbar.LENGTH_LONG)
                        .setAction("OUVRIR", v -> {
                            Bundle b = new Bundle();
                            b.putString("file_path", filePath);
                            b.putString("mail_client", clientEmail);
                            Navigation.findNavController(activity, R.id.nav_host_fragment)
                                    .navigate(R.id.webViewPdfFragment, b);
                        })
                        .show();
            }
        }
    }

    private String loadHtmlFromAssets(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open(fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
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
        picker.addOnPositiveButtonClickListener(sel -> {
             SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
             editDate.setText(sdf.format(new Date(sel)));
        });
    }
}

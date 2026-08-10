package com.chouchene.factures.fragments;

import android.app.Activity;
import android.app.Dialog;
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
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.chouchene.factures.R;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.utils.BackupUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
    
    private ViewFlipper viewFlipper;
    private MaterialButton btnBack, btnNext, btnSave;
    private TextView stepNumber1, stepNumber2, stepNumber3;
    private TextView stepLabel1, stepLabel2, stepLabel3;
    private int currentStep = 0;

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

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            FrameLayout bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupInputs(view);
        setupStepper(view);

        TextInputLayout passagerInput = view.findViewById(R.id.passager_input_layout);
        if (passagerInput != null) {
            passagerInput.setEndIconOnClickListener(v -> showClientPicker());
            editPassager = (com.google.android.material.textfield.TextInputEditText) passagerInput.getEditText();
        }
    }

    private void setupInputs(View view) {
        editDateCommandForm = view.findViewById(R.id.edit_date_commande);
        editDateCommandForm.setOnClickListener(v -> showDatePickerDialog(editDateCommandForm));

        editDatePriseForm = view.findViewById(R.id.edit_date_prise);
        editDatePriseForm.setOnClickListener(v -> showDatePickerDialog(editDatePriseForm));

        editTimeCommandForm = view.findViewById(R.id.edit_heure_commande);
        editTimeCommandForm.setOnClickListener(v -> showTimePickerDialog(editTimeCommandForm));

        editTimePriseForm = view.findViewById(R.id.edit_heure_prise);
        editTimePriseForm.setOnClickListener(v -> showTimePickerDialog(editTimePriseForm));

        editPec = view.findViewById(R.id.edit_pec);
        editDestination = view.findViewById(R.id.edit_destination);
        editTarif = view.findViewById(R.id.edit_tarif);
        editTelPassager = view.findViewById(R.id.edit_tel_passager);
        editVia = view.findViewById(R.id.edit_via);
        
        // Pre-fill today's date for order
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        editDateCommandForm.setText(sdf.format(new Date()));
        SimpleDateFormat stf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        editTimeCommandForm.setText(stf.format(new Date()));
    }

    private void setupStepper(View view) {
        viewFlipper = view.findViewById(R.id.view_flipper);
        btnBack = view.findViewById(R.id.btn_back_bon);
        btnNext = view.findViewById(R.id.btn_next_bon);
        btnSave = view.findViewById(R.id.btn_save_info_bon);

        stepNumber1 = view.findViewById(R.id.step_number_1);
        stepNumber2 = view.findViewById(R.id.step_number_2);
        stepNumber3 = view.findViewById(R.id.step_number_3);
        stepLabel1 = view.findViewById(R.id.step_label_1);
        stepLabel2 = view.findViewById(R.id.step_label_2);
        stepLabel3 = view.findViewById(R.id.step_label_3);

        btnNext.setOnClickListener(v -> goToNextStep());
        btnBack.setOnClickListener(v -> goToPreviousStep());
        btnSave.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            try {
                generateBonDeCommande();
            } catch (IOException e) {
                Log.e("BON_GEN", "Error", e);
            }
        });
        
        updateStepperUI();
    }

    private void goToNextStep() {
        if (currentStep == 0) {
            if (editPassager.getText().toString().trim().isEmpty()) {
                editPassager.setError(getString(R.string.label_required));
                return;
            }
        } else if (currentStep == 1) {
            if (editPec.getText().toString().trim().isEmpty()) {
                editPec.setError(getString(R.string.label_required)); return;
            }
            if (editDestination.getText().toString().trim().isEmpty()) {
                editDestination.setError(getString(R.string.label_required)); return;
            }
        }
        
        if (currentStep < 2) {
            currentStep++;
            viewFlipper.setInAnimation(requireContext(), R.anim.slide_in_right);
            viewFlipper.setOutAnimation(requireContext(), R.anim.slide_out_left);
            viewFlipper.showNext();
            updateStepperUI();
        }
    }

    private void goToPreviousStep() {
        if (currentStep > 0) {
            currentStep--;
            viewFlipper.setInAnimation(requireContext(), android.R.anim.slide_in_left);
            viewFlipper.setOutAnimation(requireContext(), android.R.anim.slide_out_right);
            viewFlipper.showPrevious();
            updateStepperUI();
        }
    }

    private void updateStepperUI() {
        btnBack.setVisibility(currentStep == 0 ? View.GONE : View.VISIBLE);
        btnNext.setVisibility(currentStep == 2 ? View.GONE : View.VISIBLE);
        btnSave.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);

        updateStepIndicator(stepNumber1, stepLabel1, currentStep >= 0);
        updateStepIndicator(stepNumber2, stepLabel2, currentStep >= 1);
        updateStepIndicator(stepNumber3, stepLabel3, currentStep >= 2);
        
        if (currentStep == 2) {
            updateSummary();
        }
    }

    private void updateStepIndicator(TextView number, TextView label, boolean active) {
        number.setBackgroundResource(active ? R.drawable.circle_stepper_active : R.drawable.circle_stepper_inactive);
        number.setTextColor(active ? android.graphics.Color.WHITE : android.graphics.Color.GRAY);
        label.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        
        int activeColor = android.graphics.Color.BLUE;
        try {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            requireContext().getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
            activeColor = typedValue.data;
        } catch (Exception ignored) {}

        label.setTextColor(active ? activeColor : android.graphics.Color.GRAY);
    }

    private void updateSummary() {
        String passager = editPassager.getText().toString();
        String pec = editPec.getText().toString();
        String dest = editDestination.getText().toString();
        String date = editDatePriseForm.getText().toString();
        String time = editTimePriseForm.getText().toString();
        
        String summary = "Passager: " + passager + "\n" +
                        "Départ: " + pec + "\n" +
                        "Arrivée: " + dest + "\n" +
                        "Le: " + date + " à " + time;
        
        if (getView() != null) {
            TextView summaryView = getView().findViewById(R.id.summary_text_bon);
            if (summaryView != null) summaryView.setText(summary);
        }
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
        String logoPath = settingsSharedPreferences.getString("logo_uri", null);
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
                                        if (getContext() != null) Toast.makeText(getContext(), getString(R.string.msg_pdf_error), Toast.LENGTH_SHORT).show();
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
                Snackbar.make(rootView, getString(R.string.msg_order_created), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.action_open), v -> {
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
                .setTitleText(getString(R.string.title_choose_time))
                .build();
        picker.show(requireActivity().getSupportFragmentManager(), "TIME_PICKER");
        picker.addOnPositiveButtonClickListener(v -> editTime.setText(String.format(Locale.getDefault(), "%02d:%02d", picker.getHour(), picker.getMinute())));
    }

    private void showDatePickerDialog(TextInputEditText editDate) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().setTitleText(getString(R.string.title_choose_date)).setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build();
        picker.show(requireActivity().getSupportFragmentManager(), "DATE_PICKER");
        picker.addOnPositiveButtonClickListener(sel -> {
             SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
             editDate.setText(sdf.format(new Date(sel)));
        });
    }
}

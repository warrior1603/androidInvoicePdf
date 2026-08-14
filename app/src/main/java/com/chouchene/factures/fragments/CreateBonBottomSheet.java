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
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
    private View stepIndicator1, stepIndicator2, stepIndicator3;
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupInputs(view);
        setupStepper(view);
        updateStepperUI();
    }

    private void setupInputs(View view) {
        editPassager = initItemWithAction(view.findViewById(R.id.item_passenger_name), R.drawable.ic_nav_user_outline, "Nom du passager", InputType.TYPE_TEXT_VARIATION_PERSON_NAME, this::showClientPicker);
        editTelPassager = initItem(view.findViewById(R.id.item_passenger_phone), R.drawable.ic_outline_phone, "Téléphone", InputType.TYPE_CLASS_PHONE);
        
        editDateCommandForm = initItem(view.findViewById(R.id.item_order_date), R.drawable.rounded_calendar_today_24, "Date Commande", InputType.TYPE_NULL);
        editTimeCommandForm = initItem(view.findViewById(R.id.item_order_time), R.drawable.ic_clock_outline, "Heure Commande", InputType.TYPE_NULL);
        
        editDatePriseForm = initItem(view.findViewById(R.id.item_pickup_date), R.drawable.rounded_calendar_today_24, "Date PEC", InputType.TYPE_NULL);
        editTimePriseForm = initItem(view.findViewById(R.id.item_pickup_time), R.drawable.ic_clock_outline, "Heure PEC", InputType.TYPE_NULL);
        
        editPec = initItem(view.findViewById(R.id.item_pickup_location), R.drawable.rounded_location_on_24, "Lieu PEC", InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        editDestination = initItem(view.findViewById(R.id.item_destination), R.drawable.rounded_location_on_24, "Destination", InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        editVia = initItem(view.findViewById(R.id.item_via), R.drawable.ic_route_outline, "Via (Optionnel)", InputType.TYPE_CLASS_TEXT);
        editTarif = initItem(view.findViewById(R.id.item_fare), R.drawable.ic_outline_cash, "Tarif TTC (€)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        SimpleDateFormat dateFmt = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date now = new Date();

        editDateCommandForm.setText(dateFmt.format(now));
        editTimeCommandForm.setText(timeFmt.format(now));
        editDatePriseForm.setText(dateFmt.format(now));
        editTimePriseForm.setText(timeFmt.format(now));

        editDateCommandForm.setOnClickListener(v -> showDatePickerDialog(editDateCommandForm));
        editTimeCommandForm.setOnClickListener(v -> showTimePickerDialog(editTimeCommandForm));
        editDatePriseForm.setOnClickListener(v -> showDatePickerDialog(editDatePriseForm));
        editTimePriseForm.setOnClickListener(v -> showTimePickerDialog(editTimePriseForm));
    }

    private void setupStepper(View view) {
        viewFlipper = view.findViewById(R.id.view_flipper);
        btnBack = view.findViewById(R.id.btn_back);
        btnNext = view.findViewById(R.id.btn_next);
        btnSave = view.findViewById(R.id.btnCreateBon);

        stepIndicator1 = view.findViewById(R.id.step_indicator_1);
        stepIndicator2 = view.findViewById(R.id.step_indicator_2);
        stepIndicator3 = view.findViewById(R.id.step_indicator_3);

        btnNext.setOnClickListener(v -> goToNextStep());
        btnBack.setOnClickListener(v -> goToPreviousStep());
        btnSave.setOnClickListener(v -> {
            try { generateBonDeCommande(); } catch (IOException e) { Log.e("BON_GEN", "Error", e); }
        });
    }

    private TextInputEditText initItem(View itemView, int iconRes, String label, int inputType) {
        ImageView icon = itemView.findViewById(R.id.item_icon);
        TextView txtLabel = itemView.findViewById(R.id.item_label);
        TextInputEditText input = itemView.findViewById(R.id.item_input);
        icon.setImageResource(iconRes);
        txtLabel.setText(label);

        // Use black for labels to match Document Studio
        try {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
            txtLabel.setTextColor(typedValue.data);
            txtLabel.setAlpha(0.9f);
        } catch (Exception ignored) {}

        input.setHint(label);
        input.setInputType(inputType);
        return input;
    }

    private TextInputEditText initItemWithAction(View itemView, int iconRes, String label, int inputType, Runnable action) {
        TextInputEditText input = initItem(itemView, iconRes, label, inputType);
        View actionIcon = itemView.findViewById(R.id.item_action_icon);
        if (actionIcon != null) {
            actionIcon.setVisibility(View.VISIBLE);
            actionIcon.setOnClickListener(v -> action.run());
        }
        return input;
    }

    private void goToNextStep() {
        if (currentStep == 0 && editPassager.getText().toString().trim().isEmpty()) {
            editPassager.setError(getString(R.string.label_required)); return;
        }
        if (currentStep == 1 && (editPec.getText().toString().trim().isEmpty() || editDestination.getText().toString().trim().isEmpty())) {
            return;
        }
        if (currentStep < 2) {
            currentStep++;
            viewFlipper.showNext();
            updateStepperUI();
        }
    }

    private void goToPreviousStep() {
        if (currentStep > 0) {
            currentStep--;
            viewFlipper.showPrevious();
            updateStepperUI();
        }
    }

    private void updateStepperUI() {
        btnBack.setVisibility(currentStep == 0 ? View.GONE : View.VISIBLE);
        btnNext.setVisibility(currentStep == 2 ? View.GONE : View.VISIBLE);
        btnSave.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
        updateIndicator(stepIndicator1, currentStep >= 0);
        updateIndicator(stepIndicator2, currentStep >= 1);
        updateIndicator(stepIndicator3, currentStep >= 2);
        if (currentStep == 2) updateSummary();
    }

    private void updateIndicator(View bar, boolean active) {
        bar.setAlpha(active ? 1.0f : 0.2f);
    }

    private void updateSummary() {
        String summary = "Passager: " + editPassager.getText() + "\nPEC: " + editPec.getText() + "\nDest: " + editDestination.getText();
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
        String html = loadHtmlFromAssets("order_template.html");
        // ... Replacement logic (restored from previous state)
        String userNameEmetteur = sharedPreferences.getString("User", "");
        String streetEmetteur = sharedPreferences.getString("Street", "");
        String cityEmetteur = sharedPreferences.getString("City", "");
        String codePostaleEmetteur = sharedPreferences.getString("codePostale", "");
        String telEmetteur = sharedPreferences.getString("tel", "");
        String evtc = sharedPreferences.getString("evtc", "");
        String chauffeur = sharedPreferences.getString("chauffeur", "");
        String plaque = sharedPreferences.getString("plaque", "");
        String siren = sharedPreferences.getString("siren", "");
        String tva = sharedPreferences.getString("tva", "");

        html = html.replace("{{nomEmetteur}}", userNameEmetteur).replace("{{rueEmetteur}}", streetEmetteur)
                   .replace("{{codePostaleEmetteur}}", codePostaleEmetteur).replace("{{villeEmetteur}}", cityEmetteur)
                   .replace("{{telEmetteur}}", telEmetteur).replace("{{numeroEVTC}}", evtc)
                   .replace("{{issuerSiren}}", siren).replace("{{issuerTva}}", tva)
                   .replace("{{nomConducteur}}", chauffeur).replace("{{plaque}}", plaque)
                   .replace("{{nomPassager}}", editPassager.getText().toString())
                   .replace("{{telPassager}}", editTelPassager.getText().toString())
                   .replace("{{dateCommande}}", editDateCommandForm.getText().toString() + " " + editTimeCommandForm.getText().toString())
                   .replace("{{datePriseEnCharge}}", editDatePriseForm.getText().toString() + " " + editTimePriseForm.getText().toString())
                   .replace("{{lieuPriseEnCharge}}", editPec.getText().toString())
                   .replace("{{destination}}", editDestination.getText().toString())
                   .replace("{{tarif}}", editTarif.getText().toString())
                   .replace("{{via}}", editVia.getText().toString());

        String fileName = "Bon_" + System.currentTimeMillis() + ".pdf";
        File outFile = new File(settingsSharedPreferences.getString(DIRECTORY_KEY, BackupUtils.getDefaultPdfDir(requireContext())), fileName);
        createPdfFromHtml(html, outFile, editPassager.getText().toString(), sharedPreferences.getString("email", ""));
    }

    private void createPdfFromHtml(String html, File outFile, String clientName, String clientEmail) {
        WebView webView = new WebView(requireContext());
        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    PrintAttributes attributes = new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600)).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build();
                    PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter("Bon");
                    adapter.onLayout(null, attributes, null, new PrintResultCallbackShim.LayoutResultCallbackShim() {
                        @Override public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                            try {
                                ParcelFileDescriptor pfd = ParcelFileDescriptor.open(outFile, ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
                                adapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, pfd, null, new PrintResultCallbackShim.WriteResultCallbackShim() {
                                    @Override public void onWriteFinished(PageRange[] pages) {
                                        try { pfd.close(); saveToDatabase(clientName, outFile.getAbsolutePath(), clientEmail); } catch (IOException ignored) {}
                                    }
                                });
                            } catch (Exception ignored) {}
                        }
                    }, null);
                }, 1000);
            }
        });
    }

    private void saveToDatabase(String clientName, String path, String email) {
        double amount = 0; try { amount = Double.parseDouble(editTarif.getText().toString()); } catch (Exception ignored) {}
        final double finalAmount = amount;
        Executors.newSingleThreadExecutor().execute(() -> {
            DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase().invoiceDao().insertInvoice(new Invoice(finalAmount, new Date(), clientName, path, "Bon"));
            if (getActivity() != null) getActivity().runOnUiThread(() -> {
                if (listener != null) listener.onBonGenerated();
                com.chouchene.factures.utils.UIUtils.showSuccessDialog(requireContext(), "Bon Généré", "Le bon a été généré avec succès.", this::dismiss);
            });
        });
    }

    private String loadHtmlFromAssets(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open(fileName)))) {
            String line; while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private void showTimePickerDialog(TextInputEditText target) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H).setTitleText("Heure").build();
        picker.show(requireActivity().getSupportFragmentManager(), "TIME");
        picker.addOnPositiveButtonClickListener(v -> target.setText(String.format(Locale.getDefault(), "%02d:%02d", picker.getHour(), picker.getMinute())));
    }

    private void showDatePickerDialog(TextInputEditText target) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().build();
        picker.show(requireActivity().getSupportFragmentManager(), "DATE");
        picker.addOnPositiveButtonClickListener(sel -> target.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(sel))));
    }
}

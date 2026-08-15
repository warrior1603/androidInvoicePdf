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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import com.chouchene.factures.R;
import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.api.FetchVilleFromCodePostale;
import com.chouchene.factures.utils.BackupUtils;
import com.chouchene.factures.utils.SignatureView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class CreateInvoiceBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText txtName, txtRue, txtVille, txtCodePostale, txtPays, txtSiren, txtEmail, txtTvaClient;
    private TextInputEditText txtDescription, txtQuantite, txtPrix, txtTva, editDateFactureForm;
    private AutoCompleteTextView autoCompletePaymentMode, autoCompleteTextView;
    private LinearLayout inputClientProvisoire;
    private SignatureView signatureView;
    private ViewFlipper viewFlipper;
    private MaterialButton btnBack, btnNext, btnCreatePdf;
    private View stepIndicator1, stepIndicator2, stepIndicator3;
    private int currentStep = 0;

    private String currentInvoiceNumber;
    private SharedPreferences sharedPreferences, settingsSharedPreferences;
    private AppDatabase db;
    private ClientDao itemDao;
    private boolean isClientProvisoire = false;
    private Client selectedClient = null;

    private OnInvoiceGeneratedListener listener;

    public interface OnInvoiceGeneratedListener {
        void onInvoiceGenerated();
    }

    public void setOnInvoiceGeneratedListener(OnInvoiceGeneratedListener listener) {
        this.listener = listener;
    }

    public static CreateInvoiceBottomSheet newInstance(int clientId) {
        CreateInvoiceBottomSheet fragment = new CreateInvoiceBottomSheet();
        Bundle args = new Bundle();
        args.putInt("client_id", clientId);
        fragment.setArguments(args);
        return fragment;
    }

    public static CreateInvoiceBottomSheet newInstance(String clientName, String description, double price) {
        CreateInvoiceBottomSheet fragment = new CreateInvoiceBottomSheet();
        Bundle args = new Bundle();
        args.putString("prefill_client", clientName);
        args.putString("prefill_desc", description);
        args.putDouble("prefill_price", price);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_create_invoice, container, false);
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

        db = DatabaseClient.getInstance(requireContext()).getAppDatabase();
        itemDao = db.clientDao();
        sharedPreferences = requireActivity().getSharedPreferences("InvoicePrefs", Context.MODE_PRIVATE);
        settingsSharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        generateSequentialInvoiceNumber();

        setupInputs(view);
        setupStepper(view);
        setupClientSearch(view);
        setupRadioGroup(view);
        setupPaymentMode(view);

        if (getArguments() != null) {
            if (getArguments().containsKey("client_id")) {
                int clientId = getArguments().getInt("client_id");
                loadClientForInvoice(clientId);
            } else if (getArguments().containsKey("prefill_client")) {
                String clientName = getArguments().getString("prefill_client");
                String desc = getArguments().getString("prefill_desc");
                double price = getArguments().getDouble("prefill_price");
                
                autoCompleteTextView.setText(clientName, false);
                txtDescription.setText(desc);
                txtPrix.setText(String.valueOf(price));
            }
        }

        view.findViewById(R.id.btn_clear_signature).setOnClickListener(v -> signatureView.clear());
    }

    private void setupStepper(View view) {
        viewFlipper = view.findViewById(R.id.view_flipper);
        btnBack = view.findViewById(R.id.btn_back);
        btnNext = view.findViewById(R.id.btn_next);
        btnCreatePdf = view.findViewById(R.id.btnCreatePdf);

        stepIndicator1 = view.findViewById(R.id.step_indicator_1);
        stepIndicator2 = view.findViewById(R.id.step_indicator_2);
        stepIndicator3 = view.findViewById(R.id.step_indicator_3);

        btnNext.setOnClickListener(v -> goToNextStep());
        btnBack.setOnClickListener(v -> goToPreviousStep());
        btnCreatePdf.setOnClickListener(v -> handleGenerateInvoice());
        
        updateStepperUI();
    }

    private void goToNextStep() {
        if (currentStep == 0) {
            String clientName = isClientProvisoire ? txtName.getText().toString().trim() : (selectedClient != null ? selectedClient.clientName : autoCompleteTextView.getText().toString());
            if (clientName.isEmpty()) {
                Toast.makeText(requireContext(), "Nom du client requis", Toast.LENGTH_SHORT).show();
                return;
            }
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
        btnCreatePdf.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);

        updateIndicator(stepIndicator1, currentStep >= 0);
        updateIndicator(stepIndicator2, currentStep >= 1);
        updateIndicator(stepIndicator3, currentStep >= 2);
        
        if (currentStep == 2) {
            updateSummary();
        }
    }

    private void updateIndicator(View bar, boolean active) {
        bar.setAlpha(active ? 1.0f : 0.2f);
    }
    
    private void updateSummary() {
        String clientName = isClientProvisoire ? txtName.getText().toString().trim() : (selectedClient != null ? selectedClient.clientName : autoCompleteTextView.getText().toString());
        String desc = txtDescription.getText().toString();
        String price = txtPrix.getText().toString();
        
        String summary = "Client: " + clientName + "\n" +
                        "Description: " + desc + "\n" +
                        "Montant: " + price + " €";
        
        TextView summaryView = getView().findViewById(R.id.summary_text);
        if (summaryView != null) summaryView.setText(summary);
    }

    private void generateSequentialInvoiceNumber() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String lastDate = sharedPreferences.getString("last_date", "");
        int counter = sharedPreferences.getInt("daily_counter", 1);

        if (!today.equals(lastDate)) {
            counter = 1;
        }

        currentInvoiceNumber = today + String.format(Locale.getDefault(), "%02d", counter);
    }

    private void loadClientForInvoice(int clientId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Client client = db.clientDao().getClientById(clientId);
            if (client != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    selectedClient = client;
                    autoCompleteTextView.setText(client.clientName, false);
                    updateManualFields(client);
                });
            }
        });
    }

    private void setupPaymentMode(View view) {
        // Handled in setupInputs
    }

    private void setupClientSearch(View view) {
        // Handled in setupInputs
    }

    private void setupRadioGroup(View view) {
        RadioGroup radioGroup = view.findViewById(R.id.radio_group);
        inputClientProvisoire = view.findViewById(R.id.client_input_provisoire);
        View clientSearchItem = view.findViewById(R.id.item_search_client);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isClientProvisoire = checkedId == R.id.provisoire_selected;
            inputClientProvisoire.setVisibility(isClientProvisoire ? View.VISIBLE : View.GONE);
            clientSearchItem.setVisibility(isClientProvisoire ? View.GONE : View.VISIBLE);
        });
    }

    private void setupInputs(View view) {
        autoCompleteTextView = initDropdownItem(view.findViewById(R.id.item_search_client), R.drawable.rounded_person_24, "Rechercher un client", new String[]{});
        View clientSearchLayout = view.findViewById(R.id.item_search_client);
        ImageView clientActionIcon = clientSearchLayout.findViewById(R.id.item_action_icon);
        if (clientActionIcon != null) {
            clientActionIcon.setVisibility(View.VISIBLE);
            clientActionIcon.setOnClickListener(v -> showClientPicker());
        }

        txtName = initItem(view.findViewById(R.id.item_client_name), R.drawable.ic_nav_user_outline, "Nom du client", InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        txtRue = initItem(view.findViewById(R.id.item_client_street), R.drawable.ic_outline_road, "Rue", InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        txtCodePostale = initItem(view.findViewById(R.id.item_client_zip), R.drawable.ic_outline_hash, "Code Postal", InputType.TYPE_CLASS_NUMBER);
        txtVille = initItem(view.findViewById(R.id.item_client_city), R.drawable.ic_outline_building, "Ville", InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        txtPays = initItem(view.findViewById(R.id.item_client_country), R.drawable.ic_tab_world, "Pays", InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        txtSiren = initItem(view.findViewById(R.id.item_client_siren), R.drawable.ic_outline_adjustments, "SIREN", InputType.TYPE_CLASS_NUMBER);
        txtTvaClient = initItem(view.findViewById(R.id.item_client_tva), R.drawable.ic_outline_cash, "TVA Client", InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        txtEmail = initItem(view.findViewById(R.id.item_client_email), R.drawable.ic_outline_mail, "Email", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        editDateFactureForm = initItem(view.findViewById(R.id.item_date), R.drawable.rounded_calendar_today_24, "Date", InputType.TYPE_NULL);
        txtDescription = initItem(view.findViewById(R.id.item_description), R.drawable.ic_outline_receipt, "Description", InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        txtQuantite = initItem(view.findViewById(R.id.item_qty), R.drawable.ic_outline_hash, "Quantité", InputType.TYPE_CLASS_NUMBER);
        txtPrix = initItem(view.findViewById(R.id.item_price), R.drawable.ic_outline_cash, "Prix Unitaire TTC", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        txtTva = initItem(view.findViewById(R.id.item_tva_rate), R.drawable.ic_outline_adjustments, "Taux TVA (%)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        autoCompletePaymentMode = initDropdownItem(view.findViewById(R.id.item_payment), R.drawable.ic_outline_cash, "Mode de paiement", new String[]{"Virement", "Carte", "Espèce", "Chèque"});
        
        signatureView = view.findViewById(R.id.signature_view);
        view.findViewById(R.id.btn_clear_signature).setOnClickListener(v -> signatureView.clear());

        txtQuantite.setText("1");
        
        String defaultTva = settingsSharedPreferences.getString("default_tva", "10");
        txtTva.setText(defaultTva);

        String defaultPayment = settingsSharedPreferences.getString("default_payment", "Virement");
        autoCompletePaymentMode.setText(defaultPayment, false);

        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        editDateFactureForm.setText(fmt.format(new Date()));
        editDateFactureForm.setOnClickListener(v -> showDatePickerDialog());

        txtCodePostale.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() >= 5) com.chouchene.factures.api.FetchVilleFromCodePostale.fetchDataFromApiWithParams(s.toString(), txtVille, txtPays);
            }
        });

        // Initialize client search data
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Client> clients = itemDao.getAllClients();
            List<String> names = new ArrayList<>();
            for (Client c : clients) names.add(c.clientName);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_menu_popup_item, names);
                    autoCompleteTextView.setAdapter(adapter);
                    autoCompleteTextView.setOnItemClickListener((parent, v, position, id) -> {
                        String name = (String) parent.getItemAtPosition(position);
                        for (Client c : clients) if (c.clientName.equals(name)) selectedClient = c;
                    });
                });
            }
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

    private AutoCompleteTextView initDropdownItem(View itemView, int iconRes, String label, String[] options) {
        ImageView icon = itemView.findViewById(R.id.item_icon);
        TextView txtLabel = itemView.findViewById(R.id.item_label);
        AutoCompleteTextView dropdown = itemView.findViewById(R.id.item_dropdown);
        icon.setImageResource(iconRes);
        txtLabel.setText(label);

        // Use black for labels to match Document Studio
        try {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
            txtLabel.setTextColor(typedValue.data);
            txtLabel.setAlpha(0.9f);
        } catch (Exception ignored) {}

        if (options.length > 0) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_menu_popup_item, options);
            dropdown.setAdapter(adapter);
            dropdown.setText(options[0], false);
        }
        return dropdown;
    }

    private void showDatePickerDialog() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build();
        picker.show(requireActivity().getSupportFragmentManager(), "DP");
        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            editDateFactureForm.setText(fmt.format(new Date(selection)));
        });
    }

    private void handleGenerateInvoice() {
        String clientName = isClientProvisoire ? txtName.getText().toString().trim() : (selectedClient != null ? selectedClient.clientName : autoCompleteTextView.getText().toString());
        if (clientName.isEmpty()) {
            Toast.makeText(requireContext(), "Nom du client requis", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (txtDescription.getText().toString().trim().isEmpty()) {
            txtDescription.setError("Requis");
            return;
        }
        
        if (txtPrix.getText().toString().trim().isEmpty()) {
            txtPrix.setError("Requis");
            return;
        }

        Toast.makeText(requireContext(), "Génération de la facture...", Toast.LENGTH_SHORT).show();
        generateHtmlPdf();
    }

    private void showClientPicker() {
        ClientPickerBottomSheet picker = new ClientPickerBottomSheet();
        picker.setOnClientSelectedListener(client -> {
            autoCompleteTextView.setText(client.clientName, false);
            selectedClient = client;
            updateManualFields(client);
        });
        picker.show(getChildFragmentManager(), "CLIENT_PICKER");
    }

    private void updateManualFields(Client client) {
        if (txtName != null) txtName.setText(client.clientName);
        if (txtRue != null) txtRue.setText(client.street);
        if (txtCodePostale != null) txtCodePostale.setText(client.codePostale);
        if (txtVille != null) txtVille.setText(client.ville);
        if (txtPays != null) txtPays.setText(client.pays);
        if (txtSiren != null) txtSiren.setText(client.numeroSiren);
        if (txtTvaClient != null) txtTvaClient.setText(client.getNumeroTVA());
        if (txtEmail != null) txtEmail.setText(client.email);
    }

    private void generateHtmlPdf() {
        try {
            // First check Default Preferences (from TemplatePreviewFragment)
            SharedPreferences defaultPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext());
            String templateName = defaultPrefs.getString("invoice_template", null);
            
            // Fallback to InvoicePrefs if not set in Default
            if (templateName == null) {
                templateName = sharedPreferences.getString("invoice_template", "invoice_template.html");
            }
            
            String html = loadHtmlFromAssets(templateName);

            // Handle Company Logo
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
                        logoHtml = "<img src=\"data:image/png;base64," + base64 + "\" style=\"max-height: 80px; margin-bottom: 15px;\">";
                    } catch (IOException e) {
                        Log.e("INVOICE", "Error encoding logo", e);
                    }
                }
            }
            html = html.replace("{{companyLogo}}", logoHtml)
                       .replace("{{logoEntreprise}}", logoHtml);

            // Replace issuer tags
            html = html.replace("{{issuerName}}", settingsSharedPreferences.getString("User", ""))
                       .replace("{{issuerStreet}}", settingsSharedPreferences.getString("Street", ""))
                       .replace("{{issuerCityZip}}", settingsSharedPreferences.getString("codePostale", "") + " " + settingsSharedPreferences.getString("City", ""))
                       .replace("{{issuerCountry}}", settingsSharedPreferences.getString("Pays", "France"))
                       .replace("{{issuerSiren}}", settingsSharedPreferences.getString("siren", ""))
                       .replace("{{issuerTva}}", settingsSharedPreferences.getString("tva", ""))
                       .replace("{{issuerTel}}", settingsSharedPreferences.getString("tel", ""))
                       .replace("{{issuerEmail}}", settingsSharedPreferences.getString("email", ""));

            // Replace client tags
            String cName = isClientProvisoire ? txtName.getText().toString() : (selectedClient != null ? selectedClient.clientName : autoCompleteTextView.getText().toString());
            String cStreet = isClientProvisoire ? txtRue.getText().toString() : (selectedClient != null ? selectedClient.street : "");
            String cZip = isClientProvisoire ? txtCodePostale.getText().toString() : (selectedClient != null ? selectedClient.codePostale : "");
            String cCity = isClientProvisoire ? txtVille.getText().toString() : (selectedClient != null ? selectedClient.ville : "");
            String cCountry = isClientProvisoire ? txtPays.getText().toString() : (selectedClient != null ? selectedClient.pays : "");
            String cSiren = isClientProvisoire ? txtSiren.getText().toString() : (selectedClient != null ? selectedClient.numeroSiren : "");
            String cTva = isClientProvisoire ? txtTvaClient.getText().toString() : (selectedClient != null ? selectedClient.getNumeroTVA() : "");
            String cEmail = isClientProvisoire ? txtEmail.getText().toString() : (selectedClient != null ? selectedClient.email : "");

            html = html.replace("{{clientName}}", cName)
                       .replace("{{clientStreet}}", cStreet)
                       .replace("{{clientCityZip}}", cZip + " " + cCity)
                       .replace("{{clientCountry}}", cCountry)
                       .replace("{{clientTvaSiren}}", "SIREN: " + cSiren + " | TVA: " + cTva);

            // Calculate amounts
            double qty = 1;
            try { qty = Double.parseDouble(txtQuantite.getText().toString()); } catch (Exception e) {}
            double priceTtc = 0;
            try { priceTtc = Double.parseDouble(txtPrix.getText().toString()); } catch (Exception e) {}
            double tvaRate = 10;
            try { tvaRate = Double.parseDouble(txtTva.getText().toString()); } catch (Exception e) {}
            
            double totalTtc = qty * priceTtc;
            double totalHt = totalTtc / (1 + (tvaRate / 100));
            double totalTva = totalTtc - totalHt;
            double priceHt = totalHt / qty;

            html = html.replace("{{description}}", txtDescription.getText().toString())
                       .replace("{{qty}}", String.valueOf((int)qty))
                       .replace("{{priceHt}}", String.format(Locale.getDefault(), "%.2f €", priceHt))
                       .replace("{{tvaPercent}}", String.format(Locale.getDefault(), "%.0f%%", tvaRate))
                       .replace("{{tvaEuro}}", String.format(Locale.getDefault(), "%.2f €", totalTva))
                       .replace("{{totalHt}}", String.format(Locale.getDefault(), "%.2f €", totalHt))
                       .replace("{{totalTva}}", String.format(Locale.getDefault(), "%.2f €", totalTva))
                       .replace("{{totalTtc}}", String.format(Locale.getDefault(), "%.2f €", totalTtc))
                       .replace("{{invoiceDate}}", editDateFactureForm.getText().toString())
                       .replace("{{payementMode}}", autoCompletePaymentMode.getText().toString())
                       .replace("{{invoiceNumber}}", currentInvoiceNumber);

            String fileName = "Facture_" + cName.trim().replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf";
            String dirPath = settingsSharedPreferences.getString("directory", BackupUtils.getDefaultPdfDir(requireContext()));
            File outFile = new File(dirPath, fileName);

            createPdfFromHtml(html, outFile, totalTtc, cName, cEmail);

        } catch (Exception e) {
            Log.e("INVOICE", "Error generating HTML", e);
            Toast.makeText(requireContext(), "Erreur lors de la génération", Toast.LENGTH_SHORT).show();
        }
    }

    private void createPdfFromHtml(String html, File outFile, double amount, String clientName, String clientEmail) {
        WebView webView = new WebView(requireContext());
        // Set layout params for proper measurement
        webView.layout(0, 0, 1024, 1448);
        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Give it a moment to fully render
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    PrintAttributes attributes = new PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build();

                    PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter("Facture");
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
                                            saveToDatabase(amount, clientName, outFile.getAbsolutePath(), clientEmail);
                                        } catch (IOException e) {
                                            Log.e("PDF", "Error closing PFD", e);
                                        }
                                    }

                                    @Override
                                    public void onWriteFailed(CharSequence error) {
                                        Log.e("PDF", "Write failed: " + error);
                                        if (getContext() != null) Toast.makeText(getContext(), "Échec de l'écriture PDF", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (Exception e) {
                                Log.e("PDF", "Error starting write", e);
                            }
                        }

                        @Override
                        public void onLayoutFailed(CharSequence error) {
                            Log.e("PDF", "Layout failed: " + error);
                        }
                    }, null);
                }, 1000); // 1 second delay for rendering
            }
        });
    }

    private void saveToDatabase(double amount, String clientName, String path, String clientEmail) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Invoice invoice = new Invoice(amount, new Date(), clientName, path, "Facture");
            db.invoiceDao().insertInvoice(invoice);
            
            // Increment and save daily counter
            String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
            int counter = sharedPreferences.getInt("daily_counter", 1);
            String lastDate = sharedPreferences.getString("last_date", "");

            if (today.equals(lastDate)) {
                counter++;
            } else {
                counter = 2; // Reset for new day (1 was used for this invoice)
            }

            sharedPreferences.edit()
                .putString("last_date", today)
                .putInt("daily_counter", counter)
                .apply();
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (getActivity() instanceof com.chouchene.factures.MainActivity) {
                        ((com.chouchene.factures.MainActivity) getActivity()).updateBottomNavBadges();
                    }
                    if (listener != null) listener.onInvoiceGenerated();
                    
                    Context context = getContext();
                    if (context != null && isAdded()) {
                        com.chouchene.factures.utils.UIUtils.showSuccessDialog(context, 
                            "Facture Générée", 
                            "Votre facture a été créée avec succès.", 
                            () -> {
                                if (isAdded()) {
                                    showSuccessSnackbar(path, clientEmail);
                                    dismiss();
                                }
                            });
                    }
                });
            }
        });
    }

    private void showSuccessSnackbar(String filePath, String clientEmail) {
        Activity activity = getActivity();
        if (activity != null) {
            View rootView = activity.findViewById(android.R.id.content);
            if (rootView != null) {
                Snackbar.make(rootView, "Facture créée avec succès", Snackbar.LENGTH_LONG)
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
}

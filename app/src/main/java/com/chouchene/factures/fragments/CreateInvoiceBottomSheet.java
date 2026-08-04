package com.chouchene.factures.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import com.chouchene.factures.R;
import com.chouchene.factures.api.FetchVilleFromCodePostale;
import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.entity.Invoice;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class CreateInvoiceBottomSheet extends BottomSheetDialogFragment {


    private static final String CURRENCY_KEY = "default_currency";
    private static final String TEMPLATE_KEY = "invoice_template";

    private String customerName, rueClient, villeClient, codePostaleClient, pays, siren, tva, email;
    private TextInputEditText txtName, txtRue, txtVille, txtCodePostale, txtPays, txtSiren, txtEmail, txtTvaClient;
    private TextInputEditText txtDesciption, txtQuantite, txtPrix, txtTva, editDateFactureForm;
    private TextInputLayout inputClient, layoutDescription, layoutQuantite, layoutPrix, layoutTva, layoutPaymentMode;
    private AutoCompleteTextView autoCompletePaymentMode;
    private LinearLayout inputClientProvisoire;

    private Integer mumeroFacture = 0;
    private SharedPreferences sharedPreferences, settingsSharedPreferences;
    private AppDatabase db;
    private ClientDao itemDao;
    private Boolean isClientProvisoire = false;
    private Client selectedClient = null;

    private OnInvoiceGeneratedListener listener;

    public interface OnInvoiceGeneratedListener {
        void onInvoiceGenerated();
    }

    public void setOnInvoiceGeneratedListener(OnInvoiceGeneratedListener listener) {
        this.listener = listener;
    }

    public static CreateInvoiceBottomSheet newInstance(Integer clientId) {
        CreateInvoiceBottomSheet fragment = new CreateInvoiceBottomSheet();
        if (clientId != null) {
            Bundle args = new Bundle();
            args.putInt("preselected_client_id", clientId);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        settingsSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        mumeroFacture = sharedPreferences.getInt("last-invoice-number", 0);
        return inflater.inflate(R.layout.bottom_sheet_create_invoice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase();
        itemDao = db.clientDao();
        
        setupClientSearch(view);
        setupRadioGroup(view);
        setupInputs(view);
        setupPaymentMode(view);

        // Handle pre-selected client
        if (getArguments() != null && getArguments().containsKey("preselected_client_id")) {
            int clientId = getArguments().getInt("preselected_client_id");
            Executors.newSingleThreadExecutor().execute(() -> {
                selectedClient = itemDao.getClientById(clientId);
                if (selectedClient != null && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        AutoCompleteTextView searchView = view.findViewById(R.id.autoCompleteTextView);
                        searchView.setText(selectedClient.getClientName(), false);
                        txtDesciption.requestFocus();
                    });
                }
            });
        }

        MaterialButton btnCreatePDF = view.findViewById(R.id.btnCreatePdf);
        btnCreatePDF.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            handleGenerateInvoice();
        });
    }

    private void setupPaymentMode(View view) {
        autoCompletePaymentMode = view.findViewById(R.id.autoCompletePaymentMode);
        layoutPaymentMode = view.findViewById(R.id.layout_payment_mode);
        String[] paymentModes = {"Virement", "Carte", "Espèce", "Chèque"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_menu_popup_item, paymentModes);
        autoCompletePaymentMode.setAdapter(adapter);
        autoCompletePaymentMode.setText(paymentModes[0], false);
    }

    private void setupClientSearch(View view) {
        AutoCompleteTextView searchView = view.findViewById(R.id.autoCompleteTextView);
        TextInputLayout clientInput = view.findViewById(R.id.client_input);
        // db and itemDao are now initialized in onViewCreated

        List<Client> clients = itemDao.getAllClients();
        List<String> names = new ArrayList<>();
        for (Client c : clients) names.add(c.getClientName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_menu_popup_item, names);
        searchView.setAdapter(adapter);
        searchView.setThreshold(0);
        
        searchView.setOnClickListener(v -> searchView.showDropDown());
        clientInput.setStartIconOnClickListener(v -> searchView.showDropDown());
        clientInput.setEndIconOnClickListener(v -> searchView.showDropDown());
        
        searchView.setOnItemClickListener((parent, v, position, id) -> {
            String name = (String) parent.getItemAtPosition(position);
            for (Client c : clients) if (c.getClientName().equals(name)) selectedClient = c;
        });
    }

    private void setupRadioGroup(View view) {
        RadioGroup group = view.findViewById(R.id.radio_group);
        inputClient = view.findViewById(R.id.client_input);
        inputClientProvisoire = view.findViewById(R.id.client_input_provisoire);
        group.setOnCheckedChangeListener((g, id) -> {
            boolean isPerm = (id == R.id.permanant_selected);
            inputClient.setVisibility(isPerm ? View.VISIBLE : View.GONE);
            inputClientProvisoire.setVisibility(isPerm ? View.GONE : View.VISIBLE);
            isClientProvisoire = !isPerm;
        });
    }

    private void setupInputs(View view) {
        layoutDescription = view.findViewById(R.id.layout_description);
        layoutQuantite = view.findViewById(R.id.layout_quantite);
        layoutPrix = view.findViewById(R.id.layout_prix);
        layoutTva = view.findViewById(R.id.layout_tva);

        txtDesciption = view.findViewById(R.id.edit_description);
        txtQuantite = view.findViewById(R.id.edit_quantite);
        txtPrix = view.findViewById(R.id.edit_prix);
        txtTva = view.findViewById(R.id.edit_tva);
        txtTva.setText("10");
        editDateFactureForm = view.findViewById(R.id.edit_date_emission);

        txtName = view.findViewById(R.id.edit_user_name_client1);
        txtRue = view.findViewById(R.id.edit_street1);
        txtVille = view.findViewById(R.id.edit_ville1);
        txtCodePostale = view.findViewById(R.id.edit_code_postale1);
        txtPays = view.findViewById(R.id.edit_pays1);
        txtSiren = view.findViewById(R.id.edit_siren1);
        txtEmail = view.findViewById(R.id.edit_email_client1);
        txtTvaClient = view.findViewById(R.id.edit_tva_client);

        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        editDateFactureForm.setText(fmt.format(new Date()));
        editDateFactureForm.setOnClickListener(v -> showDatePickerDialog());

        txtCodePostale.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                FetchVilleFromCodePostale.fetchDataFromApiWithParams(s.toString(), txtVille, txtPays);
            }
        });
    }

    private void showDatePickerDialog() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build();
        picker.show(requireActivity().getSupportFragmentManager(), "DP");
        picker.addOnPositiveButtonClickListener(sel -> editDateFactureForm.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(sel))));
    }

    private void handleGenerateInvoice() {
        boolean isValid = true;
        layoutDescription.setError(null);
        layoutQuantite.setError(null);
        layoutPrix.setError(null);
        layoutTva.setError(null);

        if (txtDesciption.getText().toString().trim().isEmpty()) { layoutDescription.setError("La description est obligatoire"); isValid = false; }
        if (txtQuantite.getText().toString().trim().isEmpty()) { layoutQuantite.setError("La quantité est obligatoire"); isValid = false; }
        if (txtPrix.getText().toString().trim().isEmpty()) { layoutPrix.setError("Le prix est obligatoire"); isValid = false; }
        if (txtTva.getText().toString().trim().isEmpty()) { layoutTva.setError("La TVA est obligatoire"); isValid = false; }

        if (autoCompletePaymentMode.getText().toString().trim().isEmpty()) {
            layoutPaymentMode.setError("Veuillez choisir un mode de paiement");
            isValid = false;
        }

        if (!isValid) return;

        if (isClientProvisoire) {
            customerName = txtName.getText().toString();
            if (customerName.trim().isEmpty()) { txtName.setError("Le nom est obligatoire"); return; }
            rueClient = txtRue.getText().toString();
            villeClient = txtVille.getText().toString();
            codePostaleClient = txtCodePostale.getText().toString();
            pays = txtPays.getText().toString();
            siren = txtSiren.getText().toString();
            email = txtEmail.getText().toString();
            tva = txtTvaClient.getText().toString();
        } else if (selectedClient != null) {
            customerName = selectedClient.getClientName();
            rueClient = selectedClient.getStreet();
            villeClient = selectedClient.getVille();
            codePostaleClient = selectedClient.getCodePostale();
            pays = selectedClient.getPays();
            siren = selectedClient.getNumeroSiren();
            email = selectedClient.getEmail();
            tva = selectedClient.getNumeroTVA();
        } else {
            Snackbar.make(requireView(), "Veuillez choisir un client", Snackbar.LENGTH_SHORT).show();
            return;
        }

        generateHtmlPdf();
    }

    private void generateHtmlPdf() {
        String templateName = settingsSharedPreferences.getString(TEMPLATE_KEY, "invoice_template.html");
        String template;
        try {
            template = loadHtmlFromAssets(templateName);
        } catch (IOException e) {
            Log.e("PDF_GEN", "Failed to load template", e);
            return;
        }

        float p = 0, t = 0, q = 0;
        try {
            p = Float.parseFloat(txtPrix.getText().toString());
            t = Float.parseFloat(txtTva.getText().toString());
            q = Float.parseFloat(txtQuantite.getText().toString());
        } catch (Exception ignored) {}
        
        float ht = p / (1 + t / 100);
        float totalHt = ht * q;
        float totalTtc = p * q;
        float totalTva = (p - ht) * q;
        String currency = settingsSharedPreferences.getString(CURRENCY_KEY, "EUR");
        String dateCode = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());

        String paymentMode = autoCompletePaymentMode.getText().toString();

        String logoBase64 = "";
        String logoPath = sharedPreferences.getString("logo_uri", null);
        if (logoPath != null) {
            try {
                byte[] bytes = FileUtils.readFileToByteArray(new File(logoPath));
                logoBase64 = "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
            } catch (IOException e) {
                Log.e("PDF_GEN", "Logo error", e);
            }
        }
        String logoHtml = logoBase64.isEmpty() ? "" : "<img src=\"" + logoBase64 + "\" style=\"max-height: 80px; margin-bottom: 10px;\">";

        mumeroFacture++;
        sharedPreferences.edit().putInt("last-invoice-number", mumeroFacture).apply();

        template = template
                .replace("{{companyLogo}}", logoHtml)
                .replace("{{issuerName}}", sharedPreferences.getString("User", ""))
                .replace("{{issuerStreet}}", sharedPreferences.getString("Street", ""))
                .replace("{{issuerCityZip}}", sharedPreferences.getString("codePostale", "") + " " + sharedPreferences.getString("City", ""))
                .replace("{{issuerCountry}}", sharedPreferences.getString("Country", ""))
                .replace("{{issuerSiren}}", sharedPreferences.getString("siren", ""))
                .replace("{{issuerTva}}", sharedPreferences.getString("tva", ""))
                .replace("{{issuerTel}}", sharedPreferences.getString("tel", ""))
                .replace("{{issuerEmail}}", sharedPreferences.getString("email", ""))
                .replace("{{invoiceNumber}}", dateCode + String.format(Locale.US, "%02d", mumeroFacture))
                .replace("{{invoiceDate}}", editDateFactureForm.getText().toString())
                .replace("{{payementMode}}", paymentMode)
                .replace("{{clientName}}", customerName)
                .replace("{{clientStreet}}", rueClient)
                .replace("{{clientCityZip}}", codePostaleClient + " " + villeClient)
                .replace("{{clientCountry}}", pays)
                .replace("{{clientTvaSiren}}", "SIREN: " + siren + " | TVA: " + tva)
                .replace("{{description}}", txtDesciption.getText().toString().replace("\n", "<br>"))
                .replace("{{qty}}", String.valueOf((int)q))
                .replace("{{priceHt}}", String.format(Locale.US, "%.2f %s", ht, currency))
                .replace("{{tvaPercent}}", String.format(Locale.US, "%.0f%%", t))
                .replace("{{tvaEuro}}", String.format(Locale.US, "%.2f %s", (p - ht), currency))
                .replace("{{totalHt}}", String.format(Locale.US, "%.2f %s", totalHt, currency))
                .replace("{{totalTva}}", String.format(Locale.US, "%.2f %s", totalTva, currency))
                .replace("{{totalTtc}}", String.format(Locale.US, "%.2f %s", totalTtc, currency));

        final String fileName = "Facture_" + customerName.trim().replaceAll("[^a-zA-Z0-9]", "_") + "_" + System.currentTimeMillis() + ".pdf";
        final File pdfFile = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        createPdfFromHtml(template, pdfFile, totalTtc);
    }

    private String loadHtmlFromAssets(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open(fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private void createPdfFromHtml(String html, File outputFile, float amount) {
        WebView webView = new WebView(requireContext());
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintAttributes attributes = new PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build();

                PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter("Invoice");
                
                try {
                    ParcelFileDescriptor pfd = ParcelFileDescriptor.open(outputFile, 
                        ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
                    
                    adapter.onLayout(null, attributes, new android.os.CancellationSignal(), new android.print.PrintResultCallbackShim.LayoutResultCallbackShim() {
                        @Override
                        public void onLayoutFinished(android.print.PrintDocumentInfo info, boolean changed) {
                            adapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, pfd, new android.os.CancellationSignal(), new android.print.PrintResultCallbackShim.WriteResultCallbackShim() {
                                @Override
                                public void onWriteFinished(PageRange[] pages) {
                                    try {
                                        pfd.close();
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                db.invoiceDao().insertInvoice(new Invoice(amount, new Date(), customerName, outputFile.getAbsolutePath(), "Facture"));
                                                if (listener != null) listener.onInvoiceGenerated();
                                                
                                                String filePath = outputFile.getAbsolutePath();
                                                String clientEmail = email;
                                                
                                                View rootView = getActivity().findViewById(android.R.id.content);
                                                if (rootView != null) {
                                                    Snackbar.make(rootView, "Facture créée avec succès", Snackbar.LENGTH_LONG)
                                                        .setAction("OUVRIR", v -> navigateToFragmentPreviewPdf(filePath, clientEmail))
                                                        .show();
                                                }
                                                
                                                dismiss();
                                            });
                                        }
                                    } catch (IOException e) {
                                        Log.e("PDF_GEN", "Err", e);
                                    }
                                }

                                @Override
                                public void onWriteFailed(CharSequence error) {
                                    super.onWriteFailed(error);
                                    try { pfd.close(); } catch (IOException ignored) {}
                                }
                            });
                        }

                        @Override
                        public void onLayoutFailed(CharSequence error) {
                            super.onLayoutFailed(error);
                            try { pfd.close(); } catch (IOException ignored) {}
                        }
                    }, null);
                } catch (IOException e) {
                    Log.e("PDF_GEN", "Err", e);
                }
            }
        });
    }

    private void navigateToFragmentPreviewPdf(String path, String mail) {
        Bundle b = new Bundle();
        b.putString("file_path", path);
        b.putString("mail_client", mail);
        NavHostFragment.findNavController(this).navigate(R.id.webViewPdfFragment, b);
    }
}

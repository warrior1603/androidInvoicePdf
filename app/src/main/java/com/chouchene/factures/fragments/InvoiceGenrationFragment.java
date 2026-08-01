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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.room.Room;

import com.chouchene.factures.R;
import com.chouchene.factures.api.FetchVilleFromCodePostale;
import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.entity.Invoice;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InvoiceGenrationFragment extends Fragment {

    private static final String CURRENCY_KEY = "default_currency";
    private static final String TEMPLATE_KEY = "invoice_template";

    public InvoiceGenrationFragment() {}

    private String customerName, rueClient, villeClient, codePostaleClient, pays, siren, tva, email;
    private TextInputEditText txtName, txtRue, txtVille, txtCodePostale, txtPays, txtSiren, txtEmail, txtTvaClient;
    private TextInputEditText txtDesciption, txtQuantite, txtPrix, txtTva, editDateFactureForm;
    private TextInputLayout txtModePayement, inputClient, layoutDescription, layoutQuantite, layoutPrix;
    private LinearLayout inputClientProvisoire;

    private Integer mumeroFacture = 0;
    private SharedPreferences sharedPreferences, settingsSharedPreferences;
    private AppDatabase db;
    private ClientDao itemDao;
    private Boolean isClientProvisoire = false;
    private Client selectedClient = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        sharedPreferences = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        settingsSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mumeroFacture = sharedPreferences.getInt("last-invoice-number", 0);

        View myView = inflater.inflate(R.layout.activity_invoice, container, false);

        MaterialToolbar toolbar = requireActivity().findViewById(R.id.my_toolbar);
        if (toolbar == null) toolbar = requireActivity().findViewById(R.id.my_toolbar1);
        if (toolbar != null) toolbar.setNavigationIcon(R.drawable.baseline_picture_as_pdf_24);

        setupPayementDropdown(myView);
        setupClientSearch(myView);
        setupRadioGroup(myView);
        setupInputs(myView);

        MaterialButton btnCreatePDF = myView.findViewById(R.id.btnCreatePdf);
        btnCreatePDF.setOnClickListener(v -> handleGenerateInvoice());

        return myView;
    }

    private void setupPayementDropdown(View view) {
        String[] Payements = {"Virement", "Carte", "Espèce", "Cheque"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.dropdown_menu_popup_item, Payements);
        AutoCompleteTextView dropdown = view.findViewById(R.id.filled_exposed_dropdown);
        dropdown.setAdapter(adapter);
    }

    private void setupClientSearch(View view) {
        AutoCompleteTextView searchView = view.findViewById(R.id.autoCompleteTextView);
        db = Room.databaseBuilder(requireContext(), AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build();
        itemDao = db.clientDao();

        List<Client> clients = itemDao.getAllClients();
        List<String> names = new ArrayList<>();
        for (Client c : clients) names.add(c.getClientName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
        searchView.setAdapter(adapter);
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

        txtDesciption = view.findViewById(R.id.edit_description);
        txtQuantite = view.findViewById(R.id.edit_quantite);
        txtPrix = view.findViewById(R.id.edit_prix);
        txtTva = view.findViewById(R.id.edit_tva);
        editDateFactureForm = view.findViewById(R.id.edit_date_emission);
        txtModePayement = view.findViewById(R.id.dropdown_input);

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
        txtModePayement.setError(null);

        if (txtDesciption.getText().toString().trim().isEmpty()) { layoutDescription.setError("La description est obligatoire"); isValid = false; }
        if (txtQuantite.getText().toString().trim().isEmpty()) { layoutQuantite.setError("La quantité est obligatoire"); isValid = false; }
        if (txtPrix.getText().toString().trim().isEmpty()) { layoutPrix.setError("Le prix est obligatoire"); isValid = false; }
        if (txtModePayement.getEditText().getText().toString().trim().isEmpty()) { txtModePayement.setError("Veuillez choisir un mode de paiement"); isValid = false; }

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
            Toast.makeText(requireContext(), "Veuillez choisir un client", Toast.LENGTH_SHORT).show();
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

        mumeroFacture++;
        sharedPreferences.edit().putInt("last-invoice-number", mumeroFacture).apply();

        template = template
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
                .replace("{{payementMode}}", txtModePayement.getEditText().getText().toString())
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
                                        requireActivity().runOnUiThread(() -> {
                                            navigateToFragmentPreviewPdf(outputFile.getAbsolutePath(), email);
                                            db.invoiceDao().insertInvoice(new Invoice(amount, new Date(), customerName, outputFile.getAbsolutePath(), "Facture"));
                                        });
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
        Navigation.findNavController(requireView()).navigate(R.id.webViewPdfFragment, b);
    }
}

package com.chouchene.factures;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.fragments.ClientPickerBottomSheet;
import com.chouchene.factures.utils.BackupUtils;
import com.chouchene.factures.utils.UIUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import android.net.Uri;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class DocumentStudioActivity extends AppCompatActivity {

    public static final String MODE_CREATE = "CREATE";
    public static final String MODE_EDIT = "EDIT";
    public static final String TYPE_INVOICE = "Facture";
    public static final String TYPE_BON = "Bon";

    public static final String EXTRA_MODE = "EXTRA_MODE";
    public static final String EXTRA_TYPE = "EXTRA_TYPE";
    public static final String EXTRA_DOC_ID = "EXTRA_DOC_ID";

    private MaterialToolbar toolbar;
    private WebView webViewPreview;
    private ProgressBar previewLoader;
    private ViewFlipper viewFlipper;
    private MaterialButton btnBack, btnNext, btnGenerate;

    private View stepNode1, stepNode2, stepNode3;
    private View studioAccentBar;

    // Facture Fields
    private TextInputEditText editInvName, editInvEmail, editInvTel, editInvStreet, editInvZip, editInvCity, editInvCountry, editInvSiren, editInvTva;
    private TextInputEditText editInvDate, editInvDesc, editInvQty, editInvPriceTtc, editInvTvaRate;
    private AutoCompleteTextView editInvPayment;

    // Bon Fields
    private TextInputEditText editBonPassenger, editBonTel, editBonOrderDate, editBonOrderTime, editBonPickupDate, editBonPickupTime, editBonPec, editBonDest, editBonVia, editBonTarif;

    private com.chouchene.factures.utils.SignatureView signatureView;
    private MaterialButton btnClearSignature;
    
    // Success Overlay Views
    private View successOverlay;
    private com.airbnb.lottie.LottieAnimationView lottieSuccess;
    private MaterialButton btnView, btnShare, btnDone;
    private nl.dionsegijn.konfetti.xml.KonfettiView konfettiStudio;

    private View containerInvoiceClient, containerInvoiceDetails, containerInvoicePrices;
    private View containerBonClient, containerBonDetails, containerBonPrices;

    private String mode = MODE_CREATE;
    private String type = TYPE_INVOICE;
    private int docId = -1;
    private Invoice existingInvoice;

    private SharedPreferences sharedPrefs, settingsPrefs;
    private AppDatabase db;
    private final Handler previewHandler = new Handler(Looper.getMainLooper());
    private Runnable previewRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_studio);

        db = DatabaseClient.getInstance(this).getAppDatabase();
        sharedPrefs = getSharedPreferences("InvoicePrefs", MODE_PRIVATE);
        settingsPrefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        if (getIntent() != null) {
            mode = getIntent().getStringExtra(EXTRA_MODE);
            type = getIntent().getStringExtra(EXTRA_TYPE);
            docId = getIntent().getIntExtra(EXTRA_DOC_ID, -1);
            if (mode == null) mode = MODE_CREATE;
            if (type == null) type = TYPE_INVOICE;
        }

        initViews();
        setupInputs();
        setupStepper();

        if (mode.equals(MODE_EDIT) && docId != -1) {
            loadExistingDocument();
        } else {
            prefillDefaults();
            updatePreview();
        }
    }

    private void initViews() {
        View btnBackHeader = findViewById(R.id.btn_back_header);
        if (btnBackHeader != null) btnBackHeader.setOnClickListener(v -> finish());

        TextView txtTitle = findViewById(R.id.txt_studio_title);
        if (txtTitle != null) {
            txtTitle.setText(mode.equals(MODE_EDIT) ? "Modifier " + type : "Nouveau " + type);
        }

        webViewPreview = findViewById(R.id.webview_preview);
        previewLoader = findViewById(R.id.preview_loader);
        viewFlipper = findViewById(R.id.view_flipper_studio);
        btnBack = findViewById(R.id.btn_back_studio);
        btnNext = findViewById(R.id.btn_next_studio);
        btnGenerate = findViewById(R.id.btn_generate_studio);

        stepNode1 = findViewById(R.id.step_node_1);
        stepNode2 = findViewById(R.id.step_node_2);
        stepNode3 = findViewById(R.id.step_node_3);
        studioAccentBar = findViewById(R.id.studio_accent_bar);

        containerInvoiceClient = findViewById(R.id.container_invoice_client);
        containerInvoiceDetails = findViewById(R.id.container_invoice_details);
        containerInvoicePrices = findViewById(R.id.container_invoice_prices);
        containerBonClient = findViewById(R.id.container_bon_client);
        containerBonDetails = findViewById(R.id.container_bon_details);
        containerBonPrices = findViewById(R.id.container_bon_prices);

        // Success Overlay Init
        successOverlay = findViewById(R.id.studio_success_root);
        lottieSuccess = findViewById(R.id.lottie_studio_success);
        btnView = findViewById(R.id.btn_studio_view);
        btnShare = findViewById(R.id.btn_studio_share);
        btnDone = findViewById(R.id.btn_studio_done);
        konfettiStudio = findViewById(R.id.konfetti_studio);

        signatureView = findViewById(R.id.signature_view_studio);
        btnClearSignature = findViewById(R.id.btn_clear_signature_studio);
        btnClearSignature.setOnClickListener(v -> signatureView.clear());

        boolean isInvoice = type.equals(TYPE_INVOICE);
        containerInvoiceClient.setVisibility(isInvoice ? View.VISIBLE : View.GONE);
        containerInvoiceDetails.setVisibility(isInvoice ? View.VISIBLE : View.GONE);
        containerInvoicePrices.setVisibility(isInvoice ? View.VISIBLE : View.GONE);
        
        containerBonClient.setVisibility(!isInvoice ? View.VISIBLE : View.GONE);
        containerBonDetails.setVisibility(!isInvoice ? View.VISIBLE : View.GONE);
        containerBonPrices.setVisibility(!isInvoice ? View.VISIBLE : View.GONE);

        if (studioAccentBar != null) {
            int accentColor = isInvoice ? getThemeColor(androidx.appcompat.R.attr.colorPrimary) : ContextCompat.getColor(this, R.color.icon_dashboard);
            studioAccentBar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
        }

        webViewPreview.getSettings().setJavaScriptEnabled(true);
        webViewPreview.getSettings().setLoadWithOverviewMode(true);
        webViewPreview.getSettings().setUseWideViewPort(true);
        webViewPreview.getSettings().setBuiltInZoomControls(true);
        webViewPreview.getSettings().setDisplayZoomControls(false);
        
        webViewPreview.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) { previewLoader.setVisibility(View.VISIBLE); }
            @Override public void onPageFinished(WebView view, String url) { previewLoader.setVisibility(View.GONE); }
        });
    }

    private void setupInputs() {
        // Facture
        editInvName = initItemWithAction(findViewById(R.id.item_inv_name), R.drawable.ic_typcn_clients, "Nom du Client", InputType.TYPE_TEXT_VARIATION_PERSON_NAME, this::showClientPicker);
        editInvEmail = initItem(findViewById(R.id.item_inv_email), R.drawable.ic_outline_mail, "Email du Client", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        editInvTel = initItem(findViewById(R.id.item_inv_tel), R.drawable.ic_outline_phone, "Téléphone du Client", InputType.TYPE_CLASS_PHONE);
        editInvStreet = initItem(findViewById(R.id.item_inv_street), R.drawable.ic_outline_road, "Rue / Adresse", InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        editInvZip = initItem(findViewById(R.id.item_inv_zip), R.drawable.ic_outline_hash, "Code Postal", InputType.TYPE_CLASS_NUMBER);
        editInvCity = initItem(findViewById(R.id.item_inv_city), R.drawable.ic_outline_building, "Ville", InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editInvCountry = initItem(findViewById(R.id.item_inv_country), R.drawable.ic_tab_world, "Pays", InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editInvSiren = initItem(findViewById(R.id.item_inv_siren), R.drawable.ic_outline_adjustments, "SIREN", InputType.TYPE_CLASS_NUMBER);
        editInvTva = initItem(findViewById(R.id.item_inv_tva), R.drawable.ic_outline_cash, "TVA Client", InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

        editInvDate = initItem(findViewById(R.id.item_inv_date), R.drawable.ic_outline_calendar, "Date d'émission", InputType.TYPE_NULL);
        editInvDate.setOnClickListener(v -> showDatePicker(editInvDate));
        editInvDesc = initItem(findViewById(R.id.item_inv_desc), R.drawable.ic_outline_receipt, "Description de la prestation", InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editInvDesc.setSingleLine(false);
        editInvDesc.setMinLines(3);

        editInvPayment = initDropdownItem(findViewById(R.id.item_inv_payment), R.drawable.ic_outline_cash, "Mode de paiement", new String[]{"Virement", "Carte", "Espèce", "Chèque"});
        
        editInvQty = initItem(findViewById(R.id.item_inv_qty), R.drawable.ic_outline_hash, "Quantité", InputType.TYPE_CLASS_NUMBER);
        editInvPriceTtc = initItem(findViewById(R.id.item_inv_price_ttc), R.drawable.ic_outline_cash, "Prix Total TTC", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editInvTvaRate = initItem(findViewById(R.id.item_inv_tva_rate), R.drawable.ic_outline_adjustments, "Taux TVA (%)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // Bon
        editBonPassenger = initItemWithAction(findViewById(R.id.item_bon_passenger), R.drawable.ic_typcn_clients, "Nom du Passager", InputType.TYPE_TEXT_VARIATION_PERSON_NAME, this::showClientPicker);
        editBonTel = initItem(findViewById(R.id.item_bon_tel), R.drawable.ic_outline_phone, "Téléphone du Passager", InputType.TYPE_CLASS_PHONE);
        editBonOrderDate = initItem(findViewById(R.id.item_bon_order_date), R.drawable.ic_outline_calendar, "Date de Commande", InputType.TYPE_NULL);
        editBonOrderDate.setOnClickListener(v -> showDatePicker(editBonOrderDate));
        editBonOrderTime = initItem(findViewById(R.id.item_bon_order_time), R.drawable.ic_outline_clock, "Heure de Commande", InputType.TYPE_NULL);
        editBonOrderTime.setOnClickListener(v -> showTimePicker(editBonOrderTime));

        editBonPickupDate = initItem(findViewById(R.id.item_bon_pickup_date), R.drawable.ic_outline_calendar, "Date de Prise en Charge", InputType.TYPE_NULL);
        editBonPickupDate.setOnClickListener(v -> showDatePicker(editBonPickupDate));
        editBonPickupTime = initItem(findViewById(R.id.item_bon_pickup_time), R.drawable.ic_outline_clock, "Heure de Prise en Charge", InputType.TYPE_NULL);
        editBonPickupTime.setOnClickListener(v -> showTimePicker(editBonPickupTime));

        editBonPec = initItem(findViewById(R.id.item_bon_pec), R.drawable.ic_outline_road, "Lieu de Prise en Charge", InputType.TYPE_CLASS_TEXT);
        editBonDest = initItem(findViewById(R.id.item_bon_dest), R.drawable.ic_outline_home, "Lieu de Destination", InputType.TYPE_CLASS_TEXT);
        editBonVia = initItem(findViewById(R.id.item_bon_via), R.drawable.ic_outline_route, "Via (Optionnel)", InputType.TYPE_CLASS_TEXT);
        editBonTarif = initItem(findViewById(R.id.item_bon_tarif), R.drawable.ic_outline_cash, "Tarif Total TTC", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        editInvZip.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() >= 5) com.chouchene.factures.api.FetchVilleFromCodePostale.fetchDataFromApiWithParams(s.toString(), editInvCity, editInvCountry);
            }
        });
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

    private void showClientPicker() {
        ClientPickerBottomSheet picker = new ClientPickerBottomSheet();
        picker.setOnClientSelectedListener(client -> {
            if (type.equals(TYPE_INVOICE)) {
                editInvName.setText(client.clientName);
                editInvEmail.setText(client.email);
                editInvTel.setText(client.phone);
                editInvStreet.setText(client.street);
                editInvZip.setText(client.codePostale);
                editInvCity.setText(client.ville);
                editInvCountry.setText(client.pays);
                editInvSiren.setText(client.numeroSiren);
                editInvTva.setText(client.getNumeroTVA());
            } else {
                editBonPassenger.setText(client.clientName);
                editBonTel.setText(client.phone);
            }
        });
        picker.show(getSupportFragmentManager(), "CLIENT_PICKER");
    }

    private TextInputEditText initItem(View itemView, int iconRes, String label, int inputType) {
        ImageView icon = itemView.findViewById(R.id.item_icon);
        TextView txtLabel = itemView.findViewById(R.id.item_label);
        TextInputEditText input = itemView.findViewById(R.id.item_input);

        icon.setImageResource(iconRes);
        icon.setColorFilter(getThemeColor(com.google.android.material.R.attr.colorOnSurface)); // Black icon
        
        txtLabel.setText(label);
        txtLabel.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface)); // Black label
        txtLabel.setAlpha(0.9f);
        
        input.setHint(label);
        input.setInputType(inputType);
        
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { schedulePreviewUpdate(); }
        });
        return input;
    }

    private AutoCompleteTextView initDropdownItem(View itemView, int iconRes, String label, String[] options) {
        ImageView icon = itemView.findViewById(R.id.item_icon);
        TextView txtLabel = itemView.findViewById(R.id.item_label);
        AutoCompleteTextView dropdown = itemView.findViewById(R.id.item_dropdown);

        icon.setImageResource(iconRes);
        icon.setColorFilter(getThemeColor(com.google.android.material.R.attr.colorOnSurface)); // Black icon
        
        txtLabel.setText(label);
        txtLabel.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface)); // Black label
        txtLabel.setAlpha(0.9f);
        
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, R.layout.dropdown_menu_popup_item, options);
        dropdown.setAdapter(adapter);
        dropdown.setText(options[0], false);
        
        dropdown.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { schedulePreviewUpdate(); }
        });
        return dropdown;
    }

    private void setupStepper() {
        btnNext.setOnClickListener(v -> {
            int currentStep = viewFlipper.getDisplayedChild();
            if (currentStep < 2) goToStep(currentStep + 1);
        });
        btnBack.setOnClickListener(v -> {
            int currentStep = viewFlipper.getDisplayedChild();
            if (currentStep > 0) goToStep(currentStep - 1);
        });
        btnGenerate.setOnClickListener(v -> handleGenerate());

        stepNode1.setOnClickListener(v -> goToStep(0));
        stepNode2.setOnClickListener(v -> goToStep(1));
        stepNode3.setOnClickListener(v -> goToStep(2));

        goToStep(0);
    }

    private void goToStep(int step) {
        int current = viewFlipper.getDisplayedChild();
        if (step > current) {
            viewFlipper.setInAnimation(this, R.anim.slide_in_right);
            viewFlipper.setOutAnimation(this, R.anim.slide_out_left);
        } else if (step < current) {
            viewFlipper.setInAnimation(this, R.anim.slide_in_left);
            viewFlipper.setOutAnimation(this, R.anim.slide_out_right);
        }

        viewFlipper.setDisplayedChild(step);
        btnBack.setVisibility(step == 0 ? View.GONE : View.VISIBLE);
        btnNext.setVisibility(step == 2 ? View.GONE : View.VISIBLE);
        btnGenerate.setVisibility(step == 2 ? View.VISIBLE : View.GONE);

        stepNode1.setActivated(step >= 0);
        stepNode2.setActivated(step >= 1);
        stepNode3.setActivated(step >= 2);

        // Update accent bar color based on step if needed
        if (studioAccentBar != null) {
            int color;
            if (step == 0) color = getThemeColor(androidx.appcompat.R.attr.colorPrimary);
            else if (step == 1) color = ContextCompat.getColor(this, R.color.icon_clients);
            else color = ContextCompat.getColor(this, R.color.icon_dashboard);
            studioAccentBar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        }
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void schedulePreviewUpdate() {
        if (previewRunnable != null) previewHandler.removeCallbacks(previewRunnable);
        previewRunnable = this::updatePreview;
        previewHandler.postDelayed(previewRunnable, 500);
    }

    private void showDatePicker(TextInputEditText target) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build();
        picker.show(getSupportFragmentManager(), "DATE_PICKER");
        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            target.setText(sdf.format(new Date(selection)));
        });
    }

    private void showTimePicker(TextInputEditText target) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H).setHour(12).setMinute(0).setTitleText("Heure").build();
        picker.show(getSupportFragmentManager(), "TIME_PICKER");
        picker.addOnPositiveButtonClickListener(v -> target.setText(String.format(Locale.getDefault(), "%02d:%02d", picker.getHour(), picker.getMinute())));
    }

    private String loadHtmlFromAssets(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private void updatePreview() {
        try {
            boolean isInvoice = type.equals(TYPE_INVOICE);
            String html = loadHtmlFromAssets(isInvoice ? "invoice_template.html" : "order_template.html");

            // Base preferences for issuer info
            SharedPreferences myPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

            String logoPath = myPrefs.getString("logo_uri", null);
            String logoHtml = "";
            if (logoPath != null) {
                File logoFile = new File(logoPath);
                if (logoFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(logoFile)) {
                        byte[] bytes = new byte[(int) logoFile.length()];
                        fis.read(bytes);
                        logoHtml = "<img src=\"data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP) + "\" style=\"max-height: 80px;\">";
                    }
                }
            }
            html = html.replace("{{companyLogo}}", logoHtml).replace("{{logoEntreprise}}", logoHtml);

            if (isInvoice && signatureView != null && !signatureView.isEmpty()) {
                java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
                signatureView.getSignatureBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
                html = html.replace("</body>", "<div style='text-align:right;'><img src=\"data:image/png;base64," + Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP) + "\" style=\"max-height: 100px;\"></div></body>");
            }

            String issuerName = myPrefs.getString("User", "");
            String issuerStreet = myPrefs.getString("Street", "");
            String issuerZipCity = myPrefs.getString("codePostale", "") + " " + myPrefs.getString("City", "");
            
            if (isInvoice) {
                html = html.replace("{{issuerName}}", issuerName).replace("{{issuerStreet}}", issuerStreet).replace("{{issuerCityZip}}", issuerZipCity)
                        .replace("{{issuerCountry}}", myPrefs.getString("Country", "France"))
                        .replace("{{issuerSiren}}", myPrefs.getString("siren", ""))
                        .replace("{{issuerTva}}", myPrefs.getString("tva", ""))
                        .replace("{{issuerTel}}", myPrefs.getString("tel", ""))
                        .replace("{{issuerEmail}}", myPrefs.getString("email", ""));

                html = html.replace("{{clientName}}", editInvName.getText().toString())
                        .replace("{{clientStreet}}", editInvStreet.getText().toString())
                        .replace("{{clientCityZip}}", editInvZip.getText().toString() + " " + editInvCity.getText().toString())
                        .replace("{{clientCountry}}", editInvCountry.getText().toString())
                        .replace("{{clientTvaSiren}}", "SIREN: " + editInvSiren.getText().toString() + " | TVA: " + editInvTva.getText().toString());

                double qty = 1, priceTtc = 0, tvaRate = 10;
                try { qty = Double.parseDouble(editInvQty.getText().toString()); } catch (Exception ignored) {}
                try { priceTtc = Double.parseDouble(editInvPriceTtc.getText().toString()); } catch (Exception ignored) {}
                try { tvaRate = Double.parseDouble(editInvTvaRate.getText().toString()); } catch (Exception ignored) {}

                double totalTtc = qty * priceTtc;
                double totalHt = totalTtc / (1 + (tvaRate / 100));
                double totalTva = totalTtc - totalHt;

                html = html.replace("{{description}}", editInvDesc.getText().toString())
                        .replace("{{qty}}", String.valueOf((int)qty))
                        .replace("{{priceHt}}", String.format(Locale.getDefault(), "%.2f €", totalHt / qty))
                        .replace("{{tvaPercent}}", String.format(Locale.getDefault(), "%.0f%%", tvaRate))
                        .replace("{{tvaEuro}}", String.format(Locale.getDefault(), "%.2f €", totalTva))
                        .replace("{{totalHt}}", String.format(Locale.getDefault(), "%.2f €", totalHt))
                        .replace("{{totalTva}}", String.format(Locale.getDefault(), "%.2f €", totalTva))
                        .replace("{{totalTtc}}", String.format(Locale.getDefault(), "%.2f €", totalTtc))
                        .replace("{{invoiceDate}}", editInvDate.getText().toString())
                        .replace("{{payementMode}}", editInvPayment.getText().toString())
                        .replace("{{invoiceNumber}}", mode.equals(MODE_EDIT) && existingInvoice != null ? "FAC-" + existingInvoice.id : "PROVISOIRE");
            } else {
                html = html.replace("{{nomEmetteur}}", issuerName).replace("{{rueEmetteur}}", issuerStreet).replace("{{codePostaleEmetteur}}", myPrefs.getString("codePostale", ""))
                        .replace("{{villeEmetteur}}", myPrefs.getString("City", ""))
                        .replace("{{telEmetteur}}", myPrefs.getString("tel", ""))
                        .replace("{{numeroEVTC}}", myPrefs.getString("evtc", ""))
                        .replace("{{issuerSiren}}", myPrefs.getString("siren", ""))
                        .replace("{{issuerTva}}", myPrefs.getString("tva", ""))
                        .replace("{{nomConducteur}}", myPrefs.getString("chauffeur", ""))
                        .replace("{{nomChauffeur}}", myPrefs.getString("chauffeur", ""))
                        .replace("{{plaque}}", myPrefs.getString("plaque", ""));

                html = html.replace("{{nomPassager}}", editBonPassenger.getText().toString())
                        .replace("{{telPassager}}", editBonTel.getText().toString())
                        .replace("{{dateCommande}}", editBonOrderDate.getText().toString() + " " + editBonOrderTime.getText().toString())
                        .replace("{{datePriseEnCharge}}", editBonPickupDate.getText().toString() + " " + editBonPickupTime.getText().toString())
                        .replace("{{lieuPriseEnCharge}}", editBonPec.getText().toString())
                        .replace("{{destination}}", editBonDest.getText().toString())
                        .replace("{{via}}", editBonVia.getText().toString())
                        .replace("{{tarif}}", editBonTarif.getText().toString() + " €");
            }

            webViewPreview.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
        } catch (Exception e) { Log.e("STUDIO", "Error", e); }
    }

    private void handleGenerate() {
        String clientName = type.equals(TYPE_INVOICE) ? editInvName.getText().toString() : editBonPassenger.getText().toString();
        File outFile = new File(settingsPrefs.getString("directory", BackupUtils.getDefaultPdfDir(this)), type + "_" + clientName.trim().replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf");

        webViewPreview.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    PrintAttributes attrs = new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600)).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build();
                    PrintDocumentAdapter adapter = webViewPreview.createPrintDocumentAdapter(type);
                    adapter.onLayout(null, attrs, null, new PrintResultCallbackShim.LayoutResultCallbackShim() {
                        @Override public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                            try {
                                ParcelFileDescriptor pfd = ParcelFileDescriptor.open(outFile, ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
                                adapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, pfd, null, new PrintResultCallbackShim.WriteResultCallbackShim() {
                                    @Override public void onWriteFinished(PageRange[] pages) {
                                        try { pfd.close(); saveToDatabase(outFile.getAbsolutePath()); } catch (IOException ignored) {}
                                    }
                                });
                            } catch (Exception ignored) {}
                        }
                    }, null);
                }, 1000);
            }
        });
        updatePreview();
    }

    private void saveToDatabase(String path) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Invoice invoice = (mode.equals(MODE_EDIT) && existingInvoice != null) ? existingInvoice : new Invoice();
            invoice.type = type; invoice.filePath = path; invoice.date = new Date();
            if (type.equals(TYPE_INVOICE)) {
                invoice.clientName = editInvName.getText().toString(); invoice.email = editInvEmail.getText().toString();
                invoice.tel = editInvTel.getText().toString(); invoice.street = editInvStreet.getText().toString();
                invoice.codePostale = editInvZip.getText().toString(); invoice.city = editInvCity.getText().toString();
                invoice.country = editInvCountry.getText().toString(); invoice.siren = editInvSiren.getText().toString();
                invoice.tva_client = editInvTva.getText().toString(); invoice.invoice_date = editInvDate.getText().toString();
                invoice.description = editInvDesc.getText().toString(); invoice.payment_mode = editInvPayment.getText().toString();
                try { invoice.qty = Double.parseDouble(editInvQty.getText().toString()); } catch (Exception ignored) {}
                try { invoice.price_ttc = Double.parseDouble(editInvPriceTtc.getText().toString()); } catch (Exception ignored) {}
                try { invoice.tva_rate = Double.parseDouble(editInvTvaRate.getText().toString()); } catch (Exception ignored) {}
                invoice.amount = invoice.qty * invoice.price_ttc;
                if (!signatureView.isEmpty()) {
                    java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
                    signatureView.getSignatureBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, os);
                    invoice.signature_base64 = Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP);
                }
            } else {
                invoice.clientName = editBonPassenger.getText().toString(); invoice.passenger_name = editBonPassenger.getText().toString();
                invoice.passenger_tel = editBonTel.getText().toString(); invoice.order_date = editBonOrderDate.getText().toString();
                invoice.order_time = editBonOrderTime.getText().toString(); invoice.pickup_date = editBonPickupDate.getText().toString();
                invoice.pickup_time = editBonPickupTime.getText().toString(); invoice.pickup_location = editBonPec.getText().toString();
                invoice.destination = editBonDest.getText().toString(); invoice.via = editBonVia.getText().toString();
                try { invoice.fare = Double.parseDouble(editBonTarif.getText().toString()); } catch (Exception ignored) {}
                invoice.amount = invoice.fare;
            }
            if (mode.equals(MODE_EDIT) && existingInvoice != null) db.invoiceDao().updateInvoice(invoice);
            else db.invoiceDao().insertInvoice(invoice);
            runOnUiThread(() -> showSuccessOverlay(path));
        });
    }

    private void showSuccessOverlay(String path) {
        if (successOverlay == null) {
            finish();
            return;
        }

        successOverlay.post(() -> {
            // Circular Reveal Animation
            int cx = successOverlay.getWidth() / 2;
            int cy = successOverlay.getHeight() / 2;
            float finalRadius = (float) Math.hypot(cx, cy);

            successOverlay.setVisibility(View.VISIBLE);
            android.view.ViewAnimationUtils.createCircularReveal(successOverlay, cx, cy, 0f, finalRadius)
                    .setDuration(800)
                    .start();
        });

        if (lottieSuccess != null) {
            com.chouchene.factures.utils.LottieUtils.loadLottieWithFallback(lottieSuccess, new ImageView(this), "anim_onboarding_1.json");
        }

        // Trigger Konfetti
        if (konfettiStudio != null) {
            konfettiStudio.postDelayed(() -> {
                nl.dionsegijn.konfetti.core.emitter.EmitterConfig emitterConfig = new nl.dionsegijn.konfetti.core.emitter.Emitter(1, java.util.concurrent.TimeUnit.SECONDS).perSecond(100);
                konfettiStudio.start(
                        new nl.dionsegijn.konfetti.core.PartyFactory(emitterConfig)
                                .angle(nl.dionsegijn.konfetti.core.Angle.BOTTOM)
                                .spread(nl.dionsegijn.konfetti.core.Spread.ROUND)
                                .shapes(nl.dionsegijn.konfetti.core.models.Shape.Circle.INSTANCE, nl.dionsegijn.konfetti.core.models.Shape.Square.INSTANCE)
                                .position(0.0, 0.0, 1.0, 0.0)
                                .sizes(new nl.dionsegijn.konfetti.core.models.Size(8, 50, 10))
                                .colors(java.util.Arrays.asList(0x3F51B5, 0x2E7D32, 0x81C784, 0x7986CB))
                                .build()
                );
            }, 300L);
        }

        btnView.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("navigate_to_pdf", path);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnShare.setOnClickListener(v -> {
            File file = new File(path);
            Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                    this, "com.chouchene.factures.provider", file
            );
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Partager le document"));
        });

        btnDone.setOnClickListener(v -> finish());
    }

    private void loadExistingDocument() {
        Executors.newSingleThreadExecutor().execute(() -> {
            existingInvoice = db.invoiceDao().getInvoiceById(docId);
            if (existingInvoice != null) {
                runOnUiThread(() -> {
                    if (existingInvoice.type.equals(TYPE_INVOICE)) {
                        editInvName.setText(existingInvoice.clientName); editInvEmail.setText(existingInvoice.email);
                        editInvTel.setText(existingInvoice.tel); editInvStreet.setText(existingInvoice.street);
                        editInvZip.setText(existingInvoice.codePostale); editInvCity.setText(existingInvoice.city);
                        editInvCountry.setText(existingInvoice.country); editInvSiren.setText(existingInvoice.siren);
                        editInvTva.setText(existingInvoice.tva_client); editInvDate.setText(existingInvoice.invoice_date);
                        editInvDesc.setText(existingInvoice.description); editInvPayment.setText(existingInvoice.payment_mode, false);
                        editInvQty.setText(String.valueOf((int)existingInvoice.qty)); editInvPriceTtc.setText(String.valueOf(existingInvoice.price_ttc));
                        editInvTvaRate.setText(String.valueOf(existingInvoice.tva_rate));
                    } else {
                        editBonPassenger.setText(existingInvoice.passenger_name); editBonTel.setText(existingInvoice.passenger_tel);
                        editBonOrderDate.setText(existingInvoice.order_date); editBonOrderTime.setText(existingInvoice.order_time);
                        editBonPickupDate.setText(existingInvoice.pickup_date); editBonPickupTime.setText(existingInvoice.pickup_time);
                        editBonPec.setText(existingInvoice.pickup_location); editBonDest.setText(existingInvoice.destination);
                        editBonVia.setText(existingInvoice.via); editBonTarif.setText(String.valueOf(existingInvoice.fare));
                    }
                    updatePreview();
                });
            }
        });
    }

    private void prefillDefaults() {
        String d = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
        String t = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        if (type.equals(TYPE_INVOICE)) {
            editInvDate.setText(d); 
            editInvQty.setText("1"); 
            
            // Use preferences for VAT and Payment mode
            String defaultTva = settingsPrefs.getString("default_tva", "10");
            editInvTvaRate.setText(defaultTva); 
            
            String defaultPayment = settingsPrefs.getString("default_payment", "Virement");
            editInvPayment.setText(defaultPayment, false); 
            
            editInvCountry.setText("France");
        } else {
            editBonOrderDate.setText(d); editBonOrderTime.setText(t); editBonPickupDate.setText(d); editBonPickupTime.setText(t);
        }
    }
}
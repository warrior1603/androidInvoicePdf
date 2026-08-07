package com.chouchene.factures.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chouchene.factures.R;
import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.api.FetchVilleFromCodePostale;
import com.chouchene.factures.utils.SignatureView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreateInvoiceBottomSheet extends BottomSheetDialogFragment {

    private static final String CURRENCY_KEY = "default_currency";
    private static final String TEMPLATE_KEY = "invoice_template";

    private String customerName, rueClient, villeClient, codePostaleClient, pays, siren, tva, email;
    private TextInputEditText txtName, txtRue, txtVille, txtCodePostale, txtPays, txtSiren, txtEmail, txtTvaClient;
    private TextInputEditText txtDesciption, txtQuantite, txtPrix, txtTva, editDateFactureForm;
    private TextInputLayout inputClient, layoutDescription, layoutQuantite, layoutPrix, layoutTva, layoutPaymentMode;
    private AutoCompleteTextView autoCompletePaymentMode, autoCompleteTextView;
    private LinearLayout inputClientProvisoire;
    private SignatureView signatureView;

    private Integer mumeroFacture = 0;
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

    public static CreateInvoiceBottomSheet newInstance(Integer invoiceNumber) {
        CreateInvoiceBottomSheet fragment = new CreateInvoiceBottomSheet();
        Bundle args = new Bundle();
        args.putInt("invoice_number", invoiceNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_create_invoice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = DatabaseClient.getInstance(requireContext()).getAppDatabase();
        itemDao = db.clientDao();
        sharedPreferences = requireActivity().getSharedPreferences("InvoicePrefs", Context.MODE_PRIVATE);
        settingsSharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        setupInputs(view);
        setupClientSearch(view);
        setupRadioGroup(view);
        setupPaymentMode(view);

        view.findViewById(R.id.btnCreatePdf).setOnClickListener(v -> handleGenerateInvoice());
        view.findViewById(R.id.btn_clear_signature).setOnClickListener(v -> signatureView.clear());
    }

    private void setupPaymentMode(View view) {
        layoutPaymentMode = view.findViewById(R.id.layout_payment_mode);
        autoCompletePaymentMode = view.findViewById(R.id.autoCompletePaymentMode);
        String[] paymentModes = {"Virement", "Carte", "Espèce", "Chèque"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_menu_popup_item, paymentModes);
        autoCompletePaymentMode.setAdapter(adapter);
        autoCompletePaymentMode.setText(paymentModes[0], false);
    }

    private void setupClientSearch(View view) {
        autoCompleteTextView = view.findViewById(R.id.autoCompleteTextView);
        inputClient = view.findViewById(R.id.client_input);
        
        inputClient.setEndIconOnClickListener(v -> showClientPicker());

        List<Client> clients = itemDao.getAllClients();
        List<String> names = new ArrayList<>();
        for (Client c : clients) names.add(c.clientName);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_menu_popup_item, names);
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setThreshold(0);
        
        autoCompleteTextView.setOnClickListener(v -> autoCompleteTextView.showDropDown());
        
        autoCompleteTextView.setOnItemClickListener((parent, v, position, id) -> {
            String name = (String) parent.getItemAtPosition(position);
            for (Client c : clients) if (c.clientName.equals(name)) selectedClient = c;
        });
    }

    private void setupRadioGroup(View view) {
        RadioGroup radioGroup = view.findViewById(R.id.radio_group);
        inputClientProvisoire = view.findViewById(R.id.client_input_provisoire);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isClientProvisoire = checkedId == R.id.provisoire_selected;
            inputClientProvisoire.setVisibility(isClientProvisoire ? View.VISIBLE : View.GONE);
            inputClient.setVisibility(isClientProvisoire ? View.GONE : View.VISIBLE);
        });
    }

    private void setupInputs(View view) {
        layoutDescription = view.findViewById(R.id.layout_description);
        layoutQuantite = view.findViewById(R.id.layout_quantite);
        layoutPrix = view.findViewById(R.id.layout_prix);
        layoutTva = view.findViewById(R.id.layout_tva);

        txtDesciption = view.findViewById(R.id.edit_description);
        txtQuantite = view.findViewById(R.id.edit_quantite);
        txtQuantite.setText("1");
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
        signatureView = view.findViewById(R.id.signature_view);

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
    }

    private void handleGenerateInvoice() {
        // ... Logic to generate invoice ...
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
        // Placeholder for PDF generation logic (to keep file consistent)
        if (listener != null) listener.onInvoiceGenerated();
        dismiss();
    }
}

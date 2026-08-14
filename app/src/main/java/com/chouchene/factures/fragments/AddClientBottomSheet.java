package com.chouchene.factures.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chouchene.factures.R;
import com.chouchene.factures.api.FetchVilleFromCodePostale;
import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.repository.ClientRepository;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.Executors;

public class AddClientBottomSheet extends BottomSheetDialogFragment {

    private Client client;
    private OnClientSavedListener listener;
    private ClientRepository clientRepository;
    private ClientDao clientDao;

    private TextInputEditText txtName, txtEmail, txtPhone, txtRue, txtVille, txtCodePostale, txtPays, txtSiren, txtTva;
    private ViewFlipper viewFlipper;
    private MaterialButton btnBack, btnNext, btnSave;
    private View stepIndicator1, stepIndicator2;
    private int currentStep = 0;

    public interface OnClientSavedListener {
        void onClientSaved();
    }

    public static AddClientBottomSheet newInstance(Client client) {
        AddClientBottomSheet fragment = new AddClientBottomSheet();
        fragment.client = client;
        return fragment;
    }

    public void setOnClientSavedListener(OnClientSavedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_client, container, false);
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
        clientDao = DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase().clientDao();
        clientRepository = new ClientRepository(clientDao);
        setupInputs(view);
        setupStepper(view);
        if (client != null) populateFields();
        updateStepperUI();
    }

    private void setupInputs(View view) {
        txtName = initItem(view.findViewById(R.id.item_name), R.drawable.ic_nav_user_outline, "Nom complet", android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        txtRue = initItem(view.findViewById(R.id.item_street), R.drawable.ic_outline_road, "Rue", android.text.InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        txtCodePostale = initItem(view.findViewById(R.id.item_zip), R.drawable.ic_outline_hash, "Code Postal", android.text.InputType.TYPE_CLASS_NUMBER);
        txtVille = initItem(view.findViewById(R.id.item_city), R.drawable.ic_outline_building, "Ville", android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        txtPays = initItem(view.findViewById(R.id.item_country), R.drawable.ic_tab_world, "Pays", android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        txtSiren = initItem(view.findViewById(R.id.item_siren), R.drawable.ic_outline_adjustments, "SIREN", android.text.InputType.TYPE_CLASS_NUMBER);
        txtTva = initItem(view.findViewById(R.id.item_tva), R.drawable.ic_outline_cash, "TVA", android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        txtPhone = initItem(view.findViewById(R.id.item_phone), R.drawable.ic_outline_phone, "Téléphone", android.text.InputType.TYPE_CLASS_PHONE);
        txtEmail = initItem(view.findViewById(R.id.item_email), R.drawable.ic_outline_mail, "Email", android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        txtCodePostale.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() >= 5) com.chouchene.factures.api.FetchVilleFromCodePostale.fetchDataFromApiWithParams(s.toString(), txtVille, txtPays);
            }
        });
    }

    private TextInputEditText initItem(View itemView, int iconRes, String label, int inputType) {
        ImageView icon = itemView.findViewById(R.id.item_icon);
        TextView txtLabel = itemView.findViewById(R.id.item_label);
        TextInputEditText input = itemView.findViewById(R.id.item_input);
        icon.setImageResource(iconRes);
        txtLabel.setText(label);
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

    private void setupStepper(View view) {
        viewFlipper = view.findViewById(R.id.view_flipper);
        btnBack = view.findViewById(R.id.btn_back_client);
        btnNext = view.findViewById(R.id.btn_next_client);
        btnSave = view.findViewById(R.id.btn_save);
        stepIndicator1 = view.findViewById(R.id.step_indicator_1);
        stepIndicator2 = view.findViewById(R.id.step_indicator_2);
        btnNext.setOnClickListener(v -> goToNextStep());
        btnBack.setOnClickListener(v -> goToPreviousStep());
        btnSave.setOnClickListener(v -> handleSaveClient());
    }

    private void goToNextStep() {
        if (currentStep == 0 && txtName.getText().toString().trim().isEmpty()) {
            txtName.setError(getString(R.string.msg_name_required)); return;
        }
        if (currentStep < 1) {
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
        btnNext.setVisibility(currentStep == 1 ? View.GONE : View.VISIBLE);
        btnSave.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        updateIndicator(stepIndicator1, currentStep >= 0);
        updateIndicator(stepIndicator2, currentStep >= 1);
    }

    private void updateIndicator(View bar, boolean active) {
        bar.setAlpha(active ? 1.0f : 0.2f);
    }

    private void populateFields() {
        txtName.setText(client.getClientName());
        txtRue.setText(client.getStreet());
        txtVille.setText(client.getVille());
        txtCodePostale.setText(client.getCodePostale());
        txtPays.setText(client.getPays());
        txtSiren.setText(client.getNumeroSiren());
        txtTva.setText(client.getNumeroTVA());
        txtEmail.setText(client.getEmail());
        txtPhone.setText(client.phone);
    }

    private void handleSaveClient() {
        String customerName = txtName.getText().toString();
        String rueClient = txtRue.getText().toString();
        String villeClient = txtVille.getText().toString();
        String cpClient = txtCodePostale.getText().toString();
        String paysClient = txtPays.getText().toString();
        String sirenClient = txtSiren.getText().toString();
        String tvaClient = txtTva.getText().toString();
        String emailClient = txtEmail.getText().toString();
        String phoneClient = txtPhone.getText().toString();

        Executors.newSingleThreadExecutor().execute(() -> {
            Client c = new Client(customerName, rueClient, villeClient, cpClient, paysClient, sirenClient, tvaClient, emailClient, phoneClient);
            if (client != null && client.getId() != 0) {
                c.setId(client.getId());
                clientRepository.updateClient(c);
            } else {
                clientRepository.addClientIfNotExists(c);
            }
            if (getActivity() != null) getActivity().runOnUiThread(() -> {
                if (listener != null) listener.onClientSaved();
                dismiss();
            });
        });
    }
}

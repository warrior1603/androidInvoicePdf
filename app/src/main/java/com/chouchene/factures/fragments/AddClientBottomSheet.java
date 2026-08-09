package com.chouchene.factures.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import com.chouchene.factures.utils.GlassUtils;
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
    private TextView stepNumber1, stepNumber2;
    private TextView stepLabel1, stepLabel2;
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
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            GlassUtils.applyGlassEffect(getDialog().getWindow(), 80f);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        clientDao = DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase().clientDao();
        clientRepository = new ClientRepository(clientDao);

        setupInputs(view);
        setupStepper(view);

        if (client != null) {
            populateFields();
        }
    }

    private void setupInputs(View view) {
        txtName = view.findViewById(R.id.edit_user_name_client);
        txtRue = view.findViewById(R.id.edit_street);
        txtVille = view.findViewById(R.id.edit_ville);
        txtCodePostale = view.findViewById(R.id.edit_code_postale);
        txtPays = view.findViewById(R.id.edit_pays);
        txtSiren = view.findViewById(R.id.edit_siren);
        txtTva = view.findViewById(R.id.tva_client);
        txtEmail = view.findViewById(R.id.edit_email_client);
        txtPhone = view.findViewById(R.id.edit_phone_client);

        txtCodePostale.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                FetchVilleFromCodePostale.fetchDataFromApiWithParams(s.toString(), txtVille, txtPays);
            }
        });
    }

    private void setupStepper(View view) {
        viewFlipper = view.findViewById(R.id.view_flipper);
        btnBack = view.findViewById(R.id.btn_back_client);
        btnNext = view.findViewById(R.id.btn_next_client);
        btnSave = view.findViewById(R.id.btn_save);

        stepNumber1 = view.findViewById(R.id.step_number_1);
        stepNumber2 = view.findViewById(R.id.step_number_2);
        stepLabel1 = view.findViewById(R.id.step_label_1);
        stepLabel2 = view.findViewById(R.id.step_label_2);

        btnNext.setOnClickListener(v -> goToNextStep());
        btnBack.setOnClickListener(v -> goToPreviousStep());
        btnSave.setOnClickListener(v -> handleSaveClient());

        updateStepperUI();
    }

    private void goToNextStep() {
        if (currentStep == 0) {
            if (txtName.getText().toString().trim().isEmpty()) {
                txtName.setError(getString(R.string.msg_name_required));
                return;
            }
        }
        
        if (currentStep < 1) {
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
        btnNext.setVisibility(currentStep == 1 ? View.GONE : View.VISIBLE);
        btnSave.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);

        updateStepIndicator(stepNumber1, stepLabel1, currentStep >= 0);
        updateStepIndicator(stepNumber2, stepLabel2, currentStep >= 1);
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
        boolean isEdit = client != null && client.getId() != 0;
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
            if (isEdit) {
                c.setId(client.getId());
                clientRepository.updateClient(c);
            } else {
                clientRepository.addClientIfNotExists(c);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (listener != null) listener.onClientSaved();
                    dismiss();
                });
            }
        });
    }
}

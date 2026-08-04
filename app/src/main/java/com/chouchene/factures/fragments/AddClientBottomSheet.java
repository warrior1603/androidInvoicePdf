package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import com.chouchene.factures.R;
import com.chouchene.factures.api.FetchVilleFromCodePostale;
import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.repository.ClientRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class AddClientBottomSheet extends BottomSheetDialogFragment {

    private Client client;
    private OnClientSavedListener listener;
    private ClientRepository clientRepository;
    private ClientDao clientDao;

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        clientDao = DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase().clientDao();
        clientRepository = new ClientRepository(clientDao);

        TextView title = view.findViewById(R.id.title);
        TextInputEditText txtName = view.findViewById(R.id.edit_user_name_client);
        TextInputEditText txtRue = view.findViewById(R.id.edit_street);
        TextInputEditText txtVille = view.findViewById(R.id.edit_ville);
        TextInputEditText txtCodePostale = view.findViewById(R.id.edit_code_postale);
        TextInputEditText txtPays = view.findViewById(R.id.edit_pays);
        TextInputEditText txtSiren = view.findViewById(R.id.edit_siren);
        TextInputEditText txtTva = view.findViewById(R.id.tva_client);
        TextInputEditText txtEmail = view.findViewById(R.id.edit_email_client);
        TextInputEditText txtPhone = view.findViewById(R.id.edit_phone_client);
        MaterialButton btnSave = view.findViewById(R.id.btn_save);

        boolean isEdit = client != null;
        title.setText(isEdit ? "Modifier client" : "Nouveau client");

        if (isEdit) {
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

        txtCodePostale.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                FetchVilleFromCodePostale.fetchDataFromApiWithParams(s.toString(), txtVille, txtPays);
            }
        });

        btnSave.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            String customerName = txtName.getText().toString();
            String rueClient = txtRue.getText().toString();
            String villeClient = txtVille.getText().toString();
            String cpClient = txtCodePostale.getText().toString();
            String paysClient = txtPays.getText().toString();
            String sirenClient = txtSiren.getText().toString();
            String tvaClient = txtTva.getText().toString();
            String emailClient = txtEmail.getText().toString();
            String phoneClient = txtPhone.getText().toString();

            if (customerName.trim().isEmpty()) {
                txtName.setError("Le nom est obligatoire");
                return;
            }

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
        });
    }
}

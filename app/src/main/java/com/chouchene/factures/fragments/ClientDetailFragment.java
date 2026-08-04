package com.chouchene.factures.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.chouchene.factures.R;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Client;
import com.google.android.material.transition.MaterialContainerTransform;

public class ClientDetailFragment extends Fragment {

    private Client client;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.setDrawingViewId(R.id.nav_host_fragment);
        transform.setDuration(450);
        transform.setScrimColor(Color.TRANSPARENT);
        setSharedElementEnterTransition(transform);
        
        if (getArguments() != null) {
            int clientId = getArguments().getInt("client_id", -1);
            if (clientId != -1) {
                client = DatabaseClient.getInstance(requireContext().getApplicationContext())
                        .getAppDatabase().clientDao().getClientById(clientId);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_client_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (client == null) {
            Navigation.findNavController(view).popBackStack();
            return;
        }

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        com.google.android.material.appbar.CollapsingToolbarLayout collapsingToolbarLayout = view.findViewById(R.id.toolbar_layout);
        collapsingToolbarLayout.setTitle(client.getClientName());

        TextView txtAddress = view.findViewById(R.id.detail_address);
        TextView txtPhone = view.findViewById(R.id.detail_phone);
        TextView txtEmail = view.findViewById(R.id.detail_email);
        TextView txtSiren = view.findViewById(R.id.detail_siren);
        TextView txtTva = view.findViewById(R.id.detail_tva);
        
        String fullAddress = client.getStreet() + "\n" + client.getCodePostale() + " " + client.getVille() + ", " + client.getPays();
        txtAddress.setText(fullAddress);
        
        txtPhone.setText(client.phone != null ? client.phone : "N/A");
        txtEmail.setText(client.getEmail());
        
        txtSiren.setText("SIREN: " + (client.getNumeroSiren() != null ? client.getNumeroSiren() : "N/A"));
        txtTva.setText("TVA: " + (client.getNumeroTVA() != null ? client.getNumeroTVA() : "N/A"));

        view.findViewById(R.id.layout_call).setOnClickListener(v -> {
            if (client.phone != null && !client.phone.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + client.phone)));
            }
        });

        view.findViewById(R.id.layout_email).setOnClickListener(v -> {
            if (client.getEmail() != null && !client.getEmail().isEmpty()) {
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + client.getEmail())));
            }
        });

        view.findViewById(R.id.fab_edit_client).setOnClickListener(v -> {
            AddClientBottomSheet bottomSheet = AddClientBottomSheet.newInstance(client);
            bottomSheet.setOnClientSavedListener(() -> {
                client = DatabaseClient.getInstance(requireContext().getApplicationContext())
                        .getAppDatabase().clientDao().getClientById(client.getId());
                onViewCreated(view, null); // Refresh
            });
            bottomSheet.show(getChildFragmentManager(), "EDIT_CLIENT");
        });

        view.findViewById(R.id.btn_create_invoice_for_client).setOnClickListener(v -> {
            CreateInvoiceBottomSheet bottomSheet = CreateInvoiceBottomSheet.newInstance(client.getId());
            bottomSheet.show(getChildFragmentManager(), "CREATE_INVOICE_FOR_CLIENT");
        });
    }
}

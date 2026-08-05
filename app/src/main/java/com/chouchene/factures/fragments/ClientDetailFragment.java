package com.chouchene.factures.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.adapter.HistoryAdapter;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.utils.AvatarHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.transition.MaterialContainerTransform;

import java.util.List;
import java.util.concurrent.Executors;

public class ClientDetailFragment extends Fragment implements HistoryAdapter.OnHistoryActionListener {

    private Client client;
    private AppDatabase db;
    private HistoryAdapter adapter;
    private RecyclerView rvHistory;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.setDrawingViewId(R.id.nav_host_fragment);
        transform.setDuration(450);
        transform.setScrimColor(Color.TRANSPARENT);
        setSharedElementEnterTransition(transform);
        
        db = DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase();

        if (getArguments() != null) {
            int clientId = getArguments().getInt("client_id", -1);
            if (clientId != -1) {
                client = db.clientDao().getClientById(clientId);
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

        com.google.android.material.appbar.CollapsingToolbarLayout collapsingToolbarLayout = view.findViewById(R.id.toolbar_layout);
        collapsingToolbarLayout.setTitle(client.getClientName());

        TextView txtInitials = view.findViewById(R.id.detail_initials);
        MaterialCardView avatarContainer = view.findViewById(R.id.avatar_container);
        
        if (txtInitials != null) {
            txtInitials.setText(AvatarHelper.getInitials(client.getClientName()));
        }
        if (avatarContainer != null) {
            avatarContainer.setCardBackgroundColor(AvatarHelper.getColorForName(client.getClientName()));
        }

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
                client = db.clientDao().getClientById(client.getId());
                onViewCreated(view, null); // Refresh
            });
            bottomSheet.show(getChildFragmentManager(), "EDIT_CLIENT");
        });

        view.findViewById(R.id.btn_create_invoice_for_client).setOnClickListener(v -> {
            CreateInvoiceBottomSheet bottomSheet = CreateInvoiceBottomSheet.newInstance(client.getId());
            bottomSheet.setOnInvoiceGeneratedListener(this::loadHistory);
            bottomSheet.show(getChildFragmentManager(), "CREATE_INVOICE_FOR_CLIENT");
        });

        rvHistory = view.findViewById(R.id.rv_client_history);
        setupRecyclerView();
        loadHistory();
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(this);
        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(adapter);
    }

    private void loadHistory() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Invoice> history = db.invoiceDao().getInvoicesByClient(client.getClientName());
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    adapter.setData(history);
                });
            }
        });
    }

    @Override
    public void onItemClick(Invoice invoice) {
        Bundle b = new Bundle();
        b.putString("file_path", invoice.filePath);
        b.putString("mail_client", client.getEmail());
        Navigation.findNavController(requireView()).navigate(R.id.webViewPdfFragment, b);
    }

    @Override
    public void onDeleteClick(Invoice invoice) {
        // Implementation can be added if deletion from here is desired
    }

    @Override
    public void onStatusClick(Invoice invoice) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_status_selector, null);

        view.findViewById(R.id.status_pending).setOnClickListener(v -> updateStatus(invoice, "En attente", dialog));
        view.findViewById(R.id.status_paid).setOnClickListener(v -> updateStatus(invoice, "Payée", dialog));
        view.findViewById(R.id.status_cancelled).setOnClickListener(v -> updateStatus(invoice, "Annulée", dialog));

        dialog.setContentView(view);
        dialog.show();
    }

    private void updateStatus(Invoice invoice, String status, BottomSheetDialog dialog) {
        Executors.newSingleThreadExecutor().execute(() -> {
            invoice.status = status;
            db.invoiceDao().updateInvoice(invoice);
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    dialog.dismiss();
                    loadHistory();
                });
            }
        });
    }
}

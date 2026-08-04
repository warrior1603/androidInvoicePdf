package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.adapter.HistoryAdapter;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.utils.SwipeToDeleteCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;
import java.util.concurrent.Executors;

public class InvoiceGenrationFragment extends Fragment implements HistoryAdapter.OnHistoryActionListener {

    private AppDatabase db;
    private HistoryAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;

    public InvoiceGenrationFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = DatabaseClient.getInstance(requireContext()).getAppDatabase();

        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.empty_state);
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setText("Ajouter Facture");

        adapter = new HistoryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        new ItemTouchHelper(new SwipeToDeleteCallback(requireContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                onDeleteClick(adapter.getInvoiceAt(position));
            }
        }).attachToRecyclerView(recyclerView);

        fab.setOnClickListener(v -> {
            CreateInvoiceBottomSheet bottomSheet = new CreateInvoiceBottomSheet();
            bottomSheet.setOnInvoiceGeneratedListener(this::loadInvoices);
            bottomSheet.show(getChildFragmentManager(), "CREATE_INVOICE");
        });

        loadInvoices();
    }

    private void loadInvoices() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Invoice> invoices = db.invoiceDao().getInvoicesOnly();
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    adapter.setData(invoices);
                    checkEmptyState();
                });
            }
        });
    }

    private void checkEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onItemClick(Invoice invoice) {
        Bundle b = new Bundle();
        b.putString("file_path", invoice.filePath);
        
        Executors.newSingleThreadExecutor().execute(() -> {
            com.chouchene.factures.entity.Client client = db.clientDao().getClientByName(invoice.clientName);
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (client != null) {
                        b.putString("mail_client", client.getEmail());
                    }
                    Navigation.findNavController(requireView()).navigate(R.id.webViewPdfFragment, b);
                });
            }
        });
    }

    @Override
    public void onDeleteClick(Invoice invoice) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer")
                .setMessage("Voulez-vous supprimer cette facture ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.invoiceDao().deleteInvoice(invoice);
                        loadInvoices();
                    });
                })
                .setNegativeButton("Annuler", (dialog, which) -> adapter.notifyDataSetChanged())
                .setOnCancelListener(dialog -> adapter.notifyDataSetChanged())
                .show();
    }

    @Override
    public void onStatusClick(Invoice invoice) {
        String[] statuses = {"En attente", "Payée", "Annulée"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Modifier le statut")
                .setItems(statuses, (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        invoice.status = statuses[which];
                        db.invoiceDao().updateInvoice(invoice);
                        loadInvoices();
                    });
                })
                .show();
    }
}

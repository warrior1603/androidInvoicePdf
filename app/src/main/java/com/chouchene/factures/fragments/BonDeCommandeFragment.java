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
import androidx.room.Room;

import com.chouchene.factures.R;
import com.chouchene.factures.adapter.HistoryAdapter;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.utils.SwipeToDeleteCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

public class BonDeCommandeFragment extends Fragment implements HistoryAdapter.OnHistoryActionListener {

    private AppDatabase db;
    private HistoryAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;

    public BonDeCommandeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = Room.databaseBuilder(requireContext(), AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build();

        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.empty_state);
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setText("Ajouter Bon de Commande");

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
            CreateBonBottomSheet bottomSheet = new CreateBonBottomSheet();
            bottomSheet.setOnBonGeneratedListener(this::loadBons);
            bottomSheet.show(getChildFragmentManager(), "CREATE_BON");
        });

        loadBons();
    }

    private void loadBons() {
        List<Invoice> bons = db.invoiceDao().getBonsOnly();
        adapter.setData(bons);
        checkEmptyState();
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
        
        com.chouchene.factures.entity.Client client = db.clientDao().getClientByName(invoice.clientName);
        if (client != null) {
            b.putString("mail_client", client.getEmail());
        }
        
        Navigation.findNavController(requireView()).navigate(R.id.webViewPdfFragment, b);
    }

    @Override
    public void onDeleteClick(Invoice invoice) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer")
                .setMessage("Voulez-vous supprimer ce bon de commande ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    db.invoiceDao().deleteInvoice(invoice);
                    loadBons();
                })
                .setNegativeButton("Annuler", (dialog, which) -> adapter.notifyDataSetChanged())
                .setOnCancelListener(dialog -> adapter.notifyDataSetChanged())
                .show();
    }
}

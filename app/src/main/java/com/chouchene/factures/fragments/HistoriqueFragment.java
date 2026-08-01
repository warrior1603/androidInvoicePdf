package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import androidx.navigation.Navigation;

import com.chouchene.factures.R;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.entity.Invoice;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistoriqueFragment extends Fragment {

    private RecyclerView recyclerView;
    private AppDatabase db;
    private View emptyState;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historique, container, false);

        MaterialToolbar toolbar = requireActivity().findViewById(R.id.my_toolbar);
        if (toolbar == null) toolbar = requireActivity().findViewById(R.id.my_toolbar1);
        if (toolbar != null) toolbar.setNavigationIcon(R.drawable.ic_history_custom);

        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.empty_state);

        db = Room.databaseBuilder(requireContext(), AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadHistory();

        return view;
    }

    private void loadHistory() {
        List<Invoice> invoices = db.invoiceDao().getAllInvoices();
        if (invoices.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(new HistoryAdapter(invoices));
        }
    }

    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<Invoice> invoices;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        HistoryAdapter(List<Invoice> invoices) {
            this.invoices = invoices;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Invoice invoice = invoices.get(position);
            holder.clientName.setText(invoice.clientName != null ? invoice.clientName : "Client inconnu");
            holder.date.setText(dateFormat.format(invoice.date));
            holder.amount.setText(String.format(Locale.getDefault(), "%.2f €", invoice.amount));
            holder.icon.setImageResource("Bon".equals(invoice.type) ? R.drawable.buy_icon : R.drawable.invoice_alternative);

            holder.itemView.setOnClickListener(v -> {
                Bundle b = new Bundle();
                b.putString("file_path", invoice.filePath);
                b.putString("mail_client", ""); 
                Navigation.findNavController(v).navigate(R.id.webViewPdfFragment, b);
            });
        }

        @Override
        public int getItemCount() {
            return invoices.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView clientName, date, amount;
            final ImageView icon;

            ViewHolder(View itemView) {
                super(itemView);
                clientName = itemView.findViewById(R.id.client_name);
                date = itemView.findViewById(R.id.invoice_date);
                amount = itemView.findViewById(R.id.invoice_amount);
                icon = itemView.findViewById(R.id.type_icon);
            }
        }
    }
}

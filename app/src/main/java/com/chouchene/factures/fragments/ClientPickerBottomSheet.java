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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Client;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ClientPickerBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText editSearch;
    private ClientAdapter adapter;
    private AppDatabase db;
    private List<Client> allClients = new ArrayList<>();
    private OnClientSelectedListener listener;

    public interface OnClientSelectedListener {
        void onClientSelected(Client client);
    }

    public void setOnClientSelectedListener(OnClientSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_client_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = DatabaseClient.getInstance(requireContext()).getAppDatabase();
        RecyclerView rvClients = view.findViewById(R.id.rvClients);
        editSearch = view.findViewById(R.id.editSearch);

        rvClients.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ClientAdapter();
        rvClients.setAdapter(adapter);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterClients(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadClients();
    }

    private void loadClients() {
        Executors.newSingleThreadExecutor().execute(() -> {
            allClients = db.clientDao().getAllClients();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> adapter.setClients(allClients));
            }
        });
    }

    private void filterClients(String query) {
        List<Client> filtered = new ArrayList<>();
        for (Client c : allClients) {
            if (c.clientName != null && c.clientName.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(c);
            }
        }
        adapter.setClients(filtered);
    }

    private class ClientAdapter extends RecyclerView.Adapter<ClientAdapter.ViewHolder> {
        private List<Client> clients = new ArrayList<>();

        public void setClients(List<Client> clients) {
            this.clients = clients;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client_picker, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Client client = clients.get(position);
            holder.txtName.setText(client.clientName);
            holder.txtPhone.setText(client.phone != null ? client.phone : "");
            
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClientSelected(client);
                dismiss();
            });
        }

        @Override
        public int getItemCount() { return clients.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtName, txtPhone;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtName = itemView.findViewById(R.id.txtName);
                txtPhone = itemView.findViewById(R.id.txtPhone);
            }
        }
    }
}

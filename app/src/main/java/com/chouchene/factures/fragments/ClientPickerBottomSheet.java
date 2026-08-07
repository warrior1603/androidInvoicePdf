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
    private List<Client> recentClients = new ArrayList<>();
    private OnClientSelectedListener listener;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_RECENT = 1;
    private static final int TYPE_ALL = 2;

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
            recentClients = db.clientDao().getRecentClients();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> adapter.setClients(allClients, recentClients));
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
        adapter.setClients(filtered, query.isEmpty() ? recentClients : new ArrayList<>());
    }

    private class ClientAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<Client> clients = new ArrayList<>();
        private List<Client> recents = new ArrayList<>();

        public void setClients(List<Client> clients, List<Client> recents) {
            this.clients = clients;
            this.recents = recents;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            if (!recents.isEmpty()) {
                if (position == 0) return TYPE_HEADER;
                if (position <= recents.size()) return TYPE_RECENT;
                if (position == recents.size() + 1) return TYPE_HEADER;
            }
            return TYPE_ALL;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_header_simple, parent, false);
                return new HeaderViewHolder(v);
            }
            return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client_picker, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                HeaderViewHolder h = (HeaderViewHolder) holder;
                if (!recents.isEmpty()) {
                    h.text.setText(position == 0 ? "Récents" : "Tous les clients");
                } else {
                    h.text.setText("Tous les clients");
                }
            } else {
                ItemViewHolder h = (ItemViewHolder) holder;
                Client client;
                if (!recents.isEmpty() && position <= recents.size()) {
                    client = recents.get(position - 1);
                } else {
                    int offset = recents.isEmpty() ? 0 : recents.size() + 2;
                    client = clients.get(position - offset);
                }
                
                h.txtName.setText(client.clientName);
                h.txtPhone.setText(client.phone != null ? client.phone : "");
                h.itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onClientSelected(client);
                    dismiss();
                });
            }
        }

        @Override
        public int getItemCount() {
            if (clients.isEmpty()) return 0;
            return clients.size() + (recents.isEmpty() ? 1 : recents.size() + 2);
        }

        class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            HeaderViewHolder(View v) { super(v); text = v.findViewById(R.id.txt_header_title); }
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView txtName, txtPhone;
            public ItemViewHolder(@NonNull View itemView) {
                super(itemView);
                txtName = itemView.findViewById(R.id.txtName);
                txtPhone = itemView.findViewById(R.id.txtPhone);
            }
        }
    }
}

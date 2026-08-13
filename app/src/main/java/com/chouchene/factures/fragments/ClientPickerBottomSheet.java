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
        
        // Initialize Search Field with Settings Style
        View searchItemView = view.findViewById(R.id.item_search);
        android.widget.ImageView icon = searchItemView.findViewById(R.id.item_icon);
        TextView txtLabel = searchItemView.findViewById(R.id.item_label);
        editSearch = searchItemView.findViewById(R.id.item_input);

        icon.setImageResource(R.drawable.ic_search_outline);
        txtLabel.setText("Rechercher");
        
        try {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
            txtLabel.setTextColor(typedValue.data);
            txtLabel.setAlpha(0.9f);
            icon.setColorFilter(typedValue.data);
        } catch (Exception ignored) {}

        editSearch.setHint("Nom, Téléphone...");

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
            List<Client> all = db.clientDao().getAllClients();
            List<Client> recent = db.clientDao().getRecentClients();
            
            allClients = all != null ? all : new ArrayList<>();
            recentClients = recent != null ? recent : new ArrayList<>();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter.setClients(allClients, recentClients);
                    if (getView() != null) {
                        RecyclerView rv = getView().findViewById(R.id.rvClients);
                        if (rv != null) rv.scheduleLayoutAnimation();
                    }
                });
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
        private final List<Object> displayList = new ArrayList<>();

        public void setClients(List<Client> all, List<Client> recents) {
            displayList.clear();
            if (!recents.isEmpty()) {
                displayList.add("Récents");
                displayList.addAll(recents);
            }
            if (!all.isEmpty()) {
                displayList.add("Tous les clients");
                displayList.addAll(all);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            Object item = displayList.get(position);
            if (item instanceof String) return TYPE_HEADER;
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
            Object item = displayList.get(position);
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).text.setText((String) item);
            } else if (holder instanceof ItemViewHolder) {
                Client client = (Client) item;
                ItemViewHolder h = (ItemViewHolder) holder;
                h.txtName.setText(client.clientName != null ? client.clientName : "Inconnu");
                h.txtPhone.setText(client.phone != null ? client.phone : "");
                h.itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onClientSelected(client);
                    dismiss();
                });
            }
        }

        @Override
        public int getItemCount() {
            return displayList.size();
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

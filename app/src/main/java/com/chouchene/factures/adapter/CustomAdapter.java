package com.chouchene.factures.adapter;

import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.chouchene.factures.R;
import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.fragments.AddClientBottomSheet;
import com.chouchene.factures.repository.ClientRepository;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

    private final FragmentActivity fragmentActivity;
    private final ClientDao clientDao;
    private final ArrayList<Client> originalList;
    private final ArrayList<Client> filteredList;
    private int highlightClientId = -1;
    private OnDataChangedListener onDataChangedListener;

    public interface OnDataChangedListener {
        void onDataChanged();
    }

    public void setOnDataChangedListener(OnDataChangedListener listener) {
        this.onDataChangedListener = listener;
    }

    public CustomAdapter(FragmentActivity context, ArrayList<Client> values, int highlightClientId) {
        this.fragmentActivity = context;
        this.originalList = new ArrayList<>(values);
        this.filteredList = new ArrayList<>(values);
        this.highlightClientId = highlightClientId;
        this.clientDao = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build().clientDao();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_with_button, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Client client = filteredList.get(position);
        holder.textView.setText(client.getClientName());

        if (client.getId() == highlightClientId) {
            bindDetails(holder, client);
            holder.datailsText.setVisibility(View.VISIBLE);
        } else {
            holder.datailsText.setVisibility(View.GONE);
        }

        holder.cardview.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;
            
            Client clickedClient = filteredList.get(currentPos);
            int previousHighlightId = highlightClientId;
            
            if (clickedClient.getId() == previousHighlightId) {
                // Clicking the same one closes it
                highlightClientId = -1;
            } else {
                // Open the new one, this will automatically close the previous one 
                // because of the logic in onBindViewHolder
                highlightClientId = clickedClient.getId();
            }
            
            // Using notifyDataSetChanged to ensure only one is open at a time
            notifyDataSetChanged();
        });

        holder.editbutton.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;
            
            Client currentClient = filteredList.get(currentPos);
            AddClientBottomSheet bottomSheet = AddClientBottomSheet.newInstance(currentClient);
            bottomSheet.setOnClientSavedListener(() -> {
                setData((ArrayList<Client>) clientDao.getAllClients());
            });
            bottomSheet.show(fragmentActivity.getSupportFragmentManager(), "EDIT_CLIENT");
        });
    }

    private void bindDetails(ViewHolder holder, Client client) {
        holder.txtRue.setText(client.getStreet());
        holder.txtVille.setText(client.getCodePostale() + " " + client.getVille());
        holder.txtPays.setText(client.getPays());
        holder.txtSiren.setText("SIREN: " + client.getNumeroSiren());
        holder.txtEmail.setText(client.getEmail());
        holder.txtTva.setText("TVA: " + client.getNumeroTVA());
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void setData(ArrayList<Client> clients) {
        originalList.clear();
        originalList.addAll(clients);
        filteredList.clear();
        filteredList.addAll(clients);
        notifyDataSetChanged();
        if (onDataChangedListener != null) {
            onDataChangedListener.onDataChanged();
        }
    }

    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Client obj : originalList) {
                if (obj.getClientName().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(obj);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setHighlightId(int id) {
        this.highlightClientId = id;
        notifyDataSetChanged();
    }

    public Client getClientAt(int position) {
        return filteredList.get(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        MaterialButton editbutton;
        LinearLayout datailsText, layout;
        View cardview;
        TextView txtRue, txtVille, txtPays, txtSiren, txtEmail, txtTva;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textViewItem);
            editbutton = itemView.findViewById(R.id.buttonEdit);
            datailsText = itemView.findViewById(R.id.details);
            cardview = itemView.findViewById(R.id.cardView);
            layout = itemView.findViewById(R.id.layout1);
            txtRue = itemView.findViewById(R.id.show_rue);
            txtVille = itemView.findViewById(R.id.show_ville);
            txtPays = itemView.findViewById(R.id.show_pays);
            txtSiren = itemView.findViewById(R.id.show_siren);
            txtEmail = itemView.findViewById(R.id.show_email);
            txtTva = itemView.findViewById(R.id.show_tva);
        }
    }
}

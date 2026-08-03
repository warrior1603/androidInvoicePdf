package com.chouchene.factures.adapter;

import android.animation.LayoutTransition;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

    private final FragmentActivity fragmentActivity;
    private ClientRepository clientRepository;
    private final ClientDao clientDao;
    private final ArrayList<Client> originalList;
    private final ArrayList<Client> filteredList;
    private OnDataChangedListener onDataChangedListener;

    public interface OnDataChangedListener {
        void onDataChanged();
    }

    public void setOnDataChangedListener(OnDataChangedListener listener) {
        this.onDataChangedListener = listener;
    }

    public CustomAdapter(FragmentActivity context, ArrayList<Client> values) {
        this.fragmentActivity = context;
        this.originalList = new ArrayList<>(values);
        this.filteredList = new ArrayList<>(values);
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
        Client client = filteredList.get(holder.getAdapterPosition());
        holder.textView.setText(client.getClientName());

        holder.cardview.setOnClickListener(v -> {
            Client currentClient = filteredList.get(holder.getAdapterPosition());
            holder.txtRue.setText(currentClient.getStreet());
            holder.txtVille.setText(currentClient.getCodePostale() + " " + currentClient.getVille());
            holder.txtPays.setText(currentClient.getPays());
            holder.txtSiren.setText("SIREN: " + currentClient.getNumeroSiren());
            holder.txtEmail.setText(currentClient.getEmail());
            holder.txtTva.setText("TVA: " + currentClient.getNumeroTVA());

            holder.layout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGE_APPEARING);
            int visibility = (holder.datailsText.getVisibility() == View.GONE) ? View.VISIBLE : View.GONE;
            TransitionManager.beginDelayedTransition(holder.layout, new AutoTransition());
            holder.datailsText.setVisibility(visibility);
        });

        holder.editbutton.setOnClickListener(v -> {
            Client currentClient = filteredList.get(holder.getAdapterPosition());
            AddClientBottomSheet bottomSheet = AddClientBottomSheet.newInstance(currentClient);
            bottomSheet.setOnClientSavedListener(() -> {
                setData((ArrayList<Client>) clientDao.getAllClients());
            });
            bottomSheet.show(fragmentActivity.getSupportFragmentManager(), "EDIT_CLIENT");
        });

        holder.deleteButton.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            Client clientToDelete = filteredList.get(currentPos);
            new MaterialAlertDialogBuilder(fragmentActivity)
                    .setTitle("Confirmez-vous la suppression ?")
                    .setMessage("Si vous confirmez, votre client sera definitivement effacé de la liste des clients?")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            clientRepository = new ClientRepository(clientDao);
                            clientRepository.deleteClient(clientToDelete);
                            fragmentActivity.runOnUiThread(() -> {
                                originalList.remove(clientToDelete);
                                filteredList.remove(currentPos);
                                notifyItemRemoved(currentPos);
                                notifyItemRangeChanged(currentPos, filteredList.size());
                                if (onDataChangedListener != null) {
                                    onDataChangedListener.onDataChanged();
                                }
                            });
                        });
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        MaterialButton editbutton, deleteButton;
        LinearLayout datailsText, layout;
        View cardview;
        TextView txtRue, txtVille, txtPays, txtSiren, txtEmail, txtTva;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textViewItem);
            editbutton = itemView.findViewById(R.id.buttonEdit);
            deleteButton = itemView.findViewById(R.id.imageDelete);
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

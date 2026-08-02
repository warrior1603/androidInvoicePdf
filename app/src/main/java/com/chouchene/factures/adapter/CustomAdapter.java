package com.chouchene.factures.adapter;

import android.animation.LayoutTransition;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.chouchene.factures.api.FetchVilleFromCodePostale;
import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.repository.ClientRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

    private final FragmentActivity fragmentActivity;
    private ClientRepository clientRepository;
    private final ClientDao clientDao;
    private final ArrayList<Client> originalList;
    private final ArrayList<Client> filteredList;

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
            showClientDialog(currentClient);
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
                            });
                        });
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });
    }

    public void showClientDialog(Client client) {
        boolean isEdit = client != null;
        View view = LayoutInflater.from(fragmentActivity).inflate(R.layout.popup_add_client, null);
        
        TextInputEditText txtName = view.findViewById(R.id.edit_user_name_client);
        TextInputEditText txtRue = view.findViewById(R.id.edit_street);
        TextInputEditText txtVille = view.findViewById(R.id.edit_ville);
        TextInputEditText txtCodePostale = view.findViewById(R.id.edit_code_postale);
        TextInputEditText txtPays = view.findViewById(R.id.edit_pays);
        TextInputEditText txtSiren = view.findViewById(R.id.edit_siren);
        TextInputEditText txtTva = view.findViewById(R.id.tva_client);
        TextInputEditText txtEmail = view.findViewById(R.id.edit_email_client);

        txtCodePostale.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                FetchVilleFromCodePostale.fetchDataFromApiWithParams(s.toString(), txtVille, txtPays);
            }
        });

        if (isEdit) {
            txtName.setText(client.getClientName());
            txtRue.setText(client.getStreet());
            txtVille.setText(client.getVille());
            txtCodePostale.setText(client.getCodePostale());
            txtPays.setText(client.getPays());
            txtSiren.setText(client.getNumeroSiren());
            txtTva.setText(client.getNumeroTVA());
            
            String email = client.getEmail();
            if (email != null) {
                txtEmail.setText(email);
            }
        }

        new MaterialAlertDialogBuilder(fragmentActivity)
                .setTitle(isEdit ? "Modifier client" : "Nouveau client")
                .setView(view)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String customerName = txtName.getText().toString();
                    String rueClient = txtRue.getText().toString();
                    String villeClient = txtVille.getText().toString();
                    String cpClient = txtCodePostale.getText().toString();
                    String paysClient = txtPays.getText().toString();
                    String sirenClient = txtSiren.getText().toString();
                    String tvaClient = txtTva.getText().toString();
                    String emailClient = txtEmail.getText().toString();

                    Executors.newSingleThreadExecutor().execute(() -> {
                        Client c = new Client(customerName, rueClient, villeClient, cpClient, paysClient, sirenClient, tvaClient, emailClient);
                        clientRepository = new ClientRepository(clientDao);
                        if (isEdit) {
                            c.setId(client.getId());
                            clientRepository.updateClient(c);
                        } else {
                            clientRepository.addClientIfNotExists(c);
                        }

                        fragmentActivity.runOnUiThread(() -> setData((ArrayList<Client>) clientDao.getAllClients()));
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
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
